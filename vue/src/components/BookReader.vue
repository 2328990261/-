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
      <div v-else-if="currentPageContent.length > 0" class="content-area">
        <p v-for="(para, idx) in currentPageContent" :key="idx" class="content-para">
          {{ para }}
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
import { ref, computed, watch, nextTick } from 'vue'

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

// 将章节内容分割为行数组
const contentLines = computed(() => {
  if (!props.chapter?.content) return []
  const content = props.chapter.content
  
  // 先按换行符分割
  let lines = content.split('\n').filter(line => line.trim() !== '')
  
  // 如果行数太少（说明是一大段文本），按字数自动分行
  if (lines.length < 20) {
    const allText = lines.join(' ')
    const charsPerLine = 45
    const newLines = []
    let currentPos = 0
    
    while (currentPos < allText.length) {
      let endPos = Math.min(currentPos + charsPerLine, allText.length)
      
      // 在标点符号处断行
      if (endPos < allText.length) {
        const searchEnd = Math.min(endPos + 20, allText.length)
        let bestBreak = endPos
        
        // 优先在句号等处断行
        for (let i = endPos; i < searchEnd; i++) {
          const c = allText.charAt(i)
          if (c === '。' || c === '！' || c === '？') {
            bestBreak = i + 1
            break
          }
        }
        
        // 其次在分号处
        if (bestBreak === endPos) {
          for (let i = endPos; i < searchEnd; i++) {
            if (allText.charAt(i) === '；') {
              bestBreak = i + 1
              break
            }
          }
        }
        
        // 最后在逗号处
        if (bestBreak === endPos) {
          for (let i = endPos; i < searchEnd; i++) {
            const c = allText.charAt(i)
            if (c === '，' || c === '、') {
              bestBreak = i + 1
              break
            }
          }
        }
        
        endPos = bestBreak
      }
      
      const line = allText.substring(currentPos, endPos).trim()
      if (line) newLines.push(line)
      currentPos = endPos
    }
    
    return newLines
  }
  
  // 如果已经有足够的行数，直接返回
  return lines
})

// 计算总页数
const totalPages = computed(() => {
  const lines = contentLines.value.length
  return Math.ceil(lines / props.pageSize)
})

// 计算当前页内容
const currentPageContent = computed(() => {
  const start = (currentPage.value - 1) * props.pageSize
  const end = start + props.pageSize
  return contentLines.value.slice(start, end)
})

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
</style>
