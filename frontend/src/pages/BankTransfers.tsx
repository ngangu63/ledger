import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { TopBar } from '../components/Layout'
import { Alert, ErrorState, Loading } from '../components/ui'
import AccountSelect from '../components/AccountSelect'
import { useAsync, errorMessage } from '../lib/useAsync'
import { useAuth } from '../auth/AuthContext'
import { useToast } from '../lib/ToastContext'
import { initiateTransfer, listAccounts } from '../api/endpoints'
import { newIdempotencyKey } from '../api/client'
import { BANK_RAILS } from '../api/types'
import type { BankRail } from '../api/types'

export default function BankTransfers() {
  const navigate = useNavigate()
  const { canWrite } = useAuth()
  const toast = useToast()
  const { data: accounts, loading, error } = useAsync(listAccounts, [])

  const [rail, setRail] = useState<BankRail>('ACH')
  const [amount, setAmount] = useState('')
  const [currency, setCurrency] = useState('USD')
  const [source, setSource] = useState('')
  const [destination, setDestination] = useState('')
  const [externalRef, setExternalRef] = useState('')
  const [lookupId, setLookupId] = useState('')
  const [busy, setBusy] = useState(false)
  const [formErr, setFormErr] = useState<string | null>(null)

  const currencyValid = /^[A-Z]{3}$/.test(currency)
  const canSubmit = canWrite && currencyValid && Number(amount) >= 0.0001 && source && destination

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setBusy(true)
    setFormErr(null)
    try {
      const t = await initiateTransfer(
        {
          rail,
          amount,
          currency,
          sourceAccountId: source,
          destinationAccountId: destination,
          externalRef: externalRef || undefined,
        },
        newIdempotencyKey(),
      )
      toast.success('Transfer initiated')
      navigate(`/bank-transfers/${t.id}`)
    } catch (err) {
      setFormErr(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <TopBar title="Bank Transfers" subtitle="ACH & wire · settles asynchronously" />
      <div className="content">
        <div className="col-main">
          {loading ? (
            <Loading />
          ) : error ? (
            <ErrorState message={error} />
          ) : (
            <div className="card">
              <div className="head">
                <h2>Initiate transfer</h2>
              </div>
              <form className="body" onSubmit={submit}>
                {!canWrite && <Alert kind="error">Your role is read-only.</Alert>}
                {formErr && <Alert kind="error">{formErr}</Alert>}
                <div className="form-row two">
                  <div>
                    <label>Rail</label>
                    <select value={rail} onChange={(e) => setRail(e.target.value as BankRail)}>
                      {BANK_RAILS.map((r) => (
                        <option key={r}>{r}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label>Currency</label>
                    <input
                      value={currency}
                      onChange={(e) => setCurrency(e.target.value.toUpperCase())}
                      maxLength={3}
                    />
                  </div>
                </div>
                <div className="form-row">
                  <label>Amount</label>
                  <input
                    inputMode="decimal"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    placeholder="0.0000"
                  />
                </div>
                <div className="form-row two">
                  <div>
                    <label>Source Account</label>
                    <AccountSelect accounts={accounts ?? []} value={source} onChange={setSource} />
                  </div>
                  <div>
                    <label>Destination Account</label>
                    <AccountSelect
                      accounts={accounts ?? []}
                      value={destination}
                      onChange={setDestination}
                    />
                  </div>
                </div>
                <div className="form-row">
                  <label>External Ref (optional)</label>
                  <input value={externalRef} onChange={(e) => setExternalRef(e.target.value)} />
                </div>
                <div className="form-actions">
                  <span className="badge-note" style={{ marginRight: 'auto' }}>
                    🔑 Idempotency-Key auto-generated
                  </span>
                  <button className="btn" type="submit" disabled={!canSubmit || busy}>
                    {busy ? <span className="spinner" /> : 'Initiate Transfer'}
                  </button>
                </div>
              </form>
            </div>
          )}
        </div>

        <aside className="panel">
          <h3>Look up transfer</h3>
          <div className="sub">Fetch a transfer by its ID to watch settlement.</div>
          <div className="form-row">
            <label>Transfer ID</label>
            <input value={lookupId} onChange={(e) => setLookupId(e.target.value)} placeholder="UUID" />
          </div>
          <button
            className="btn secondary"
            style={{ width: '100%', justifyContent: 'center' }}
            disabled={!lookupId}
            onClick={() => navigate(`/bank-transfers/${lookupId.trim()}`)}
          >
            Open transfer
          </button>
        </aside>
      </div>
    </>
  )
}
