import { createContext, useContext, useEffect, useState } from 'react'

const ThemeContext = createContext({ light: false, toggle: () => {} })

// Light (default on first visit) = white surfaces + black text.
// Dark = black surfaces + white text.
// Applied via `data-theme` on <html>; all colors flow from CSS variables. Persisted.
export function ThemeProvider({ children }) {
  const [light, setLight] = useState(() => {
    try {
      return localStorage.getItem('canteen-theme') !== 'dark'
    } catch {
      return true
    }
  })

  useEffect(() => {
    document.documentElement.dataset.theme = light ? 'light' : 'dark'
    try {
      localStorage.setItem('canteen-theme', light ? 'light' : 'dark')
    } catch {
      /* storage unavailable — theme still applies for this session */
    }
  }, [light])

  return (
    <ThemeContext.Provider value={{ light, toggle: () => setLight((v) => !v) }}>
      {children}
    </ThemeContext.Provider>
  )
}

export const useTheme = () => useContext(ThemeContext)
