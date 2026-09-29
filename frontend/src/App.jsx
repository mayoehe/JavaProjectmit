import { useEffect, useState } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext.jsx'
import { ThemeProvider } from './context/ThemeContext.jsx'
import ClickSpark from './components/ClickSpark.jsx'
import Splash from './components/Splash.jsx'
import Navbar from './components/Navbar.jsx'
import { RequireAuth } from './components/Protected.jsx'
import Landing from './pages/Landing.jsx'
import { Login, Register } from './pages/Auth.jsx'
import Ordering from './pages/Ordering.jsx'
import Profile from './pages/Profile.jsx'
import Admin from './pages/Admin.jsx'

export default function App() {
  // Intro splash: shown once per page load only (App never remounts on route changes).
  const [splash, setSplash] = useState(true)
  useEffect(() => {
    const t = setTimeout(() => setSplash(false), 3200)
    return () => clearTimeout(t)
  }, [])

  return (
    <ThemeProvider>
    <AuthProvider>
      <BrowserRouter>
        <ClickSpark
          sparkColor="#38bdf8"
          sparkSize={10}
          sparkRadius={15}
          sparkCount={8}
          duration={400}
        >
          <Navbar />
          <Routes>
            <Route path="/" element={<Landing />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/order" element={<RequireAuth><Ordering /></RequireAuth>} />
            <Route path="/profile" element={<RequireAuth><Profile /></RequireAuth>} />
            <Route path="/admin" element={<RequireAuth adminOnly><Admin /></RequireAuth>} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </ClickSpark>
      </BrowserRouter>
    </AuthProvider>
    {splash && <Splash />}
    </ThemeProvider>
  )
}
