<template>
  <div class="ai-page">
    <Navbar />

    <main class="ai-main">
      <!-- 左侧会话栏：支持折叠、多选置顶/删除、重命名 -->
      <aside class="session-side" :class="{ collapsed: sidebarCollapsed }">
        <template v-if="!sidebarCollapsed">
          <div class="session-top">
            <button type="button" class="new-session-btn" @click="newSession">
              <span class="plus">＋</span> 新对话
            </button>
            <div class="side-tools">
              <button
                type="button"
                class="icon-btn"
                :class="{ active: multiSelect }"
                title="多选管理"
                @click="toggleMultiSelect"
              >
                <Close v-if="multiSelect" />
                <Select v-else />
              </button>
              <button type="button" class="icon-btn" title="收起边栏" @click="toggleSidebar"><Fold /></button>
            </div>
          </div>

          <!-- 历史会话列表（置顶会话排在前面） -->
          <div class="session-list">
            <div
              v-for="item in sortedSessions"
              :key="item.id"
              class="session-item"
              :class="{ active: item.id === currentSessionId, pinned: isPinned(item), selecting: multiSelect }"
              @click="onSessionClick(item)"
            >
              <label v-if="multiSelect" class="session-check" @click.stop>
                <input
                  type="checkbox"
                  :checked="isSelected(item.id)"
                  @change="toggleSelect(item.id)"
                />
              </label>

              <span v-if="isPinned(item)" class="pin-badge" title="已置顶">📌</span>

              <div class="session-main">
                <input
                  v-if="renamingId === item.id"
                  v-model="renameDraft"
                  class="rename-input"
                  maxlength="40"
                  @click.stop
                  @keydown.enter.prevent="confirmRename(item)"
                  @keydown.esc.prevent="cancelRename"
                  @blur="confirmRename(item)"
                  :ref="(el) => { if (el && renamingId === item.id) el.focus() }"
                />
                <span v-else class="session-title">{{ item.title || '新对话' }}</span>
                <span class="session-time">{{ formatDate(item.updatedAt) }}</span>
              </div>

              <!-- 会话项悬浮操作：置顶 / 重命名 / 删除 -->
              <div v-if="!multiSelect && renamingId !== item.id" class="item-actions" @click.stop>
                <button
                  type="button"
                  class="mini-btn"
                  :title="isPinned(item) ? '取消置顶' : '置顶'"
                  @click="togglePin(item)"
                >
                  {{ isPinned(item) ? '取消置顶' : '置顶' }}
                </button>
                <button type="button" class="mini-btn" title="重命名" @click="startRename(item)">重命名</button>
                <button type="button" class="mini-btn danger" title="删除" @click="removeSession(item)">删除</button>
              </div>
            </div>
            <p v-if="!sortedSessions.length" class="session-empty">暂无历史会话</p>
          </div>

          <!-- 多选批量操作栏：全选 / 置顶 / 删除 -->
          <div v-if="multiSelect" class="multi-bar">
            <span class="selected-count">已选 {{ selectedIds.length }} 项</span>
            <div class="multi-actions">
              <button type="button" class="mini-btn" @click="toggleSelectAll">
                {{ selectedIds.length === sortedSessions.length && sortedSessions.length ? '取消全选' : '全选' }}
              </button>
              <button type="button" class="mini-btn" :disabled="!selectedIds.length" @click="batchPin">置顶</button>
              <button type="button" class="mini-btn danger" :disabled="!selectedIds.length" @click="batchDelete">删除</button>
            </div>
          </div>
        </template>

        <button v-else type="button" class="expand-btn" title="展开边栏" @click="toggleSidebar"><Expand /></button>
      </aside>

      <section class="chat-card">
        <header class="chat-header">
          <div class="ai-avatar"><img :src="logo" alt="AI" class="ai-avatar-img" /></div>
          <div class="chat-title">
            <h1>AI 荐书</h1>
            <p>说说你想看什么，剩下的交给它</p>
          </div>
        </header>

        <div ref="messageList" class="message-list">
          <div v-if="!messages.length" class="welcome">
            <p class="welcome-title">你好，我是 AI 荐书助手 ✨</p>
            <p class="welcome-sub">告诉我你的口味，比如「想看校园恋爱、最好带点催泪的」，我会推荐能直接点开的书。</p>
          </div>

          <div v-for="m in messages" :key="m.localId" :class="['message-row', m.role]">
            <p v-if="m.role === 'system'" class="system-message">{{ m.content }}</p>
            <template v-else>
              <div class="message-avatar">
                <img v-if="m.role === 'assistant'" :src="logo" alt="AI" class="message-avatar-img" />
                <span v-else>我</span>
              </div>
              <div class="message-body">
                <div
  v-if="m.role === 'assistant' && (m.thinking || (m.pending && !m.content))"
  class="think-box"
