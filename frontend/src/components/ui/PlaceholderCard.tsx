import { Card } from './Card'
import { Badge } from './Badge'

export function PlaceholderCard({ message }: { message: string }) {
  return (
    <Card className="border-dashed text-center">
      <Badge tone="warning">Placeholder</Badge>
      <p className="mt-3 text-sm text-slate-600">{message}</p>
    </Card>
  )
}
