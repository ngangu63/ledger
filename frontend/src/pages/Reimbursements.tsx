import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { TopBar } from '../components/Layout'
import { Alert, ErrorState, Loading } from '../components/ui'
import AccountSelect from '../components/AccountSelect'
import { useAsync, errorMessage } from '../lib/useAsync'
import { useAuth } from '../auth/AuthContext'
import { useToast } from '../lib/ToastContext'
import { listAccounts, submitReimbursement } from '../api/endpoints'

export default function Reimbursements() {
  const navigate = useNavigate()
  const { canWrite } = useAuth()
  const toast = useToast()
  const { data: accounts, loading, error } = useAsync(listAccounts, [])

  const [requester, setRequester] = useState('')
  const [amount, setAmount] = useState('')
  const [currency, setCurrency] = useState('USD')
  const [description, setDescription] = useState('')
  const [funding, setFunding] = useState('')
  const [payee, setPayee] = useState('')
  const [lookupId, setLookupId] = useState('')
  const [busy, setBusy] = useState(false)
  const [formErr, setFormErr] = useState<string | null>(null)

  const currencyValid = /^[A-Z]{3}$/.test(currency)
  const canSubmit =
    canWrite && requester && currencyValid && Number(amount) >= 0.0001 && funding && payee

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setBusy(true)
    setFormErr(null)
    try {
      const r = await submitReimbursement({
        requester,
        amount,
        currency,
        description: description || undefined,
        fundingAccountId: funding,
        payeeAccountId: payee,
      })
      toast.success('Reimbursement submitted')
      navigate(`/reimbursements/${r.id}`)
    } catch (err) {
      setFormErr(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <TopBar title="Reimbursements" subtitle="Submit → Approve/Reject → Pay" />
      <div className="content">
        <div className="col-main">
          {loading ? (
            <Loading />
          ) : error ? (
            <ErrorState message={error} />
          ) : (
            <div className="card">
              <div className="head">
                <h2>Submit reimbursement</h2>
              </div>
              <form className="body" onSubmit={submit}>
                {!canWrite && <Alert kind="error">Your role is read-only.</Alert>}
                {formErr && <Alert kind="error">{formErr}</Alert>}
                <div className="form-row two">
                  <div>
                    <label>Requester</label>
                    <input value={requester} onChange={(e) => setRequester(e.target.value)} />
                  </div>
                  <div>
                    <label>Amount</label>
                    <input
                      inputMode="decimal"
                      value={amount}
                      onChange={(e) => setAmount(e.target.value)}
                      placeholder="0.0000"
                    />
                  </div>
                </div>
                <div className="form-row two">
                  <div>
                    <label>Currency</label>
                    <input
                      value={currency}
                      onChange={(e) => setCurrency(e.target.value.toUpperCase())}
                      maxLength={3}
                    />
                  </div>
                  <div>
                    <label>Funding Account</label>
                    <AccountSelect accounts={accounts ?? []} value={funding} onChange={setFunding} />
                  </div>
                </div>
                <div className="form-row two">
                  <div>
                    <label>Payee Account</label>
                    <AccountSelect accounts={accounts ?? []} value={payee} onChange={setPayee} />
                  </div>
                  <div>
                    <label>Description (optional)</label>
                    <input value={description} onChange={(e) => setDescription(e.target.value)} />
                  </div>
                </div>
                <div className="form-actions">
                  <button className="btn" type="submit" disabled={!canSubmit || busy}>
                    {busy ? <span className="spinner" /> : 'Submit'}
                  </button>
                </div>
              </form>
            </div>
          )}
        </div>

        <aside className="panel">
          <h3>Look up reimbursement</h3>
          <div className="sub">Fetch one by ID to approve, reject, or pay.</div>
          <div className="form-row">
            <label>Reimbursement ID</label>
            <input value={lookupId} onChange={(e) => setLookupId(e.target.value)} placeholder="UUID" />
          </div>
          <button
            className="btn secondary"
            style={{ width: '100%', justifyContent: 'center' }}
            disabled={!lookupId}
            onClick={() => navigate(`/reimbursements/${lookupId.trim()}`)}
          >
            Open reimbursement
          </button>
        </aside>
      </div>
    </>
  )
}
