import type { ReactNode } from 'react'

export function Loading({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="state">
      <span className="spinner" /> {label}
    </div>
  )
}

export function ErrorState({ message }: { message: string }) {
  return <div className="state error">{message}</div>
}

export function Empty({ message }: { message: string }) {
  return <div className="state">{message}</div>
}

/** Status pill; the lowercased status becomes the CSS modifier class. */
export function StatusTag({ status }: { status: string }) {
  return <span className={`tag ${status.toLowerCase()}`}>{status}</span>
}

export function KeyVal({ k, children }: { k: string; children: ReactNode }) {
  return (
    <div className="kv">
      <span className="k">{k}</span>
      <span className="val">{children}</span>
    </div>
  )
}

export function Alert({ kind, children }: { kind: 'error' | 'success'; children: ReactNode }) {
  return <div className={`alert ${kind}`}>{children}</div>
}
