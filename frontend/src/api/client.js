import axios from 'axios'

// Same-origin by default (nginx proxies /api in prod).
// Local dev: set VITE_API_URL=http://localhost:8080 (see .env.example).
const baseURL = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '')

export const api = axios.create({ baseURL: baseURL || undefined })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('canteen_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err?.response?.status === 401) {
      localStorage.removeItem('canteen_token')
      localStorage.removeItem('canteen_user')
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(err)
  }
)

/** Always returns a string — never an object (which React renders as [object Object]). */
export const errMsg = (e, fallback = 'Something went wrong') => {
  const status = e?.response?.status
  const data = e?.response?.data
  if (typeof data === 'string' && data.trim()) {
    // Same-origin fallback on Vercel serves index.html for unknown /api routes.
    if (data.trim().startsWith('<')) {
      return `API not reachable (got HTML, HTTP ${status ?? '?'}). Check VITE_API_URL points to the backend.`
    }
    return data.slice(0, 300)
  }
  if (data && typeof data === 'object') {
    if (typeof data.message === 'string' && data.message) return data.message
    if (typeof data.error === 'string' && data.error) {
      const extra = Array.isArray(data.errors) && data.errors.length
        ? `: ${data.errors.map((x) => (typeof x === 'string' ? x : x?.defaultMessage || JSON.stringify(x))).join(', ')}`
        : ''
      return `${data.error}${extra} (HTTP ${status ?? '?'})`
    }
    if (Array.isArray(data.errors) && data.errors.length) {
      const first = data.errors[0]
      if (typeof first === 'string') return first
      if (first?.defaultMessage) return first.defaultMessage
    }
    try {
      const s = JSON.stringify(data)
      if (s && s !== '{}') return s.slice(0, 300)
    } catch { /* fall through to e.message */ }
  }
  if (e?.message) {
    if (e.message === 'Network Error') {
      return 'Cannot reach the backend (Network Error). Check VITE_API_URL and that the backend is running with CORS allowing this site.'
    }
    return e.message
  }
  return fallback
}
