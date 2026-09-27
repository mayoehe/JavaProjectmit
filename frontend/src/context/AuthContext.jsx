import { createContext, useContext, useMemo, useState } from 'react'
import { api } from '../api/client.js'

const AuthCtx = createContext(null)

const loadUser = () => {
  try {
    return JSON.parse(localStorage.getItem('canteen_user') || 'null')
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(loadUser)
  const token = localStorage.getItem('canteen_token')

  const save = (authResp) => {
    localStorage.setItem('canteen_token', authResp.token)
    const u = {
      studentId: authResp.studentId,
      name: authResp.name,
      role: authResp.role,
      tabBalance: authResp.tabBalance,
    }
    localStorage.setItem('canteen_user', JSON.stringify(u))
    setUser(u)
  }

  const login = async (studentId, password) => {
    const { data } = await api.post('/api/auth/login', { studentId, password })
    save(data)
    return data
  }

  const register = async (payload) => {
    const { data } = await api.post('/api/auth/register', payload)
    save(data)
    return data
  }

  const logout = () => {
    localStorage.removeItem('canteen_token')
    localStorage.removeItem('canteen_user')
    setUser(null)
  }

  const value = useMemo(
    () => ({ user, token, isAdmin: user?.role === 'ADMIN', login, register, logout, setUser }),
    [user, token]
  )
  return <AuthCtx.Provider value={value}>{children}</AuthCtx.Provider>
}

export const useAuth = () => useContext(AuthCtx)
