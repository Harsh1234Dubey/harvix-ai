import { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Card, Spinner, StatusBadge } from '@/components/ui'
import { get, post, errorMessage } from '@/lib/api'
import type { InterviewFeedback, InterviewQuestion } from '@/lib/types'

interface SessionInfo {
  sessionId: number
  uuid: string
  skill?: string
  difficulty?: string
  status: string
  totalQuestions: number
  answeredQuestions: number
}

export default function InterviewSession() {
  const { id = '' } = useParams()
  const [session, setSession] = useState<SessionInfo | null>(null)
  const [questions, setQuestions] = useState<InterviewQuestion[]>([])
  const [idx, setIdx] = useState(0)
  const [answer, setAnswer] = useState('')
  const [answers, setAnswers] = useState<Record<number, string>>({})
  const [submitting, setSubmitting] = useState(false)
  const [feedback, setFeedback] = useState<InterviewFeedback | null>(null)
  const [error, setError] = useState('')
  const [secondsLeft, setSecondsLeft] = useState<number | null>(null)
  const timerRef = useRef<number | null>(null)

  const cur = questions[idx]

  useEffect(() => {
    void get<SessionInfo>(`/interviews/sessions/${id}`)
      .then(setSession)
      .catch((e) => setError(errorMessage(e)))
    void get<InterviewQuestion[]>(`/interviews/sessions/${id}/questions`)
      .then((q) => {
        setQuestions(q)
        if (q.length > 0) setSecondsLeft(120)
      })
      .catch((e) => setError(errorMessage(e)))
    return () => {
      if (timerRef.current) window.clearInterval(timerRef.current)
    }
  }, [id])

  useEffect(() => {
    if (secondsLeft == null) return
    if (secondsLeft <= 0) {
      if (timerRef.current) window.clearInterval(timerRef.current)
      return
    }
    timerRef.current = window.setInterval(() => setSecondsLeft((s) => (s ?? 120) - 1), 1000)
    return () => {
      if (timerRef.current) window.clearInterval(timerRef.current)
    }
  }, [secondsLeft])

  if (error && !session) return <div className="alert alert-error">{error}</div>
  if (!session || !cur) return <Spinner />

  const submitAnswer = (next = true) => {
    if (!answer.trim()) return
    setSubmitting(true)
    setError('')
    void post<unknown>(`/interviews/sessions/${id}/answers`, { questionId: cur.id, answerText: answer })
      .then(() => {
        setAnswers((a) => ({ ...a, [cur.id]: answer }))
        setAnswer('')
        if (next && idx + 1 < questions.length) {
          setIdx(idx + 1)
          setSecondsLeft(120)
        } else if (!next) {
          setSecondsLeft(null)
          if (timerRef.current) window.clearInterval(timerRef.current)
        }
      })
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setSubmitting(false))
  }

  const finish = () => {
    setSecondsLeft(null)
    if (timerRef.current) window.clearInterval(timerRef.current)
    setSubmitting(true)
    setError('')
    void post<InterviewFeedback>(`/interviews/sessions/${id}/feedback`, {})
      .then(setFeedback)
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setSubmitting(false))
  }

  return (
    <>
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
          <div>
            <h2 style={{ margin: 0 }}>AI Mock Interview</h2>
            <p className="muted" style={{ margin: 0 }}>
              {session.skill} · {session.difficulty} · {session.answeredQuestions}/{session.totalQuestions} answered
            </p>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <StatusBadge status={session.status} />
            {secondsLeft != null && (
              <span className={secondsLeft <= 10 ? 'timer timer-danger' : 'timer'}>
                {Math.floor(secondsLeft / 60)}:{String(secondsLeft % 60).padStart(2, '0')}
              </span>
            )}
          </div>
        </div>
      </Card>

      <div className="steps">
        {questions.map((q, i) => (
          <div key={q.id} className={`step ${i === idx ? 'step-active' : ''} ${answers[q.id] ? 'step-done' : ''}`}>
            {i + 1}
          </div>
        ))}
      </div>

      {feedback ? (
        <Card title="Session feedback">
          <div className="grid grid-4">
            {[
              ['Overall', feedback.overallScore],
              ['Communication', feedback.communicationScore],
              ['Technical', feedback.technicalScore],
              ['Confidence', feedback.confidenceScore],
            ].map(([label, value]) => (
              <div key={String(label)} className="stat-card">
                <span>{String(label)}</span>
                <strong>{value != null ? Number(value).toFixed(1) : '—'}</strong>
              </div>
            ))}
          </div>
          {feedback.strengths.length > 0 && (
            <p>
              <strong>Strengths:</strong>{' '}
              <ul style={{ margin: '6px 0 0 18px' }}>
                {feedback.strengths.map((s, i) => (
                  <li key={i}>{s}</li>
                ))}
              </ul>
            </p>
          )}
          {feedback.weaknesses.length > 0 && (
            <p>
              <strong>To improve:</strong>{' '}
              <ul style={{ margin: '6px 0 0 18px' }}>
                {feedback.weaknesses.map((w, i) => (
                  <li key={i}>{w}</li>
                ))}
              </ul>
            </p>
          )}
          {feedback.suggestions && (
            <p>
              <strong>Suggestions:</strong> {feedback.suggestions}
            </p>
          )}
          {feedback.hiringRecommendation && (
            <p>
              <strong>Recommendation:</strong> {feedback.hiringRecommendation}
            </p>
          )}
          {feedback.detailed && feedback.detailed.length > 0 && (
            <>
              <h4>Per-question breakdown</h4>
              {feedback.detailed.map((d, i) => (
                <div key={i} className="job-card" style={{ borderBottom: '1px solid var(--border)', padding: '8px 0' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                    <strong style={{ flex: 1 }}>{d.question}</strong>
                    <span className="chip">{d.score != null ? `${d.score}/10` : '—'}</span>
                  </div>
                  {d.comment && <p className="muted" style={{ margin: '4px 0 0' }}>{d.comment}</p>}
                </div>
              ))}
            </>
          )}
          <a href="/app/interviews">
            <button className="btn btn-secondary">Back to interviews</button>
          </a>
        </Card>
      ) : (
        <Card>
          <h4>
            Question {idx + 1} of {questions.length}
            <span className="chip" style={{ marginLeft: 8 }}>
              {cur.difficulty}
            </span>
          </h4>
          <p style={{ fontSize: 18 }}>{cur.questionText}</p>
          <div className="form-row">
            <label>Your answer</label>
            <textarea
              className="input"
              rows={7}
              value={answer}
              onChange={(e) => setAnswer(e.target.value)}
              placeholder="Speak / type your answer as you would in a real interview…"
            />
          </div>
          {error && <div className="alert alert-error">{error}</div>}
          <div style={{ display: 'flex', gap: 8, justifyContent: 'space-between', flexWrap: 'wrap' }}>
            <button className="btn btn-secondary" disabled={idx === 0} onClick={() => setIdx(idx - 1)}>
              ← Previous
            </button>
            <div style={{ display: 'flex', gap: 8 }}>
              {idx + 1 < questions.length ? (
                <button className="btn btn-primary" disabled={submitting || !answer.trim()} onClick={() => submitAnswer(true)}>
                  {submitting ? 'Saving…' : 'Save & next →'}
                </button>
              ) : (
                <button className="btn btn-primary" disabled={submitting} onClick={finish}>
                  {submitting ? 'Finishing…' : 'Finish interview'}
                </button>
              )}
            </div>
          </div>
        </Card>
      )}
    </>
  )
}
