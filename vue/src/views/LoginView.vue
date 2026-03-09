<template>
  <div id="login">
    <div class="back-home">
      <button @click="goHome">返回首页</button>
    </div>

    <div id="contain">
      <div id="left_card">
        <h1>轻小说</h1>
        <span>ライトノベル</span>
        <div class="welcome-text">へようこそ</div>
      </div>
      <div id="right_card">
        <div class="login-card">
          <h2>欢迎登录</h2>

          <!-- 登录方式切换 -->
          <div class="login-tabs">
            <button
              type="button"
              class="tab"
              :class="{ active: loginType === 'account' }"
              @click="loginType = 'account'; error = ''"
            >
              账号密码
            </button>
            <button
              type="button"
              class="tab"
              :class="{ active: loginType === 'phone' }"
              @click="loginType = 'phone'; error = ''"
            >
              手机验证码
            </button>
          </div>

          <!-- 账号密码登录 -->
          <form v-if="loginType === 'account'" class="login-form" @submit.prevent="submitAccount">
            <input
              v-model="accountForm.username"
              type="text"
              placeholder="请输入账号（3-20位）"
              autocomplete="username"
              maxlength="20"
            />
            <input
              v-model="accountForm.password"
              type="password"
              placeholder="请输入密码（6-20位）"
              autocomplete="current-password"
              maxlength="20"
            />
            <div class="remember">
              <input
                id="psd"
                v-model="rememberPwd"
                type="checkbox"
                class="radio"
                name="remember"
              />
              <label for="psd"></label>
              记住密码
            </div>
          </form>

          <!-- 手机验证码登录 -->
          <form v-else class="login-form" @submit.prevent="submitPhone">
            <input
              v-model="phoneForm.phone"
              type="tel"
              placeholder="请输入手机号"
              autocomplete="tel"
              maxlength="11"
            />
            <div class="code-row">
              <input
                v-model="phoneForm.code"
                type="text"
                placeholder="请输入验证码"
                autocomplete="one-time-code"
                maxlength="6"
                class="code-input"
              />
              <button
                type="button"
                class="send-code-btn"
                :disabled="codeCountdown > 0 || !isPhoneValid"
                @click="sendCode"
              >
                {{ codeCountdown > 0 ? `${codeCountdown}s 后重发` : '获取验证码' }}
              </button>
            </div>
          </form>

          <div class="message">
            <span v-html="error"></span>
          </div>
          <div class="register-link">
            还没有账号？<router-link to="/register">立即注册</router-link>
          </div>
          <div class="btn-group">
            <button
              type="button"
              class="login-btn"
              :disabled="loading"
              @click="loginType === 'account' ? submitAccount() : submitPhone()"
            >
              {{ loading ? '登录中...' : '登录' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { reactive, ref, computed } from 'vue'
import { login, sendCode as apiSendCode, loginByPhone } from '@/api/auth'

const loginType = ref('account')
const accountForm = reactive({ username: '', password: '' })
const phoneForm = reactive({ phone: '', code: '' })
const rememberPwd = ref(false)
const error = ref('')
const loading = ref(false)
const codeCountdown = ref(0)
const router = useRouter()

const isPhoneValid = computed(() => /^1[3-9]\d{9}$/.test(phoneForm.phone))

function goHome() {
  router.push('/')
}

// 账号格式：3-20 位
function validateUsername(s) {
  const t = (s || '').trim()
  if (t.length < 3 || t.length > 20) return '账号长度为 3-20 个字符'
  return ''
}

// 密码格式：6-20 位
function validatePassword(s) {
  if (!s || s.length < 6 || s.length > 20) return '密码长度为 6-20 个字符'
  return ''
}

function submitAccount() {
  error.value = ''
  const uErr = validateUsername(accountForm.username)
  if (uErr) {
    error.value = "<font color='red'>" + uErr + "</font>"
    return
  }
  const pErr = validatePassword(accountForm.password)
  if (pErr) {
    error.value = "<font color='red'>" + pErr + "</font>"
    return
  }
  doAccountLogin()
}

async function doAccountLogin() {
  loading.value = true
  error.value = ''
  try {
    const res = await login(accountForm.username.trim(), accountForm.password)
    if (res.code === 200) {
      const { token, user } = res.data
      localStorage.setItem('token', token)
      localStorage.setItem('userInfo', JSON.stringify(user))
      if (user && user.id != null) localStorage.setItem('userId', String(user.id))
      if (rememberPwd.value) {
        localStorage.setItem('rememberUsername', accountForm.username.trim())
      } else {
        localStorage.removeItem('rememberUsername')
      }
      error.value = "<font color='green'>登录成功！</font>"
      setTimeout(() => router.push('/'), 1000)
    } else {
      error.value = "<font color='red'>" + (res.msg || '登录失败') + "</font>"
    }
  } catch (err) {
    error.value = "<font color='red'>登录失败，请检查网络连接</font>"
  } finally {
    loading.value = false
  }
}

function submitPhone() {
  error.value = ''
  if (!isPhoneValid.value) {
    error.value = "<font color='red'>请输入正确的手机号</font>"
    return
  }
  if (!phoneForm.code || phoneForm.code.trim().length < 4) {
    error.value = "<font color='red'>请输入验证码</font>"
    return
  }
  doPhoneLogin()
}

async function doPhoneLogin() {
  loading.value = true
  error.value = ''
  try {
    const res = await loginByPhone(phoneForm.phone.trim(), phoneForm.code.trim())
    if (res.code === 200) {
      const { token, user } = res.data
      localStorage.setItem('token', token)
      localStorage.setItem('userInfo', JSON.stringify(user))
      if (user && user.id != null) localStorage.setItem('userId', String(user.id))
      error.value = "<font color='green'>登录成功！</font>"
      setTimeout(() => router.push('/'), 1000)
    } else {
      error.value = "<font color='red'>" + (res.msg || '登录失败') + "</font>"
    }
  } catch (err) {
    error.value = "<font color='red'>登录失败，请检查网络连接</font>"
  } finally {
    loading.value = false
  }
}

async function sendCode() {
  if (!isPhoneValid.value) {
    error.value = "<font color='red'>请输入正确的手机号</font>"
    return
  }
  error.value = ''
  try {
    const res = await apiSendCode(phoneForm.phone.trim())
    if (res.code === 200) {
      codeCountdown.value = 60
      const timer = setInterval(() => {
        codeCountdown.value--
        if (codeCountdown.value <= 0) clearInterval(timer)
      }, 1000)
      const devCode = res.data && res.data.devCode
      error.value = devCode
        ? "<font color='green'>验证码已发送。本次验证码：<strong>" + devCode + "</strong>（请填入上方输入框）</font>"
        : "<font color='green'>验证码已发送，请查收</font>"
    } else {
      error.value = "<font color='red'>" + (res.msg || '发送失败') + "</font>"
    }
  } catch (err) {
    error.value = "<font color='red'>发送失败，请检查网络</font>"
  }
}
</script>

<style scoped>
.back-home {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 2;
}
.back-home button {
  padding: 8px 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  background-color: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(5px);
  border-radius: 8px;
  color: white;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s;
}
.back-home button:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

#login {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-image: url('../assets/login-background.png');
  background-size: cover;
  background-position: center center;
  background-repeat: no-repeat;
  background-color: #a7a8bd;
  z-index: 1;
}

#contain {
  width: 900px;
  height: 420px;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-radius: 25px;
  border: 1px solid black;
  background-color: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(5px);
  box-shadow: -5px -5px 10px rgb(39, 65, 65), 5px 5px 20px aqua;
  animation: animate 5s linear infinite;
  display: flex;
  flex-direction: row;
  text-align: center;
  align-items: center;
}

