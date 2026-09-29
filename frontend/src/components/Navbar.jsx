import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { useTheme } from '../context/ThemeContext.jsx'
import SquishSwitch from './SquishSwitch.jsx'

export default function Navbar() {
  const { user, logout, isAdmin } = useAuth()
  const { light, toggle } = useTheme()
  const nav = useNavigate()
  return (
    <nav className="nav">
      <div className="nav-inner">
        <Link className="brand" to="/">🍛 Campus<span>Canteen</span></Link>
        <div className="nav-links">
          <span className="theme-toggle" title={light ? 'Switch to dark mode' : 'Switch to light mode'}>
            <span aria-hidden="true">{light ? '☀️' : '🌙'}</span>
            <SquishSwitch
              checked={light}
              onChange={toggle}
              ariaLabel="Toggle light and dark mode"
              width={60}
              height={30}
              radius={15}
              trackColor="#131e36"
              trackOnColor="#dbeafe"
              thumbColor="#38bdf8"
              thumbOnColor="#2563eb"
            />
          </span>
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
