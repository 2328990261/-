<template>
  <div class="ai-config-page">
    <h2 class="page-heading">AI 助手配置</h2>
    <p class="lead">
      配置轻小说 AI 荐书助手的人设、推荐规则与所用模型，并在下方直接对话测试。
      人设与推荐规则会拼成系统提示词（人设在前、推荐规则追加在末尾）。
    </p>

    <section v-if="loadError" class="card error-card">{{ loadError }}</section>

    <section v-else class="card">
      <div class="section-title">人设与推荐规则</div>

      <div class="form-block">
        <label class="label-block">
          <span class="label-title">人设（Persona）</span>
          <span class="label-sub">定义助手是谁、说话风格与应对话术；闲聊时生效。</span>
        </label>
        <textarea v-model="cfg.persona" class="textarea" rows="7" placeholder="例如：你是轻小说推荐助手 MiMo，说话温柔但专业……"></textarea>
      </div>

      <div class="form-block">
        <label class="label-block">
          <span class="label-title">推荐规则（Recommendation Rules）</span>
          <span class="label-sub">追加在系统提示词末尾，约束书单以 JSON 输出；可自定义，留空则保存默认规则。</span>
        </label>
        <textarea v-model="cfg.recommendationRules" class="textarea" rows="8" placeholder="例如：【推荐场景】当用户需要推荐轻小说时，只输出 JSON……"></textarea>
      </div>

      <div class="section-title">模型与连接</div>

      <div class="form-row">
        <label class="label-block">
          <span class="label-title">选用模型</span>
          <span class="label-sub">可从预设选择，也可直接输入自定义模型名</span>
        </label>
        <div class="field-wrap">
          <input v-model="cfg.model" list="ai-model-options" type="text" class="field" placeholder="mimo-v2.6-pro" />
          <datalist id="ai-model-options">
            <option value="mimo-v2.6-pro"></option>
            <option value="deepseek-v4-flash"></option>
            <option value="glm-5-3-flash"></option>
          </datalist>
        </div>
      </div>

      <div class="form-row">
        <label class="label-block">
          <span class="label-title">接口地址（Base URL）</span>
          <span class="label-sub">OpenAI 兼容接口，需包含 <code>/v1</code></span>
        </label>
        <input v-model="cfg.baseUrl" type="text" class="field" placeholder="https://api.example.com/v1" />
      </div>

      <div class="form-row">
        <label class="label-block">
          <span class="label-title">API Key</span>
          <span class="label-sub">密钥加密存储、脱敏显示；输入新密钥才会覆盖，留空保持不变</span>
        </label>
        <input v-model="cfg.apiKey" type="password" class="field" placeholder="sk-..." autocomplete="new-password" />
      </div>

      <div class="form-row">
        <label class="label-block">
          <span class="label-title">采样温度</span>
          <span class="label-sub">0～2 之间，越高越随机</span>
        </label>
        <input v-model.number="cfg.temperature" type="number" min="0" max="2" step="0.1" class="field narrow" />
      </div>

      <p v-if="cfg.updatedAt" class="meta">最近更新：{{ cfg.updatedAt }}</p>

      <div class="actions">
        <button type="button" class="admin-btn admin-btn--primary" :disabled="saving" @click="save">
          {{ saving ? '保存中...' : '保存配置' }}
        </button>
        <button type="button" class="admin-btn admin-btn--secondary" @click="load">重新加载</button>
      </div>
      <p v-if="tip" class="tip" :class="{ ok: tipOk }">{{ tip }}</p>
    </section>

    <section class="card test-card">
      <div class="section-title">对话测试</div>
      <p class="lead">
        测试使用<strong>当前表单（未保存）</strong>的人设与模型，刷新后测试记录清空。
      </p>

      <div class="test-toolbar">
        <label class="mode-option">
          <input v-model="mode" type="radio" value="persona" />
          <span>仅人设闲聊</span>
        </label>
        <label class="mode-option">
          <input v-model="mode" type="radio" value="recommend" />
          <span>完整推荐（带书单）</span>
        </label>
        <button type="button" class="admin-btn admin-btn--secondary" @click="clearMessages">清空对话</button>
      </div>

      <div ref="messageList" class="message-list">
        <div v-if="!messages.length" class="welcome">
          发送一条消息试试，例如「你好，你是谁」或「想看校园恋爱，最好带点催泪的」。
        </div>

        <div v-for="m in messages" :key="m.localId" class="message-row" :class="m.role">
          <div class="message-avatar">{{ m.role === 'user' ? '我' : 'AI' }}</div>
          <div class="message-body">
            <div
              v-if="m.role === 'assistant' && (m.thinking || m.pending)"
              class="think-box"
            >
              <button type="button" class="think-box-head" @click="m.thinkExpanded = !m.thinkExpanded">
                <span>{{ m.thinking ? '🤔 思考过程' : '思考中…' }}</span>
                <span class="think-box-arrow">{{ m.thinkExpanded ? '▴' : '▾' }}</span>
              </button>
              <div v-if="m.thinkExpanded" class="think-box-body">{{ m.thinking || '正在分析…' }}</div>
            </div>

            <p v-if="m.content" class="message-content">{{ m.content }}</p>
            <p v-else-if="m.pending" class="message-content pending-text">正在思考…</p>

            <div v-if="m.books && m.books.length" class="book-list">
              <router-link
                v-for="b in m.books"
                :key="b.novelId"
                :to="`/book/detail/${b.novelId}`"
                class="book-card"
              >
                <img v-if="b.cover" :src="b.cover" class="book-cover" :alt="b.title" />
                <div class="book-meta">
                  <p class="book-title">{{ b.title || '未命名' }}</p>
                  <p class="book-reason">{{ b.reason || '' }}</p>
                </div>
              </router-link>
            </div>
          </div>
        </div>
      </div>

      <div class="test-input">
        <textarea
          v-model="draft"
          class="textarea"
          rows="3"
          placeholder="输入测试内容，回车发送（Shift+Enter 换行）"
          :disabled="sending"
          @keydown.enter.exact.prevent="send"
        ></textarea>
        <div class="test-input-footer">
          <span class="meta">模式：{{ mode === 'persona' ? '仅人设闲聊' : '完整推荐' }}</span>
          <button type="button" class="admin-btn admin-btn--primary" :disabled="sending || !draft.trim()" @click="send">
            {{ sending ? '回复中...' : '发送' }}
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import adminHttp from '@/utils/adminHttp'
import { backendUrl } from '@/config/env'

