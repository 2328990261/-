<template>
  <nav class="navbar" :class="{ 'eye-protection-mode': eyeProtection }">
    <div class="navbar-inner">
      <h1 class="logo">轻小说</h1>

      <!-- 桌面端：搜索 + 主导航 -->
      <div class="navbar-expand">
        <div class="search-bar">
          <input type="text" placeholder="搜索书名/作者/ID" class="search-input" v-model="searchKeyword" @keyup.enter="handleSearch">
          <button type="button" class="search-btn" @click="handleSearchClick">
            <img src="../assets/搜索.png" alt="搜索" />
          </button>
        </div>
        <div class="nav-menu">
          <router-link to="/" class="nav-link" @click="closeMobileMenu">首页</router-link>
          <router-link to="/library" class="nav-link" @click="closeMobileMenu">书库</router-link>
          <router-link to="/recommend" class="nav-link" @click="closeMobileMenu">推荐</router-link>
        </div>
      </div>

      <!-- 桌面端：用户信息 -->
      <div class="navbar-right">
        <span v-if="isLoggedIn" class="user-info">您好, {{ username }}</span>
        <span v-else class="user-info">您好, 游客</span>
        <router-link v-if="!isLoggedIn" to="/login" class="nav-link">登录</router-link>
        <router-link v-if="!isLoggedIn" to="/register" class="nav-link">注册</router-link>
        <span v-if="isLoggedIn" class="divider">|</span>
        <router-link v-if="isLoggedIn" to="/user/center" class="nav-link">个人中心</router-link>
        <span v-if="isLoggedIn" class="divider">|</span>
        <router-link v-if="isLoggedIn && isAdmin" to="/admin" class="nav-link admin-btn">后台管理</router-link>
        <span v-if="isLoggedIn && isAdmin" class="divider">|</span>
        <button v-if="isLoggedIn" type="button" @click="handleLogout" class="nav-link logout-btn">退出登录</button>
      </div>

      <!-- 移动端：菜单按钮 -->
      <button
        type="button"
        class="navbar-burger"
        :aria-expanded="mobileMenuOpen"
        aria-controls="navbar-mobile-drawer"
        aria-label="打开或关闭菜单"
        @click="toggleMobileMenu"
      >
        <span class="navbar-burger-line" :class="{ open: mobileMenuOpen }"></span>
        <span class="navbar-burger-line" :class="{ open: mobileMenuOpen }"></span>
        <span class="navbar-burger-line" :class="{ open: mobileMenuOpen }"></span>
      </button>
    </div>

    <!-- 移动端抽屉 -->
    <Teleport to="body">
      <div
        class="mobile-drawer-backdrop"
        :class="{ visible: mobileMenuOpen }"
        :aria-hidden="!mobileMenuOpen"
        @click="closeMobileMenu"
      ></div>
      <aside
        id="navbar-mobile-drawer"
        class="mobile-drawer"
        :class="{ visible: mobileMenuOpen, 'eye-protection-mode': eyeProtection }"
      >
        <div class="mobile-drawer-body" @click.stop>
          <div class="mobile-drawer-search search-bar search-bar-mobile">
            <input
              type="text"
              placeholder="搜索书名/作者/ID"
              class="search-input"
              v-model="searchKeyword"
              @keyup.enter="handleMobileSearchEnter"
            >
            <button type="button" class="search-btn" @click="handleMobileSearchEnter">
              <img src="../assets/搜索.png" alt="搜索" />
            </button>
          </div>

          <div class="mobile-drawer-nav">
            <router-link to="/" class="mobile-nav-link nav-link" @click="closeMobileMenu">首页</router-link>
            <router-link to="/library" class="mobile-nav-link nav-link" @click="closeMobileMenu">书库</router-link>
            <router-link to="/recommend" class="mobile-nav-link nav-link" @click="closeMobileMenu">推荐</router-link>
          </div>

          <div class="mobile-drawer-divider"></div>

          <div class="mobile-drawer-user">
            <p v-if="isLoggedIn" class="mobile-user-row">您好, {{ username }}</p>
            <p v-else class="mobile-user-row">您好, 游客</p>

            <template v-if="!isLoggedIn">
              <router-link to="/login" class="mobile-nav-link nav-link" @click="closeMobileMenu">登录</router-link>
              <router-link to="/register" class="mobile-nav-link nav-link" @click="closeMobileMenu">注册</router-link>
            </template>

            <template v-else>
              <router-link to="/user/center" class="mobile-nav-link nav-link" @click="closeMobileMenu">个人中心</router-link>
              <router-link v-if="isAdmin" to="/admin" class="mobile-nav-link nav-link admin-btn mobile-admin-btn" @click="closeMobileMenu">
                后台管理
              </router-link>
              <button type="button" class="mobile-nav-link nav-link logout-btn mobile-logout" @click="onMobileLogout">
                退出登录
              </button>
            </template>
          </div>
        </div>
      </aside>
    </Teleport>
  </nav>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { clearReadingCachesForLogout } from '@/utils/userBrowserCache'

