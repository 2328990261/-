<template>
  <div class="home-page">
    <!-- 1. 导航栏组件 -->
    <Navbar />

    <!-- 2. 轮播图组件 -->
    <div class="banner-container">
      <Banner :bannerList="bannerList" :errorMsg="bannerErrorMsg" />
    </div>

    <!-- 3. 标签栏组件（使用 LibraryTagSearch 支持多标签选择） -->
    <div class="tag-bar-container">
      <LibraryTagSearch @selectTags="handleTagsSelect" />
    </div>

    <!-- 4. 核心内容：书籍列表 + 榜单 -->
    <div class="container">
      <div class="tag-content">
        <!-- 左侧：书籍列表区域 -->
        <div class="books-section">
          <DailyBooks
            :bookList="displayBooks"
            :title="currentTagName"
            :total-count="allBooks.length"
            :columns="4"
            @load-more="loadMore"
          />
        </div>

        <!-- 右侧：榜单区域（核心修改：只传rankList和tagName） -->
        <div class="rank-section">
          <RankList
            :rankList="rankList"
            :tagName="currentTagName"
            :fixedOffset="500"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 导入组件
import Navbar from '@/components/Navbar.vue'
import Banner from '@/components/Banner.vue'
import LibraryTagSearch from '@/components/LibraryTagSearch.vue'
import DailyBooks from '@/components/DailyBooks.vue'
import RankList from '@/components/RankList.vue'

// Vue核心API
import { ref, computed, onMounted } from 'vue'

// 导入接口（修正：确保导入的接口与调用一致）
import { getAllNovels, getNovelsByTag, getNovelsByLabels, getBannerList } from '@/api/novel'

// ========== 数据定义 ==========
// 1. 轮播图数据及错误信息
const bannerList = ref([])
const bannerErrorMsg = ref('')

// 2. 当前选中的标签列表（支持多标签）
const selectedTags = ref([])

// 3. 当前选中的标签名称（用于显示）
const currentTagName = computed(() => {
  return selectedTags.value.length > 0 ? selectedTags.value.join('、') : '全部'
})

// 4. 小说数据
const allBooks = ref([])
const pageSize = ref(10)
const displayBooks = computed(() => allBooks.value.slice(0, pageSize.value))

// 5. 榜单数据
const rankList = ref([])

// ========== 方法定义 ==========
// 1. 标签切换方法（支持多标签筛选）
const handleTagsSelect = async (tags) => {
  console.log('HomeView收到的标签:', tags) // 调试日志
  selectedTags.value = tags
  pageSize.value = 10

  if (tags.length === 0) {
    await loadAllNovels()
    return
  }

  try {
    console.log('准备调用getNovelsByLabels，参数:', tags) // 调试日志
    const res = await getNovelsByLabels(tags)
    console.log('getNovelsByLabels返回结果:', res) // 调试日志
    const novelData = res.code === 200 ? res.data : []
    allBooks.value = Array.isArray(novelData) ? novelData.sort((a, b) => b.readCount - a.readCount) : []
    rankList.value = allBooks.value.slice(0, 10)
  } catch (err) {
    console.error('获取标签小说失败', err)
    allBooks.value = []
    rankList.value = []
  }
}

// 2. 加载所有小说
const loadAllNovels = async () => {
  try {
    const res = await getAllNovels()
    const novelData = res.code === 200 ? res.data : []
    allBooks.value = Array.isArray(novelData) ? novelData.sort((a, b) => b.readCount - a.readCount) : []
    rankList.value = allBooks.value.slice(0, 10)
  } catch (err) {
    console.error('加载所有小说失败', err)
    allBooks.value = []
    rankList.value = []
  }
}

// 3. 加载更多
const loadMore = () => {
  pageSize.value += 10
}

// 4. 加载轮播图
const loadBanner = async () => {
  try {
    const res = await getBannerList()
    const bannerData = res.code === 200 ? res.data : []
    bannerList.value = Array.isArray(bannerData) ? bannerData : []
  } catch (err) {
    console.error('轮播图加载失败:', err)
    bannerErrorMsg.value = err.message || '轮播图数据加载失败'
    bannerList.value = [
      {id: 1, image: 'https://picsum.photos/1200/400?random=1', title: '兜底小说1'},
      {id: 2, image: 'https://picsum.photos/1200/400?random=2', title: '兜底小说2'}
    ]
  }
}

