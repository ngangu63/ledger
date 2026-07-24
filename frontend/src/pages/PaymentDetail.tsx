import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { TopBar } from '../components/Layout'
import { ErrorState, KeyVal, Loading, StatusTag } from '../components/ui'
import { useAsync, errorMessage } from '../lib/useAsync'
import { useAuth } from '../auth/AuthContext'
import { useToast } from '../lib/ToastContext'
import { getPayment, refundPayment } from '../api/endpoints'
import { newIdempotencyKey } from '../api/client'
import { formatDate, formatMoney, shortId } from '../lib/format'

const REFUNDABLE = ['CAPTURED', 'SETTLED']

export default function PaymentDetail() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const { canWrite } = useAuth()
  const toast = useToast()
  const { data: p, loading, error, reload } = useAsync(() => getPayment(id), [id])
  const [busy, setBusy] = useState(false)

  async function refund() {
    if (!p) return
    setBusy(true)
    try {
      await refundPayment(p.id, newIdempotencyKey())
      toast.success('Payment refunded')
      reload()
    } catch (err) {
      toast.error(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const refundable = p ? REFUNDABLE.includes(p.status) : false

  return (
    <>
      <TopBar
        title="Payment"
        subtitle={shortId(id)}
        actions={
          <button className="btn secondary" onClick={() => navigate('/payments')}>
            ← Back
          </button>
        }
      />
      <div className="content">
        {loading ? (
          <Loading />
        ) : error || !p ? (
          <ErrorState message={error ?? 'Payment not found'} />
        ) : (
          <>
            <div className="col-main">
              <div className="card">
                <div className="head">
                  <h2>Details</h2>
                  <StatusTag status={p.status} />
                </div>
                <div className="body">
                  <KeyVal k="Direction">
                    <span className={`tag ${p.direction.toLowerCase()}`}>{p.direction}</span>
                  </KeyVal>
                  <KeyVal k="Method">{p.method}</KeyVal>
                  <KeyVal k="Amount">{formatMoney(p.amount, p.currency)}</KeyVal>
                  <KeyVal k="Source">
                    <span
                      className="mono"
                      style={{ cursor: 'pointer', color: 'var(--brand)' }}
                      onClick={() => navigate(`/accounts/${p.sourceAccountId}`)}
                    >
                      {shortId(p.sourceAccountId)}
                    </span>
                  </KeyVal>
                  <KeyVal k="Destination">
                    <span
                      className="mono"
                      style={{ cursor: 'pointer', color: 'var(--brand)' }}
                      onClick={() => navigate(`/accounts/${p.destinationAccountId}`)}
                    >
                      {shortId(p.destinationAccountId)}
                    </span>
                  </KeyVal>
                  <KeyVal k="Provider">{p.provider || '—'}</KeyVal>
                  <KeyVal k="Provider Ref">
                    <span className="mono">{p.providerRef || '—'}</span>
                  </KeyVal>
                  <KeyVal k="Transaction">
                    {p.transactionId ? (
                      <span
                        className="mono"
                        style={{ cursor: 'pointer', color: 'var(--brand)' }}
                        onClick={() => navigate(`/transactions/${p.transactionId}`)}
                      >
                        {shortId(p.transactionId)}
                      </span>
                    ) : (
                      '—'
                    )}
                  </KeyVal>
                  <KeyVal k="Failure Reason">{p.failureReason || '—'}</KeyVal>
                  <KeyVal k="Created">{formatDate(p.createdAt)}</KeyVal>
                </div>
              </div>
            </div>

            <aside className="panel">
              <h3>Payment {shortId(p.id)}</h3>
              <div className="sub">
                {p.method} · {p.direction}
              </div>
              <div className="hero-box">
                <div className="lbl">Amount</div>
                <div className="big">{formatMoney(p.amount, p.currency)}</div>
                <div className="signed">
                  {p.currency} · {formatDate(p.createdAt)}
                </div>
              </div>
              <KeyVal k="Status">
                <StatusTag status={p.status} />
              </KeyVal>
              <KeyVal k="External Ref">
                <span className="mono">{p.externalRef || '—'}</span>
              </KeyVal>
              {canWrite && (
                <div className="actions">
                  <button className="btn danger" disabled={!refundable || busy} onClick={refund}>
                    {busy ? <span className="spinner" /> : 'Refund'}
                  </button>
                </div>
              )}
              <div className="hint" style={{ textAlign: 'center', marginTop: 8 }}>
                Refund enabled only for CAPTURED / SETTLED
              </div>
            </aside>
          </>
        )}
      </div>
    </>
  )
}
