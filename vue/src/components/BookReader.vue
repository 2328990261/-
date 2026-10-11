<template>
  <div class="book-reader" :class="[
    { 'eye-protection-mode': eyeProtection },
    `font-size-${fontSize}`,
    `page-width-${pageWidth}`
  ]">
    <!-- 小说标题和章节标题 -->
    <div class="reader-header">
      <h1 class="book-title">{{ bookDetail?.bookMainName || '未知小说' }}</h1>
      <h2 class="chapter-title">{{ chapter?.volumeName || '暂无章节' }}</h2>
    </div>

    <!-- 作者和更新信息 -->
    <div class="reader-meta">
      <span class="meta-author">作者：{{ bookDetail?.author || '未知作者' }}</span>
      <div class="meta-right">
      </div>
    </div>

    <!-- 阅读内容区域 -->
    <div class="reader-content">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="allBlocks.length > 0" ref="contentAreaRef" class="content-area">
        <p
          v-for="(para, idx) in allBlocks"
          :key="para.key"
          class="content-para"
          :class="{ 'content-image-line': para.type === 'image', 'is-hidden-page': !blockInCurrentPage(idx) }"
        >
          <img
            v-if="para.type === 'image'"
            :src="illustrationUrl(para.order)"
            :style="illustrationStyle"
            class="reader-illustration"
            alt="插图"
            @load="scheduleRelayout"
            @error="scheduleRelayout"
          >
          <template v-else>{{ para.text }}</template>
        </p>
      </div>
      <div v-else class="empty-content">暂无章节内容</div>
    </div>

    <!-- 分页控件 -->
    <div class="pagination" v-if="totalPages > 1">
      <div class="pagination-buttons">
        <button
          class="page-btn"
          :disabled="currentPage === 1"
          @click="prevPage"
        >
          上一页
        </button>
        <span class="page-info">
          {{ currentPage }} / {{ totalPages }}
        </span>
        <button
          class="page-btn"
          :disabled="currentPage === totalPages"
          @click="nextPage"
        >
          下一页
        </button>
      </div>
      
      <!-- 页码跳转功能 -->
      <div class="page-jump">
        <span class="jump-text">跳转到</span>
        <input
          type="number"
          v-model.number="jumpPage"
          class="jump-input"
          :min="1"
          :max="totalPages"
          @keyup.enter="goToJumpPage"
          placeholder="页码"
        >
        <button
          class="page-btn jump-btn"
          @click="goToJumpPage"
          :disabled="!jumpPage || jumpPage < 1 || jumpPage > totalPages"
        >
          跳转
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { backendUrl } from '@/config/env'

// 定义组件属性
const props = defineProps({
  // 小说详情
  bookDetail: {
    type: Object,
    default: () => ({})
  },
  // 章节内容
  chapter: {
    type: Object,
    default: () => ({})
  },
  // 是否加载中
  loading: {
    type: Boolean,
    default: false
  },
  // 每页显示行数
  pageSize: {
    type: Number,
    default: 100
  },
  // 护眼模式
  eyeProtection: {
    type: Boolean,
    default: false
  },
  // 字体大小
  fontSize: {
    type: String,
    default: 'medium'
  },
  // 页面宽度
  pageWidth: {
    type: String,
    default: 'medium'
  }
})

// 定义事件
const emit = defineEmits([
  'prev-page',   // 上一页事件
  'next-page',   // 下一页事件
  'page-change'  // 页码变化事件
])

// 分页状态
const currentPage = ref(1)
// 跳转页码
const jumpPage = ref(null)

/** 正文中的插画占位标记：[插图n]，n 对应插画表 sort_order */
const ILLUS_RE = /^\[插图(\d+)\]$/

/** 内容区根节点（用于测量分页高度） */
const contentAreaRef = ref(null)

/** 内容块：文本块 { type:'text', text } 与插画块 { type:'image', order }，按出现顺序排列 */
const allBlocks = computed(() => {
  if (!props.chapter?.content) return []
  const blocks = []
  props.chapter.content.split('\n').forEach((rawLine, i) => {
    const line = rawLine.trim()
    if (!line) return
    const m = line.match(ILLUS_RE)
    if (m) {
      blocks.push({ key: 'img' + i, type: 'image', order: Number(m[1]) })
    } else {
      blocks.push({ key: 'txt' + i, type: 'text', text: line })
    }
  })
  return blocks
})

