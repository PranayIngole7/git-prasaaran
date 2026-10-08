import { createContext } from 'react'
import type { CurrentUser } from './api'

export interface AuthContextValue {
  accessToken: string | null
  currentUser: CurrentUser | null
  isAuthenticated: boolean
  isInitializing: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | undefined>(
  undefined,
)
