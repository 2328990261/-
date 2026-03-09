<template>
  <div class="book-read-container" :class="{ 'eye-protection-mode': eyeProtection }">
    <!-- 主内容区域 -->
    <div class="book-read-main">
      <!-- 1. 导航栏组件 -->
      <Navbar :eye-protection="eyeProtection" />
      <!-- 左侧导航栏组件 -->
      <LeftNavigationBar
        @eye-protection-change="handleEyeProtectionChange"
        @font-size-change="handleFontSizeChange"
        @page-width-change="handlePageWidthChange"
        @page-size-change="handlePageSizeChange"
        @return-detail="handleReturnDetail"
        @prev-chapter="handlePrevChapter"
        @next-chapter="handleNextChapter"
        @settings-toggle="isSettingsOpen = $event"
        :eye-protection="eyeProtection"
      />

      <!-- 阅读页实时评论面板（右侧） -->
      <div class="read-comment-panel" :class="{ 'eye-protection-mode': eyeProtection }">
        <h4 class="read-comment-title">实时评论</h4>
        <template v-if="commentUserId">
          <textarea v-model="readCommentContent" placeholder="写下你对本书的评论…" rows="3" maxlength="500" class="read-comment-input" />
          <button class="read-comment-submit" :disabled="readCommentSubmitting || !readCommentContent.trim()" @click="submitReadComment">发布</button>
        </template>
        <p v-else class="read-comment-hint">登录后即可评论</p>
        <div class="read-comment-list-wrap">
          <p v-if="readCommentLoading" class="read-comment-meta">加载中…</p>
          <template v-else>
            <div v-for="c in readCommentList" :key="c.id" class="read-comment-item">
              <span class="read-comment-user">{{ c.username || ('用户' + c.userId) }}</span>
              <span class="read-comment-time">{{ formatReadCommentTime(c.createdAt) }}</span>
              <p class="read-comment-text">{{ c.content }}</p>
            </div>
            <p v-if="readCommentList.length === 0" class="read-comment-meta">暂无评论</p>
          </template>
        </div>
      </div>

      <!-- 上一页/下一页/收藏/回到顶部（打开设置时隐藏，避免遮挡设置面板） -->
      <NavigationBar
        v-show="!isSettingsOpen"
        :can-go-prev="canGoPrevPage"
        :can-go-next="canGoNextPage"
        :is-favorited="isFavorited"
        @prev-page="handlePrevPage"
        @next-page="handleNextPage"
        @toggle-favorite="toggleFavorite"
        :eye-protection="eyeProtection"
      />

      <!-- 小说阅读组件 -->
      <BookReader
        ref="bookReaderRef"
        :book-detail="novelDetail"
        :chapter="currentChapter"
        :loading="loading"
        :page-size="pageSize"
        :eye-protection="eyeProtection"
        :font-size="fontSize"
        :page-width="pageWidth"
        @page-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, defineProps, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getNovelDetailById, getChapterContentById, addCollection, removeCollection, checkCollection } from '@/api/novel'
import { markNovelAsFinished as apiMarkFinished, saveReadingHistory, getNovelComments, addComment } from '@/api/behavior'
import BookReader from '@/components/BookReader.vue'
import NavigationBar from '@/components/NavigationBar.vue'
import LeftNavigationBar from '@/components/LeftNavigationBar.vue'
import Navbar from "@/components/Navbar.vue";

/**
 * 路由和路由器实例
 */
const route = useRoute()
const router = useRouter()

/**
 * 组件属性定义
 */
const props = defineProps({
  // 小说详情ID
  id: {
    type: [String, Number],
    required: true
  }
})

/**
 * 小说阅读组件引用，用于调用组件方法
 */
const bookReaderRef = ref(null)

/**
 * 护眼模式状态
 */
const eyeProtection = ref(false)

/** 设置面板是否打开（打开时隐藏上一页/下一页导航栏避免遮挡） */
const isSettingsOpen = ref(false)

/**
 * 字体大小状态
 */
const fontSize = ref('medium')

/**
 * 页面宽度状态
 */