>
  <button type="button" class="think-box-head" @click="toggleThink(m)">
    <span>{{ m.thinking ? '🤔 思考过程' : '思考中…' }}</span>
    <span class="think-box-arrow">{{ m.thinkExpanded ? '▴' : '▾' }}</span>
  </button>
  <div v-if="m.thinkExpanded" class="think-box-body">{{ m.thinking || '正在分析你的偏好…' }}</div>
</div>

<p v-if="m.content" class="message-content">{{ m.content }}</p>

                <img v-if="m.image" :src="m.image" class="msg-image" alt="上传的图片" />

                <div v-if="m.books && m.books.length" class="book-list">
                  <router-link
                    v-for="b in m.books"
                    :key="b.novelId"
                    :to="`/book/detail/${b.novelId}`"
                    class="book-card"
                  >
                    <img
                      v-if="b.cover"
                      :src="coverUrl(b)"
                      class="book-cover"
                      alt=""
                      @error="onCoverError"
                    />
                    <div v-else class="book-cover book-cover-ph">暂无封面</div>
                    <div class="book-meta">
                      <p class="book-title">{{ b.title }}</p>
                      <p class="book-reason">{{ b.reason }}</p>
                    </div>
                  </router-link>
                </div>

                <span class="message-time">{{ formatTime(m.sentAt) }}</span>
              </div>
            </template>
          </div>
        </div>

        <form class="chat-input-area" @submit.prevent="send">
          <div v-if="imagePreview" class="image-preview">
            <img :src="imagePreview" alt="图片预览" />
            <button type="button" class="image-remove" @click="removeImage">×</button>
          </div>
          <textarea
            v-model="draft"
            rows="3"
            maxlength="500"
            :disabled="sending"
            placeholder="说说你想看什么，或者上传一张图…（Enter 发送，Shift + Enter 换行）"
            @keydown.enter.exact.prevent="send"
          ></textarea>
          <div class="input-footer">
            <div class="input-left">
              <button type="button" class="upload-btn" :disabled="sending || uploading" @click="pickImage">
                {{ uploading ? '上传中…' : '🖼 上传图片' }}
              </button>
              <span>{{ draft.length }}/500</span>
            </div>
            <input ref="fileInput" type="file" accept="image/*" class="file-input" @change="onFileChange" />
            <button type="submit" class="send-btn" :disabled="sending || (!draft.trim() && !imageUrl)">发送</button>
          </div>
        </form>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import Navbar from '@/components/Navbar.vue'
import { backendUrl } from '@/config/env'
import {
  deleteSession,
  listMessages,
  listSessions,
  streamChat,
  updateSessionPin,
  updateSessionTitle,
  uploadImage
} from '@/api/aiAssistant'
import logo from '@/assets/logo.png'
import { Select, Close, Fold, Expand } from '@element-plus/icons-vue'

const sessions = ref([])
const currentSessionId = ref(null)
const messages = ref([])
const draft = ref('')
const sending = ref(false)
const imageUrl = ref('')
const imagePreview = ref('')
const uploading = ref(false)
const messageList = ref(null)
const fileInput = ref(null)

// ========== 侧边栏会话管理（折叠 / 多选 / 置顶 / 重命名 / 删除） ==========
const sidebarCollapsed = ref(false)
const multiSelect = ref(false)
const selectedIds = ref([])
const renamingId = ref(null)
const renameDraft = ref('')

let localIdSeed = 0

/** 置顶优先，其次按最近活跃时间倒序 */
const sortedSessions = computed(() =>
  [...sessions.value].sort((a, b) => {
    const pa = isPinned(a) ? 1 : 0
    const pb = isPinned(b) ? 1 : 0
    if (pa !== pb) return pb - pa
    return new Date(b.updatedAt || 0).getTime() - new Date(a.updatedAt || 0).getTime()
  })
)

// 判断会话是否置顶
function isPinned(item) {
  return item.pinned === 1 || item.pinned === true
}

