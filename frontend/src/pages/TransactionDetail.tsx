import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { TopBar } from '../components/Layout'
import { ErrorState, KeyVal, Loading, StatusTag } from '../components/ui'
import { useAsync, errorMessage } from '../lib/useAsync'
import { useAuth } from '../auth/AuthContext'
import { useToast } from '../lib/ToastContext'
import { getTransaction, reverseTransaction } from '../api/endpoints'
import { newIdempotencyKey } from '../api/client'
import { formatDate, formatMoney, shortId } from '../lib/format'

export default function TransactionDetail() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const { canWrite } = useAuth()
  const toast = useToast()
  const { data: txn, loading, error, reload } = useAsync(() => getTransaction(id), [id])
  const [busy, setBusy] = useState(false)

  async function reverse() {
    if (!txn) return
    setBusy(true)
    try {
      const rev = await reverseTransaction(txn.id, 'Reversal via console', newIdempotencyKey())
      toast.success('Reversal posted')
      navigate(`/transactions/${rev.id}`)
      reload()
    } catch (err) {
      toast.error(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <TopBar
        title="Transaction"
        subtitle={shortId(id)}
        actions={
          <button className="btn secondary" onClick={() => navigate('/transactions')}>
            ← Back
          </button>
        }
      />
      <div className="content">
        {loading ? (
          <Loading />
        ) : error || !txn ? (
          <ErrorState message={error ?? 'Transaction not found'} />
        ) : (
          <>
            <div className="col-main">
              <div className="card">
                <div className="head">
                  <h2>Postings</h2>
                  <StatusTag status={txn.status} />
                </div>
                <table>
                  <thead>
                    <tr>
                      <th>Account</th>
                      <th>Direction</th>
                      <th className="right">Amount</th>
                    </tr>
                  </thead>
                  <tbody>
                    {txn.postings.map((p) => (
                      <tr
                        key={p.id}
                        className="clickable"
                        onClick={() => navigate(`/accounts/${p.accountId}`)}
                      >
                        <td className="mono">{shortId(p.accountId)}</td>
                        <td>
                          <span className={`tag ${p.direction === 'DEBIT' ? 'type' : 'inbound'}`}>
                            {p.direction}
                          </span>
                        </td>
                        <td className="amt">{formatMoney(p.amount, p.currency)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            <aside className="panel">
              <h3>TXN {shortId(txn.id)}</h3>
              <div className="sub">{txn.description || 'No description'}</div>
              <KeyVal k="Status">
                <StatusTag status={txn.status} />
              </KeyVal>
              <KeyVal k="External Ref">
                <span className="mono">{txn.externalRef || '—'}</span>
              </KeyVal>
              <KeyVal k="Created">{formatDate(txn.createdAt)}</KeyVal>
              <KeyVal k="Postings">{txn.postings.length}</KeyVal>
              {txn.reversalOfId && (
                <KeyVal k="Reversal of">
                  <span
                    className="mono"
                    style={{ cursor: 'pointer', color: 'var(--brand)' }}
                    onClick={() => navigate(`/transactions/${txn.reversalOfId}`)}
                  >
                    {shortId(txn.reversalOfId)}
                  </span>
                </KeyVal>
              )}
              {canWrite && (
                <div className="actions">
                  <button
                    className="btn danger"
                    disabled={txn.status === 'REVERSED' || busy}
                    onClick={reverse}
                  >
                    {busy ? <span className="spinner" /> : 'Reverse Transaction'}
                  </button>
                </div>
              )}
              {txn.status === 'REVERSED' && (
                <div className="hint" style={{ textAlign: 'center', marginTop: 8 }}>
                  Already reversed — cannot reverse again.
                </div>
              )}
            </aside>
          </>
        )}
      </div>
    </>
  )
}
