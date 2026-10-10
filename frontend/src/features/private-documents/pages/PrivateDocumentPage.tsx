import { ArrowLeft, Pencil, Trash2 } from 'lucide-react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { PageHeader } from '../../../components/ui/PageHeader'
import {
  EmptyCard,
  ErrorCard,
  LoadingCard,
} from '../../../components/ui/StateCards'
import { getErrorMessage, isNotFoundError } from '../../../lib/api'
import { useAuth } from '../../auth/hooks/useAuth'
import { deletePrivateDocument } from '../api/privateDocumentsApi'
import { privateDocumentKeys, usePrivateDocument } from '../hooks/usePrivateDocuments'

export function PrivateDocumentPage() {
  const { id: idParam } = useParams<{ id: string }>()
  const id = Number(idParam)
  const validId = Number.isSafeInteger(id) && id > 0 ? id : undefined
  const { currentUser } = useAuth()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const {
    data: document,
    isPending,
    isError,
    error,
    refetch,
  } = usePrivateDocument(currentUser?.email, validId)
  const deleteMutation = useMutation({
    mutationFn: deletePrivateDocument,
    onSuccess: async () => {
      if (currentUser?.email) {
        await queryClient.invalidateQueries({
          queryKey: privateDocumentKeys.all(currentUser.email),
        })
      }
      navigate('/private-documents')
    },
  })

  function handleDelete() {
    if (validId && window.confirm('Delete this private document?')) {
      deleteMutation.mutate(validId)
    }
  }

  return (
    <>
      <Link
        to="/private-documents"
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-indigo-600 hover:underline"
      >
        <ArrowLeft className="size-4" />
        My private documents
      </Link>

      {isPending && <LoadingCard label="Loading private document…" />}

      {isError && isNotFoundError(error) && (
        <EmptyCard message="This private document was not found." />
      )}

      {isError && !isNotFoundError(error) && (
        <ErrorCard
          title="Could not load private document"
          message={getErrorMessage(error)}
          onRetry={() => void refetch()}
        />
      )}

      {document && (
        <>
          <PageHeader
            title={document.title}
            description={`Private document · ${document.slug}`}
            actions={
              <div className="flex gap-2">
                <Link to={`/private-documents/${document.id}/edit`}>
                  <span className="inline-flex items-center justify-center gap-2 rounded-md border border-slate-300 bg-white px-3.5 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50">
                    <Pencil className="size-4" />
                    Edit
                  </span>
                </Link>
                <Button
                  variant="secondary"
                  onClick={handleDelete}
                  disabled={deleteMutation.isPending}
                >
                  <Trash2 className="size-4" />
                  {deleteMutation.isPending ? 'Deleting…' : 'Delete'}
                </Button>
              </div>
            }
          />
          {deleteMutation.isError && (
            <ErrorCard
              title="Could not delete private document"
              message={getErrorMessage(deleteMutation.error)}
            />
          )}
          <Card className="p-5 sm:p-8">
            {document.html ? (
              <article
                className="doc-content"
                dangerouslySetInnerHTML={{ __html: document.html }}
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