const pageWidth = ref('medium')

/**
 * 每页行数状态
 */
const pageSize = ref(100)

/**
 * 加载状态
 */
const loading = ref(false)

/**
 * 错误信息
 */
const errorMsg = ref('')

/**
 * 小说详情
 */
const novelDetail = ref(null)

/**
 * 当前章节内容
 */
const currentChapter = ref(null)

/**
 * 当前章节ID
 */
const currentChapterId = ref(null)

/**
 * 收藏状态
 */
const isFavorited = ref(false)

/** 当前会话累计阅读秒数（用于上报） */
const readDurationSeconds = ref(0)
/** 阅读时长定时器 ID（每秒累加） */
let readDurationTimerId = null
/** 阅读时长上报定时器 ID（每 60 秒上报一次） */
let readReportIntervalId = null

/** 从本地存储获取当前用户 ID */
const getUserId = () => {
  try {
    const userInfoStr = localStorage.getItem('userInfo')
    if (userInfoStr) {
      const userInfo = JSON.parse(userInfoStr)
      if (userInfo?.id) return userInfo.id
    }
    const uid = localStorage.getItem('userId')
    return uid ? JSON.parse(uid) : null
  } catch {
    return null
  }
}

// 阅读页实时评论
const readCommentList = ref([])
const readCommentContent = ref('')
const readCommentLoading = ref(false)
const readCommentSubmitting = ref(false)
const commentUserId = ref(null)
const getCommentUserId = () => {
  const uid = getUserId()
  commentUserId.value = uid
  return uid
}
const formatReadCommentTime = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = (now - d) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  return d.toLocaleDateString()
}
const loadReadComments = async () => {
  const novelId = props.id
  if (!novelId) return
  readCommentLoading.value = true
  try {
    const res = await getNovelComments(novelId)
    readCommentList.value = Array.isArray(res) ? res : (res?.data ?? [])
  } catch {
    readCommentList.value = []
  } finally {
    readCommentLoading.value = false
  }
}
const submitReadComment = async () => {
  const content = readCommentContent.value?.trim()
  if (!content) return
  const userId = getCommentUserId()
  if (!userId) return
  readCommentSubmitting.value = true
  try {
    await addComment({ userId, novelId: Number(props.id), content })
    readCommentContent.value = ''
    await loadReadComments()
  } catch (e) {
    console.error('发表评论失败', e)
  } finally {
    readCommentSubmitting.value = false
  }
}

/**
 * 计算是否可以上一页（仅章节内分页）
 */
const canGoPrevPage = computed(() => {
  return bookReaderRef.value && bookReaderRef.value.currentPage > 1
})

/**
 * 计算是否可以下一页（仅章节内分页）
 */
const canGoNextPage = computed(() => {
  return bookReaderRef.value && bookReaderRef.value.currentPage < bookReaderRef.value.totalPages
})

/**
 * 计算当前页面标识
 */
const currentPageKey = computed(() => {
  return `${route.name}-${route.params.id || ''}`
})

/**
 * 处理护眼模式变化
 * @param {boolean} status - 护眼模式状态
 */
const handleEyeProtectionChange = (status) => {
  eyeProtection.value = status
}

/**
 * 处理字体大小变化
 * @param {string} size - 字体大小
 */
const handleFontSizeChange = (size) => {
  fontSize.value = size
}

/**
 * 处理页面宽度变化
 * @param {string} width - 页面宽度
 */
const handlePageWidthChange = (width) => {
  pageWidth.value = width
}

/**
 * 处理每页行数变化
 * @param {number} size - 每页行数
 */
const handlePageSizeChange = (size) => {
  pageSize.value = size
}

/**
 * 处理返回小说详情
 */
const handleReturnDetail = () => {
  router.push(`/book/detail/${props.id}`)
}

/**
 * 处理页码变化，当到达第5页时增加阅读量
 * @param {number} page - 当前页码
 */
