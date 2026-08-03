import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Card, EmptyState, Skeleton, StatusBadge } from '@/components/ui'
import { get, del, errorMessage } from '@/lib/api'
import type { Page } from '@/lib/types'

interface BookmarkItem {
  id: number
  entityType: string
  entityId: number
  createdAt: string
  title?: string
  company?: string
  status?: string
  topic?: string
  difficulty?: string
}

export default function Bookmarks() {
  const [tab, setTab] = useState<'JOB' | 'QUESTION'>('JOB')
  const [items, setItems] = useState<BookmarkItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    setLoading(true)
    void get<Page<BookmarkItem>>('/bookmarks/me', { params: { entityType: tab, page: 0, size: 20 } })
      .then((p) => setItems(p.content))
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false))
  }, [tab])

  const remove = (entityId: number) => {
    void del<unknown>('/bookmarks', { params: { entityType: tab, entityId } })
      .then(() => setItems((arr) => arr.filter((b) => b.entityId !== entityId)))
      .catch((e) => setError(errorMessage(e)))
  }

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
        <h2 style={{ margin: 0 }}>Saved items</h2>
        <div className="tabs">
          <button className={tab === 'JOB' ? 'tab tab-active' : 'tab'} onClick={() => setTab('JOB')}>
            Jobs
          </button>
          <button className={tab === 'QUESTION' ? 'tab tab-active' : 'tab'} onClick={() => setTab('QUESTION')}>
            Questions
          </button>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {loading && <Skeleton lines={4} />}

      {!loading && items.length === 0 && (
        <EmptyState title="Nothing saved yet" hint="Bookmark jobs or questions to find them here." />
      )}

      {items.map((b) => (
        <Card key={b.id}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
            <div>
              <h4 style={{ margin: 0 }}>
                {tab === 'JOB' ? <Link to={`/app/jobs/${b.entityId}`}>{b.title}</Link> : b.title || `Question #${b.entityId}`}
              </h4>
              <p className="muted" style={{ margin: 0 }}>
                {tab === 'JOB' ? b.company : b.topic}
              </p>
            </div>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              {b.status && <StatusBadge status={b.status} />}
              {b.difficulty && <StatusBadge status={b.difficulty} />}
              <button className="btn btn-secondary btn-sm" onClick={() => remove(b.entityId)}>
                Remove
              </button>
            </div>
          </div>
        </Card>
      ))}
    </>
  )
}
