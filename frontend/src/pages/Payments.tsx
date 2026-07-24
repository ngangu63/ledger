import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { TopBar } from '../components/Layout'
import { Alert, ErrorState, Loading } from '../components/ui'
import AccountSelect from '../components/AccountSelect'
import { useAsync, errorMessage } from '../lib/useAsync'
import { useAuth } from '../auth/AuthContext'
import { useToast } from '../lib/ToastContext'
import { createPayment, listAccounts } from '../api/endpoints'
import { newIdempotencyKey } from '../api/client'
import { PAYMENT_DIRECTIONS, PAYMENT_METHODS } from '../api/types'
import type { PaymentDirection, PaymentMethod } from '../api/types'

export default function Payments() {
  const navigate = useNavigate()
  const { canWrite } = useAuth()
  const toast = useToast()
  const { data: accounts, loading, error } = useAsync(listAccounts, [])

  const [direction, setDirection] = useState<PaymentDirection>('INBOUND')
  const [method, setMethod] = useState<PaymentMethod>('CARD')
  const [amount, setAmount] = useState('')
  const [currency, setCurrency] = useState('USD')
  const [source, setSource] = useState('')
  const [destination, setDestination] = useState('')
  const [instrumentToken, setInstrumentToken] = useState('')
  const [externalRef, setExternalRef] = useState('')
  const [lookupId, setLookupId] = useState('')
  const [busy, setBusy] = useState(false)
  const [formErr, setFormErr] = useState<string | null>(null)

  const currencyValid = /^[A-Z]{3}$/.test(currency)
  const canSubmit =
    canWrite && currencyValid && Number(amount) >= 0.0001 && source && destination

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setBusy(true)
    setFormErr(null)
    try {
      const payment = await createPayment(
        {
          direction,
          method,
          amount,
          currency,
          sourceAccountId: source,
          destinationAccountId: destination,
          instrumentToken: instrumentToken || undefined,
          externalRef: externalRef || undefined,
        },
        newIdempotencyKey(),
      )
      toast.success(`Payment ${payment.status}`)
      navigate(`/payments/${payment.id}`)
    } catch (err) {
      setFormErr(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <TopBar title="Payments" subtitle="Charge & refund card / bank payments" />
      <div className="content">
        <div className="col-main">
          {loading ? (
            <Loading />
          ) : error ? (
            <ErrorState message={error} />
          ) : (
            <div className="card">
              <div className="head">
                <h2>Charge a payment</h2>
              </div>
              <form className="body" onSubmit={submit}>
                {!canWrite && <Alert kind="error">Your role is read-only.</Alert>}
                {formErr && <Alert kind="error">{formErr}</Alert>}
                <div className="form-row two">
                  <div>
                    <label>Direction</label>
                    <select
                      value={direction}
                      onChange={(e) => setDirection(e.target.value as PaymentDirection)}
                    >
                      {PAYMENT_DIRECTIONS.map((d) => (
                        <option key={d}>{d}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label>Method</label>
                    <select
                      value={method}
                      onChange={(e) => setMethod(e.target.value as PaymentMethod)}
                    >
                      {PAYMENT_METHODS.map((m) => (
                        <option key={m}>{m}</option>
                      ))}
                    </select>
                  </div>
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
                    <input
                      value={currency}
                      onChange={(e) => setCurrency(e.target.value.toUpperCase())}
                      maxLength={3}
                    />
                  </div>
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
                <div className="form-row two">
                  <div>
                    <label>Instrument Token (optional)</label>
                    <input
                      value={instrumentToken}
                      onChange={(e) => setInstrumentToken(e.target.value)}
                      placeholder="tok_…"
                    />
                  </div>
                  <div>
                    <label>External Ref (optional)</label>
                    <input value={externalRef} onChange={(e) => setExternalRef(e.target.value)} />
                  </div>
                </div>
                <div className="form-actions">
                  <span className="badge-note" style={{ marginRight: 'auto' }}>
                    🔑 Idempotency-Key auto-generated
                  </span>
                  <button className="btn" type="submit" disabled={!canSubmit || busy}>
                    {busy ? <span className="spinner" /> : 'Charge Payment'}
                  </button>
                </div>
              </form>
            </div>
          )}
        </div>

        <aside className="panel">
          <h3>Look up payment</h3>
          <div className="sub">Fetch a payment by its ID.</div>
          <div className="form-row">
            <label>Payment ID</label>
            <input value={lookupId} onChange={(e) => setLookupId(e.target.value)} placeholder="UUID" />
          </div>
          <button
            className="btn secondary"
            style={{ width: '100%', justifyContent: 'center' }}
            disabled={!lookupId}
            onClick={() => navigate(`/payments/${lookupId.trim()}`)}
          >
            Open payment
          </button>
        </aside>
      </div>
    </>
  )
}
