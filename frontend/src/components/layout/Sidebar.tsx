import { Activity, Bot, FileLock2, FolderGit2, LayoutDashboard } from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { cn } from '../../lib/utils/cn'

const links = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/repositories', label: 'Repositories', icon: FolderGit2 },
  { to: '/activity', label: 'Activity', icon: Activity },
  { to: '/assistant', label: 'Assistant', icon: Bot },
  { to: '/private-documents', label: 'My documents', icon: FileLock2 },
]

interface SidebarProps {
  open: boolean
  onNavigate: () => void
}

export function Sidebar({ open, onNavigate }: SidebarProps) {
  return (
    <>
      {open && (
        <div
          className="fixed inset-0 top-14 z-10 bg-slate-900/30 lg:hidden"
          onClick={onNavigate}
          aria-hidden
        />
      )}

      <nav
        className={cn(
          'fixed inset-y-0 top-14 z-10 w-60 border-r border-slate-200 bg-white p-3 transition-transform',
          'lg:static lg:translate-x-0',
          open ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <ul className="space-y-1">
          {links.map(({ to, label, icon: Icon }) => (
            <li key={to}>
              <NavLink
                to={to}
                onClick={onNavigate}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-2.5 rounded-md px-3 py-2 text-sm font-medium',
                    isActive
                      ? 'bg-indigo-50 text-indigo-700'
                      : 'text-slate-600 hover:bg-slate-100',
                  )
                }
              >
                <Icon className="size-4" />
                {label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </>
  )
}
