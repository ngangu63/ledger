import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { TopBar } from '../components/Layout'
import { Alert, ErrorState, Loading, StatusTag } from '../components/ui'
import { useAsync, errorMessage } from '../lib/useAsync'
import { useAuth } from '../auth/AuthContext'
import { useToast } from '../lib/ToastContext'
import { createAccount, listAccounts } from '../api/endpoints'
import { ACCOUNT_TYPES } from '../api/types'
import type { AccountType } from '../api/types'
import { shortId } from '../lib/format'

export default function Accounts() {
  const navigate = useNavigate()
  const { canWrite } = useAuth()
  const toast = useToast()
  const { data, loading, error, reload } = useAsync(listAccounts, [])
  const [showForm, setShowForm] = useState(false)
  const [search, setSearch] = useState('')

  const accounts = data ?? []
  const filtered = useMemo(() => {
    const q = search.toLowerCase()
    return (data ?? []).filter(
      (a) => a.name.toLowerCase().includes(q) || a.currency.toLowerCase().includes(q),
    )
  }, [data, search])
  const activeCount = accounts.filter((a) => a.status === 'ACTIVE').length
  const currencies = [...new Set(accounts.map((a) => a.currency))]

  return (
    <>
      <TopBar
        title="Accounts"
        subtitle="Chart of accounts · double-entry ledger"
        actions={
          canWrite && (
            <button className="btn" onClick={() => setShowForm((s) => !s)}>
              <span>＋</span> {showForm ? 'Close' : 'New Account'}
            </button>
          )
        }
      />
      <div className="content">
        <div className="col-main">
          <div className="stats">
            <div className="stat">
              <div className="k">Total Accounts</div>
              <div className="v">{accounts.length}</div>
            </div>
            <div className="stat">
              <div className="k">Active</div>
              <div className="v">
                {activeCount} <small>/ {accounts.length}</small>
              </div>
            </div>
            <div className="stat">
              <div className="k">Currencies</div>
              <div className="v" style={{ fontSize: 18 }}>
                {currencies.join(' · ') || '—'}
              </div>
            </div>
          </div>

          {showForm && <CreateAccountForm onCreated={() => { setShowForm(false); reload() }} />}

          <div className="card">
            <div className="head">
              <h2>All accounts</h2>
              <input
                className="search"
                placeholder="Search accounts…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
            {loading ? (
              <Loading />
            ) : error ? (
              <ErrorState message={error} />
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Type</th>
                    <th>Currency</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((a) => (
                    <tr key={a.id} className="clickable" onClick={() => navigate(`/accounts/${a.id}`)}>
                      <td>
                        <div className="name">{a.name}</div>
                        <div className="mono">{shortId(a.id)}</div>
                      </td>
                      <td>
                        <span className="tag type">{a.type}</span>
                      </td>
                      <td>{a.currency}</td>
                      <td>
                        <StatusTag status={a.status} />
                      </td>
                    </tr>
                  ))}
                  {filtered.length === 0 && (
                    <tr>
                      <td colSpan={4} style={{ textAlign: 'center', color: 'var(--muted)' }}>
                        No accounts found.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>
    </>
  )

  function CreateAccountForm({ onCreated }: { onCreated: () => void }) {
    const [name, setName] = useState('')
    const [type, setType] = useState<AccountType>('ASSET')
    const [currency, setCurrency] = useState('USD')
    const [busy, setBusy] = useState(false)
    const [err, setErr] = useState<string | null>(null)

    const currencyValid = /^[A-Z]{3}$/.test(currency)

    async function submit(e: React.FormEvent) {
      e.preventDefault()
      setBusy(true)
      setErr(null)
      try {
        const acct = await createAccount({ name, type, currency })
        toast.success(`Account "${acct.name}" created`)
        onCreated()
      } catch (e2) {
        setErr(errorMessage(e2))
      } finally {
        setBusy(false)
      }
    }

    return (
      <div className="card">
        <div className="head">
          <h2>New account</h2>
        </div>
        <form className="body" onSubmit={submit}>
          {err && <Alert kind="error">{err}</Alert>}
          <div className="form-row two">
            <div>
              <label>Name</label>
              <input value={name} onChange={(e) => setName(e.target.value)} maxLength={255} required />
            </div>
            <div>
              <label>Type</label>
              <select value={type} onChange={(e) => setType(e.target.value as AccountType)}>
                {ACCOUNT_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="form-row">
            <label>Currency</label>
            <input
              value={currency}
              onChange={(e) => setCurrency(e.target.value.toUpperCase())}
              maxLength={3}
            />
            <div className={`hint ${currency && !currencyValid ? 'err' : ''}`}>
              3-letter ISO-4217 code (^[A-Z]{'{3}'}$)
            </div>
          </div>
          <div className="form-actions">
            <button className="btn secondary" type="button" onClick={onCreated}>
              Cancel
            </button>
            <button className="btn" type="submit" disabled={busy || !name || !currencyValid}>
              {busy ? <span className="spinner" /> : 'Create Account'}
            </button>
          </div>
        </form>
      </div>
    )
  }
}