/** 分页结果：每页为 { start, end } 块下标区间（按实际渲染高度切分） */
const pages = ref([])

/** 当前页高度（px）= 每页行数 × 行高，用于超高插图限高 */
const pageHeightPx = ref(0)

/** 根据插画序号解析对应插画的访问 URL */
function illustrationUrl(order) {
  const illustrations = props.chapter?.illustrations || []
  const illustration = illustrations.find(i => Number(i.sortOrder) === order)
  return illustration?.url ? backendUrl(illustration.url) : ''
}

/** 图片限高：超高插图单独成页时也不会超出页面高度 */
const illustrationStyle = computed(() => {
  return pageHeightPx.value ? { maxHeight: pageHeightPx.value + 'px' } : {}
})

/** 某块是否属于当前页（分页未就绪时全部显示，便于测量） */
function blockInCurrentPage(idx) {
  const page = pages.value[currentPage.value - 1]
  if (!page) return true
  return idx >= page.start && idx < page.end
}

/** 计算总页数 */
const totalPages = computed(() => pages.value.length)

/** 防抖定时器 */
let relayoutTimer = null

/** 重新测量并分页：先清空分页让所有块可见以测真实高度，再按高度贪心切页 */
async function relayout() {
  if (!props.chapter?.content) {
    pages.value = []
    pageHeightPx.value = 0
    return
  }
  pages.value = []
  await nextTick()
  const area = contentAreaRef.value
  if (!area) return
  const items = area.querySelectorAll('.content-para')
  if (!items.length) {
    pages.value = [{ start: 0, end: 0 }]
    return
  }

  // 估算行高：取第一个文本块的实际行高
  let lineHeight = 0
  const firstText = area.querySelector('.content-para:not(.content-image-line)')
  if (firstText) {
    const cs = getComputedStyle(firstText)
    lineHeight = parseFloat(cs.lineHeight) || (parseFloat(cs.fontSize) * 1.6)
  }
  if (!lineHeight || lineHeight < 12) lineHeight = 40
  const pageHeight = Math.max(1, Math.round(props.pageSize * lineHeight))
  pageHeightPx.value = pageHeight

  // 贪心分页：累计高度超过页高则换页；超高块（限高后的插图）单独成页
  const result = []
  let start = 0
  let acc = 0
  for (let i = 0; i < items.length; i++) {
    const h = items[i].offsetHeight
    if (h >= pageHeight) {
      if (i > start) result.push({ start, end: i })
      result.push({ start: i, end: i + 1 })
      start = i + 1
      acc = 0
    } else if (acc + h > pageHeight && i > start) {
      result.push({ start, end: i })
      start = i
      acc = h
    } else {
      acc += h
    }
  }
  if (start < items.length) result.push({ start, end: items.length })
  pages.value = result.length ? result : [{ start: 0, end: 0 }]

  // 页码越界保护（换章节/设置变化后页数可能变少）
  if (currentPage.value > pages.value.length) {
    currentPage.value = pages.value.length || 1
  }
}

/** 安排一次防抖的重新分页（字体/宽度/章节/图片加载等变化时调用） */
function scheduleRelayout() {
  if (relayoutTimer) clearTimeout(relayoutTimer)
  relayoutTimer = setTimeout(() => { relayout() }, 60)
}

// 上一页
const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    emit('prev-page')
    emit('page-change', currentPage.value)
  }
}

// 下一页
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    emit('next-page')
    emit('page-change', currentPage.value)
  }
}

// 跳转到指定页
const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    emit('page-change', currentPage.value)
  }
}

// 处理页码跳转
const goToJumpPage = () => {
  const page = Number(jumpPage.value)
  if (page >= 1 && page <= totalPages.value) {
    goToPage(page)
  }
  // 清空输入框
  jumpPage.value = null
}

