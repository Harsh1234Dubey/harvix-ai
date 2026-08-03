import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '@/providers/AuthProvider'
import { Button, Field, Input } from '@/components/ui'
import { errorMessage } from '@/lib/api'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [remember, setRemember] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await login(email, password, remember)
      const from = (location.state as { from?: string } | null)?.from ?? '/app/dashboard'
      navigate(from, { replace: true })
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
        <p className="auth-sub">Sign in to your workspace</p>
        {error ? <div className="alert alert-error">{error}</div> : null}
        <div className="grid" style={{ gap: 14 }}>
          <Field label="Email">
            <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
          </Field>
          <Field label="Password">
            <Input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </Field>
          <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 14 }}>
            <input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} />
            Remember me for 30 days
          </label>
          <Button type="submit" variant="primary" loading={busy}>
            Sign In
          </Button>
          <Link to="/forgot-password" className="muted" style={{ textAlign: 'center', fontSize: 13 }}>
            Forgot password?
          </Link>
          <div className="divider">or</div>
          <Link to="/register">
            <Button type="button" variant="secondary" style={{ width: '100%' }}>
              Create an account
            </Button>
          </Link>
        </div>
      </form>
    </div>
  )
}

