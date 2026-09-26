<template>
  <div class="daily-books">
    <h2 class="title">{{ title }}</h2>

    <div class="books-grid" :style="{ gridTemplateColumns: `repeat(${gridColumns}, minmax(0, 1fr))` }">
      <NovelGridCard
        v-for="book in bookList"
        :key="book.id"
        :book="book"
        :show-more-menu="enableDislike"
        @select="onCardSelect"
        @open-dislike="openDislike"
      />
    </div>

    <!-- 滚动触发哨兵元素 -->
    <div
      ref="sentinelRef"
      class="scroll-sentinel"
      v-if="bookList.length < totalCount"
    ></div>

    <!-- 加载状态指示器 -->
    <div class="loading-indicator" v-if="loading">
      <span class="loading-spinner"></span>
      <span>加载中...</span>
    </div>

    <DislikeBookDialog
      v-model="dislikeVisible"
      :novel-id="dislikeBook?.id"
      :author="dislikeBook?.author"
      :label="dislikeBook?.label"
      @saved="onDislikeDialogSaved"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import DislikeBookDialog from '@/components/DislikeBookDialog.vue'
import NovelGridCard from '@/components/NovelGridCard.vue'

const props = defineProps({
  bookList: { type: Array, default: () => [] },
  title: { type: String, default: '' },
  totalCount: { type: Number, default: 0 },
  columns: { type: Number, default: 2 },
  enableDislike: { type: Boolean, default: false }
})

const emit = defineEmits(['load-more', 'dislike-saved'])

const viewportW = ref(typeof window !== 'undefined' ? window.innerWidth : 1200)
function onResize() {
  viewportW.value = window.innerWidth
}
onMounted(() => {
  onResize()
  window.addEventListener('resize', onResize, { passive: true })
})
onUnmounted(() => window.removeEventListener('resize', onResize))

const gridColumns = computed(() => {
  const c = props.columns
  const w = viewportW.value
  if (w < 480) return Math.min(2, c)
  if (w < 640) return Math.min(3, c)
  if (w < 900) return Math.min(4, c)
  if (w < 1200) return Math.min(5, c)
  return c
})

// ========== 无限滚动 ==========
const sentinelRef = ref(null)
const loading = ref(false)
let observer = null

const hasMore = computed(() => props.bookList.length < props.totalCount)

function setupObserver() {
  if (observer) {
    observer.disconnect()
    observer = null
  }
  if (!sentinelRef.value) return

  observer = new IntersectionObserver(
    (entries) => {
      const entry = entries[0]
      if (entry.isIntersecting && hasMore.value && !loading.value) {
        loading.value = true
        emit('load-more')
        nextTick(() => {
          loading.value = false
        })
      }
    },
    { rootMargin: '200px' }
  )
  observer.observe(sentinelRef.value)
}

// 书籍列表变化或哨兵DOM变化时重新绑定
watch([() => props.bookList.length, sentinelRef], () => {
  nextTick(() => setupObserver())
})

onMounted(() => {
  nextTick(() => setupObserver())
})

onUnmounted(() => {
  if (observer) {
    observer.disconnect()
    observer = null
  }
})

// ========== 不感兴趣 ==========

const dislikeVisible = ref(false)
const dislikeBook = ref(null)

const openDislike = (book) => {
  dislikeBook.value = book
  dislikeVisible.value = true
}

const onDislikeDialogSaved = () => {
  dislikeBook.value = null
  emit('dislike-saved')
}

const router = useRouter()
const toBookDetail = (bookId) => {
  localStorage.setItem('lastClickedNovelId', String(bookId))
  router.push({
    name: 'BookDetail',
    params: { id: bookId }
  })
}

const onCardSelect = (book) => {
  if (book?.id != null) toBookDetail(book.id)
}
</script>

<style scoped>
.daily-books {
  padding: 0;
  position: relative;
}

.title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 24px;
  color: #1a1a1a;
  position: relative;
  padding-left: 12px;
}

.title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 24px;
  background: linear-gradient(180deg, #e91e8c 0%, #f472b6 100%);
  border-radius: 2px;
}

.books-grid {
  display: grid;
  gap: 20px 18px;
  padding: 0;
  overflow: visible;
  align-items: stretch;
}

/* 滚动哨兵元素（不可见，仅用于检测） */
.scroll-sentinel {
  height: 1px;
  width: 100%;
}

/* 加载状态指示器 */
.loading-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 24px 0;
  color: #999;
  font-size: 14px;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #e5e7eb;
  border-top-color: #e91e8c;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .books-grid {
    gap: 16px 12px;
  }

  .title {
    font-size: 20px;
    margin-bottom: 20px;
  }
}
</style>
