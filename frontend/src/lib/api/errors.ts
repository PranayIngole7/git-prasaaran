import { isAxiosError } from 'axios'

export function isNotFoundError(error: unknown): boolean {
  return isAxiosError(error) && error.response?.status === 404
}

export function getErrorMessage(error: unknown): string {
  if (isAxiosError(error)) {
    if (!error.response) {
      return 'Could not reach the backend. Check that it is running, that VITE_API_BASE_URL is correct, and that CORS allows this origin.'
    }
    return `The backend returned an error (HTTP ${error.response.status}).`
  }

  return error instanceof Error ? error.message : 'An unexpected error occurred.'
}
