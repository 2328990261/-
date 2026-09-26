<template>
  <div class="customer-service-page">
    <Navbar />

    <main class="service-main">
      <section class="chat-card">
        <header class="chat-header">
          <div class="service-avatar">客</div>
          <div class="service-title">
            <h1>联系客服</h1>
            <p :class="['service-status', connectionState]">
              <span class="status-dot"></span>
              {{ connectionText }}
            </p>
          </div>
          <button
            type="button"
            class="reconnect-btn"
            :disabled="connectionState === 'connecting'"
            @click="connect"
          >
            {{ connectionState === 'connected' ? '重新连接' : '连接客服' }}
          </button>
        </header>

        <div ref="messageList" class="message-list">
          <div
            v-for="message in messages"
            :key="message.id"
            :class="['message-row', message.role]"
          >
            <p v-if="message.role === 'system'" class="system-message">{{ message.content }}</p>
            <template v-else>
              <div class="message-avatar">{{ message.role === 'user' ? '我' : '客' }}</div>
              <div class="message-body">
                <p class="message-content">{{ message.content }}</p>
                <span class="message-time">{{ formatTime(message.sentAt) }}</span>
              </div>
            </template>
          </div>
        </div>

        <form class="chat-input-area" @submit.prevent="sendMessage">
          <textarea
            v-model="draft"
            rows="3"
            maxlength="500"
            placeholder="请输入您的问题，Enter 发送，Shift + Enter 换行"
            :disabled="connectionState !== 'connected'"
            @keydown.enter.exact.prevent="sendMessage"
          ></textarea>
          <div class="input-footer">
            <span>{{ draft.length }}/500</span>
            <button type="submit" :disabled="!canSend">发送</button>
          </div>
        </form>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import Navbar from '@/components/Navbar.vue'
import { API_BASE_URL } from '@/config/env'
import request from '@/utils/request'

const messages = ref([])
const draft = ref('')
const connectionState = ref('disconnected')
const messageList = ref(null)

let socket = null
let reconnectTimer = null
let reconnectAttempts = 0
let manuallyClosed = false

const canSend = computed(() => connectionState.value === 'connected' && draft.value.trim().length > 0)

const connectionText = computed(() => {
  if (connectionState.value === 'connected') return '连接成功'
  if (connectionState.value === 'connecting') return '正在连接...'
  return '连接已断开'
})

function buildWebSocketUrl() {
  const token = localStorage.getItem('token') || ''
  const path = `/ws/customer-service?token=${encodeURIComponent(token)}`
  if (API_BASE_URL) {
    return `${API_BASE_URL.replace(/^http/i, 'ws')}${path}`
  }
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${protocol}://${window.location.host}${path}`
}

function connect() {
  if (!localStorage.getItem('token')) {
    addSystemMessage('请先登录后再联系客服。')
    return
  }
  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
    return
  }

  clearTimeout(reconnectTimer)
  manuallyClosed = false
  connectionState.value = 'connecting'
  socket = new WebSocket(buildWebSocketUrl())

  socket.onopen = () => {
    reconnectAttempts = 0
    connectionState.value = 'connected'
  }

  socket.onmessage = (event) => {
    try {
      const payload = JSON.parse(event.data)
      if (payload.type === 'SYSTEM') {
        appendMessage('system', payload)
      } else if (payload.type === 'USER') {
        appendMessage('user', payload)
      } else if (payload.type === 'SERVICE') {
        appendMessage('service', payload)
      } else if (payload.type === 'ERROR') {
        addSystemMessage(payload.content)
      }
    } catch (error) {
      addSystemMessage('收到无法解析的客服消息。')
    }
  }

  socket.onclose = () => {
    connectionState.value = 'disconnected'
    if (!manuallyClosed) {
      scheduleReconnect()
    }
  }

  socket.onerror = () => {
    connectionState.value = 'disconnected'
  }
}

function scheduleReconnect() {
  if (reconnectAttempts >= 5) {
    addSystemMessage('多次连接失败，请确认后端服务已启动，然后点击“重新连接”。')
    return
  }
  reconnectAttempts += 1
  const delay = Math.min(1000 * reconnectAttempts, 5000)
  clearTimeout(reconnectTimer)
  reconnectTimer = setTimeout(connect, delay)
}

function sendMessage() {
  if (!canSend.value) return
  const content = draft.value.trim()
  socket.send(JSON.stringify({ type: 'CHAT', content }))
  draft.value = ''
}

function appendMessage(role, payload) {
  messages.value.push({
    id: payload.id ?? `${role}-${payload.sentAt ?? Date.now()}`,
    role,
    content: payload.content,
    sentAt: payload.sentAt || Date.now()
  })
  nextTick(scrollToBottom)
}

function addSystemMessage(content) {
  appendMessage('system', { content, sentAt: Date.now() })
}

function scrollToBottom() {
  if (messageList.value) {
    messageList.value.scrollTop = messageList.value.scrollHeight
  }
}

