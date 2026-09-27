import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

export default function Navbar() {
  const { user, logout, isAdmin } = useAuth()
  const nav = useNavigate()
  return (
    <nav className="nav">
      <div className="nav-inner">
        <Link className="brand" to="/">🍛 Campus<span>Canteen</span></Link>
        <div className="nav-links">
          {user && !isAdmin && (
            <>
              <Link to="/order">Order</Link>
              <Link to="/profile">Profile</Link>
              <span className="tab-pill">Tab: ₹{Number(user.tabBalance ?? 0).toFixed(2)}</span>
            </>
          )}
          {user && isAdmin && <Link to="/admin">Dashboard</Link>}
          {user ? (
            <button className="btn small secondary" onClick={() => { logout(); nav('/') }}>
              Logout ({user.studentId})
            </button>
          ) : (
            <>
              <Link to="/login">Login</Link>
              <Link className="btn small" to="/register">Register</Link>
            </>
          )}
        </div>
      </div>
    </nav>
  )
}
