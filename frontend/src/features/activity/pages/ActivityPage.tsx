import { PageHeader } from '../../../components/ui/PageHeader'
import { PlaceholderCard } from '../../../components/ui/PlaceholderCard'

export function ActivityPage() {
  return (
    <>
      <PageHeader
        title="Activity"
        description="Recent activity across your repositories."
      />

      <PlaceholderCard message="Activity feed will appear here." />
    </>
  )
}
