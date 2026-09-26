<template>
  <div class="home-page">
    <!-- 1. 导航栏 -->
    <Navbar />

    <!-- 2. 轮播图 -->
    <div class="banner-container">
      <Banner :bannerList="bannerList" :errorMsg="bannerErrorMsg" />
    </div>

    <!-- 3. 五个分类模块 -->
    <div class="modules-container">
      <section
        v-for="cat in categoryModules"
        :key="cat.name"
        class="category-module"
        :style="{ '--cat-accent': cat.color }"
      >
        <!-- 左：书籍卡片 -->
        <div class="module-left">
          <h2 class="module-title">
            <span class="title-dot"></span>
            {{ cat.name }}
            <span class="title-count">更多</span>
          </h2>
          <div class="module-books">
            <NovelGridCard
              v-for="book in cat.books"
              :key="book.id"
              :book="book"
              @select="onCardSelect"
            />
          </div>
        </div>

        <!-- 右：人气榜单 -->
        <div class="module-right">
          <div class="mini-rank">
            <h3 class="rank-title">{{ cat.name }} · 人气榜</h3>
            <div
              v-for="(item, idx) in cat.rankBooks"
              :key="item.id"
              class="rank-row"
              @click="goToDetail(item.id)"
            >
              <span class="rank-num" :class="'top' + (idx + 1)">{{ idx + 1 }}</span>
              <span class="rank-book-name">{{ item.bookMainName }}</span>
              <span class="rank-read">{{ formatReadCount(item.readCount) }}</span>
            </div>
            <div v-if="cat.rankBooks.length === 0" class="rank-empty">暂无数据</div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Navbar from '@/components/Navbar.vue'
import Banner from '@/components/Banner.vue'
import NovelGridCard from '@/components/NovelGridCard.vue'
import { getAllNovels, getBannerList } from '@/api/novel'
import { backendUrl } from '@/config/env'

const router = useRouter()

// ========== 五个固定分类 ==========
const CATEGORIES = [
  { name: '日常', color: '#e91e8c' },
  { name: '奇幻', color: '#7c3aed' },
  { name: '校园', color: '#0891b2' },
  { name: '冒险', color: '#ea580c' },
  { name: '异世界', color: '#16a34a' }
]

// ========== 轮播图 ==========
const bannerList = ref([])
const bannerErrorMsg = ref('')

// ========== 所有小说数据 ==========
const allBooks = ref([])

/**
 * 判断某本书是否属于指定分类
 * label 字段为逗号分隔的标签字符串，如 "日常,轻松,治愈"
 */
function bookHasLabel(book, labelName) {
  if (!book || !book.label) return false
  const labels = book.label.split(',').map(s => s.trim())
  return labels.includes(labelName)
}

// ========== 五大分类模块（响应式） ==========
const categoryModules = computed(() => {
  return CATEGORIES.map(cat => {
    const sorted = allBooks.value
      .filter(b => bookHasLabel(b, cat.name))
      .sort((a, b) => (b.readCount || 0) - (a.readCount || 0))
    return {
      name: cat.name,
      color: cat.color,
      books: sorted.slice(0, 10),
      rankBooks: sorted.slice(0, 8)
    }
  })
})

// ========== 格式化阅读量 ==========
function formatReadCount(count) {
  if (!count) return '0'
  if (count >= 10000) return (count / 10000).toFixed(1) + '万'
  return String(count)
}

// ========== 跳转详情 ==========
function onCardSelect(book) {
  if (book?.id != null) goToDetail(book.id)
}

function goToDetail(bookId) {
  localStorage.setItem('lastClickedNovelId', String(bookId))
  router.push({ name: 'BookDetail', params: { id: bookId } })
}

