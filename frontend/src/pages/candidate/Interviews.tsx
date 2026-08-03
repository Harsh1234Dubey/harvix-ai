import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, EmptyState, Pagination, Skeleton, StatusBadge } from '@/components/ui'
import { get, post, errorMessage } from '@/lib/api'
import { useAuth } from '@/providers/AuthProvider'
import type { Interview, Page } from '@/lib/types'

const DIFFICULTIES = ['EASY', 'MEDIUM', 'HARD']

export default function Interviews() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const isCandidate = user?.roles.includes('CANDIDATE')
  const [items, setItems] = useState<Interview[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [skill, setSkill] = useState('')
  const [difficulty, setDifficulty] = useState('MEDIUM')
  const [starting, setStarting] = useState(false)

  useEffect(() => {
    setLoading(true)
    const url = isCandidate ? '/interviews/me' : '/interviews/recruiter'
    void get<Page<Interview>>(url, { params: { page: 0, size: 10 } })
      .then((p) => {
        setItems(p.content)
        setPage(p.page)
        setTotalPages(p.totalPages)
      })
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false))
  }, [isCandidate])

  const load = (p: number) => {
    const url = isCandidate ? '/interviews/me' : '/interviews/recruiter'
    void get<Page<Interview>>(url, { params: { page: p, size: 10 } })
      .then((data) => {
        setItems(data.content)
        setPage(data.page)
        setTotalPages(data.totalPages)
      })
      .catch((e) => setError(errorMessage(e)))
  }

  const startSession = () => {
    if (!skill.trim()) return
    setStarting(true)
    setError('')
    void post<{ sessionId: number }>('/interviews/sessions', { skill: skill.trim(), difficulty })
      .then((d) => navigate(`/app/interviews/session/${d.sessionId}`))
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setStarting(false))
  }

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
        <h2 style={{ margin: 0 }}>{isCandidate ? 'My interviews' : 'Interviews'}</h2>
      </div>

      {isCandidate && (
        <Card title="Start an AI mock interview">
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'flex-end' }}>
            <div className="form-row" style={{ flex: 2, minWidth: 220 }}>
              <label>Skill / role</label>
              <input className="input" placeholder="e.g. Java, React, System Design" value={skill} onChange={(e) => setSkill(e.target.value)} />
            </div>
            <div className="form-row" style={{ flex: 1, minWidth: 140 }}>
              <label>Difficulty</label>
              <select className="input" value={difficulty} onChange={(e) => setDifficulty(e.target.value)}>
                {DIFFICULTIES.map((d) => (
                  <option key={d} value={d}>
                    {d}
                  </option>
                ))}
              </select>
            </div>
            <button className="btn btn-primary" disabled={starting || !skill.trim()} onClick={startSession}>
              {starting ? 'Starting…' : 'Start interview'}
            </button>
          </div>
          {error && <div className="alert alert-error" style={{ marginTop: 10 }}>{error}</div>}
        </Card>
      )}

      {loading && <Skeleton lines={5} />}
      {!loading && items.length === 0 && <EmptyState title="No interviews yet" hint="Scheduled interviews will appear here." />}

      {!loading && items.length > 0 && (
        <>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Type</th>
                  <th>Candidate</th>
                  <th>Scheduled</th>
                  <th>Status</th>
                  <th>Score</th>
                </tr>
              </thead>
              <tbody>
                {items.map((i) => (
                  <tr key={i.id}>
                    <td>{i.title}</td>
                    <td>{i.type}</td>
                    <td>{i.candidateName}</td>
                    <td>{i.scheduledAt ? new Date(i.scheduledAt).toLocaleString() : '—'}</td>
                    <td>
                      <StatusBadge status={i.status} />
                    </td>
                    <td>{i.score != null ? i.score : '—'}</td>
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
