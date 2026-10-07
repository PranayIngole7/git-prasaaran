import { useParams } from 'react-router-dom'
import { Badge } from '../../../components/ui/Badge'
import { PageHeader } from '../../../components/ui/PageHeader'
import { PlaceholderCard } from '../../../components/ui/PlaceholderCard'

export function RepositoryDocsPage() {
  const { repositoryId } = useParams<{ repositoryId: string }>()

  return (
    <>
      <PageHeader
        title="Documentation"
        actions={<Badge tone="info">{repositoryId}</Badge>}
      />

      <PlaceholderCard message="Generated documentation will appear here." />
    </>
  )
}
