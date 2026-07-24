import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { TopBar } from '../components/Layout'
import { Alert, ErrorState, Loading } from '../components/ui'
import AccountSelect from '../components/AccountSelect'
import { useAsync, errorMessage } from '../lib/useAsync'
import { useAuth } from '../auth/AuthContext'
import { useToast } from '../lib/ToastContext'
import { createTransaction, listAccounts } from '../api/endpoints'
import { newIdempotencyKey } from '../api/client'
import { POSTING_DIRECTIONS } from '../api/types'
import type { PostingDirection, PostingRequest } from '../api/types'

interface Row {
  accountId: string
  direction: PostingDirection
  amount: string
}

const emptyRow = (accountId = ''): Row => ({ accountId, direction: 'DEBIT', amount: '' })

export default function Transactions() {
  const navigate = useNavigate()
  const location = useLocation()
  const presetAccountId = (location.state as { presetAccountId?: string } | null)?.presetAccountId
  const { canWrite } = useAuth()
  const toast = useToast()
  const { data: accounts, loading, error } = useAsync(listAccounts, [])

  const [currency, setCurrency] = useState('USD')
  const [externalRef, setExternalRef] = useState('')
  const [description, setDescription] = useState('')
  const [rows, setRows] = useState<Row[]>([emptyRow(presetAccountId), emptyRow()])
  const [lookupId, setLookupId] = useState('')
  const [busy, setBusy] = useState(false)
  const [formErr, setFormErr] = useState<string | null>(null)

  const sum = (dir: PostingDirection) =>
    rows
      .filter((r) => r.direction === dir)
      .reduce((acc, r) => acc + (Number(r.amount) || 0), 0)
  const debits = sum('DEBIT')
  const credits = sum('CREDIT')
  const balanced = debits > 0 && Math.abs(debits - credits) < 0.00005
  const currencyValid = /^[A-Z]{3}$/.test(currency)
  const allRowsValid = rows.every((r) => r.accountId && Number(r.amount) >= 0.0001)
  const canSubmit = canWrite && currencyValid && rows.length >= 2 && allRowsValid && balanced

  function setRow(i: number, patch: Partial<Row>) {
    setRows((rs) => rs.map((r, idx) => (idx === i ? { ...r, ...patch } : r)))
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setBusy(true)
    setFormErr(null)
    try {
      const postings: PostingRequest[] = rows.map((r) => ({
        accountId: r.accountId,
        direction: r.direction,
        amount: r.amount,
      }))
      const txn = await createTransaction(
        {
          currency,
          externalRef: externalRef || undefined,
          description: description || undefined,
          postings,
        },
        newIdempotencyKey(),
      )
      toast.success('Transaction posted')
      navigate(`/transactions/${txn.id}`)
    } catch (err) {
      setFormErr(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <TopBar title="New Transaction" subtitle="Balanced double-entry posting" />
      <div className="content">
        <div className="col-main">
          {loading ? (
            <Loading />
          ) : error ? (
            <ErrorState message={error} />
          ) : (
            <div className="card">
              <div className="head">
                <h2>Transaction details</h2>
              </div>
              <form className="body" onSubmit={submit}>
                {!canWrite && (
                  <Alert kind="error">Your role is read-only — posting is disabled.</Alert>
                )}
                {formErr && <Alert kind="error">{formErr}</Alert>}

                <div className="form-row two">
                  <div>
                    <label>Currency</label>
                    <input
                      value={currency}
                      onChange={(e) => setCurrency(e.target.value.toUpperCase())}
                      maxLength={3}
                    />
                    <div className={`hint ${currency && !currencyValid ? 'err' : ''}`}>
                      3-letter ISO-4217 (^[A-Z]{'{3}'}$)
                    </div>
                  </div>
                  <div>
                    <label>External Ref (optional)</label>
                    <input
                      value={externalRef}
                      onChange={(e) => setExternalRef(e.target.value)}
                      maxLength={255}
                    />
                    <div className="hint">Must be globally unique</div>
                  </div>
                </div>
                <div className="form-row">
                  <label>Description (optional)</label>
                  <input
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    maxLength={1024}
                  />
                </div>

                <label>Postings · minimum 2</label>
                <table className="postings">
                  <thead>
                    <tr>
                      <th style={{ width: '50%' }}>Account</th>
                      <th>Direction</th>
                      <th className="right">Amount</th>
                      <th />
                    </tr>
                  </thead>
                  <tbody>
                    {rows.map((r, i) => (
                      <tr key={i}>
                        <td>
                          <AccountSelect
                            accounts={accounts ?? []}
                            value={r.accountId}
                            onChange={(id) => setRow(i, { accountId: id })}
                          />
                        </td>
                        <td>
                          <select
                            value={r.direction}
                            onChange={(e) =>
                              setRow(i, { direction: e.target.value as PostingDirection })
                            }
                          >
                            {POSTING_DIRECTIONS.map((d) => (
                              <option key={d} value={d}>
                                {d}
                              </option>
                            ))}
                          </select>
                        </td>
                        <td>
                          <input
                            className="right"
                            inputMode="decimal"
                            placeholder="0.0000"
                            value={r.amount}
                            onChange={(e) => setRow(i, { amount: e.target.value })}
                          />
                        </td>
                        <td>
                          <button
                            type="button"
                            className="icon-btn"
                            disabled={rows.length <= 2}
                            onClick={() => setRows((rs) => rs.filter((_, idx) => idx !== i))}
                          >
                            ✕
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <button
                  type="button"
                  className="btn ghost"
                  style={{ marginTop: 12 }}
                  onClick={() => setRows((rs) => [...rs, emptyRow()])}
                >
                  ＋ Add posting
                </button>

                <div className={`balance-strip ${balanced ? 'ok' : 'bad'}`}>
                  <span>
                    {balanced
                      ? '✓ Balanced — debits equal credits'
                      : '✗ Debits must equal credits (and be non-zero)'}
                  </span>
                  <span className="figs">
                    DR {debits.toFixed(4)} = CR {credits.toFixed(4)}
                  </span>
                </div>

                <div className="form-actions">
                  <span className="badge-note" style={{ marginRight: 'auto' }}>
                    🔑 Idempotency-Key auto-generated
                  </span>
                  <button
                    type="button"
                    className="btn secondary"
                    onClick={() => setRows([emptyRow(), emptyRow()])}
                  >
                    Reset
                  </button>
                  <button className="btn" type="submit" disabled={!canSubmit || busy}>
                    {busy ? <span className="spinner" /> : 'Post Transaction'}
                  </button>
                </div>
              </form>
            </div>
          )}
        </div>

        <aside className="panel">
          <h3>Look up transaction</h3>
          <div className="sub">Transactions have no list endpoint — fetch one by its ID.</div>
          <div className="form-row">
            <label>Transaction ID</label>
            <input value={lookupId} onChange={(e) => setLookupId(e.target.value)} placeholder="UUID" />
          </div>
          <button
            className="btn secondary"
            style={{ width: '100%', justifyContent: 'center' }}
            disabled={!lookupId}
            onClick={() => navigate(`/transactions/${lookupId.trim()}`)}
          >
            Open transaction
          </button>
        </aside>
      </div>
    </>
  )
}
