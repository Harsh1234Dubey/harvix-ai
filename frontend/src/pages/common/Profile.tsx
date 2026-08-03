import { useEffect, useState, type FormEvent } from 'react'
import { useAuth } from '@/providers/AuthProvider'
import { Button, Card, Field, Input } from '@/components/ui'
import { errorMessage, get, put } from '@/lib/api'
import type { User } from '@/lib/types'

export default function Profile() {
  const { user, setUser } = useAuth()
  const [form, setForm] = useState({ firstName: '', lastName: '', phone: '', avatarUrl: '' })
  const [pw, setPw] = useState({ currentPassword: '', newPassword: '', confirm: '' })
  const [msg, setMsg] = useState('')
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (user) setForm({ firstName: user.firstName, lastName: user.lastName, phone: user.phone ?? '', avatarUrl: user.avatarUrl ?? '' })
  }, [user])

  async function saveProfile(e: FormEvent) {
    e.preventDefault()
    setErr('')
    setMsg('')
    setBusy(true)
    try {
      const updated = await put<User>('/users/me', form)
      setUser(updated)
      setMsg('Profile updated.')
    } catch (ex) {
      setErr(errorMessage(ex))
    } finally {
      setBusy(false)
    }
  }

  async function savePassword(e: FormEvent) {
    e.preventDefault()
    setErr('')
    setMsg('')
    if (pw.newPassword !== pw.confirm) {
      setErr('New passwords do not match')
      return
    }
    setBusy(true)
    try {
      await put('/users/me/password', { currentPassword: pw.currentPassword, newPassword: pw.newPassword })
      setMsg('Password updated.')
      setPw({ currentPassword: '', newPassword: '', confirm: '' })
    } catch (ex) {
      setErr(errorMessage(ex))
    } finally {
      setBusy(false)
    }
  }

  if (!user) return null

  return (
    <>
      <h2>Profile</h2>
      {msg ? <div className="alert alert-success">{msg}</div> : null}
      {err ? <div className="alert alert-error">{err}</div> : null}
      <div className="grid grid-2">
        <Card title="Personal details">
          <form onSubmit={saveProfile} className="grid" style={{ gap: 14 }}>
            <div className="row">
              <Field label="First name">
                <Input value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} required />
              </Field>
              <Field label="Last name">
                <Input value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} required />
              </Field>
            </div>
            <Field label="Email (read-only)">
              <Input value={user.email} disabled />
            </Field>
            <Field label="Phone">
              <Input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
            </Field>
            <Field label="Avatar URL">
              <Input value={form.avatarUrl} onChange={(e) => setForm({ ...form, avatarUrl: e.target.value })} placeholder="https://…" />
            </Field>
            <Button type="submit" variant="primary" loading={busy}>
              Save profile
            </Button>
          </form>
        </Card>
        <Card title="Change password">
          <form onSubmit={savePassword} className="grid" style={{ gap: 14 }}>
            <Field label="Current password">
              <Input type="password" value={pw.currentPassword} onChange={(e) => setPw({ ...pw, currentPassword: e.target.value })} required />
            </Field>
            <Field label="New password">
              <Input type="password" value={pw.newPassword} onChange={(e) => setPw({ ...pw, newPassword: e.target.value })} required />
            </Field>
            <Field label="Confirm new password">
              <Input type="password" value={pw.confirm} onChange={(e) => setPw({ ...pw, confirm: e.target.value })} required />
            </Field>
            <Button type="submit" variant="primary" loading={busy}>
              Update password
            </Button>
          </form>
        </Card>
      </div>
    </>
  )
}
