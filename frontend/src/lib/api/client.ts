import axios from 'axios'

// Local-development default; override with VITE_API_BASE_URL in .env.
const DEFAULT_API_BASE_URL = 'http://localhost:8080'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || DEFAULT_API_BASE_URL,
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
