import axios from 'axios'

// 创建Axios实例，统一配置
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
    console.error('请求失败：', error.message)
    // 异常时返回统一格式，避免前端崩溃
    return Promise.reject({
      code: 500,
      message: '接口请求失败',
      data: []
    })
  }
)

export default request
