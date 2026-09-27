import { useEffect, useState } from 'react'
import { api, errMsg } from '../api/client.js'

const STATUSES = ['PENDING', 'READY', 'COLLECTED', 'UNCOLLECTED', 'CANCELLED']

export default function Admin() {
  const [tab, setTab] = useState('orders') // orders | menu | tabs
  const [orders, setOrders] = useState([])
  const [menu, setMenu] = useState([])
  const [students, setStudents] = useState([])
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [form, setForm] = useState({ name: '', price: '', description: '', category: 'Snacks', available: true, imageUrl: '' })
  const [editing, setEditing] = useState(null)

  const load = async () => {
    setLoading(true); setError('')
    try {
      const [o, m, s, sum] = await Promise.all([
        api.get('/api/orders'),
        api.get('/api/menu?all=true'),
        api.get('/api/students'),
        api.get('/api/orders/summary'),
      ])
      setOrders(o.data); setMenu(m.data); setStudents(s.data); setSummary(sum.data)
    } catch (e) {
      setError(errMsg(e, 'Could not load dashboard'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const setStatus = async (id, status) => {
    try {
      await api.patch(`/api/orders/${id}/status`, { status })
      load()
    } catch (e) {
      alert(errMsg(e, 'Status update failed'))
    }
  }

  const saveDish = async (e) => {
    e.preventDefault()
    try {
      const payload = { ...form, price: Number(form.price) }
      if (editing) await api.put(`/api/menu/${editing}`, payload)
      else await api.post('/api/menu', payload)
      setForm({ name: '', price: '', description: '', category: 'Snacks', available: true, imageUrl: '' })
      setEditing(null)
      load()
    } catch (err) {
      alert(errMsg(err, 'Could not save dish'))
    }
  }

  const delDish = async (id) => {
    if (!confirm('Delete this dish?')) return
    await api.delete(`/api/menu/${id}`)
    load()
  }

  const toggleAvail = async (m) => {
    await api.patch(`/api/menu/${m.id}/availability`, { available: !m.available })
    load()
  }

  const clearTab = async (sid, full = true) => {
    const amount = full ? undefined : Number(prompt('Partial payment amount (₹):', '50'))
    if (!full && !amount) return
    await api.patch(`/api/students/${sid}/tab`, full ? {} : { amount })
    load()
  }

  return (
    <div className="page">
      <h2>🍳 Canteen Dashboard</h2>
      {error && <p className="error">{String(error)}</p>}

      {summary && (
        <div className="grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', marginBottom: 16 }}>
          {[['Total orders', summary.totalOrders], ['Pending', summary.pending], ['Ready', summary.ready],
            ['Collected', summary.collected], ['Uncollected', summary.uncollected]].map(([k, v]) => (
            <div className="card" key={k}><div className="kpi-label">{k}</div><div className="kpi">{v}</div></div>
          ))}
          <div className="card"><div className="kpi-label">Collected revenue</div><div className="kpi" style={{ color: 'var(--green)' }}>₹{summary.collectedRevenue}</div></div>
          <div className="card"><div className="kpi-label">Tab outstanding</div><div className="kpi" style={{ color: 'var(--accent)' }}>₹{summary.tabOutstanding}</div></div>
        </div>
      )}

      <div className="row" style={{ marginBottom: 16 }}>
        {['orders', 'menu', 'tabs'].map((t) => (
          <button key={t} className={tab === t ? 'btn small' : 'btn small secondary'} onClick={() => setTab(t)}>
            {t === 'orders' ? '📦 Orders' : t === 'menu' ? '📋 Menu' : '📒 Student Tabs'}
          </button>
        ))}
        <button className="btn small ghost" onClick={load}>↻ Refresh</button>
      </div>

      {loading ? <p>Loading dashboard…</p> : (
        <>
          {tab === 'orders' && (
            <div className="card" style={{ overflowX: 'auto' }}>
              <table className="table">
                <thead><tr><th>#</th><th>Student</th><th>Items</th><th>Total</th><th>Status</th><th>Actions</th></tr></thead>
                <tbody>
                  {orders.map((o) => (
                    <tr key={o.id}>
                      <td>#{o.id}</td>
                      <td>{o.student?.studentId}<br /><span className="muted">{o.student?.name}</span></td>
                      <td>{o.items?.map((i) => `${i.name} × ${i.quantity}`).join(', ')}</td>
                      <td className="price">₹{Number(o.total).toFixed(2)}</td>
                      <td><span className={`badge ${o.status.toLowerCase()}`}>{o.status}</span></td>
                      <td>
                        <div className="row">
                          {STATUSES.filter((s) => s !== o.status).map((s) => (
                            <button key={s} className="btn small secondary" onClick={() => setStatus(o.id, s)}>{s}</button>
                          ))}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {orders.length === 0 && <p className="muted">No orders yet.</p>}
            </div>
          )}

          {tab === 'menu' && (
            <div className="grid two">
              <div className="card">
                <h3>{editing ? '✏️ Edit dish' : '➕ Add dish'}</h3>
                <form className="form" onSubmit={saveDish}>
                  <label className="label">Name *</label>
                  <input className="input" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
                  <label className="label">Price (₹) *</label>
                  <input className="input" type="number" min="1" step="0.5" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} required />
                  <label className="label">Category</label>
                  <select className="select" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
                    {['Breakfast', 'Lunch', 'Snacks', 'Beverages', 'General'].map((c) => <option key={c}>{c}</option>)}
                  </select>
                  <label className="label">Description</label>
                  <textarea className="textarea" rows={2} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
                  <label className="label">Image URL (optional)</label>
                  <input className="input" value={form.imageUrl} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} placeholder="https://…" />
                  <label className="row" style={{ marginTop: 8 }}>
                    <input type="checkbox" checked={form.available} onChange={(e) => setForm({ ...form, available: e.target.checked })} /> Available today
                  </label>
                  <div className="row" style={{ marginTop: 10 }}>
                    <button className="btn">{editing ? 'Save' : 'Add dish'}</button>
                    {editing && <button type="button" className="btn secondary" onClick={() => { setEditing(null); setForm({ name: '', price: '', description: '', category: 'Snacks', available: true, imageUrl: '' }) }}>Cancel</button>}
                  </div>
                </form>
              </div>
              <div className="card">
                <h3>Today&apos;s menu ({menu.length})</h3>
                {menu.map((m) => (
                  <div key={m.id} style={{ borderTop: '1px solid var(--line)', padding: '8px 0' }}>
                    <b>{m.name}</b> <span className="price">₹{Number(m.price).toFixed(2)}</span>{' '}
                    <span className={`badge ${m.available ? 'collected' : 'cancelled'}`}>{m.available ? 'Available' : 'Hidden'}</span>
                    <div className="row" style={{ marginTop: 6 }}>
                      <button className="btn small secondary" onClick={() => { setEditing(m.id); setForm({ name: m.name, price: m.price, description: m.description || '', category: m.category || 'General', available: m.available, imageUrl: m.imageUrl || '' }) }}>Edit</button>
                      <button className="btn small secondary" onClick={() => toggleAvail(m)}>{m.available ? 'Hide' : 'Show'}</button>
                      <button className="btn small ghost" onClick={() => delDish(m.id)}>Delete</button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {tab === 'tabs' && (
            <div className="card" style={{ overflowX: 'auto' }}>
              <table className="table">
                <thead><tr><th>Student ID</th><th>Name</th><th>Tab balance</th><th>Actions</th></tr></thead>
                <tbody>
                  {students.map((s) => (
                    <tr key={s.studentId}>
                      <td><b>{s.studentId}</b></td>
                      <td>{s.name}</td>
                      <td className="price">₹{Number(s.tabBalance).toFixed(2)}</td>
                      <td>
                        <div className="row">
                          <button className="btn small secondary" disabled={!s.tabBalance} onClick={() => clearTab(s.studentId, true)}>Mark paid</button>
                          <button className="btn small ghost" disabled={!s.tabBalance} onClick={() => clearTab(s.studentId, false)}>Partial…</button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {students.length === 0 && <p className="muted">No students yet.</p>}
            </div>
          )}
        </>
      )}
    </div>
  )
}
