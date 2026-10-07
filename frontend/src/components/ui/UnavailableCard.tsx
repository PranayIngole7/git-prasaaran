import { Card } from './Card'

interface UnavailableCardProps {
  title: string
  message: string
}

export function UnavailableCard({ title, message }: UnavailableCardProps) {
  return (
    <Card>
      <div className="space-y-2">
        <h2 className="text-base font-semibold text-slate-900">{title}</h2>
        <p className="text-sm leading-6 text-slate-600">{message}</p>
      </div>
    </Card>
  )
}
