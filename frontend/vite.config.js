import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Dev server proxies /api -> Spring Boot so no CORS issues locally.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_API_URL || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  preview: { port: 5173 },
})
