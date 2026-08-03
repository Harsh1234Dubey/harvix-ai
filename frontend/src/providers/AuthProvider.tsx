import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { get, post, tokenStore } from '@/lib/api'
import type { AuthData, User } from '@/lib/types'

interface AuthContextValue {
  user: User | null
  loading: boolean
  login: (email: string, password: string, rememberMe: boolean) => Promise<User>
  logout: () => Promise<void>
  setUser: (user: User) => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUserState] = useState<User | null>(() => tokenStore.getUser() as User | null)
  const [loading, setLoading] = useState<boolean>(true)

  useEffect(() => {
    async function bootstrap() {
      const access = tokenStore.getAccess()
      if (access) {
        try {
          const me = await get<User>('/auth/me')
          setUserState(me)
          tokenStore.setUser(me)
        } catch {
          tokenStore.clear()
          setUserState(null)
        }
      }
      setLoading(false)
    }
    void bootstrap()
  }, [])

  const login = useCallback(async (email: string, password: string, rememberMe: boolean) => {
    const data = await post<AuthData>('/auth/login', { email, password, rememberMe })
    tokenStore.setTokens(data.accessToken, data.refreshToken)
    tokenStore.setUser(data.user)
    setUserState(data.user)
    return data.user
  }, [])

  const logout = useCallback(async () => {
    try {
      await post('/auth/logout', { email: user?.email })
    } catch {
      // ignore
    }
    tokenStore.clear()
    setUserState(null)
  }, [user])

  const setUser = useCallback((next: User) => {
    tokenStore.setUser(next)
    setUserState(next)
  }, [])

  const value = useMemo(
    () => ({ user, loading, login, logout, setUser }),
    [user, loading, login, logout, setUser],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
