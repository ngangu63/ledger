// Domain types mirroring the Ledger Spring Boot backend (/api/v1).

// ---- Enums ----
export type AccountType = 'ASSET' | 'EXPENSE' | 'LIABILITY' | 'EQUITY' | 'REVENUE'
export type AccountStatus = 'ACTIVE' | 'FROZEN' | 'CLOSED'
export type TransactionStatus = 'POSTED' | 'REVERSED'
export type PostingDirection = 'DEBIT' | 'CREDIT'
export type PaymentStatus =
  | 'PENDING'
  | 'AUTHORIZED'
  | 'CAPTURED'
  | 'SETTLED'
  | 'FAILED'
  | 'REVERSED'
export type PaymentMethod = 'CARD' | 'BANK'
export type PaymentDirection = 'INBOUND' | 'OUTBOUND'
export type BankTransferStatus = 'PENDING' | 'SETTLED' | 'RETURNED'
export type BankRail = 'ACH' | 'WIRE'
export type ReimbursementStatus = 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'PAID'

export const ACCOUNT_TYPES: AccountType[] = ['ASSET', 'EXPENSE', 'LIABILITY', 'EQUITY', 'REVENUE']
export const PAYMENT_DIRECTIONS: PaymentDirection[] = ['INBOUND', 'OUTBOUND']
export const PAYMENT_METHODS: PaymentMethod[] = ['CARD', 'BANK']
export const BANK_RAILS: BankRail[] = ['ACH', 'WIRE']
export const POSTING_DIRECTIONS: PostingDirection[] = ['DEBIT', 'CREDIT']

// ---- Auth ----
export interface LoginRequest {
  username: string
  password: string
}
export interface TokenResponse {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
  roles: string[]
}

// ---- Accounts ----
export interface CreateAccountRequest {
  name: string
  type: AccountType
  currency: string
}
export interface AccountResponse {
  id: string
  name: string
  type: AccountType
  currency: string
  status: AccountStatus
  createdAt: string
}
export interface BalanceResponse {
  accountId: string
  currency: string
  signedBalance: string
  naturalBalance: string
}
export interface DepositRequest {
  amount: string
  description?: string
}
export interface WithdrawRequest {
  amount: string
  description?: string
}

// ---- Transactions ----
export interface PostingRequest {
  accountId: string
  direction: PostingDirection
  amount: string
}
export interface CreateTransactionRequest {
  currency: string
  externalRef?: string
  description?: string
  postings: PostingRequest[]
}
export interface PostingResponse {
  id: string
  accountId: string
  direction: PostingDirection
  amount: string
  currency: string
}
export interface TransactionResponse {
  id: string
  externalRef: string | null
  description: string | null
  status: TransactionStatus
  reversalOfId: string | null
  createdAt: string
  postings: PostingResponse[]
}

// ---- Payments ----
export interface CreatePaymentRequest {
  direction: PaymentDirection
  method: PaymentMethod
  amount: string
  currency: string
  sourceAccountId: string
  destinationAccountId: string
  instrumentToken?: string
  externalRef?: string
}
export interface PaymentResponse {
  id: string
  direction: PaymentDirection
  method: PaymentMethod
  status: PaymentStatus
  amount: string
  currency: string
  sourceAccountId: string
  destinationAccountId: string
  provider: string | null
  providerRef: string | null
  externalRef: string | null
  transactionId: string | null
  failureReason: string | null
  createdAt: string
}

// ---- Bank Transfers ----
export interface InitiateTransferRequest {
  rail: BankRail
  amount: string
  currency: string
  sourceAccountId: string
  destinationAccountId: string
  externalRef?: string
}
export interface BankTransferResponse {
  id: string
  rail: BankRail
  amount: string
  currency: string
  sourceAccountId: string
  destinationAccountId: string
  status: BankTransferStatus
  provider: string | null
  providerRef: string | null
  externalRef: string | null
  transactionId: string | null
  settledAt: string | null
  createdAt: string
}

// ---- Reimbursements ----
export interface SubmitReimbursementRequest {
  requester: string
  amount: string
  currency: string
  description?: string
  fundingAccountId: string
  payeeAccountId: string
}
export interface ReimbursementResponse {
  id: string
  requester: string
  amount: string
  currency: string
  description: string | null
  status: ReimbursementStatus
  fundingAccountId: string
  payeeAccountId: string
  decidedBy: string | null
  paymentId: string | null
  createdAt: string
}

// ---- Errors ----
export interface ApiViolation {
  field: string
  message: string
}
export interface ApiError {
  timestamp: string
  status: number
  code: string
  message: string
  path: string
  violations?: ApiViolation[]
}
