import { useCallback, useEffect, useRef, useState } from 'react'
import { Card, EmptyState, Skeleton } from '@/components/ui'
import { get, post, del, errorMessage, apiUrl } from '@/lib/api'
import type { AtsReportEntry, AtsResult, Job, Resume } from '@/lib/types'

export default function Resumes() {
  const [resumes, setResumes] = useState<Resume[]>([])
  const [jobs, setJobs] = useState<Job[]>([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [scoringId, setScoringId] = useState<number | null>(null)
  const [jobForScore, setJobForScore] = useState<Record<number, number>>({})
  const [ats, setAts] = useState<Record<number, AtsResult>>({})
  const [history, setHistory] = useState<Record<number, AtsReportEntry[]>>({})
  const fileRef = useRef<HTMLInputElement>(null)

  const loadHistory = useCallback((resumeId: number) => {
    void get<AtsReportEntry[]>(`/resumes/${resumeId}/ats-history`)
      .then((entries) => setHistory((prev) => ({ ...prev, [resumeId]: entries })))
      .catch(() => {})
  }, [])

  const load = useCallback(() => {
    void get<Resume[]>('/resumes/me')
      .then((list) => {
        setResumes(list)
        list.forEach((r) => loadHistory(r.id))
      })
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false))
    void get<{ content: Job[] }>('/jobs', { params: { page: 0, size: 100, status: 'PUBLISHED' } })
      .then((p) => setJobs(p.content))
      .catch(() => {})
  }, [loadHistory])

  useEffect(load, [load])

  const upload = (file: File) => {
    if (!file) return
    setUploading(true)
    setError('')
    setSuccess('')
    const fd = new FormData()
    fd.append('file', file)
    void post<Resume>('/resumes/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
      .then(() => {
        setSuccess('Resume uploaded.')
        load()
      })
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setUploading(false))
  }

  const remove = (id: number) => {
    void del<unknown>(`/resumes/${id}`)
      .then(() => {
        setSuccess('Resume deleted.')
        load()
      })
      .catch((e) => setError(errorMessage(e)))
  }

  const checkScore = (id: number) => {
    const jobId = jobForScore[id]
    if (!jobId) {
      setError('Select a job to score against first.')
      return
    }
    setScoringId(id)
    setError('')
    void post<AtsResult>(`/resumes/${id}/ats-score`, { jobId })
      .then((result) => {
        setAts((prev) => ({ ...prev, [id]: result }))
        loadHistory(id)
      })
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setScoringId(null))
  }

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
        <h2 style={{ margin: 0 }}>My resumes</h2>
        <input
          ref={fileRef}
          type="file"
          accept=".pdf"
          hidden
          onChange={(e) => {
            const f = e.target.files?.[0]
            if (f) upload(f)
            e.target.value = ''
          }}
        />
        <button className="btn btn-primary" disabled={uploading} onClick={() => fileRef.current?.click()}>
          {uploading ? 'Uploading…' : 'Upload PDF'}
        </button>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      {loading && <Skeleton lines={4} />}

      {!loading && resumes.length === 0 && (
        <EmptyState title="No resumes yet" hint="Upload a resume to start applying for jobs." />
      )}

      <div className="grid grid-2">
        {resumes.map((r) => (
          <Card key={r.id} title={r.title}>
            <p className="muted" style={{ margin: '0 0 10px' }}>
              {r.primary ? <span className="tag">Primary</span> : null} Version {r.currentVersion} ·{' '}
              {r.versions.length} version(s)
            </p>
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Version</th>
                    <th>Size</th>
                    <th>Uploaded</th>
                    <th>File</th>
                  </tr>
                </thead>
                <tbody>
                  {r.versions.map((v) => (
                    <tr key={v.id}>
                      <td>v{v.versionNo}</td>
                      <td>{v.fileSize ? `${Math.round(v.fileSize / 1024)} KB` : '—'}</td>
                      <td className="muted">{new Date(v.uploadedAt).toLocaleDateString()}</td>
                      <td>
                        <a href={apiUrl(v.filePath)} target="_blank" rel="noreferrer">
                          Open
                        </a>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div style={{ marginTop: 12, borderTop: '1px solid var(--border)', paddingTop: 12 }}>
              <p className="muted" style={{ margin: '0 0 8px' }}>ATS score against a job</p>
              <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
                <select
                  className="input"
                  style={{ width: 220 }}
                  value={jobForScore[r.id] ?? ''}
                  onChange={(e) => setJobForScore((prev) => ({ ...prev, [r.id]: Number(e.target.value) }))}
                >
                  <option value="">Select a job…</option>
                  {jobs.map((j) => (
                    <option key={j.id} value={j.id}>
                      {j.title}
                    </option>
                  ))}
                </select>
                <button className="btn btn-primary btn-sm" disabled={scoringId === r.id} onClick={() => checkScore(r.id)}>
                  {scoringId === r.id ? 'Scoring…' : 'Check ATS score'}
                </button>
              </div>
              {ats[r.id] ? (
                <div style={{ marginTop: 12 }}>
                  <div className="tag" style={{ fontSize: 18, padding: '4px 10px' }}>
                    Score: {ats[r.id].score}/100
                  </div>
                  <span className="muted" style={{ marginLeft: 8, fontSize: 12 }}>
                    {ats[r.id].source === 'AI' ? 'Gemini AI' : 'Keyword fallback'}
                  </span>
                  {ats[r.id].summary && <p style={{ margin: '10px 0 8px' }}>{ats[r.id].summary}</p>}
                  {ats[r.id].strengths.length > 0 && (
                    <p className="muted" style={{ margin: '4px 0' }}>
                      <strong>Strengths:</strong> {ats[r.id].strengths.join(' · ')}
                    </p>
                  )}
                  {ats[r.id].gaps.length > 0 && (
                    <p className="muted" style={{ margin: '4px 0' }}>
                      <strong>Gaps:</strong> {ats[r.id].gaps.join(' · ')}
                    </p>
                  )}
                  {ats[r.id].missingKeywords.length > 0 && (
                    <p className="muted" style={{ margin: '4px 0' }}>
                      <strong>Missing keywords:</strong>{' '}
                      {ats[r.id].missingKeywords.map((k) => (
                        <span key={k} className="tag" style={{ marginRight: 4 }}>
                          {k}
                        </span>
                      ))}
                    </p>
                  )}
                </div>
              ) : null}
              {(history[r.id]?.length ?? 0) > 0 && (
                <div style={{ marginTop: 12 }}>
                  <p className="muted" style={{ margin: '0 0 6px' }}>Score history</p>
                  <div className="table-wrap">
                    <table className="table">
                      <thead>
                        <tr>
                          <th>Version</th>
                          <th>Job</th>
                          <th>Score</th>
                          <th>Date</th>
                        </tr>
                      </thead>
                      <tbody>
                        {history[r.id].map((h) => (
                          <tr key={h.id}>
                            <td>v{h.versionNo}</td>
                            <td>{h.jobTitle}</td>
                            <td>
                              {h.score}
                              <span className="muted" style={{ fontSize: 11 }}>
                                {' '}
                                {h.source === 'AI' ? '(AI)' : '(kw)'}
                              </span>
                            </td>
                            <td className="muted">{new Date(h.createdAt).toLocaleDateString()}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>
            <button className="btn btn-danger btn-sm" style={{ marginTop: 10 }} onClick={() => remove(r.id)}>
              Delete
            </button>
          </Card>
        ))}
      </div>
    </>
  )
}
