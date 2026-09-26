import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import electron from 'vite-plugin-electron/simple'

// https://vite.dev/config/
export default defineConfig({
  // Electron 通过 file:// 加载，必须用相对路径，否则 JS/CSS 会 404 导致白屏
  base: './',
  plugins: [
    vue(),
    vueDevTools(),
    electron({
      main: {
        entry: 'electron/main.js',
      },
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => {
          if (
            path.startsWith('/api/user/') ||
            path.startsWith('/api/recommend') ||
            path.startsWith('/api/customer-service') ||
            path.startsWith('/api/admin')
          ) {
            return path
          }
          return path.replace(/^\/api/, '')
        },
      },
      // 封面等静态资源：前端用 backendUrl('/novel/cover/...')，需代理到后端
      '/novel': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8081',
        ws: true,
        changeOrigin: true,
      },
      '/cover': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
})
