<template>
  <div class="book-detail">
    <Navbar />

    <div class="detail-container">
      <!-- 左侧主要内容区 -->
      <div class="main-content">
        <!-- 1. 小说基础信息区域（封面+名称+作者+简介） -->
        <div class="book-info">
          <!-- 封面区域 -->
          <div class="cover-wrap">
            <img
              v-if="novelDetail.cover"
              :src="coverUrl"
              alt="小说封面"
              class="cover-img"
              @error="handleCoverErrorOnce"
              ref="coverImg"
            >
            <div v-else class="cover-placeholder">暂无封面</div>
          </div>

          <!-- 小说信息 -->
          <div class="info-content">
            <h1 class="book-title">{{ novelDetail.bookMainName || '未知小说' }}</h1>
            <p class="book-author">作者：{{ novelDetail.author || '未知作者' }}</p>
            <p class="book-label">标签：{{ novelDetail.label || '暂无标签' }}</p>

            <div class="buttons-container">
              <div class="read-favorite-container">
                <button class="read-btn" @click="gotoReadPage">开始阅读</button>
                <!-- 收藏按钮 -->
                <button class="favorite-btn" @click="toggleFavorite" :class="{ 'favorited': isFavorited }">
                  <span class="btn-icon">
                    <span v-if="isFavorited">★</span>
                    <span v-else>☆</span>
                  </span>
                  <span class="btn-text">收藏</span>
                </button>
                <button type="button" class="dislike-outline-btn" @click="dislikeVisible = true">
                  不感兴趣
                </button>
              </div>

              <!-- 继续阅读区域，有阅读记录时显示 -->
              <div v-if="readingProgress" class="continue-reading-section">
                <div class="reading-progress-info">
                  <span class="progress-text">继续阅读：{{ readingProgress.chapterName }}</span>
                  <span class="progress-page">第 {{ readingProgress.page }} 页</span>
                </div>
                <button class="continue-read-btn" @click="gotoContinueRead">
                  继续阅读
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 返回按钮 -->
        <div class="back-btn-container">
          <button class="back-btn" @click="goBack" title="返回首页">
            <img src="../assets/返回.png" alt="返回" />
          </button>
        </div>

        <!-- 2. Element Plus 表格版章节列表 -->
        <div class="chapter-section">
          <h2>章节列表</h2>
          <el-table
            :data="novelDetail.volumeList || []"
            height="400"
            style="width: 100%; margin-top: 10px;"
            @row-click="handleRowClick"
            empty-text="暂无章节数据"
          >
            <el-table-column
              label="序号"
              width="80"
              type="index"
              align="center"
            />
            <el-table-column
              prop="volumeName"
              label="章节名称"
              min-width="400"
              align="center"
            >
              <template #default="scope">
                {{ scope.row.volumeName || `第${scope.$index + 1}章` }}
              </template>
            </el-table-column>
            <el-table-column
              label="操作"
              width="120"
              align="center"
            >
              <template #default="scope">
                <el-button
                  type="primary"
                  size="small"
                  @click.stop="gotoReadPageWithChapter(scope.row.id)"
                >
                  阅读
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 右侧评论区（固定） -->
      <div class="comment-sidebar">
        <div class="comment-section">
          <div class="comment-header">
            <h2>读者评论</h2>
            <div class="comment-sort">
              <button 
                :class="['sort-btn', { active: sortType === 'time' }]" 
                @click="sortType = 'time'; loadComments()"
              >
                最新
              </button>
              <button 
                :class="['sort-btn', { active: sortType === 'hot' }]" 
                @click="sortType = 'hot'; loadComments()"
              >
                热门
              </button>
            </div>
          </div>
          
          <div v-if="!commentUserId" class="comment-login-hint">登录后即可评论</div>
          <template v-else>
            <div class="comment-form">
              <textarea v-model="commentContent" placeholder="写下你的评论…" rows="3" maxlength="500" />
              <button class="comment-submit-btn" :disabled="commentSubmitting || !commentContent.trim()" @click="submitComment">
                {{ commentSubmitting ? '提交中…' : '发表评论' }}
              </button>
            </div>
          </template>
          
          <div v-if="commentLoading" class="comment-loading">加载中…</div>
          <ul v-else class="comment-list">
            <li v-for="c in displayedComments" :key="c.id" class="comment-item">
              <span class="comment-user">{{ c.username || ('用户' + c.userId) }}</span>
              <span class="comment-time">{{ formatCommentTime(c.createdAt) }}</span>
              <p class="comment-text">{{ c.content }}</p>
            </li>
          </ul>
          <p v-if="!commentLoading && commentList.length === 0" class="comment-empty">暂无评论</p>
          
          <!-- 显示更多按钮 -->
          <div v-if="!commentLoading && commentList.length > displayCount" class="show-more-container">
            <button class="show-more-btn" @click="showMore">
              显示更多 ({{ commentList.length - displayCount }})
            </button>
          </div>
        </div>
      </div>
    </div>

    <DislikeBookDialog
      v-model="dislikeVisible"
      :novel-id="detailNovelId"
      :author="novelDetail.author"
      :label="novelDetail.label"
    />
  </div>
