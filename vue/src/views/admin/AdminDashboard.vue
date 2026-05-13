<template>
  <div class="admin-dashboard">
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon stat-icon--books">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path></svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalBooks }}</div>
          <div class="stat-label">书籍总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon--users">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalUsers }}</div>
          <div class="stat-label">用户总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon--views">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalViews }}</div>
          <div class="stat-label">总阅读量</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon--star">
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

    <TagSiteHeatDashboard
      :rows="tagHeat.top5 || []"
      :subtitle="heatSubtitle"
      :description="tagHeat.description"
      :last-computed-at="tagHeat.lastComputedAt"
      :error="tagHeatError"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import adminHttp from '@/utils/adminHttp'
import TagSiteHeatDashboard from '@/components/admin/TagSiteHeatDashboard.vue'

const stats = ref({ totalBooks: 0, totalUsers: 0, totalViews: 0, totalCollections: 0 })
const tagHeat = ref({ top5: [], lastComputedAt: 0, cacheTtlSeconds: 120, description: '' })
const tagHeatError = ref('')

const heatSubtitle = computed(() => {
  const ttl = tagHeat.value.cacheTtlSeconds || 120
  return `以下为全站热度前 5 名标签的实时「热度」与映射后的推荐权重；数据每约 ${ttl} 秒与后端缓存同步。`
})

onMounted(async () => {
  try {
    const res = await adminHttp.get('/admin/stats/overview')
    if (res.data?.code === 200 && res.data?.data) {
      stats.value = res.data.data
    }
  } catch (e) {
    console.error('加载统计失败', e)
  }
  try {
    const res = await adminHttp.get('/admin/stats/tag-site-heat')
    if (res.data?.code === 200 && res.data?.data) {
      tagHeat.value = res.data.data
      tagHeatError.value = ''
    } else {
      tagHeatError.value = res.data?.msg || '加载标签走向失败'
    }
  } catch (e) {
    tagHeatError.value = '加载标签走向失败（请确认已登录管理员且后端可访问）'
    console.error(e)
  }
})
</script>

<style scoped>
.admin-dashboard {
  max-width: 1200px;
  margin: 0 auto;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: clamp(16px, 2vw, 24px);
  margin-bottom: clamp(20px, 3vw, 28px);
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: clamp(20px, 2.5vw, 26px);
  background: #fff;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 10px 40px rgba(99, 102, 241, 0.07), 0 2px 8px rgba(15, 23, 42, 0.04);
  transition: box-shadow 0.25s ease, transform 0.2s ease;
}

.stat-card:hover {
  box-shadow: 0 16px 48px rgba(99, 102, 241, 0.12), 0 4px 12px rgba(15, 23, 42, 0.06);
  transform: translateY(-2px);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.15);
}

.stat-icon--books {
  background: linear-gradient(135deg, #6a11cb 0%, #2575fc 100%);
}

.stat-icon--users {
  background: linear-gradient(135deg, #7c3aed 0%, #c4b5fd 100%);
}

.stat-icon--views {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon--star {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-value {
  font-size: clamp(26px, 4vw, 32px);
  font-weight: 800;
  color: #1e293b;
  letter-spacing: -0.02em;
  line-height: 1.15;
  margin-bottom: 6px;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.welcome-section {
  position: relative;
  background: #fff;
  border-radius: 16px;
  padding: clamp(40px, 6vw, 56px) clamp(24px, 4vw, 48px);
  text-align: center;
  margin-bottom: clamp(20px, 3vw, 28px);
  border: 1px solid rgba(226, 232, 240, 0.95);
  box-shadow: 0 10px 40px rgba(99, 102, 241, 0.06), 0 2px 8px rgba(15, 23, 42, 0.04);
  overflow: hidden;
}

.welcome-section::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 3px;
  background: linear-gradient(90deg, #2575fc 0%, #8b5cf6 45%, #f093fb 85%, #f5576c 100%);
}

.welcome-section h2 {
  font-size: clamp(22px, 3.2vw, 28px);
  font-weight: 800;
  color: #312e81;
  margin: 0 0 14px 0;
  letter-spacing: -0.02em;
}

.welcome-section p {
  font-size: 16px;
  color: #64748b;
  margin: 0;
  line-height: 1.65;
  font-weight: 400;
}
</style>
