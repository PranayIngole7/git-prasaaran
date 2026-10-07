import { Activity } from 'lucide-react'
import { Card } from '../../../components/ui/Card'
import { PageHeader } from '../../../components/ui/PageHeader'

export function ActivityPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Activity"
        description="Webhook and repository activity."
      />

      <Card>
        <Activity className="mb-3 h-5 w-5 text-slate-600" />
        <h2 className="font-semibold text-slate-900">
          Activity API not connected
        </h2>
        <p className="mt-1 text-sm leading-6 text-slate-600">
          Activity data will appear here once the backend activity API is
          implemented. The current backend already processes GitHub webhook
          events, but does not expose an activity feed endpoint.
        </p>
      </Card>
    </div>
  )
}
