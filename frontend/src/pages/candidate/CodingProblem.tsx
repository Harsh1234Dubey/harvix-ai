import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Card, Spinner, StatusBadge } from '@/components/ui'
import { get, post, errorMessage } from '@/lib/api'
import type { CodingTest, Submission } from '@/lib/types'

interface TestCaseView {
  orderIndex: number
  inputData?: string
  expectedOutput?: string
}

const LANGS: Record<string, string> = {
  JAVA: 'Java',
  PYTHON: 'Python',
  JAVASCRIPT: 'JavaScript',
  CPP: 'C++',
  GO: 'Go',
}

export default function CodingProblem() {
  const { id = '' } = useParams()
  const [test, setTest] = useState<CodingTest | null>(null)
  const [cases, setCases] = useState<TestCaseView[]>([])
  const [code, setCode] = useState('')
  const [lang, setLang] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [result, setResult] = useState<Submission | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    void get<CodingTest>(`/coding/tests/${id}`)
      .then((t) => {
        setTest(t)
        setLang(t.language)
        setCode(t.starterCode ?? '')
      })
      .catch((e) => setError(errorMessage(e)))
    void get<TestCaseView[]>(`/coding/tests/${id}/cases`).then(setCases).catch(() => {})
  }, [id])

  if (error && !test) return <div className="alert alert-error">{error}</div>
  if (!test) return <Spinner />

  const submit = () => {
    setSubmitting(true)
    setError('')
    void post<Submission>('/coding/submissions', { codingTestId: test.id, language: lang, sourceCode: code })
      .then(setResult)
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setSubmitting(false))
  }

  return (
    <div className="grid grid-2">
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <h2 style={{ margin: 0 }}>{test.title}</h2>
            <p className="muted">
              {LANGS[test.language] || test.language} · {test.timeLimitSec}s · {test.memoryLimitMb} MB
            </p>
          </div>
          <StatusBadge status={test.difficulty} />
        </div>
        <p style={{ whiteSpace: 'pre-wrap' }}>{test.description}</p>

        <h4>Example cases</h4>
        {cases.length === 0 ? (
          <p className="muted">No public cases.</p>
        ) : (
          cases.map((c) => (
            <div key={c.orderIndex} className="code-block">
              <p className="muted" style={{ margin: '0 0 4px' }}>
                Case {c.orderIndex}
              </p>
              <div>
                <span className="muted">Input: </span>
                <code>{c.inputData ?? '—'}</code>
              </div>
              <div>
                <span className="muted">Expected: </span>
                <code>{c.expectedOutput ?? '—'}</code>
              </div>
            </div>
          ))
        )}
      </Card>

      <div>
        <Card title="Solution">
          <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
            <select className="input" value={lang} onChange={(e) => setLang(e.target.value)}>
              {Object.entries(LANGS).map(([k, v]) => (
                <option key={k} value={k}>
                  {v}
                </option>
              ))}
            </select>
            <button className="btn btn-primary" disabled={submitting} onClick={submit}>
              {submitting ? 'Evaluating…' : 'Submit'}
            </button>
          </div>
          <textarea
            className="input code-editor"
            rows={16}
            spellCheck={false}
            value={code}
            onChange={(e) => setCode(e.target.value)}
            style={{ fontFamily: 'var(--font-mono)', fontSize: 13 }}
          />
          {error && <div className="alert alert-error">{error}</div>}
        </Card>

        {result && (
          <Card title="Result">
            <div className="grid grid-2">
              <div className="stat-card">
                <span>Status</span>
                <strong>
                  <StatusBadge status={result.status} />
                </strong>
              </div>
              <div className="stat-card">
                <span>Passed</span>
                <strong>
                  {result.passedCases}/{result.totalCases}
                </strong>
              </div>
              {result.codeScore != null && (
                <div className="stat-card">
                  <span>Code score</span>
                  <strong>{result.codeScore}</strong>
                </div>
              )}
              {result.complexityTime || result.complexitySpace ? (
                <div className="stat-card">
                  <span>Complexity</span>
                  <strong>
                    {result.complexityTime ?? '—'} / {result.complexitySpace ?? '—'}
                  </strong>
                </div>
              ) : null}
            </div>
            {result.stdout && (
              <div className="code-block">
                <p className="muted" style={{ marginTop: 0 }}>
                  Output
                </p>
                <pre style={{ margin: 0 }}>{result.stdout}</pre>
              </div>
            )}
            {result.errorMessage && <div className="alert alert-error">{result.errorMessage}</div>}
          </Card>
        )}
      </div>
    </div>
  )
}
