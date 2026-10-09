import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Badge } from '../../../components/ui/Badge'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { ErrorCard, EmptyCard, LoadingCard } from '../../../components/ui/StateCards'
import { PageHeader } from '../../../components/ui/PageHeader'
import { getErrorMessage } from '../../../lib/api'
import { useRepositoryActivity } from '../hooks/useActivity'
import type { ActivityStatus } from '../types/activity'

const PAGE_SIZE = 20

const STATUS_LABELS: Record<ActivityStatus, string> = {
  RECEIVED: 'Received',
  PROCESSED: 'Processed',
  FAILED: 'Failed',
}

function parseRepositoryId(value: string | undefined): number | undefined {
  if (!value || !/^\d+$/.test(value)) {
    return undefined
  }

  const repositoryId = Number(value)
  return Number.isSafeInteger(repositoryId) && repositoryId > 0
    ? repositoryId
    : undefined
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

export function RepositoryActivityPage() {
  const { repositoryId: routeRepositoryId } = useParams<{ repositoryId: string }>()
  const repositoryId = parseRepositoryId(routeRepositoryId)
  const [page, setPage] = useState(0)
  const activityQuery = useRepositoryActivity(
    repositoryId,
    { page, size: PAGE_SIZE },
  )
  const response = activityQuery.data
  const currentPage = response?.page ?? page
  const totalPages = response?.totalPages ?? 0

  return (
    <div className="space-y-6">
      <PageHeader
        title="Repository Activity"
        description={repositoryId
          ? `Recent webhook activity for repository #${repositoryId}.`
          : 'Recent webhook activity for this repository.'}
        actions={repositoryId && (
          <Link
            to={`/repositories/${repositoryId}`}
            className="text-sm font-medium text-indigo-600 hover:underline"
          >
            Back to repository
          </Link>
        )}
      />

      {!repositoryId && (
        <ErrorCard
          title="Invalid repository"
          message="The repository ID in this URL must be a positive whole number."
        />
      )}

      {repositoryId && activityQuery.isLoading && (
        <LoadingCard label="Loading repository activity…" />
      )}

      {repositoryId && activityQuery.isError && (
        <ErrorCard
          title="Could not load repository activity"
          message={getErrorMessage(activityQuery.error)}
          onRetry={() => void activityQuery.refetch()}
        />
      )}

      {response && response.items.length === 0 && (
        <EmptyCard message="No activity has been recorded for this repository yet." />
      )}

      {response && response.items.length > 0 && (
        <Card className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[800px] border-collapse text-left text-sm">
              <caption className="sr-only">
                Webhook activity for repository #{repositoryId}
              </caption>
              <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-600">
                <tr>
                  <th scope="col" className="px-4 py-3">Event type</th>
                  <th scope="col" className="px-4 py-3">Status</th>
                  <th scope="col" className="px-4 py-3">Delivery ID</th>
                  <th scope="col" className="px-4 py-3">Commit SHA</th>
                  <th scope="col" className="px-4 py-3">Created</th>
                  <th scope="col" className="px-4 py-3">Processed</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {response.items.map((item) => (
                  <tr key={item.id} className="align-top text-slate-700">
                    <td className="px-4 py-3 font-medium text-slate-900">{item.eventType}</td>
                    <td className="px-4 py-3">
                      <Badge tone={statusTone(item.status)}>{STATUS_LABELS[item.status]}</Badge>
                    </td>
                    <td className="max-w-56 break-all px-4 py-3 font-mono text-xs">{item.deliveryId}</td>
                    <td className="px-4 py-3 font-mono text-xs">{item.commitSha ?? 'Not recorded'}</td>
                    <td className="whitespace-nowrap px-4 py-3">
                      <time dateTime={item.createdAt}>{formatTimestamp(item.createdAt)}</time>
                    </td>
                    <td className="whitespace-nowrap px-4 py-3">
                      {item.processedAt
                        ? <time dateTime={item.processedAt}>{formatTimestamp(item.processedAt)}</time>
                        : 'Not recorded'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {repositoryId && (response || activityQuery.isLoading) && (
        <Card className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-sm text-slate-600" aria-live="polite">
            {response
              ? <>
                  {response.totalElements} {response.totalElements === 1 ? 'event' : 'events'}
                  {' · '}
                  {totalPages === 0 ? 'No pages' : `Page ${currentPage + 1} of ${totalPages}`}
                </>
              : `Loading page ${page + 1}…`}
          </p>
          <div className="flex gap-2">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setPage(currentPage - 1)}
              disabled={!response || currentPage <= 0 || activityQuery.isFetching}
              aria-label="Previous page"
            >
              Previous
            </Button>
            <Button
              type="button"
              variant="secondary"
              onClick={() => setPage(currentPage + 1)}
              disabled={!response || totalPages === 0 || currentPage >= totalPages - 1 || activityQuery.isFetching}
              aria-label="Next page"
            >
              Next
            </Button>
          </div>
        </Card>
      )}
    </div>
  )
}