const defaultCfg = () => ({
  id: 1,
  persona: '',
  recommendationRules: '',
  model: 'mimo-v2.6-pro',
  baseUrl: 'https://api.ls1223.cn/v1',
  apiKey: '',
  temperature: 0.6,
  updatedAt: null
})

const cfg = ref(defaultCfg())
const loadedApiKey = ref('')
const loadError = ref('')
const saving = ref(false)
const tip = ref('')
const tipOk = ref(false)

const mode = ref('persona')
const draft = ref('')
const sending = ref(false)
const messages = ref([])
const messageList = ref(null)
let localIdSeed = 0
const apiKeyForSubmit = () => (cfg.value.apiKey === loadedApiKey.value ? '' : cfg.value.apiKey)

const mapFromApi = (d) => {
  if (!d || typeof d !== 'object') return defaultCfg()
  return {
    id: d.id != null ? Number(d.id) : 1,
    persona: d.persona || '',
    recommendationRules: d.recommendationRules || '',
    model: d.model || 'mimo-v2.6-pro',
    baseUrl: d.baseUrl || 'https://api.ls1223.cn/v1',
    apiKey: d.apiKey || '',
    temperature: d.temperature != null ? Number(d.temperature) : 0.6,
    updatedAt: d.updatedAt || null
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageList.value) {
    messageList.value.scrollTop = messageList.value.scrollHeight
  }
}

const load = async () => {
  loadError.value = ''
  try {
    const res = await adminHttp.get('/admin/ai-assistant-config')
    if (res.data?.code === 200) {
      cfg.value = mapFromApi(res.data.data)
      loadedApiKey.value = cfg.value.apiKey
    } else {
      loadError.value = res.data?.msg || '加载失败'
    }
  } catch (e) {
    console.error(e)
    loadError.value = '无法连接服务或加载失败。请确认后端已执行 sql/ai_assistant_config.sql 建表脚本。'
  }
}

const save = async () => {
  tip.value = ''
  saving.value = true
  try {
    const res = await adminHttp.put('/admin/ai-assistant-config', { ...cfg.value, apiKey: apiKeyForSubmit() })
    if (res.data?.code === 200) {
      tipOk.value = true
      tip.value = '保存成功'
      load()
    } else {
      tipOk.value = false
      tip.value = res.data?.msg || '保存失败'
    }
  } catch (e) {
    tipOk.value = false
    tip.value = e?.msg || e?.message || '保存失败'
  } finally {
    saving.value = false
  }
}

const clearMessages = () => {
  messages.value = []
}

