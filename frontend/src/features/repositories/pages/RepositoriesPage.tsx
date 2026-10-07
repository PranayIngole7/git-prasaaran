import { Link } from 'react-router-dom'
import { BookOpen } from 'lucide-react'
import { Card } from '../../../components/ui/Card'
import { PageHeader } from '../../../components/ui/PageHeader'

export function RepositoriesPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Repositories"
        description="Repositories connected to Git-Prasaaran."
      />

      <Card>
        <BookOpen className="mb-3 h-5 w-5 text-slate-600" />
        <h2 className="font-semibold text-slate-900">git-prasaaran</h2>
        <p className="mt-1 text-sm text-slate-600">
          Documentation is currently available for the Git-Prasaaran project.
          Repository metadata and management APIs will be added separately.
        </p>
        <Link
          to="/repositories/git-prasaaran/docs"
          className="mt-4 inline-block text-sm font-medium text-slate-900 underline"
        >
          View documentation
        </Link>
      </Card>
    </div>
  )
}
