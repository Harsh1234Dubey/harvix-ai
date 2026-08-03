import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Card, EmptyState, Pagination, Skeleton, StatusBadge } from '@/components/ui'
import { get, errorMessage } from '@/lib/api'
import type { CodingTest, Page, Submission } from '@/lib/types'

export default function Coding() {
  const [tests, setTests] = useState<CodingTest[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [language, setLanguage] = useState('')
  const [difficulty, setDifficulty] = useState('')
  const [history, setHistory] = useState<Submission[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(
    (p: number) => {
      setLoading(true)
      void get<Page<CodingTest>>('/coding/tests', {
        params: { page: p, size: 9, ...(language ? { language } : {}), ...(difficulty ? { difficulty } : {}) },
      })
        .then((d) => {
          setTests(d.content)
          setPage(d.page)
          setTotalPages(d.totalPages)
        })
        .catch((e) => setError(errorMessage(e)))
        .finally(() => setLoading(false))
    },
    [language, difficulty]
  )

  useEffect(() => {
    load(0)
    void get<Submission[]>('/coding/submissions/me').then(setHistory).catch(() => {})
  }, [load])

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
        <h2 style={{ margin: 0 }}>Coding challenges</h2>
        <div style={{ display: 'flex', gap: 8 }}>
          <select className="input" value={language} onChange={(e) => setLanguage(e.target.value)}>
            <option value="">All languages</option>
            <option>JAVA</option>
            <option>PYTHON</option>
            <option>JAVASCRIPT</option>
            <option>CPP</option>
            <option>GO</option>
          </select>
          <select className="input" value={difficulty} onChange={(e) => setDifficulty(e.target.value)}>
            <option value="">All levels</option>
            <option>EASY</option>
            <option>MEDIUM</option>
            <option>HARD</option>
          </select>
          <button className="btn btn-primary" onClick={() => load(0)}>
            Filter
          </button>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {loading && <Skeleton lines={6} />}

      {!loading && tests.length === 0 && <EmptyState title="No challenges found" hint="Try different filters." />}

      <div className="grid grid-3">
        {tests.map((t) => (
          <Card key={t.id} className="job-card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <h3 style={{ margin: 0 }}>{t.title}</h3>
              <StatusBadge status={t.difficulty} />
            </div>
            <p className="muted" style={{ marginTop: 6, minHeight: 40 }}>
              {t.description.length > 120 ? t.description.slice(0, 120) + '…' : t.description}
            </p>
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', margin: '10px 0' }}>
              <span className="chip">{t.language}</span>
              <span className="chip">{t.timeLimitSec}s</span>
              <span className="chip">{t.memoryLimitMb} MB</span>
            </div>
            <Link to={`/app/coding/problem/${t.id}`}>
              <button className="btn btn-primary btn-sm">Solve</button>
            </Link>
          </Card>
        ))}
      </div>

      {!loading && totalPages > 1 && <Pagination page={page} totalPages={totalPages} onPage={load} />}

      <Card title="Submission history">
        {history.length === 0 ? (
          <p className="muted">No submissions yet.</p>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Test</th>
                  <th>Status</th>
                  <th>Passed</th>
                  <th>Score</th>
                  <th>When</th>
                </tr>
              </thead>
              <tbody>
                {history.slice(0, 10).map((s) => (
                  <tr key={s.id}>
                    <td>#{s.codingTestId}</td>
                    <td>
                      <StatusBadge status={s.status} />
                    </td>
                    <td>
                      {s.passedCases}/{s.totalCases}
                    </td>
                    <td>{s.codeScore ?? '—'}</td>
                    <td className="muted">{new Date(s.submittedAt).toLocaleString()}</td>
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
