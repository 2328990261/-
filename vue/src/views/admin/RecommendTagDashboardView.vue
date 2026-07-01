<template>
  <div class="tag-dash-page">
    <header class="dash-top">
      <div class="dash-top-left">
        <button type="button" class="dash-back" @click="goBack">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
          返回
        </button>
        <div>
          <h1 class="dash-title">仪表盘</h1>
          <p class="dash-lead">全站标签阅读走向与推荐权重分布</p>
        </div>
      </div>
      <button type="button" class="dash-refresh" :disabled="loading" @click="loadHeat">
        {{ loading ? '刷新中…' : '刷新数据' }}
      </button>
    </header>

    <div v-if="summaryReady" class="dash-stats">
      <div class="dash-stat-card">
        <span class="dash-stat-label">标签总数</span>
        <span class="dash-stat-value">{{ heatRows.length }}</span>
      </div>
      <div class="dash-stat-card dash-stat-card--accent">
        <span class="dash-stat-label">热度最高</span>
        <span class="dash-stat-value dash-stat-value--sm">{{ topTagName }}</span>
      </div>
      <div class="dash-stat-card">
        <span class="dash-stat-label">Top5 热度占比</span>
        <span class="dash-stat-value">{{ top5Share }}%</span>
      </div>
      <div class="dash-stat-card">
        <span class="dash-stat-label">数据同步</span>
        <span class="dash-stat-value dash-stat-value--sm">约 {{ cacheTtl }} 秒</span>
      </div>
    </div>

    <TagSiteHeatDashboard
      title="全站标签阅读走向"
      :rows="heatRows"
      :subtitle="heatSubtitle"
      :last-computed-at="lastComputedAt"
      :error="heatError"
      scrollable
      empty-text="暂无标签或尚无行为数据；请先在「系统设置」维护标签，并产生阅读/收藏等行为。"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import adminHttp from '@/utils/adminHttp'
import TagSiteHeatDashboard from '@/components/admin/TagSiteHeatDashboard.vue'

const router = useRouter()
const heatRows = ref([])
const lastComputedAt = ref(0)
const cacheTtl = ref(120)
const heatError = ref('')
const loading = ref(false)

const heatSubtitle = computed(() =>
  `全部标签实时热度与映射权重；数据每约 ${cacheTtl.value} 秒与后端缓存同步。`
)

const summaryReady = computed(() => heatRows.value.length > 0 && !heatError.value)

const topTagName = computed(() => heatRows.value[0]?.tagName || '—')

const top5Share = computed(() => {
  const sum = heatRows.value.slice(0, 5).reduce((s, r) => s + (Number(r.shareOfAllTagsPercent) || 0), 0)
  return sum.toFixed(1)
})

const loadHeat = async () => {
  loading.value = true
  try {
    const res = await adminHttp.get('/admin/stats/tag-site-heat-rows')
    if (res.data?.code === 200 && res.data?.data) {
      const d = res.data.data
      heatRows.value = Array.isArray(d.rows) ? d.rows : []
      lastComputedAt.value = d.lastComputedAt || 0
      cacheTtl.value = d.cacheTtlSeconds ?? 120
      heatError.value = ''
    } else {
      heatError.value = res.data?.msg || '加载标签热度失败'
    }
  } catch (e) {
    heatError.value = '加载标签热度失败（请确认已登录管理员且后端可访问）'
    console.error(e)
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/admin/recommend')
}

onMounted(loadHeat)
</script>

<style scoped>
.tag-dash-page {
  max-width: 1080px;
  margin: 0 auto;
}

.dash-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
}

.dash-top-left {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.dash-back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  padding: 8px 12px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 10px;
  background: #fff;
  color: #64748b;
  font-size: 13px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s, box-shadow 0.2s;
}

.dash-back:hover {
  color: #6d28d9;
  border-color: #ddd6fe;
  box-shadow: 0 4px 14px rgba(109, 40, 217, 0.08);
}

.dash-title {
  margin: 0 0 6px 0;
  font-size: 26px;
  font-weight: 800;
  color: #312e81;
  letter-spacing: -0.02em;
}

.dash-lead {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

.dash-refresh {
  flex-shrink: 0;
  padding: 10px 18px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #7c3aed 0%, #a855f7 100%);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 6px 18px rgba(124, 58, 237, 0.28);
  transition: transform 0.2s, box-shadow 0.2s, opacity 0.2s;
}

.dash-refresh:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 22px rgba(124, 58, 237, 0.34);
}

.dash-refresh:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.dash-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 22px;
}

.dash-stat-card {
  padding: 16px 18px;
  background: #fff;
  border-radius: 14px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  box-shadow: 0 6px 24px rgba(99, 102, 241, 0.06);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dash-stat-card--accent {
  background: linear-gradient(145deg, #faf5ff 0%, #fff 100%);
  border-color: rgba(216, 180, 254, 0.6);
}

.dash-stat-label {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
}

.dash-stat-value {
  font-size: 28px;
  font-weight: 800;
  color: #1e293b;
  line-height: 1.1;
  letter-spacing: -0.02em;
}

.dash-stat-value--sm {
  font-size: 18px;
  font-weight: 700;
  color: #6d28d9;
}

@media (max-width: 900px) {
  .dash-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .dash-top {
    flex-direction: column;
  }

  .dash-stats {
    grid-template-columns: 1fr;
  }
}
</style>
