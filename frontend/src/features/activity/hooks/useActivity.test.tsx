import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { type ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getActivity, getRepositoryActivity } from '../api/activityApi'
import { activityKeys, useActivity, useRepositoryActivity } from './useActivity'

vi.mock('../api/activityApi', () => ({
  getActivity: vi.fn(),
  getRepositoryActivity: vi.fn(),
}))

const mockedGetActivity = vi.mocked(getActivity)
const mockedGetRepositoryActivity = vi.mocked(getRepositoryActivity)
const page = { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  return function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        {children}
      </QueryClientProvider>
    )
  }
}

describe('activity hooks', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads global activity and keys the query by filters', async () => {
    const filters = { page: 2, status: 'FAILED' as const }
    mockedGetActivity.mockResolvedValue(page)

    const { result } = renderHook(() => useActivity(filters), {
      wrapper: createWrapper(),
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(result.current.data).toEqual(page)
    expect(result.current.dataUpdatedAt).toBeGreaterThan(0)
    expect(mockedGetActivity).toHaveBeenCalledWith(filters, expect.any(AbortSignal))
    expect(activityKeys.global(filters)).toEqual(['activity', 'global', filters])
  })

  it('loads repository activity with a repository-specific key', async () => {
    const filters = { page: 1, size: 5 }
    mockedGetRepositoryActivity.mockResolvedValue(page)

    const { result } = renderHook(() => useRepositoryActivity(12, filters), {
      wrapper: createWrapper(),
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(mockedGetRepositoryActivity).toHaveBeenCalledWith(
      12,
      filters,
      expect.any(AbortSignal),
    )
    expect(activityKeys.repository(12, filters)).toEqual([
      'activity',
      'repository',
      12,
      filters,
    ])
  })

  it('does not request repository activity until an ID is provided', () => {
    const { result } = renderHook(() => useRepositoryActivity(undefined), {
      wrapper: createWrapper(),
    })

    expect(result.current.fetchStatus).toBe('idle')
    expect(mockedGetRepositoryActivity).not.toHaveBeenCalled()
  })
})
