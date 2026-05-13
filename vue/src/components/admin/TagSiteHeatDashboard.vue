<template>
  <section class="heat-dash">
    <div class="heat-header">
      <h2 class="heat-title">{{ title }}</h2>
      <p v-if="subtitle" class="heat-sub">{{ subtitle }}</p>
      <p v-if="description" class="heat-desc">{{ description }}</p>
      <p v-if="lastComputedAt" class="heat-meta">最近时间：{{ formatHeatTime(lastComputedAt) }}</p>
    </div>
    <div v-if="error" class="heat-error">{{ error }}</div>
    <div v-else :class="['heat-list-wrap', scrollable && 'heat-list-wrap--scroll']">
      <div class="heat-cards">
        <div v-for="row in rows" :key="rowKey(row)" class="heat-card">
          <div class="heat-rank">{{ row.rank }}</div>
          <div class="heat-body">
            <div class="heat-tag">{{ row.tagName }}</div>
            <div class="heat-bar-wrap">
              <div class="heat-bar" :style="{ width: barWidth(row.shareOfAllTagsPercent) }" />
            </div>
            <div class="heat-stats">
              <span class="heat-raw">热度 {{ row.rawScore }}</span>
              <span v-if="row.computedRecommendWeight != null" class="heat-w">映射权重 {{ row.computedRecommendWeight }}</span>
              <span class="heat-pct">占全站标签热度 {{ row.shareOfAllTagsPercent }}%</span>
            </div>
          </div>
        </div>
      </div>
      <p v-if="!rows?.length && !error" class="heat-empty">{{ emptyText }}</p>
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: { type: String, default: '全站标签阅读走向' },
  subtitle: { type: String, default: '' },
  description: { type: String, default: '' },
  lastComputedAt: { type: Number, default: 0 },
  error: { type: String, default: '' },
  rows: { type: Array, default: () => [] },
  emptyText: { type: String, default: '暂无行为数据，产生收藏或阅读后将显示排行。' },
  scrollable: { type: Boolean, default: false }
})

const rowKey = (row) => (row.tagId != null ? row.tagId : `${row.rank}-${row.tagName}`)

const barWidth = (pct) => {
  const p = Number(pct)
  if (!Number.isFinite(p) || p <= 0) return '4%'
  return `${Math.min(100, Math.max(p, 4))}%`
}

const formatHeatTime = (ms) => {
  try {
    return new Date(ms).toLocaleString('zh-CN')
  } catch {
    return '-'
  }
}
</script>

<style scoped>
.heat-dash {
  background: #fff;
  border-radius: 16px;
  padding: clamp(22px, 3vw, 28px) clamp(22px, 3vw, 32px) clamp(26px, 3vw, 32px);
  border: 1px solid rgba(226, 232, 240, 0.95);
  box-shadow: 0 10px 40px rgba(99, 102, 241, 0.06), 0 2px 8px rgba(15, 23, 42, 0.04);
}

.heat-header {
  margin-bottom: 22px;
  padding-bottom: 4px;
  border-bottom: 1px solid rgba(241, 245, 249, 0.95);
}

.heat-title {
  font-size: 20px;
  font-weight: 700;
  color: #4c3d6b;
  margin: 0 0 10px 0;
  letter-spacing: -0.01em;
}

.heat-sub {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 8px 0;
  line-height: 1.6;
}

.heat-desc {
  font-size: 13px;
  color: #475569;
  margin: 0 0 6px 0;
  line-height: 1.55;
}

.heat-meta {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
}

.heat-error {
  color: #dc2626;
  font-size: 14px;
}

.heat-list-wrap--scroll {
  max-height: min(68vh, 760px);
  overflow-y: auto;
  padding-right: 6px;
}

.heat-cards {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.heat-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 18px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid rgba(241, 245, 249, 0.98);
  box-shadow: 0 4px 18px rgba(15, 23, 42, 0.06);
}

.heat-rank {
  flex: 0 0 36px;
  font-weight: 800;
  font-size: 20px;
  color: #6d28d9;
  text-align: center;
  line-height: 1.15;
  padding-top: 2px;
}

.heat-body {
  flex: 1;
  min-width: 0;
}

.heat-tag {
  font-weight: 700;
  color: #0f172a;
  font-size: 16px;
  margin-bottom: 10px;
  letter-spacing: -0.01em;
}

.heat-bar-wrap {
  height: 9px;
  background: #e8ecf4;
  border-radius: 999px;
  overflow: hidden;
  margin-bottom: 10px;
}

.heat-bar {
  height: 100%;
  background: linear-gradient(90deg, #8b5cf6 0%, #a855f7 40%, #ec4899 100%);
  border-radius: 999px;
  transition: width 0.35s ease;
}

.heat-stats {
  font-size: 12px;
  color: #64748b;
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
}

.heat-raw {
  color: #475569;
}

.heat-w {
  color: #14b8a6;
  font-weight: 600;
}

.heat-pct {
  color: #64748b;
}

.heat-empty {
  color: #94a3b8;
  font-size: 14px;
  margin: 8px 0 0 0;
}

.heat-list-wrap--scroll::-webkit-scrollbar {
  width: 6px;
}

.heat-list-wrap--scroll::-webkit-scrollbar-thumb {
  background: rgba(139, 92, 246, 0.35);
  border-radius: 3px;
}
</style>
