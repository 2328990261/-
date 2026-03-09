<template>
  <div class="admin-container">
    <!-- 顶部标题栏 -->
    <div class="admin-header">
      <div class="header-content">
        <button class="back-btn" @click="goBack">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 12H5M12 19l-7-7 7-7"/>
          </svg>
          返回
        </button>
        <h1 class="page-title">后台管理</h1>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="admin-main">
      <!-- 左侧管理功能列表 -->
      <div class="sidebar">
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
            <div class="menu-icon"><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="12" y1="18" x2="12" y2="12"></line><line x1="9" y1="15" x2="15" y2="15"></line></svg></div>
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
            <span class="menu-text">系统设置（标签）</span>
            <svg class="menu-arrow" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"></polyline></svg>
          </li>
        </ul>
      </div>

      <div class="content-wrapper">
        <!-- 标签页栏 -->
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

        <!-- 内容区域 -->
        <div class="content-area">
          <router-view />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const tabs = ref([])
const activeTab = ref('')

const goBack = () => router.push('/')

const openTab = (name, title, path) => {
  // 检查标签是否已存在
  const existingTab = tabs.value.find(t => t.name === name)
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
  const index = tabs.value.findIndex(t => t.name === name)
  if (index === -1) return
  
  tabs.value.splice(index, 1)
  
  // 如果关闭的是当前标签，切换到其他标签
  if (activeTab.value === name && tabs.value.length > 0) {
    const newTab = tabs.value[Math.max(0, index - 1)]
    switchTab(newTab)
  } else if (tabs.value.length === 0) {
    router.push('/admin/dashboard')
  }
}

const isTabActive = (name) => {
  return route.name === name
}

// 监听路由变化，同步激活的标签并自动打开标签页
watch(() => route.name, (newName) => {
  if (newName && newName.startsWith('admin') && newName !== 'admin') {
    activeTab.value = newName
    
    // 自动打开标签页（如果不存在）
    const existingTab = tabs.value.find(t => t.name === newName)
    if (!existingTab) {
      // 根据路由名称自动添加标签
      const tabConfig = {
        'adminDashboard': { title: '概览', path: '/admin/dashboard' },
        'adminPublish': { title: '书籍上架', path: '/admin/publish' },
        'adminBooks': { title: '书籍管理', path: '/admin/books' },
        'adminUsers': { title: '用户管理', path: '/admin/users' },
        'adminRecommend': { title: '推荐配置', path: '/admin/recommend' },
        'adminConfig': { title: '配置管理', path: '/admin/config' },
        'AdminBannerManage': { title: '轮播图管理', path: '/admin/config/banner' },
        'adminSettings': { title: '系统设置', path: '/admin/settings' }
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
}, { immediate: true })
</script>

<style scoped>
.admin-container {
  min-height: 100vh;
  background-color: #ffffff;
}

/* 顶部标题栏 */
.admin-header {
  background-color: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  padding: 20px 40px;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background-color: #f3f4f6;
  color: #374151;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.back-btn:hover {
  background-color: #e5e7eb;
  border-color: #9ca3af;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

/* 主内容区域 */
.admin-main {
  display: flex;
  min-height: calc(100vh - 81px);
}

/* 左侧边栏 */
.sidebar {
  width: 280px;
  background-color: #f9fafb;
  border-right: 1px solid #e5e7eb;
  padding: 24px 0;
}

.sidebar-header {
  padding: 0 24px 16px;
  border-bottom: 1px solid #e5e7eb;
}

.sidebar-header h2 {
  font-size: 16px;
  font-weight: 600;
  color: #6b7280;
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.menu-list {
  list-style: none;
  padding: 16px 0 0;
  margin: 0;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 24px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.menu-item:not(.disabled):hover {
  background-color: #f3f4f6;
}

.menu-item.active {
  background-color: #eff6ff;
  border-left: 3px solid #3b82f6;
}

.menu-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.menu-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
}

.menu-item.active .menu-icon {
  color: #3b82f6;
}

.menu-text {
  flex: 1;
  font-size: 15px;
  font-weight: 500;
  color: #374151;
}

.menu-item.active .menu-text {
  color: #3b82f6;
}

.menu-arrow {
  color: #9ca3af;
}

.badge {
  font-size: 11px;
  padding: 2px 8px;
  background-color: #fef3c7;
  color: #92400e;
  border-radius: 12px;
  font-weight: 500;
}

/* 右侧内容区域 */
.content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tabs-bar {
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  padding: 0 16px;
  display: flex;
  align-items: center;
  min-height: 48px;
}

.tabs-list {
  display: flex;
  gap: 4px;
  overflow-x: auto;
  flex: 1;
}

.tabs-list::-webkit-scrollbar {
  height: 4px;
}

.tabs-list::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 2px;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #f3f4f6;
  border-radius: 6px 6px 0 0;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
  border: 1px solid transparent;
  border-bottom: none;
}

.tab-item:hover {
  background: #e5e7eb;
}

.tab-item.active {
  background: #ffffff;
  border-color: #e5e7eb;
  border-bottom-color: #ffffff;
  position: relative;
  margin-bottom: -1px;
}

.tab-title {
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}

.tab-item.active .tab-title {
  color: #3b82f6;
}

.tab-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 3px;
  color: #9ca3af;
  transition: all 0.2s ease;
}

.tab-close:hover {
  background: #e5e7eb;
  color: #374151;
}

.content-area {
  flex: 1;
  padding: 40px;
  overflow-y: auto;
  background: #ffffff;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .admin-main {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #e5e7eb;
  }
}

@media (max-width: 768px) {
  .admin-header {
    padding: 16px 20px;
  }

  .content-area {
    padding: 24px 20px;
  }
}
</style>
