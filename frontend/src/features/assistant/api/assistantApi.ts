import { apiClient } from '../../../lib/api/client'

export interface AssistantRepository {
  id: number
  owner: string
  name: string
  active: boolean
}

export interface AssistantAnswer {
  answer: string
  sources: string[]
  model: string
}

export async function getAssistantRepositories(): Promise<AssistantRepository[]> {
  const response = await apiClient.get<AssistantRepository[]>('/api/v1/repositories')
  return response.data
}

export async function askAssistant(
  repositoryId: number,
  question: string,
): Promise<AssistantAnswer> {
  const response = await apiClient.post<AssistantAnswer>(
    '/api/v1/assistant/ask',
    { repositoryId, question },
    { timeout: 120_000 },
  )
  return response.data
}
