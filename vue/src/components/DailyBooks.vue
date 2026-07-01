<template>
  <div class="daily-books">
    <h2 class="title">{{ title }}</h2>

    <!-- 小说列表，使用动态列数 -->
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

    <!-- 加载更多按钮 -->
    <button
      class="load-more-btn"
      @click="$emit('load-more')"
      v-if="bookList.length < totalCount"
    >
      加载更多
    </button>

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
import { ref, computed, onMounted, onUnmounted } from 'vue'
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

const viewportW = ref(typeof window !== 'undefined' ? window.innerWidth : 1200)
function onResize() {
  viewportW.value = window.innerWidth
}
onMounted(() => {
  onResize()
  window.addEventListener('resize', onResize, { passive: true })
})
onUnmounted(() => window.removeEventListener('resize', onResize))

/** 大屏用 props.columns，窄屏自动减少列数避免卡片挤成一团 */
const gridColumns = computed(() => {
  const c = props.columns
  const w = viewportW.value
  if (w < 480) return Math.min(2, c)
  if (w < 640) return Math.min(3, c)
  if (w < 900) return Math.min(4, c)
  if (w < 1200) return Math.min(5, c)
  return c
})

const emit = defineEmits(['load-more', 'dislike-saved'])

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

/* 加载更多按钮 */
.load-more-btn {
  margin-top: 32px;
  padding: 14px 40px;
  background: linear-gradient(135deg, #e91e8c 0%, #f472b6 100%);
  color: #fff;
  border: none;
  border-radius: 24px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(233, 30, 140, 0.35);
  letter-spacing: 0.5px;
}

.load-more-btn:hover {
  background: linear-gradient(135deg, #db2777 0%, #e91e8c 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(233, 30, 140, 0.45);
}

.load-more-btn:active {
  transform: translateY(0);
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
