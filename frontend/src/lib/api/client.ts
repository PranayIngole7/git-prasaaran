import axios from 'axios'

const DEFAULT_API_BASE_URL = 'http://localhost:8080'

function resolveApiBaseUrl(): string {
  const configuredUrl = import.meta.env.VITE_API_BASE_URL?.trim()

  if (configuredUrl) {
    return configuredUrl
  }

  if (import.meta.env.PROD) {
    throw new Error(
      'VITE_API_BASE_URL must be configured for production builds.',
    )
  }

  return DEFAULT_API_BASE_URL
}

export const apiClient = axios.create({
  baseURL: resolveApiBaseUrl(),
  headers: { 'Content-Type': 'application/json' },
  timeout: 15_000,
})

let accessToken: string | null = null
let unauthorizedHandler: (() => void) | null = null

export function setApiAccessToken(token: string | null) {
  accessToken = token
}

export function setUnauthorizedHandler(handler: (() => void) | null) {
  unauthorizedHandler = handler
}

apiClient.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }

  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error)) {
      if (error.response?.status === 401) {
        accessToken = null
        unauthorizedHandler?.()

        if (window.location.pathname !== '/login') {
          window.location.assign('/login')
        }
      }

      if (error.response?.status === 403) {
        if (window.location.pathname !== '/forbidden') {
          window.location.assign('/forbidden')
        }
      }
    }

    return Promise.reject(error)
  },
)
