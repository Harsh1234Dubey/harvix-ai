import { useCallback, useEffect, useState } from 'react'
import { Card, Pagination, Skeleton } from '@/components/ui'
import { get, errorMessage } from '@/lib/api'
import type { AuditLog, Page } from '@/lib/types'

export default function Audit() {
  const [items, setItems] = useState<AuditLog[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [resource, setResource] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(
    (p: number) => {
      setLoading(true)
      void get<Page<AuditLog>>('/admin/audit-logs', { params: { resource, page: p, size: 15 } })
        .then((d) => {
          setItems(d.content)
          setPage(d.page)
          setTotalPages(d.totalPages)
        })
        .catch((e) => setError(errorMessage(e)))
        .finally(() => setLoading(false))
    },
    [resource]
  )

  useEffect(() => {
    load(0)
  }, [load])

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
        <h2 style={{ margin: 0 }}>Audit logs</h2>
        <div style={{ display: 'flex', gap: 8 }}>
          <input className="input" style={{ width: 200 }} placeholder="Resource filter…" value={resource} onChange={(e) => setResource(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && load(0)} />
          <button className="btn btn-primary" onClick={() => load(0)}>
            Filter
          </button>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {loading && <Skeleton lines={8} />}

      {!loading && (
        <Card>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>User</th>
                  <th>Action</th>
                  <th>Resource</th>
                  <th>Resource ID</th>
                  <th>IP</th>
                  <th>When</th>
                </tr>
              </thead>
              <tbody>
                {items.map((l) => (
                  <tr key={l.id}>
                    <td>{l.id}</td>
                    <td>{l.userId ?? 'system'}</td>
                    <td>
                      <span className="chip">{l.action}</span>
                    </td>
                    <td>{l.resource}</td>
                    <td className="muted">{l.resourceId ?? '—'}</td>
                    <td className="muted">{l.ip ?? '—'}</td>
                    <td className="muted">{new Date(l.createdAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {!loading && totalPages > 1 && <Pagination page={page} totalPages={totalPages} onPage={load} />}
    </>
  )
}
