import { apiClient } from '../../../lib/api'
import type { Document } from '../types/document'

const DOCUMENTS_PATH = '/api/v1/documents'

export async function getDocuments(signal?: AbortSignal): Promise<Document[]> {
  const { data } = await apiClient.get<Document[]>(DOCUMENTS_PATH, { signal })
  return data
}

export async function getDocument(slug: string, signal?: AbortSignal): Promise<Document> {
  const { data } = await apiClient.get<Document>(
    `${DOCUMENTS_PATH}/${encodeURIComponent(slug)}`,
    { signal },
  )
  return data
}
