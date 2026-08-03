import { useCallback, useEffect, useRef, useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '@/providers/AuthProvider'
import { get, patch } from '@/lib/api'
import type { NotificationItem, Page } from '@/lib/types'
import { Avatar, Badge, statusTone } from '@/components/ui'

interface NavItem {
  to: string
  label: string
  icon: string
}

const candidateNav: NavItem[] = [
  { to: '/app/dashboard', label: 'Dashboard', icon: '▦' },
  { to: '/app/jobs', label: 'Jobs', icon: '⌗' },
  { to: '/app/interviews', label: 'Interviews', icon: '◉' },
  { to: '/app/coding', label: 'Coding', icon: '⌘' },
  { to: '/app/questions', label: 'Question Bank', icon: '❔' },
  { to: '/app/resumes', label: 'Resumes', icon: '▤' },
  { to: '/app/bookmarks', label: 'Bookmarks', icon: '★' },
  { to: '/app/reports', label: 'Reports', icon: '▦' },
]

const recruiterNav: NavItem[] = [
  { to: '/app/dashboard', label: 'Dashboard', icon: '▦' },
  { to: '/app/company', label: 'My Company', icon: '▣' },
  { to: '/app/jobs', label: 'Jobs', icon: '⌗' },
  { to: '/app/jobs/new', label: 'Post a Job', icon: '＋' },
  { to: '/app/analytics', label: 'Analytics', icon: '◔' },
  { to: '/app/reports', label: 'Reports', icon: '▦' },
]

const adminNav: NavItem[] = [
  { to: '/app/admin', label: 'Dashboard', icon: '▦' },
  { to: '/app/admin/users', label: 'Users', icon: '☺' },
  { to: '/app/admin/subscriptions', label: 'Subscriptions', icon: '◫' },
  { to: '/app/admin/audit', label: 'Audit Logs', icon: '⌨' },
]

export default function AppLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [notifications, setNotifications] = useState<NotificationItem[]>([])
  const [showNotif, setShowNotif] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)
  const notifRef = useRef<HTMLDivElement>(null)

  const isRecruiter = user?.roles.includes('RECRUITER') ?? false
  const isAdmin = user?.roles.includes('ADMIN') ?? false
  const nav = isAdmin ? adminNav : isRecruiter ? recruiterNav : candidateNav

  const loadNotifications = useCallback(async () => {
    try {
      const list = await get<Page<NotificationItem>>('/notifications/me')
      setNotifications(list.content)
    } catch {
      // ignore
    }
  }, [])

  useEffect(() => {
    void loadNotifications()
    const t = setInterval(loadNotifications, 30000)
    return () => clearInterval(t)
  }, [loadNotifications])

  useEffect(() => {
    function onClick(e: MouseEvent) {
      if (notifRef.current && !notifRef.current.contains(e.target as Node)) {
        setShowNotif(false)
      }
    }
    document.addEventListener('mousedown', onClick)
    return () => document.removeEventListener('mousedown', onClick)
  }, [])

  const unread = notifications.filter((n) => !n.read).length

  async function markAllRead() {
    await patch('/notifications/read-all')
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })))
  }

  async function handleLogout() {
    await logout()
    navigate('/login')
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <span className="brand-dot" />
          <span>Harvix AI</span>
        </div>
        <nav className="sidebar-nav">
          {nav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/app/dashboard' || item.to === '/app/admin'}
              className={({ isActive }) => `nav-link ${isActive ? 'nav-active' : ''}`}
            >
              <span className="nav-icon">{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-foot">
          <button className="btn btn-ghost" onClick={handleLogout}>
            ↪ Sign out
          </button>
        </div>
      </aside>

      <div className="app-main">
        <header className="topbar">
          <div className="topbar-title">
            <h1>Harvix AI</h1>
          </div>
          <div className="topbar-actions">
            <div className="notif-wrap" ref={notifRef}>
              <button className="icon-btn" onClick={() => setShowNotif((v) => !v)} aria-label="Notifications">
                🔔
                {unread > 0 ? <span className="notif-dot">{unread}</span> : null}
              </button>
              {showNotif ? (
                <div className="notif-dropdown">
                  <div className="notif-head">
                    <strong>Notifications</strong>
                    <button className="btn btn-ghost btn-sm" onClick={markAllRead}>
                      Mark all read
                    </button>
                  </div>
                  <div className="notif-list">
                    {notifications.length === 0 ? <p className="muted">No notifications</p> : null}
                    {notifications.slice(0, 10).map((n) => (
                      <div key={n.id} className={`notif-item ${n.read ? '' : 'notif-unread'}`}>
                        <Badge tone={statusTone(n.type)}>{n.type}</Badge>
                        <strong>{n.title}</strong>
                        <p>{n.message}</p>
                      </div>
                    ))}
                  </div>
                </div>
              ) : null}
            </div>
            <div className="user-menu" onClick={() => setMenuOpen((v) => !v)}>
              <Avatar name={user ? `${user.firstName} ${user.lastName}` : '?'} />
              <span className="user-name">
                {user?.firstName} {user?.lastName}
              </span>
              {menuOpen ? (
                <div className="user-dropdown">
                  <NavLink to="/app/profile" className="nav-link" onClick={() => setMenuOpen(false)}>
                    Profile
                  </NavLink>
                  <button className="btn btn-ghost" onClick={handleLogout}>
                    Sign out
                  </button>
                </div>
              ) : null}
            </div>
          </div>
        </header>
        <main className="page">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

