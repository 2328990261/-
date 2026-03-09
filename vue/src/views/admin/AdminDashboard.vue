<template>
  <div>
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon" style="background: #667eea;">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path></svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalBooks }}</div>
          <div class="stat-label">书籍总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #f5576c;">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalUsers }}</div>
          <div class="stat-label">用户总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #4facfe;">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalViews }}</div>
          <div class="stat-label">总阅读量</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #fa709a;">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalCollections }}</div>
          <div class="stat-label">总收藏数</div>
        </div>
      </div>
    </div>
    <div class="welcome-section">
      <h2>欢迎使用后台管理系统</h2>
      <p>请从左侧菜单选择要管理的功能</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const stats = ref({ totalBooks: 0, totalUsers: 0, totalViews: 0, totalCollections: 0 })

onMounted(async () => {
  try {
    const res = await axios.get('http://localhost:8081/api/admin/stats/overview')
    if (res.data?.code === 200 && res.data?.data) {
      stats.value = res.data.data
    }
  } catch (e) {
    console.error('加载统计失败', e)
  }
})
</script>

<style scoped>
.stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 24px; margin-bottom: 40px; }
.stat-card { background: #fff; border: 1px solid #e5e7eb; border-radius: 12px; padding: 24px; display: flex; align-items: center; gap: 16px; }
.stat-icon { width: 56px; height: 56px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: white; flex-shrink: 0; }
.stat-value { font-size: 28px; font-weight: 700; color: #111827; margin-bottom: 4px; }
.stat-label { font-size: 14px; color: #6b7280; }
.welcome-section { background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 12px; padding: 48px; text-align: center; }
.welcome-section h2 { font-size: 24px; font-weight: 600; color: #111827; margin: 0 0 12px 0; }
.welcome-section p { font-size: 16px; color: #6b7280; margin: 0; }
</style>
