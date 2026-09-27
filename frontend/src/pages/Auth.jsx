import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { errMsg } from '../api/client.js'

export function Login() {
  const [params] = useSearchParams()
  const isAdminHint = params.get('admin') === '1'
  const [studentId, setStudentId] = useState(isAdminHint ? 'admin' : '')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const nav = useNavigate()

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const data = await login(studentId.trim(), password)
      nav(data.role === 'ADMIN' ? '/admin' : '/order')
    } catch (err) {
      setError(errMsg(err, 'Login failed — check your ID and password'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="page">
      <div className="card" style={{ maxWidth: 480, margin: '24px auto' }}>
        <h2>{isAdminHint ? '🔑 Admin Login' : '👋 Student Login'}</h2>
        <p className="muted">Use your college student ID (roll / enrollment number).</p>
        {error && <p className="error">{String(error)}</p>}
        <form className="form" onSubmit={submit}>
          <label className="label">Student ID / Admin ID</label>
          <input className="input" value={studentId} onChange={(e) => setStudentId(e.target.value)} required placeholder={isAdminHint ? 'admin' : 'e.g. 2024CS101'} />
          <label className="label">Password</label>
          <input className="input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          <div style={{ marginTop: 14 }}>
            <button className="btn" disabled={loading}>{loading ? 'Logging in…' : 'Login'}</button>
          </div>
        </form>
        <p className="muted">New student? <Link to="/register">Register here</Link></p>
        {isAdminHint && <p className="muted">Demo credentials: <b>admin / admin123</b></p>}
      </div>
    </div>
  )
}

export function Register() {
  const [form, setForm] = useState({ studentId: '', name: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { register } = useAuth()
  const nav = useNavigate()
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await register(form)
      nav('/order')
    } catch (err) {
      setError(errMsg(err, 'Registration failed — ID may already exist'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="page">
      <div className="card" style={{ maxWidth: 480, margin: '24px auto' }}>
        <h2>🎓 Student Registration</h2>
        <p className="muted">Your student ID is your unique login.</p>
        {error && <p className="error">{String(error)}</p>}
        <form className="form" onSubmit={submit}>
          <label className="label">Student ID *</label>
          <input className="input" value={form.studentId} onChange={set('studentId')} required placeholder="e.g. 2024CS101" />
          <label className="label">Full name *</label>
          <input className="input" value={form.name} onChange={set('name')} required placeholder="Your name" />
          <label className="label">Email</label>
          <input className="input" type="email" value={form.email} onChange={set('email')} placeholder="you@college.edu" />
          <label className="label">Password *</label>
          <input className="input" type="password" value={form.password} onChange={set('password')} required minLength={4} />
          <div style={{ marginTop: 14 }}>
            <button className="btn" disabled={loading}>{loading ? 'Creating…' : 'Create account'}</button>
          </div>
        </form>
        <p className="muted">Already registered? <Link to="/login">Login</Link></p>
      </div>
    </div>
  )
}
