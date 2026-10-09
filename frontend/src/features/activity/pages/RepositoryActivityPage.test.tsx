import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { getRepositoryActivity } from '../api/activityApi'
import type { ActivityEvent, ActivityPage } from '../types/activity'
import { RepositoryActivityPage } from './RepositoryActivityPage'

vi.mock('../api/activityApi', () => ({
  getActivity: vi.fn(),
  getRepositoryActivity: vi.fn(),
}))

const mockedGetRepositoryActivity = vi.mocked(getRepositoryActivity)

function event(overrides: Partial<ActivityEvent> = {}): ActivityEvent {
  return {
    id: 1,
    deliveryId: 'delivery-123',
    eventType: 'push',
    commitSha: 'abc123',
    status: 'PROCESSED',
    createdAt: '2026-10-09T12:00:00Z',
    processedAt: '2026-10-09T12:00:01Z',
    repositoryId: 17,
    ...overrides,
  }
}

function page(
  items: ActivityEvent[],
  overrides: Partial<ActivityPage> = {},
): ActivityPage {
  return {
    items,
    page: 0,
    size: 20,
    totalElements: items.length,
    totalPages: items.length > 0 ? 1 : 0,
    ...overrides,
  }
}

function renderRepositoryActivity(initialPath = '/repositories/17/activity') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[initialPath]}>
          <Routes>
            <Route path="/repositories/:repositoryId/activity" element={children} />
            <Route path="/repositories/:repositoryId" element={<p>Repository context</p>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    )
  }

  return render(<RepositoryActivityPage />, { wrapper: Wrapper })
}

describe('RepositoryActivityPage', () => {
  afterEach(() => cleanup())

  beforeEach(() => {
    vi.clearAllMocks()
    mockedGetRepositoryActivity.mockResolvedValue(page([]))
  })

  it('shows loading state and requests activity for the validated route ID', () => {
    mockedGetRepositoryActivity.mockReturnValue(new Promise(() => {}))

    renderRepositoryActivity()

    expect(screen.getByRole('status')).toHaveTextContent('Loading repository activity')
    expect(screen.getByRole('button', { name: 'Previous page' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Next page' })).toBeDisabled()
    expect(mockedGetRepositoryActivity).toHaveBeenCalledWith(
      17,
      { page: 0, size: 20 },
      expect.any(AbortSignal),
    )
  })

  it('shows repository event fields and safely renders missing optional values', async () => {
    mockedGetRepositoryActivity.mockResolvedValue(page([
      event(),
      event({
        id: 2,
        deliveryId: 'delivery-old',
        status: 'FAILED',
        commitSha: null,
        processedAt: null,
      }),
    ]))

    renderRepositoryActivity()

    const table = await screen.findByRole('table', {
      name: 'Webhook activity for repository #17',
    })
    const rows = within(table).getAllByRole('row')
    expect(within(rows[1]).getByText('push')).toBeInTheDocument()
    expect(within(rows[1]).getByText('Processed')).toBeInTheDocument()
    expect(within(rows[1]).getByText('delivery-123')).toBeInTheDocument()
    expect(within(rows[1]).getByText('abc123')).toBeInTheDocument()
    expect(within(rows[2]).getByText('Failed')).toBeInTheDocument()
    expect(within(rows[2]).getAllByText('Not recorded')).toHaveLength(2)
    expect(screen.getByRole('link', { name: 'Back to repository' })).toHaveAttribute(
      'href',
      '/repositories/17',
    )
    expect(screen.getByText(
      (_content, element) => element?.getAttribute('aria-live') === 'polite',
    )).toHaveTextContent('Page 1 of 1')
  })

  it('shows an empty activity state and disables pagination with no results', async () => {
    renderRepositoryActivity()

    expect(await screen.findByText(
      'No activity has been recorded for this repository yet.',
    )).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Previous page' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Next page' })).toBeDisabled()
  })

  it('shows API errors and retries the request', async () => {
    mockedGetRepositoryActivity
      .mockRejectedValueOnce(new Error('Service unavailable'))
      .mockResolvedValueOnce(page([]))

    renderRepositoryActivity()

    expect(await screen.findByRole('alert')).toHaveTextContent('Service unavailable')
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))

    expect(await screen.findByText(
      'No activity has been recorded for this repository yet.',
    )).toBeInTheDocument()
    expect(mockedGetRepositoryActivity).toHaveBeenCalledTimes(2)
  })

  it('paginates using response metadata and disables controls at boundaries', async () => {
    mockedGetRepositoryActivity
      .mockResolvedValueOnce(page([event()], {
        totalElements: 21,
        totalPages: 2,
      }))
      .mockResolvedValueOnce(page([event({ id: 2 })], {
        page: 1,
        totalElements: 21,
        totalPages: 2,
      }))

    renderRepositoryActivity()

    await waitFor(() => {
      expect(screen.getByText(
        (_content, element) => element?.getAttribute('aria-live') === 'polite',
      )).toHaveTextContent('Page 1 of 2')
    })
    expect(screen.getByRole('button', { name: 'Previous page' })).toBeDisabled()
    fireEvent.click(screen.getByRole('button', { name: 'Next page' }))

    await waitFor(() => {
      expect(mockedGetRepositoryActivity).toHaveBeenLastCalledWith(
        17,
        { page: 1, size: 20 },
        expect.any(AbortSignal),
      )
    })
    await waitFor(() => {
      expect(screen.getByText(
        (_content, element) => element?.getAttribute('aria-live') === 'polite',
      )).toHaveTextContent('Page 2 of 2')
    })
    expect(screen.getByRole('button', { name: 'Next page' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Previous page' })).toBeEnabled()
  })

  it('rejects invalid repository IDs without calling the activity API', () => {
    renderRepositoryActivity('/repositories/not-a-number/activity')

    expect(screen.getByRole('alert')).toHaveTextContent('positive whole number')
    expect(mockedGetRepositoryActivity).not.toHaveBeenCalled()
  })
})
