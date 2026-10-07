import { BookOpen } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { Badge } from '../../../components/ui/Badge'
import { Card } from '../../../components/ui/Card'
import { PageHeader } from '../../../components/ui/PageHeader'

export function RepositoryDetailPage() {
  const { repositoryId } = useParams<{ repositoryId: string }>()

  return (
    <>
      <PageHeader
        title="Repository"
        actions={
          <Badge tone="info">{repositoryId}</Badge>
        }
      />

      <Card>
        <Link
          to={`/repositories/${repositoryId}/docs`}
          className="flex items-center gap-2 text-sm font-medium text-indigo-600 hover:underline"
        >
          <BookOpen className="size-4" />
          Documentation
        </Link>

        <p className="mt-3 text-sm text-slate-600">
          Repository metadata is not available yet because the backend has no repository API.
        </p>
      </Card>
    </>
  )
}
