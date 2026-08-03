import { useCallback, useEffect, useState } from 'react'
import { Button, Card, EmptyState, Select, Skeleton, Textarea } from '@/components/ui'
import { del, download, errorMessage, get, post } from '@/lib/api'
import type { ReportMeta } from '@/lib/types'

const types = ['RESUME', 'INTERVIEW', 'CODING', 'PERFORMANCE', 'HIRING']

export default function Reports() {
  const [reports, setReports] = useState<ReportMeta[]>([])
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState('')
  const [showNew, setShowNew] = useState(false)
  const [form, setForm] = useState({ type: 'PERFORMANCE', title: '', scope: '', data: '', format: 'PDF' })

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const page = await get<{ content: ReportMeta[] }>('/reports')
      setReports(page.content)
    } catch (e) {
      setErr(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  async function generate() {
    setErr('')
    try {
      await post('/reports', form)
      setShowNew(false)
      setForm({ type: 'PERFORMANCE', title: '', scope: '', data: '', format: 'PDF' })
      await load()
    } catch (e) {
      setErr(errorMessage(e))
    }
  }

  async function remove(uuid: string) {
    try {
      await del(`/reports/${uuid}`)
      setReports((prev) => prev.filter((r) => r.uuid !== uuid))
    } catch (e) {
      setErr(errorMessage(e))
    }
  }

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2>Reports</h2>
        <Button onClick={() => setShowNew((v) => !v)}>{showNew ? 'Cancel' : '+ Generate'}</Button>
      </div>
      {err ? <div className="alert alert-error">{err}</div> : null}
      {showNew ? (
        <Card title="Generate a report">
          <div className="grid" style={{ gap: 14 }}>
            <div className="row">
              <label className="field">
                <span className="field-label">Type</span>
                <Select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
                  {types.map((t) => (
                    <option key={t}>{t}</option>
                  ))}
                </Select>
              </label>
              <label className="field">
                <span className="field-label">Format</span>
                <Select value={form.format} onChange={(e) => setForm({ ...form, format: e.target.value })}>
                  <option>PDF</option>
                  <option>CSV</option>
                </Select>
              </label>
            </div>
            <label className="field">
              <span className="field-label">Title</span>
              <input
                className="input"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                placeholder={`${form.type} Report`}
              />
            </label>
            <label className="field">
              <span className="field-label">Scope (optional)</span>
              <input className="input" value={form.scope} onChange={(e) => setForm({ ...form, scope: e.target.value })} placeholder="e.g. 2026 Q1" />
            </label>
            <label className="field">
              <span className="field-label">Data (optional JSON)</span>
              <Textarea value={form.data} onChange={(e) => setForm({ ...form, data: e.target.value })} placeholder='{"note": "anything"}' />
            </label>
            <div>
              <Button variant="primary" onClick={generate}>
                Generate report
              </Button>
            </div>
          </div>
        </Card>
      ) : null}
      <Card>
        {loading ? (
          <Skeleton lines={4} />
        ) : reports.length === 0 ? (
          <EmptyState title="No reports yet" message="Generate your first report to download PDF/CSV files." />
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Type</th>
                  <th>Format</th>
                  <th>Created</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {reports.map((r) => (
                  <tr key={r.uuid}>
                    <td>
                      <strong>{r.title}</strong>
                    </td>
                    <td>{r.reportType}</td>
                    <td>{r.format}</td>
                    <td className="muted">{new Date(r.createdAt).toLocaleString()}</td>
                    <td style={{ textAlign: 'right' }}>
                      <span className="row" style={{ justifyContent: 'flex-end' }}>
                        <Button variant="secondary" className="btn-sm" onClick={() => void download(`/reports/${r.uuid}/download?format=pdf`, `${r.title}.pdf`)}>
                          PDF
                        </Button>
                        <Button variant="secondary" className="btn-sm" onClick={() => void download(`/reports/${r.uuid}/download?format=csv`, `${r.title}.csv`)}>
                          CSV
                        </Button>
                        <Button variant="danger" className="btn-sm" onClick={() => void remove(r.uuid)}>
                          Delete
                        </Button>
                      </span>
                    </td>
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
