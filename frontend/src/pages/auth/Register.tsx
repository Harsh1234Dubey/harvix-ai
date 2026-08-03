import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Button, Field, Input } from '@/components/ui'
import { errorMessage, post } from '@/lib/api'
import type { ApiResponse } from '@/lib/types'

export default function Register() {
  const navigate = useNavigate()
  const [role, setRole] = useState('CANDIDATE')
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [agree, setAgree] = useState(false)
  const [error, setError] = useState('')
  const [info, setInfo] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError('')
    if (password !== confirm) {
      setError('Passwords do not match')
      return
    }
    if (!agree) {
      setError('Please accept the terms to continue')
      return
    }
    setBusy(true)
    try {
      const res = await post<ApiResponse<never>>('/auth/register', {
        firstName,
        lastName,
        email,
        password,
        role,
      })
      setInfo(res.message ?? 'Registration successful.')
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
        <p className="auth-sub">Create your account</p>
        {error ? <div className="alert alert-error">{error}</div> : null}
        {info ? (
          <div className="alert alert-info">
            {info}
            <div style={{ marginTop: 10 }}>
              <Link to="/verify-email">
                <Button type="button" variant="primary" className="btn-sm">
                  I have a verification token
                </Button>
              </Link>
            </div>
          </div>
        ) : null}
        {!info ? (
          <div className="grid" style={{ gap: 14 }}>
            <div>
              <span className="field-label">I am a</span>
              <div className="role-picker" style={{ marginTop: 6 }}>
                <div className={`role-option ${role === 'CANDIDATE' ? 'active' : ''}`} onClick={() => setRole('CANDIDATE')}>
                  Candidate
                </div>
                <div className={`role-option ${role === 'RECRUITER' ? 'active' : ''}`} onClick={() => setRole('RECRUITER')}>
                  Recruiter
                </div>
              </div>
            </div>
            <div className="row">
              <Field label="First name">
                <Input value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
              </Field>
              <Field label="Last name">
                <Input value={lastName} onChange={(e) => setLastName(e.target.value)} required />
              </Field>
            </div>
            <Field label="Email">
              <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
            </Field>
            <Field label="Password">
              <Input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
            </Field>
            <Field label="Confirm password">
              <Input type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} required />
            </Field>
            <label style={{ display: 'flex', gap: 8, alignItems: 'flex-start', fontSize: 13 }}>
              <input type="checkbox" checked={agree} onChange={(e) => setAgree(e.target.checked)} />
              I agree to the terms of service and privacy policy.
            </label>
            <Button type="submit" variant="primary" loading={busy}>
              Create Account
            </Button>
            <div className="divider">or</div>
            <Link to="/login">
              <Button type="button" variant="secondary" style={{ width: '100%' }}>
                Already have an account?
              </Button>
            </Link>
          </div>
        ) : null}
      </form>
    </div>
  )
}

