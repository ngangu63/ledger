import { useNavigate } from 'react-router-dom'
import { TopBar } from '../components/Layout'
import { ErrorState, Loading } from '../components/ui'
import { useAsync } from '../lib/useAsync'
import { getBalance, getHealth, listAccounts } from '../api/endpoints'
import type { AccountResponse, BalanceResponse } from '../api/types'
import { formatMoney } from '../lib/format'

interface Row {
  account: AccountResponse
  balance: BalanceResponse | null
}

async function loadOverview(): Promise<Row[]> {
  const accounts = await listAccounts()
  const rows = await Promise.all(
    accounts.map(async (account) => {
      try {
        return { account, balance: await getBalance(account.id) }
      } catch {
        return { account, balance: null }
      }
    }),
  )
  return rows
}

export default function Dashboard() {
  const navigate = useNavigate()
  const { data: rows, loading, error } = useAsync(loadOverview, [])
  const { data: health } = useAsync(getHealth, [])

  const healthy = health?.status === 'UP'
  const currencies = rows ? [...new Set(rows.map((r) => r.account.currency))] : []

  return (
    <>
      <TopBar
        title="Dashboard"
        subtitle="System health & ledger overview"
        actions={
          <span className="badge-note">
            {health ? (healthy ? '● API healthy' : `● API ${health.status}`) : '● checking…'} ·
            /actuator/health
          </span>
        }
      />
      <div className="content">
        <div className="col-main">
          <div className="stats">
            <div className="stat">
              <div className="k">Accounts</div>
              <div className="v">{rows?.length ?? '—'}</div>
            </div>
            <div className="stat">
              <div className="k">Currencies</div>
              <div className="v" style={{ fontSize: 18 }}>
                {currencies.join(' · ') || '—'}
              </div>
            </div>
            <div className="stat">
              <div className="k">API Status</div>
              <div className="v" style={{ fontSize: 18 }}>
                {health?.status ?? '—'}
              </div>
            </div>
          </div>

          <div className="card">
            <div className="head">
              <h2>Account balances</h2>
              <span className="badge-note">natural balance</span>
            </div>
            {loading ? (
              <Loading />
            ) : error ? (
              <ErrorState message={error} />
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>Account</th>
                    <th>Type</th>
                    <th>Currency</th>
                    <th className="right">Balance</th>
                  </tr>
                </thead>
                <tbody>
                  {rows?.map(({ account, balance }) => (
                    <tr
                      key={account.id}
                      className="clickable"
                      onClick={() => navigate(`/accounts/${account.id}`)}
                    >
                      <td className="name">{account.name}</td>
                      <td>
                        <span className="tag type">{account.type}</span>
                      </td>
                      <td>{account.currency}</td>
                      <td className="amt">
                        {balance ? formatMoney(balance.naturalBalance, balance.currency) : '—'}
                      </td>
                    </tr>
                  ))}
                  {rows?.length === 0 && (
                    <tr>
                      <td colSpan={4} style={{ textAlign: 'center', color: 'var(--muted)' }}>
                        No accounts yet. Create one under Accounts.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>
    </>
  )
}