const router = useRouter()
const route = useRoute()
const username = ref('')
const searchKeyword = ref('')
const mobileMenuOpen = ref(false)

const BREAKPOINT_MOBILE = 992
let mq = null

defineProps({
  eyeProtection: {
    type: Boolean,
    default: false
  }
})

const isLoggedIn = computed(() => {
  return !!localStorage.getItem('token')
})

const isAdmin = computed(() => {
  const userInfoStr = localStorage.getItem('userInfo')
  if (userInfoStr && userInfoStr !== 'undefined') {
    try {
      const parsedUserInfo = JSON.parse(userInfoStr)
      return parsedUserInfo.isAdmin === 1
    } catch (e) {
      console.error('解析用户信息失败：', e)
      localStorage.removeItem('userInfo')
      return false
    }
  }
  return false
})

function toggleMobileMenu() {
  mobileMenuOpen.value = !mobileMenuOpen.value
}

function closeMobileMenu() {
  mobileMenuOpen.value = false
}

function unlockBodyScroll() {
  document.body.style.overflow = ''
}

function syncBodyScroll() {
  if (typeof document === 'undefined') return
  document.body.style.overflow = mobileMenuOpen.value ? 'hidden' : ''
}

/** 宽度回到桌面区间时收起抽屉（matchMedia：`max-width < 992px` 为移动端） */
function maybeCloseMenuAfterResize() {
  if (mq && !mq.matches) {
    mobileMenuOpen.value = false
  }
}

watch(mobileMenuOpen, syncBodyScroll)

watch(() => route.fullPath, closeMobileMenu)

onMounted(() => {
  mq = typeof window !== 'undefined' ? window.matchMedia(`(max-width: ${BREAKPOINT_MOBILE - 1}px)`) : null
  mq?.addEventListener('change', maybeCloseMenuAfterResize)

  const userInfoStr = localStorage.getItem('userInfo')
  if (userInfoStr && userInfoStr !== 'undefined') {
    try {
      const parsedUserInfo = JSON.parse(userInfoStr)
      username.value = parsedUserInfo.username
    } catch (e) {
      console.error('解析用户信息失败：', e)
      localStorage.removeItem('userInfo')
      username.value = ''
    }
  }
})

onBeforeUnmount(() => {
  mq?.removeEventListener('change', maybeCloseMenuAfterResize)
  unlockBodyScroll()
})

const handleSearch = (e) => {
  if (e.key === 'Enter') doSearch()
}

const handleSearchClick = () => doSearch()

const handleMobileSearchEnter = () => {
  doSearch()
  closeMobileMenu()
}

function doSearch() {
  const q = searchKeyword.value && searchKeyword.value.trim()
  if (!q) return
  router.push({ path: '/library', query: { keyword: q } })
}

const handleLogout = () => {
  performLogoutAndRedirect()
}

function onMobileLogout() {
  closeMobileMenu()
  performLogoutAndRedirect()
}

