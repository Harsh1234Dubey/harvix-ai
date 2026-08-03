import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Card, EmptyState, Pagination, Skeleton, StatusBadge } from '@/components/ui'
import { get, errorMessage } from '@/lib/api'
import type { Job, Page } from '@/lib/types'

export default function Jobs() {
  const [jobs, setJobs] = useState<Job[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [q, setQ] = useState('')
  const [location, setLocation] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const load = useCallback(
    (p: number) => {
      setLoading(true)
      void get<Page<Job>>('/jobs', {
        params: {
          page: p,
          size: 9,
          status: 'PUBLISHED',
          ...(q ? { q } : {}),
          ...(location ? { location } : {}),
        },
      })
        .then((data) => {
          setJobs(data.content)
          setPage(data.page)
          setTotalPages(data.totalPages)
          setTotalElements(data.totalElements)
          setError('')
        })
        .catch((e) => setError(errorMessage(e)))
        .finally(() => setLoading(false))
    },
    [q, location]
  )

  useEffect(() => {
    load(0)
  }, [load])

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
        <h2 style={{ margin: 0 }}>Browse jobs</h2>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          <input
            className="input"
            style={{ width: 220 }}
            placeholder="Search title, skills…"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && load(0)}
          />
          <input
            className="input"
            style={{ width: 160 }}
            placeholder="Location"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && load(0)}
          />
          <button className="btn btn-primary" onClick={() => load(0)}>
            Search
          </button>
        </div>
      </div>

      <p className="muted">{totalElements} jobs found</p>

      {error && <div className="alert alert-error">{error}</div>}
      {loading && <Skeleton lines={9} />}

      {!loading && !error && jobs.length === 0 && (
        <EmptyState title="No jobs found" hint="Try different keywords or filters." />
      )}

      <div className="grid grid-3">
        {jobs.map((job) => (
          <Card key={job.uuid} className="job-card" style={{ cursor: 'pointer' }} onClick={() => navigate(`/app/jobs/${job.uuid}`)}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
              <h3 style={{ margin: 0 }}>{job.title}</h3>
              <StatusBadge status={job.status} />
            </div>
            <p className="muted" style={{ marginTop: 4 }}>
              {job.companyName} · {job.location || 'Remote'}
            </p>
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', margin: '10px 0' }}>
              <span className="chip">{job.workMode}</span>
              <span className="chip">{job.employmentType}</span>
              {job.experienceMin != null && <span className="chip">{job.experienceMin}–{job.experienceMax ?? '∞'} yrs</span>}
              {job.salaryMin != null && (
                <span className="chip">
                  {job.currency} {job.salaryMin}–{job.salaryMax}
                </span>
              )}
            </div>
            {job.requiredSkills.slice(0, 4).map((s) => (
              <span key={s} className="tag">
                {s}
              </span>
            ))}
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 12 }}>
              <span className="muted" style={{ fontSize: 13 }}>
                {job.applicationsCount} applicants
              </span>
              <Link to={`/app/jobs/${job.uuid}`}>View →</Link>
            </div>
          </Card>
        ))}
      </div>

      {!loading && totalPages > 1 && (
        <Pagination page={page} totalPages={totalPages} onPage={(p) => load(p)} />
      )}
    </>
  )
}
