<template>
  <div class="email-config">
    <h2 class="page-heading">邮箱配置（SMTP）</h2>

    <p class="lead">
      用于「邮箱验证码登录 / 注册」时的验证码邮件发送。默认已按
      <strong>QQ 邮箱</strong>模板（<code>smtp.qq.com:465</code>）预填，只需填写
      <strong>发件邮箱</strong>与 <strong>SMTP 授权码</strong>并打开「启用」开关即可。
    </p>

    <section v-if="loadError" class="card error-card">
      {{ loadError }}
    </section>

    <section v-else class="card">
      <div class="form-row">
        <label class="label-block">
          <span class="label-title">启用邮箱服务</span>
          <span class="label-sub">关闭后发送验证码将按「开发模式」返回，不真正发信</span>
        </label>
        <label class="switch">
          <input v-model="cfg.enabled" type="checkbox" :true-value="1" :false-value="0" />
          <span class="switch-slider"></span>
        </label>
      </div>

      <div class="form-row">
        <label class="label-block">
          <span class="label-title">SMTP 服务器地址</span>
          <span class="label-sub">QQ 邮箱：smtp.qq.com；163：smtp.163.com</span>
        </label>
        <input v-model="cfg.smtpHost" type="text" class="field" placeholder="smtp.qq.com" />
      </div>

      <div class="form-row">
        <label class="label-block">
          <span class="label-title">SMTP 端口</span>
          <span class="label-sub">QQ 邮箱 465（SSL）；587（STARTTLS）</span>
        </label>
        <input v-model.number="cfg.smtpPort" type="number" min="1" max="65535" class="field narrow" />
      </div>

      <div class="form-row">
        <label class="label-block">
          <span class="label-title">发件邮箱</span>
          <span class="label-sub">例如 yourname@qq.com，须与授权码对应</span>
        </label>
        <input v-model="cfg.username" type="email" class="field" placeholder="yourname@qq.com" />
      </div>

      <div class="form-row">
        <label class="label-block">
          <span class="label-title">SMTP 授权码</span>
          <span class="label-sub">QQ 邮箱：设置 → 账号 → 开启 SMTP 服务后生成授权码（不是登录密码）</span>
        </label>
        <input v-model="cfg.authCode" type="password" class="field" placeholder="16 位授权码" autocomplete="new-password" />
      </div>

      <div class="form-row">
        <label class="label-block">
          <span class="label-title">SSL 加密</span>
          <span class="label-sub">QQ 邮箱 465 端口需开启</span>
        </label>
        <label class="switch">
          <input v-model="cfg.sslEnabled" type="checkbox" :true-value="1" :false-value="0" />
          <span class="switch-slider"></span>
        </label>
      </div>

      <p v-if="cfg.updatedAt" class="meta">最近更新：{{ cfg.updatedAt }}</p>

      <div class="actions">
        <button type="button" class="admin-btn admin-btn--primary" :disabled="saving" @click="save">
          {{ saving ? '保存中...' : '保存配置' }}
        </button>
        <button type="button" class="admin-btn admin-btn--secondary" @click="load">重新加载</button>
      </div>

      <div class="test-section">
        <h3 class="test-title">发送测试邮件</h3>
        <div class="test-row">
          <input v-model="testTo" type="email" class="field" placeholder="收件邮箱" />
          <button type="button" class="admin-btn admin-btn--success" :disabled="testing" @click="sendTest">
            {{ testing ? '发送中...' : '发送测试邮件' }}
          </button>
        </div>
        <p class="meta">保存配置并开启「启用」后，可用测试邮件验证授权码是否正确。</p>
      </div>

      <p v-if="tip" class="tip" :class="{ ok: tipOk }">{{ tip }}</p>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import adminHttp from '@/utils/adminHttp'

const defaultCfg = () => ({
  id: 1,
  smtpHost: 'smtp.qq.com',
  smtpPort: 465,
  username: '',
  authCode: '',
  sslEnabled: 1,
  enabled: 0,
  updatedAt: null
})

const cfg = ref(defaultCfg())
const loadError = ref('')
const saving = ref(false)
const testing = ref(false)
const testTo = ref('')
const tip = ref('')
const tipOk = ref(false)

const mapFromApi = (d) => {
  if (!d || typeof d !== 'object') return defaultCfg()
  return {
    id: d.id != null ? Number(d.id) : 1,
    smtpHost: d.smtpHost || 'smtp.qq.com',
    smtpPort: d.smtpPort != null ? Number(d.smtpPort) : 465,
    username: d.username || '',
    authCode: d.authCode || '',
    sslEnabled: d.sslEnabled != null ? Number(d.sslEnabled) : 1,
    enabled: d.enabled != null ? Number(d.enabled) : 0,
    updatedAt: d.updatedAt || null
  }
}

const load = async () => {
  loadError.value = ''
  try {
    const res = await adminHttp.get('/admin/email-config')
    if (res.data?.code === 200) {
      cfg.value = mapFromApi(res.data.data)
    } else {
      loadError.value = res.data?.msg || '加载失败'
    }
  } catch (e) {
    console.error(e)
    loadError.value = '无法连接服务或加载失败。请确认后端已执行 email_config 建表脚本。'
  }
}

const save = async () => {
  tip.value = ''
  saving.value = true
  try {
    const res = await adminHttp.put('/admin/email-config', cfg.value)
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
    tip.value = e?.message || '保存失败'
  } finally {
    saving.value = false
  }
}

const sendTest = async () => {
  tip.value = ''
  if (!testTo.value || !/^[\w.+-]+@[\w-]+(\.[\w-]+)+$/.test(testTo.value.trim())) {
    tipOk.value = false
    tip.value = '请输入正确的收件邮箱'
    return
  }
  testing.value = true
  try {
    const res = await adminHttp.post('/admin/email-config/test', { to: testTo.value.trim() })
    if (res.data?.code === 200) {
      tipOk.value = true
      tip.value = res.data?.msg || '测试邮件已发送'
    } else {
      tipOk.value = false
      tip.value = res.data?.msg || '发送失败'
    }
  } catch (e) {
    tipOk.value = false
    tip.value = e?.message || '发送失败'
  } finally {
    testing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.email-config {
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
  max-width: 760px;
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
  max-width: 760px;
}

.error-card {
  color: #b91c1c;
  background: #fef2f2;
  border-color: #fecaca;
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

.label-block {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
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

.field {
  width: 260px;
  max-width: 45%;
  padding: 8px 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
  flex-shrink: 0;
}

.field.narrow {
  width: 120px;
}

.field:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15);
}

.switch {
  position: relative;
  display: inline-flex;
  flex-shrink: 0;
  cursor: pointer;
}

.switch input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.switch-slider {
  width: 44px;
  height: 24px;
  border-radius: 24px;
  background: #d1d5db;
  position: relative;
  transition: background 0.2s;
}

.switch-slider::after {
  content: '';
  position: absolute;
  top: 3px;
  left: 3px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
  transition: transform 0.2s;
}

.switch input:checked + .switch-slider {
  background: #10b981;
}

.switch input:checked + .switch-slider::after {
  transform: translateX(20px);
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

.test-section {
  margin-top: 26px;
  padding-top: 18px;
  border-top: 1px dashed #e5e7eb;
}

.test-title {
  margin: 0 0 12px 0;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.test-row {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.test-row .field {
  flex: 1;
  max-width: 320px;
}

.tip {
  margin: 16px 0 0 0;
  font-size: 13px;
  color: #b91c1c;
}

.tip.ok {
  color: #047857;
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

  .switch {
    align-self: flex-start;
  }
}
</style>
