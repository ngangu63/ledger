# Ledger — Financial Ledger & Payment Infrastructure: Completion Plan

## Context

The goal is a Spring Boot reference implementation for money-movement infrastructure
(double-entry ledger, payments, card/bank rails, reimbursements, banking integrations)
that demonstrates proven ledger/payment patterns and sound architectural decisions.

A substantial, high-quality core **already exists** at
`/Users/bwamutalamiantezila/Documents/ANY/ClaudeCode/Ledger` and **compiles cleanly**
(`mvn compile` passes). Stack: Java 17, Spring Boot 3.3.5, Postgres 16, Flyway, Maven,
Docker. So this is a **completion** effort, not a greenfield build.

### What already exists (verified)
- **Ledger core** — `ledger/` double-entry engine: `LedgerService.post()` validates
  balanced, single-currency transactions; append-only postings; balance checkpoints
  updated under pessimistic lock in deterministic id-order (deadlock-safe); reversals by
  offsetting entry.
- **Money value object** — `common/money/Money.java` (BigDecimal + ISO-4217, no floats,
  `RoundingMode.UNNECESSARY`, currency-mismatch guard).
- **Payments** — `payment/` orchestration: charge external rail → record balanced ledger
  entry on capture; pluggable `PaymentProvider` + `MockPaymentProvider`.
- **Reimbursements** — `reimbursement/` full service workflow (submit/approve/reject/pay).
- **Idempotency** — `common/idempotency/` `Idempotency-Key` header on POST /transactions
  and POST /payments (SHA-256 of scope|body); plus `externalRef` uniqueness anchor.
- **Security** — JWT (jjwt), stateless, role-based (ADMIN/SERVICE/VIEWER); in-memory
  seeded users in `AuthService` (explicit dev stand-in).
- **Cross-cutting** — Flyway `V1__init_ledger_core.sql`, global exception handler,
  OpenAPI/Swagger, Actuator, `docker-compose.yml` (postgres + pgadmin), `Dockerfile`.

### Gaps this plan closes
1. **Zero automated tests** — unacceptable for a money system.
2. **Reimbursement is unreachable** — service exists but no `ReimbursementController`.
3. **No refund flow** — `PaymentProvider.refund()` implemented but never called.
4. **No banking-integration package** — the role explicitly names banking integrations.
5. **No README / CI / Maven wrapper.**
6. **Reversal endpoint** is not idempotency-wrapped.

---

## Plan

### Step 0 — Persist this plan in the repo
Copy this plan to `docs/PLAN.md` so it lives with the project for future review.

### Step 1 — Test suite (highest priority)
Add `src/test/java/...` mirroring the main package layout. Testcontainers + Postgres deps
already in `pom.xml`.
- **Unit (no Spring):**
  - `MoneyTest` — add/subtract/negate, currency-mismatch rejection, scale rounding,
    equality by value.
  - `LedgerServiceTest` (Mockito) — rejects unbalanced tx, multi-currency tx, zero-effect
    tx, non-postable account; reversal flips directions and marks REVERSED.
- **Integration (Testcontainers `@SpringBootTest`, real Postgres, Flyway):**
  - `LedgerPostingIT` — post balanced tx; balances reflect debit/credit; `externalRef`
    reuse rejected; reversal restores balances.
  - `IdempotencyIT` — same `Idempotency-Key` + same body returns same resource; same key +
    different body → `IDEMPOTENCY_KEY_REUSED`.
  - `ConcurrencyIT` — N concurrent postings to one account never lose an update (exercises
    the pessimistic-lock path in `LedgerService.applyToBalances`).
  - `PaymentFlowIT` — capture records a ledger entry; `instrumentToken="decline"` marks
    payment FAILED and writes no ledger entry.
  - `AuthIT` / `SecurityIT` — login issues JWT; write endpoint 401 without token,
    403 for VIEWER, 200 for SERVICE/ADMIN.
- Add a shared `AbstractPostgresIT` base (singleton container pattern) to avoid per-class
  container startup cost.