const handlePageChange = async (page) => {
  // 检查是否到达第5页
  if (page === 5) {
    // 检查localStorage，避免重复增加阅读量
    const readCountKey = `readCount_${props.id}_${currentChapterId.value}`
    const hasIncreased = localStorage.getItem(readCountKey)
    
    if (!hasIncreased) {
      try {
        const response = await fetch(`http://localhost:8081/novel/increaseReadCount/${props.id}`, {
          method: 'POST'
        })
        
        const result = await response.json()
        
        if (result.code === 200) {
          // 标记已增加阅读量
          localStorage.setItem(readCountKey, 'true')
        }
      } catch (error) {
        console.error('增加阅读量失败:', error)
      }
    }
  }
}

/**
 * 加载小说数据
 */
const loadNovelData = async () => {
  // 转换小说ID为数字类型
  const novelId = Number(props.id)
  
  // 验证小说ID格式
  if (isNaN(novelId)) {
    errorMsg.value = '小说ID格式错误'
    return
  }
  
  // 设置加载状态
  loading.value = true
  
  try {
    // 调用API获取小说详情
    const res = await getNovelDetailById(novelId)
    
    // 检查API响应
    if (res.code === 200 && res.data) {
      // 保存小说详情
      novelDetail.value = res.data
    } else {
      // 设置错误信息
      errorMsg.value = res.msg || '获取小说信息失败'
    }
  } catch (err) {
    // 处理网络错误
    errorMsg.value = '网络错误，无法加载小说'
  } finally {
    // 清除加载状态
    loading.value = false
  }
}

/**
 * 加载章节内容
 * @param {number|string} chapterId - 章节ID
 */
const loadChapter = async (chapterId) => {
  // 验证章节ID
  if (!chapterId) {
    return
  }
  // 切章前上报当前章节的阅读时长
  if (currentChapterId.value && readDurationSeconds.value > 0) {
    await reportReadingDuration(readDurationSeconds.value)
    readDurationSeconds.value = 0
  }
  
  // 设置加载状态
  loading.value = true
  
  // 保存当前章节ID
  currentChapterId.value = chapterId
  
  try {
    // 调用API获取章节内容
    const res = await getChapterContentById(chapterId)
    
    // 检查API响应
    if (res.code === 200 && res.data) {
      // 保存章节内容
      currentChapter.value = res.data
      
      // 处理页码参数，跳转到指定页
      const pageParam = route.query.page
      if (pageParam && bookReaderRef.value) {
        const targetPage = Number(pageParam)
        if (!isNaN(targetPage) && targetPage > 0) {
          // 使用 setTimeout 确保在章节变化监听器执行后再跳转
          setTimeout(() => {
            if (bookReaderRef.value) {
              bookReaderRef.value.goToPage(targetPage)
            }
          }, 100)
        }
      }
    } else {
      // 处理获取章节失败的情况
      const errorMsg = res.msg || '获取章节内容失败'
    }
  } catch (err) {
    // 处理网络错误
  } finally {
    // 清除加载状态
    loading.value = false
  }
}

/** 上报阅读时长到后端（传入本次要上报的秒数，上报后清零本地累计） */
const reportReadingDuration = async (seconds) => {
  if (!seconds || seconds <= 0) return
  const userId = getUserId()
  const novelId = props.id
  const chapterId = currentChapterId.value
  if (!userId || !novelId || !chapterId) return
  try {
    await saveReadingHistory({
      userId,
      novelId,
      chapterId,
      readDuration: seconds,
      readProgress: bookReaderRef.value?.currentPage || 1
    })
  } catch (e) {
    // 静默失败
  }
}

/**
 * 组件挂载时执行
 */
onMounted(async () => {
  // 加载小说数据
  await loadNovelData()
  
  // 确保章节列表存在且有数据
  if (novelDetail.value?.volumeList && novelDetail.value.volumeList.length > 0) {
    // 获取章节ID，优先使用路由参数中的章节ID，否则使用第一个章节的ID
    const chapterId = route.query.chapterId || novelDetail.value.volumeList[0].id
    
    // 加载章节内容
    if (chapterId) {
      loadChapter(chapterId)
    }
  }
  
  // 检查收藏状态
  await checkFavoriteStatus()

  getCommentUserId()
  loadReadComments()

  // 阅读时长：每秒累加，每 60 秒上报一次
  readDurationTimerId = setInterval(() => {
    readDurationSeconds.value += 1
  }, 1000)
  readReportIntervalId = setInterval(async () => {
    const sec = readDurationSeconds.value
    if (sec > 0) {
      await reportReadingDuration(sec)
      readDurationSeconds.value = 0
    }
  }, 60000)
})

