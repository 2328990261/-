<template>
  <section class="trend-panel">
    <header class="trend-head">
      <h2 class="trend-title">{{ panelTitle }}</h2>
      <!-- 收藏数/评论数/完读数：保留日/周/月切换 -->
      <div v-if="metric !== 'heat'" class="seg trend-time">
        <button
          v-for="opt in ranges"
          :key="opt.value"
          type="button"
          class="seg-btn"
          :class="{ active: granularity === opt.value }"
          @click="granularity = opt.value"
        >
          {{ opt.label }}
        </button>
      </div>
      <!-- 标签走向：仪表盘放到原简略/详情切换位置，展示 Top5 喜好占比 -->
      <div v-if="metric === 'heat' && gaugeLegend.length" class="heat-gauge-panel" aria-label="标签喜好占比仪表盘">
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
    </header>

    <div class="trend-body">
      <!-- 左侧指标切换 -->
      <nav class="metric-nav">
        <button
          v-for="m in metrics"
          :key="m.value"
          type="button"
          class="metric-btn"
          :class="{ active: metric === m.value }"
          @click="metric = m.value"
        >
          {{ m.label }}
        </button>
      </nav>

      <!-- 标签走向：固定显示前 5 个标签 + “其他”，按全站标签喜好占比展示 -->
      <div v-if="metric === 'heat'" class="heat-wrap">
        <div v-if="error" class="trend-hint trend-hint--error">{{ error }}</div>
        <div v-else-if="loading && !heatBarData.names.length" class="trend-hint">加载中…</div>
        <div v-else-if="!heatBarData.names.length" class="trend-hint">{{ emptyText }}</div>
        <div v-else ref="heatChartRef" class="heat-chart" />
      </div>

      <!-- 收藏数/评论数/完读数：按日/周/月展示时序折线 -->
      <div v-else class="trend-chart-wrap">
        <div v-if="error" class="trend-hint trend-hint--error">{{ error }}</div>
        <div v-else-if="loading && !hasData" class="trend-hint">加载中…</div>
        <div v-else-if="!hasData" class="trend-hint">{{ emptyText }}</div>
        <div v-else ref="trendChartRef" class="trend-chart" />
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onBeforeUnmount, watch, nextTick } from 'vue'
// 标签走向使用 ECharts 柱状图（按喜好占比）；收藏/评论/完读使用折线图
import * as echarts from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import adminHttp from '@/utils/adminHttp'

