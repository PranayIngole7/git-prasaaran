import { PageHeader } from '../../../components/ui/PageHeader'
import { PlaceholderCard } from '../../../components/ui/PlaceholderCard'

export function RepositoriesPage() {
  return (
    <>
      <PageHeader
        title="Repositories"
        description="Repositories connected to Git-Prasaaran."
      />

      <PlaceholderCard message="Repository list will appear here." />
    </>
  )
}
