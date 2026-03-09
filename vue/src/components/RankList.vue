<template>
  <!-- 新增外层容器用于固定定位，不影响原有结构 -->
  <div class="rank-list-container">
    <div class="rank-list">
      <!-- 只保留榜单标题，删掉周榜/月榜切换按钮 -->
      <h2 class="rank-title">{{ tagName }} · 人气榜单</h2>

      <!-- 直接渲染单个榜单（无需切换） -->
      <div class="rank-card">
        <div class="rank-list-content">
          <!-- 循环渲染榜单，新增点击量显示 -->
          <div class="rank-item" v-for="(item, index) in rankList" :key="item.id" @click="goToDetail(item)">
            <!-- 序号（前3名配色保留） -->
            <div class="rank-num" :class="{ top1: index===0, top2: index===1, top3: index===2 }">
              {{ index + 1 }}
            </div>

            <!-- 前2项显示封面 -->
            <img
              v-if="index < 2"
              :src="`http://localhost:8081/novel/cover/${encodeURIComponent(item.cover)}`"
              alt="榜单封面"
              class="rank-cover"
            >

            <!-- 小说信息 -->
            <div class="rank-info" :class="{ 'info-with-cover': index < 2 }">
              <p class="rank-book-title">{{ item.bookMainName }}</p>
              <p class="rank-book-author">作者：{{ item.author }}</p>
            </div>

            <!-- 新增：右侧显示点击量 -->
            <div class="rank-read-count">
              点击量：{{ formatReadCount(item.readCount) }}
            </div>
          </div>

          <!-- 无数据提示 -->
          <div class="empty-tip" v-if="rankList.length === 0">
            暂无榜单数据
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 接收父组件传的榜单数据和标签名
const props = defineProps({
  rankList: {
    type: Array,
    default: () => []
  },
  tagName: {
    type: String,
    default: '日常'
  },
  // 新增：接收你自定义的固定阈值（单位px），由你自己设置
  fixedOffset: {
    type: Number,
    default: 500 // 默认值，你可以在父组件传入自定义值
  }
})

// 格式化点击量（超过1万显示x万，更友好）
const formatReadCount = (count) => {
  if (!count) return '0'
  if (count >= 10000) {
    return (count / 10000).toFixed(1) + '万'
  }
  return count
}

// 跳转到小说详情页
const goToDetail = (book) => {
  if (book && book.id) {
    // 记录最近点击的小说 ID 到 localStorage
    localStorage.setItem('lastClickedNovelId', String(book.id))
    router.push(`/book/detail/${book.id}`)
  }
}
</script>

<style scoped>
/* 使用position: sticky实现粘性定位 */
.rank-list-container {
  position: sticky;
  top: 80px;
  width: 100%;
  transition: all 0.2s ease;
  z-index: 999;
}

/* 整体容器 */
.rank-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 榜单标题 */
.rank-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  position: relative;
  padding-left: 12px;
  margin: 0 0 12px 0;
}

.rank-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 22px;
  background: linear-gradient(180deg, #f8a555 0%, #ff8c42 100%);
  border-radius: 2px;
}

/* 榜单卡片 */
.rank-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  padding: 0;
}

/* 榜单列表内容 */
.rank-list-content {
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* 榜单项 */
.rank-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-bottom: 1px solid #f5f5f5;
  transition: all 0.3s ease;
  cursor: pointer;
}

.rank-item:last-child {
  border-bottom: none;
}

.rank-item:hover {
  background-color: #fff8f0;
  transform: translateX(2px);
}

/* 榜单序号 */
.rank-num {
  width: 26px;
  height: 26px;
  line-height: 26px;
  text-align: center;
  border-radius: 50%;
  background-color: #e5e5e5;
  color: #666;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}
.rank-num.top1 { background: linear-gradient(135deg, #ffd700 0%, #ffed4e 100%); color: #fff; box-shadow: 0 2px 8px rgba(255, 215, 0, 0.4); }
.rank-num.top2 { background: linear-gradient(135deg, #c0c0c0 0%, #e8e8e8 100%); color: #fff; box-shadow: 0 2px 8px rgba(192, 192, 192, 0.4); }
.rank-num.top3 { background: linear-gradient(135deg, #cd7f32 0%, #e89b5a 100%); color: #fff; box-shadow: 0 2px 8px rgba(205, 127, 50, 0.4); }

/* 封面样式 */
.rank-cover {
  width: 50px;
  height: 70px;
  object-fit: cover;
  border-radius: 4px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  flex-shrink: 0;
}

.rank-item:hover .rank-cover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

/* 小说信息区域 */
.rank-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}

/* 标题样式 */
.rank-book-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin: 0 0 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.3s ease;
}

.rank-item:hover .rank-book-title {
  color: #f8a555;
}

.rank-item:nth-child(n+3) .rank-book-title {
  font-size: 13px;
}

.rank-book-author {
  font-size: 12px;
  color: #999;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 点击量样式 */
.rank-read-count {
  font-size: 12px;
  color: #f8a555;
  font-weight: 600;
  white-space: nowrap;
  flex-shrink: 0;
}

/* 无数据提示 */
.empty-tip {
  text-align: center;
  font-size: 14px;
  color: #999;
  padding: 30px 0;
}

/* 手机端适配 */
@media (max-width: 768px) {
  .rank-list-container {
    top: 20px;
  }
  
  .rank-card { 
    padding: 0; 
  }
  
  .rank-item {
    padding: 10px;
  }
  
  .rank-cover { 
    width: 45px; 
    height: 63px; 
  }
  
  .rank-read-count { 
    font-size: 11px; 
  }
  
  .rank-title {
    font-size: 18px;
  }
}
</style>
