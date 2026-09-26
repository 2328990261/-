<template>
  <div class="service-console">
    <aside class="conversation-panel">
      <header class="panel-header">
        <div>
          <h2>客服会话</h2>
          <p :class="['connection-status', connectionState]">{{ connectionText }}</p>
        </div>
        <button type="button" @click="loadConversations">刷新</button>
      </header>

      <div class="conversation-list">
        <button
          v-for="conversation in conversations"
          :key="conversation.userId"
          type="button"
          class="conversation-item"
          :class="{ active: conversation.userId === selectedUserId }"
          @click="selectConversation(conversation.userId)"
        >
          <span class="conversation-avatar">{{ (conversation.username || '用户').slice(0, 1) }}</span>
          <span class="conversation-main">
            <span class="conversation-top">
              <strong>{{ conversation.username || `用户 ${conversation.userId}` }}</strong>
              <small>{{ formatTime(conversation.lastMessageTime) }}</small>
            </span>
            <span class="conversation-preview">{{ conversation.lastContent }}</span>
          </span>
          <span v-if="conversation.unreadCount > 0" class="unread-badge">
            {{ conversation.unreadCount > 99 ? '99+' : conversation.unreadCount }}
          </span>
        </button>
        <p v-if="conversations.length === 0" class="empty-conversation">暂无用户咨询</p>
      </div>
    </aside>

    <section class="chat-panel">
      <header class="chat-panel-header">
        <div>
          <h1>{{ selectedConversation ? selectedConversation.username : '选择会话' }}</h1>
          <p>{{ selectedConversation ? `用户ID：${selectedUserId}` : '从左侧选择用户后进行人工回复' }}</p>
        </div>
        <button type="button" :disabled="connectionState !== 'connected'" @click="connect">重新连接</button>
      </header>

      <div ref="messageList" class="message-list">
        <div v-for="message in messages" :key="message.id" :class="['message-row', message.role]">
          <p v-if="message.role === 'system'" class="system-message">{{ message.content }}</p>
          <template v-else>
            <div class="message-avatar">{{ message.role === 'service' ? '客' : '我' }}</div>
            <div class="message-body">
              <p class="message-content">{{ message.content }}</p>
              <span class="message-time">{{ formatTime(message.sentAt) }}</span>
            </div>
          </template>
        </div>
        <p v-if="selectedUserId && messages.length === 0" class="empty-conversation">该用户暂无历史消息</p>
      </div>

      <form class="reply-form" @submit.prevent="sendMessage">
        <textarea
          v-model="draft"
          rows="3"
          maxlength="500"
          :disabled="!selectedUserId || connectionState !== 'connected'"
          placeholder="输入人工客服回复，Enter 发送，Shift + Enter 换行"
          @keydown.enter.exact.prevent="sendMessage"
        ></textarea>
        <div class="reply-footer">
          <span>{{ draft.length }}/500</span>
          <button type="submit" :disabled="!canSend">发送回复</button>
        </div>
      </form>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import adminHttp from '@/utils/adminHttp'
import { API_BASE_URL } from '@/config/env'

const conversations = ref([])
const messages = ref([])
const selectedUserId = ref(null)
const draft = ref('')
const connectionState = ref('disconnected')
const messageList = ref(null)

let socket = null

const selectedConversation = computed(() =>
  conversations.value.find((conversation) => conversation.userId === selectedUserId.value)
)
const canSend = computed(() =>
  selectedUserId.value && connectionState.value === 'connected' && draft.value.trim().length > 0
)
const connectionText = computed(() => {
  if (connectionState.value === 'connected') return '实时连接正常'
  if (connectionState.value === 'connecting') return '连接中...'
  return '连接已断开'
})

function buildWebSocketUrl() {
  const token = localStorage.getItem('token') || ''
  const path = `/ws/customer-service?role=service&token=${encodeURIComponent(token)}`
  if (API_BASE_URL) return `${API_BASE_URL.replace(/^http/i, 'ws')}${path}`
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${protocol}://${window.location.host}${path}`
}

function connect() {
  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) return
  connectionState.value = 'connecting'
  socket = new WebSocket(buildWebSocketUrl())
  socket.onopen = () => {
    connectionState.value = 'connected'
  }
  socket.onclose = () => {
    connectionState.value = 'disconnected'
  }
  socket.onerror = () => {
    connectionState.value = 'disconnected'
  }
  socket.onmessage = (event) => {
    try {
      const payload = JSON.parse(event.data)
      if (payload.type === 'SYSTEM') {
        appendMessage('system', payload)
      } else if (payload.type === 'USER' || payload.type === 'SERVICE') {
        handleChatMessage(payload)
      }
    } catch (error) {
      appendMessage('system', { content: '收到无法解析的消息。', sentAt: Date.now() })
    }
  }
}

function handleChatMessage(payload) {
  if (payload.userId === selectedUserId.value) {
    appendMessage(payload.sender === 'USER' ? 'user' : 'service', payload)
  }
  updateConversation(payload)
  if (payload.type === 'USER' && payload.userId === selectedUserId.value) {
    markConversationRead(payload.userId)
  }
}

async function loadConversations() {
  try {
    const response = await adminHttp.get('/admin/customer-service/conversations')
    if (response.data.code === 200) conversations.value = response.data.data || []
  } catch (error) {
    appendMessage('system', { content: '会话列表加载失败。', sentAt: Date.now() })
  }
}

async function selectConversation(userId) {
  selectedUserId.value = userId
  messages.value = []
  await loadMessages(userId)
  await markConversationRead(userId)
}

