import { useEffect, useState } from 'react'
import { Card, Skeleton, StatCard } from '@/components/ui'
import { get, errorMessage } from '@/lib/api'
import type { AnalyticsSummary } from '@/lib/types'

export default function AdminDashboard() {
  const [data, setData] = useState<AnalyticsSummary | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    void get<AnalyticsSummary>('/analytics/admin')
      .then(setData)
      .catch((e) => setError(errorMessage(e)))
  }, [])

  if (error && !data) return <div className="alert alert-error">{error}</div>
  if (!data) return <Skeleton lines={8} />

  return (
    <>
      <h2>Platform overview</h2>
      <div className="grid grid-4">
        <StatCard label="Users" value={data.totalUsers} icon="👤" />
        <StatCard label="Recruiters" value={data.totalRecruiters} icon="🏢" />
        <StatCard label="Candidates" value={data.totalCandidates} icon="🎓" />
        <StatCard label="Companies" value={data.totalCompanies} icon="🏛" />
        <StatCard label="Jobs" value={data.totalJobs} icon="💼" />
        <StatCard label="Applications" value={data.totalApplications} icon="⌗" />
        <StatCard label="Interviews" value={data.totalInterviews} icon="◉" />
        <StatCard label="Pending verifications" value={data.pendingVerifications} icon="✓" />
      </div>

      <div className="grid grid-2">
        <Card title="Applications by status">
          {Object.keys(data.applicationsByStatus).length === 0 ? (
            <p className="muted">No data.</p>
          ) : (
            Object.entries(data.applicationsByStatus).map(([k, v]) => (
              <div key={k} style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--border)' }}>
                <span>{k}</span>
                <strong>{v}</strong>
              </div>
            ))
          )}
        </Card>
        <Card title="Jobs by status">
          {Object.keys(data.jobsByStatus).length === 0 ? (
            <p className="muted">No data.</p>
          ) : (
            Object.entries(data.jobsByStatus).map(([k, v]) => (
              <div key={k} style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--border)' }}>
                <span>{k}</span>
                <strong>{v}</strong>
              </div>
            ))
          )}
        </Card>
      </div>
    </>
  )
}
