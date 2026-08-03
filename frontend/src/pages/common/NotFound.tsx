import { Link } from 'react-router-dom'
import { Button } from '@/components/ui'

export default function NotFound() {
  return (
    <div className="auth-wrap">
      <div className="card auth-card" style={{ textAlign: 'center' }}>
        <h1 style={{ fontSize: 60, margin: 0 }}>404</h1>
        <p className="muted">The page you are looking for does not exist.</p>
        <Link to="/">
          <Button variant="primary">Go home</Button>
        </Link>
      </div>
    </div>
  )
}
