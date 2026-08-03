import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Card, Skeleton, StatCard, StatusBadge } from '@/components/ui'
import { get } from '@/lib/api'
import type { Application, DashboardStats, Interview } from '@/lib/types'

export default function CandidateDashboard() {
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [interviews, setInterviews] = useState<Interview[]>([])
  const [apps, setApps] = useState<Application[]>([])

  useEffect(() => {
    void get<DashboardStats>('/users/me/dashboard').then(setStats).catch(() => {})
    void get<{ content: Interview[] }>('/interviews/me').then((p) => setInterviews(p.content)).catch(() => {})
    void get<{ content: Application[] }>('/applications/me').then((p) => setApps(p.content)).catch(() => {})
  }, [])

  if (!stats) return <Skeleton lines={8} />

  const xpPct = Math.min(100, (stats.totalXp % 100) * 100 / 100)

  return (
    <>
      <h2>Welcome back 👋</h2>
      <div className="grid grid-4">
        <StatCard label="Applications" value={stats.totalApplications} icon="⌗" />
        <StatCard label="Interviews" value={`${stats.interviewsCompleted}/${stats.interviewsScheduled}`} icon="◉" />
        <StatCard label="Coding submissions" value={stats.submissions} icon="⌘" />
        <StatCard label="Resume score" value={stats.bestResumeScore != null ? `${stats.bestResumeScore}/100` : '—'} icon="▤" />
      </div>
      <div className="grid grid-2">
        <Card title="Level & XP">
          <p style={{ fontSize: 30, fontWeight: 800, margin: 0 }}>Level {stats.level}</p>
          <p className="muted">{stats.totalXp} total XP</p>
          <div className="progress">
            <div className="progress-fill" style={{ width: `${xpPct}%` }} />
          </div>
          <p className="muted" style={{ marginTop: 8 }}>
            Average interview score:{' '}
            <strong>{stats.averageInterviewScore != null ? stats.averageInterviewScore.toFixed(1) : '—'}</strong>
          </p>
        </Card>
        <Card title="Upcoming interviews">
          {interviews.filter((i) => i.status === 'SCHEDULED').length === 0 ? (
            <p className="muted">No upcoming interviews.</p>
          ) : (
            interviews
              .filter((i) => i.status === 'SCHEDULED')
              .slice(0, 5)
              .map((i) => (
                <div key={i.id} className="job-card" style={{ borderBottom: '1px solid var(--border)', padding: '10px 0' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <strong>{i.title}</strong>
                    <StatusBadge status={i.status} />
                  </div>
                  <p className="muted" style={{ margin: 0, fontSize: 13 }}>
                    {i.scheduledAt ? new Date(i.scheduledAt).toLocaleString() : 'Not scheduled'}
                  </p>
                </div>
              ))
          )}
        </Card>
      </div>
      <Card
        title="Recent applications"
        actions={
          <Link to="/app/jobs">
            <button className="btn btn-secondary btn-sm">Browse jobs</button>
          </Link>
        }
      >
        {apps.length === 0 ? (
          <p className="muted">You haven't applied to any jobs yet.</p>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Job</th>
                  <th>Company</th>
                  <th>Status</th>
                  <th>ATS</th>
                  <th>Applied</th>
                </tr>
              </thead>
              <tbody>
                {apps.slice(0, 6).map((a) => (
                  <tr key={a.id}>
                    <td>{a.jobTitle}</td>
                    <td>{a.companyName}</td>
                    <td>
                      <StatusBadge status={a.status} />
                    </td>
                    <td>{a.atsScore != null ? `${a.atsScore}/100` : '—'}</td>
                    <td className="muted">{new Date(a.appliedAt).toLocaleDateString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </>
  )
}
