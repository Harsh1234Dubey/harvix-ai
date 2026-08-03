import { useCallback, useEffect, useState } from 'react'
import { Card, EmptyState, Pagination, Skeleton, StatusBadge } from '@/components/ui'
import { get, post, errorMessage } from '@/lib/api'
import { useAuth } from '@/providers/AuthProvider'
import type { Page, QuestionBankQuestion } from '@/lib/types'

export default function Questions() {
  const { user } = useAuth()
  const isCandidate = user?.roles.includes('CANDIDATE')
  const [items, setItems] = useState<QuestionBankQuestion[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [topic, setTopic] = useState('')
  const [difficulty, setDifficulty] = useState('')
  const [q, setQ] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [revealed, setRevealed] = useState<Record<number, boolean>>({})

  const load = useCallback(
    (p: number) => {
      setLoading(true)
      void get<Page<QuestionBankQuestion>>('/questions', {
        params: { page: p, size: 10, ...(topic ? { topic } : {}), ...(difficulty ? { difficulty } : {}), ...(q ? { q } : {}) },
      })
        .then((d) => {
          setItems(d.content)
          setPage(d.page)
          setTotalPages(d.totalPages)
        })
        .catch((e) => setError(errorMessage(e)))
        .finally(() => setLoading(false))
    },
    [topic, difficulty, q]
  )

  useEffect(() => {
    load(0)
  }, [load])

  const bookmark = (id: number) => {
    void post<unknown>(`/questions/${id}/bookmark`, {})
      .then(() => alert('Bookmark toggled'))
      .catch((e) => alert(errorMessage(e)))
  }

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
        <h2 style={{ margin: 0 }}>Question bank</h2>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          <input className="input" style={{ width: 180 }} placeholder="Search…" value={q} onChange={(e) => setQ(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && load(0)} />
          <input className="input" style={{ width: 140 }} placeholder="Topic" value={topic} onChange={(e) => setTopic(e.target.value)} />
          <select className="input" value={difficulty} onChange={(e) => setDifficulty(e.target.value)}>
            <option value="">All levels</option>
            <option>EASY</option>
            <option>MEDIUM</option>
            <option>HARD</option>
          </select>
          <button className="btn btn-primary" onClick={() => load(0)}>
            Search
          </button>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {loading && <Skeleton lines={6} />}
      {!loading && items.length === 0 && <EmptyState title="No questions found" hint="Try different filters." />}

      {items.map((item) => (
        <Card key={item.id}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
            <div>
              <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 6 }}>
                <span className="chip">{item.topic}</span>
                {item.subTopic && <span className="chip">{item.subTopic}</span>}
                <StatusBadge status={item.difficulty} />
                {item.type && <span className="chip">{item.type}</span>}
              </div>
              <p style={{ margin: 0 }}>{item.question}</p>
            </div>
            {isCandidate && (
              <button className="btn btn-secondary btn-sm" onClick={() => bookmark(item.id)}>
                ★ Bookmark
              </button>
            )}
          </div>
          <div style={{ marginTop: 10 }}>
            {revealed[item.id] ? (
              <div className="code-block">
                <p className="muted" style={{ margin: 0 }}>
                  Model answer
                </p>
                <p style={{ margin: '8px 0 0', whiteSpace: 'pre-wrap' }}>{item.answer ?? 'No answer provided.'}</p>
              </div>
            ) : (
              <button className="btn btn-secondary btn-sm" onClick={() => setRevealed((r) => ({ ...r, [item.id]: true }))}>
                Reveal answer
              </button>
            )}
          </div>
        </Card>
      ))}

      {!loading && totalPages > 1 && <Pagination page={page} totalPages={totalPages} onPage={load} />}
    </>
  )
}
