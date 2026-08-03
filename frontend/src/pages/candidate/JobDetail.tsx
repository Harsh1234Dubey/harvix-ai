import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Card, Spinner, StatusBadge } from '@/components/ui'
import { get, post, errorMessage } from '@/lib/api'
import { useAuth } from '@/providers/AuthProvider'
import type { Application, Job, Resume } from '@/lib/types'

export default function JobDetail() {
  const { uuid = '' } = useParams()
  const { user } = useAuth()
  const [job, setJob] = useState<Job | null>(null)
  const [resumes, setResumes] = useState<Resume[]>([])
  const [coverLetter, setCoverLetter] = useState('')
  const [resumeId, setResumeId] = useState<number | ''>('')
  const [applying, setApplying] = useState(false)
  const [applied, setApplied] = useState<Application | null>(null)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    void get<Job>(`/jobs/${uuid}`).then(setJob).catch((e) => setError(errorMessage(e)))
  }, [uuid])

  useEffect(() => {
    if (!user?.roles.includes('CANDIDATE')) return
    void get<{ content: Resume[] }>('/resumes/me').then((p) => {
      setResumes(p.content)
      const primary = p.content.find((r) => r.primary)
      setResumeId(primary ? primary.id : (p.content[0]?.id ?? ''))
    })
  }, [user])

  useEffect(() => {
    if (!user?.roles.includes('CANDIDATE') || !job) return
    void get<{ content: Application[] }>('/applications/me')
      .then((p) => setApplied(p.content.find((a) => a.jobId === job.id) ?? null))
      .catch(() => {})
  }, [user, job])

  useEffect(() => {
    if (job) void post<void>(`/jobs/${job.uuid}/view`, {}).catch(() => {})
  }, [job])

  if (error && !job) return <div className="alert alert-error">{error}</div>
  if (!job) return <Spinner />

  const isCandidate = user?.roles.includes('CANDIDATE')
  const canApply = isCandidate && !applied

  const submit = () => {
    setApplying(true)
    setError('')
    void post<Application>('/applications', {
      ...(resumeId ? { resumeId: Number(resumeId) } : {}),
      ...(coverLetter.trim() ? { coverLetter } : {}),
    }, { params: { jobUuid: job.uuid } })
      .then((app) => {
        setApplied(app)
        setSuccess('Application submitted!')
      })
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setApplying(false))
  }

  return (
    <>
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12, flexWrap: 'wrap' }}>
          <div>
            <h2 style={{ margin: 0 }}>{job.title}</h2>
            <p className="muted">{job.companyName} · {job.location || 'Remote'}</p>
          </div>
          <StatusBadge status={job.status} />
        </div>

        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', margin: '12px 0' }}>
          <span className="chip">{job.workMode}</span>
          <span className="chip">{job.employmentType}</span>
          {job.experienceMin != null && <span className="chip">{job.experienceMin}–{job.experienceMax ?? '∞'} years</span>}
          {job.salaryMin != null && <span className="chip">{job.currency} {job.salaryMin}–{job.salaryMax}</span>}
          <span className="chip">{job.vacancyCount} open position(s)</span>
        </div>

        <h4>Description</h4>
        <p style={{ whiteSpace: 'pre-wrap' }}>{job.description}</p>
      </Card>

      <Card title="Required skills">
        {job.requiredSkills.map((s) => (
          <span key={s} className="tag">
            {s}
          </span>
        ))}
      </Card>

      <div style={{ display: 'flex', gap: 16 }}>
        <Card title="Job stats" className="flex-1">
          <p className="muted" style={{ margin: 0 }}>
            {job.viewsCount} views · {job.applicationsCount} applicants
          </p>
        </Card>
        <Card title="Dates" className="flex-1">
          <p className="muted" style={{ margin: 0 }}>
            Published {job.publishedAt ? new Date(job.publishedAt).toLocaleDateString() : '—'}
            {job.expiresAt && ` · Closes ${new Date(job.expiresAt).toLocaleDateString()}`}
          </p>
        </Card>
      </div>

      {isCandidate && (
        <Card title={applied ? 'Application' : 'Apply now'}>
          {applied ? (
            <>
              <StatusBadge status={applied.status} />
              <p className="muted" style={{ marginTop: 8 }}>
                Applied {new Date(applied.appliedAt).toLocaleString()}
                {applied.atsScore != null && ` · ATS ${applied.atsScore}/100`}
              </p>
            </>
          ) : (
            <>
              {resumes.length === 0 ? (
                <p className="muted">
                  You need a resume first —{' '}
                  <a href="/app/resumes">upload one</a> before applying.
                </p>
              ) : (
                <>
                  <div className="form-row">
                    <label>Resume</label>
                    <select className="input" value={resumeId} onChange={(e) => setResumeId(e.target.value ? Number(e.target.value) : '')}>
                      {resumes.map((r) => (
                        <option key={r.id} value={r.id}>
                          {r.title}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="form-row">
                    <label>Cover letter (optional)</label>
                    <textarea className="input" rows={4} value={coverLetter} onChange={(e) => setCoverLetter(e.target.value)} placeholder="Why are you a good fit?" />
                  </div>
                  {error && <div className="alert alert-error">{error}</div>}
                  {success && <div className="alert alert-success">{success}</div>}
                  <button className="btn btn-primary" disabled={applying || resumeId === ''} onClick={submit}>
                    {applying ? 'Submitting…' : 'Submit application'}
                  </button>
                </>
              )}
            </>
          )}
        </Card>
      )}
    </>
  )
}
