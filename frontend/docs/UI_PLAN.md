# Ledger React Frontend — UI Plan

Frontend for the Spring Boot double-entry **Ledger** backend
(`/Users/bwamutalamiantezila/Documents/ANY/ClaudeCode/Ledger`).

- **Base URL:** `http://localhost:8080/api/v1`
- **Auth:** JWT bearer (HS256). Login returns `accessToken` + `roles`.
- **Roles:** `ADMIN`/`SERVICE` can write (POST); `VIEWER` is read-only.
- **Transport:** REST only — no WebSocket/SSE. Async flows (bank transfers) are polled.
- **CORS:** none configured on the backend → use a Vite dev proxy.

---

## Design mockups

Static visual mockups for each view live in [`docs/mockups/`](mockups/). Each view has
its **own HTML file** (individually editable) plus a rendered **JPEG** preview; all views
share [`mockups/theme.css`](mockups/theme.css) — edit that to restyle every view at once,
or edit a single `.html` to change just one view.

To re-render after editing (macOS, headless Chrome + `sips`):

```sh
cd docs/mockups
CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
for f in login dashboard accounts transactions payments bank-transfers reimbursements; do
  "$CHROME" --headless --disable-gpu --hide-scrollbars \
    --screenshot="$f.png" --window-size=1280,820 --default-background-color=FFFFFFFF \
    "file://$(pwd)/$f.html" 2>/dev/null
  sips -s format jpeg -s formatOptions 92 "$f.png" --out "$f.jpg" && rm -f "$f.png"
done
```

| View | Source | Preview |
| --- | --- | --- |
| Login | [login.html](mockups/login.html) | [login.jpg](mockups/login.jpg) |
| Dashboard | [dashboard.html](mockups/dashboard.html) | [dashboard.jpg](mockups/dashboard.jpg) |
| Accounts | [accounts.html](mockups/accounts.html) | [accounts.jpg](mockups/accounts.jpg) |
| Transactions | [transactions.html](mockups/transactions.html) | [transactions.jpg](mockups/transactions.jpg) |
| Payments | [payments.html](mockups/payments.html) | [payments.jpg](mockups/payments.jpg) |
| Bank Transfers | [bank-transfers.html](mockups/bank-transfers.html) | [bank-transfers.jpg](mockups/bank-transfers.jpg) |
| Reimbursements | [reimbursements.html](mockups/reimbursements.html) | [reimbursements.jpg](mockups/reimbursements.jpg) |

---

## 0. Foundation (cross-cutting, build first)
- **API client** (`src/api/client.ts`): base-URL wrapper, auto-attach
  `Authorization: Bearer <token>`, centralized parsing of the `ApiError` shape
  (`code`, `message`, `violations[]`) into user-facing messages.
- **Dev proxy**: Vite proxy `/api → http://localhost:8080` in `vite.config.ts`
  (backend has no CORS).
- **Auth context** (`src/auth/`): store `accessToken` + `roles`, expiry handling,
  `useRole()` helper. Gate all write UI on `ADMIN`/`SERVICE`.
- **Routing + layout**: React Router, protected-route wrapper, sidebar nav,
  shared money/currency formatter respecting ISO-4217 fraction digits.

## 1. Login window
> Mockup: [login.jpg](mockups/login.jpg) · source [login.html](mockups/login.html)
- Username/password → `POST /auth/login`. Store token + roles, redirect to Dashboard.
- Seeded dev users: `admin/admin123`, `service/service123`, `viewer/viewer123`.

## 2. Accounts
> Mockup: [accounts.jpg](mockups/accounts.jpg) · source [accounts.html](mockups/accounts.html)
- **List** — `GET /accounts` (name, type, currency, status).
- **Detail** — account fields + balance `GET /accounts/{id}/balance` (show `naturalBalance`).
- **Create** (write) — name, `type` (ASSET/LIABILITY/EQUITY/REVENUE/EXPENSE),
  3-letter currency (`^[A-Z]{3}$`).

## 3. Transactions (double-entry)
> Mockup: [transactions.jpg](mockups/transactions.jpg) · source [transactions.html](mockups/transactions.html)
- **Create** (write) — currency, optional externalRef/description, dynamic postings
  editor (≥2 rows: account, DEBIT/CREDIT, amount). Live running balance must net to
  zero before submit. Optional `Idempotency-Key`.
- **Detail** — `GET /transactions/{id}` header + postings table.
- **Reverse** (write) — `POST /transactions/{id}/reversal`; disabled if already `REVERSED`.

## 4. Payments
> Mockup: [payments.jpg](mockups/payments.jpg) · source [payments.html](mockups/payments.html)
- **Create/Charge** (write) — direction (INBOUND/OUTBOUND), method (CARD/BANK),
  amount, currency, source/destination accounts.
- **Detail** — status, `failureReason`, linked `transactionId`.
- **Refund** (write) — enabled only for `CAPTURED`/`SETTLED`.

## 5. Bank Transfers
> Mockup: [bank-transfers.jpg](mockups/bank-transfers.jpg) · source [bank-transfers.html](mockups/bank-transfers.html)
- **Initiate** (write) — rail (ACH/WIRE), amount, currency, source/destination.
- **Detail** — status with polling `GET /bank-transfers/{id}`
  (PENDING → SETTLED/RETURNED; async, no realtime channel).

## 6. Reimbursements
> Mockup: [reimbursements.jpg](mockups/reimbursements.jpg) · source [reimbursements.html](mockups/reimbursements.html)
- **Submit** (write) — requester, amount, currency, funding + payee accounts.
- **Detail + state-machine actions** (write) — Approve/Reject (require `approver`
  query param) from SUBMITTED; Pay from APPROVED. Disable buttons per current status.

## 7. Dashboard (optional landing)
> Mockup: [dashboard.jpg](mockups/dashboard.jpg) · source [dashboard.html](mockups/dashboard.html)
- Health tile (`GET /actuator/health`), quick account balances, activity shortcuts.

---

## Cross-cutting concerns
- **Validation** mirrored client-side: currency regex, amount ≥ 0.0001 at the
  currency's natural scale, transaction ≥2 balanced postings. Always surface backend
  `violations[]`.
- **Idempotency**: auto-generate a UUID `Idempotency-Key` per create submission.
- **Error handling**: toast/banner from `ApiError.code`
  (e.g. `CURRENCY_MISMATCH`, `ACCOUNT_NOT_POSTABLE`, `CONCURRENT_MODIFICATION` → retry).
- **Role awareness**: hide/disable write controls for VIEWER.

## Build order
1. Foundation (client, proxy, auth, routing)
2. Login
3. Accounts (needed as pickers everywhere)
4. Transactions
5. Payments
6. Bank Transfers
7. Reimbursements
8. Dashboard polish
