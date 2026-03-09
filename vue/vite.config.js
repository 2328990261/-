import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    port: 5173, // 强制使用5173端口
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        // 仅对 /novel、/auth 去掉 /api 前缀；/api/user/behavior、/api/recommend、/api/admin 保留 /api
        rewrite: (path) => {
          if (path.startsWith('/api/user/behavior') || path.startsWith('/api/recommend') || path.startsWith('/api/admin')) {
            return path
          }
          return path.replace(/^\/api/, '')
        }
      },
      '/cover': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
