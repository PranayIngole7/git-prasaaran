import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { ProtectedRoute } from './ProtectedRoute'

vi.mock('../hooks/useAuth', () => ({
  useAuth: vi.fn(),
}))

import { useAuth } from '../hooks/useAuth'

const mockedUseAuth = vi.mocked(useAuth)

function renderProtectedRoute() {
  return render(
    <MemoryRouter initialEntries={['/documents']}>
      <Routes>
        <Route element={<ProtectedRoute />}>
          <Route path="/documents" element={<div>Protected content</div>} />
        </Route>
        <Route path="/login" element={<div>Login page</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ProtectedRoute', () => {
  it('redirects unauthenticated users to login', () => {
    mockedUseAuth.mockReturnValue({
      isAuthenticated: false,
      isInitializing: false,
    } as ReturnType<typeof useAuth>)

    renderProtectedRoute()

    expect(screen.getByText('Login page')).toBeInTheDocument()
    expect(screen.queryByText('Protected content')).not.toBeInTheDocument()
  })

  it('renders protected content for authenticated users', () => {
    mockedUseAuth.mockReturnValue({
      isAuthenticated: true,
      isInitializing: false,
    } as ReturnType<typeof useAuth>)

    renderProtectedRoute()

    expect(screen.getByText('Protected content')).toBeInTheDocument()
  })

})
