import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthContext } from '../../../features/auth/auth-context'
import type { AuthContextValue } from '../../../features/auth/auth-context'
import {
  createPrivateDocument,
  deletePrivateDocument,
  getPrivateDocument,
  getPrivateDocuments,
  updatePrivateDocument,
} from '../api/privateDocumentsApi'
import type { PrivateDocument } from '../types/privateDocument'
import { PrivateDocumentEditorPage } from './PrivateDocumentEditorPage'
import { PrivateDocumentPage } from './PrivateDocumentPage'
import { PrivateDocumentsPage } from './PrivateDocumentsPage'

vi.mock('../api/privateDocumentsApi', () => ({
  createPrivateDocument: vi.fn(),
  deletePrivateDocument: vi.fn(),
  getPrivateDocument: vi.fn(),
  getPrivateDocuments: vi.fn(),
  updatePrivateDocument: vi.fn(),
}))

const mockedCreate = vi.mocked(createPrivateDocument)
const mockedDelete = vi.mocked(deletePrivateDocument)
const mockedGetDocument = vi.mocked(getPrivateDocument)
const mockedGetDocuments = vi.mocked(getPrivateDocuments)
const mockedUpdate = vi.mocked(updatePrivateDocument)

const document: PrivateDocument = {
  id: 12,
  title: 'Personal notes',
  slug: 'personal-notes',
  content: '# Personal notes',
  html: '<h1>Personal notes</h1>\n',
  createdAt: '2026-10-10T10:00:00Z',
  updatedAt: '2026-10-10T10:00:00Z',
}

function renderPrivateDocuments(initialEntry = '/private-documents') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const authValue: AuthContextValue = {
    accessToken: 'token',
    currentUser: { email: 'owner@example.com', roles: ['ROLE_CUSTOMER'] },
    isAuthenticated: true,
    isInitializing: false,
    login: vi.fn(),
    logout: vi.fn(),
  }

  return render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={authValue}>
        <MemoryRouter initialEntries={[initialEntry]}>
          <Routes>
            <Route path="/private-documents" element={<PrivateDocumentsPage />} />
            <Route path="/private-documents/new" element={<PrivateDocumentEditorPage />} />
            <Route path="/private-documents/:id" element={<PrivateDocumentPage />} />
            <Route path="/private-documents/:id/edit" element={<PrivateDocumentEditorPage />} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>
    </QueryClientProvider>,
  )
}

describe('private document pages', () => {
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  beforeEach(() => {
    vi.clearAllMocks()
    mockedGetDocuments.mockResolvedValue([document])
    mockedGetDocument.mockResolvedValue(document)
    mockedCreate.mockResolvedValue(document)
    mockedDelete.mockResolvedValue()
    mockedUpdate.mockResolvedValue(document)
  })

  it('shows only the authenticated user’s private-document response', async () => {
    renderPrivateDocuments()

    expect(await screen.findByRole(
      'heading',
      { name: 'Personal notes' },
      { timeout: 5000 },
    )).toBeInTheDocument()
    expect(screen.getByText('personal-notes')).toBeInTheDocument()
    expect(mockedGetDocuments).toHaveBeenCalledWith(expect.any(AbortSignal))
    expect(screen.getByRole('link', { name: /Personal notes/ }))
      .toHaveAttribute('href', '/private-documents/12')
  })

  it('creates a Markdown document without sending an owner identifier', async () => {
    renderPrivateDocuments('/private-documents/new')

    fireEvent.change(await screen.findByLabelText('Title'), {
      target: { value: 'New notes' },
    })
    fireEvent.change(screen.getByLabelText('Slug'), {
      target: { value: 'new-notes' },
    })
    fireEvent.change(screen.getByLabelText('Markdown'), {
      target: { value: '# New notes' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Create document' }))

    await waitFor(() => {
      expect(mockedCreate).toHaveBeenCalledWith({
        title: 'New notes',
        slug: 'new-notes',
        content: '# New notes',
      })
    })
    expect(await screen.findAllByRole('heading', { name: 'Personal notes' })).toHaveLength(2)
  })

  it('updates an existing private Markdown document', async () => {
    renderPrivateDocuments('/private-documents/12/edit')

    expect(await screen.findByLabelText('Title')).toHaveValue('Personal notes')
    fireEvent.change(screen.getByLabelText('Title'), {
      target: { value: 'Revised notes' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Save changes' }))

    await waitFor(() => {
      expect(mockedUpdate).toHaveBeenCalledWith(12, {
        title: 'Revised notes',
        slug: 'personal-notes',
        content: '# Personal notes',
      })
    })
  })

  it('deletes an owned document and navigates back to the list', async () => {
    vi.stubGlobal('confirm', vi.fn(() => true))
    mockedGetDocuments.mockResolvedValueOnce([])
    renderPrivateDocuments('/private-documents/12')

    expect(await screen.findAllByRole('heading', { name: 'Personal notes' })).toHaveLength(2)
    fireEvent.click(screen.getByRole('button', { name: 'Delete' }))

    await waitFor(() => {
      expect(mockedDelete).toHaveBeenCalledWith(12, expect.anything())
    })
    expect(await screen.findByText('You have not created any private documents yet.'))
      .toBeInTheDocument()
  })
})
