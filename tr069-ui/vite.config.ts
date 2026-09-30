import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  base: './',
  build: {
    outDir: '../src/main/resources/static/tr069-admin',
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    proxy: {
      '/api/v2/tr069': {
        target: 'http://2.24.202.99:8088',
        changeOrigin: true,
      },
    },
  },
})
