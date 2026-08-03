import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { Button, Field, Input } from '@/components/ui'
import { errorMessage, post } from '@/lib/api'

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await post('/auth/forgot-password', { email })
      setMessage('If the email exists, a reset link has been sent (simulated email).')
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-wrap">
      <form className="card auth-card" onSubmit={submit}>
        <div className="auth-brand">
          <span className="brand-dot" />
          Harvix AI
        </div>
        <p className="auth-sub">Reset your password</p>
        {error ? <div className="alert alert-error">{error}</div> : null}
        {message ? <div className="alert alert-info">{message}</div> : null}
        <div className="grid" style={{ gap: 14 }}>
          <Field label="Email">
            <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
          </Field>
          <Button type="submit" variant="primary" loading={busy}>
            Send reset link
          </Button>
          <Link to="/login" className="muted" style={{ textAlign: 'center', fontSize: 13 }}>
            Back to sign in
          </Link>
        </div>
      </form>
    </div>
  )
}

