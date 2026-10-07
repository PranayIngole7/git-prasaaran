import { Link } from 'react-router-dom'
import { BookOpen, GitBranch, Activity } from 'lucide-react'
import { Card } from '../../../components/ui/Card'
import { PageHeader } from '../../../components/ui/PageHeader'

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
            Browse documentation served directly from the GitHub-backed API.
          </p>
          <Link
            to="/repositories/git-prasaaran/docs"
            className="mt-4 inline-block text-sm font-medium text-slate-900 underline"
          >
            Open documentation
          </Link>
        </Card>

        <Card>
          <GitBranch className="mb-3 h-5 w-5 text-slate-600" />
          <h2 className="font-semibold text-slate-900">Repositories</h2>
          <p className="mt-1 text-sm text-slate-600">
            Repository management will be connected when its backend API is
            implemented.
          </p>
        </Card>

        <Card>
          <Activity className="mb-3 h-5 w-5 text-slate-600" />
          <h2 className="font-semibold text-slate-900">Activity</h2>
          <p className="mt-1 text-sm text-slate-600">
            Activity data will be connected when its backend API is available.
          </p>
        </Card>
      </div>
    </div>
  )
}
