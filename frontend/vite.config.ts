import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server:{
    port: 5173,
    proxy:{
      '/ws': {
        target: 'ws://localhost:8888',
        ws: true,
        changeOrigin: true
      }
    }
  }
})
