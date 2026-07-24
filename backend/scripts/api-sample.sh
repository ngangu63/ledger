#!/usr/bin/env bash
#
# Sample end-to-end API walkthrough for the Ledger service.
#
# Exercises the full happy path against a running instance:
#   auth -> accounts -> double-entry transaction -> balances
#   -> payment -> reimbursement workflow -> bank transfer + settlement
#   -> idempotency replay.
#
# Prereqs: the app + Postgres are running (see README "Quick start"), plus
# `curl` and `jq` on PATH.
#
# Usage:
#   ./scripts/api-sample.sh
#   BASE_URL=http://localhost:8080 USERNAME=admin PASSWORD=admin123 ./scripts/api-sample.sh
#
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
API="${BASE_URL}/api/v1"
USERNAME="${USERNAME:-admin}"
PASSWORD="${PASSWORD:-admin123}"

# --- pretty output -----------------------------------------------------------
bold=$'\033[1m'; green=$'\033[32m'; red=$'\033[31m'; dim=$'\033[2m'; reset=$'\033[0m'
step() { printf '\n%s==> %s%s\n' "$bold" "$1" "$reset"; }
info() { printf '%s    %s%s\n' "$dim" "$1" "$reset"; }
ok()   { printf '%s    ✓ %s%s\n' "$green" "$1" "$reset"; }
die()  { printf '%s    ✗ %s%s\n' "$red" "$1" "$reset" >&2; exit 1; }

command -v jq >/dev/null 2>&1 || die "jq is required (brew install jq)"

# Unique suffix so externalRefs don't collide across re-runs (they are unique).
RUN="$$"
TOKEN=""

# authed request that fails loudly on a non-2xx status.
#   api METHOD PATH [json-body] [extra curl args...]
api() {
  local method="$1" path="$2" body="${3:-}"; shift || true; shift || true; shift || true
  local tmp status
  tmp="$(mktemp)"
  local -a args=(-sS -o "$tmp" -w '%{http_code}' -X "$method" "${API}${path}")
  [[ -n "$TOKEN" ]] && args+=(-H "Authorization: Bearer ${TOKEN}")
  if [[ -n "$body" ]]; then args+=(-H 'Content-Type: application/json' -d "$body"); fi
  args+=("$@")
  status="$(curl "${args[@]}")"
  if [[ "$status" -lt 200 || "$status" -ge 300 ]]; then
    printf '%s    ✗ %s %s -> HTTP %s%s\n' "$red" "$method" "$path" "$status" "$reset" >&2
    cat "$tmp" >&2; echo >&2; rm -f "$tmp"; exit 1
  fi
  cat "$tmp"; rm -f "$tmp"
}

# --- 0. health ---------------------------------------------------------------
step "Health check"
curl -sS "${BASE_URL}/actuator/health" | jq -e '.status == "UP"' >/dev/null \
  && ok "service is UP" || die "service not healthy at ${BASE_URL}"

# --- 1. auth -----------------------------------------------------------------
step "Login as '${USERNAME}'"
TOKEN="$(api POST /auth/login "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}" | jq -r .accessToken)"
[[ -n "$TOKEN" && "$TOKEN" != "null" ]] || die "no token returned"
ok "got JWT (${#TOKEN} chars)"

# --- 2. accounts -------------------------------------------------------------
step "Create accounts"
CASH_ID="$(api POST /accounts '{"name":"Cash","type":"ASSET","currency":"USD"}' | jq -r .id)"
ok "Cash (ASSET)      $CASH_ID"
REVENUE_ID="$(api POST /accounts '{"name":"Sales Revenue","type":"REVENUE","currency":"USD"}' | jq -r .id)"
ok "Sales (REVENUE)   $REVENUE_ID"
EXPENSE_ID="$(api POST /accounts '{"name":"Travel Expense","type":"EXPENSE","currency":"USD"}' | jq -r .id)"
ok "Travel (EXPENSE)  $EXPENSE_ID"
info "list: $(api GET /accounts | jq 'length') accounts total"

