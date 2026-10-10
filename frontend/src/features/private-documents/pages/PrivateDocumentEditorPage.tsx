import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
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
import {
  createPrivateDocument,
  updatePrivateDocument,
} from '../api/privateDocumentsApi'
import { privateDocumentKeys, usePrivateDocument } from '../hooks/usePrivateDocuments'
import type {
  PrivateDocument,
  PrivateDocumentRequest,
} from '../types/privateDocument'

export function PrivateDocumentEditorPage() {
  const { id: idParam } = useParams<{ id: string }>()
  const id = idParam === undefined ? undefined : Number(idParam)
  const validId = id !== undefined && Number.isSafeInteger(id) && id > 0
    ? id
    : undefined
  const { currentUser } = useAuth()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const documentQuery = usePrivateDocument(currentUser?.email, validId)
  const isEditing = idParam !== undefined
  const mutation = useMutation({
    mutationFn: (request: PrivateDocumentRequest) =>
      validId === undefined
        ? createPrivateDocument(request)
        : updatePrivateDocument(validId, request),
    onSuccess: async (document) => {
      if (currentUser?.email) {
        await queryClient.invalidateQueries({
          queryKey: privateDocumentKeys.all(currentUser.email),
        })
        queryClient.setQueryData(
          privateDocumentKeys.detail(currentUser.email, document.id),
          document,
        )
      }
      navigate(`/private-documents/${document.id}`)
    },
  })

  if (isEditing && validId === undefined) {
    return <EmptyCard message="This private document ID is invalid." />
  }

  if (isEditing && documentQuery.isPending) {
    return <LoadingCard label="Loading private document…" />
  }

  if (isEditing && documentQuery.isError && isNotFoundError(documentQuery.error)) {
    return <EmptyCard message="This private document was not found." />
  }

  if (isEditing && documentQuery.isError) {
    return (
      <ErrorCard
        title="Could not load private document"
        message={getErrorMessage(documentQuery.error)}
        onRetry={() => void documentQuery.refetch()}
      />
    )
  }

  const existing = isEditing ? documentQuery.data : undefined
  if (isEditing && !existing) {
    return <LoadingCard label="Loading private document…" />
  }

  return (
    <PrivateDocumentForm
      key={existing?.id ?? 'new'}
      initial={existing}
      pending={mutation.isPending}
      error={mutation.isError ? getErrorMessage(mutation.error) : undefined}
      onSubmit={(request) => mutation.mutate(request)}
    />
  )
}

function PrivateDocumentForm({
  initial,
  pending,
  error,
  onSubmit,
}: {
  initial: PrivateDocument | undefined
  pending: boolean
  error: string | undefined
  onSubmit: (request: PrivateDocumentRequest) => void
}) {
  const [title, setTitle] = useState(initial?.title ?? '')
  const [slug, setSlug] = useState(initial?.slug ?? '')
  const [content, setContent] = useState(initial?.content ?? '')
  const [validationError, setValidationError] = useState('')
  const navigate = useNavigate()

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const normalizedTitle = title.trim()
    const normalizedSlug = slug.trim().toLowerCase()

    if (!normalizedTitle || !normalizedSlug || !content.trim()) {
      setValidationError('Title, slug, and Markdown content are required.')
      return
    }
    if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(normalizedSlug)) {
      setValidationError('Use lowercase letters, numbers, and single hyphens in the slug.')
      return
    }
    if (normalizedTitle.length > 200 || normalizedSlug.length > 120 || content.length > 100_000) {
      setValidationError('One or more fields exceed the allowed length.')
      return
    }

    setValidationError('')
    onSubmit({
      title: normalizedTitle,
      slug: normalizedSlug,
      content,
    })
  }

  return (
    <>
      <PageHeader
        title={initial ? 'Edit private document' : 'Create private document'}
        description="Only you can access this Markdown document."
      />
      <Card>
        <form className="space-y-4" onSubmit={submit}>
          <div>
            <label htmlFor="private-document-title" className="mb-1.5 block text-sm font-medium text-slate-700">
              Title
            </label>
            <input
              id="private-document-title"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              maxLength={200}
              required
              className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
            />
          </div>
          <div>
            <label htmlFor="private-document-slug" className="mb-1.5 block text-sm font-medium text-slate-700">
              Slug
            </label>
            <input
              id="private-document-slug"
              value={slug}
              onChange={(event) => setSlug(event.target.value)}
              maxLength={120}
              pattern="[a-z0-9]+(?:-[a-z0-9]+)*"
              required
              className="w-full rounded-md border border-slate-300 px-3 py-2 font-mono text-sm outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
            />
          </div>
          <div>
            <label htmlFor="private-document-content" className="mb-1.5 block text-sm font-medium text-slate-700">
              Markdown
            </label>
            <textarea
              id="private-document-content"
              value={content}
              onChange={(event) => setContent(event.target.value)}
              maxLength={100_000}
              rows={16}
              required
              className="w-full resize-y rounded-md border border-slate-300 px-3 py-2 font-mono text-sm outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
            />
          </div>

          {(validationError || error) && (
            <p role="alert" className="text-sm text-red-700">
              {validationError || error}
            </p>
          )}

          <div className="flex gap-2">
            <Button type="submit" disabled={pending}>
              {pending ? 'Saving…' : initial ? 'Save changes' : 'Create document'}
            </Button>
            <Button
              variant="secondary"
              onClick={() => navigate(initial ? `/private-documents/${initial.id}` : '/private-documents')}
            >
              Cancel
            </Button>
          </div>
        </form>
      </Card>
    </>
  )
}