// 判断会话是否被多选勾选
function isSelected(id) {
  return selectedIds.value.includes(id)
}

// 展开 / 收起思考过程小框
function toggleThink(message) {
  message.thinkExpanded = !message.thinkExpanded
}

function checkLogin() {
  if (!localStorage.getItem('token')) {
    window.location.assign('/login')
    return false
  }
  return true
}

function coverUrl(book) {
  if (!book || !book.cover) return ''
  return `${backendUrl('/novel/cover')}/${encodeURIComponent(book.cover)}`
}

function onCoverError(event) {
  event.target.src = '/src/assets/default-cover.jpg'
}

function pad(n) {
  return String(n).padStart(2, '0')
}

function formatTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatDate(value) {
  return formatTime(value)
}

function scrollToBottom() {
  nextTick(() => {
    const el = messageList.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function mapMessage(raw) {
  let books = []
  if (raw.booksJson) {
    try {
      books = JSON.parse(raw.booksJson)
    } catch (e) {
      books = []
    }
  }
  return {
    localId: ++localIdSeed,
    id: raw.id,
    role: raw.role === 'USER' ? 'user' : 'assistant',
    content: raw.content,
    thinking: raw.thinking || '',
    thinkExpanded: false,
    image: raw.imageUrl || '',
    books,
    sentAt: raw.createdAt,
    pending: false
  }
}

async function loadSessions() {
  if (!checkLogin()) return
  try {
    const res = await listSessions()
    sessions.value = res.data || []
  } catch (e) {
    sessions.value = []
  }
}

async function selectSession(id) {
  if (!checkLogin()) return
  currentSessionId.value = id
  messages.value = []
  try {
    const res = await listMessages(id)
    messages.value = (res.data || []).map(mapMessage)
  } catch (e) {
    messages.value = [{ localId: ++localIdSeed, role: 'system', content: '历史消息加载失败', books: [] }]
  }
  scrollToBottom()
}

function newSession() {
  currentSessionId.value = null
  messages.value = []
  draft.value = ''
  multiSelect.value = false
  selectedIds.value = []
  renamingId.value = null
  removeImage()
}

async function send() {
  if (!checkLogin()) return
  const content = draft.value.trim()
  const picture = imageUrl.value
  if ((!content && !picture) || sending.value) return

  sending.value = true
  draft.value = ''

  const userIndex = messages.value.length
  messages.value.push({
    localId: ++localIdSeed,
    id: null,
    role: 'user',
    content,
    image: picture || '',
    books: [],
    sentAt: new Date(),
    pending: false
  })

  const assistantIndex = messages.value.length
  messages.value.push({
    localId: ++localIdSeed,
    id: null,
    role: 'assistant',
    content: '',
    thinking: '',
    thinkExpanded: true,
    books: [],
    sentAt: new Date(),
    pending: true
  })
  scrollToBottom()

  try {
    await streamChat({ sessionId: currentSessionId.value, content, imageUrl: picture || null }, (event) => {
      if (event.type === 'session') {
        currentSessionId.value = event.sessionId
      } else if (event.type === 'think') {
        messages.value[assistantIndex].thinking += event.text || ''
        scrollToBottom()
      } else if (event.type === 'reply') {
        messages.value[assistantIndex].content += event.text || ''
        messages.value[assistantIndex].pending = false
        scrollToBottom()
      } else if (event.type === 'books') {
        messages.value[assistantIndex].books = event.books || []
        scrollToBottom()
      } else if (event.type === 'error') {
        messages.value[assistantIndex].content = event.message || 'AI 助手暂时不可用'
        messages.value[assistantIndex].pending = false
      } else if (event.type === 'done') {
        messages.value[assistantIndex].pending = false
        messages.value[assistantIndex].thinkExpanded = false
        loadSessions()
      }
    })
  } catch (e) {
    messages.value[assistantIndex].content = e.message || '请求失败，请稍后重试'
    messages.value[assistantIndex].pending = false
  } finally {
    sending.value = false
    imageUrl.value = ''
    imagePreview.value = ''
    scrollToBottom()
  }
}

function pickImage() {
  if (!checkLogin()) return
  fileInput.value && fileInput.value.click()
}

async function onFileChange(event) {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    alert('请选择图片文件')
    return
  }
  uploading.value = true
  try {
    const res = await uploadImage(file)
    const url = res && res.data && res.data.url
    if (!url) throw new Error('上传失败')
    imageUrl.value = url
    imagePreview.value = url
  } catch (err) {
    alert(err && err.msg ? err.msg : '图片上传失败')
  } finally {
    uploading.value = false
  }
}

function removeImage() {
  imageUrl.value = ''
  imagePreview.value = ''
}

// 收起 / 展开侧边栏
function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

// 进入 / 退出多选模式，退出时清空勾选
function toggleMultiSelect() {
  multiSelect.value = !multiSelect.value
  selectedIds.value = []
  renamingId.value = null
}

// 会话项点击：多选模式下切换勾选，否则打开会话
function onSessionClick(item) {
  if (renamingId.value === item.id) return
  if (multiSelect.value) {
    toggleSelect(item.id)
    return
  }
  selectSession(item.id)
}

// 勾选 / 取消勾选单个会话
function toggleSelect(id) {
  if (isSelected(id)) {
    selectedIds.value = selectedIds.value.filter((x) => x !== id)
  } else {
    selectedIds.value = [...selectedIds.value, id]
  }
}

// 全选 / 取消全选
function toggleSelectAll() {
  const all = sortedSessions.value.map((s) => s.id)
  selectedIds.value = selectedIds.value.length === all.length ? [] : all
}

// 进入行内重命名状态
function startRename(item) {
  renamingId.value = item.id
  renameDraft.value = item.title || '新对话'
}

// 提交重命名（Enter 或失焦触发）
async function confirmRename(item) {
  if (renamingId.value !== item.id) return
  const title = renameDraft.value.trim()
  const previous = renamingId.value
  renamingId.value = null
  renameDraft.value = ''
  if (previous === item.id && title && title !== (item.title || '')) {
    try {
      await updateSessionTitle(item.id, title)
      item.title = title
    } catch (e) {
      alert(e && e.msg ? e.msg : '重命名失败')
    }
  }
}

// 取消重命名
function cancelRename() {
  renamingId.value = null
  renameDraft.value = ''
}

// 单个会话置顶 / 取消置顶
async function togglePin(item) {
  if (!checkLogin()) return
  const pinned = !isPinned(item)
  try {
    await updateSessionPin(item.id, pinned)
    await loadSessions()
  } catch (e) {
    alert(e && e.msg ? e.msg : '操作失败')
  }
}

// 删除单个会话
async function removeSession(item) {
  if (!checkLogin()) return
  if (!window.confirm(`确定删除会话「${item.title || '新对话'}」吗？`)) return
  try {
    await deleteSession(item.id)
    const wasCurrent = item.id === currentSessionId.value
    await loadSessions()
    if (wasCurrent) {
      currentSessionId.value = null
      messages.value = []
      const first = sortedSessions.value[0]
      if (first) await selectSession(first.id)
    }
  } catch (e) {
    alert(e && e.msg ? e.msg : '删除失败')
  }
}

// 批量置顶选中的会话
async function batchPin() {
  if (!checkLogin() || !selectedIds.value.length) return
  try {
    for (const id of selectedIds.value) {
      await updateSessionPin(id, true)
    }
    await loadSessions()
    selectedIds.value = []
    multiSelect.value = false
  } catch (e) {
    alert(e && e.msg ? e.msg : '置顶失败')
  }
}

// 批量删除选中的会话
async function batchDelete() {
  if (!checkLogin() || !selectedIds.value.length) return
  if (!window.confirm(`确定删除选中的 ${selectedIds.value.length} 个会话吗？此操作不可恢复。`)) return
  const ids = selectedIds.value.slice()
  try {
    for (const id of ids) {
      await deleteSession(id)
    }
    const wasCurrent = ids.includes(currentSessionId.value)
    await loadSessions()
    selectedIds.value = []
    multiSelect.value = false
    if (wasCurrent) {
      currentSessionId.value = null
      messages.value = []
      const first = sortedSessions.value[0]
      if (first) await selectSession(first.id)
    }
  } catch (e) {
    alert(e && e.msg ? e.msg : '删除失败')
  }
}

onMounted(async () => {
  await loadSessions()
  if (sortedSessions.value.length) {
    await selectSession(sortedSessions.value[0].id)
  }
})
</script>

<style scoped>
.ai-page {
  /* 主题色：现代靛蓝 → 紫罗兰渐变 */
  --accent: #6366f1;
  --accent-2: #7c3aed;
  --accent-soft: #eef2ff;
  --accent-border: #e0e7ff;
  --ink: #1f2937;
  --muted: #94a3b8;
  --border: #e6e9f2;
  --card: #ffffff;
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(180deg, #eef2ff 0%, #f7f9fc 100%);
}

.ai-main {
  display: flex;
  gap: 14px;
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
  padding: 74px 14px 14px;
  height: 100vh;
  box-sizing: border-box;
}

/* 左侧会话栏 */
.session-side {
  display: flex;
  flex-direction: column;
  width: 280px;
  flex-shrink: 0;
  gap: 12px;
  transition: width 0.25s ease;
}

.session-side.collapsed {
  width: 52px;
}

.session-top {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.new-session-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: none;
  border-radius: 12px;
  padding: 12px;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  background: linear-gradient(135deg, var(--accent), var(--accent-2));
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.25);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.new-session-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(99, 102, 241, 0.3);
}

.new-session-btn .plus {
  font-size: 16px;
  line-height: 1;
}

.side-tools {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.icon-btn {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--card);
  color: #475569;
  cursor: pointer;
  transition: border-color 0.15s ease, color 0.15s ease, background 0.15s ease;
}

.icon-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.icon-btn.active {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-soft);
}

.icon-btn svg {
  width: 16px;
  height: 16px;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid var(--border);
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.06);
}