@keyframes animate {
  0% { filter: hue-rotate(0deg); }
  100% { filter: hue-rotate(360deg); }
}

#left_card {
  width: 500px;
  padding: 0 20px;
}
#left_card h1 {
  color: white;
  white-space: nowrap;
  text-shadow: 0 0 5px #000;
  font-size: 3rem;
}
#left_card span {
  font-size: 2rem;
  color: white;
  text-shadow: 0 0 3px #000;
}
#left_card .welcome-text {
  font-size: 3rem;
  color: white;
  text-shadow: 0 0 3px #000;
}

#right_card {
  width: 400px;
  display: flex;
  justify-content: center;
  align-items: center;
}
#right_card .login-card {
  margin: 0 45px;
  border-radius: 25px;
  background-color: rgba(255, 255, 255, 0.1);
  padding: 20px;
  color: #fff;
  width: 100%;
}

#right_card h2 {
  margin-bottom: 12px;
  font-size: 24px;
  text-align: center;
}

.login-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.login-tabs .tab {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid rgba(255,255,255,0.5);
  background: rgba(255,255,255,0.1);
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}
.login-tabs .tab.active {
  background: rgba(207, 38, 38, 0.6);
  border-color: rgba(207, 38, 38, 0.8);
}

.login-form input[type="text"],
.login-form input[type="password"],
.login-form input[type="tel"] {
  width: 100%;
  height: 45px;
  margin-top: 10px;
  border: 1px solid white;
  background-color: rgba(255, 255, 255, 0.5);
  border-radius: 10px;
  font-size: 16px;
  padding-left: 20px;
  outline: none;
  color: #333;
  box-sizing: border-box;
}
.login-form input::placeholder {
  color: #666;
}

.code-row {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}
.code-input {
  flex: 1;
  min-width: 0;
}
.send-code-btn {
  flex-shrink: 0;
  padding: 0 14px;
  height: 45px;
  border-radius: 10px;
  border: 1px solid rgba(255,255,255,0.8);
  background: rgba(255,255,255,0.2);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
}
.send-code-btn:hover:not(:disabled) {
  background: rgba(255,255,255,0.3);
}
.send-code-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.remember {
  float: right;
  margin-top: 10px;
  font-size: 14px;
  color: #fff;
  text-align: right;
}
.remember .radio {
  margin-right: 6px;
  vertical-align: middle;
}

.message {
  margin-top: 14px;
  font-size: 0.9rem;
  text-align: center;
  min-height: 20px;
}

.register-link {
  margin-top: 12px;
  text-align: center;
  font-size: 14px;
  color: #fff;
}
.register-link a {
  color: #409eff;
  text-decoration: none;
  margin-left: 5px;
}
.register-link a:hover {
  text-decoration: underline;
}

.btn-group {
  width: 100%;
  margin-top: 14px;
}
.login-btn {
  width: 100%;
  height: 40px;
  border-radius: 10px;
  background-color: rgba(207, 38, 38, 0.8);
  cursor: pointer;
  border: none;
  color: white;
  font-size: 16px;
  transition: background-color 0.3s;
}
.login-btn:hover:not(:disabled) {
  background-color: rgba(187, 28, 28, 0.8);
}
.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>
