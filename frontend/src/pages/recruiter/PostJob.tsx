import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Skeleton } from '@/components/ui'
import { get, post, errorMessage } from '@/lib/api'
import type { Company, Job } from '@/lib/types'

const EMPTY = {
  title: '',
  description: '',
  requirements: '',
  responsibilities: '',
  location: '',
  workMode: 'REMOTE',
  employmentType: 'FULL_TIME',
  experienceMin: '',
  experienceMax: '',
  salaryMin: '',
  salaryMax: '',
  currency: 'USD',
  vacancyCount: '1',
  requiredSkills: '',
}

export default function PostJob() {
  const navigate = useNavigate()
  const [companies, setCompanies] = useState<Company[]>([])
  const [companyId, setCompanyId] = useState<number | ''>('')
  const [form, setForm] = useState({ ...EMPTY })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    void get<Company[]>('/companies/mine')
      .then((list) => {
        setCompanies(list)
        if (list[0]) setCompanyId(list[0].id)
      })
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false))
  }, [])

  const set = (k: string) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.value }))

  const submit = () => {
    if (companyId === '') return
    setSaving(true)
    setError('')
    const skills = form.requiredSkills.split(',').map((s) => s.trim()).filter(Boolean)
    void post<Job>('/jobs', {
      title: form.title,
      description: form.description,
      requirements: form.requirements,
      responsibilities: form.responsibilities,
      location: form.location || null,
      workMode: form.workMode,
      employmentType: form.employmentType,
      experienceMin: form.experienceMin ? Number(form.experienceMin) : null,
      experienceMax: form.experienceMax ? Number(form.experienceMax) : null,
      salaryMin: form.salaryMin ? Number(form.salaryMin) : null,
      salaryMax: form.salaryMax ? Number(form.salaryMax) : null,
      currency: form.currency,
      vacancyCount: Number(form.vacancyCount),
      requiredSkills: skills,
    }, { params: { companyId } })
      .then((job) => navigate(`/app/jobs/${job.uuid}`))
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setSaving(false))
  }

  if (loading) return <Skeleton lines={6} />

  if (companies.length === 0) {
    return (
      <Card title="Create your company first">
        <p className="muted">You must create a company before posting jobs.</p>
        <button className="btn btn-primary" onClick={() => navigate('/app/company')}>
          Go to company settings
        </button>
      </Card>
    )
  }

  return (
    <>
      <h2>Post a job</h2>
      {error && <div className="alert alert-error">{error}</div>}
      <Card title="Job details">
        <div className="form-row">
          <label>Company</label>
          <select className="input" value={companyId} onChange={(e) => setCompanyId(e.target.value ? Number(e.target.value) : '')}>
            {companies.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>
        <div className="form-row">
          <label>Job title *</label>
          <input className="input" value={form.title} onChange={set('title')} placeholder="e.g. Senior Backend Engineer" />
        </div>
        <div className="form-row">
          <label>Description *</label>
          <textarea className="input" rows={5} value={form.description} onChange={set('description')} />
        </div>
        <div className="grid grid-2">
          <div className="form-row">
            <label>Requirements</label>
            <textarea className="input" rows={4} value={form.requirements} onChange={set('requirements')} placeholder="Line-separated or comma-separated" />
          </div>
          <div className="form-row">
            <label>Responsibilities</label>
            <textarea className="input" rows={4} value={form.responsibilities} onChange={set('responsibilities')} />
          </div>
        </div>
        <div className="grid grid-2">
          <div className="form-row">
            <label>Location</label>
            <input className="input" value={form.location} onChange={set('location')} />
          </div>
          <div className="form-row">
            <label>Work mode</label>
            <select className="input" value={form.workMode} onChange={set('workMode')}>
              <option>REMOTE</option>
              <option>HYBRID</option>
              <option>ONSITE</option>
            </select>
          </div>
          <div className="form-row">
            <label>Employment type</label>
            <select className="input" value={form.employmentType} onChange={set('employmentType')}>
              <option>FULL_TIME</option>
              <option>PART_TIME</option>
              <option>CONTRACT</option>
              <option>INTERNSHIP</option>
            </select>
          </div>
          <div className="form-row">
            <label>Vacancies</label>
            <input className="input" type="number" min={1} value={form.vacancyCount} onChange={set('vacancyCount')} />
          </div>
          <div className="form-row">
            <label>Min experience (yrs)</label>
            <input className="input" type="number" value={form.experienceMin} onChange={set('experienceMin')} />
          </div>
          <div className="form-row">
            <label>Max experience (yrs)</label>
            <input className="input" type="number" value={form.experienceMax} onChange={set('experienceMax')} />
          </div>
          <div className="form-row">
            <label>Min salary</label>
            <input className="input" type="number" value={form.salaryMin} onChange={set('salaryMin')} />
          </div>
          <div className="form-row">
            <label>Max salary</label>
            <input className="input" type="number" value={form.salaryMax} onChange={set('salaryMax')} />
          </div>
          <div className="form-row">
            <label>Currency</label>
            <input className="input" value={form.currency} onChange={set('currency')} />
          </div>
          <div className="form-row">
            <label>Required skills (comma-separated)</label>
            <input className="input" value={form.requiredSkills} onChange={set('requiredSkills')} placeholder="Java, Spring Boot, SQL" />
          </div>
        </div>
        <button className="btn btn-primary" disabled={saving || !form.title || !form.description} onClick={submit}>
          {saving ? 'Creating…' : 'Create job'}
        </button>
      </Card>
    </>
  )
}
