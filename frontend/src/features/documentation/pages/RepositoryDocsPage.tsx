import { FileText } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { Badge } from '../../../components/ui/Badge'
import { Card } from '../../../components/ui/Card'
import { PageHeader } from '../../../components/ui/PageHeader'
import {
  EmptyCard,
  ErrorCard,
  LoadingCard,
} from '../../../components/ui/StateCards'
import { getErrorMessage } from '../../../lib/api'
import { useDocuments } from '../hooks/useDocuments'

export function RepositoryDocsPage() {
  const { repositoryId } = useParams<{ repositoryId: string }>()

  const {
    data,
    isPending,
    isError,
    error,
    refetch,
  } = useDocuments()

  return (
    <>
      <PageHeader
        title="Documentation"
        description="Documents served by the Git-Prasaaran backend."
        actions={
          repositoryId ? (
            <Badge tone="info">{repositoryId}</Badge>
          ) : undefined
        }
      />

      {isPending && (
        <LoadingCard label="Loading documents…" />
      )}

      {isError && (
        <ErrorCard
          title="Could not load documents"
          message={getErrorMessage(error)}
          onRetry={() => void refetch()}
        />
      )}

      {data && data.length === 0 && (
        <EmptyCard message="No documents are available yet." />
      )}

      {data && data.length > 0 && (
        <ul className="grid gap-3 sm:grid-cols-2">
          {data.map((doc) => (
            <li key={doc.slug}>
              <Link
                to={`/repositories/${repositoryId}/docs/${encodeURIComponent(doc.slug)}`}
                className="block h-full rounded-lg focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"
              >
                <Card className="h-full transition-colors hover:border-indigo-300">
                  <div className="flex items-start gap-3">
                    <FileText className="mt-0.5 size-5 shrink-0 text-indigo-600" />

                    <div className="min-w-0">
                      <h2 className="truncate font-medium">
                        {doc.title}
                      </h2>

                      {doc.description && (
                        <p className="mt-1 text-sm text-slate-600">
                          {doc.description}
                        </p>
                      )}

                      <p className="mt-2 truncate font-mono text-xs text-slate-500">
                        {doc.sourcePath}
                      </p>
                    </div>
                  </div>
                </Card>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
