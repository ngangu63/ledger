import { request } from './client'
import type {
  AccountResponse,
  BalanceResponse,
  BankTransferResponse,
  CreateAccountRequest,
  CreatePaymentRequest,
  CreateTransactionRequest,
  DepositRequest,
  WithdrawRequest,
  InitiateTransferRequest,
  LoginRequest,
  PaymentResponse,
  ReimbursementResponse,
  SubmitReimbursementRequest,
  TokenResponse,
  TransactionResponse,
} from './types'

// ---- Auth ----
export const login = (body: LoginRequest) =>
  request<TokenResponse>('/auth/login', { method: 'POST', body, anonymous: true })

// ---- Accounts ----
export const listAccounts = () => request<AccountResponse[]>('/accounts')
export const getAccount = (id: string) => request<AccountResponse>(`/accounts/${id}`)
export const createAccount = (body: CreateAccountRequest) =>
  request<AccountResponse>('/accounts', { method: 'POST', body })
export const getBalance = (accountId: string) =>
  request<BalanceResponse>(`/accounts/${accountId}/balance`)
export const depositCash = (accountId: string, body: DepositRequest, idempotencyKey?: string) =>
  request<TransactionResponse>(`/accounts/${accountId}/deposit`, {
    method: 'POST',
    body,
    idempotencyKey,
  })
export const withdrawCash = (accountId: string, body: WithdrawRequest, idempotencyKey?: string) =>
  request<TransactionResponse>(`/accounts/${accountId}/withdraw`, {
    method: 'POST',
    body,
    idempotencyKey,
  })

// ---- Transactions ----
export const getTransaction = (id: string) => request<TransactionResponse>(`/transactions/${id}`)
export const createTransaction = (body: CreateTransactionRequest, idempotencyKey?: string) =>
  request<TransactionResponse>('/transactions', { method: 'POST', body, idempotencyKey })
export const reverseTransaction = (id: string, description?: string, idempotencyKey?: string) =>
  request<TransactionResponse>(`/transactions/${id}/reversal`, {
    method: 'POST',
    query: { description },
    idempotencyKey,
  })

// ---- Payments ----
export const getPayment = (id: string) => request<PaymentResponse>(`/payments/${id}`)
export const createPayment = (body: CreatePaymentRequest, idempotencyKey?: string) =>
  request<PaymentResponse>('/payments', { method: 'POST', body, idempotencyKey })
export const refundPayment = (id: string, idempotencyKey?: string) =>
  request<PaymentResponse>(`/payments/${id}/refund`, { method: 'POST', idempotencyKey })

// ---- Bank Transfers ----
export const getBankTransfer = (id: string) => request<BankTransferResponse>(`/bank-transfers/${id}`)
export const initiateTransfer = (body: InitiateTransferRequest, idempotencyKey?: string) =>
  request<BankTransferResponse>('/bank-transfers', { method: 'POST', body, idempotencyKey })

// ---- Reimbursements ----
export const getReimbursement = (id: string) =>
  request<ReimbursementResponse>(`/reimbursements/${id}`)
export const submitReimbursement = (body: SubmitReimbursementRequest) =>
  request<ReimbursementResponse>('/reimbursements', { method: 'POST', body })
export const approveReimbursement = (id: string, approver: string) =>
  request<ReimbursementResponse>(`/reimbursements/${id}/approve`, {
    method: 'POST',
    query: { approver },
  })
export const rejectReimbursement = (id: string, approver: string) =>
  request<ReimbursementResponse>(`/reimbursements/${id}/reject`, {
    method: 'POST',
    query: { approver },
  })
export const payReimbursement = (id: string) =>
  request<ReimbursementResponse>(`/reimbursements/${id}/pay`, { method: 'POST' })

// ---- Health ----
// Note: /actuator is NOT under /api/v1, so call it directly (proxied in vite.config.ts).
export interface HealthResponse {
  status: string
}
export const getHealth = async (): Promise<HealthResponse> => {
  const res = await fetch('/actuator/health', { headers: { Accept: 'application/json' } })
  if (!res.ok) throw new Error(`Health check failed: ${res.status}`)
  return (await res.json()) as HealthResponse
}
