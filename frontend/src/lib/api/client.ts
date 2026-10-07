import axios from 'axios'

// Local-development default; override with VITE_API_BASE_URL in .env.
const DEFAULT_API_BASE_URL = 'http://localhost:8080'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || DEFAULT_API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15_000,
})
