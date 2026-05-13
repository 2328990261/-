<template>
  <div class="admin-container">
    <div class="admin-layout">
      <!-- 左侧：背景图 + 深色遮罩 + 浅色字菜单（参考深色侧栏稿） -->
      <aside class="sidebar" :aria-label="'管理菜单'">
        <div class="sidebar-bg" :style="sidebarBgStyle" />
        <div class="sidebar-scrim" aria-hidden="true" />
        <div class="sidebar-inner">
          <div class="sidebar-header">
            <h2>管理功能</h2>
          </div>
          <ul class="menu-list">
            <li class="menu-item" :class="{ active: isTabActive('adminDashboard') }" @click="openTab('adminDashboard', '概览', '/admin/dashboard')">
              <div class="menu-icon"><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg></div>
              <span class="menu-text">概览</span>
              <svg class="menu-arrow" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"></polyline></svg>
            </li>
            <li class="menu-item" :class="{ active: isTabActive('adminPublish') }" @click="openTab('adminPublish', '书籍上架', '/admin/publish')">
              <div class="menu-icon"><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="12" y1="11" x2="12" y2="17"></line><line x1="9" y1="14" x2="15" y2="14"></line></svg></div>
              <span class="menu-text">书籍上架</span>
              <svg class="menu-arrow" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"></polyline></svg>
            </li>
            <li class="menu-item" :class="{ active: isTabActive('adminBooks') }" @click="openTab('adminBooks', '书籍管理', '/admin/books')">
              <div class="menu-icon"><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg></div>
              <span class="menu-text">书籍管理</span>
              <svg class="menu-arrow" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"></polyline></svg>
            </li>
            <li class="menu-item" :class="{ active: isTabActive('adminUsers') }" @click="openTab('adminUsers', '用户管理', '/admin/users')">
              <div class="menu-icon"><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg></div>
              <span class="menu-text">用户管理</span>
              <svg class="menu-arrow" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"></polyline></svg>
            </li>
            <li class="menu-item" :class="{ active: isTabActive('adminManage') }" @click="openTab('adminManage', '管理员管理', '/admin/admins')">
              <div class="menu-icon"><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg></div>
              <span class="menu-text">管理员管理</span>
              <svg class="menu-arrow" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"></polyline></svg>
            </li>
            <li class="menu-item" :class="{ active: isTabActive('adminRecommend') }" @click="openTab('adminRecommend', '推荐配置', '/admin/recommend')">
              <div class="menu-icon"><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg></div>
              <span class="menu-text">推荐配置</span>
              <svg class="menu-arrow" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"></polyline></svg>
            </li>
            <li class="menu-item" :class="{ active: isTabActive('adminConfig') }" @click="openTab('adminConfig', '配置管理', '/admin/config')">
              <div class="menu-icon"><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><line x1="9" y1="9" x2="15" y2="9"></line><line x1="9" y1="15" x2="15" y2="15"></line></svg></div>
              <span class="menu-text">配置管理</span>
              <svg class="menu-arrow" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"></polyline></svg>
            </li>
            <li class="menu-item" :class="{ active: isTabActive('adminSettings') }" @click="openTab('adminSettings', '系统设置', '/admin/settings')">
              <div class="menu-icon"><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg></div>
              <span class="menu-text">系统设置 (标签)</span>
              <svg class="menu-arrow" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"></polyline></svg>
            </li>
            <li class="menu-item menu-item-home" @click="goHome">
              <div class="menu-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path><polyline points="9 22 9 12 15 12 15 22"></polyline></svg>
              </div>
              <span class="menu-text">回到首页</span>
              <svg class="menu-arrow" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"></polyline></svg>
            </li>
          </ul>
        </div>
      </aside>

      <!-- 右侧：顶栏仅标签页 + 内容 -->
      <div class="admin-main">
        <header class="top-strip">
          <div class="tabs-bar" v-if="tabs.length > 0">
            <div class="tabs-list">
              <div
                v-for="tab in tabs"
                :key="tab.name"
                class="tab-item"
                :class="{ active: activeTab === tab.name }"
                @click="switchTab(tab)"
              >
                <span class="tab-title">{{ tab.title }}</span>
                <span class="tab-close" @click.stop="closeTab(tab.name)">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                  </svg>
                </span>
              </div>
            </div>
          </div>
        </header>

        <div class="content-area">
          <router-view />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import sidebarBgUrl from '@/assets/爱莉.jpg'

const router = useRouter()
const route = useRoute()

const sidebarBgStyle = computed(() => ({
  backgroundImage: `url(${sidebarBgUrl})`
}))

const tabs = ref([])
const activeTab = ref('')

const goHome = () => router.push('/')

const openTab = (name, title, path) => {
  const existingTab = tabs.value.find((t) => t.name === name)
  if (!existingTab) {
    tabs.value.push({ name, title, path })
  }
  activeTab.value = name
  router.push(path)
}

const switchTab = (tab) => {
  activeTab.value = tab.name
  router.push(tab.path)
}

const closeTab = (name) => {
  const index = tabs.value.findIndex((t) => t.name === name)
  if (index === -1) return

  tabs.value.splice(index, 1)

  if (activeTab.value === name && tabs.value.length > 0) {
    const newTab = tabs.value[Math.max(0, index - 1)]
    switchTab(newTab)
  } else if (tabs.value.length === 0) {
    router.push('/admin/dashboard')
  }
}

const isTabActive = (name) => route.name === name