// 内容、字体、宽度、行数、加载状态变化时重新分页
watch(() => [props.chapter, props.fontSize, props.pageWidth], scheduleRelayout, { deep: true })
watch(() => props.pageSize, scheduleRelayout)
watch(() => props.loading, (v) => { if (!v) scheduleRelayout() })
onMounted(scheduleRelayout)

// 监听章节变化，重置页码
watch(() => props.chapter, () => {
  // 只有在没有外部指定页码时才重置为1
  // 这样当从继续阅读跳转到指定页码时不会被重置
  if (!window.location.search.includes('page=')) {
    currentPage.value = 1
    emit('page-change', 1)
  }
}, { deep: true })

// 监听页码变化，自动滚动到顶部
watch(currentPage, () => {
  // 使用 nextTick 确保 DOM 更新后再滚动
  nextTick(() => {
    window.scrollTo({
      top: 0,
      behavior: 'auto' // 直接跳转，没有平滑滚动效果
    })
  })
})

// 暴露方法给父组件
defineExpose({
  prevPage,
  nextPage,
  goToPage,
  currentPage,
  totalPages
})
</script>

<style scoped>
/* 小说阅读主容器 */
.book-reader {
  width: 100%;
  margin: 0;
  padding: 0;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  background-color: #f8f6f0;
  overflow: hidden;
  transition: background-color 0.3s ease;
}

/* 页面宽度设置 */
.book-reader.page-width-narrow {
  max-width: 600px;
  transition: none;
}

.book-reader.page-width-medium {
  max-width: 800px;
  transition: none;
}

.book-reader.page-width-wide {
  max-width: 1000px;
  transition: none;
}

/* 护眼模式 */
.book-reader.eye-protection-mode {
  background-color: #121212;
  border-color: #333;
  color: #e0e0e0;
}

/* 护眼模式下的内容区域 */
.book-reader.eye-protection-mode .content-area {
  color: #e0e0e0;
}

/* 护眼模式下的背景区域 */
.book-reader.eye-protection-mode .reader-header,
.book-reader.eye-protection-mode .reader-meta,
.book-reader.eye-protection-mode .pagination {
  background-color: #1e1e1e;
  border-color: #333;
}

/* 护眼模式下的文字颜色 */
.book-reader.eye-protection-mode .book-title,
.book-reader.eye-protection-mode .chapter-title,
.book-reader.eye-protection-mode .meta-author,
.book-reader.eye-protection-mode .meta-update,
.book-reader.eye-protection-mode .meta-word-count {
  color: #e0e0e0;
}

/* 护眼模式下的按钮样式 */
.book-reader.eye-protection-mode .page-btn {
  background-color: #1e1e1e;
  border-color: #444;
  color: #e0e0e0;
}

.book-reader.eye-protection-mode .page-btn:hover:not(:disabled) {
  background-color: #3a3a3a;
  border-color: #666;
  color: #fff;
}

.book-reader.eye-protection-mode .page-btn:disabled {
  background-color: #1e1e1e;
  border-color: #333;
  color: #666;
}

.book-reader.eye-protection-mode .page-info {
  color: #e0e0e0;
}

/* 护眼模式下的加载和空内容样式 */
.book-reader.eye-protection-mode .loading,
.book-reader.eye-protection-mode .empty-content {
  color: #a0a0a0;
}

/* 标题区域 */
.reader-header {
  padding: 30px 40px 20px;
  background-color: #f8f6f0;
  border-bottom: 1px solid #e0e0e0;
  text-align: center;
}

.book-title {
  font-size: 28px;
  color: #333;
  margin: 0 0 12px 0;
  font-weight: 700;
}

.chapter-title {
  font-size: 22px;
  color: #666;
  margin: 0 0 15px 0;
  font-weight: 500;
}

/* 作者和更新信息 */
.reader-meta {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  font-size: 16px;
  color: #999;
  padding: 15px 40px 20px;
  background-color: #f8f6f0;
  border-bottom: 1px solid #e0e0e0;
  text-align: center;
}

.meta-right {
  display: flex;
  gap: 25px;
  margin-top: 8px;
}