.session-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.session-item:hover,
.session-item.active {
  background: var(--accent-soft);
}

.session-item.pinned {
  background: rgba(250, 245, 255, 0.9);
}

.session-item.pinned:hover {
  background: #f3e8ff;
}

.session-check {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 18px;
  height: 18px;
}

.session-check input {
  width: 16px;
  height: 16px;
  accent-color: var(--accent);
  cursor: pointer;
  margin: 0;
}

.pin-badge {
  flex-shrink: 0;
  font-size: 12px;
  line-height: 1;
}

.session-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.session-title {
  color: var(--ink);
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rename-input {
  width: 100%;
  height: 24px;
  border: 1px solid var(--accent);
  border-radius: 6px;
  padding: 2px 6px;
  color: var(--ink);
  font-size: 13px;
  font-family: inherit;
  outline: none;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.12);
}

.session-time {
  color: var(--muted);
  font-size: 11px;
}

.item-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.session-item:hover .item-actions {
  opacity: 1;
}

/* 默认隐藏操作，悬浮时显示，保持列表清爽 */
.item-actions {
  opacity: 0;
  transition: opacity 0.15s ease;
}

.mini-btn {
  border: 1px solid var(--border);
  border-radius: 7px;
  padding: 4px 7px;
  background: #fff;
  color: #475569;
  cursor: pointer;
  font-size: 12px;
  white-space: nowrap;
  transition: color 0.15s ease, border-color 0.15s ease, background 0.15s ease;
}

