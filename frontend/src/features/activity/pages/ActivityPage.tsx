import { useState, type FormEvent } from 'react'
import { Badge } from '../../../components/ui/Badge'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { ErrorCard, EmptyCard, LoadingCard } from '../../../components/ui/StateCards'
import { PageHeader } from '../../../components/ui/PageHeader'
import { getErrorMessage } from '../../../lib/api'
import { useActivity } from '../hooks/useActivity'
import type { ActivityFilters, ActivityStatus } from '../types/activity'

const PAGE_SIZE = 20

interface FilterDraft {
  status: string
  eventType: string
  from: string
  to: string
}

const EMPTY_FILTERS: FilterDraft = {
  status: '',
  eventType: '',
  from: '',
  to: '',
}

const STATUS_LABELS: Record<ActivityStatus, string> = {
  RECEIVED: 'Received',
  PROCESSED: 'Processed',
  FAILED: 'Failed',
}

function formatTimestamp(value: string | null): string {
  if (value === null) {
    return 'Not recorded'
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function statusTone(status: ActivityStatus): 'info' | 'success' | 'warning' {
  if (status === 'PROCESSED') {
    return 'success'
  }
  if (status === 'FAILED') {
    return 'warning'
  }
  return 'info'
}

export function ActivityPage() {
  const [draft, setDraft] = useState<FilterDraft>(EMPTY_FILTERS)
  const [filters, setFilters] = useState<ActivityFilters>({ page: 0, size: PAGE_SIZE })
  const [dateRangeError, setDateRangeError] = useState('')
  const activityQuery = useActivity(filters)
  const response = activityQuery.data
  const currentPage = response?.page ?? filters.page ?? 0
  const totalPages = response?.totalPages ?? 0
  const hasActiveFilters = Boolean(
    filters.status || filters.eventType || filters.from || filters.to,
  )

  function updateDraft(field: keyof FilterDraft, value: string) {
    setDraft((current) => ({ ...current, [field]: value }))
    if (dateRangeError) {
      setDateRangeError('')
    }
  }

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (draft.from && draft.to && draft.from > draft.to) {
      setDateRangeError('The start date and time must be before or equal to the end date and time.')
      return
    }

    const nextFilters: ActivityFilters = { page: 0, size: PAGE_SIZE }
    if (
      draft.status === 'RECEIVED'
      || draft.status === 'PROCESSED'
      || draft.status === 'FAILED'
    ) {
      nextFilters.status = draft.status
    }
    const eventType = draft.eventType.trim()
    if (eventType) {
      nextFilters.eventType = eventType
    }
    if (draft.from) {
      nextFilters.from = new Date(draft.from).toISOString()
    }
    if (draft.to) {
      nextFilters.to = new Date(draft.to).toISOString()
    }

    setDateRangeError('')
    setFilters(nextFilters)
  }

  function clearFilters() {
    setDraft(EMPTY_FILTERS)
    setDateRangeError('')
    setFilters({ page: 0, size: PAGE_SIZE })
  }

  function changePage(page: number) {
    setFilters((current) => ({ ...current, page }))
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Activity"
        description="Recent GitHub webhook activity across configured repositories."
      />

      <Card>
        <form className="space-y-4" onSubmit={applyFilters}>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div>
              <label htmlFor="activity-status" className="mb-1 block text-sm font-medium text-slate-700">
                Status
              </label>
              <select
                id="activity-status"
                value={draft.status}
                onChange={(event) => updateDraft('status', event.target.value)}
                className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus-visible:outline-2 focus-visible:outline-indigo-600"
              >
                <option value="">All statuses</option>
                <option value="RECEIVED">Received</option>
                <option value="PROCESSED">Processed</option>
                <option value="FAILED">Failed</option>
              </select>
            </div>

            <div>
              <label htmlFor="activity-event-type" className="mb-1 block text-sm font-medium text-slate-700">
                Event type
              </label>
              <input
                id="activity-event-type"
                type="text"
                value={draft.eventType}
                onChange={(event) => updateDraft('eventType', event.target.value)}
                placeholder="e.g. push"
                maxLength={100}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900 focus-visible:outline-2 focus-visible:outline-indigo-600"
              />
            </div>

            <div>
              <label htmlFor="activity-from" className="mb-1 block text-sm font-medium text-slate-700">
                From
              </label>
              <input
                id="activity-from"
                type="datetime-local"
                value={draft.from}
                onChange={(event) => updateDraft('from', event.target.value)}
                className="w-full min-w-0 rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900 focus-visible:outline-2 focus-visible:outline-indigo-600"
              />
            </div>

            <div>
              <label htmlFor="activity-to" className="mb-1 block text-sm font-medium text-slate-700">
                To
              </label>
              <input
                id="activity-to"
                type="datetime-local"
                value={draft.to}
                onChange={(event) => updateDraft('to', event.target.value)}
                className="w-full min-w-0 rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900 focus-visible:outline-2 focus-visible:outline-indigo-600"
              />
            </div>
          </div>

          {dateRangeError && (
            <p className="text-sm text-red-700" role="alert">
              {dateRangeError}
            </p>
          )}

          <div className="flex flex-wrap gap-2">
            <Button type="submit">Apply filters</Button>
            <Button type="button" variant="secondary" onClick={clearFilters}>
              Clear filters
            </Button>
          </div>
        </form>
      </Card>

      {activityQuery.isLoading && <LoadingCard label="Loading activity…" />}

      {activityQuery.isError && (
        <ErrorCard
          title="Could not load activity"
          message={getErrorMessage(activityQuery.error)}
          onRetry={() => void activityQuery.refetch()}
        />
      )}

      {response && response.items.length === 0 && response.totalElements === 0 && (
        hasActiveFilters
          ? <EmptyCard message="No activity matches these filters. Try changing or clearing them." />
          : <EmptyCard message="No webhook activity has been recorded yet." />
      )}

      {response && response.items.length > 0 && (
        <Card className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] border-collapse text-left text-sm">
              <caption className="sr-only">GitHub webhook activity</caption>
              <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-600">
                <tr>
                  <th scope="col" className="px-4 py-3">Event type</th>
                  <th scope="col" className="px-4 py-3">Delivery ID</th>
                  <th scope="col" className="px-4 py-3">Status</th>
                  <th scope="col" className="px-4 py-3">Commit SHA</th>
                  <th scope="col" className="px-4 py-3">Created</th>
                  <th scope="col" className="px-4 py-3">Processed</th>
                  <th scope="col" className="px-4 py-3">Repository</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {response.items.map((item) => (
                  <tr key={item.id} className="align-top text-slate-700">
                    <td className="px-4 py-3 font-medium text-slate-900">{item.eventType}</td>
                    <td className="max-w-56 break-all px-4 py-3 font-mono text-xs">{item.deliveryId}</td>
                    <td className="px-4 py-3">
                      <Badge tone={statusTone(item.status)}>{STATUS_LABELS[item.status]}</Badge>
                    </td>
                    <td className="px-4 py-3 font-mono text-xs">
                      {item.commitSha ?? 'Not recorded'}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3">
                      <time dateTime={item.createdAt}>{formatTimestamp(item.createdAt)}</time>
                    </td>
                    <td className="whitespace-nowrap px-4 py-3">
                      {item.processedAt
                        ? <time dateTime={item.processedAt}>{formatTimestamp(item.processedAt)}</time>
                        : 'Not recorded'}
                    </td>
                    <td className="px-4 py-3">
                      {item.repositoryId === null ? 'Unassociated' : `Repository #${item.repositoryId}`}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="flex flex-col gap-3 border-t border-slate-200 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
            <p className="text-sm text-slate-600" aria-live="polite">
              {response.totalElements} {response.totalElements === 1 ? 'event' : 'events'}
              {' · '}
              {totalPages === 0 ? 'No pages' : `Page ${currentPage + 1} of ${totalPages}`}
            </p>
            <div className="flex gap-2">
              <Button
                type="button"
                variant="secondary"
                onClick={() => changePage(currentPage - 1)}
                disabled={currentPage <= 0 || activityQuery.isFetching}
                aria-label="Previous page"
              >
                Previous
              </Button>
              <Button
                type="button"
                variant="secondary"
                onClick={() => changePage(currentPage + 1)}
                disabled={totalPages === 0 || currentPage >= totalPages - 1 || activityQuery.isFetching}
                aria-label="Next page"
              >
                Next
              </Button>
            </div>
          </div>
        </Card>
      )}
    </div>
  )
}
