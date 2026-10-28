<template>
  <div class="admin-dashboard">
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon stat-icon--books">
          <el-icon :size="26"><Reading /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalBooks }}</div>
          <div class="stat-label">书籍总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon--users">
          <el-icon :size="26"><User /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalUsers }}</div>
          <div class="stat-label">用户总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon--views">
          <el-icon :size="26"><View /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalViews }}</div>
          <div class="stat-label">总阅读量</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon--star">
          <el-icon :size="26"><Star /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalCollections }}</div>
          <div class="stat-label">总收藏数</div>
        </div>
      </div>
    </div>

    <!-- 全站标签趋势：组件内部自取时序数据，并内置 日/周/月、指标、简略/详细 切换 -->
    <TagSiteHeatDashboard />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import adminHttp from '@/utils/adminHttp'
import TagSiteHeatDashboard from '@/components/admin/TagSiteHeatDashboard.vue'
// 统计卡片图标：使用 Element Plus 标准图标（书籍/用户/浏览/收藏）
import { Reading, User, View, Star } from '@element-plus/icons-vue'

const stats = ref({ totalBooks: 0, totalUsers: 0, totalViews: 0, totalCollections: 0 })

onMounted(async () => {
  try {
    // 请求后台概览统计数据
    const res = await adminHttp.get('/admin/stats/overview')
    // 成功时回填统计卡片数据
    if (res.data?.code === 200 && res.data?.data) {
      stats.value = res.data.data
    }
  } catch (e) {
    console.error('加载统计失败', e)
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

/* 统计卡片图标底色：使用 Element Plus 标准色，去掉渐变更简洁统一 */
.stat-icon--books {
  background: #409eff;
}

.stat-icon--users {
  background: #67c23a;
}

.stat-icon--views {
  background: #e6a23c;
}

.stat-icon--star {
  background: #f56c6c;
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

/* ========== 移动端响应式 ========== */
@media (max-width: 768px) {
  .admin-dashboard {
    padding: 0 4px;
  }

  .stats-grid {
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
    gap: 12px;
  }

  .stat-card {
    gap: 12px;
    padding: 16px;
  }

  .stat-icon {
    width: 44px;
    height: 44px;
    border-radius: 12px;
  }
}

</style>
