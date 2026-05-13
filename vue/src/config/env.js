/**
 * 后端 HTTP 根地址（不含末尾 /）
 * - 开发：.env.development 中设为 http://localhost:8081
 * - 生产：.env.production 中留空，走当前域名同源，由 Nginx 反代到 Java
 */
export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

/**
 * 拼接访问后端的完整 URL。path 须以 / 开头，可带 query，如 /api/admin、/novel/cover/xx
 */
export function backendUrl(path) {
  const p = path.startsWith('/') ? path : `/${path}`
  if (!API_BASE_URL) return p
  return `${API_BASE_URL}${p}`
}
