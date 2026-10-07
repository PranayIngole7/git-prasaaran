import { GitBranch } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'

export function LoginPage() {
  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <Card className="w-full max-w-sm text-center">
        <GitBranch className="mx-auto size-8 text-indigo-600" />

        <h1 className="mt-3 text-xl font-semibold">
          Sign in to Git-Prasaaran
        </h1>

        <p className="mt-1 text-sm text-slate-600">
          Authentication is not implemented yet.
        </p>

        <Link to="/dashboard" className="mt-5 block">
          <Button className="w-full">
            Continue to dashboard
          </Button>
        </Link>
      </Card>
    </div>
  )
}