const send = async () => {
  const content = draft.value.trim()
  if (!content || sending.value) return

  messages.value.push({ localId: ++localIdSeed, role: 'user', content, thinking: '', books: [], pending: false })
  draft.value = ''

  const assistantIndex = messages.value.length
  messages.value.push({
    localId: ++localIdSeed,
    role: 'assistant',
    content: '',
    thinking: '',
    thinkExpanded: true,
    books: [],
    pending: true
  })
  await scrollToBottom()

  const payload = {
    persona: cfg.value.persona || '',
    recommendationRules: cfg.value.recommendationRules || '',
    model: cfg.value.model || '',
    baseUrl: cfg.value.baseUrl || '',
    apiKey: apiKeyForSubmit(),
    temperature: cfg.value.temperature != null ? Number(cfg.value.temperature) : 0.6,
    mode: mode.value,
    content
  }

  sending.value = true
  try {
    await streamTestChat(payload, (event) => {
      if (event.type === 'think') {
        messages.value[assistantIndex].thinking += event.text || ''
        scrollToBottom()
      } else if (event.type === 'reply') {
        messages.value[assistantIndex].content += event.text || ''
        messages.value[assistantIndex].pending = false
        scrollToBottom()
      } else if (event.type === 'books') {
        messages.value[assistantIndex].books = event.books || []
      } else if (event.type === 'error') {
        messages.value[assistantIndex].content = event.message || 'AI 助手暂时不可用'
        messages.value[assistantIndex].pending = false
      } else if (event.type === 'done') {
        messages.value[assistantIndex].pending = false
        messages.value[assistantIndex].thinkExpanded = false
      }
    })
  } catch (e) {
    messages.value[assistantIndex].content = e?.message || '请求失败，请稍后重试'
    messages.value[assistantIndex].pending = false
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

const streamTestChat = async (body, onEvent) => {
  const token = localStorage.getItem('token') || ''
  const controller = new AbortController()
  const response = await fetch(backendUrl('/api/admin/ai-assistant/test'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(body),
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
}

onMounted(load)
</script>

<style scoped>
.ai-config-page {
  padding: 0;
}

.page-heading {
  margin: 0 0 12px 0;
  font-size: 20px;
}

.lead {
  margin: 0 0 16px 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
  max-width: 860px;
}

.lead code {
  font-size: 12px;
  background: #f1f5f9;
  padding: 1px 6px;
  border-radius: 4px;
}

.card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px 22px;
  max-width: 860px;
  margin-bottom: 22px;
}

.error-card {
  color: #b91c1c;
  background: #fef2f2;
  border-color: #fecaca;
}

.section-title {
  margin: 18px 0 4px 0;
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.section-title:first-child {
  margin-top: 0;
}

.form-block {
  padding: 14px 0;
}

.label-block {
  display: flex;
  flex-direction: column;
  gap: 3px;
  margin-bottom: 8px;
}

.label-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.label-sub {
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.5;
}

.textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
  resize: vertical;
  font-family: inherit;
  color: #1f2937;
}

.textarea:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15);
}

.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 14px 0;
  border-bottom: 1px solid #f3f4f6;
}

.form-row:last-of-type {
  border-bottom: none;
}

.field-wrap {
  flex-shrink: 0;
}

.field {
  width: 300px;
  max-width: 48%;
  padding: 8px 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
}

.field.narrow {
  width: 120px;
}

.field:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15);
}

.meta {
  margin: 12px 0 0 0;
  font-size: 12px;
  color: #94a3b8;
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: 18px;
}

.tip {
  margin: 16px 0 0 0;
  font-size: 13px;
  color: #b91c1c;
}

.tip.ok {
  color: #047857;
}

/* 测试面板 */
.test-card .lead {
  margin-bottom: 14px;
}

.test-toolbar {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  padding-bottom: 14px;
  border-bottom: 1px solid #f3f4f6;
}

.mode-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #334155;
  cursor: pointer;
}

.message-list {
  max-height: 420px;
  overflow-y: auto;
  padding: 16px 2px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.welcome {
  font-size: 13px;
  color: #94a3b8;
  text-align: center;
  padding: 24px 0;
}

.message-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.message-row.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: #6366f1;
  flex-shrink: 0;
}

.message-row.user .message-avatar {
  background: #10b981;
}

.message-body {
  max-width: 78%;
  min-width: 0;
}

.message-content {
  margin: 0;
  padding: 11px 14px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #e5e7eb;
  color: #1f2937;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-row.user .message-content {
  color: #fff;
  border-color: transparent;
  background: #10b981;
}

.pending-text {
  color: #94a3b8;
  font-style: italic;
}

.think-box {
  margin-bottom: 8px;
  border: 1px solid #e5e7eb;
  border-left: 3px solid #6366f1;
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

.think-box-body {
  padding: 0 10px 8px;
  color: #64748b;
  font-size: 12.5px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
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
  border: 1px solid #e5e7eb;
  text-decoration: none;
}

.book-cover {
  width: 48px;
  height: 64px;
  flex-shrink: 0;
  border-radius: 8px;
  object-fit: cover;
  background: #f3f4f6;
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
  color: #1f2937;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.book-reason {
  margin: 0;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}

.test-input {
  margin-top: 14px;
  border-top: 1px solid #f3f4f6;
  padding-top: 14px;
}

.test-input .textarea:disabled {
  cursor: not-allowed;
  background: #f5f6f8;
}

.test-input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

@media (max-width: 640px) {
  .card {
    padding: 16px;
  }

  .form-row {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .field {
    width: 100%;
    max-width: none;
  }

  .field.narrow {
    width: 100%;
  }
}
</style>