# --- 3. double-entry transaction ---------------------------------------------
step "Post a balanced transaction (debit Cash 100, credit Revenue 100)"
TX_BODY="$(cat <<JSON
{
  "currency": "USD",
  "description": "Sample sale",
  "externalRef": "sample-sale-${RUN}",
  "postings": [
    {"accountId": "${CASH_ID}",    "direction": "DEBIT",  "amount": "100.00"},
    {"accountId": "${REVENUE_ID}", "direction": "CREDIT", "amount": "100.00"}
  ]
}
JSON
)"
TX_ID="$(api POST /transactions "$TX_BODY" | jq -r .id)"
ok "transaction $TX_ID"
api GET "/accounts/${CASH_ID}/balance"    | jq -c '{account:"Cash",    signed:.signedBalance, natural:.naturalBalance, currency:.currency}' | sed 's/^/    /'
api GET "/accounts/${REVENUE_ID}/balance" | jq -c '{account:"Revenue", signed:.signedBalance, natural:.naturalBalance, currency:.currency}' | sed 's/^/    /'

# --- 4. idempotency replay ---------------------------------------------------
# Uses a fresh body with no externalRef (externalRef is globally unique), so the
# replay is driven purely by the Idempotency-Key.
step "Idempotency: replay a transaction with the same Idempotency-Key"
IDEMPO_BODY="$(cat <<JSON
{
  "currency": "USD",
  "description": "Idempotency demo",
  "postings": [
    {"accountId": "${CASH_ID}",    "direction": "DEBIT",  "amount": "5.00"},
    {"accountId": "${REVENUE_ID}", "direction": "CREDIT", "amount": "5.00"}
  ]
}
JSON
)"
KEY="sample-key-$$"
FIRST="$(api POST /transactions "$IDEMPO_BODY" -H "Idempotency-Key: ${KEY}" | jq -r .id)"
SECOND="$(api POST /transactions "$IDEMPO_BODY" -H "Idempotency-Key: ${KEY}" | jq -r .id)"
[[ "$FIRST" == "$SECOND" ]] \
  && ok "same key -> same transaction id ($FIRST)" \
  || die "idempotency broken: $FIRST != $SECOND"

# --- 5. payment --------------------------------------------------------------
step "Charge an inbound card payment (Cash <- Revenue, 42.50)"
PAY_BODY="$(cat <<JSON
{
  "direction": "INBOUND",
  "method": "CARD",
  "amount": "42.50",
  "currency": "USD",
  "sourceAccountId": "${REVENUE_ID}",
  "destinationAccountId": "${CASH_ID}",
  "instrumentToken": "tok_sample_visa",
  "externalRef": "sample-pay-${RUN}"
}
JSON
)"
PAY="$(api POST /payments "$PAY_BODY")"
PAY_ID="$(echo "$PAY" | jq -r .id)"
ok "payment $PAY_ID status=$(echo "$PAY" | jq -r .status)"

# --- 6. reimbursement workflow ----------------------------------------------
step "Reimbursement workflow: submit -> approve -> pay"
REIMB_BODY="$(cat <<JSON
{
  "requester": "alice@example.com",
  "amount": "25.00",
  "currency": "USD",
  "description": "Client lunch",
  "fundingAccountId": "${CASH_ID}",
  "payeeAccountId": "${EXPENSE_ID}"
}
JSON
)"
REIMB_ID="$(api POST /reimbursements "$REIMB_BODY" | jq -r .id)"
ok "submitted   $REIMB_ID"
api POST "/reimbursements/${REIMB_ID}/approve?approver=bob@example.com" >/dev/null && ok "approved"
PAID_STATUS="$(api POST "/reimbursements/${REIMB_ID}/pay" | jq -r .status)"
ok "paid        status=$PAID_STATUS"

# --- 7. bank transfer + settlement webhook -----------------------------------
step "Initiate an ACH transfer, then settle it via the webhook"
XFER_BODY="$(cat <<JSON
{
  "rail": "ACH",
  "amount": "500.00",
  "currency": "USD",
  "sourceAccountId": "${REVENUE_ID}",
  "destinationAccountId": "${CASH_ID}",
  "externalRef": "sample-ach-${RUN}"
}
JSON
)"
XFER="$(api POST /bank-transfers "$XFER_BODY")"
XFER_ID="$(echo "$XFER" | jq -r .id)"
PROVIDER_REF="$(echo "$XFER" | jq -r .providerRef)"
ok "transfer $XFER_ID status=$(echo "$XFER" | jq -r .status) providerRef=$PROVIDER_REF"

api POST /banking/webhooks "{\"providerRef\":\"${PROVIDER_REF}\",\"status\":\"SETTLED\"}" >/dev/null
ok "settlement callback accepted"
FINAL_STATUS="$(api GET "/bank-transfers/${XFER_ID}" | jq -r .status)"
ok "transfer final status=$FINAL_STATUS"

printf '\n%s%sAll sample API calls succeeded.%s\n' "$bold" "$green" "$reset"
