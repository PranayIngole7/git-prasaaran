import { Link } from 'react-router-dom'
import { Activity, BookOpen, GitBranch } from 'lucide-react'
import { Card } from '../../../components/ui/Card'
import { PageHeader } from '../../../components/ui/PageHeader'

const linkClass =
  'mt-4 inline-block text-sm font-medium text-indigo-700 underline underline-offset-4 hover:text-indigo-900'

export function DashboardPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Dashboard"
        description="Git-Prasaaran developer workspace."
      />

      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <BookOpen className="mb-3 h-5 w-5 text-slate-600" />
          <h2 className="font-semibold text-slate-900">Documentation</h2>
          <p className="mt-1 text-sm text-slate-600">
            Browse documentation published from your configured Git repository.
          </p>
          <Link to="/repositories" className={linkClass}>
            Browse repositories and documentation
          </Link>
        </Card>

        <Card>
          <GitBranch className="mb-3 h-5 w-5 text-slate-600" />
          <h2 className="font-semibold text-slate-900">Repositories</h2>
          <p className="mt-1 text-sm text-slate-600">
            View configured repositories and access their documentation.
          </p>
          <Link to="/repositories" className={linkClass}>
            View repositories
          </Link>
        </Card>

        <Card>
          <Activity className="mb-3 h-5 w-5 text-slate-600" />
          <h2 className="font-semibold text-slate-900">Activity</h2>
          <p className="mt-1 text-sm text-slate-600">
            Review available repository activity and recent events.
          </p>
          <Link to="/activity" className={linkClass}>
            View activity
          </Link>
        </Card>
      </div>
    </div>
  )
}