import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

export function RequireAuth({ children, adminOnly = false }) {
  const { user, isAdmin } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (adminOnly && !isAdmin) return <Navigate to="/order" replace />
  return children
}
