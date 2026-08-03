import type { ButtonHTMLAttributes, CSSProperties, InputHTMLAttributes, MouseEventHandler, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react'
import { useEffect } from 'react'

export function Button({
  variant = 'primary',
  loading,
  children,
  className = '',
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' | 'ghost' | 'danger'; loading?: boolean }) {
  return (
    <button className={`btn btn-${variant} ${className}`} disabled={loading || props.disabled} {...props}>
      {loading ? <span className="spinner" /> : null}
      {children}
    </button>
  )
}

export function Input(props: InputHTMLAttributes<HTMLInputElement>) {
  return <input className="input" {...props} />
}

export function Select(props: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select className="input" {...props} />
}

export function Textarea(props: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className="input" {...props} />
}

export function Field({ label, children, hint }: { label: string; children: ReactNode; hint?: string }) {
  return (
    <label className="field">
      <span className="field-label">{label}</span>
      {children}
      {hint ? <span className="field-hint">{hint}</span> : null}
    </label>
  )
}

export function Card({ children, className = '', title, actions, style, onClick }: { children: ReactNode; className?: string; title?: string; actions?: ReactNode; style?: CSSProperties; onClick?: MouseEventHandler<HTMLDivElement> }) {
  return (
    <div className={`card ${className}`} style={style} onClick={onClick}>
      {title || actions ? (
        <div className="card-head">
          {title ? <h3 className="card-title">{title}</h3> : null}
          {actions ? <div className="card-actions">{actions}</div> : null}
        </div>
      ) : null}
      {children}
    </div>
  )
}

const badgeTones: Record<string, string> = {
  success: 'badge-success',
  warning: 'badge-warning',
  danger: 'badge-danger',
  info: 'badge-info',
  muted: 'badge-muted',
}

export function Badge({ tone = 'muted', children }: { tone?: keyof typeof badgeTones | string; children: ReactNode }) {
  return <span className={`badge ${badgeTones[tone] ?? 'badge-muted'}`}>{children}</span>
}

export function statusTone(status?: string): string {
  if (!status) return 'muted'
  const s = status.toUpperCase()
  if (['ACTIVE', 'PUBLISHED', 'COMPLETED', 'OFFERED', 'HIRED', 'ACCEPTED', 'VERIFIED', 'SUCCESS', 'SHORTLISTED'].includes(s)) return 'success'
  if (['PENDING', 'PENDING_VERIFICATION', 'REVIEWING', 'SCHEDULED', 'IN_PROGRESS', 'SUBMITTED', 'INTERVIEW'].includes(s)) return 'warning'
  if (['BLOCKED', 'REJECTED', 'CLOSED', 'EXPIRED', 'FAILED', 'ERROR', 'CANCELLED', 'WITHDRAWN'].includes(s)) return 'danger'
  if (['INTERVIEWED', 'AI', 'RECRUITER', 'CANDIDATE', 'ADMIN'].includes(s)) return 'info'
  return 'muted'
}

export function StatusBadge({ status }: { status?: string }) {
  return <Badge tone={statusTone(status)}>{status ?? '—'}</Badge>
}

export function Spinner({ label }: { label?: string }) {
  return (
    <div className="spinner-wrap">
      <span className="spinner large" />
      {label ? <p className="muted">{label}</p> : null}
    </div>
  )
}

export function EmptyState({ title, message, hint, action }: { title: string; message?: string; hint?: string; action?: ReactNode }) {
  return (
    <div className="empty">
      <div className="empty-icon">◍</div>
      <h3>{title}</h3>
      {message ? <p className="muted">{message}</p> : null}
      {hint ? <p className="muted">{hint}</p> : null}
      {action}
    </div>
  )
}

export function Modal({ open, title, onClose, children }: { open: boolean; title: string; onClose: () => void; children: ReactNode }) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    if (open) window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  if (!open) return null
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-head">
          <h3>{title}</h3>
          <button className="btn-close" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  )
}

export function StatCard({ label, value, icon, sub }: { label: string; value: ReactNode; icon?: string; sub?: string }) {
  return (
    <div className="card stat">
      {icon ? <div className="stat-icon">{icon}</div> : null}
      <div>
        <p className="stat-label">{label}</p>
        <p className="stat-value">{value}</p>
        {sub ? <p className="stat-sub">{sub}</p> : null}
      </div>
    </div>
  )
}

export function Avatar({ name, color }: { name?: string; color?: string }) {
  const initials = (name ?? '?')
    .split(' ')
    .map((p) => p[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()
  return (
    <span className="avatar" style={color ? { background: color } : undefined}>
      {initials}
    </span>
  )
}

export function Pagination({ page, totalPages, onPage }: { page: number; totalPages: number; onPage: (p: number) => void }) {
  if (totalPages <= 1) return null
  return (
    <div className="pagination">
      <button className="btn btn-secondary" disabled={page <= 0} onClick={() => onPage(page - 1)}>
        ← Prev
      </button>
      <span className="pagination-info">
        Page {page + 1} of {totalPages}
      </span>
      <button className="btn btn-secondary" disabled={page >= totalPages - 1} onClick={() => onPage(page + 1)}>
        Next →
      </button>
    </div>
  )
}

export function Tabs({ tabs, active, onChange }: { tabs: string[]; active: string; onChange: (t: string) => void }) {
  return (
    <div className="tabs">
      {tabs.map((t) => (
        <button key={t} className={`tab ${active === t ? 'tab-active' : ''}`} onClick={() => onChange(t)}>
          {t}
        </button>
      ))}
    </div>
  )
}

export function Skeleton({ lines = 3 }: { lines?: number }) {
  return (
    <div className="skeleton-wrap">
      {Array.from({ length: lines }).map((_, i) => (
        <div key={i} className="skeleton-line" style={{ width: `${90 - i * 12}%` }} />
      ))}
    </div>
  )
}