onBeforeUnmount(() => {
  if (readDurationTimerId) clearInterval(readDurationTimerId)
  if (readReportIntervalId) clearInterval(readReportIntervalId)
  const remaining = readDurationSeconds.value
  if (remaining > 0) {
    reportReadingDuration(remaining)
  }
})

/**
 * 监听路由参数变化，重新加载数据
 */
watch([() => route.query.chapterId, () => props.id], async () => {
  // 加载小说数据
  await loadNovelData()
  
  // 确保章节列表存在且有数据
  if (novelDetail.value?.volumeList && novelDetail.value.volumeList.length > 0) {
    // 获取章节ID，优先使用路由参数中的章节ID，否则使用第一个章节的ID
    const chapterId = route.query.chapterId || novelDetail.value.volumeList[0].id
    
    // 加载章节内容
    if (chapterId) {
      loadChapter(chapterId)
    }
  }
  loadReadComments()
}, { immediate: true })

/**
 * 处理上一页按钮点击（仅章节内分页）
 */
const handlePrevPage = async () => {
  // 检查组件引用是否存在
  if (!bookReaderRef.value) return
  
  // 调用组件方法切换到上一页
  bookReaderRef.value.prevPage()
  
  // 保存阅读进度
  await saveReadingProgress()
}

/**
 * 处理下一页按钮点击（仅章节内分页）
 */
const handleNextPage = async () => {
  // 检查组件引用是否存在
  if (!bookReaderRef.value) return
  
  // 调用组件方法切换到下一页
  bookReaderRef.value.nextPage()
  
  // 保存阅读进度
  await saveReadingProgress()
}

/**
 * 处理上一章按钮点击
 */
const handlePrevChapter = async () => {
  // 获取章节列表
  const chapterList = novelDetail.value?.volumeList || []
  
  // 检查章节列表是否为空
  if (chapterList.length === 0) {
    return
  }
  
  // 检查当前章节ID是否存在
  if (!currentChapterId.value) {
    // 尝试从currentChapter对象获取ID
    if (currentChapter.value?.id) {
      currentChapterId.value = currentChapter.value.id
    } else {
      return
    }
  }
  
  // 查找当前章节索引
  let currentIndex = -1
  for (let i = 0; i < chapterList.length; i++) {
    const chapter = chapterList[i]
    
    // 比较章节ID
    if (String(chapter.id) === String(currentChapterId.value)) {
      currentIndex = i
      break
    }
  }
  
  // 如果找不到当前章节，尝试使用currentChapter对象的ID重新查找
  if (currentIndex === -1) {
    if (currentChapter.value?.id) {
      for (let i = 0; i < chapterList.length; i++) {
        if (String(chapterList[i].id) === String(currentChapter.value.id)) {
          currentIndex = i
          currentChapterId.value = currentChapter.value.id
          break
        }
      }
    }
    
    // 如果还是找不到，默认使用第一个章节
    if (currentIndex === -1) {
      currentIndex = 0
    }
  }
  
  // 加载上一章
  if (currentIndex > 0) {
    const prevChapter = chapterList[currentIndex - 1]
    loadChapter(prevChapter.id)
  }
  
  // 保存阅读进度
  await saveReadingProgress()
}

/**
 * 处理下一章按钮点击
 */
