import { useEffect, useState } from 'react'
import { Card, Skeleton, StatCard } from '@/components/ui'
import { get, errorMessage } from '@/lib/api'
import type { Company, RecruiterAnalytics } from '@/lib/types'

export default function Analytics() {
  const [companies, setCompanies] = useState<Company[]>([])
  const [companyId, setCompanyId] = useState<number | ''>('')
  const [data, setData] = useState<RecruiterAnalytics | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    void get<Company[]>('/companies/mine')
      .then((list) => {
        setCompanies(list)
        if (list[0]) setCompanyId(list[0].id)
      })
      .catch((e) => setError(errorMessage(e)))
  }, [])

  useEffect(() => {
    if (companyId === '') return
    void get<RecruiterAnalytics>(`/analytics/recruiter/${companyId}`)
      .then(setData)
      .catch((e) => setError(errorMessage(e)))
  }, [companyId])

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
        <h2 style={{ margin: 0 }}>Analytics</h2>
        <select className="input" style={{ width: 240 }} value={companyId} onChange={(e) => setCompanyId(e.target.value ? Number(e.target.value) : '')}>
          {companies.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {data && (
        <>
          <div className="grid grid-4">
            <StatCard label="Total" value={data.totalApplications} icon="⌗" />
            <StatCard label="Submitted" value={data.submitted} icon="→" />
            <StatCard label="Reviewing" value={data.reviewing} icon="◐" />
            <StatCard label="Shortlisted" value={data.shortlisted} icon="★" />
            <StatCard label="Interviewed" value={data.interviewed} icon="◉" />
            <StatCard label="Offered / Hired" value={`${data.hired}`} icon="✓" />
            <StatCard label="Rejected" value={data.rejected} icon="✕" />
          </div>

          <Card title="Applications by job">
            {Object.keys(data.applicationsByJob).length === 0 ? (
              <p className="muted">No applications recorded.</p>
            ) : (
              Object.entries(data.applicationsByJob).map(([jobTitle, count]) => (
                <div key={jobTitle} style={{ marginBottom: 10 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                    <span>{jobTitle}</span>
                    <strong>{count}</strong>
                  </div>
                  <div className="progress">
                    <div
                      className="progress-fill"
                      style={{ width: `${Math.min(100, (Number(count) / Math.max(1, data.totalApplications)) * 100)}%` }}
                    />
                  </div>
                </div>
              ))
            )}
          </Card>
        </>
      )}

      {!data && !error && <Skeleton lines={6} />}
    </>
  )
}
