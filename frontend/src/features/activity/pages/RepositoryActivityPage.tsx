import { useParams } from 'react-router-dom'
import { Badge } from '../../../components/ui/Badge'
import { PageHeader } from '../../../components/ui/PageHeader'
import { PlaceholderCard } from '../../../components/ui/PlaceholderCard'

export function RepositoryActivityPage() {
  const { repositoryId } = useParams<{ repositoryId: string }>()

  return (
    <>
      <PageHeader
        title="Repository activity"
        actions={<Badge tone="info">{repositoryId}</Badge>}
      />

      <PlaceholderCard message="Repository activity will appear here." />
    </>
  )
}
