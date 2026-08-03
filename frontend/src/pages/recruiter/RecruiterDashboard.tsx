import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Card, Skeleton, StatCard, StatusBadge } from '@/components/ui'
import { get, errorMessage } from '@/lib/api'
import type { Company, Interview, Job, Page, RecruiterAnalytics } from '@/lib/types'

export default function RecruiterDashboard() {
  const [companies, setCompanies] = useState<Company[]>([])
  const [analytics, setAnalytics] = useState<RecruiterAnalytics | null>(null)
  const [jobs, setJobs] = useState<Job[]>([])
  const [interviews, setInterviews] = useState<Interview[]>([])
  const [error, setError] = useState('')

  useEffect(() => {
    void get<Company[]>('/companies/mine')
      .then((list) => {
        setCompanies(list)
        const company = list[0]
        if (company) {
          void get<RecruiterAnalytics>(`/analytics/recruiter/${company.id}`).then(setAnalytics).catch((e) => setError(errorMessage(e)))
          void get<Page<Job>>('/jobs', { params: { companyId: company.id, page: 0, size: 6 } }).then((p) => setJobs(p.content)).catch(() => {})
        }
      })
      .catch((e) => setError(errorMessage(e)))
    void get<Page<Interview>>('/interviews/recruiter', { params: { page: 0, size: 5 } })
      .then((p) => setInterviews(p.content))
      .catch(() => {})
  }, [])

  if (companies.length === 0 && !error) return <Skeleton lines={6} />

  if (companies.length === 0) {
    return (
      <>
        <h2>Recruiter dashboard</h2>
        <Card title="Create your company">
          <p className="muted">You need a company before posting jobs.</p>
          <Link to="/app/company">
            <button className="btn btn-primary">Create company</button>
          </Link>
        </Card>
      </>
    )
  }

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
        <h2 style={{ margin: 0 }}>{companies[0].name}</h2>
        <Link to="/app/jobs/new">
          <button className="btn btn-primary">Post a job</button>
        </Link>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {analytics && (
        <div className="grid grid-4">
          <StatCard label="Applications" value={analytics.totalApplications} icon="⌗" />
          <StatCard label="Shortlisted" value={analytics.shortlisted} icon="★" />
          <StatCard label="Interviewed" value={analytics.interviewed} icon="◉" />
          <StatCard label="Hired" value={analytics.hired} icon="✓" />
        </div>
      )}

      <div className="grid grid-2">
        <Card
          title="My jobs"
          actions={
            <Link to="/app/jobs/new">
              <button className="btn btn-secondary btn-sm">+ New</button>
            </Link>
          }
        >
          {jobs.length === 0 ? (
            <p className="muted">No jobs posted yet.</p>
          ) : (
            jobs.map((j) => (
              <div key={j.uuid} className="job-card" style={{ borderBottom: '1px solid var(--border)', padding: '10px 0' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Link to={`/app/jobs/${j.uuid}/applications`}>
                    <strong>{j.title}</strong>
                  </Link>
                  <StatusBadge status={j.status} />
                </div>
                <p className="muted" style={{ margin: 0, fontSize: 13 }}>
                  {j.applicationsCount} applications · {j.viewsCount} views
                </p>
              </div>
            ))
          )}
        </Card>

        <Card title="Upcoming interviews">
          {interviews.length === 0 ? (
            <p className="muted">No upcoming interviews.</p>
          ) : (
            interviews
              .filter((i) => i.status === 'SCHEDULED')
              .map((i) => (
                <div key={i.id} className="job-card" style={{ borderBottom: '1px solid var(--border)', padding: '10px 0' }}>
                  <strong>{i.candidateName}</strong> — {i.title}
                  <p className="muted" style={{ margin: 0, fontSize: 13 }}>
                    {i.scheduledAt ? new Date(i.scheduledAt).toLocaleString() : 'Not scheduled'}
                  </p>
                </div>
              ))
          )}
        </Card>
      </div>
    </>
  )
}