.meta-author,
.meta-update,
.meta-word-count {
  font-size: 16px;
  color: #999;
  line-height: 1.5;
}

/* 内容区域 */
.reader-content {
  padding: 40px;
  min-height: 500px;
}

.loading,
.empty-content {
  text-align: center;
  padding: 60px 0;
  color: #909399;
  font-size: 18px;
}

.content-area {
  line-height: 2.2;
  color: #333;
  font-size: 18px;
  letter-spacing: 0.5px;
}

.content-para {
  margin: 8px 0;
  line-height: 2;
  text-indent: 2em;
  word-break: break-word;
}

/* EPUB 插画：正文占位标记渲染出的图片 */
.reader-illustration {
  display: block;
  max-width: 100%;
  height: auto;
  margin: 16px auto;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
}

/* 插画行：去掉段落缩进，避免图片被缩进 */
.content-para.content-image-line {
  text-indent: 0;
}

/* 分页：隐藏非当前页的内容块（全部渲染仅用于测量高度） */
.content-para.is-hidden-page {
  display: none;
}

/* 字体大小设置 */
.book-reader.font-size-small .content-area {
  font-size: 16px;
  transition: none;
}

.book-reader.font-size-medium .content-area {
  font-size: 18px;
  transition: none;
}

.book-reader.font-size-large .content-area {
  font-size: 20px;
  transition: none;
}

/* 字体大小影响标题 */
.book-reader.font-size-small .book-title {
  font-size: 24px;
  transition: none;
}

.book-reader.font-size-small .chapter-title {
  font-size: 18px;
  transition: none;
}

.book-reader.font-size-medium .book-title {
  font-size: 28px;
  transition: none;
}

.book-reader.font-size-medium .chapter-title {
  font-size: 22px;
  transition: none;
}

.book-reader.font-size-large .book-title {
  font-size: 32px;
  transition: none;
}

.book-reader.font-size-large .chapter-title {
  font-size: 26px;
  transition: none;
}

/* 分页控件 */
.pagination {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 40px 30px;
  background-color: #f8f6f0;
  border-top: 1px solid #e0e0e0;
  gap: 15px;
}

/* 分页按钮组 */
.pagination-buttons {
  display: flex;
  align-items: center;
  gap: 20px;
}

.page-btn {
  padding: 12px 24px;
  border: 1px solid #dcdfe6;
  border-radius: 25px;
  background-color: #fff;
  color: #606266;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.page-btn:hover:not(:disabled) {
  background-color: #409eff;
  color: #fff;
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  transform: translateY(-1px);
}

.page-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  box-shadow: none;
}

.page-info {
  font-size: 16px;
  color: #606266;
  font-weight: 500;
}

/* 页码跳转功能 */
.page-jump {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 5px;
}

.jump-text {
  font-size: 14px;
  color: #606266;
}

.jump-input {
  width: 80px;
  padding: 10px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  text-align: center;
  transition: all 0.3s ease;
}

.jump-input:focus {
  outline: none;
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.jump-btn {
  padding: 10px 20px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #fff;
  color: #606266;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.jump-btn:hover:not(:disabled) {
  background-color: #409eff;
  color: #fff;
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.jump-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  box-shadow: none;
}
/* ========== 阅读组件移动端响应式 ========== */
@media (max-width: 768px) {
  .reader-header {
    padding: 20px 16px 14px;
  }

  .book-title {
    font-size: 22px;
  }

  .chapter-title {
    font-size: 18px;
  }

  .reader-meta {
    padding: 12px 16px 16px;
    font-size: 14px;
  }

  .reader-content {
    padding: 20px 16px;
    min-height: 320px;
  }

  .content-area {
    font-size: 17px;
    line-height: 2;
  }

  .book-reader.font-size-large .content-area {
    font-size: 18px;
  }

  .pagination {
    padding: 16px 16px 24px;
  }

  .page-jump {
    flex-wrap: wrap;
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .book-title {
    font-size: 20px;
  }

  .chapter-title {
    font-size: 16px;
  }

  .pagination-buttons {
    gap: 10px;
  }

  .page-btn {
    padding: 10px 18px;
  }
}
</style>
