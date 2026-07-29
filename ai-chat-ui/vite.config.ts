import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  // We add this server block to act as a reverse proxy
  server: {
    proxy: {
      // Any request starting with /api will be forwarded to your Spring Boot backend
      '/api': {
        target: 'http://localhost:8080', // <-- CHANGE THIS IF YOUR SPRING BOOT PORT IS DIFFERENT
        changeOrigin: true,
        secure: false,
      }
    }
  }
});