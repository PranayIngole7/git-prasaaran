import { ArrowLeft } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { Card } from '../../../components/ui/Card'
import { PageHeader } from '../../../components/ui/PageHeader'
import {
  EmptyCard,
  ErrorCard,
  LoadingCard,
} from '../../../components/ui/StateCards'
import {
  getErrorMessage,
  isNotFoundError,
} from '../../../lib/api'
import { useDocument } from '../hooks/useDocuments'

export function DocumentViewerPage() {
  const {
    repositoryId,
    slug,
  } = useParams<{
    repositoryId: string
    slug: string
  }>()

  const {
    data: doc,
    isPending,
    isError,
    error,
    refetch,
  } = useDocument(slug)

  const backTo = `/repositories/${repositoryId}/docs`

  return (
    <>
      <Link
        to={backTo}
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-indigo-600 hover:underline"
      >
        <ArrowLeft className="size-4" />
        All documents
      </Link>

      {isPending && (
        <LoadingCard label="Loading document…" />
      )}

      {isError && isNotFoundError(error) && (
        <EmptyCard
          message={`No document found with the slug "${slug}".`}
        />
      )}

      {isError && !isNotFoundError(error) && (
        <ErrorCard
          title="Could not load document"
          message={getErrorMessage(error)}
          onRetry={() => void refetch()}
        />
      )}

      {doc && (
        <>
          <PageHeader
            title={doc.title}
            description={doc.sourcePath}
          />

          <Card className="p-5 sm:p-8">
            {doc.html ? (
              // SECURITY ASSUMPTION: `html` is generated and sanitized server-side
              // with the OWASP HTML Sanitizer. Never render `doc.content` this way.
              <article
                className="doc-content"
                dangerouslySetInnerHTML={{ __html: doc.html }}
              />
            ) : (
              <p className="text-sm text-slate-600">
                This document has no rendered content.
              </p>
            )}
          </Card>
        </>
      )}
    </>
  )
}
