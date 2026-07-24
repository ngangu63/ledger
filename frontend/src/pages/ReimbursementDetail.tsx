import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { TopBar } from '../components/Layout'
import { ErrorState, KeyVal, Loading, StatusTag } from '../components/ui'
import { useAsync, errorMessage } from '../lib/useAsync'
import { useAuth } from '../auth/AuthContext'
import { useToast } from '../lib/ToastContext'
import {
  approveReimbursement,
  getReimbursement,
  payReimbursement,
  rejectReimbursement,
} from '../api/endpoints'
import { formatDate, formatMoney, shortId } from '../lib/format'

export default function ReimbursementDetail() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const { canWrite, username } = useAuth()
  const toast = useToast()
  const { data: r, loading, error, reload } = useAsync(() => getReimbursement(id), [id])
  const [busy, setBusy] = useState(false)

  async function run(action: () => Promise<unknown>, ok: string) {
    setBusy(true)
    try {
      await action()
      toast.success(ok)
      reload()
    } catch (err) {
      toast.error(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const approver = username ?? 'console'
  const isSubmitted = r?.status === 'SUBMITTED'
  const isApproved = r?.status === 'APPROVED'

  return (
    <>
      <TopBar
        title="Reimbursement"
        subtitle={shortId(id)}
        actions={
          <button className="btn secondary" onClick={() => navigate('/reimbursements')}>
            ← Back
          </button>
        }
      />
      <div className="content">
        {loading ? (
          <Loading />
        ) : error || !r ? (
          <ErrorState message={error ?? 'Reimbursement not found'} />
        ) : (
          <>
            <div className="col-main">
              <div className="card">
                <div className="head">
                  <h2>Details</h2>
                  <StatusTag status={r.status} />
                </div>
                <div className="body">
                  <KeyVal k="Requester">{r.requester}</KeyVal>
                  <KeyVal k="Amount">{formatMoney(r.amount, r.currency)}</KeyVal>
                  <KeyVal k="Description">{r.description || '—'}</KeyVal>
                  <KeyVal k="Funding Account">
                    <span
                      className="mono"
                      style={{ cursor: 'pointer', color: 'var(--brand)' }}
                      onClick={() => navigate(`/accounts/${r.fundingAccountId}`)}
                    >
                      {shortId(r.fundingAccountId)}
                    </span>
                  </KeyVal>
                  <KeyVal k="Payee Account">
                    <span
                      className="mono"
                      style={{ cursor: 'pointer', color: 'var(--brand)' }}
                      onClick={() => navigate(`/accounts/${r.payeeAccountId}`)}
                    >
                      {shortId(r.payeeAccountId)}
                    </span>
                  </KeyVal>
                  <KeyVal k="Decided By">{r.decidedBy || '—'}</KeyVal>
                  <KeyVal k="Payment">
                    {r.paymentId ? (
                      <span
                        className="mono"
                        style={{ cursor: 'pointer', color: 'var(--brand)' }}
                        onClick={() => navigate(`/payments/${r.paymentId}`)}
                      >
                        {shortId(r.paymentId)}
                      </span>
                    ) : (
                      '— (unpaid)'
                    )}
                  </KeyVal>
                  <KeyVal k="Created">{formatDate(r.createdAt)}</KeyVal>
                </div>
              </div>
            </div>

            <aside className="panel">
              <h3>Reimbursement {shortId(r.id)}</h3>
              <div className="sub">Requested by {r.requester}</div>
              <div className="hero-box">
                <div className="lbl">Amount</div>
                <div className="big">{formatMoney(r.amount, r.currency)}</div>
                <div className="signed">
                  {r.currency} · {r.description || 'No description'}
                </div>
              </div>
              <KeyVal k="Status">
                <StatusTag status={r.status} />
              </KeyVal>

              {canWrite && (
                <>
                  <label style={{ marginTop: 16 }}>Actions · current state {r.status}</label>
                  <div className="actions">
                    <button
                      className="btn success"
                      disabled={!isSubmitted || busy}
                      onClick={() =>
                        run(() => approveReimbursement(r.id, approver), 'Approved')
                      }
                    >
                      Approve
                    </button>
                    <button
                      className="btn danger"
                      disabled={!isSubmitted || busy}
                      onClick={() => run(() => rejectReimbursement(r.id, approver), 'Rejected')}
                    >
                      Reject
                    </button>
                  </div>
                  <div className="actions">
                    <button
                      className="btn"
                      disabled={!isApproved || busy}
                      onClick={() => run(() => payReimbursement(r.id), 'Paid via OUTBOUND bank payment')}
                    >
                      Pay (OUTBOUND bank payment)
                    </button>
                  </div>
                  <div className="hint" style={{ textAlign: 'center', marginTop: 8 }}>
                    Approve/Reject only from SUBMITTED · Pay only from APPROVED
                  </div>
                </>
              )}
            </aside>
          </>
        )}
      </div>
    </>
  )
}
