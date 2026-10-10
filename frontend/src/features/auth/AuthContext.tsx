import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { getCurrentUser, login as loginRequest } from './api'
import { AuthContext, type AuthContextValue } from './auth-context'
import {
  setApiAccessToken,
  setUnauthorizedHandler,
} from '../../lib/api'
import { useQueryClient } from '@tanstack/react-query'

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [accessToken, setAccessToken] = useState<string | null>(null)
  const [currentUser, setCurrentUser] =
    useState<AuthContextValue['currentUser']>(null)
  const [isInitializing] = useState(false)

  const logout = useCallback(() => {
    setAccessToken(null)
    setApiAccessToken(null)
    setCurrentUser(null)
    queryClient.clear()
  }, [queryClient])

  const handleUnauthorized = useCallback(() => {
    setAccessToken(null)
    setApiAccessToken(null)
    setCurrentUser(null)
    queryClient.clear()
  }, [queryClient])

  useEffect(() => {
    setUnauthorizedHandler(handleUnauthorized)

    return () => {
      setUnauthorizedHandler(null)
    }
  }, [handleUnauthorized])

  const login = useCallback(async (email: string, password: string) => {
    const response = await loginRequest({ email, password })

    setAccessToken(response.accessToken)
    setApiAccessToken(response.accessToken)

    try {
      const user = await getCurrentUser()
      setCurrentUser(user)
    } catch (error) {
      setAccessToken(null)
      setApiAccessToken(null)
      throw error
    }
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      accessToken,
      currentUser,
      isAuthenticated: accessToken !== null && currentUser !== null,
      isInitializing,
      login,
      logout,
    }),
    [accessToken, currentUser, isInitializing, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
