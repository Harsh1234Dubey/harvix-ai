import { useEffect, useState } from 'react'
import { Card, Skeleton } from '@/components/ui'
import { get, post, put, errorMessage } from '@/lib/api'
import type { Company, CompanyMember } from '@/lib/types'

interface MemberView {
  id: number
  userId: number
  name: string
  role?: string
  owner: boolean
}

const EMPTY: Record<string, string> = {
  name: '',
  description: '',
  website: '',
  industry: '',
  location: '',
  sizeRange: '',
  logoUrl: '',
  brandingColor: '',
  foundedYear: '',
}

export default function CompanyPage() {
  const [companies, setCompanies] = useState<Company[]>([])
  const [members, setMembers] = useState<MemberView[]>([])
  const [form, setForm] = useState({ ...EMPTY })
  const [editId, setEditId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    void get<Company[]>('/companies/mine')
      .then((list) => {
        setCompanies(list)
        if (list.length > 0) {
          const c = list[0]
          setEditId(c.id)
          setForm({
            name: c.name,
            description: c.description ?? '',
            website: c.website ?? '',
            industry: c.industry ?? '',
            location: c.location ?? '',
            sizeRange: c.sizeRange ?? '',
            logoUrl: c.logoUrl ?? '',
            brandingColor: c.brandingColor ?? '',
            foundedYear: c.foundedYear ? String(c.foundedYear) : '',
          })
          void get<MemberView[]>(`/companies/${c.id}/members`).then(setMembers).catch(() => {})
        }
      })
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false))
  }, [])

  const set = (k: string) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.value }))

  const save = () => {
    setSaving(true)
    setError('')
    setSuccess('')
    const payload = {
      name: form.name,
      description: form.description,
      website: form.website,
      industry: form.industry,
      location: form.location,
      sizeRange: form.sizeRange,
      logoUrl: form.logoUrl,
      brandingColor: form.brandingColor,
      foundedYear: form.foundedYear ? Number(form.foundedYear) : null,
    }
    const done = (c: Company) => {
      setEditId(c.id)
      setSuccess('Saved successfully.')
      setCompanies((prev) => (prev.find((p) => p.id === c.id) ? prev.map((p) => (p.id === c.id ? c : p)) : [...prev, c]))
    }
    const req = editId
      ? put<Company>(`/companies/${editId}`, payload)
      : post<Company>('/companies', payload)
    req.then(done).catch((e) => setError(errorMessage(e))).finally(() => setSaving(false))
  }

  if (loading) return <Skeleton lines={6} />

  return (
    <>
      <h2>{editId ? 'Edit company' : 'Create company'}</h2>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="grid grid-2">
        <Card title="Company profile">
          <div className="form-row">
            <label>Company name *</label>
            <input className="input" value={form.name} onChange={set('name')} />
          </div>
          <div className="form-row">
            <label>Industry</label>
            <input className="input" value={form.industry} onChange={set('industry')} placeholder="e.g. SaaS, Fintech" />
          </div>
          <div className="form-row">
            <label>Location</label>
            <input className="input" value={form.location} onChange={set('location')} />
          </div>
          <div className="form-row">
            <label>Size range</label>
            <select className="input" value={form.sizeRange} onChange={set('sizeRange')}>
              <option value="">Select…</option>
              <option>1-10</option>
              <option>11-50</option>
              <option>51-200</option>
              <option>201-500</option>
              <option>501-1000</option>
              <option>1000+</option>
            </select>
          </div>
          <div className="form-row">
            <label>Founded year</label>
            <input className="input" type="number" value={form.foundedYear} onChange={set('foundedYear')} />
          </div>
          <div className="form-row">
            <label>Website</label>
            <input className="input" value={form.website} onChange={set('website')} />
          </div>
          <div className="form-row">
            <label>Description</label>
            <textarea className="input" rows={4} value={form.description} onChange={set('description')} />
          </div>
          <div className="form-row">
            <label>Logo URL</label>
            <input className="input" value={form.logoUrl} onChange={set('logoUrl')} />
          </div>
          <div className="form-row">
            <label>Branding color</label>
            <input className="input" type="color" value={form.brandingColor || '#6366f1'} onChange={set('brandingColor')} />
          </div>
          <button className="btn btn-primary" disabled={saving || !form.name} onClick={save}>
            {saving ? 'Saving…' : editId ? 'Save changes' : 'Create company'}
          </button>
        </Card>

        <div>
          {editId && (
            <Card title="Members">
              {members.length === 0 ? (
                <p className="muted">No members.</p>
              ) : (
                <div className="table-wrap">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Name</th>
                        <th>Role</th>
                      </tr>
                    </thead>
                    <tbody>
                      {members.map((m) => (
                        <tr key={m.id}>
                          <td>{m.name}</td>
                          <td>{m.owner ? 'Owner' : m.role ?? 'Member'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </Card>
          )}

          {companies.length > 0 && (
            <Card title="Your companies">
              {companies.map((c) => (
                <div key={c.id} className="job-card" style={{ borderBottom: '1px solid var(--border)', padding: '10px 0' }}>
                  <strong>{c.name}</strong>
                  <span className="muted"> · {c.industry ?? '—'}</span>
                </div>
              ))}
            </Card>
          )}
        </div>
      </div>
    </>
  )
}