const handleNextChapter = async () => {
  // 获取章节列表
  const chapterList = novelDetail.value?.volumeList || []
  
  // 检查章节列表是否为空
  if (chapterList.length === 0) {
    return
  }
  
  // 检查当前章节ID是否存在
  if (!currentChapterId.value) {
    // 尝试从currentChapter对象获取ID
    if (currentChapter.value?.id) {
      currentChapterId.value = currentChapter.value.id
    } else {
      return
    }
  }
  
  // 查找当前章节索引
  let currentIndex = -1
  for (let i = 0; i < chapterList.length; i++) {
    const chapter = chapterList[i]
    
    // 比较章节ID
    if (String(chapter.id) === String(currentChapterId.value)) {
      currentIndex = i
      break
    }
  }
  
  // 如果找不到当前章节，尝试使用currentChapter对象的ID重新查找
  if (currentIndex === -1) {
    if (currentChapter.value?.id) {
      for (let i = 0; i < chapterList.length; i++) {
        if (String(chapterList[i].id) === String(currentChapter.value.id)) {
          currentIndex = i
          currentChapterId.value = currentChapter.value.id
          break
        }
      }
    }
    
    // 如果还是找不到，默认使用第一个章节
    if (currentIndex === -1) {
      currentIndex = 0
    }
  }
  
  // 加载下一章
  if (currentIndex < chapterList.length - 1) {
    const nextChapter = chapterList[currentIndex + 1]
    loadChapter(nextChapter.id)
  }
  
  // 保存阅读进度
  await saveReadingProgress()
}

/**
 * 切换收藏状态
 */
const toggleFavorite = async () => {
  // 从localStorage获取用户ID
  const userId = JSON.parse(localStorage.getItem('userId') || '1')
  
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
    // 静默处理错误
  }
}

/**
 * 检查收藏状态
 */
const checkFavoriteStatus = async () => {
  // 从localStorage获取用户ID
  const userId = JSON.parse(localStorage.getItem('userId') || '1')
  
  try {
    // 调用API检查收藏状态
    const res = await checkCollection(userId, props.id)
    if (res.code === 200) {
      isFavorited.value = res.data
    }
  } catch (error) {
    // 静默处理错误
  }
}

/**
 * 保存阅读进度
 */
const saveReadingProgress = async () => {
  // 获取小说ID
  const novelId = props.id
  
  // 验证必要参数
  if (!novelId || !currentChapterId.value) return
  
  // 构建进度数据
  const progressData = {
    novelId: novelId,
    chapterId: currentChapterId.value,
    page: bookReaderRef.value?.currentPage || 1,
    timestamp: new Date().getTime()
  }
  
  // 获取现有的阅读记录
  const readingHistory = JSON.parse(localStorage.getItem('readingHistory') || '[]')
  
  // 查找是否已有该小说的记录
  const existingIndex = readingHistory.findIndex(item => item.novelId === novelId)
  
  if (existingIndex >= 0) {
    // 更新现有记录
    readingHistory[existingIndex] = progressData
  } else {
    // 添加新记录
    readingHistory.push(progressData)
  }
  
  // 保存到localStorage
  localStorage.setItem('readingHistory', JSON.stringify(readingHistory))
  
  // 检查是否已读完
  await checkIfFinished()
}

/**
 * 检查是否已读完小说
 */
const checkIfFinished = async () => {
  // 获取小说ID
  const novelId = props.id
  
  // 验证必要参数
  if (!novelId || !novelDetail.value?.volumeList || !bookReaderRef.value) return
  
  // 获取章节列表和最后一章
  const chapterList = novelDetail.value.volumeList
  const lastChapter = chapterList[chapterList.length - 1]
  
  // 检查是否在最后一章
  if (String(currentChapterId.value) !== String(lastChapter.id)) return
  
  // 检查是否在最后一页
  if (bookReaderRef.value.currentPage < bookReaderRef.value.totalPages) return
  
  // 标记为已完成
  await markAsFinished(novelId)
}

/**
 * 标记小说为已完成（写入后端并同步到本地）
 * @param {number|string} novelId - 小说ID
 */
