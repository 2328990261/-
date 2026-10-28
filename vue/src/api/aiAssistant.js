import request from '@/utils/request'
import { backendUrl } from '@/config/env'

/** 获取当前用户的会话列表 */
export function listSessions() {
  return request({ url: '/ai-assistant/sessions', method: 'get' })
}

/** 获取指定会话的消息列表 */
export function listMessages(sessionId) {
  return request({ url: `/ai-assistant/sessions/${sessionId}/messages`, method: 'get' })
}

/** 重命名会话标题 */
export function updateSessionTitle(sessionId, title) {
  return request({ url: `/ai-assistant/sessions/${sessionId}/title`, method: 'put', data: { title } })
}

/** 设置会话置顶状态（true=置顶，false=取消置顶） */
export function updateSessionPin(sessionId, pinned) {
  return request({ url: `/ai-assistant/sessions/${sessionId}/pin`, method: 'put', data: { pinned } })
}

/** 删除会话（后端会级联删除该会话下的消息） */
export function deleteSession(sessionId) {
  return request({ url: `/ai-assistant/sessions/${sessionId}`, method: 'delete' })
}

/** 上传聊天图片（用于向 AI 发送图片内容） */
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/ai-assistant/upload-image',
    method: 'post',
    data: formData,
    timeout: 30000
  })
}

/**
 * 通过 POST + fetch 读取 SSE 流。
 * onEvent 接收后端按帧推送的对象：{type:'session'|'reply'|'books'|'error'|'done', ...}
 */
export async function streamChat({ sessionId, content, imageUrl }, onEvent) {
  const token = localStorage.getItem('token') || ''
  const controller = new AbortController()
  const response = await fetch(backendUrl('/api/ai-assistant/chat'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify({ sessionId, content, imageUrl: imageUrl || null }),
    signal: controller.signal
  })

  if (!response.ok) {
    if (response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      window.location.assign('/login')
    }
    const text = await response.text().catch(() => '')
    throw new Error(text || `请求失败(${response.status})`)
  }
  if (!response.body) {
    throw new Error('当前环境不支持流式响应')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const handleDataLine = (dataLine) => {
    const jsonText = dataLine.slice(dataLine.indexOf(':') + 1).trim()
    if (!jsonText) return
    try {
      onEvent(JSON.parse(jsonText))
    } catch (e) {
      // 忽略无法解析的帧
    }
  }

  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    let sepIndex
    while ((sepIndex = buffer.indexOf('\n\n')) !== -1) {
      const rawEvent = buffer.slice(0, sepIndex)
      buffer = buffer.slice(sepIndex + 2)
      const dataLine = rawEvent.split('\n').find((line) => line.startsWith('data:'))
      if (dataLine) handleDataLine(dataLine)
    }
  }

  if (buffer.trim()) {
    const dataLine = buffer.split('\n').find((line) => line.startsWith('data:'))
    if (dataLine) handleDataLine(dataLine)
  }

  return { abort: () => controller.abort() }
}
