<template>
  <section class="heat-dash">
    <div class="heat-header">
      <div class="heat-header-main">
        <h2 class="heat-title">{{ title }}</h2>
        <p v-if="subtitle" class="heat-sub">{{ subtitle }}</p>
        <p v-if="description" class="heat-desc">{{ description }}</p>
        <p v-if="lastComputedAt" class="heat-meta">最近时间：{{ formatHeatTime(lastComputedAt) }}</p>
      </div>
      <div v-if="showGauge && gaugeReady" class="heat-gauge-panel" aria-label="标签热度分布仪表盘">
        <div class="heat-gauge-ring" :style="{ background: gaugeGradient }">
          <div class="heat-gauge-hole">
            <span class="heat-gauge-value">{{ top5ShareTotal }}%</span>
            <span class="heat-gauge-label">Top5 占比</span>
          </div>
        </div>
        <ul class="heat-gauge-legend">
          <li v-for="item in gaugeLegend" :key="item.name">
            <span class="heat-gauge-dot" :style="{ background: item.color }" />
            <span class="heat-gauge-name">{{ item.name }}</span>
            <span class="heat-gauge-pct">{{ item.pct }}%</span>
          </li>
        </ul>
      </div>
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
import { computed } from 'vue'

const GAUGE_COLORS = ['#8b5cf6', '#a855f7', '#ec4899', '#14b8a6', '#f59e0b']

const props = defineProps({
  title: { type: String, default: '全站标签阅读走向' },
  subtitle: { type: String, default: '' },
  description: { type: String, default: '' },
  lastComputedAt: { type: Number, default: 0 },
  error: { type: String, default: '' },
  rows: { type: Array, default: () => [] },
  emptyText: { type: String, default: '暂无行为数据，产生收藏或阅读后将显示排行。' },
  scrollable: { type: Boolean, default: false },
  showGauge: { type: Boolean, default: true }
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

const topRowsForGauge = computed(() => (props.rows || []).slice(0, 5))

const gaugeReady = computed(() => topRowsForGauge.value.some((r) => Number(r.shareOfAllTagsPercent) > 0))

const top5ShareTotal = computed(() => {
  const sum = topRowsForGauge.value.reduce((s, r) => s + (Number(r.shareOfAllTagsPercent) || 0), 0)
  return sum.toFixed(1)
})

const gaugeLegend = computed(() =>
  topRowsForGauge.value.slice(0, 3).map((row, i) => ({
    name: row.tagName,
    pct: Number(row.shareOfAllTagsPercent).toFixed(2),
    color: GAUGE_COLORS[i % GAUGE_COLORS.length]
  }))
)

const gaugeGradient = computed(() => {
  const top = topRowsForGauge.value
  if (!top.length) return '#e2e8f0'
  let acc = 0
  const parts = []
  top.forEach((row, i) => {
    const pct = Number(row.shareOfAllTagsPercent) || 0
    if (pct <= 0) return
    const start = acc
    acc += pct
    parts.push(`${GAUGE_COLORS[i % GAUGE_COLORS.length]} ${start * 3.6}deg ${acc * 3.6}deg`)
  })
  if (acc < 100) {
    parts.push(`#e8ecf4 ${acc * 3.6}deg 360deg`)
  }
  return parts.length ? `conic-gradient(${parts.join(', ')})` : '#e2e8f0'
})
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
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px 28px;
  margin-bottom: 22px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(241, 245, 249, 0.95);
}

.heat-header-main {
  flex: 1;
  min-width: 0;
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

.heat-gauge-panel {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: linear-gradient(145deg, #faf5ff 0%, #f8fafc 100%);
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 14px;
  min-width: 148px;
}

.heat-gauge-ring {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 0 0 1px rgba(139, 92, 246, 0.08);
}

.heat-gauge-hole {
  width: 62px;
  height: 62px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.06);
}

.heat-gauge-value {
  font-size: 15px;
  font-weight: 800;
  color: #6d28d9;
  line-height: 1.1;
}

.heat-gauge-label {
  font-size: 10px;
  color: #94a3b8;
  margin-top: 2px;
}

.heat-gauge-legend {
  list-style: none;
  margin: 0;
  padding: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.heat-gauge-legend li {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #64748b;
}

.heat-gauge-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.heat-gauge-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #475569;
}

.heat-gauge-pct {
  flex-shrink: 0;
  font-weight: 600;
  color: #6d28d9;
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

@media (max-width: 720px) {
  .heat-header {
    flex-direction: column;
  }

  .heat-gauge-panel {
    align-self: flex-end;
  }
}
</style>
