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
        // 多数接口去掉 /api 前缀（与后端 /novel、/auth 等一致）；
        // 以下路径后端本身带 /api 前缀，必须原样转发到 8081。
        rewrite: (path) => {
          if (
            path.startsWith('/api/user/') ||
            path.startsWith('/api/recommend') ||
            path.startsWith('/api/admin')
          ) {
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
