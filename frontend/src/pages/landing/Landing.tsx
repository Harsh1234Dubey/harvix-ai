import { Link } from 'react-router-dom'
import { Button } from '@/components/ui'

const features = [
  { icon: '◉', title: 'AI Mock Interviews', desc: 'Realistic interview practice with instant AI feedback on communication, confidence and technical depth.' },
  { icon: '⌘', title: 'Coding Platform', desc: 'Solve curated challenges in a full editor with test cases and automatic evaluation.' },
  { icon: '▤', title: 'Resume Intelligence', desc: 'Upload your resume and get an ATS score with skill-gap analysis and suggestions.' },
  { icon: '⌗', title: 'Smart Job Matching', desc: 'Machine-ranked job matches based on your skills, experience and resume.' },
  { icon: '◔', title: 'Hiring Analytics', desc: 'Recruiters get a live pipeline funnel, skill distribution and candidate scoring.' },
  { icon: '◫', title: 'Gamified Growth', desc: 'Earn XP, unlock badges, keep streaks and climb the candidate leaderboard.' },
]

export default function Landing() {
  return (
    <div className="landing">
      <nav className="landing-nav">
        <span className="brand">
          <span className="brand-dot" />
          Harvix AI
        </span>
        <span className="row" style={{ flex: 'none' }}>
          <Link to="/login">
            <Button variant="ghost">Log in</Button>
          </Link>
          <Link to="/register">
            <Button>Get Started</Button>
          </Link>
        </span>
      </nav>

      <section className="hero">
        <div>
          <h1>AI-powered hiring. Done right.</h1>
          <p className="lead">
            Harvix AI is the complete recruitment ecosystem — AI mock interviews, a coding
            platform, resume intelligence and smart hiring analytics for candidates, recruiters
            and admins.
          </p>
          <div className="hero-cta">
            <Link to="/register">
              <Button variant="primary" className="btn-lg">
                Get Started Free
              </Button>
            </Link>
            <Link to="/login">
              <Button className="btn-lg">Sign In</Button>
            </Link>
          </div>
        </div>
        <div className="hero-mock">
          <p style={{ fontSize: 20, margin: 0 }}>AI Interview Session</p>
          <p style={{ color: 'var(--warning)', fontWeight: 800, fontSize: 30, margin: '12px 0' }}>
            ● REC
          </p>
          <p className="muted">“Explain how two-pointer technique works and give an example.”</p>
          <div className="score-ring" style={{ '--score': 82 } as React.CSSProperties}>
            <span>82</span>
          </div>
          <p className="muted">Overall Score</p>
        </div>
      </section>

      <div className="stats-row">
        <div className="stat-block">
          <strong>12k+</strong>
          <p className="muted">Candidates</p>
        </div>
        <div className="stat-block">
          <strong>400+</strong>
          <p className="muted">Companies</p>
        </div>
        <div className="stat-block">
          <strong>98%</strong>
          <p className="muted">Match accuracy</p>
        </div>
      </div>

      <section className="section">
        <h2>Everything you need to hire &amp; get hired</h2>
        <div className="feature-grid">
          {features.map((f) => (
            <div key={f.title} className="card" style={{ display: 'flex', gap: 14, flexDirection: 'column' }}>
              <div className="stat-icon" style={{ width: 40, height: 40, fontSize: 18 }}>
                {f.icon}
              </div>
              <h3 style={{ margin: 0 }}>{f.title}</h3>
              <p className="muted" style={{ margin: 0, fontSize: 14, lineHeight: 1.6 }}>
                {f.desc}
              </p>
            </div>
          ))}
        </div>
      </section>

      <footer className="landing-foot">
        © {new Date().getFullYear()} Harvix AI — The Complete AI Recruitment Ecosystem · Built for local development
      </footer>
    </div>
  )
}

