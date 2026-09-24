import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // Same-origin in dev: the browser talks to /api, Vite forwards to the gateway.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        rewrite: (path) => path.replace(/^\/api/, ''),
        // The refresh cookie is scoped to Path=/auth by the backend; in dev the
        // browser sees it under /api/auth, so rewrite the path accordingly.
        cookiePathRewrite: { '/auth': '/api/auth' },
      },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
})
