import { useEffect, useState } from 'react'
import { Card, Skeleton, StatusBadge } from '@/components/ui'
import { get, patch, errorMessage } from '@/lib/api'
import type { Subscription } from '@/lib/types'

export default function Subscriptions() {
  const [items, setItems] = useState<Subscription[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    void get<Subscription[]>('/admin/subscriptions')
      .then(setItems)
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false))
  }, [])

  const update = (id: number, plan: string, aiQuota: number | null) => {
    void patch<unknown>(`/admin/subscriptions/${id}`, {
      plan,
      ...(aiQuota != null ? { aiQuotaMonth: aiQuota } : {}),
    })
      .then(() => setItems((arr) => arr.map((s) => (s.id === id ? { ...s, plan, ...(aiQuota != null ? { aiQuotaMonth: aiQuota } : {}) } : s))))
      .catch((e) => alert(errorMessage(e)))
  }

  return (
    <>
      <h2>Subscriptions</h2>
      {error && <div className="alert alert-error">{error}</div>}
      {loading && <Skeleton lines={5} />}

      {!loading && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Owner</th>
                <th>Plan</th>
                <th>Status</th>
                <th>AI quota</th>
                <th>Expires</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {items.map((s) => (
                <tr key={s.id}>
                  <td>#{s.id}</td>
                  <td>
                    {s.companyId ? `Company ${s.companyId}` : `User ${s.userId ?? '—'}`}
                  </td>
                  <td>
                    <StatusBadge status={s.plan} />
                  </td>
                  <td>
                    <StatusBadge status={s.status} />
                  </td>
                  <td>
                    {s.aiUsed ?? s.aiUsedMonth ?? 0}/{s.aiQuota ?? s.aiQuotaMonth ?? 0}
                  </td>
                  <td className="muted">{s.expiresAt ? new Date(s.expiresAt).toLocaleDateString() : '—'}</td>
                  <td style={{ whiteSpace: 'nowrap' }}>
                    <select className="input" style={{ width: 140, marginRight: 6 }} value={s.plan} onChange={(e) => update(s.id, e.target.value, s.aiQuota ?? s.aiQuotaMonth ?? null)}>
                      <option>FREE</option>
                      <option>PRO</option>
                      <option>ENTERPRISE</option>
                    </select>
                    <input
                      className="input"
                      style={{ width: 90 }}
                      type="number"
                      placeholder="Quota"
                      onBlur={(e) => {
                        if (e.target.value !== '') update(s.id, s.plan, Number(e.target.value))
                      }}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
