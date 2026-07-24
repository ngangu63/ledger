import type { ApiError } from './types'

const BASE = '/api/v1'

const TOKEN_KEY = 'ledger.token'
const ROLES_KEY = 'ledger.roles'
const USER_KEY = 'ledger.user'

export const tokenStore = {
  get token() {
    return localStorage.getItem(TOKEN_KEY)
  },
  get roles(): string[] {
    const raw = localStorage.getItem(ROLES_KEY)
    return raw ? (JSON.parse(raw) as string[]) : []
  },
  get username() {
    return localStorage.getItem(USER_KEY)
  },
  set(token: string, roles: string[], username: string) {
    localStorage.setItem(TOKEN_KEY, token)
    localStorage.setItem(ROLES_KEY, JSON.stringify(roles))
    localStorage.setItem(USER_KEY, username)
  },
  clear() {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(ROLES_KEY)
    localStorage.removeItem(USER_KEY)
  },
}

/** Error thrown by the API client; carries the parsed backend ApiError when present. */
export class ApiClientError extends Error {
  status: number
  code: string
  apiError?: ApiError

  constructor(status: number, message: string, code = 'ERROR', apiError?: ApiError) {
    super(message)
    this.name = 'ApiClientError'
    this.status = status
    this.code = code
    this.apiError = apiError
  }

  /** Human-readable message that includes field violations when present. */
  get detail(): string {
    if (this.apiError?.violations?.length) {
      return this.apiError.violations.map((v) => `${v.field}: ${v.message}`).join('\n')
    }
    return this.message
  }
}

export interface RequestOptions {
  method?: string
  body?: unknown
  /** Sent as the Idempotency-Key header on write requests. */
  idempotencyKey?: string
  /** Query string params. */
  query?: Record<string, string | undefined>
  /** Skip auth header (used for login). */
  anonymous?: boolean
}

function buildUrl(path: string, query?: RequestOptions['query']): string {
  const url = BASE + path
  if (!query) return url
  const usp = new URLSearchParams()
  for (const [k, v] of Object.entries(query)) {
    if (v !== undefined && v !== '') usp.append(k, v)
  }
  const qs = usp.toString()
  return qs ? `${url}?${qs}` : url
}

export async function request<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }

  if (opts.body !== undefined) headers['Content-Type'] = 'application/json'
  if (opts.idempotencyKey) headers['Idempotency-Key'] = opts.idempotencyKey
  if (!opts.anonymous && tokenStore.token) {
    headers['Authorization'] = `Bearer ${tokenStore.token}`
  }

  let res: Response
  try {
    res = await fetch(buildUrl(path, opts.query), {
      method: opts.method ?? 'GET',
      headers,
      body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
    })
  } catch {
    throw new ApiClientError(0, 'Network error — is the backend running on :8080?', 'NETWORK')
  }

  // 401 → session invalid; clear so the app redirects to login.
  if (res.status === 401 && !opts.anonymous) {
    tokenStore.clear()
  }

  if (!res.ok) {
    let apiError: ApiError | undefined
    try {
      apiError = (await res.json()) as ApiError
    } catch {
      /* non-JSON error body */
    }
    throw new ApiClientError(
      res.status,
      apiError?.message ?? res.statusText ?? 'Request failed',
      apiError?.code ?? `HTTP_${res.status}`,
      apiError,
    )
  }

  if (res.status === 204 || res.status === 202) return undefined as T
  const text = await res.text()
  return (text ? JSON.parse(text) : undefined) as T
}

/** RFC-4122 v4 UUID for idempotency keys (crypto.randomUUID is available in modern browsers). */
export function newIdempotencyKey(): string {
  return crypto.randomUUID()
}
