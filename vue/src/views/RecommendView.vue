<template>
  <div class="recommend-container">
    <Navbar />

    <!-- 未登录：与书库一样的展示（标签 + 书单） -->
    <div v-if="!isLoggedIn" class="recommend-content recommend-as-library">
      <h1 class="page-title">推荐</h1>
      <LibraryTagSearch @selectTags="handleLibraryTagsSelect" />
      <DailyBooks
        v-if="libraryDisplayBooks.length > 0"
        :book-list="libraryDisplayBooks"
        :title="librarySelectedTags.length > 0 ? `${librarySelectedTags.join('、')}小说` : '全部小说'"
        :total-count="libraryAllBooks.length"
        :columns="5"
        @load-more="libraryLoadMore"
      />
      <div v-if="libraryDisplayBooks.length === 0" class="empty-tip">
        {{ librarySelectedTags.length > 0 ? `暂无${librarySelectedTags.join('、')}标签的小说` : '暂无小说' }}
      </div>
    </div>

    <!-- 已登录：推荐结果 + 不满意？ -->
    <div v-else class="recommend-content">
      <div v-if="recommendedBooks.length > 0" class="recommend-actions">
        <button class="dissatisfied-btn" @click="showSortDialog">
          不满意？
        </button>
      </div>

      <div v-else-if="!hasLoadedOnce && !loading" class="recommend-actions">
        <button class="recommend-btn" @click="fetchRecommendByCollection">
          <span class="btn-icon">🎯</span>
          <span>获取推荐</span>
        </button>
      </div>

      <div v-if="recommendedBooks.length > 0" class="books-grid">
        <div
          v-for="book in recommendedBooks"
          :key="book.id"
          class="book-card"
          @click="goToDetail(book.id)"
        >
          <div class="book-cover">
            <img
              :src="`http://localhost:8081/novel/cover/${encodeURIComponent(book.cover)}`"
              :alt="book.bookMainName"
              @error="handleImageError"
            />
          </div>
          <div class="book-info">
            <h3 class="book-title">{{ book.bookMainName }}</h3>
            <p class="book-author">{{ book.author }}</p>
            <div class="book-tags">
              <span
                v-for="(tag, index) in getBookTags(book.label)"
                :key="index"
                class="tag"
              >
                {{ tag }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="!loading && hasLoadedOnce" class="empty-state">
        <p>暂无匹配书籍，请到个人中心调整阅读偏好标签</p>
        <router-link to="/user/center" class="link-center">去个人中心</router-link>
      </div>

      <div v-else-if="!loading && !hasPreferenceTags" class="empty-state">
        <p>请先在个人中心设置阅读偏好（收藏排序标签）</p>
        <router-link to="/user/center" class="link-center">去个人中心设置</router-link>
      </div>

      <div v-if="loading" class="loading-state">
        <p>正在为您推荐...</p>
      </div>
    </div>

    <!-- 不满意？→ 选择 收藏排序 / 自定义排序 -->
    <div v-if="showDialog" class="dialog-overlay" @click="closeDialog">
      <div class="dialog-content sort-dialog" @click.stop>
        <h2>选择推荐方式</h2>
        <p class="dialog-desc">与个人中心阅读偏好一致，任选一种方式</p>

        <div class="sort-options">
          <button class="sort-option sort-option-collection" @click="chooseCollectionSort">
            <div class="option-icon-wrap">
              <span class="option-icon">📚</span>
            </div>
            <div class="option-body">
              <div class="option-title">收藏排序</div>
              <div class="option-desc">按个人中心「收藏排序」标签推荐</div>
            </div>
          </button>

          <button class="sort-option sort-option-custom" @click="openCustomTagDialog">
            <div class="option-icon-wrap">
              <span class="option-icon">⭐</span>
            </div>
            <div class="option-body">
              <div class="option-title">自定义排序</div>
              <div class="option-desc">选择标签后保存到个人中心并推荐</div>
            </div>
          </button>
        </div>

        <button class="close-btn" @click="closeDialog">取消</button>
      </div>
    </div>

    <!-- 自定义排序：标签栏（与个人中心同一套规则） -->
    <div v-if="showTagDialog" class="dialog-overlay" @click="closeTagDialog">
      <div class="dialog-content tag-dialog" @click.stop>
        <h2>选择偏好标签</h2>
        <p class="dialog-desc">与个人中心「自定义排序」同步，五大标签选1个，其他最多4个</p>

        <div class="tag-categories">
          <div class="category-section">
            <h5>五大标签（选择1个）</h5>
            <div class="category-tags">
              <span
                v-for="tag in firstRowTags"
                :key="tag"
                class="category-tag"
                :class="{ selected: customSelectedTags.includes(tag) }"
                @click="toggleCustomTag(tag)"
              >
                {{ tag }}
              </span>
            </div>
          </div>
          <div class="category-section">
            <h5>其他标签（最多选择4个）</h5>
            <div class="category-tags">
              <span
                v-for="tag in otherTags"
                :key="tag"
                class="category-tag"
                :class="{ selected: customSelectedTags.includes(tag) }"
                @click="toggleCustomTag(tag)"
              >
                {{ tag }}
              </span>
            </div>
          </div>
        </div>

        <div class="dialog-buttons">
          <button class="close-btn" @click="closeTagDialog">取消</button>
          <button class="confirm-tags-btn" @click="confirmCustomTagsAndRecommend">
            确认并推荐
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import Navbar from '@/components/Navbar.vue'
import LibraryTagSearch from '@/components/LibraryTagSearch.vue'
import DailyBooks from '@/components/DailyBooks.vue'
import { getAllNovels, getNovelsByLabels } from '@/api/novel'

const router = useRouter()

// 是否已登录（有 token 视为已登录）
const isLoggedIn = computed(() => !!localStorage.getItem('token'))

// ---------- 未登录时：与书库一致的数据与逻辑 ----------
const libraryAllBooks = ref([])
const libraryPageSize = ref(50)
const librarySelectedTags = ref([])
const libraryDisplayBooks = computed(() => libraryAllBooks.value.slice(0, libraryPageSize.value))

const handleLibraryTagsSelect = async (tags) => {
  librarySelectedTags.value = tags
  libraryPageSize.value = 50
  if (tags.length === 0) {
    await libraryLoadAllNovels()
    return
  }
  try {
    const res = await getNovelsByLabels(tags)
    const novelData = res.code === 200 ? res.data : []
    libraryAllBooks.value = Array.isArray(novelData) ? novelData.sort((a, b) => (b.readCount || 0) - (a.readCount || 0)) : []
  } catch (err) {
    console.error('获取标签小说失败', err)
    libraryAllBooks.value = []
  }
}

const libraryLoadAllNovels = async () => {
  try {
    const res = await getAllNovels()
    const novelData = res.code === 200 ? res.data : []
    libraryAllBooks.value = Array.isArray(novelData) ? novelData.sort((a, b) => (b.readCount || 0) - (a.readCount || 0)) : []
  } catch (err) {
    console.error('获取所有小说失败', err)
    libraryAllBooks.value = []
  }
}

const libraryLoadMore = () => {
  libraryPageSize.value += 50
}

// 与个人中心一致的标签分类
const firstRowTags = ['日常', '奇幻', '校园', '冒险', '异世界']
const otherTags = [
  '轻松', '搞笑', '治愈', '致郁', '甜宠', '热血', '恋爱', '成长',
  '智斗', '悬疑', '推理', '心理惊悚', '战斗', '竞技', '基建',
  '宫廷', '虚拟网游', '现实题材', '种田文', '转生', '穿越', '魔法',
  '全年龄', '轻百'
]

const showDialog = ref(false)
const showTagDialog = ref(false)
const loading = ref(false)
const recommendedBooks = ref([])
const hasLoadedOnce = ref(false)

// 从个人中心同步的阅读偏好（与 GET /api/user/behavior/preference/tags 一致）
const preferenceTagsFromCenter = ref({
  collection: [],
  custom: []
})

// 当前推荐使用的排序类型
const currentSortType = ref('collection')

// 自定义排序弹窗里已选标签（与个人中心选择规则一致）
const customSelectedTags = ref([])

const getUserId = () => {
  try {
    const userInfoStr = localStorage.getItem('userInfo')
    if (userInfoStr && userInfoStr !== 'undefined') {
      const userInfo = JSON.parse(userInfoStr)
      if (userInfo && userInfo.id) return userInfo.id
    }
    const uid = localStorage.getItem('userId')
    if (uid) return Number(uid)
  } catch (e) {}
  return 1
}

// 当前推荐依据文案
const currentSortLabel = computed(() => {
  return currentSortType.value === 'collection' ? '收藏排序' : '自定义排序'
})

// 当前展示的标签（与个人中心显示一致）
const currentDisplayTags = computed(() => {
  if (currentSortType.value === 'collection') {
    return preferenceTagsFromCenter.value.collection
  }
  return preferenceTagsFromCenter.value.custom
})

// 是否已有任意偏好标签（用于提示去个人中心设置）
const hasPreferenceTags = computed(() => {
  const c = preferenceTagsFromCenter.value.collection
  const u = preferenceTagsFromCenter.value.custom
  return (c && c.length > 0) || (u && u.length > 0)
})

// 加载个人中心阅读偏好（与个人中心同一接口）
const loadPreferenceTagsFromCenter = async () => {
  const userId = getUserId()
  try {
    const res = await axios.get(
      `http://localhost:8081/api/user/behavior/preference/tags?userId=${userId}`
    )
    const list = res.data && Array.isArray(res.data) ? res.data : []
    const collection = list
      .filter((t) => t.tagType === 'collection')
      .sort((a, b) => (a.tagOrder || 0) - (b.tagOrder || 0))
      .map((t) => t.tagName)
    const custom = list
      .filter((t) => t.tagType === 'custom')
      .sort((a, b) => (a.tagOrder || 0) - (b.tagOrder || 0))
      .map((t) => t.tagName)
    preferenceTagsFromCenter.value = { collection, custom }
  } catch (err) {
    console.error('加载阅读偏好失败:', err)
    preferenceTagsFromCenter.value = { collection: [], custom: [] }
  }
}

// 使用收藏排序拉取推荐（默认）
const fetchRecommendByCollection = async () => {
  await getRecommendations('collection')
}

const getRecommendations = async (sortType) => {
  showDialog.value = false
  showTagDialog.value = false
  loading.value = true
  currentSortType.value = sortType

  try {
    const userId = getUserId()
    const response = await axios.get('http://localhost:8081/api/recommend/books', {
      params: { userId, sortType }
    })

    if (response.data.code === 200) {
      recommendedBooks.value = response.data.data || []
      hasLoadedOnce.value = true
      if (recommendedBooks.value.length === 0) {
        alert('没有找到匹配的书籍，请到个人中心调整阅读偏好标签')
      }
    } else {
      const msg = response.data.msg || '推荐失败'
      if (msg.includes('请先设置')) {
        if (confirm(msg + '，是否前往个人中心设置？')) {
          router.push('/user/center')
        }
      } else {
        alert(msg)
      }
    }
  } catch (error) {
    console.error('推荐错误：', error)
    const msg =
      (error.response && error.response.data && error.response.data.msg) ||
      '推荐失败，请先在个人中心设置阅读偏好'
    if (String(msg).includes('请先设置') && confirm(msg + '，是否前往个人中心？')) {
      router.push('/user/center')
    } else {
      alert(msg)
    }
  } finally {
    loading.value = false
  }
}

const showSortDialog = () => {
  showDialog.value = true
}

const closeDialog = () => {
  showDialog.value = false
}

const chooseCollectionSort = () => {
  getRecommendations('collection')
}

const openCustomTagDialog = () => {
  showDialog.value = false
  customSelectedTags.value = [...preferenceTagsFromCenter.value.custom]
  showTagDialog.value = true
}

const closeTagDialog = () => {
  showTagDialog.value = false
  customSelectedTags.value = []
}

const toggleCustomTag = (tag) => {
  const idx = customSelectedTags.value.indexOf(tag)
  if (firstRowTags.includes(tag)) {
    if (idx > -1) {
      customSelectedTags.value.splice(idx, 1)
    } else {
      customSelectedTags.value = customSelectedTags.value.filter((t) => !firstRowTags.includes(t))
      customSelectedTags.value.push(tag)
    }
  } else {
    if (idx > -1) {
      customSelectedTags.value.splice(idx, 1)
    } else {
      const otherCount = customSelectedTags.value.filter((t) => !firstRowTags.includes(t)).length
      if (otherCount >= 4) {
        alert('其他标签最多选择4个')
        return
      }
      customSelectedTags.value.push(tag)
    }
  }
}

// 确认自定义标签：保存到个人中心（与个人中心同一接口），再按自定义排序推荐
const confirmCustomTagsAndRecommend = async () => {
  if (customSelectedTags.value.length === 0) {
    alert('请至少选择一个标签')
    return
  }

  const userId = getUserId()
  const tagData = customSelectedTags.value.map((tagName, index) => ({
    userId,
    tagName,
    tagType: 'custom',
    tagOrder: index + 1,
    isFirstRow: 0
  }))

  try {
    await axios.post(
      `http://localhost:8081/api/user/behavior/preference/tags?userId=${userId}&tagType=custom`,
      tagData
    )
    preferenceTagsFromCenter.value.custom = [...customSelectedTags.value]
    closeTagDialog()
    await getRecommendations('custom')
  } catch (err) {
    console.error('保存自定义标签失败:', err)
    alert('保存失败，请重试')
  }
}

const getBookTags = (label) => {
  if (!label) return []
  return label
    .split(/[,，]/)
    .map((tag) => tag.trim())
    .filter((tag) => tag)
}

const goToDetail = (bookId) => {
  router.push(`/book/detail/${bookId}`)
}

const handleImageError = (e) => {
  e.target.src = '/src/assets/default-cover.jpg'
}

onMounted(async () => {
  if (!localStorage.getItem('token')) {
    await libraryLoadAllNovels()
    return
  }
  await loadPreferenceTagsFromCenter()
  if (preferenceTagsFromCenter.value.collection.length > 0) {
    await fetchRecommendByCollection()
  }
})
</script>

<style scoped>
.recommend-container {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.recommend-content {
  max-width: 1200px;
  margin: 80px auto 0;
  padding: 40px 20px;
}

.recommend-as-library .page-title {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 3px solid #f8a555;
}

.recommend-as-library .empty-tip {
  text-align: center;
  padding: 40px 20px;
  font-size: 16px;
  color: #666;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

h1 {
  color: #303133;
  font-size: 32px;
  margin-bottom: 10px;
  text-align: center;
}

.link-center {
  color: #667eea;
  text-decoration: none;
  font-size: 14px;
}

.link-center:hover {
  text-decoration: underline;
}

.recommend-actions {
  text-align: center;
  margin-bottom: 24px;
}

.recommend-btn {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 15px 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 50px;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.recommend-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
}

.dissatisfied-btn {
  padding: 14px 32px;
  background: linear-gradient(135deg, #f8a555 0%, #e89544 100%);
  color: #fff;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 4px 14px rgba(248, 165, 85, 0.45);
}

.dissatisfied-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(248, 165, 85, 0.55);
  background: linear-gradient(135deg, #e89544 0%, #d88534 100%);
}

.btn-icon {
  font-size: 24px;
}

.books-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 24px;
  margin-top: 30px;
}

.book-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.book-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

.book-cover {
  width: 100%;
  height: 280px;
  overflow: hidden;
  background: #f0f0f0;
}

.book-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.book-info {
  padding: 15px;
}

.book-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-author {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.book-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag {
  padding: 4px 10px;
  background: #f0f2f5;
  color: #606266;
  font-size: 12px;
  border-radius: 12px;
}

.empty-state,
.loading-state {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
  font-size: 16px;
}

.empty-state .link-center {
  display: inline-block;
  margin-top: 12px;
}

.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog-content {
  background: #fff;
  border-radius: 20px;
  padding: 36px 32px;
  max-width: 500px;
  width: 90%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15), 0 0 1px rgba(0, 0, 0, 0.1);
}

.sort-dialog {
  max-width: 480px;
}

.dialog-content h2 {
  color: #1a1a2e;
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 8px;
  text-align: center;
  letter-spacing: 0.3px;
}

.dialog-desc {
  color: #6b7280;
  text-align: center;
  margin-bottom: 28px;
  font-size: 14px;
  line-height: 1.5;
}

.sort-options {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 24px;
}

.sort-option {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 20px 22px;
  border: 2px solid #e5e7eb;
  border-radius: 14px;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.25s ease;
  text-align: left;
}

.sort-option:hover {
  border-color: #f8a555;
  background: #fffbf7;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(248, 165, 85, 0.18);
}

.sort-option-collection:hover .option-icon-wrap {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.sort-option-custom:hover .option-icon-wrap {
  background: linear-gradient(135deg, #f8a555 0%, #e89544 100%);
  color: #fff;
}

.option-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.25s ease;
}

.option-icon {
  font-size: 26px;
  line-height: 1;
}

.option-body {
  flex: 1;
  min-width: 0;
}

.option-title {
  font-size: 17px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 4px;
}

.option-desc {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.45;
}

.sort-dialog .close-btn {
  width: 100%;
  padding: 12px 24px;
  border-radius: 12px;
  font-size: 15px;
  color: #6b7280;
  border: 1px solid #e5e7eb;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.sort-dialog .close-btn:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
  color: #374151;
}

.tag-categories .category-section {
  margin-bottom: 20px;
}

.tag-categories h5 {
  font-size: 14px;
  color: #606266;
  margin-bottom: 10px;
}

.category-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.category-tag {
  padding: 8px 14px;
  border: 1px solid #e4e7ed;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafafa;
  color: #606266;
}

.category-tag:hover {
  border-color: #667eea;
  color: #667eea;
}

.category-tag.selected {
  border-color: #667eea;
  background: #eff6ff;
  color: #667eea;
}

.dialog-buttons {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.dialog-buttons .close-btn,
.dialog-buttons .confirm-tags-btn {
  flex: 1;
  padding: 12px;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.close-btn {
  border: 1px solid #dcdfe6;
  background: white;
  color: #606266;
}

.close-btn:hover {
  background: #f5f7fa;
}

.confirm-tags-btn {
  border: none;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: 600;
}

.confirm-tags-btn:hover {
  opacity: 0.9;
}
</style>
