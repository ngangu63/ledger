import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { errorMessage } from '../lib/useAsync'
import { Alert } from '../components/ui'

const DEV_USERS = [
  { creds: 'admin / admin123', note: 'full write access (ADMIN)' },
  { creds: 'service / service123', note: 'write access (SERVICE)' },
  { creds: 'viewer / viewer123', note: 'read-only (VIEWER)' },
]

export default function Login() {
  const { login, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (isAuthenticated) {
    navigate('/dashboard', { replace: true })
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await login(username, password)
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  function fill(creds: string) {
    const [u, p] = creds.split(' / ')
    setUsername(u)
    setPassword(p)
  }

  return (
    <div className="login-wrap">
      <form className="login-card" onSubmit={submit}>
        <div className="logo">
          <span className="dot">₪</span> Ledger
        </div>
        <div className="tag-line">Sign in to the double-entry ledger console</div>

        {error && <Alert kind="error">{error}</Alert>}

        <div className="form-row">
          <label>Username</label>
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus />
        </div>
        <div className="form-row">
          <label>Password</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>
        <button className="btn" type="submit" disabled={busy}>
          {busy ? <span className="spinner" /> : 'Sign in'}
        </button>

        <div className="dev-users">
          <strong>Dev accounts</strong> — click to fill
          <br />
          {DEV_USERS.map((u) => (
            <div key={u.creds}>
              <code onClick={() => fill(u.creds)}>{u.creds}</code> — {u.note}
            </div>
          ))}
        </div>
      </form>
    </div>
  )
}
