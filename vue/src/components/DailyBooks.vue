<template>
  <div class="daily-books">
    <h2 class="title">{{ title }}</h2>

    <!-- 小说列表，使用动态列数 -->
    <div class="books-grid" :style="{ gridTemplateColumns: `repeat(${columns}, 1fr)` }">
      <div
        v-for="book in bookList"
        :key="book.id"
        class="book-item"
        @click="toBookDetail(book.id)"
      >
        <img :src="`http://localhost:8081/novel/cover/${encodeURIComponent(book.cover)}`" alt="小说封面" class="book-cover">
        <p class="book-name">{{ book.bookMainName }}</p>
        <div class="book-footer-row">
          <p class="book-author">作者：{{ book.author }}</p>
          <BookCardMoreMenu
            v-if="enableDislike"
            :book="book"
            @open-dislike="openDislike"
          />
        </div>
      </div>
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import DislikeBookDialog from '@/components/DislikeBookDialog.vue'
import BookCardMoreMenu from '@/components/BookCardMoreMenu.vue'

defineProps({
  bookList: { type: Array, default: () => [] },
  title: { type: String, default: '' },
  totalCount: { type: Number, default: 0 },
  columns: { type: Number, default: 2 },
  enableDislike: { type: Boolean, default: false }
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
  console.log('点击了小说，ID是：', bookId)
  console.log('路由实例：', router)

  // 记录最近点击的小说 ID 到 localStorage
  localStorage.setItem('lastClickedNovelId', String(bookId))

  router.push({
    name: 'BookDetail',
    params: { id: bookId }
  })
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
  background: linear-gradient(180deg, #f8a555 0%, #ff8c42 100%);
  border-radius: 2px;
}

/* 核心：小说列表布局 */
.books-grid {
  display: grid;
  gap: 12px;
  padding: 0;
  overflow: visible;
}

.book-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 12px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  box-sizing: border-box;
  position: relative;
  overflow: visible;
}

.book-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #f8a555 0%, #ff8c42 100%);
  transform: scaleX(0);
  transition: transform 0.3s ease;
}

.book-item:hover::before {
  transform: scaleX(1);
}

.book-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  border-color: #f8a555;
  z-index: 10;
}

.book-footer-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  width: 100%;
  min-height: 28px;
}

/* 封面图 */
.book-cover {
  width: 100px;
  height: 145px;
  object-fit: cover;
  margin-bottom: 10px;
  border-radius: 6px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.book-item:hover .book-cover {
  transform: scale(1.05);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

/* 图书名 */
.book-name {
  font-size: 13px;
  font-weight: 600;
  margin: 0 0 5px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
  text-align: center;
  color: #1a1a1a;
  transition: color 0.3s ease;
}

.book-item:hover .book-name {
  color: #f8a555;
}

/* 作者信息 */
.book-author {
  font-size: 12px;
  color: #999;
  margin: 0;
  text-align: left;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.3s ease;
}

.book-item:hover .book-author {
  color: #666;
}

/* 加载更多按钮 */
.load-more-btn {
  margin-top: 32px;
  padding: 14px 40px;
  background: linear-gradient(135deg, #f8a555 0%, #ff8c42 100%);
  color: #fff;
  border: none;
  border-radius: 24px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(248, 165, 85, 0.3);
  letter-spacing: 0.5px;
}

.load-more-btn:hover {
  background: linear-gradient(135deg, #ff8c42 0%, #f8a555 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(248, 165, 85, 0.4);
}

.load-more-btn:active {
  transform: translateY(0);
}

@media (max-width: 768px) {
  .books-grid {
    gap: 14px;
  }
  
  .book-cover {
    width: 130px;
    height: 180px;
  }
  
  .book-item {
    padding: 14px;
  }
  
  .title {
    font-size: 20px;
    margin-bottom: 20px;
  }
}
</style>
