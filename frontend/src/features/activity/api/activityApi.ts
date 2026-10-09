import { apiClient } from '../../../lib/api'
import type { ActivityFilters, ActivityPage } from '../types/activity'

const ACTIVITY_PATH = '/api/v1/activity'

export async function getActivity(
  filters: ActivityFilters = {},
  signal?: AbortSignal,
): Promise<ActivityPage> {
  const { data } = await apiClient.get<ActivityPage>(ACTIVITY_PATH, {
    params: filters,
    signal,
  })
  return data
}

export async function getRepositoryActivity(
  repositoryId: number | string,
  filters: ActivityFilters = {},
  signal?: AbortSignal,
): Promise<ActivityPage> {
  const { data } = await apiClient.get<ActivityPage>(
    `/api/v1/repositories/${encodeURIComponent(repositoryId)}/activity`,
    {
      params: filters,
      signal,
    },
  )
  return data
}
