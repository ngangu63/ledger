# Ledger

A Spring Boot reference implementation for money-movement infrastructure: a
double-entry **ledger**, **payments** (card/bank), **reimbursements**, and
**banking integrations** (ACH/wire with async settlement). It exists to
demonstrate proven ledger and payment patterns and the architectural decisions
behind them.

**Stack:** Java 17 · Spring Boot 3.3 · PostgreSQL 16 · Flyway · Maven · Docker.

---

## Quick start

```bash
# 1. Start Postgres (Docker)
docker compose up -d postgres

# 2. Run the app
./mvnw spring-boot:run
#   or: ./mvnw -DskipTests package && java -jar target/ledger-0.1.0.jar
```

- API base: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
- Optional DB UI: `docker compose --profile tools up -d pgadmin` → `http://localhost:5050`

Seeded dev users (see [Security](#security)): `admin/admin123`,
`service/service123`, `viewer/viewer123`.

```bash
# Get a token, then call the API
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r .accessToken)
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/v1/accounts
```

---

## Architecture

### Layering

A conventional Spring layering, kept strict so money logic never leaks into the
edges. Dependencies point **inward** — controllers know services, services know
the domain and ports, nothing points back out to the web or a vendor SDK.

```
        HTTP (JSON, JWT, Idempotency-Key)
                     │
        ┌────────────▼────────────┐
        │  Controllers (@Rest…)    │  DTOs in/out, validation, HTTP status
        └────────────┬────────────┘
        ┌────────────▼────────────┐
        │  Services (@Service,     │  business rules, @Transactional boundaries
        │  @Transactional)         │  → LedgerService is the one money-writer
        └──────┬───────────┬───────┘
     ┌─────────▼──┐   ┌─────▼──────────────┐
     │ Repositories│   │ Ports (interfaces) │  PaymentProvider, BankConnector
     │ (Spring Data)│  └─────┬──────────────┘
     └─────────┬──┘         │ adapters (mock / real vendor)
        ┌──────▼───────┐    ▼
        │ PostgreSQL    │  external rails (card processor, bank API)
        │ (Flyway)      │
        └───────────────┘
```

- **`open-in-view: false`** — the persistence session is closed before the
  controller renders, so services must load what the DTO needs inside their
  transaction. This is deliberate: it keeps DB work bounded to the service layer.
- **`ddl-auto: validate`** — Hibernate never generates schema; Flyway owns it, and
  a mismatch fails startup rather than silently drifting.
- Cross-cutting concerns live in `common/`: `GlobalExceptionHandler`
  (`@RestControllerAdvice`) maps exceptions to a uniform `ApiError`; a JWT filter
  populates the security context; `IdempotencyService` guards writes.

### Module map

```
com.lukala.ledger
├── ledger/          Double-entry engine (the core). LedgerService.post/reverse,
│                    Account/Transaction/Posting, AccountBalance checkpoint.
├── account/         Account lifecycle (create/list) + balance checkpoint init.
├── payment/         Payment orchestration → records movement via the ledger.
│   └── provider/    PaymentProvider port + MockPaymentProvider adapter.
├── banking/         Bank transfers (ACH/wire) with async settlement.
│   └── connector/   BankConnector port + MockBankConnector adapter.
├── reimbursement/   Submit→approve→pay workflow; pays via a bank payment.
├── security/        JWT issue/validate, filter, role rules, seeded dev users.
├── config/          OpenAPI, JPA auditing.
└── common/          Money value object, idempotency, BaseEntity, exceptions.
```

**Direction of dependencies:** `payment`, `banking`, and `reimbursement` all
depend on `ledger` (they route money through `LedgerService`) and on `common`.
`ledger` depends only on `common`. No cycles; the ledger core has no knowledge of
payments, banks, or HTTP.

### Request flow — a payment (representative)

Payments, bank transfers, and reimbursements all follow the same shape: an
orchestration service talks to an external **port**, then records the result as a
**balanced ledger transaction**. Payment example:

```
POST /payments (Idempotency-Key)
   → PaymentController
     → IdempotencyService.execute(key, "payment.charge", body)   [at-most-once]
       → PaymentService.charge                    [one DB transaction]
           1. persist Payment (PENDING)
           2. PaymentProvider.charge(...)          → external rail
              • declined → mark FAILED, no ledger entry, return
           3. LedgerService.post(balanced tx)      → debit dest / credit source
              • validates debits == credits, single-currency
              • locks + updates account_balance checkpoints
           4. mark Payment CAPTURED, link ledger tx id
   ← 201 Created (PaymentResponse)
```

A refund or an ACH return runs the inverse: `LedgerService.reverse` posts an
offsetting entry — the original is never mutated.

### Persistence model

```
account ──1:N── posting ──N:1── ledger_transaction ──self-ref── (reversal_of_id)
   │                                    ▲
   └──1:1── account_balance             │ transaction_id
                                payment / bank_transfer / reimbursement
```

`ledger_transaction` + `posting` are the append-only source of truth;
`account_balance` is a derived checkpoint updated in the same transaction.
`payment`, `bank_transfer`, and `reimbursement` are lifecycle records that
**reference** the authoritative `ledger_transaction`. `idempotency_record` backs
the `Idempotency-Key` guarantee.

### Deployment

Stateless JVM service (12-factor: config via env), so it scales horizontally
behind a load balancer; all state is in Postgres. Actuator exposes
`health`/`info`/`metrics`/`prometheus` for load-balancer checks and monitoring.
Packaged as a multi-stage Docker image ([Dockerfile](Dockerfile)); `docker-compose`
runs Postgres (and optional pgAdmin) for local dev. See
[Productionizing](#productionizing-next-steps) for the AWS-targeted evolution.

---

## Core patterns

### Double-entry, append-only ledger
Every movement is a **balanced transaction**: the sum of debits equals the sum
of credits, in a single currency. Postings are **append-only** — nothing is ever
mutated or deleted. To undo, you post a **reversing** transaction (see
`LedgerService.reverse`); the original is kept and flagged `REVERSED`. This is
what makes the ledger auditable.

Balances are **checkpointed** in `account_balance` and updated in the *same* DB
transaction as the postings, under a pessimistic row lock taken in a
deterministic (id-sorted) order — so concurrent postings to an account can't lose
an update or deadlock. `LedgerService` is the one place money is recorded;
payments and bank transfers both route through it.

### Money as `BigDecimal` + currency
`common/money/Money.java` is an immutable value object pairing a `BigDecimal`
with an ISO-4217 currency. Never `double`/`float`. Amounts are stored as
`NUMERIC(19,4)`; values with more precision than the currency allows are
**rejected**, not silently rounded, and cross-currency arithmetic throws.

### Idempotency
Write endpoints accept an `Idempotency-Key` header. `IdempotencyService` records
`SHA-256(scope | body)` against the key: a retry with the same key + body returns
the original resource id without re-running the action; the same key with a
different body is rejected. Ledger transactions additionally enforce
`externalRef` uniqueness as a second, natural anchor.

### Pluggable rails (ports & adapters)
The domain depends on interfaces, never a vendor SDK:
- `payment/provider/PaymentProvider` — card/bank charge + refund
- `banking/connector/BankConnector` — ACH/wire transfer + settlement

Sandbox adapters (`MockPaymentProvider`, `MockBankConnector`) are the default
beans via `@ConditionalOnProperty`; a real adapter drops in by implementing the
interface and setting `ledger.payment.provider=<name>` /
`ledger.bank.connector=<name>` — no ledger-core changes.

### Async settlement + returns
Bank transfers settle asynchronously. `BankingService.initiate` records the
ledger entry up front and marks the transfer `PENDING`. A settlement callback
(`POST /banking/webhooks`) drives it to `SETTLED`, or on a return, reverses the
ledger entry and marks it `RETURNED`. Callbacks are idempotent (a terminal
transfer ignores repeats).

---

## API surface

| Area | Endpoint |
|------|----------|
| Auth | `POST /auth/login` |
| Accounts | `POST /accounts` · `GET /accounts` · `GET /accounts/{id}` |
| Ledger | `POST /transactions` · `GET /transactions/{id}` · `POST /transactions/{id}/reversal` · `GET /accounts/{id}/balance` |
| Payments | `POST /payments` · `GET /payments/{id}` · `POST /payments/{id}/refund` |
| Reimbursements | `POST /reimbursements` · `POST /reimbursements/{id}/approve` · `.../reject` · `.../pay` · `GET /reimbursements/{id}` |
| Banking | `POST /bank-transfers` · `GET /bank-transfers/{id}` · `POST /banking/webhooks` |

All under `/api/v1`. See Swagger for schemas.

---

## Security

Stateless JWT (HS256), role-based: `ADMIN`/`SERVICE` may write, all three roles
may read. `401` when unauthenticated, `403` when authenticated but the role is
insufficient.

> The seeded in-memory users in `AuthService` are a **development stand-in**. In
> production, replace them with a real user store or IdP (e.g. AWS Cognito),
> source `LEDGER_JWT_SECRET` from a secret manager (AWS SSM/Secrets Manager), and
> verify a provider signature (HMAC) on `POST /banking/webhooks` instead of the
> service JWT.

---

## Testing

```bash
./mvnw test      # unit tests (no Docker): Money, posting-engine validation
./mvnw verify    # + Testcontainers integration tests (requires Docker)
```

Integration tests (`*IT`, run by Failsafe) spin up a real Postgres via
Testcontainers and cover the posting engine, concurrency (no lost updates),
idempotency, the payment/refund and reimbursement flows, bank settlement/returns,
and the security matrix. A shared singleton container is reused across classes.

> **Local note (Docker Desktop):** some Docker Desktop builds negotiate an old
> Docker API version with the Testcontainers Java client, which can fail
> container startup locally. CI (Linux Docker Engine) is unaffected. If you hit
> it locally, run the app against `docker compose` Postgres and exercise the API
> directly (the flows above), or run the unit tests with `./mvnw test`.

CI runs `./mvnw verify` on every push/PR — see [.github/workflows/ci.yml](.github/workflows/ci.yml).

---

## Configuration

| Env var | Default | Purpose |
|---------|---------|---------|
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | local Postgres | datasource |
| `LEDGER_JWT_SECRET` | dev secret | Base64 256-bit HS256 key — **override in prod** |
| `LEDGER_JWT_TTL` | `3600` | token TTL (seconds) |
| `ledger.payment.provider` | `mock` | active payment adapter |
| `ledger.bank.connector` | `mock` | active bank adapter |

Schema is Flyway-managed (`src/main/resources/db/migration`); Hibernate runs with
`ddl-auto=validate`, so the mappings and migrations must agree.

---

## Productionizing (next steps)

- **Users/secrets:** external IdP (Cognito), secrets via SSM/Secrets Manager.
- **AWS wiring for banking:** publish settlement events to **SQS** and consume
  them to drive transfer state; store/parse ACH **NACHA** files in **S3**. The
  `BankConnector` port is the seam — add a LocalStack profile in `docker-compose`
  and an AWS SDK v2 adapter without touching the ledger core.
- **Connection pool vs. contention:** posting is short and row-locked; size the
  Hikari pool to real write concurrency and add retry/backoff on lock timeouts.
- **Observability:** metrics/traces via Actuator + Prometheus; alert on unbalanced
  postings (should be impossible) and settlement backlogs.

See [docs/PLAN.md](docs/PLAN.md) for the design/build plan.
