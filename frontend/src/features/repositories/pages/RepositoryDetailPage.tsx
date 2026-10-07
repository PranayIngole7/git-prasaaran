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
        description="Repository details."
        actions={<Badge tone="info">{repositoryId}</Badge>}
      />

      <Card className="flex gap-4 text-sm">
        <Link
          className="text-indigo-600 hover:underline"
          to={`/repositories/${repositoryId}/docs`}
        >
          Documentation
        </Link>

        <Link
          className="text-indigo-600 hover:underline"
          to={`/repositories/${repositoryId}/activity`}
        >
          Activity
        </Link>
      </Card>
    </>
  )
}
