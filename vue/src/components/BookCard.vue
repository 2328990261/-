<template>
  <div class="book-card" :key="book.id || book.title">
    <div class="card-cover">
      <img :src="book.cover" :alt="book.title || '小说封面'" />
    </div>
    <div class="card-title">{{ book.title }}</div>
    <div class="card-meta">字数：{{ book.wordCount }}</div>
  </div>
</template>

<script setup>
const props = defineProps({
  book: {
    type: Object,
    required: true,
    default: () => ({ title: '未知小说', wordCount: '0万字', cover: '' })
  }
})
</script>

<style>
/* 卡片基础样式*/
.book-card {
  width: 160px;
  height: 240px;
  background-color: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  padding: 8px;
  margin: 12px;
  transition: all 0.3s ease;
  cursor: pointer;
  box-sizing: border-box;
  display: inline-block;
  vertical-align: top;

}

/* hover  */
.book-card:hover {
  transform: translateY(-6px) scale(1.05) ;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12) ;
  z-index: 10 ; /* 悬停时置顶，避免被其他卡片遮挡 */
}

/* 封面容器 */
.book-card .card-cover {
  width: 100%;
  height: 180px;
  overflow: hidden;
  border-radius: 4px;
  margin-bottom: 8px;
}

.book-card .card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.book-card:hover .card-cover img {
  transform: scale(1.02);
}

/* 标题和字数样式 */
.book-card .card-title {
  font-size: 14px;
  color: #333 ;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.book-card .card-meta {
  font-size: 12px;
  color: #999;
}
</style>
