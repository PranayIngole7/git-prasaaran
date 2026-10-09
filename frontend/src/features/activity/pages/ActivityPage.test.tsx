import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getActivity } from '../api/activityApi'
import type { ActivityEvent, ActivityPage as ActivityPageData } from '../types/activity'
import { ActivityPage } from './ActivityPage'

vi.mock('../api/activityApi', () => ({
  getActivity: vi.fn(),
}))

const mockedGetActivity = vi.mocked(getActivity)

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
  overrides: Partial<ActivityPageData> = {},
): ActivityPageData {
  return {
    items,
    page: 0,
    size: 20,
    totalElements: items.length,
    totalPages: items.length > 0 ? 1 : 0,
    ...overrides,
  }
}

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <ActivityPage />
    </QueryClientProvider>,
  )
}

describe('ActivityPage', () => {
  afterEach(() => cleanup())

  beforeEach(() => {
    vi.clearAllMocks()
    mockedGetActivity.mockResolvedValue(page([]))
  })

  it('shows loading state', () => {
    mockedGetActivity.mockReturnValue(new Promise(() => {}))

    renderPage()

    expect(screen.getByRole('status')).toHaveTextContent('Loading activity')
  })

  it('shows persisted event fields and safe labels for nullable values', async () => {
    mockedGetActivity.mockResolvedValue(page([
      event(),
      event({
        id: 2,
        deliveryId: 'delivery-old',
        status: 'FAILED',
        commitSha: null,
        processedAt: null,
        repositoryId: null,
      }),
    ]))

    renderPage()

    await screen.findByRole('table', { name: 'GitHub webhook activity' })
    expect(screen.getByRole('table', { name: 'GitHub webhook activity' })).toBeInTheDocument()
    expect(screen.getByText('delivery-123')).toBeInTheDocument()
    expect(screen.getByText('abc123')).toBeInTheDocument()
    const rows = screen.getAllByRole('row')
    expect(within(rows[1]).getByText('Processed')).toBeInTheDocument()
    expect(within(rows[2]).getByText('Failed')).toBeInTheDocument()
    expect(screen.getByText('Repository #17')).toBeInTheDocument()
    expect(screen.getByText('Unassociated')).toBeInTheDocument()
    expect(screen.getAllByText('Not recorded')).toHaveLength(2)
    expect(screen.getByText(
      (_content, element) => element?.getAttribute('aria-live') === 'polite',
    )).toHaveTextContent('Page 1 of 1')
  })

  it('distinguishes an empty feed from an empty filtered result', async () => {
    mockedGetActivity.mockResolvedValue(page([]))
    renderPage()

    expect(await screen.findByText('No webhook activity has been recorded yet.')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Status'), { target: { value: 'FAILED' } })
    fireEvent.click(screen.getByRole('button', { name: 'Apply filters' }))

    expect(await screen.findByText('No activity matches these filters. Try changing or clearing them.')).toBeInTheDocument()
  })

  it('shows an API error and retries the query', async () => {
    mockedGetActivity
      .mockRejectedValueOnce(new Error('Backend unavailable'))
      .mockResolvedValueOnce(page([]))

    renderPage()
    expect(await screen.findByRole('alert')).toHaveTextContent('Backend unavailable')
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))

    expect(await screen.findByText('No webhook activity has been recorded yet.')).toBeInTheDocument()
    expect(mockedGetActivity).toHaveBeenCalledTimes(2)
  })

  it('applies non-empty filters and resets page to zero', async () => {
    mockedGetActivity
      .mockResolvedValueOnce(page([event()], { page: 2, totalPages: 3, totalElements: 41 }))
      .mockResolvedValueOnce(page([event()]))

    renderPage()
    await screen.findByRole('table', { name: 'GitHub webhook activity' })
    fireEvent.change(screen.getByLabelText('Status'), { target: { value: 'FAILED' } })
    fireEvent.change(screen.getByLabelText('Event type'), { target: { value: ' push ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Apply filters' }))

    await waitFor(() => {
      expect(mockedGetActivity).toHaveBeenLastCalledWith(
        { page: 0, size: 20, status: 'FAILED', eventType: 'push' },
        expect.any(AbortSignal),
      )
    })
  })

  it('validates date ranges before applying filters', () => {
    renderPage()
    fireEvent.change(screen.getByLabelText('From'), { target: { value: '2026-10-10T12:00' } })
    fireEvent.change(screen.getByLabelText('To'), { target: { value: '2026-10-09T12:00' } })

    const callCount = mockedGetActivity.mock.calls.length
    fireEvent.click(screen.getByRole('button', { name: 'Apply filters' }))

    expect(screen.getByRole('alert')).toHaveTextContent('must be before or equal to')
    expect(mockedGetActivity).toHaveBeenCalledTimes(callCount)
  })

  it('moves between pages and disables controls at page boundaries', async () => {
    mockedGetActivity
      .mockResolvedValueOnce(page([event()], { page: 0, totalPages: 2, totalElements: 21 }))
      .mockResolvedValueOnce(page([event()], { page: 1, totalPages: 2, totalElements: 21 }))

    renderPage()
    await screen.findByRole('table', { name: 'GitHub webhook activity' })
    const previousButton = screen.getByRole('button', { name: 'Previous page' })
    const nextButton = screen.getByRole('button', { name: 'Next page' })
    expect(previousButton).toBeDisabled()
    expect(nextButton).toBeEnabled()

    fireEvent.click(nextButton)
    await waitFor(() => {
      expect(mockedGetActivity).toHaveBeenLastCalledWith(
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

  it('clears filters without sending empty filter values', async () => {
    renderPage()
    await screen.findByText('No webhook activity has been recorded yet.')

    fireEvent.change(screen.getByLabelText('Event type'), { target: { value: 'push' } })
    fireEvent.click(screen.getByRole('button', { name: 'Apply filters' }))
    fireEvent.click(screen.getByRole('button', { name: 'Clear filters' }))

    await waitFor(() => {
      expect(mockedGetActivity).toHaveBeenLastCalledWith(
        { page: 0, size: 20 },
        expect.any(AbortSignal),
      )
    })
    expect(screen.getByLabelText('Event type')).toHaveValue('')
  })
})
