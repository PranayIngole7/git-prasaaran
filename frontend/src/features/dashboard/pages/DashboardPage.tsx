import { PageHeader } from '../../../components/ui/PageHeader'
import { PlaceholderCard } from '../../../components/ui/PlaceholderCard'

export function DashboardPage() {
  return (
    <>
      <PageHeader
        title="Dashboard"
        description="Overview of your repositories and recent activity."
      />

      <PlaceholderCard message="Dashboard content will appear here." />
    </>
  )
}
