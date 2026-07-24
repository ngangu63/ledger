import { useNavigate, useParams } from 'react-router-dom'
import { TopBar } from '../components/Layout'
import { ErrorState, KeyVal, Loading, StatusTag } from '../components/ui'
import { useAsync } from '../lib/useAsync'
import { getAccount, getBalance } from '../api/endpoints'
import { formatDate, formatMoney, shortId } from '../lib/format'

export default function AccountDetail() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const { data: account, loading, error } = useAsync(() => getAccount(id), [id])
  const { data: balance } = useAsync(() => getBalance(id), [id])

  return (
    <>
      <TopBar
        title={account ? account.name : 'Account'}
        subtitle="Account detail & balance"
        actions={
          <button className="btn secondary" onClick={() => navigate('/accounts')}>
            ← Back
          </button>
        }
      />
      <div className="content">
        {loading ? (
          <Loading />
        ) : error || !account ? (
          <ErrorState message={error ?? 'Account not found'} />
        ) : (
          <>
            <div className="col-main">
              <div className="card">
                <div className="head">
                  <h2>Overview</h2>
                  <StatusTag status={account.status} />
                </div>
                <div className="body">
                  <KeyVal k="Account ID">
                    <span className="mono">{account.id}</span>
                  </KeyVal>
                  <KeyVal k="Name">{account.name}</KeyVal>
                  <KeyVal k="Type">
                    <span className="tag type">{account.type}</span>
                  </KeyVal>
                  <KeyVal k="Currency">{account.currency}</KeyVal>
                  <KeyVal k="Created">{formatDate(account.createdAt)}</KeyVal>
                </div>
              </div>
              <div className="form-actions">
                <button
                  className="btn"
                  onClick={() =>
                    navigate('/cash', {
                      state: { presetAccountId: account.id, mode: 'DEPOSIT' },
                    })
                  }
                >
                  Deposit cash
                </button>
                <button
                  className="btn secondary"
                  onClick={() =>
                    navigate('/cash', {
                      state: { presetAccountId: account.id, mode: 'WITHDRAW' },
                    })
                  }
                >
                  Withdraw cash
                </button>
                <button
                  className="btn secondary"
                  onClick={() =>
                    navigate('/transactions', { state: { presetAccountId: account.id } })
                  }
                >
                  New Transaction
                </button>
              </div>
            </div>

            <aside className="panel">
              <h3>{account.name}</h3>
              <div className="sub">
                {account.type} · created {formatDate(account.createdAt)}
              </div>
              <div className="hero-box">
                <div className="lbl">Natural Balance</div>
                <div className="big">
                  {balance ? formatMoney(balance.naturalBalance, balance.currency) : '—'}
                </div>
                <div className="signed">
                  {balance
                    ? `Signed (debit-positive): ${balance.signedBalance}`
                    : 'Loading balance…'}
                </div>
              </div>
              <KeyVal k="Account ID">
                <span className="mono">{shortId(account.id)}</span>
              </KeyVal>
              <KeyVal k="Type">{account.type}</KeyVal>
              <KeyVal k="Currency">{account.currency}</KeyVal>
              <KeyVal k="Status">{account.status}</KeyVal>
            </aside>
          </>
        )}
      </div>
    </>
  )
}