echarts.use([BarChart, LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

// 前 5 个标签使用 Element Plus 标准色，“其他”统一用中性灰
const PALETTE = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399']
const OTHER_COLOR = '#cbd5e1'

// 时间粒度：仅收藏数/评论数/完读数使用
const ranges = [
  { label: '日', value: 'day' },
  { label: '周', value: 'week' },
  { label: '月', value: 'month' }
]

// 左侧统计指标
const metrics = [
  { label: '标签走向', value: 'heat' },
  { label: '收藏数', value: 'collection' },
  { label: '评论数', value: 'comment' },
  { label: '完读数', value: 'finished' }
]

const granularity = ref('day')
const metric = ref('heat')

// 标签走向使用的全局标签热度行与更新时间
const heatRows = ref([])
const lastComputedAt = ref(0)
// 收藏/评论/完读时序数据
const buckets = ref([])
const seriesData = ref([])

const error = ref('')
const loading = ref(false)

const heatChartRef = ref(null)
const trendChartRef = ref(null)
let heatChart = null
let trendChart = null
let heatResizeObserver = null
let trendResizeObserver = null

const emptyText = '暂无数据'

const isHeat = computed(() => metric.value === 'heat')

// 按占据全站标签喜好的占比从高到低排序，保证柱状图前 5 个就是占比最高的 5 个标签
const sortedHeatRows = computed(() =>
  [...(heatRows.value || [])].sort((a, b) => (Number(b.shareOfAllTagsPercent) || 0) - (Number(a.shareOfAllTagsPercent) || 0))
)

// 柱状图数据：前 5 个标签分别展示，其余合并为“其他”
const heatBarData = computed(() => {
  const rows = sortedHeatRows.value
  const top = rows.slice(0, 5).map((row) => ({
    name: row.tagName,
    share: Number(row.shareOfAllTagsPercent) || 0,
    color: null
  }))
  if (!top.length) {
    return { names: [], values: [], bars: [] }
  }
  // “其他”占比 = 100% - 前 5 占比之和，兜底为 0 避免小数舍入导致负值
  const otherShare = Math.max(0, 100 - top.reduce((sum, item) => sum + item.share, 0))
  if (rows.length > 5) {
    top.push({ name: '其他', share: otherShare, color: OTHER_COLOR })
  }
  const bars = top.map((item, index) => ({
    name: item.name,
    value: item.share,
    color: item.color || PALETTE[index % PALETTE.length]
  }))
  return { names: bars.map((b) => b.name), values: bars.map((b) => b.value), bars }
})

// 收藏/评论/完读折线序列（单条全站总量）
const displaySeries = computed(() => (!isHeat.value ? seriesData.value : []))

// 折线是否有数据；柱状图用 heatBarData 判断，不走此逻辑
const hasData = computed(() => !isHeat.value && buckets.value.length > 0 && displaySeries.value.length > 0)

const panelTitle = computed(() => (isHeat.value ? '全站标签喜好占比' : `${metricLabel.value}趋势`))

// 当前指标显示名，用于折线图标题与轴名称
const metricLabel = computed(() => {
  const found = metrics.find((m) => m.value === metric.value)
  return found ? found.label : '数据'
})

// Top5 占比合计（仪表盘中心数值）
const top5ShareTotal = computed(() => {
  const sum = sortedHeatRows.value.slice(0, 5).reduce((s, r) => s + (Number(r.shareOfAllTagsPercent) || 0), 0)
  return sum.toFixed(1)
})

// 仪表盘图例取前 3 个主标签
const gaugeLegend = computed(() =>
  sortedHeatRows.value.slice(0, 3).map((row, i) => ({
    name: row.tagName,
    pct: Number(row.shareOfAllTagsPercent).toFixed(1),
    color: PALETTE[i % PALETTE.length]
  }))
)

// 仪表盘环形：按 Top5 占比份额拼接 conic-gradient
const gaugeGradient = computed(() => {
  const top = sortedHeatRows.value.slice(0, 5)
  if (!top.length || top5ShareTotal.value === '0.0') return '#e2e8f0'
  let acc = 0
  const parts = []
  top.forEach((row, i) => {
    const pct = Number(row.shareOfAllTagsPercent) || 0
    if (pct <= 0) return
    const start = acc
    acc += pct
    parts.push(`${PALETTE[i % PALETTE.length]} ${start * 3.6}deg ${acc * 3.6}deg`)
  })
  if (acc < 100) {
    parts.push(`#e8ecf4 ${acc * 3.6}deg 360deg`)
  }
  return parts.length ? `conic-gradient(${parts.join(', ')})` : '#e2e8f0'
})

// 拉取数据：标签走向走全站标签热度行接口，其余指标走时序接口
const load = async () => {
  loading.value = true
  try {
    if (isHeat.value) {
      const res = await adminHttp.get('/admin/stats/tag-site-heat-rows')
      if (res.data?.code === 200 && res.data?.data) {
        heatRows.value = Array.isArray(res.data.data.rows) ? res.data.data.rows : []
        lastComputedAt.value = Number(res.data.data.lastComputedAt) || 0
        error.value = ''
      } else {
        error.value = res.data?.msg || '加载标签喜好占比失败'
        heatRows.value = []
        lastComputedAt.value = 0
      }
    } else {
      const res = await adminHttp.get('/admin/stats/tag-trend', {
        params: { granularity: granularity.value, metric: metric.value }
      })
      if (res.data?.code === 200 && res.data?.data) {
        buckets.value = Array.isArray(res.data.data.buckets) ? res.data.data.buckets : []
        seriesData.value = Array.isArray(res.data.data.series) ? res.data.data.series : []
        error.value = ''
      } else {
        error.value = res.data?.msg || '加载趋势数据失败'
        buckets.value = []
        seriesData.value = []
      }
    }
  } catch (e) {
    error.value = '加载数据失败（请确认已登录管理员且后端可访问）'
    heatRows.value = []
    buckets.value = []
    seriesData.value = []
    console.error(e)
  } finally {
    loading.value = false
    nextTick(renderCharts)
  }
}

// 构建标签走向柱状图配置：横轴为前 5 标签 + 其他，纵轴为占比百分比
const buildHeatOption = () => {
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (v) => `${Number(v).toFixed(1)}% 喜好占比`
    },
    grid: { left: 8, right: 18, top: 26, bottom: 20, containLabel: true },
    xAxis: {
      type: 'category',
      data: heatBarData.value.names,
      axisTick: { alignWithLabel: true },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisLabel: { color: '#64748b', interval: 0 }
    },
    yAxis: {
      type: 'value',
      name: '喜好占比',
      nameTextStyle: { color: '#94a3b8' },
      axisLabel: { color: '#94a3b8', formatter: '{value}%' },
      splitLine: { lineStyle: { color: '#f1f5f9' } }
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 46,
        itemStyle: { borderRadius: [4, 4, 0, 0] },
        data: heatBarData.value.bars.map((b) => ({ value: b.value, itemStyle: { color: b.color } }))
      }
    ]
  }
}