### Step 2 — Finish reimbursement + refund
- Add `ReimbursementController` (`/api/v1/reimbursements`): `POST` (submit),
  `POST /{id}/approve`, `POST /{id}/reject`, `POST /{id}/pay`, `GET /{id}` — mirror the
  patterns in `PaymentController`/`AccountController`; map to existing
  `ReimbursementResponse`. Reuse `GlobalExceptionHandler`.
- Wire **refund**: add `PaymentService.refund(paymentId)` that calls
  `paymentProvider.refund(providerRef)` and posts the **reversing** ledger entry (reuse
  `LedgerService.reverse` on the payment's `ledgerTransactionId`), guarded by payment
  status. Expose `POST /api/v1/payments/{id}/refund`. Add integration test.
- Wrap the **reversal** endpoint in `IdempotencyService.execute` (scope
  `ledger.transaction.reverse`) for parity with the other write endpoints.

### Step 3 — Banking integration layer (in-process mock; LocalStack deferred)
New package `banking/`:
- `BankConnector` interface — `initiateTransfer(BankTransferCommand)` (ACH/wire),
  `getTransferStatus(ref)`; models async settlement (returns `PENDING`, later `SETTLED`/
  `RETURNED`). Mirrors the `PaymentProvider` abstraction style.
- `MockBankConnector` (`@ConditionalOnProperty ledger.bank.connector=mock`,
  `matchIfMissing=true`) — deterministic settlement, a `"return"` trigger token for the
  returned-payment path.
- `BankWebhookController` (`POST /api/v1/banking/webhooks`) — receives settlement
  callbacks; on RETURN, posts an offsetting ledger entry (reuse `LedgerService.reverse`).
  Flyway `V2__banking.sql` for a `bank_transfer` table (status, provider_ref, settled_at).
- Document intended AWS wiring (SQS for async settlement events, S3 for ACH NACHA files)
  in the README as the next phase; **no AWS SDK/LocalStack added now** to keep the
  reference clean. (Optional follow-up: LocalStack in docker-compose + AWS SDK v2.)

### Step 4 — Docs, CI, Maven wrapper
- **`README.md`** — architecture overview, ledger/payment patterns explained (double-entry
  invariants, idempotency strategy, money handling, reversal-not-mutation), run
  instructions (`docker compose up -d`, `./mvnw spring-boot:run`), API tour + Swagger URL,
  security notes, and the "productionize this" checklist (replace in-memory users with
  Cognito/IdP, secrets via env/SSM, connection-pool sizing vs. serializable postings).
- **Maven wrapper** — `mvnw`/`mvnw.cmd`/`.mvn/wrapper` via `mvn wrapper:wrapper` (the
  `.gitignore` already anticipates it).
- **CI** — `.github/workflows/ci.yml`: JDK 17 (Temurin), cache Maven, `./mvnw verify`
  (Testcontainers uses the runner's Docker).

---

## Design notes / decisions
- **Defaults chosen** (user declined the scope prompt): include all four workstreams;
  banking = in-process mock connectors, LocalStack/AWS SDK deferred to a documented
  follow-up phase.
- **Reuse over rebuild:** refunds and bank-returns both go through the existing
  `LedgerService.reverse` rather than new posting logic. Controllers copy existing
  controller patterns and the shared `GlobalExceptionHandler`.
- Keep `MockPaymentProvider`'s `@ConditionalOnProperty` pattern for `MockBankConnector` so
  real providers can drop in without code change.

## Verification
1. `./mvnw clean verify` — unit + Testcontainers integration tests green (requires Docker).
2. `docker compose up -d postgres` then `./mvnw spring-boot:run`; smoke test via Swagger
   (`http://localhost:8080/swagger-ui.html`):
   - `POST /api/v1/auth/login` (`admin/admin123`) → JWT.
   - Create two accounts → `POST /api/v1/transactions` (balanced) → `GET .../balance`
     reflects it → `POST .../reversal` restores balances.
   - `POST /api/v1/payments` with `instrumentToken="decline"` → payment FAILED, no ledger
     entry; then a successful charge → refund → offsetting entry.
   - Reimbursement submit → approve → pay → linked payment + ledger entry.
   - Repeat a POST with the same `Idempotency-Key` → identical resource id.
3. `docker build .` succeeds.
```