function formatTime(timestamp) {
  return new Date(timestamp).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

async function loadHistory() {
  if (!localStorage.getItem('token')) {
    addSystemMessage('请先登录后再联系客服。')
    return
  }

  try {
    const response = await request({
      url: '/customer-service/messages',
      method: 'get'
    })
    if (response.code === 200) {
      messages.value = (response.data || []).map((message) => ({
        id: message.id,
        role: message.sender === 'USER' ? 'user' : 'service',
        content: message.content,
        sentAt: new Date(message.createdAt).getTime()
      }))
      nextTick(scrollToBottom)
    }
  } catch (error) {
    addSystemMessage('聊天记录加载失败，请稍后重试。')
  }
}

function closeSocket() {
  manuallyClosed = true
  clearTimeout(reconnectTimer)
  if (socket) {
    socket.close()
    socket = null
  }
}

onMounted(() => {
  loadHistory().then(() => connect())
})

onBeforeUnmount(closeSocket)
</script>

<style scoped>
.customer-service-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #fff7fb 0%, #f7f9fc 100%);
}

.service-main {
  width: min(920px, calc(100% - 32px));
  margin: 0 auto;
  padding: 88px 0 32px;
}

.chat-card {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
  min-height: 560px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #ffe1ee;
  border-radius: 20px;
  box-shadow: 0 16px 40px rgba(255, 20, 147, 0.08);
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 22px;
  border-bottom: 1px solid #f0e5ec;
  background: linear-gradient(135deg, #fff 0%, #ffeaf4 100%);
}

.service-avatar {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  flex-shrink: 0;
  border-radius: 50%;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #ff6fa5, #ff1493);
}

.service-title {
  flex: 1;
  min-width: 0;
}

.service-title h1 {
  margin: 0;
  color: #333;
  font-size: 20px;
}

.service-status {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 5px 0 0;
  font-size: 13px;
  color: #777;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #a0a0a0;
}

.service-status.connected {
  color: #18a058;
}

.service-status.connected .status-dot {
  background: #18a058;
  box-shadow: 0 0 0 4px rgba(24, 160, 88, 0.12);
}

.service-status.connecting .status-dot {
  background: #f0a020;
}

.reconnect-btn {
  border: 1px solid #ff1493;
  border-radius: 999px;
  padding: 8px 15px;
  color: #ff1493;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s ease;
}

.reconnect-btn:hover:not(:disabled) {
  color: #fff;
  background: #ff1493;
}

.reconnect-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 22px;
  background: #fbfcfe;
}

.message-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  margin-bottom: 16px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.message-row.system {
  justify-content: center;
}

.system-message {
  margin: 0;
  max-width: 82%;
  padding: 6px 12px;
  border-radius: 999px;
  background: #eef2f7;
  color: #667085;
  font-size: 12px;
  text-align: center;
}

.message-avatar {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 50%;
  color: #fff;
  font-size: 13px;
  background: #8a97a8;
}

.message-row.user .message-avatar {
  background: #ff6fa5;
}

.message-body {
  max-width: 68%;
}

.message-content {
  margin: 0;
  padding: 11px 14px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #e8edf3;
  color: #333;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-row.user .message-content {
  color: #fff;
  border-color: transparent;
  background: linear-gradient(135deg, #ff6fa5, #ff1493);
}

.message-row.user .message-body {
  text-align: right;
}

.message-time {
  display: block;
  margin-top: 5px;
  color: #98a2b3;
  font-size: 11px;
}

.chat-input-area {
  padding: 14px 18px 16px;
  border-top: 1px solid #f0e5ec;
  background: #fff;
}

textarea {
  width: 100%;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 14px;
  resize: none;
  outline: none;
  color: #333;
  font-size: 14px;
  font-family: inherit;
  line-height: 1.5;
}

textarea:focus {
  border-color: #ff8ab8;
  box-shadow: 0 0 0 3px rgba(255, 143, 184, 0.15);
}

textarea:disabled {
  cursor: not-allowed;
  background: #f5f6f8;
}

.input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  color: #98a2b3;
  font-size: 12px;
}

.input-footer button {
  border: none;
  border-radius: 999px;
  padding: 9px 22px;
  color: #fff;
  background: linear-gradient(135deg, #ff6fa5, #ff1493);
  cursor: pointer;
  font-size: 14px;
}

.input-footer button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .service-main {
    width: calc(100% - 20px);
    padding-top: 74px;
    padding-bottom: 16px;
  }

  .chat-card {
    height: calc(100vh - 92px);
    min-height: 520px;
    border-radius: 14px;
  }

  .chat-header {
    padding: 14px;
  }

  .service-title h1 {
    font-size: 17px;
  }

  .reconnect-btn {
    padding: 7px 10px;
  }

  .message-list {
    padding: 14px;
  }

  .message-body {
    max-width: 76%;
  }
}
</style>
