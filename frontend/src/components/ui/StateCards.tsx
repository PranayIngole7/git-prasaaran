import { AlertTriangle, Loader2 } from 'lucide-react'
import { Button } from './Button'
import { Card } from './Card'

export function LoadingCard({ label }: { label: string }) {
  return (
    <Card
      className="flex items-center gap-3 text-sm text-slate-600"
      role="status"
    >
      <Loader2 className="size-4 motion-safe:animate-spin" />
      {label}
    </Card>
  )
}

export function EmptyCard({ message }: { message: string }) {
  return (
    <Card className="text-center text-sm text-slate-600">
      {message}
    </Card>
  )
}

interface ErrorCardProps {
  title: string
  message: string
  onRetry?: () => void
}

export function ErrorCard({
  title,
  message,
  onRetry,
}: ErrorCardProps) {
  return (
    <Card className="border-red-200 bg-red-50" role="alert">
      <div className="flex items-start gap-3">
        <AlertTriangle className="mt-0.5 size-5 shrink-0 text-red-600" />

        <div className="min-w-0">
          <p className="text-sm font-medium text-red-800">
            {title}
          </p>

          <p className="mt-1 text-sm text-red-700">
            {message}
          </p>

          {onRetry && (
            <Button
              variant="secondary"
              className="mt-3"
              onClick={onRetry}
            >
              Try again
            </Button>
          )}
        </div>
      </div>
    </Card>
  )
}
