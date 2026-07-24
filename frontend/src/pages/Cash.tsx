import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { TopBar } from '../components/Layout'
import { Alert, ErrorState, Loading } from '../components/ui'
import AccountSelect from '../components/AccountSelect'
import { useAsync, errorMessage } from '../lib/useAsync'
import { useAuth } from '../auth/AuthContext'
import { useToast } from '../lib/ToastContext'
import { depositCash, listAccounts, withdrawCash } from '../api/endpoints'
import { newIdempotencyKey } from '../api/client'

type Mode = 'DEPOSIT' | 'WITHDRAW'

export default function Cash() {
  const navigate = useNavigate()
  const location = useLocation()
  const presetState = location.state as { presetAccountId?: string; mode?: Mode } | null
  const { canWrite } = useAuth()
  const toast = useToast()
  const { data: accounts, loading, error } = useAsync(listAccounts, [])

  const [mode, setMode] = useState<Mode>(presetState?.mode ?? 'DEPOSIT')
  const [accountId, setAccountId] = useState(presetState?.presetAccountId ?? '')
  const [amount, setAmount] = useState('')
  const [description, setDescription] = useState('')
  const [busy, setBusy] = useState(false)
  const [formErr, setFormErr] = useState<string | null>(null)

  const selected = accounts?.find((a) => a.id === accountId)
  const canSubmit = canWrite && !!accountId && Number(amount) >= 0.0001
  const isDeposit = mode === 'DEPOSIT'

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setBusy(true)
    setFormErr(null)
    try {
      const body = { amount, description: description || undefined }
      const call = isDeposit ? depositCash : withdrawCash
      const tx = await call(accountId, body, newIdempotencyKey())
      toast.success(isDeposit ? 'Cash deposited' : 'Cash withdrawn')
      navigate(`/transactions/${tx.id}`)
    } catch (err) {
      setFormErr(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <TopBar title="Cash" subtitle="Add or remove cash (balanced double-entry)" />
      <div className="content">
        <div className="col-main">
          {loading ? (
            <Loading />
          ) : error ? (
            <ErrorState message={error} />
          ) : (
            <div className="card">
              <div className="head">
                <h2>{isDeposit ? 'Deposit cash' : 'Withdraw cash'}</h2>
              </div>
              <form className="body" onSubmit={submit}>
                {!canWrite && <Alert kind="error">Your role is read-only.</Alert>}
                {formErr && <Alert kind="error">{formErr}</Alert>}
                <div className="form-row">
                  <label>Operation</label>
                  <div className="seg">
                    <button
                      type="button"
                      className={`btn ${isDeposit ? '' : 'secondary'}`}
                      onClick={() => setMode('DEPOSIT')}
                    >
                      Deposit
                    </button>
                    <button
                      type="button"
                      className={`btn ${isDeposit ? 'secondary' : ''}`}
                      onClick={() => setMode('WITHDRAW')}
                    >
                      Withdraw
                    </button>
                  </div>
                </div>
                <div className="form-row">
                  <label>Account</label>
                  <AccountSelect
                    accounts={accounts ?? []}
                    value={accountId}
                    onChange={setAccountId}
                  />
                </div>
                <div className="form-row two">
                  <div>
                    <label>Amount</label>
                    <input
                      inputMode="decimal"
                      value={amount}
                      onChange={(e) => setAmount(e.target.value)}
                      placeholder="0.0000"
                    />
                  </div>
                  <div>
                    <label>Currency</label>
                    <input value={selected?.currency ?? '—'} disabled readOnly />
                  </div>
                </div>
                <div className="form-row">
                  <label>Description (optional)</label>
                  <input
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    placeholder={isDeposit ? 'e.g. Opening cash deposit' : 'e.g. ATM withdrawal'}
                  />
                </div>
                <div className="form-actions">
                  <span className="badge-note" style={{ marginRight: 'auto' }}>
                    🔑 Idempotency-Key auto-generated
                  </span>
                  <button className="btn" type="submit" disabled={!canSubmit || busy}>
                    {busy ? <span className="spinner" /> : isDeposit ? 'Deposit Cash' : 'Withdraw Cash'}
                  </button>
                </div>
              </form>
            </div>
          )}
        </div>

        <aside className="panel">
          <h3>How it posts</h3>
          <div className="sub">
            Cash movements keep the ledger balanced with a two-legged entry:
          </div>
          <ul className="hint-list">
            <li>
              The selected account is posted so its natural balance{' '}
              <strong>{isDeposit ? 'increases' : 'decreases'}</strong>.
            </li>
            <li>
              A system <strong>Cash Clearing</strong> account (one per currency) takes the opposite
              leg.
            </li>
            {!isDeposit && (
              <li>Withdrawals are rejected if they would overdraw the account.</li>
            )}
          </ul>
          <div className="sub">Debits always equal credits — the books stay balanced.</div>
        </aside>
      </div>
    </>
  )
}
