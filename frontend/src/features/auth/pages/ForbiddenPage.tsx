import { Link } from 'react-router-dom'
import { Card } from '../../../components/ui/Card'
import { Button } from '../../../components/ui/Button'

export function ForbiddenPage() {
  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <Card className="w-full max-w-md text-center">
        <h1 className="text-2xl font-semibold text-slate-900">
          Access denied
        </h1>

        <p className="mt-2 text-sm text-slate-600">
          You do not have permission to access this resource.
        </p>

        <Link to="/dashboard" className="mt-6 inline-block">
          <Button>Back to dashboard</Button>
        </Link>
      </Card>
    </div>
  )
}
