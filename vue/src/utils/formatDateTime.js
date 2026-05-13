/**
 * 将接口常见的 ISO 时间（如 2026-01-26T16:01:27）格式化为「空格分隔」的本地可读串，去掉中间的 T。
 */
export function formatDateTime(value) {
  if (value == null || value === '') return '-'
  let s = String(value)
  if (!s.includes('T')) return s
  s = s.replace('T', ' ')
  s = s.replace(/\.\d{3}Z?$/, '').replace(/Z$/, '')
  return s
}
