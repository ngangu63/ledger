import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const NAV = [
  {
    label: 'Overview',
    items: [
      { to: '/dashboard', ico: '▤', text: 'Dashboard' },
      { to: '/accounts', ico: '▦', text: 'Accounts' },
    ],
  },
  {
    label: 'Money movement',
    items: [
      { to: '/transactions', ico: '⇄', text: 'Transactions' },
      { to: '/cash', ico: '＄', text: 'Cash In / Out' },
      { to: '/payments', ico: '▣', text: 'Payments' },
      { to: '/bank-transfers', ico: '◫', text: 'Bank Transfers' },
      { to: '/reimbursements', ico: '↺', text: 'Reimbursements' },
    ],
  },
]

export default function Layout() {
  const { username, roles, canWrite, logout } = useAuth()
  const initials = (username ?? '?').slice(0, 2).toUpperCase()

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand">
          <span className="dot">₪</span> Ledger
        </div>
        {NAV.map((group) => (
          <div key={group.label}>
            <div className="nav-label">{group.label}</div>
            {group.items.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
              >
                <span className="ico">{item.ico}</span> {item.text}
              </NavLink>
            ))}
          </div>
        ))}
        <div className="spacer" />
        <div className="user">
          <div className="avatar">{initials}</div>
          <div>
            <div className="who">{username}</div>
            <div className="role">
              {roles.join(', ') || '—'} · {canWrite ? 'can write' : 'read-only'}
            </div>
          </div>
          <button className="logout" onClick={logout} title="Sign out">
            ⎋
          </button>
        </div>
      </aside>
      <div className="main">
        <Outlet />
      </div>
    </div>
  )
}

/** Standard top bar used by every page. */
export function TopBar({
  title,
  subtitle,
  actions,
}: {
  title: string
  subtitle?: string
  actions?: React.ReactNode
}) {
  return (
    <div className="topbar">
      <div>
        <h1>{title}</h1>
        {subtitle && <div className="sub">{subtitle}</div>}
      </div>
      {actions && <div style={{ display: 'flex', gap: 10 }}>{actions}</div>}
    </div>
  )
}