async function loadMessages(userId) {
  try {
    const response = await adminHttp.get(`/admin/customer-service/messages/${userId}`)
    if (response.data.code === 200) {
      messages.value = (response.data.data || []).map((message) => ({
        id: message.id,
        role: message.sender === 'USER' ? 'user' : 'service',
        content: message.content,
        sentAt: new Date(message.createdAt).getTime()
      }))
      nextTick(scrollToBottom)
    }
  } catch (error) {
    appendMessage('system', { content: '聊天记录加载失败。', sentAt: Date.now() })
  }
}

async function markConversationRead(userId) {
  const conversation = conversations.value.find((item) => item.userId === userId)
  if (conversation) conversation.unreadCount = 0
  try {
    await adminHttp.put(`/admin/customer-service/messages/${userId}/read`)
  } catch (error) {
    // 已读状态更新失败不影响当前聊天，下次刷新会重新计算。
  }
}

function sendMessage() {
  if (!canSend.value) return
  const content = draft.value.trim()
  socket.send(JSON.stringify({ type: 'SERVICE_REPLY', userId: selectedUserId.value, content }))
  draft.value = ''
}

function updateConversation(payload) {
  let conversation = conversations.value.find((item) => item.userId === payload.userId)
  if (!conversation) {
    conversation = {
      userId: payload.userId,
      username: payload.senderUsername || `用户 ${payload.userId}`,
      unreadCount: 0
    }
    conversations.value.unshift(conversation)
  }
  conversation.lastContent = payload.content
  conversation.lastSender = payload.sender
  conversation.lastMessageTime = payload.sentAt
  if (payload.type === 'USER' && payload.userId !== selectedUserId.value) {
    conversation.unreadCount = (conversation.unreadCount || 0) + 1
  }
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

function scrollToBottom() {
  if (messageList.value) messageList.value.scrollTop = messageList.value.scrollHeight
}

function formatTime(timestamp) {
  if (!timestamp) return ''
  return new Date(timestamp).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

onMounted(() => {
  loadConversations()
  connect()
})

onBeforeUnmount(() => {
  if (socket) {
    socket.close()
    socket = null
  }
})
</script>

<style scoped>
.service-console {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  height: calc(100vh - 118px);
  min-height: 560px;
  padding: 16px;
}

.conversation-panel,
.chat-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  overflow: hidden;
}

.panel-header,
.chat-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid #eef0f4;
}

.panel-header h2,
.chat-panel-header h1 {
  margin: 0;
  color: #1f2937;
  font-size: 18px;
}

.panel-header p,
.chat-panel-header p {
  margin: 5px 0 0;
  color: #6b7280;
  font-size: 12px;
}

.connection-status.connected { color: #16a34a; }
.connection-status.connecting { color: #d97706; }

.panel-header button,
.chat-panel-header button {
  border: 1px solid #ff1493;
  border-radius: 999px;
  padding: 7px 12px;
  color: #ff1493;
  background: #fff;
  cursor: pointer;
  font-size: 12px;
}

.panel-header button:hover,
.chat-panel-header button:hover:not(:disabled) {
  color: #fff;
  background: #ff1493;
}

.chat-panel-header button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.conversation-list,
.message-list {
  flex: 1;
  overflow-y: auto;
}

.conversation-list { padding: 10px; }

.conversation-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  border: none;
  border-radius: 10px;
  padding: 10px;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.conversation-item:hover,
.conversation-item.active { background: #fff1f7; }

.conversation-avatar {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border-radius: 50%;
  color: #fff;
  background: #ff6fa5;
  font-weight: 700;
}

.conversation-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  flex: 1;
}

.conversation-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.conversation-top strong { color: #1f2937; font-size: 14px; }
.conversation-top small { color: #9ca3af; font-size: 11px; }

.conversation-preview {
  overflow: hidden;
  color: #6b7280;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread-badge {
  min-width: 20px;
  border-radius: 999px;
  padding: 2px 6px;
  background: #ef4444;
  color: #fff;
  font-size: 11px;
  text-align: center;
}

.empty-conversation {
  margin: 0;
  padding: 30px 16px;
  color: #9ca3af;
  font-size: 13px;
  text-align: center;
}

.message-list {
  padding: 18px;
  background: #f8fafc;
}

.message-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  margin-bottom: 14px;
}

.message-row.user { flex-direction: row-reverse; }
.message-row.system { justify-content: center; }

.system-message {
  margin: 0;
  border-radius: 999px;
  padding: 5px 10px;
  background: #e5e7eb;
  color: #6b7280;
  font-size: 12px;
}

.message-avatar {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 50%;
  color: #fff;
  background: #8a97a8;
  font-size: 12px;
}

.message-row.user .message-avatar { background: #ff6fa5; }
.message-body { max-width: 68%; }

.message-content {
  margin: 0;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 10px 12px;
  background: #fff;
  color: #1f2937;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-row.user .message-content {
  color: #fff;
  border-color: transparent;
  background: #ff1493;
}

.message-time {
  display: block;
  margin-top: 4px;
  color: #9ca3af;
  font-size: 11px;
}

.reply-form {
  padding: 14px;
  border-top: 1px solid #eef0f4;
  background: #fff;
}

textarea {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 10px 12px;
  resize: none;
  outline: none;
  font-family: inherit;
  font-size: 14px;
}

textarea:focus {
  border-color: #ff8ab8;
  box-shadow: 0 0 0 3px rgba(255, 143, 184, 0.15);
}

textarea:disabled {
  background: #f3f4f6;
  cursor: not-allowed;
}

.reply-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  color: #6b7280;
  font-size: 12px;
}

.reply-footer button {
  border: none;
  border-radius: 999px;
  padding: 8px 18px;
  background: #ff1493;
  color: #fff;
  cursor: pointer;
}

.reply-footer button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .service-console {
    grid-template-columns: 1fr;
    height: auto;
  }

  .conversation-list {
    max-height: 260px;
  }
}
</style>