.mini-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-soft);
}

.mini-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.mini-btn.danger {
  color: #dc2626;
}

.mini-btn.danger:hover {
  border-color: #dc2626;
  color: #dc2626;
  background: #fef2f2;
}

.session-empty {
  margin: 8px 0;
  color: var(--muted);
  font-size: 13px;
  text-align: center;
}

/* 多选批量操作栏 */
.multi-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px;
  background: #fff;
  border: 1px solid var(--accent-border);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.08);
}

.selected-count {
  color: #475569;
  font-size: 12px;
  white-space: nowrap;
}

.multi-actions {
  display: flex;
  gap: 6px;
}

/* 折叠态展开按钮 */
.expand-btn {
  width: 100%;
  height: 44px;
  display: grid;
  place-items: center;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 12px;
  color: var(--accent);
  cursor: pointer;
  font-size: 16px;
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.08);
}

.chat-card {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid var(--border);
  border-radius: 20px;
  box-shadow: 0 16px 40px rgba(99, 102, 241, 0.08);
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 22px;
  border-bottom: 1px solid var(--border);
  background: linear-gradient(135deg, #ffffff 0%, var(--accent-soft) 100%);
}

.ai-avatar {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #fff;
  border: 1px solid var(--accent-border);
  overflow: hidden;
}

.ai-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 5px;
  box-sizing: border-box;
}

.chat-title h1 {
  margin: 0;
  color: var(--ink);
  font-size: 20px;
}

.chat-title p {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 22px;
  background: #fbfcfe;
}

