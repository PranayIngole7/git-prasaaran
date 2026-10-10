import { useQuery } from '@tanstack/react-query'
import { isNotFoundError } from '../../../lib/api'
import {
  getPrivateDocument,
  getPrivateDocuments,
} from '../api/privateDocumentsApi'

export const privateDocumentKeys = {
  all: (email: string) => ['private-documents', email] as const,
  detail: (email: string, id: number) =>
    ['private-documents', email, id] as const,
}

export function usePrivateDocuments(email: string | undefined) {
  return useQuery({
    queryKey: privateDocumentKeys.all(email ?? ''),
    queryFn: ({ signal }) => getPrivateDocuments(signal),
    enabled: Boolean(email),
  })
}

export function usePrivateDocument(
  email: string | undefined,
  id: number | undefined,
) {
  return useQuery({
    queryKey: privateDocumentKeys.detail(email ?? '', id ?? 0),
    queryFn: ({ signal }) => {
      if (id === undefined) {
        throw new Error('A private document ID is required.')
      }

      return getPrivateDocument(id, signal)
    },
    enabled: Boolean(email) && id !== undefined,
    retry: (failureCount, error) =>
      !isNotFoundError(error) && failureCount < 1,
  })
}
