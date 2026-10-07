import { Activity } from 'lucide-react'
import { Card } from '../../../components/ui/Card'
import { PageHeader } from '../../../components/ui/PageHeader'

export function RepositoryActivityPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Repository Activity"
        description="Activity for this repository."
      />

      <Card>
        <Activity className="mb-3 h-5 w-5 text-slate-600" />
        <h2 className="font-semibold text-slate-900">
          Activity API not connected
        </h2>
        <p className="mt-1 text-sm leading-6 text-slate-600">
          Repository activity will be connected when the backend exposes the
          required activity endpoint.
        </p>
      </Card>
    </div>
  )
}
