import { useEffect, useMemo, useState } from 'react'
import { api, errMsg } from '../api/client.js'
import HoldButton from '../components/HoldButton.jsx'

const EMOJI = ['🍛', '🍜', '🥪', '🍔', '☕', '🧃', '🍩', '🥗']

export default function Ordering() {
  const [menu, setMenu] = useState([])
  const [cart, setCart] = useState({}) // menuId -> qty
  const [myOrders, setMyOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [placing, setPlacing] = useState(false)
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')
  const [filter, setFilter] = useState('All')
  const [cancelling, setCancelling] = useState(null)

  const load = async () => {
    setLoading(true)
    setError('')
    try {
      const [m, o] = await Promise.all([api.get('/api/menu'), api.get('/api/orders/my')])
      setMenu(m.data)
      setMyOrders(o.data)
    } catch (e) {
      setError(errMsg(e, 'Could not load menu'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const categories = useMemo(() => ['All', ...new Set(menu.map((m) => m.category || 'General'))], [menu])
  const shown = menu.filter((m) => filter === 'All' || (m.category || 'General') === filter)

  const cartLines = Object.entries(cart)
    .map(([id, qty]) => ({ item: menu.find((m) => String(m.id) === String(id)), qty }))
    .filter((l) => l.item && l.qty > 0)
  const total = cartLines.reduce((s, l) => s + l.item.price * l.qty, 0)

  const add = (id) => { setOk(''); setCart({ ...cart, [id]: (cart[id] || 0) + 1 }) }
  const sub = (id) => {
    const q = (cart[id] || 0) - 1
    const c = { ...cart }
    if (q <= 0) delete c[id]
    else c[id] = q
    setCart(c)
  }

  const place = async () => {
    setError(''); setOk('')
    if (!cartLines.length) return
    setPlacing(true)
    try {
      await api.post('/api/orders', {
        items: cartLines.map((l) => ({ menuItemId: l.item.id, quantity: l.qty })),
      })
      setCart({})
      setOk('Order placed! Pay at the counter when you collect. 🎉')
      const { data } = await api.get('/api/orders/my')
      setMyOrders(data)
    } catch (e) {
      setError(errMsg(e, 'Could not place order'))
    } finally {
      setPlacing(false)
    }
  }

  const cancelOrder = async (id) => {
    if (!confirm(`Cancel order #${id}? The canteen hasn't started it yet, so no charge applies.`)) return
    setError(''); setOk(''); setCancelling(id)
    try {
      await api.patch(`/api/orders/my/${id}/cancel`)
      setOk(`Order #${id} cancelled. Nothing was charged. ✅`)
      const { data } = await api.get('/api/orders/my')
      setMyOrders(data)
    } catch (e) {
      setError(errMsg(e, 'Could not cancel order'))
    } finally {
      setCancelling(null)
    }
  }

  const active = myOrders.filter((o) => ['PENDING', 'READY'].includes(o.status))

  return (
    <div className="page">
      <h2>🍽️ Today&apos;s Menu</h2>
      <p className="muted">Pre-order now, pay in person when collecting.</p>
      {error && <p className="error">{String(error)}</p>}
      {ok && <p className="ok">{ok}</p>}

      <div className="row" style={{ marginBottom: 14 }}>
        {categories.map((c) => (
          <button key={c} className={filter === c ? 'btn small' : 'btn small secondary'} onClick={() => setFilter(c)}>{c}</button>
        ))}
        <button className="btn small ghost" onClick={load}>↻ Refresh</button>
      </div>

      <div className="grid two">
        <div>
          {loading ? <p>Loading delicious things…</p> : shown.length === 0 ? (
            <div className="card"><p>No items right now — check back soon!</p></div>
          ) : (
            <div className="grid menu">
              {shown.map((m, i) => (
                <div className="card" key={m.id}>
                  <div className="dish-img">{m.imageUrl ? <img src={m.imageUrl} alt={m.name} /> : EMOJI[i % EMOJI.length]}</div>
                  <span className="badge cat">{m.category}</span>
                  <h3 style={{ margin: '8px 0 4px' }}>{m.name}</h3>
                  <p className="muted" style={{ minHeight: 40 }}>{m.description}</p>
                  <div className="row" style={{ justifyContent: 'space-between' }}>
                    <span className="price">₹{Number(m.price).toFixed(2)}</span>
                    <div className="row">
                      <button className="btn small secondary" onClick={() => sub(m.id)}>−</button>
                      <b>{cart[m.id] || 0}</b>
                      <button className="btn small" onClick={() => add(m.id)}>+ Add</button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div>
          <div className="card cart">
            <h3>🧺 Your Cart</h3>
            {cartLines.length === 0 ? <p className="muted">Cart is empty — tap + Add on anything tasty.</p> : (
              <>
                {cartLines.map((l) => (
                  <div className="row" key={l.item.id} style={{ justifyContent: 'space-between', marginBottom: 8 }}>
                    <span>{l.item.name} × {l.qty}</span>
                    <span className="price">₹{(l.item.price * l.qty).toFixed(2)}</span>
                  </div>
                ))}
                <hr />
                <div className="row" style={{ justifyContent: 'space-between' }}>
                  <b>Total (pay on pickup)</b><b className="price">₹{total.toFixed(2)}</b>
                </div>
                <div className="cart-actions">
                  <button className="btn" disabled={placing} onClick={place}>
                    {placing ? 'Placing…' : 'Place Pre-order'}
                  </button>
                  <HoldButton
                    className="cart-hold"
                    doneLabel="Cleared"
                    backgroundColor="#131e36"
                    fillColor="#2563eb"
                    textColor="#ffffff"
                    fillTextColor="#ffffff"
                    size="md"
                    radius={12}
                    fillDirection="right"
                    holdTime={2000}
                    onHold={() => { setCart({}); setOk('') }}
                  >
                    Hold to delete
                  </HoldButton>
                </div>
              </>
            )}
          </div>

          <div className="card" style={{ marginTop: 16 }}>
            <h3>⏳ Active Orders ({active.length})</h3>
            {active.length === 0 ? <p className="muted">No active orders.</p> : active.map((o) => (
              <div key={o.id} style={{ borderTop: '1px solid var(--line)', paddingTop: 8, marginTop: 8 }}>
                <span className={`badge ${o.status.toLowerCase()}`}>{o.status}</span>
                <span className="muted"> #{o.id} · ₹{Number(o.total).toFixed(2)}</span>
                <div className="muted">{o.items?.map((i) => `${i.name} × ${i.quantity}`).join(', ')}</div>
                {o.status === 'PENDING' ? (
                  <div className="row" style={{ marginTop: 6 }}>
                    <button className="btn small secondary" disabled={cancelling === o.id} onClick={() => cancelOrder(o.id)}>
                      {cancelling === o.id ? 'Cancelling…' : '✖ Cancel Order'}
                    </button>
                  </div>
                ) : (
                  <div className="muted" style={{ marginTop: 6 }}>🔔 Being prepared — cancellation closed.</div>
                )}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
