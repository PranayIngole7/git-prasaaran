import { useQuery } from '@tanstack/react-query'
import { isNotFoundError } from '../../../lib/api'
import { getDocument, getDocuments } from '../api/documentsApi'

export const documentKeys = {
  all: ['documents'] as const,
  detail: (slug: string) => ['documents', slug] as const,
}

export function useDocuments() {
  return useQuery({
    queryKey: documentKeys.all,
    queryFn: ({ signal }) => getDocuments(signal),
  })
}

export function useDocument(slug: string | undefined) {
  return useQuery({
    queryKey: documentKeys.detail(slug ?? ''),
    queryFn: ({ signal }) => {
      if (!slug) {
        throw new Error('A document slug is required.')
      }

      return getDocument(slug, signal)
    },
    enabled: Boolean(slug),
    retry: (failureCount, error) =>
      !isNotFoundError(error) && failureCount < 1,
  })
}
