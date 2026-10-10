import { FileText, Plus } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Card } from '../../../components/ui/Card'
import { PageHeader } from '../../../components/ui/PageHeader'
import {
  EmptyCard,
  ErrorCard,
  LoadingCard,
} from '../../../components/ui/StateCards'
import { getErrorMessage } from '../../../lib/api'
import { useAuth } from '../../auth/hooks/useAuth'
import { usePrivateDocuments } from '../hooks/usePrivateDocuments'

export function PrivateDocumentsPage() {
  const { currentUser } = useAuth()
  const { data, isPending, isError, error, refetch } =
    usePrivateDocuments(currentUser?.email)

  return (
    <>
      <PageHeader
        title="My private documents"
        description="Markdown documents visible only to your account."
        actions={
          <Link
            to="/private-documents/new"
            className="rounded-md focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"
          >
            <span className="inline-flex items-center justify-center gap-2 rounded-md bg-indigo-600 px-3.5 py-2 text-sm font-medium text-white transition-colors hover:bg-indigo-700">
              <Plus className="size-4" />
              Create document
            </span>
          </Link>
        }
      />

      {isPending && <LoadingCard label="Loading your private documents…" />}

      {isError && (
        <ErrorCard
          title="Could not load your private documents"
          message={getErrorMessage(error)}
          onRetry={() => void refetch()}
        />
      )}

      {data && data.length === 0 && (
        <EmptyCard message="You have not created any private documents yet." />
      )}

      {data && data.length > 0 && (
        <ul className="grid gap-3 sm:grid-cols-2">
          {data.map((document) => (
            <li key={document.id}>
              <Link
                to={`/private-documents/${document.id}`}
                className="block h-full rounded-lg focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"
              >
                <Card className="h-full transition-colors hover:border-indigo-300">
                  <div className="flex items-start gap-3">
                    <FileText className="mt-0.5 size-5 shrink-0 text-indigo-600" />
                    <div className="min-w-0">
                      <h2 className="truncate font-medium">{document.title}</h2>
                      <p className="mt-1 truncate font-mono text-xs text-slate-500">
                        {document.slug}
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
