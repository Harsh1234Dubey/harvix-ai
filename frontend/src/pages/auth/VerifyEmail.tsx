import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Button, Field, Input } from '@/components/ui'
import { errorMessage, post } from '@/lib/api'

export default function VerifyEmail() {
  const navigate = useNavigate()
  const [token, setToken] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await post('/auth/verify-email', { token })
      setSuccess('Email verified successfully. You can now sign in.')
      setTimeout(() => navigate('/login'), 1500)
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
        <p className="auth-sub">Verify your email</p>
        {error ? <div className="alert alert-error">{error}</div> : null}
        {success ? <div className="alert alert-success">{success}</div> : null}
        <div className="grid" style={{ gap: 14 }}>
          <Field label="Verification token">
            <Input value={token} onChange={(e) => setToken(e.target.value)} required autoFocus placeholder="Paste the token from the simulated email" />
          </Field>
          <Button type="submit" variant="primary" loading={busy}>
            Verify email
          </Button>
          <Link to="/login" className="muted" style={{ textAlign: 'center', fontSize: 13 }}>
            Back to sign in
          </Link>
        </div>
      </form>
    </div>
  )
}