// ========== 初始化加载 ==========
onMounted(async () => {
  // 加载轮播图
  try {
    const carouselRes = await getBannerList()
    const carouselData = carouselRes.code === 200 ? carouselRes.data : carouselRes
    if (Array.isArray(carouselData) && carouselData.length > 0) {
      bannerList.value = carouselData.map(item => ({
        id: item.id,
        image: `${backendUrl('/novel/cover')}/${encodeURIComponent(item.cover)}`,
        title: ''
      }))
    } else {
      bannerList.value = [
        { id: 1, image: 'https://picsum.photos/1200/400?random=1', title: '' },
        { id: 2, image: 'https://picsum.photos/1200/400?random=2', title: '' }
      ]
    }
  } catch (err) {
    console.error('轮播图加载失败:', err)
    bannerErrorMsg.value = err.message || '轮播图数据加载失败'
    bannerList.value = [
      { id: 1, image: 'https://picsum.photos/1200/400?random=1', title: '' },
      { id: 2, image: 'https://picsum.photos/1200/400?random=2', title: '' }
    ]
  }

  // 加载所有小说（一次加载，前端按分类筛选）
  try {
    const novelRes = await getAllNovels()
    const novelData = novelRes.code === 200 ? novelRes.data : novelRes
    allBooks.value = Array.isArray(novelData) ? novelData : []
  } catch (err) {
    console.error('加载小说失败', err)
    allBooks.value = []
  }
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8f9fc 0%, #f5f7fa 100%);
  padding-top: 60px;
}

/* ========== 轮播图 ========== */
.banner-container {
  max-width: 1400px;
  margin: 0 auto 20px;
  padding: 0 20px;
  height: 380px;
  position: relative;
  z-index: 1;
}

/* ========== 分类模块容器 ========== */
.modules-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px 40px;
}

.category-module {
  display: flex;
  gap: 20px;
  margin-bottom: 32px;
  background: #fff;
  border-radius: 16px;
  padding: 24px 28px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.3s ease;
}

.category-module:hover {
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}

/* ========== 左：书籍卡片区 ========== */
.module-left {
  flex: 1;
  min-width: 0;
}

.module-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f0f0f0;
}

.title-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--cat-accent);
  box-shadow: 0 0 8px var(--cat-accent);
}

.title-count {
  font-size: 12px;
  font-weight: 500;
  color: #999;
  background: #f5f5f5;
  padding: 2px 10px;
  border-radius: 10px;
  margin-left: auto;
}

.module-books {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

/* ========== 右：人气榜单 ========== */
.module-right {
  width: 240px;
  flex-shrink: 0;
}

.mini-rank {
  background: linear-gradient(135deg, #fafbfc 0%, #f8f9fc 100%);
  border-radius: 12px;
  padding: 16px 14px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  height: 100%;
}

.rank-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 2px solid var(--cat-accent);
}

.rank-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 6px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.rank-row:hover {
  background: rgba(0, 0, 0, 0.03);
}

.rank-num {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #999;
  background: #f0f0f0;
  border-radius: 6px;
  flex-shrink: 0;
}

.rank-num.top1 { background: #e91e8c; color: #fff; }
.rank-num.top2 { background: #f472b6; color: #fff; }
.rank-num.top3 { background: #f9a8d4; color: #fff; }

.rank-book-name {
  flex: 1;
  font-size: 13px;
  color: #444;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-read {
  font-size: 11px;
  color: #bbb;
  flex-shrink: 0;
}

.rank-empty {
  text-align: center;
  color: #ccc;
  font-size: 13px;
  padding: 20px 0;
}

/* ========== 响应式 ========== */
@media (max-width: 1200px) {
  .category-module {
    flex-direction: column;
    padding: 20px;
  }

  .module-right {
    width: 100%;
  }

  .mini-rank {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
  }

  .mini-rank .rank-title {
    width: 100%;
    margin-bottom: 6px;
  }

  .mini-rank .rank-row {
    flex: 1;
    min-width: 160px;
  }
}

@media (max-width: 900px) {
  .module-books {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .banner-container {
    height: 260px;
  }
}

@media (max-width: 640px) {
  .home-page {
    padding-top: 50px;
  }

  .banner-container {
    height: 200px;
    padding: 0 12px;
    margin-bottom: 12px;
  }

  .modules-container {
    padding: 0 12px 30px;
  }

  .category-module {
    padding: 16px;
    margin-bottom: 20px;
  }

  .module-books {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .module-title {
    font-size: 18px;
    margin-bottom: 14px;
  }
}
</style>
