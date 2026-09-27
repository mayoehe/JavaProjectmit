import { Link } from 'react-router-dom'

export default function Landing() {
  return (
    <div className="page">
      <div className="hero">
        <h1>Skip the queue.<br />Pre-order <em>fresh canteen food.</em></h1>
        <p>
          Browse today&apos;s menu, place your pre-order in seconds, and pay when you
          pick up. Missed a pickup? It simply goes on your tab — no stress.
        </p>
        <div className="cta-row">
          <Link className="btn" to="/register">Student Login / Register</Link>
          <Link className="btn secondary" to="/login?admin=1">Admin Login</Link>
        </div>
        <p className="muted" style={{ marginTop: 16 }}>
          Demo admin: <b>admin / admin123</b> · Students register with their college ID.
        </p>
      </div>

      <div className="grid two" style={{ marginTop: 20 }}>
        <div className="card">
          <h3>🎓 For Students</h3>
          <p className="muted">Today&apos;s menu · cart &amp; pre-order · live order status · tab balance · order history.</p>
          <Link className="btn secondary" to="/order">Start ordering</Link>
        </div>
        <div className="card">
          <h3>🍳 For Canteen Staff</h3>
          <p className="muted">Menu CRUD · Ready / Collected / Uncollected flow · student tabs · revenue overview.</p>
          <Link className="btn secondary" to="/admin">Open dashboard</Link>
        </div>
      </div>

      <div className="grid two" style={{ marginTop: 20 }}>
        <div className="card"><h4>💳 Pay on pickup</h4><p className="muted">No online payment needed. Pay cash/UPI at the counter when you collect.</p></div>
        <div className="card"><h4>📒 Automatic tab</h4><p className="muted">Uncollected orders are added to your tab so nothing is lost or disputed.</p></div>
      </div>
    </div>
  )
}