// 构建折线图配置（收藏/评论/完读）
const buildLineOption = () => {
  const name = metricLabel.value
  return {
    color: ['#409eff'],
    tooltip: { trigger: 'axis', axisPointer: { type: 'line' } },
    legend: { top: 0, textStyle: { color: '#64748b' } },
    grid: { left: 8, right: 18, top: 36, bottom: 10, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: buckets.value,
      axisTick: { alignWithLabel: true },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisLabel: { color: '#64748b' }
    },
    yAxis: {
      type: 'value',
      name: '数量',
      nameTextStyle: { color: '#94a3b8' },
      axisLabel: { color: '#94a3b8' },
      splitLine: { lineStyle: { color: '#f1f5f9' } }
    },
    series: displaySeries.value.map((it) => ({
      name: it.name || name,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      data: it.values || [],
      lineStyle: { width: 2 },
      areaStyle: { opacity: 0.12 }
    }))
  }
}

// 初始化或更新图表；不存在的图表实例会被释放
const renderCharts = () => {
  const heatEl = heatChartRef.value
  const trendEl = trendChartRef.value

  // 柱状图分支：首次渲染时初始化实例并监听尺寸变化
  if (heatEl && isHeat.value && heatBarData.value.names.length) {
    if (!heatChart) {
      heatChart = echarts.init(heatEl)
      if (typeof ResizeObserver !== 'undefined') {
        heatResizeObserver = new ResizeObserver(() => heatChart && heatChart.resize())
        heatResizeObserver.observe(heatEl)
      }
    }
    heatChart.setOption(buildHeatOption(), true)
  } else if (heatChart) {
    heatChart.dispose()
    heatChart = null
    if (heatResizeObserver) heatResizeObserver.disconnect()
    heatResizeObserver = null
  }

  // 折线图分支：收藏/评论/完读
  if (trendEl && !isHeat.value && hasData.value) {
    if (!trendChart) {
      trendChart = echarts.init(trendEl)
      if (typeof ResizeObserver !== 'undefined') {
        trendResizeObserver = new ResizeObserver(() => trendChart && trendChart.resize())
        trendResizeObserver.observe(trendEl)
      }
    }
    trendChart.setOption(buildLineOption(), true)
  } else if (trendChart) {
    trendChart.dispose()
    trendChart = null
    if (trendResizeObserver) trendResizeObserver.disconnect()
    trendResizeObserver = null
  }
}

