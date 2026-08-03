import { useCallback, useEffect, useState } from 'react'
import { Card, Pagination, Skeleton, StatusBadge } from '@/components/ui'
import { get, patch, del, errorMessage } from '@/lib/api'
import type { Page, User } from '@/lib/types'

const STATUSES = ['ACTIVE', 'BLOCKED', 'PENDING', 'DISABLED']

export default function Users() {
  const [items, setItems] = useState<User[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(
    (p: number) => {
      setLoading(true)
      void get<Page<User>>('/users', { params: { search, page: p, size: 10 } })
        .then((d) => {
          setItems(d.content)
          setPage(d.page)
          setTotalPages(d.totalPages)
        })
        .catch((e) => setError(errorMessage(e)))
        .finally(() => setLoading(false))
    },
    [search]
  )

  useEffect(() => {
    load(0)
  }, [load])

  const setStatus = (id: number, status: string) => {
    void patch<User>(`/users/${id}/status`, { status })
      .then((u) => setItems((arr) => arr.map((x) => (x.id === id ? u : x))))
      .catch((e) => alert(errorMessage(e)))
  }

  const remove = (id: number) => {
    if (!window.confirm('Delete this user permanently?')) return
    void del<unknown>(`/users/${id}`)
      .then(() => setItems((arr) => arr.filter((x) => x.id !== id)))
      .catch((e) => alert(errorMessage(e)))
  }

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
        <h2 style={{ margin: 0 }}>Users</h2>
        <div style={{ display: 'flex', gap: 8 }}>
          <input className="input" style={{ width: 220 }} placeholder="Search name/email…" value={search} onChange={(e) => setSearch(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && load(0)} />
          <button className="btn btn-primary" onClick={() => load(0)}>
            Search
          </button>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {loading && <Skeleton lines={6} />}

      {!loading && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>User</th>
                <th>Email</th>
                <th>Roles</th>
                <th>Status</th>
                <th>Verified</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {items.map((u) => (
                <tr key={u.id}>
                  <td>
                    <strong>
                      {u.firstName} {u.lastName}
                    </strong>
                  </td>
                  <td>{u.email}</td>
                  <td>{u.roles.join(', ')}</td>
                  <td>
                    <StatusBadge status={u.status} />
                  </td>
                  <td>{u.emailVerified ? '✓' : '—'}</td>
                  <td style={{ whiteSpace: 'nowrap' }}>
                    <select className="input" style={{ width: 130, marginRight: 6 }} value={u.status} onChange={(e) => setStatus(u.id, e.target.value)}>
                      {STATUSES.map((s) => (
                        <option key={s} value={s}>
                          {s}
                        </option>
                      ))}
                    </select>
                    <button className="btn btn-danger btn-sm" onClick={() => remove(u.id)}>
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {!loading && totalPages > 1 && <Pagination page={page} totalPages={totalPages} onPage={load} />}
    </>
  )
}