const markAsFinished = async (novelId) => {
  // 检查小说是否已收藏
  const isCollected = await checkIfCollected(novelId)
  
  // 仅对已收藏的小说进行标记
  if (!isCollected) {
    return
  }

  const userId = getUserId()
  if (userId) {
    try {
      await apiMarkFinished(userId, novelId)
    } catch (e) {
      // 静默失败，保留本地标记
    }
  }
  
  // 同步到本地，便于离线或接口失败时仍能展示
  const finishedNovels = JSON.parse(localStorage.getItem('finishedNovels') || '[]')
  if (!finishedNovels.includes(String(novelId))) {
    finishedNovels.push(String(novelId))
    localStorage.setItem('finishedNovels', JSON.stringify(finishedNovels))
  }
}

/**
 * 检查小说是否已收藏
 * @param {number|string} novelId - 小说ID
 * @returns {boolean} 是否已收藏
 */
const checkIfCollected = async (novelId) => {
  // 从localStorage获取用户ID
  const userId = JSON.parse(localStorage.getItem('userId') || '1')
  
  try {
    // 调用API检查收藏状态
    const res = await checkCollection(userId, novelId)
    return res.code === 200 && res.data
  } catch (error) {
    // 处理错误，默认返回false
    return false
  }
}

/**
 * 获取指定小说的阅读进度
 * @param {number|string} novelId - 小说ID
 * @returns {object|null} 阅读进度数据
 */
const getReadingProgress = (novelId) => {
  // 获取阅读历史
  const readingHistory = JSON.parse(localStorage.getItem('readingHistory') || '[]')
  
  // 查找指定小说的进度
  return readingHistory.find(item => item.novelId === novelId) || null
}

/**
 * 暴露方法给外部组件
 */
defineExpose({
  saveReadingProgress,
  getReadingProgress
})
</script>

<style scoped>
/**
 * 基础容器样式
 */
.book-read-container {
  display: block;
  box-sizing: border-box;
  min-height: 100vh;
  background-color: #e8e4d8;
  transition: background-color 0.3s ease;
}

/**
 * 主内容区域
 */
.book-read-main {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px 40px;
  background-color: #f8f6f0;
  min-height: 100vh;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.1);
}

/**
 * 护眼模式样式
 */
.book-read-container.eye-protection-mode {
  background-color: #1a1a1a;
  color: #e0e0e0;
}

.book-read-container.eye-protection-mode .book-read-main {
  background-color: #1e1e1e;
}

/* 阅读页实时评论面板（右侧贴边，不遮挡正文） */
.read-comment-panel {
  position: fixed;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  width: 260px;
  max-height: 70vh;
  z-index: 999;
  background-color: rgba(255, 255, 255, 0.96);
  border-radius: 12px;
  padding: 14px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.read-comment-panel.eye-protection-mode {
  background-color: rgba(30, 30, 30, 0.96);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  border: 1px solid #444;
}
.read-comment-title {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
}
.read-comment-panel.eye-protection-mode .read-comment-title {
  color: #e0e0e0;
}
.read-comment-input {
  width: 100%;
  box-sizing: border-box;
  padding: 8px 10px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 13px;
  resize: vertical;
  min-height: 56px;
}
.read-comment-panel.eye-protection-mode .read-comment-input {
  background: #2a2a2a;
  border-color: #444;
  color: #e0e0e0;
}
.read-comment-submit {
  padding: 8px 16px;
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  align-self: flex-start;
}
.read-comment-submit:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}
.read-comment-hint, .read-comment-meta {
  margin: 0;
  font-size: 12px;
  color: #999;
}
.read-comment-list-wrap {
  overflow-y: auto;
  flex: 1;
  min-height: 60px;
}
.read-comment-item {
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}
.read-comment-panel.eye-protection-mode .read-comment-item {
  border-bottom-color: #333;
}
.read-comment-user {
  font-size: 12px;
  color: #666;
  margin-right: 8px;
}
.read-comment-panel.eye-protection-mode .read-comment-user {
  color: #aaa;
}
.read-comment-time {
  font-size: 11px;
  color: #999;
}
.read-comment-text {
  margin: 6px 0 0;
  font-size: 13px;
  color: #333;
  line-height: 1.4;
}
.read-comment-panel.eye-protection-mode .read-comment-text {
  color: #ddd;
}
@media (max-width: 1100px) {
  .read-comment-panel {
    right: 8px;
    width: 220px;
  }
}
</style>