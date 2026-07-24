/** Formatting helpers shared across views. */

/** Format a decimal-string amount with its currency, respecting ISO-4217 fraction digits. */
export function formatMoney(amount: string | number, currency: string): string {
  const value = typeof amount === 'string' ? Number(amount) : amount
  if (Number.isNaN(value)) return `${amount} ${currency}`
  try {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency,
    }).format(value)
  } catch {
    // Unknown currency code — fall back to plain number + code.
    return `${value.toLocaleString(undefined, { minimumFractionDigits: 2 })} ${currency}`
  }
}

/** Short, human-readable timestamp from an ISO instant. */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/** Truncate a UUID for compact display (a1f0…8c2d). */
export function shortId(id: string | null | undefined): string {
  if (!id) return '—'
  return id.length > 12 ? `${id.slice(0, 4)}…${id.slice(-4)}` : id
}
