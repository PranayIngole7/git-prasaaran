import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiClient } from '../../../lib/api'
import { getActivity, getRepositoryActivity } from './activityApi'

vi.mock('../../../lib/api', () => ({
  apiClient: {
    get: vi.fn(),
  },
}))

const mockedGet = vi.mocked(apiClient.get)

describe('activityApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('requests global activity with filters and forwards the abort signal', async () => {
    const response = { items: [], page: 0, size: 10, totalElements: 0, totalPages: 0 }
    const signal = new AbortController().signal
    mockedGet.mockResolvedValue({ data: response })

    await expect(getActivity(
      {
        page: 0,
        size: 10,
        status: 'PROCESSED',
        eventType: 'push',
        from: '2026-10-01T00:00:00Z',
        to: '2026-10-09T23:59:59Z',
      },
      signal,
    )).resolves.toEqual(response)

    expect(mockedGet).toHaveBeenCalledWith('/api/v1/activity', {
      params: {
        page: 0,
        size: 10,
        status: 'PROCESSED',
        eventType: 'push',
        from: '2026-10-01T00:00:00Z',
        to: '2026-10-09T23:59:59Z',
      },
      signal,
    })
  })

  it('requests repository activity for an encoded repository ID', async () => {
    const response = {
      items: [{
        id: 1,
        deliveryId: 'delivery-id',
        eventType: 'push',
        commitSha: null,
        status: 'FAILED' as const,
        createdAt: '2026-10-09T12:00:00Z',
        processedAt: null,
        repositoryId: 1,
      }],
      page: 1,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    }
    mockedGet.mockResolvedValue({ data: response })

    await expect(getRepositoryActivity('owner/repo', { page: 1 })).resolves.toEqual(response)

    expect(mockedGet).toHaveBeenCalledWith(
      '/api/v1/repositories/owner%2Frepo/activity',
      { params: { page: 1 }, signal: undefined },
    )
  })
})
