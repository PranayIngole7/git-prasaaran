import { apiClient } from '../../lib/api'

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export interface CurrentUser {
  email: string
  roles: string[]
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await apiClient.post<LoginResponse>(
    '/api/v1/auth/login',
    request,
  )

  return response.data
}

export async function getCurrentUser(): Promise<CurrentUser> {
  const response = await apiClient.get<CurrentUser>('/api/v1/me')
  return response.data
}
