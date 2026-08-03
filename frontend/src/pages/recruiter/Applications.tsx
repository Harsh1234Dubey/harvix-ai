import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Card, Pagination, Skeleton, Spinner, StatusBadge } from '@/components/ui'
import { get, patch, errorMessage } from '@/lib/api'
import type { Application, Job, Page } from '@/lib/types'

const STATUSES = ['SUBMITTED', 'REVIEWING', 'SHORTLISTED', 'INTERVIEWED', 'OFFERED', 'HIRED', 'REJECTED', 'WITHDRAWN']

export default function Applications() {
  const { uuid = '' } = useParams()
  const [job, setJob] = useState<Job | null>(null)
  const [items, setItems] = useState<Application[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    void get<Job>(`/jobs/${uuid}`)
      .then((j) => {
        setJob(j)
        return get<Page<Application>>(`/applications/job/${j.id}`, { params: { page: 0, size: 10 } })
      })
      .then((p) => {
        setItems(p.content)
        setPage(p.page)
        setTotalPages(p.totalPages)
      })
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false))
  }, [uuid])

  const load = (p: number) => {
    if (!job) return
    void get<Page<Application>>(`/applications/job/${job.id}`, { params: { page: p, size: 10 } })
      .then((data) => {
        setItems(data.content)
        setPage(data.page)
        setTotalPages(data.totalPages)
      })
      .catch((e) => setError(errorMessage(e)))
  }

  const setStatus = (id: number, status: string) => {
    void patch<Application>(`/applications/${id}/status`, { status })
      .then((app) => setItems((arr) => arr.map((a) => (a.id === id ? app : a))))
      .catch((e) => alert(errorMessage(e)))
  }

  if (error && !job) return <div className="alert alert-error">{error}</div>
  if (!job) return <Spinner />

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
        <h2 style={{ margin: 0 }}>Applications — {job.title}</h2>
        <StatusBadge status={job.status} />
      </div>
      <p className="muted">{job.applicationsCount} total applications</p>

      {loading && <Skeleton lines={6} />}

      {!loading && items.length === 0 && <Card><p className="muted">No applications yet.</p></Card>}

      {!loading && items.length > 0 && (
        <>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Candidate</th>
                  <th>Status</th>
                  <th>ATS</th>
                  <th>Applied</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map((a) => (
                  <tr key={a.id}>
                    <td>
                      <strong>{a.candidateName}</strong>
                      {a.coverLetter && (
                        <details style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                          <summary>Cover letter</summary>
                          <p>{a.coverLetter}</p>
                        </details>
                      )}
                    </td>
                    <td>
                      <StatusBadge status={a.status} />
                    </td>
                    <td>{a.atsScore != null ? `${a.atsScore}/100` : '—'}</td>
                    <td className="muted">{new Date(a.appliedAt).toLocaleDateString()}</td>
                    <td>
                      <select
                        className="input"
                        style={{ width: 140 }}
                        value={a.status}
                        onChange={(e) => setStatus(a.id, e.target.value)}
                      >
                        {STATUSES.map((s) => (
                          <option key={s} value={s}>
                            {s}
                          </option>
                        ))}
                      </select>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {totalPages > 1 && <Pagination page={page} totalPages={totalPages} onPage={load} />}
        </>
      )}
    </>
  )
}
