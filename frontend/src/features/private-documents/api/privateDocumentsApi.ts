import { apiClient } from '../../../lib/api'
import type {
  PrivateDocument,
  PrivateDocumentRequest,
} from '../types/privateDocument'

const PRIVATE_DOCUMENTS_PATH = '/api/v1/private-documents'

export async function getPrivateDocuments(
  signal?: AbortSignal,
): Promise<PrivateDocument[]> {
  const { data } = await apiClient.get<PrivateDocument[]>(
    PRIVATE_DOCUMENTS_PATH,
    { signal },
  )
  return data
}

export async function getPrivateDocument(
  id: number,
  signal?: AbortSignal,
): Promise<PrivateDocument> {
  const { data } = await apiClient.get<PrivateDocument>(
    `${PRIVATE_DOCUMENTS_PATH}/${id}`,
    { signal },
  )
  return data
}

export async function createPrivateDocument(
  request: PrivateDocumentRequest,
): Promise<PrivateDocument> {
  const { data } = await apiClient.post<PrivateDocument>(
    PRIVATE_DOCUMENTS_PATH,
    request,
  )
  return data
}

export async function updatePrivateDocument(
  id: number,
  request: PrivateDocumentRequest,
): Promise<PrivateDocument> {
  const { data } = await apiClient.put<PrivateDocument>(
    `${PRIVATE_DOCUMENTS_PATH}/${id}`,
    request,
  )
  return data
}

export async function deletePrivateDocument(id: number): Promise<void> {
  await apiClient.delete(`${PRIVATE_DOCUMENTS_PATH}/${id}`)
}
