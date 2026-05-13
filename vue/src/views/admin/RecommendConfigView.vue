<template>
  <div class="recommend-config">
    <h2 class="page-heading">推荐配置</h2>
    <p class="lead">
      推荐对各标签的权重由<strong>全站行为</strong>（收藏、阅读、评论、完读）计算，映射为约
      <strong>0.1～1.0</strong> 的「映射权重」。
      
    </p>

    <TagSiteHeatDashboard
      :rows="heatRows"
      :subtitle="heatSubtitle"
      :description="heatMeta.description"
      :last-computed-at="heatMeta.lastComputedAt"
      :error="heatError"
      scrollable
      empty-text="暂无标签或尚无行为数据；请先在「系统设置」维护标签，并产生阅读/收藏等行为。"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import adminHttp from '@/utils/adminHttp'
import TagSiteHeatDashboard from '@/components/admin/TagSiteHeatDashboard.vue'

const heatRows = ref([])
const heatMeta = ref({ description: '', lastComputedAt: 0, cacheTtlSeconds: 120 })
const heatError = ref('')

const heatSubtitle = computed(() => {
  const ttl = heatMeta.value.cacheTtlSeconds || 120
  return `以下为全部标签的实时「热度」与映射后的推荐权重；数据每约 ${ttl} 秒与后端缓存同步。`
})

const loadHeat = async () => {
  try {
    const res = await adminHttp.get('/admin/stats/tag-site-heat-rows')
    if (res.data?.code === 200 && res.data?.data) {
      const d = res.data.data
      heatRows.value = Array.isArray(d.rows) ? d.rows : []
      heatMeta.value = {
        description: d.description || '',
        lastComputedAt: d.lastComputedAt || 0,
        cacheTtlSeconds: d.cacheTtlSeconds ?? 120
      }
      heatError.value = ''
    } else {
      heatError.value = res.data?.msg || '加载标签热度失败'
    }
  } catch (e) {
    heatError.value = '加载标签热度失败（请确认已登录管理员且后端可访问）'
    console.error(e)
  }
}

onMounted(loadHeat)
</script>

<style scoped>
.recommend-config {
  padding: 0;
  max-width: 960px;
}

.page-heading {
  margin: 0 0 8px 0;
  font-size: 22px;
  font-weight: 800;
  color: #312e81;
  letter-spacing: -0.02em;
}

.lead {
  margin: 0 0 22px 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.65;
}

.lead strong {
  color: #334155;
}

.lead code {
  font-size: 12px;
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
  color: #475569;
}
</style>