function performLogoutAndRedirect() {
  clearReadingCachesForLogout()
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  router.push('/login')
}
</script>

<style scoped>
.navbar {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  min-height: 60px;
  background: linear-gradient(135deg, #ffffff 0%, #ffe5f0 50%, #ffd6e8 100%);
  box-shadow: 0px 4px 8px rgba(255, 214, 232, 0.2);
  padding: 0 24px;
  color: #333;
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 999;
}

.navbar-inner {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  min-height: 60px;
}

.navbar-expand {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  min-width: 0;
}

.logo {
  font-size: clamp(18px, 4vw, 24px);
  font-weight: 900;
  margin: 0;
  color: #ff1493;
  font-family: 'Microsoft YaHei', 'PingFang SC', 'Hiragino Sans GB', sans-serif;
  letter-spacing: 1px;
  text-shadow: 0 2px 4px rgba(255, 20, 147, 0.2);
  flex-shrink: 0;
  white-space: nowrap;
}

.search-bar {
  display: flex;
  align-items: center;
  background-color: #fff;
  border-radius: 24px;
  padding: 4px 12px;
  height: 36px;
  width: 220px;
  max-width: 100%;
  flex-shrink: 0;
}

.search-bar-mobile {
  width: 100%;
  height: 40px;
}

.search-input {
  border: none;
  outline: none;
  background: transparent;
  flex: 1;
  min-width: 0;
  font-size: 14px;
  color: #333;
}

.search-input::placeholder {
  color: #999;
}

.search-btn {
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0;
  margin-left: 4px;
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-btn img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.nav-menu {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  flex-shrink: 0;
  flex-wrap: nowrap;
}

.nav-link {
  color: #555;
  text-decoration: none;
  font-size: 16px;
  font-weight: 600;
  font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
  transition: all 0.3s;
  white-space: nowrap;
}

.nav-link:hover,
.nav-link.active {
  color: #ff1493;
  border-bottom: 2px solid #ff1493;
  padding-bottom: 2px;
  transform: translateY(-1px);
}

.user-info {
  font-size: 14px;
  color: #555;
  font-weight: 500;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.divider {
  color: #ccc;
  flex-shrink: 0;
}

.navbar-burger {
  display: none;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 5px;
  width: 44px;
  height: 44px;
  margin-left: auto;
  padding: 0;
  border: none;
  border-radius: 10px;
  background: transparent;
  cursor: pointer;
  flex-shrink: 0;
}

.navbar-burger:hover {
  background: rgba(255, 105, 180, 0.12);
}

.navbar-burger-line {
  display: block;
  width: 22px;
  height: 2px;
  background: #ff1493;
  border-radius: 1px;
  transition:
    transform 0.22s ease,
    opacity 0.22s ease;
}

.navbar-burger-line:nth-child(1).open {
  transform: translateY(7px) rotate(45deg);
}

.navbar-burger-line:nth-child(2).open {
  opacity: 0;
  transform: scaleX(0);
}

.navbar-burger-line:nth-child(3).open {
  transform: translateY(-7px) rotate(-45deg);
}

.mobile-drawer-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1098;
  background: rgba(0, 0, 0, 0.38);
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
  transition:
    opacity 0.26s ease,
    visibility 0.26s ease;
}

.mobile-drawer-backdrop.visible {
  opacity: 1;
  visibility: visible;
  pointer-events: auto;
}

.mobile-drawer {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 1099;
  width: min(300px, 88vw);
  max-width: 100%;
  background: linear-gradient(180deg, #fff8fc 0%, #ffeaf3 55%, #ffd6e8 100%);
  box-shadow: -6px 0 24px rgba(255, 105, 180, 0.18);
  transform: translateX(100%);
  transition: transform 0.28s ease;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.mobile-drawer.visible {
  transform: translateX(0);
}

.mobile-drawer-body {
  padding: 72px 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.mobile-drawer-nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.mobile-nav-link {
  display: block;
  padding: 12px 4px;
  border-radius: 10px;
  font-size: 16px;
  border-bottom: none !important;
  transform: none !important;
}

.mobile-nav-link.nav-link:hover,
.mobile-nav-link.nav-link.router-link-active {
  color: #ff1493;
  background: rgba(255, 105, 180, 0.1);
}

.mobile-drawer-divider {
  height: 1px;
  background: rgba(255, 20, 147, 0.15);
}

.mobile-drawer-user {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.mobile-user-row {
  margin: 0 0 8px;
  font-size: 15px;
  color: #555;
  word-break: break-all;
}

.mobile-admin-btn.mobile-nav-link {
  text-align: center;
  border: 1px solid #ffd700 !important;
  padding: 10px !important;
}

.mobile-logout {
  margin-top: 4px;
  text-align: center;
  width: 100%;
}

@media (max-width: 991px) {
  .navbar {
    padding: 0 12px;
  }

  .navbar-expand,
  .navbar-right {
    display: none;
  }

  .navbar-burger {
    display: flex;
  }
}

.logout-btn {
  background: transparent;
  color: #555;
  border: 1px solid #ffb3d9;
  padding: 5px 14px;
  border-radius: 16px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
  transition: all 0.3s;
}

.logout-btn:hover {
  background: #ffb3d9;
  color: #fff;
  border-color: #ffb3d9;
}

.admin-btn {
  background: transparent;
  color: #555;
  border: 1px solid #ffd700;
  padding: 5px 14px;
  border-radius: 16px;
  font-weight: 500;
  transition: all 0.3s;
}

.admin-btn:hover {
  background: #ffd700;
  color: #fff;
  border-color: #ffd700;
  border-bottom: 1px solid #ffd700;
}

.navbar.eye-protection-mode {
  background: #121212;
  color: #e0e0e0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5);
}

.navbar.eye-protection-mode .navbar-burger-line {
  background: #ffb86c;
}

.navbar.eye-protection-mode .navbar-burger:hover {
  background: rgba(255, 255, 255, 0.08);
}

.navbar.eye-protection-mode .logo {
  color: #e0e0e0;
}

.navbar.eye-protection-mode .search-bar {
  background-color: #1e1e1e;
  border: 1px solid #333;
}

.navbar.eye-protection-mode .search-input {
  color: #e0e0e0;
}

.navbar.eye-protection-mode .search-input::placeholder {
  color: #888;
}

.navbar.eye-protection-mode .nav-link {
  color: #e0e0e0;
}

.navbar.eye-protection-mode .nav-link:hover,
.navbar.eye-protection-mode .nav-link.active {
  color: #fff;
  border-bottom-color: #fff;
}

.navbar.eye-protection-mode .user-info {
  color: rgba(224, 224, 224, 0.9);
}

.navbar.eye-protection-mode .divider {
  color: rgba(224, 224, 224, 0.7);
}

.navbar.eye-protection-mode .search-btn {
  color: #ff8c00;
}

.mobile-drawer.eye-protection-mode {
  background: #1e1e1e;
  box-shadow: -6px 0 28px rgba(0, 0, 0, 0.6);
}

.mobile-drawer.eye-protection-mode .mobile-drawer-divider {
  background: rgba(255, 255, 255, 0.12);
}

.mobile-drawer.eye-protection-mode .mobile-user-row {
  color: rgba(224, 224, 224, 0.9);
}

.mobile-drawer.eye-protection-mode .mobile-nav-link.nav-link {
  color: #e0e0e0;
}

.mobile-drawer.eye-protection-mode .mobile-nav-link.nav-link:hover,
.mobile-drawer.eye-protection-mode .mobile-nav-link.router-link-active {
  color: #ffb86c;
  background: rgba(255, 255, 255, 0.06);
}

@media (min-width: 992px) and (max-width: 1180px) {
  .search-bar {
    width: clamp(140px, 18vw, 220px);
  }

  .nav-menu {
    gap: 12px;
  }

  .nav-link {
    font-size: 15px;
  }

  .user-info {
    max-width: 100px;
  }
}
</style>