</template>

<script setup>
import Navbar from '@/components/Navbar.vue'
import DislikeBookDialog from '@/components/DislikeBookDialog.vue'
import { ref, onMounted, defineProps, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getNovelDetailById, addCollection, removeCollection, checkCollection } from '@/api/novel'
import { getNovelComments, addComment } from '@/api/behavior'

// 路由与传参
const router = useRouter()
const route = useRoute()

const props = defineProps({
  id: {
    type: [String, Number],
    required: true
  }
})

// 数据存储
const novelDetail = ref({})
const coverImg = ref(null)
const isCoverErrorHandled = ref(false)
const readingProgress = ref(null)
const isFavorited = ref(false)
const dislikeVisible = ref(false)

const detailNovelId = computed(() => {
  const n = Number(props.id)
  return Number.isNaN(n) ? props.id : n
})

// 评论
const commentList = ref([])
const commentContent = ref('')
const commentLoading = ref(false)
const commentSubmitting = ref(false)
const commentUserId = ref(null)
const sortType = ref('time') // 'time' 或 'hot'
const displayCount = ref(5) // 当前显示的评论数量

// 计算显示的评论列表
const displayedComments = computed(() => {
  return commentList.value.slice(0, displayCount.value)
})

// 显示更多评论
const showMore = () => {
  displayCount.value += 5
}

const getCommentUserId = () => {
  try {
    const userInfoStr = localStorage.getItem('userInfo')
    if (userInfoStr) {
      const u = JSON.parse(userInfoStr)
      if (u?.id) return u.id
    }
    const uid = localStorage.getItem('userId')
    return uid ? JSON.parse(uid) : null
  } catch {
    return null
  }
}

const formatCommentTime = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = (now - d) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)} 分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`
  return d.toLocaleDateString()
}

const loadComments = async () => {
  const novelId = Number(props.id)
  if (!novelId) return
  commentLoading.value = true
  try {
    const res = await getNovelComments(novelId)
    let allComments = Array.isArray(res) ? res : (res?.data ?? [])
    
    // 根据排序类型排序
    if (sortType.value === 'hot') {
      // 热门排序：假设评论有点赞数或回复数，这里暂时用评论长度模拟热度
      allComments = allComments.sort((a, b) => {
        const heatA = (a.content?.length || 0)
        const heatB = (b.content?.length || 0)
        return heatB - heatA
      })
    } else {
      // 时间排序：最新的在前
      allComments = allComments.sort((a, b) => {
        return new Date(b.createdAt) - new Date(a.createdAt)
      })
    }
    
    commentList.value = allComments
    // 重置显示数量
    displayCount.value = 5
  } catch (e) {
    commentList.value = []
  } finally {
    commentLoading.value = false
  }
}

const submitComment = async () => {
  const content = commentContent.value?.trim()
  if (!content) return
  const userId = getCommentUserId()
  if (!userId) return
  commentSubmitting.value = true
  try {
    await addComment({ userId, novelId: Number(props.id), content })
    commentContent.value = ''
    await loadComments()
  } catch (e) {
    console.error('发表评论失败', e)
  } finally {
    commentSubmitting.value = false
  }
}

// 封面URL（对接后端接口，编码中文）
const coverUrl = computed(() => {
  if (!novelDetail.value.cover) return ''
  return `http://localhost:8081/novel/cover/${encodeURIComponent(novelDetail.value.cover)}`
})

// 封面加载失败处理（仅执行一次，避免死循环）
const handleCoverErrorOnce = (e) => {
  if (isCoverErrorHandled.value) return
  isCoverErrorHandled.value = true

  e.target.style.display = 'none'
  coverImg.value.parentElement.innerHTML = '<div class="cover-placeholder">封面加载失败</div>'
  console.warn('封面加载失败：', novelDetail.value.cover)
}

// 获取阅读进度
const getReadingProgress = () => {
  const novelId = Number(props.id)
  if (!novelId) return

  const readingHistory = JSON.parse(localStorage.getItem('readingHistory') || '[]')
  // 注意类型匹配，将小说ID转换为字符串进行比较
  const progress = readingHistory.find(item => String(item.novelId) === String(novelId))

  if (progress && novelDetail.value.volumeList) {
    // 查找章节名称，同样注意类型匹配
    const chapter = novelDetail.value.volumeList.find(v => String(v.id) === String(progress.chapterId))
    if (chapter) {
      readingProgress.value = {
        chapterId: progress.chapterId,
        page: progress.page,
        chapterName: chapter.volumeName || `第${novelDetail.value.volumeList.indexOf(chapter) + 1}章`
      }
    }
  }
}

