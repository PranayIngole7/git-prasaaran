import { GitBranch, LogOut, Menu } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { Button } from '../ui/Button'
import { useAuth } from '../../features/auth/hooks/useAuth'

export function Header({ onMenuClick }: { onMenuClick: () => void }) {
  const { currentUser, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

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

      <div className="ml-auto flex items-center gap-3">
        {currentUser?.email && (
          <span className="hidden text-sm text-slate-600 sm:inline">
            {currentUser.email}
          </span>
        )}

        <Button
          variant="ghost"
          onClick={handleLogout}
          aria-label="Log out"
          title="Log out"
        >
          <LogOut className="mr-2 size-4" />
          Log out
        </Button>
      </div>
    </header>
  )
}