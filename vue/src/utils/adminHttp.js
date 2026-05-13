import axios from 'axios'

/**
 * 管理端专用 HTTP 客户端：与 {@link ./request.js} 相同地附加 JWT、走同源 `/api`（Vite/Nginx 代理），
 * 但响应保持 Axios 原生形态（res.data 为后端 Result），便于与历史「axios 直连」写法一致，避免与全局 request 的解包语义混用导致界面数据错位。
 */
const adminHttp = axios.create({
  baseURL: '/api',
  timeout: 60000
})

adminHttp.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

adminHttp.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
      const path = window.location.pathname || ''
      if (!path.includes('/login')) {
        window.location.assign('/login')
      }
      const body = error.response?.data
      return Promise.reject(
        body && typeof body === 'object' ? body : { code: 401, msg: '未授权或登录已过期', data: null }
      )
    }
    console.error('管理端请求失败：', error.message)
    return Promise.reject(
      error.response?.data && typeof error.response.data === 'object'
        ? error.response.data
        : { code: 500, msg: '接口请求失败', data: null }
    )
  }
)

export default adminHttp