// ========== 页面初始化 ==========
onMounted(async () => {
  // 1. 加载轮播图数据
  try {
    const carouselRes = await getBannerList()
    // console.log('后端返回的轮播数据:', carouselRes)
    const carouselData = carouselRes.code === 200 ? carouselRes.data : carouselRes

    if (Array.isArray(carouselData) && carouselData.length > 0) {
      // 核心修复：使用完整的后端URL
      const tempBannerList = carouselData.map(item => ({
        id: item.id,
        image: `http://localhost:8081/novel/cover/${encodeURIComponent(item.cover)}`,
        title: item.bookMainName || '热门小说'
      }))
      // 方式1：用数组解构强制更新（最稳妥）
      bannerList.value = [...tempBannerList]
      console.log('轮播图最终数据:', bannerList.value)
    } else {
      bannerList.value = [
        {id: 1, image: 'https://picsum.photos/1200/400?random=1', title: '兜底小说1'},
        {id: 2, image: 'https://picsum.photos/1200/400?random=2', title: '兜底小说2'}
      ]
    }
  } catch (err) {
    console.error('轮播图加载失败:', err)
    bannerErrorMsg.value = err.message || '轮播图数据加载失败'
    bannerList.value = [
      {id: 1, image: 'https://picsum.photos/1200/400?random=1', title: '兜底小说1'},
      {id: 2, image: 'https://picsum.photos/1200/400?random=2', title: '兜底小说2'}
    ]
  }

  // 2. 加载所有小说
  try {
    const novelRes = await getAllNovels()
    const novelData = novelRes.code === 200 ? novelRes.data : novelRes
    allBooks.value = Array.isArray(novelData) ? novelData.sort((a, b) => b.readCount - a.readCount) : []
    rankList.value = allBooks.value.slice(0, 10)
  } catch (err) {
    console.error('加载所有小说失败', err)
    allBooks.value = []
    rankList.value = []
  }
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8f9fc 0%, #f5f7fa 100%);
  padding-top: 60px;
}

/* 轮播图容器 */
.banner-container {
  max-width: 1400px;
  margin: 0 auto 16px;
  padding: 0 20px;
  height: 380px;
  position: relative;
  z-index: 1;
}

/* 标签栏容器 */
.tag-bar-container {
  max-width: 1400px;
  margin: 0 auto 16px;
  padding: 0 20px;
  position: relative;
  z-index: 2;
}

/* 核心内容容器 */
.container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px 40px;
  position: relative;
}

.tag-content {
  display: flex;
  gap: 16px;
  position: relative;
  align-items: flex-start;
}

/* 书籍列表区域 */
.books-section {
  flex: 1;
  min-width: 0;
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.04);
}

/* 解决 flex 布局下卡片变形问题 */
.books-section, .rank-section {
  overflow: visible;
}

/* 榜单区域 */
.rank-section {
  width: 300px;
  flex-shrink: 0;
  background: linear-gradient(135deg, #fff 0%, #fafbfc 100%);
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(248, 165, 85, 0.1);
  position: sticky;
  top: 76px;
  align-self: flex-start;
}

/* 响应式适配 */
@media (max-width: 1024px) {
  .tag-content {
    flex-direction: column;
  }
  
  .rank-section {
    width: 100%;
    position: static;
    max-height: none;
  }

  .banner-container {
    height: 240px;
  }
  
  .container {
    max-width: 100%;
  }
}

@media (max-width: 768px) {
  .home-page {
    padding-top: 50px;
  }

  .banner-container {
    height: 200px;
    margin-bottom: 12px;
    padding: 0 12px;
  }
  
  .tag-bar-container {
    margin-bottom: 12px;
    padding: 12px;
  }
  
  .container {
    padding: 0 12px 30px;
  }
  
  .tag-content {
    gap: 12px;
  }
  
  .books-section, .rank-section {
    padding: 16px;
  }
}
</style>
