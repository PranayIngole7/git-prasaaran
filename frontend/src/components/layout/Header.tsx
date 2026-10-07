import { GitBranch, Menu } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Button } from '../ui/Button'

export function Header({ onMenuClick }: { onMenuClick: () => void }) {
  return (
    <header className="sticky top-0 z-20 flex h-14 items-center gap-3 border-b border-slate-200 bg-white px-4">
      <Button
        variant="ghost"
        className="lg:hidden"
        onClick={onMenuClick}
        aria-label="Toggle navigation"
      >
        <Menu className="size-5" />
      </Button>

      <Link
        to="/dashboard"
        className="flex items-center gap-2 font-semibold tracking-tight"
      >
        <GitBranch className="size-5 text-indigo-600" />
        Git-Prasaaran
      </Link>
    </header>
  )
}