// 继续阅读
const gotoContinueRead = () => {
  if (!readingProgress.value) return

  // 记录最近点击的小说 ID 到 localStorage
  localStorage.setItem('lastClickedNovelId', String(props.id))
  router.push({
    name: 'BookRead',
    params: { id: props.id },
    query: {
      chapterId: readingProgress.value.chapterId,
      page: readingProgress.value.page
    }
  })
}

// 切换收藏状态
const toggleFavorite = async () => {
  const userId = JSON.parse(localStorage.getItem('userId') || '1') // 假设从localStorage获取用户ID
  
  try {
    if (isFavorited.value) {
      // 取消收藏
      const res = await removeCollection(userId, props.id)
      if (res.code === 200) {
        isFavorited.value = false
      }
    } else {
      // 添加收藏
      const res = await addCollection(userId, props.id)
      if (res.code === 200) {
        isFavorited.value = true
      }
    }
  } catch (error) {
    console.error('收藏操作失败:', error)
  }
}

// 检查收藏状态
const checkFavoriteStatus = async () => {
  const userId = JSON.parse(localStorage.getItem('userId') || '1')
  
  try {
    const res = await checkCollection(userId, props.id)
    if (res.code === 200) {
      isFavorited.value = res.data
    }
  } catch (error) {
    console.error('检查收藏状态失败:', error)
  }
}

// 初始化加载小说详情
onMounted(async () => {
  commentUserId.value = getCommentUserId()
  const novelId = Number(props.id)
  if (!isNaN(novelId)) {
    const res = await getNovelDetailById(novelId)
    if (res.code === 200 && res.data) {
      novelDetail.value = res.data // 绑定完整的小说数据（含名称、作者、封面、章节）
      // 等待volumeList加载完成后获取阅读进度
      setTimeout(() => {
        getReadingProgress()
      }, 100)
    }
    loadComments()
  }
  await checkFavoriteStatus()
})

// 监听volumeList变化，确保获取阅读进度
watch(() => novelDetail.value.volumeList, (newVolumeList) => {
  if (newVolumeList && newVolumeList.length > 0) {
    getReadingProgress()
  }
}, { immediate: true })

// 跳转阅读页（默认第一章）
const gotoReadPage = () => {
  // 记录最近点击的小说 ID 到 localStorage
  localStorage.setItem('lastClickedNovelId', String(props.id))
  router.push({
    name: 'BookRead',
    params: { id: props.id }
  })
}

// 跳转指定章节
const gotoReadPageWithChapter = (chapterId) => {
  // 记录最近点击的小说 ID 到 localStorage
  localStorage.setItem('lastClickedNovelId', String(props.id))
  router.push({
    name: 'BookRead',
    params: { id: props.id },
    query: { chapterId: chapterId }
  })
}

// 表格行点击跳转
const handleRowClick = (row) => {
  gotoReadPageWithChapter(row.id)
}

// 直接返回首页
const goBack = () => {
  router.push('/')
}
</script>

<style scoped>
/* 基础布局样式 */
.book-detail {
  max-width: 1400px;
  margin: 20px auto;
  padding-top: 60px;
}

/* 主容器：左右布局 */
.detail-container {
  display: flex;
  gap: 40px;
  align-items: flex-start;
  justify-content: space-between;
}

/* 左侧主要内容区 */
.main-content {
  flex: 1;
  min-width: 0;
  max-width: calc(100% - 340px);
}

/* 右侧评论区（固定宽度，靠右） */
.comment-sidebar {
  width: 300px;
  flex-shrink: 0;
  position: sticky;
  top: 80px;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
  margin-left: auto;
}

/* 小说信息区域 */
.book-info {
  display: flex;
  gap: 30px;
  margin-bottom: 40px;
  align-items: flex-start;
}

/* 封面样式 */
.cover-wrap {
  width: 180px;
  height: 240px;
  border: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  overflow: hidden;
}
.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover-placeholder {
  color: #999;
  font-size: 14px;
  text-align: center;
  padding: 10px;
}

/* 小说文字信息 */
.info-content {
  flex: 1;
}
.book-title {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 15px;
  color: #333;
}
.book-author, .book-label {
  font-size: 16px;
  color: #666;
  margin-bottom: 10px;
}
.book-intro {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 20px;
  max-width: 800px;
}

/* 按钮容器，垂直排列两个按钮区域 */
.buttons-container {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-bottom: 15px;
}