const disposeCharts = () => {
  if (heatResizeObserver) heatResizeObserver.disconnect()
  if (trendResizeObserver) trendResizeObserver.disconnect()
  heatResizeObserver = null
  trendResizeObserver = null
  if (heatChart) heatChart.dispose()
  if (trendChart) trendChart.dispose()
  heatChart = null
  trendChart = null
}

// 指标或时间粒度变化时重新拉取
watch([metric, granularity], load, { immediate: true })

// 标签走向占比数据变化时重绘柱状图
watch(heatBarData, () => nextTick(renderCharts), { deep: true })

onBeforeUnmount(disposeCharts)
</script>

<style scoped>
.trend-panel {
  background: #fff;
  border-radius: 16px;
  padding: clamp(20px, 2.8vw, 28px);
  border: 1px solid rgba(226, 232, 240, 0.95);
  box-shadow: 0 10px 40px rgba(99, 102, 241, 0.06), 0 2px 8px rgba(15, 23, 42, 0.04);
}

.trend-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px 16px;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(241, 245, 249, 0.95);
}

.trend-title {
  font-size: 20px;
  font-weight: 700;
  color: #4c3d6b;
  margin: 0 auto 0 0;
  letter-spacing: -0.01em;
}

/* 分段切换按钮组 */
.seg {
  display: inline-flex;
  padding: 3px;
  background: #f1f5f9;
  border-radius: 10px;
}

.seg-btn {
  border: none;
  background: transparent;
  padding: 7px 14px;
  border-radius: 8px;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.seg-btn.active {
  background: #409eff;
  color: #fff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.28);
}

.trend-body {
  display: flex;
  gap: 16px;
  align-items: stretch;
}

/* 左侧指标导航 */
.metric-nav {
  flex: 0 0 120px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.metric-btn {
  border: 1px solid #e5e7eb;
  background: #f8fafc;
  padding: 12px 10px;
  border-radius: 10px;
  font-size: 14px;
  color: #475569;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s ease;
}

.metric-btn:hover {
  border-color: #b4d5ff;
  color: #409eff;
}

.metric-btn.active {
  border-color: #409eff;
  background: #ecf5ff;
  color: #409eff;
  font-weight: 600;
}

/* 标签走向柱状图区 */
.heat-wrap {
  flex: 1;
  min-width: 0;
}

.heat-chart {
  width: 100%;
  height: clamp(340px, 46vh, 460px);
}

/* 仪表盘放在标题旁，保持紧凑 */
.heat-gauge-panel {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #f8fafc;
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 14px;
}

.heat-gauge-ring {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.heat-gauge-hole {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
}

.heat-gauge-value {
  font-size: 13px;
  font-weight: 800;
  color: #409eff;
  line-height: 1.1;
}

.heat-gauge-label {
  font-size: 9px;
  color: #94a3b8;
  margin-top: 1px;
}

.heat-gauge-legend {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 96px;
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
  color: #409eff;
}

/* 时序折线区 */
.trend-chart-wrap {
  flex: 1;
  min-width: 0;
}

.trend-chart {
  width: 100%;
  height: clamp(320px, 44vh, 420px);
}

.trend-hint {
  color: #94a3b8;
  font-size: 14px;
  padding: 24px 8px;
}

.trend-hint--error {
  color: #dc2626;
}

@media (max-width: 720px) {
  .trend-body {
    flex-direction: column;
  }

  .metric-nav {
    flex: 0 0 auto;
    flex-direction: row;
    flex-wrap: wrap;
  }

  .metric-btn {
    flex: 1;
    text-align: center;
  }

  .heat-gauge-panel {
    width: 100%;
    justify-content: center;
    flex-wrap: wrap;
  }
}
</style>