.welcome {
  max-width: 560px;
  margin: 40px auto;
  text-align: center;
  color: #667085;
}

.welcome-title {
  margin: 0;
  font-size: 18px;
  color: var(--ink);
}

.welcome-sub {
  margin: 10px 0 0;
  font-size: 14px;
  line-height: 1.6;
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
  background: var(--accent-soft);
  color: #64748b;
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
  background: #fff;
  border: 1px solid #e9ecf5;
  overflow: hidden;
}

.message-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 3px;
  box-sizing: border-box;
}

.message-row.user .message-avatar {
  background: var(--accent);
}

.message-body {
  max-width: 78%;
}

/* AI 思考过程：thinking 包裹的内容，用小框展示 */
.think-box {
  margin-bottom: 8px;
  border: 1px solid var(--border);
  border-left: 3px solid var(--accent);
  border-radius: 10px;
  background: #f8fafc;
  overflow: hidden;
}

.think-box-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  padding: 6px 10px;
  background: transparent;
  border: none;
  color: #64748b;
  cursor: pointer;
  font-size: 12px;
}

.think-box-arrow {
  font-size: 11px;
  line-height: 1;
}

.think-box-body {
  padding: 0 10px 8px;
  color: #64748b;
  font-size: 12.5px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.message-content {
  margin: 0;
  padding: 11px 14px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid var(--border);
  color: var(--ink);
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-row.user .message-content {
  color: #fff;
  border-color: transparent;
  background: linear-gradient(135deg, var(--accent), var(--accent-2));
}

.message-row.user .message-body {
  text-align: right;
}

.book-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 10px;
}

.book-card {
  display: flex;
  gap: 12px;
  padding: 10px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid var(--border);
  text-decoration: none;
  transition: box-shadow 0.2s ease, transform 0.2s ease, border-color 0.2s ease;
}

.book-card:hover {
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.14);
  border-color: var(--accent-border);
  transform: translateY(-2px);
}

.book-cover {
  width: 52px;
  height: 70px;
  flex-shrink: 0;
  border-radius: 8px;
  object-fit: cover;
  background: #f3f4f6;
}

.book-cover-ph {
  display: grid;
  place-items: center;
  color: var(--muted);
  font-size: 12px;
}

.book-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
}

.book-title {
  margin: 0;
  color: var(--ink);
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.book-reason {
  margin: 0;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.5;
}

.message-time {
  display: block;
  margin-top: 5px;
  color: var(--muted);
  font-size: 11px;
}

.msg-image {
  display: block;
  max-width: 220px;
  max-height: 220px;
  margin-top: 8px;
  border-radius: 10px;
  object-fit: cover;
}

.chat-input-area {
  padding: 14px 18px 16px;
  border-top: 1px solid var(--border);
  background: #fff;
}

textarea {
  width: 100%;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 14px;
  resize: none;
  outline: none;
  color: var(--ink);
  font-size: 14px;
  font-family: inherit;
  line-height: 1.5;
  box-sizing: border-box;
}

textarea:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
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
  color: var(--muted);
  font-size: 12px;
}

.input-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.upload-btn {
  border: 1px solid var(--accent-border);
  border-radius: 999px;
  padding: 6px 12px;
  background: var(--accent-soft);
  color: var(--accent);
  cursor: pointer;
  font-size: 13px;
}

.upload-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.file-input {
  display: none;
}

.image-preview {
  position: relative;
  width: 72px;
  height: 72px;
  margin-bottom: 8px;
}

.image-preview img {
  width: 100%;
  height: 100%;
  border-radius: 10px;
  object-fit: cover;
}

.image-remove {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 50%;
  color: #fff;
  background: #1f2937;
  cursor: pointer;
  font-size: 12px;
  line-height: 1;
}

.input-footer .send-btn {
  border: none;
  border-radius: 999px;
  padding: 9px 22px;
  color: #fff;
  background: linear-gradient(135deg, var(--accent), var(--accent-2));
  cursor: pointer;
  font-size: 14px;
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.25);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.input-footer .send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(99, 102, 241, 0.3);
}

.input-footer .send-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

@media (max-width: 760px) {
  .ai-main {
    width: 100%;
    padding-top: 66px;
    padding-bottom: 8px;
    height: 100vh;
  }

  .session-side {
    display: none;
  }

  .chat-card {
    border-radius: 14px;
  }

  .message-body {
    max-width: 82%;
  }
}
</style>