watch(
  () => route.name,
  (newName) => {
    if (newName && newName.startsWith('admin') && newName !== 'admin') {
      activeTab.value = newName

      const existingTab = tabs.value.find((t) => t.name === newName)
      if (!existingTab) {
        const tabConfig = {
          adminDashboard: { title: '概览', path: '/admin/dashboard' },
          adminPublish: { title: '书籍上架', path: '/admin/publish' },
          adminBooks: { title: '书籍管理', path: '/admin/books' },
          adminUsers: { title: '用户管理', path: '/admin/users' },
          adminManage: { title: '管理员管理', path: '/admin/admins' },
          adminRecommend: { title: '推荐配置', path: '/admin/recommend' },
          adminConfig: { title: '配置管理', path: '/admin/config' },
          AdminBannerManage: { title: '轮播图管理', path: '/admin/config/banner' },
          adminSettings: { title: '系统设置', path: '/admin/settings' }
        }

        if (tabConfig[newName]) {
          tabs.value.push({
            name: newName,
            title: tabConfig[newName].title,
            path: tabConfig[newName].path
          })
        }
      }
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.admin-container {
  min-height: 100vh;
  background: #eef0fb;
}

.admin-layout {
  display: flex;
  min-height: 100vh;
  width: 100%;
}

/* ========== 左侧边栏：底图 + 深色遮罩 + 白字（参考深色导航稿） ========== */
.sidebar {
  position: relative;
  flex: 0 0 clamp(220px, 24vw, 300px);
  width: clamp(220px, 24vw, 300px);
  min-height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(15, 23, 42, 0.65);
  box-shadow: 4px 0 24px rgba(15, 23, 42, 0.12);
}

.sidebar-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  background-position: center;
  background-size: cover;
  background-repeat: no-repeat;
  transform: scale(1.02);
}

.sidebar-scrim {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background: linear-gradient(
    180deg,
    rgba(15, 23, 42, 0.78) 0%,
    rgba(49, 46, 129, 0.82) 42%,
    rgba(15, 23, 42, 0.88) 100%
  );
}

.sidebar-inner {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  padding: 22px 0 16px;
}

.sidebar-header {
  padding: 0 20px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.sidebar-header h2 {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  margin: 0;
  letter-spacing: 0.06em;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.35);
}

.menu-list {
  list-style: none;
  padding: 10px 0 0;
  margin: 0;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px 14px 16px;
  cursor: pointer;
  transition: background 0.2s ease;
  flex-shrink: 0;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

.menu-item.active {
  background: rgba(64, 158, 255, 0.32);
  box-shadow: inset 3px 0 0 rgba(96, 165, 250, 0.95);
}

.menu-item-home {
  margin-top: 6px;
  padding-top: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.menu-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.95);
  flex-shrink: 0;
}

.menu-item.active .menu-icon {
  color: #fff;
}

.menu-text {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.94);
  letter-spacing: 0.02em;
}

.menu-item.active .menu-text {
  color: #fff;
}

.menu-arrow {
  color: rgba(255, 255, 255, 0.55);
  flex-shrink: 0;
}

.menu-item:hover .menu-arrow,
.menu-item.active .menu-arrow {
  color: rgba(255, 255, 255, 0.85);
}

/* ========== 右侧主区 ========== */
.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 100vh;
  background: #eef0fb;
}

.top-strip {
  display: flex;
  align-items: stretch;
  gap: 0;
  flex-shrink: 0;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06);
  min-height: 52px;
}

.tabs-bar {
  flex: 1;
  min-width: 0;
  width: 100%;
  display: flex;
  align-items: center;
  padding: 0 14px 0 16px;
  background: #fff;
}

.tabs-list {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  flex: 1;
  align-items: flex-end;
  padding-bottom: 0;
  min-height: 48px;
}

.tabs-list::-webkit-scrollbar {
  height: 4px;
}

.tabs-list::-webkit-scrollbar-thumb {
  background: #c4b5fd;
  border-radius: 2px;
}

.tab-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px 10px 16px;
  margin-top: 8px;
  background: #f3f4f6;
  border-radius: 8px 8px 0 0;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s ease, color 0.2s ease;
  border: 1px solid transparent;
  border-bottom: none;
  flex-shrink: 0;
}

.tab-item:hover {
  background: #ede9fe;
}

.tab-item.active {
  background: #eef0fb;
  border-color: #e5e7eb;
  border-bottom-color: #eef0fb;
  margin-bottom: -1px;
  position: relative;
  z-index: 1;
}

.tab-title {
  font-size: 13px;
  color: #4b5563;
  font-weight: 500;
}

.tab-item.active .tab-title {
  color: #5b21b6;
  font-weight: 600;
}

.tab-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  color: #9ca3af;
  transition: background 0.2s ease, color 0.2s ease;
}

.tab-close:hover {
  background: rgba(91, 33, 182, 0.12);
  color: #5b21b6;
}

.content-area {
  flex: 1;
  overflow: auto;
  padding: clamp(16px, 2.5vw, 32px);
  background: linear-gradient(145deg, #f3f4ff 0%, #faf8ff 38%, #ffffff 100%);
}

@media (max-width: 900px) {
  .admin-layout {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    flex: 0 0 auto;
    min-height: 220px;
    max-height: 42vh;
  }

  .sidebar-bg {
    transform: scale(1.05);
  }

  .top-strip {
    flex-wrap: nowrap;
  }

  .tabs-bar {
    width: 100%;
  }
}
</style>

<style>
@import '@/styles/admin-ui.css';
</style>
