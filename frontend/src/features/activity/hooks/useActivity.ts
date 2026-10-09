import { useQuery } from '@tanstack/react-query'
import { getActivity, getRepositoryActivity } from '../api/activityApi'
import type { ActivityFilters } from '../types/activity'

export const activityKeys = {
  all: ['activity'] as const,
  global: (filters: ActivityFilters = {}) => ['activity', 'global', filters] as const,
  repository: (repositoryId: number | string, filters: ActivityFilters = {}) =>
    ['activity', 'repository', repositoryId, filters] as const,
}

export function useActivity(filters: ActivityFilters = {}) {
  return useQuery({
    queryKey: activityKeys.global(filters),
    queryFn: ({ signal }) => getActivity(filters, signal),
  })
}

export function useRepositoryActivity(
  repositoryId: number | string | undefined,
  filters: ActivityFilters = {},
) {
  return useQuery({
    queryKey: repositoryId === undefined
      ? [...activityKeys.all, 'repository', '']
      : activityKeys.repository(repositoryId, filters),
    queryFn: ({ signal }) => {
      if (repositoryId === undefined) {
        throw new Error('A repository ID is required.')
      }

      return getRepositoryActivity(repositoryId, filters, signal)
    },
    enabled: repositoryId !== undefined,
  })
}
