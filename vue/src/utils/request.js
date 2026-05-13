import axios from 'axios'

/**
 * 默认请求实例：成功时直接返回后端 JSON（即原 axios 的 `response.data`），字段为 `{ code, msg, data }`。
 * 后台管理（/api/admin/**）若需与「`const res = await axios.get(...); res.data.code`」一致，请使用 `@/utils/adminHttp`。
 */
const request = axios.create({
  // 使用代理服务器，baseURL改为'/api'
  baseURL: '/api',
  timeout: 5000 // 请求超时时间
  // 移除全局设置的Content-Type头，让axios根据请求数据自动设置
})

// 请求拦截器：添加token到请求头
request.interceptors.request.use(
  (config) => {
    // 从localStorage中获取token
    const token = localStorage.getItem('token')
    if (token) {
      // 添加token到请求头
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器：保留完整后端返回结构，不直接截取data
request.interceptors.response.use(
  (response) => {
    // 完整返回{code, message, data}，让前端自主处理
    return response.data
  },
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
      const body = error.response.data
      return Promise.reject(
        body && typeof body === 'object' ? body : { code: 401, msg: '未授权或登录已过期', data: null }
      )
    }
    console.error('请求失败：', error.message)
    return Promise.reject({
      code: 500,
      message: '接口请求失败',
      data: []
    })
  }
)

export default request
