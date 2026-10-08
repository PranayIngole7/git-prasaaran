import { Navigate, createBrowserRouter } from 'react-router-dom'
import { AppLayout } from '../../components/layout/AppLayout'
import { LoginPage } from '../../features/auth/pages/LoginPage'
import { ProtectedRoute } from '../../features/auth/components/ProtectedRoute'
import { DashboardPage } from '../../features/dashboard/pages/DashboardPage'
import { RepositoriesPage } from '../../features/repositories/pages/RepositoriesPage'
import { RepositoryDetailPage } from '../../features/repositories/pages/RepositoryDetailPage'
import { RepositoryDocsPage } from '../../features/documentation/pages/RepositoryDocsPage'
import { DocumentViewerPage } from '../../features/documentation/pages/DocumentViewerPage'
import { ActivityPage } from '../../features/activity/pages/ActivityPage'

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { path: '/', element: <Navigate to="/dashboard" replace /> },
          { path: '/dashboard', element: <DashboardPage /> },
          { path: '/repositories', element: <RepositoriesPage /> },
          {
            path: '/repositories/:repositoryId',
            element: <RepositoryDetailPage />,
          },
          {
            path: '/repositories/:repositoryId/docs',
            element: <RepositoryDocsPage />,
          },
          {
            path: '/repositories/:repositoryId/docs/:slug',
            element: <DocumentViewerPage />,
          },
          { path: '/activity', element: <ActivityPage /> },
          { path: '*', element: <Navigate to="/dashboard" replace /> },
        ],
      },
    ],
  },
])
