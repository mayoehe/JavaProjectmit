import { useEffect, useState } from 'react'
import { api, errMsg } from '../api/client.js'
import { useAuth } from '../context/AuthContext.jsx'

export default function Profile() {
  const { user, setUser } = useAuth()
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    (async () => {
      try {
        const [me, o] = await Promise.all([api.get('/api/users/me'), api.get('/api/orders/my')])
        setOrders(o.data)
        // refresh tab balance in navbar
        const stored = JSON.parse(localStorage.getItem('canteen_user') || '{}')
        const updated = { ...stored, tabBalance: me.data.tabBalance }
        localStorage.setItem('canteen_user', JSON.stringify(updated))
        setUser(updated)
      } catch (e) {
        setError(errMsg(e, 'Could not load profile'))
      } finally {
        setLoading(false)
      }
    })()
  }, [setUser])

  const active = orders.filter((o) => ['PENDING', 'READY'].includes(o.status))
  const history = orders.filter((o) => !['PENDING', 'READY'].includes(o.status))

  const OrderRow = ({ o }) => (
    <div style={{ borderTop: '1px solid var(--line)', padding: '10px 0' }}>
      <span className={`badge ${o.status.toLowerCase()}`}>{o.status}</span>
      <span className="muted"> #{o.id} · {new Date(o.createdAt).toLocaleString()} · </span>
      <b className="price">₹{Number(o.total).toFixed(2)}</b>
      <div className="muted">{o.items?.map((i) => `${i.name} × ${i.quantity}`).join(', ')}</div>
      {o.status === 'UNCOLLECTED' && <div className="muted">⚠️ Added to your tab.</div>}
    </div>
  )

  return (
    <div className="page">
      <h2>👤 My Profile</h2>
      {error && <p className="error">{String(error)}</p>}
      {loading ? <p>Loading…</p> : (
        <div className="grid two">
          <div>
            <div className="card">
              <h3>{user?.name}</h3>
              <p className="muted">ID: <b>{user?.studentId}</b></p>
              <p style={{ fontSize: 28, margin: '8px 0' }}>📒 Tab: <b style={{ color: 'var(--accent)' }}>₹{Number(user?.tabBalance ?? 0).toFixed(2)}</b></p>
              <p className="muted">Uncollected orders are added here automatically. Clear it at the counter.</p>
            </div>
            <div className="card" style={{ marginTop: 16 }}>
              <h3>⏳ Active Orders ({active.length})</h3>
              {active.length === 0 ? <p className="muted">Nothing cooking for you right now.</p> : active.map((o) => <OrderRow key={o.id} o={o} />)}
            </div>
          </div>
          <div className="card">
            <h3>📜 Order History ({history.length})</h3>
            {history.length === 0 ? <p className="muted">No past orders yet.</p> : history.map((o) => <OrderRow key={o.id} o={o} />)}
          </div>
        </div>
      )}
    </div>
  )
}
