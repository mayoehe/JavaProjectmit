import { useEffect, useState } from 'react'
import { api, errMsg } from '../api/client.js'
import { useAuth } from '../context/AuthContext.jsx'

export default function Profile() {
  const { user, setUser } = useAuth()
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [cancelling, setCancelling] = useState(null)

  const reload = async () => {
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
    }
  }

  useEffect(() => {
    (async () => {
      setLoading(true)
      await reload()
      setLoading(false)
    })()
  }, [setUser])

  const cancelOrder = async (id) => {
    if (!confirm(`Cancel order #${id}? The canteen hasn't started it yet, so no charge applies.`)) return
    setError(''); setNotice(''); setCancelling(id)
    try {
      await api.patch(`/api/orders/my/${id}/cancel`)
      setNotice(`Order #${id} cancelled. Nothing was charged to your tab. ✅`)
      await reload()
    } catch (e) {
      setError(errMsg(e, 'Could not cancel order'))
    } finally {
      setCancelling(null)
    }
  }

  const active = orders.filter((o) => ['PENDING', 'READY'].includes(o.status))
  const history = orders.filter((o) => !['PENDING', 'READY'].includes(o.status))

  const OrderRow = ({ o }) => (
    <div style={{ borderTop: '1px solid var(--line)', padding: '10px 0' }}>
      <span className={`badge ${o.status.toLowerCase()}`}>{o.status}</span>
      <span className="muted"> #{o.id} · {new Date(o.createdAt).toLocaleString()} · </span>
      <b className="price">₹{Number(o.total).toFixed(2)}</b>
      <div className="muted">{o.items?.map((i) => `${i.name} × ${i.quantity}`).join(', ')}</div>
      {o.status === 'UNCOLLECTED' && <div className="muted">⚠️ Added to your tab.</div>}
      {o.status === 'PENDING' && (
        <div className="row" style={{ marginTop: 6 }}>
          <button className="btn small secondary" disabled={cancelling === o.id} onClick={() => cancelOrder(o.id)}>
            {cancelling === o.id ? 'Cancelling…' : '✖ Cancel Order'}
          </button>
          <span className="muted">Free cancellation while it&apos;s still Pending.</span>
        </div>
      )}
      {o.status === 'READY' && (
        <div className="muted" style={{ marginTop: 6 }}>🔔 Being prepared — cancellation closed, please collect at the counter.</div>
      )}
    </div>
  )

  return (
    <div className="page">
      <h2>👤 My Profile</h2>
      {error && <p className="error">{String(error)}</p>}
      {notice && <p className="ok">{notice}</p>}
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
