import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { tokenStore } from '../api/client'
import { login as loginRequest } from '../api/endpoints'

interface AuthState {
  username: string | null
  roles: string[]
  isAuthenticated: boolean
  /** ADMIN or SERVICE can perform writes; VIEWER is read-only. */
  canWrite: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthState | null>(null)

const WRITE_ROLES = ['ADMIN', 'SERVICE']

export function AuthProvider({ children }: { children: ReactNode }) {
  const [username, setUsername] = useState<string | null>(tokenStore.username)
  const [roles, setRoles] = useState<string[]>(tokenStore.roles)
  const [token, setToken] = useState<string | null>(tokenStore.token)

  const login = useCallback(async (user: string, password: string) => {
    const res = await loginRequest({ username: user, password })
    tokenStore.set(res.accessToken, res.roles, user)
    setToken(res.accessToken)
    setRoles(res.roles)
    setUsername(user)
  }, [])

  const logout = useCallback(() => {
    tokenStore.clear()
    setToken(null)
    setRoles([])
    setUsername(null)
  }, [])

  const value = useMemo<AuthState>(
    () => ({
      username,
      roles,
      isAuthenticated: !!token,
      canWrite: roles.some((r) => WRITE_ROLES.includes(r)),
      login,
      logout,
    }),
    [username, roles, token, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
