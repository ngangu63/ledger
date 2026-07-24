import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { TopBar } from '../components/Layout'
import { ErrorState, KeyVal, Loading, StatusTag } from '../components/ui'
import { errorMessage } from '../lib/useAsync'
import { getBankTransfer } from '../api/endpoints'
import type { BankTransferResponse } from '../api/types'
import { formatDate, formatMoney, shortId } from '../lib/format'

const POLL_MS = 5000

export default function BankTransferDetail() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const [transfer, setTransfer] = useState<BankTransferResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const timer = useRef<number | undefined>(undefined)

  const load = useCallback(
    async (initial = false) => {
      if (initial) setLoading(true)
      try {
        const t = await getBankTransfer(id)
        setTransfer(t)
        setError(null)
      } catch (err) {
        setError(errorMessage(err))
      } finally {
        if (initial) setLoading(false)
      }
    },
    [id],
  )

  // Poll while the transfer is still PENDING (settlement is async, no realtime channel).
  useEffect(() => {
    load(true)
    return () => window.clearTimeout(timer.current)
  }, [load])

  useEffect(() => {
    window.clearTimeout(timer.current)
    if (transfer?.status === 'PENDING') {
      timer.current = window.setTimeout(() => load(false), POLL_MS)
    }
    return () => window.clearTimeout(timer.current)
  }, [transfer, load])

  const t = transfer
  const settled = t && t.status !== 'PENDING'

  return (
    <>
      <TopBar
        title="Bank Transfer"
        subtitle={shortId(id)}
        actions={
          <button className="btn secondary" onClick={() => navigate('/bank-transfers')}>
            ← Back
          </button>
        }
      />
      <div className="content">
        {loading ? (
          <Loading />
        ) : error || !t ? (
          <ErrorState message={error ?? 'Transfer not found'} />
        ) : (
          <>
            <div className="col-main">
              <div className="card">
                <div className="head">
                  <h2>Details</h2>
                  <StatusTag status={t.status} />
                </div>
                <div className="body">
                  <KeyVal k="Rail">{t.rail}</KeyVal>
                  <KeyVal k="Amount">{formatMoney(t.amount, t.currency)}</KeyVal>
                  <KeyVal k="Source">
                    <span
                      className="mono"
                      style={{ cursor: 'pointer', color: 'var(--brand)' }}
                      onClick={() => navigate(`/accounts/${t.sourceAccountId}`)}
                    >
                      {shortId(t.sourceAccountId)}
                    </span>
                  </KeyVal>
                  <KeyVal k="Destination">
                    <span
                      className="mono"
                      style={{ cursor: 'pointer', color: 'var(--brand)' }}
                      onClick={() => navigate(`/accounts/${t.destinationAccountId}`)}
                    >
                      {shortId(t.destinationAccountId)}
                    </span>
                  </KeyVal>
                  <KeyVal k="Provider">{t.provider || '—'}</KeyVal>
                  <KeyVal k="Provider Ref">
                    <span className="mono">{t.providerRef || '—'}</span>
                  </KeyVal>
                  <KeyVal k="Transaction">
                    {t.transactionId ? (
                      <span
                        className="mono"
                        style={{ cursor: 'pointer', color: 'var(--brand)' }}
                        onClick={() => navigate(`/transactions/${t.transactionId}`)}
                      >
                        {shortId(t.transactionId)}
                      </span>
                    ) : (
                      '—'
                    )}
                  </KeyVal>
                  <KeyVal k="External Ref">
                    <span className="mono">{t.externalRef || '—'}</span>
                  </KeyVal>
                  <KeyVal k="Created">{formatDate(t.createdAt)}</KeyVal>
                  <KeyVal k="Settled At">{formatDate(t.settledAt)}</KeyVal>
                </div>
              </div>
            </div>

            <aside className="panel">
              <h3>Transfer {shortId(t.id)}</h3>
              <div className="sub">
                {t.rail} · {formatMoney(t.amount, t.currency)}
              </div>
              <KeyVal k="Status">
                <StatusTag status={t.status} />
              </KeyVal>

              <label style={{ marginTop: 18 }}>Settlement status</label>
              <ul className="timeline">
                <li className="done">
                  Initiated
                  <div className="when">{formatDate(t.createdAt)}</div>
                </li>
                <li className={settled ? 'done' : 'now'}>
                  Pending settlement
                  <div className="when">
                    {settled ? 'completed' : 'polling every 5s…'}
                  </div>
                </li>
                <li className={settled ? 'done' : ''}>
                  {t.status === 'RETURNED' ? 'Returned' : 'Settled'}
                  <div className="when">
                    {settled ? formatDate(t.settledAt) : 'awaiting bank webhook'}
                  </div>
                </li>
              </ul>
              <div className="actions">
                <button className="btn secondary" onClick={() => load(false)}>
                  ↻ Refresh status
                </button>
              </div>
            </aside>
          </>
        )}
      </div>
    </>
  )
}