/* 开始阅读和收藏按钮容器 */
.read-favorite-container {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  align-items: center;
}

/* 开始阅读按钮 */
.read-btn {
  padding: 10px 20px;
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  align-self: flex-start;
}
.read-btn:hover {
  background: #66b1ff;
}

/* 收藏按钮 */
.favorite-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #fff;
  color: #606266;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.3s ease;
  align-self: flex-start;
}

.favorite-btn:hover {
  background-color: #f8f9fa;
  border-color: #ff8c00;
  color: #ff8c00;
}

/* 已收藏状态 */
.favorite-btn.favorited {
  background-color: #fff;
  border-color: #ff8c00;
  color: #ff8c00;
}

.favorite-btn.favorited:hover {
  background-color: #fff8f0;
  border-color: #ff6b00;
  color: #ff6b00;
}

/* 不感兴趣（次按钮样式） */
.dislike-outline-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 20px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #fff;
  color: #909399;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.3s ease;
  align-self: flex-start;
}

.dislike-outline-btn:hover {
  border-color: #c0c4cc;
  color: #606266;
  background-color: #f5f7fa;
}

.favorite-btn .btn-icon {
  font-size: 18px;
}

.favorite-btn .btn-text {
  font-size: 14px;
}

/* 继续阅读区域 */
.continue-reading-section {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px 15px;
  border: 1px solid #e6f7ff;
  border-radius: 4px;
  background-color: #f6ffed;
}

.reading-progress-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-text {
  font-size: 14px;
  color: #666;
}

.progress-page {
  font-size: 12px;
  color: #999;
}

.continue-read-btn {
  padding: 10px 20px;
  background: #67c23a;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
}
.continue-read-btn:hover {
  background: #85ce61;
}

/* 章节列表区域 */
.chapter-section {
  border-top: 1px solid #eee;
  padding-top: 20px;
}
.chapter-section h2 {
  font-size: 20px;
  margin-bottom: 15px;
  color: #333;
}

/* Element Plus 表格样式微调 */
:deep(.el-table) {
  --el-table-header-text-color: #333;
  --el-table-row-hover-bg-color: #f8f9fa;
}
:deep(.el-button--small) {
  padding: 5px 10px;
}

/* 返回按钮样式 */
.back-btn-container {
  margin: 20px 0;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  right: 20px;
  padding: 12px 24px;
  background-color: #fff;
  color: #333;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.back-btn:hover {
  background-color: #f5f5f5;
  border-color: #b0b0b0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  transform: translateX(2px);
}

/* 返回按钮图片样式 */
.back-btn img {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

.btn-text {
  font-size: 14px;
}

/* 读者评论区域 */
.comment-section {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.comment-section h2 {
  font-size: 16px;
  margin: 0;
  color: #333;
  font-weight: 600;
}

.comment-sort {
  display: flex;
  gap: 6px;
}

.sort-btn {
  padding: 4px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 5px;
  background: #fff;
  color: #666;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}

.sort-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.sort-btn.active {
  background: #3b82f6;
  color: #fff;
  border-color: #3b82f6;
}

.show-more-container {
  margin-top: 12px;
  text-align: center;
}

.show-more-btn {
  padding: 6px 20px;
  background: #f3f4f6;
  color: #666;
  border: 1px solid #e5e7eb;
  border-radius: 5px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}

.show-more-btn:hover {
  background: #e5e7eb;
  color: #333;
}
.comment-login-hint {
  color: #999;
  font-size: 12px;
  margin-bottom: 12px;
  text-align: center;
  padding: 16px 0;
}
.comment-form {
  margin-bottom: 16px;
}
.comment-form textarea {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #dcdfe6;
  border-radius: 5px;
  font-size: 12px;
  resize: vertical;
  display: block;
  margin-bottom: 8px;
}
.comment-form textarea:focus {
  outline: none;
  border-color: #409eff;
}
.comment-submit-btn {
  width: 100%;
  padding: 6px 16px;
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 13px;
}
.comment-submit-btn:hover {
  background: #66b1ff;
}
.comment-submit-btn:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}
.comment-loading, .comment-empty {
  color: #999;
  font-size: 12px;
  text-align: center;
  padding: 16px 0;
}
.comment-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.comment-item {
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}
.comment-item:last-child {
  border-bottom: none;
}
.comment-user {
  font-size: 11px;
  color: #666;
  margin-right: 6px;
  font-weight: 500;
}
.comment-time {
  font-size: 10px;
  color: #999;
}
.comment-text {
  margin: 5px 0 0;
  font-size: 12px;
  color: #333;
  line-height: 1.5;
  word-break: break-word;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .detail-container {
    flex-direction: column;
  }
  
  .comment-sidebar {
    width: 100%;
    position: static;
    max-height: none;
  }
}
</style>
