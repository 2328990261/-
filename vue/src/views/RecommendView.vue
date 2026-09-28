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

      <p v-if="hasLoadedOnce && !loading && isLoggedIn" class="preference-summary">
        当前推荐：<strong>{{ currentSortLabel }}</strong>
        <template v-if="currentDisplayTags.length">
          · 偏好标签（与个人中心同步）：{{ currentDisplayTags.join('、') }}
        </template>
      </p>

      <div v-if="recommendedBooks.length > 0" class="books-grid">
        <div
          v-for="book in recommendedBooks"
          :key="book.id"
          class="book-card"
          @click="goToDetail(book.id)"
        >
          <div class="book-cover">
            <img
              :src="`${backendUrl('/novel/cover')}/${encodeURIComponent(book.cover)}`"
              :alt="book.bookMainName"
              @error="handleImageError"
            />
          </div>
          <div class="book-info">
            <h3 class="book-title">{{ book.bookMainName }}</h3>
            <div class="book-meta-row">
              <p class="book-author">{{ book.author }}</p>
              <BookCardMoreMenu :book="book" @open-dislike="openDislikeDialog" />
            </div>
            <div class="book-tags">
              <span
                v-for="(tag, index) in getBookTags(book.label)"
                :key="index"
                class="tag"
              >
                {{ tag }}
              </span>
            </div>
            <div class="book-badges">
              <span v-if="bookOthersReading(book)" class="others-reading-badge">别人在看</span>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="!loading && hasLoadedOnce" class="empty-state">
        <p>暂无匹配书籍，请到个人中心调整阅读偏好标签</p>
        <router-link to="/user/center" class="link-center">去个人中心</router-link>
      </div>

      <div v-else-if="!loading && !hasPreferenceTags" class="empty-state">
        <p>请先在个人中心设置阅读偏好（收藏排序或自定义排序标签）</p>
        <router-link to="/user/center" class="link-center">去个人中心设置</router-link>
      </div>

      <div v-if="loading" class="loading-state">
        <p>加载中...</p>
      </div>
    </div>

    <!-- 不满意？→ 选择 收藏排序 / 自定义排序 -->
    <div v-if="showDialog" class="dialog-overlay" @click="closeDialog">
      <div class="dialog-content sort-dialog" @click.stop>
        <h2>选择推荐方式</h2>
        <p class="dialog-desc">任选一种方式</p>

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
        <p class="dialog-desc">五个主标签选1个，其他最多选4个</p>

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

    <DislikeBookDialog
      v-model="dislikeDialogVisible"
      :novel-id="dislikeBook?.id"
      :author="dislikeBook?.author"
      :label="dislikeBook?.label"
      @saved="onDislikeSaved"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { backendUrl } from '@/config/env'
import Navbar from '@/components/Navbar.vue'
import LibraryTagSearch from '@/components/LibraryTagSearch.vue'
import DailyBooks from '@/components/DailyBooks.vue'
import DislikeBookDialog from '@/components/DislikeBookDialog.vue'
import BookCardMoreMenu from '@/components/BookCardMoreMenu.vue'
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

const dislikeDialogVisible = ref(false)
const dislikeBook = ref(null)

const openDislikeDialog = (book) => {
  dislikeBook.value = book
  dislikeDialogVisible.value = true
}

const onDislikeSaved = async () => {
  dislikeBook.value = null
  await getRecommendations(currentSortType.value)
}

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
      if (userInfo && userInfo.id != null) return Number(userInfo.id)
    }
    const uid = localStorage.getItem('userId')
    if (uid) return Number(JSON.parse(uid))
  } catch (e) {}
  return null
}

const isRecommendResponseOk = (response) => {
  const c = response?.code
  return c === 200 || c === '200' || Number(c) === 200
}

/**
 * 推荐调试：控制台只保留三块——①个人标签权重表 ②本次算分（Top5/覆盖/统计）③最终书单分项。
 */
const logRecommendationDebug = (userId, sortType, payload) => {
  if (!payload || Array.isArray(payload)) {
    console.info(
      '[推荐调试]',
      `用户${userId}，「${sortType === 'collection' ? '收藏排序' : '自定义排序'}」`,
      '（接口未返回 debug，无法输出表格）'
    )
    return
  }
  const debug = payload.debug
  if (!debug) return
  const label = sortType === 'collection' ? '收藏排序' : '自定义排序'
  const books = payload.books || []
  console.groupCollapsed(`[推荐调试] 用户${userId}，「${label}」，共${books.length}本`)

  // —— 一、个人标签权重（画像向量，与 /api/user/recommend/tag-weights 同源）——
  const vec = debug.tagVectorTop
  if (Array.isArray(vec) && vec.length > 0) {
    console.log('【一】个人标签权重（含占比，按权重从高到低排）')
    console.table(vec.map((row) => ({ 标签: row.tag, 权重: row.weight, 占比: row.pct })))
  } else {
    console.log('【一】个人标签权重：无数据')
  }

  // —— 二、本次算分：tagHit 只用「偏好顺序前 5 个标签名」+ 上表中的权重去匹配书的 label ——
  const t = debug.tagFilterStats
  if (t) {
    console.log(
      '【二】本次算分说明：下表「用于 tagHit 的 Top5」= 个人中心收藏/自定义排序里排在前面的 5 个标签名，每个标签的权重取自【一】里同名标签（与【一】表格前几行不是同一概念）。'
    )
    const sw = t.scoringTop5Weights && typeof t.scoringTop5Weights === 'object' ? t.scoringTop5Weights : {}
    const top5Rows = Object.keys(sw).map((tag, i) => ({
      偏好顺序: i + 1,
      标签名: tag,
      参与tagHit的权重: Number(sw[tag])
    }))
    console.table(top5Rows)

    const cov = t.perScoringTagCoverInFiltered
    if (cov && typeof cov === 'object' && Object.keys(cov).length > 0) {
      console.log('【二·续】过滤后候选里，各「打分标签」在书本 label 中出现的本数')
      console.table(Object.keys(cov).map((tag) => ({ 标签: tag, 覆盖本书数: cov[tag] })))
    }

    console.table([
      {
        项: '全库书本数',
        值: t.allNovelsCount
      },
      {
        项: '黑名单等过滤后',
        值: t.filteredAfterBlacklistCount
      },
      {
        项: '自定义排序被标签筛掉',
        值: t.customTagExcludedCount
      },
      {
        项: '进入打分的候选数',
        值: t.candidateCount
      },
      {
        项: '其中 tagHit>0',
        值: t.candidateTagHitPositiveCount
      },
      {
        项: '其中 tagHit=0',
        值: t.candidateTagHitZeroCount
      },
      {
        项: '过滤后至少命中任一打分标签的书',
        值: t.filteredBooksHitAnyScoringTagOnLabelCount
      }
    ])

    if (t.scoringTop5KeySetEqualsPersonalWeightTop5KeySet === false) {
      console.warn(
        '【提示】个人向量里「权重最高的 5 个标签」与上表「用于 tagHit 的 Top5」不是同一组名字；若首页按奇幻等筛得到的书在推荐里 tagHit 很低，请到个人中心看收藏排序前 5 个标签是否包含奇幻等。'
      )
    }

    if (Array.isArray(t.sampleTagHitZeroBooks) && t.sampleTagHitZeroBooks.length > 0) {
      console.log('【二·样例】部分 tagHit=0 的书（核对 label 是否与平台标签字面值一致）')
      console.table(
        t.sampleTagHitZeroBooks.map((r) => ({
          书ID: r.novelId,
          书名: r.bookMainName,
          label原文: r.labelRaw
        }))
      )
    }
  } else if (Array.isArray(debug.top5PreferenceKeys) && debug.top5PreferenceKeys.length) {
    console.log('【二】偏好 Top5 键（无 tagFilterStats 时的简略输出）', debug.top5PreferenceKeys)
  }

  // —— 三、最终推荐书单分项 ——
  const rows = debug.candidates
  if (Array.isArray(rows) && rows.length > 0) {
    console.log(
      '【三】最终推荐书单分项（「标签命中」= 上表 Top5 在本书 label 上的权重之和；「综合排序分」= 混合标签/协同/热度后的排序分）'
    )
    const mixVal = (r) => (r.S_mix != null ? r.S_mix : r.mix)
    console.table(
      rows.map((r) => ({
        排名: r.rank,
        书ID: r.novelId,
        书名: r.name,
        标签命中: r.tagHit,
        行为加分: r.behavior,
        评论加分: r.commentBonus,
        最近读过加分: r.recentReadBonus,
        阅读条数调试分: r.readDebugBonus,
        原始标签分: r.rawTag,
        协同分: r.cfScore,
        标签分归一: r.normTag,
        协同分归一: r.normCf,
        热度加分: r.popBoost,
        综合排序分: mixVal(r),
        全站阅读量: r.readCount,
        别人在看: r.othersReading ? '是' : '否',
        换换口味: r.changeFlavor ? '是' : '否'
      }))
    )
  }

  if (Array.isArray(debug.warnings) && debug.warnings.length) {
    console.warn('【后端提示】', debug.warnings)
  }
  console.groupEnd()
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
  if (userId == null || !Number.isFinite(userId)) {
    preferenceTagsFromCenter.value = { collection: [], custom: [] }
    return
  }
  try {
    const res = await request.get(`/user/behavior/preference/tags?userId=${userId}`)
    const list = Array.isArray(res) ? res : []
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
    if (userId == null || !Number.isFinite(userId)) {
      alert('未登录或用户信息无效，请重新登录')
      return
    }
    const response = await request.get('/recommend/books', {
      params: { userId, sortType, _t: Date.now() }
    })

    if (isRecommendResponseOk(response)) {
      const payload = response.data
      const list = Array.isArray(payload) ? payload : payload?.books ?? []
      recommendedBooks.value = [...list]
      hasLoadedOnce.value = true
      logRecommendationDebug(userId, sortType, payload)
      if (recommendedBooks.value.length === 0) {
        alert('没有找到匹配的书籍，请到个人中心调整阅读偏好标签')
      }
    } else {
      const msg = response.msg || '推荐失败'
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
    const msg = error?.msg || '推荐失败，请先在个人中心设置阅读偏好'
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

/** 偏好标签保存接口：兼容旧版返回 true，新版统一 Result */
const isPreferenceSaveOk = (res) => {
  if (res === true) return true
  return res != null && typeof res === 'object' && Number(res.code) === 200
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

// 确认自定义标签：保存到个人中心（与个人中心同一接口），再从服务端拉偏好并拉自定义排序推荐
const confirmCustomTagsAndRecommend = async () => {
  if (customSelectedTags.value.length === 0) {
    alert('请至少选择一个标签')
    return
  }

  const firstPicked = customSelectedTags.value.filter((t) => firstRowTags.includes(t))
  const others = customSelectedTags.value.filter((t) => !firstRowTags.includes(t))
  if (firstPicked.length > 1) {
    alert('五大标签请只选 1 个')
    return
  }
  if (others.length > 4) {
    alert('其他标签最多选 4 个')
    return
  }

  const userId = getUserId()
  if (userId == null || !Number.isFinite(userId)) {
    alert('未登录或用户信息无效，请重新登录')
    return
  }

  const tagData = customSelectedTags.value.map((tagName, index) => ({
    userId,
    tagName,
    tagType: 'custom',
    tagOrder: index + 1,
    isFirstRow: false
  }))

  try {
    const saveRes = await request.post(
      `/user/behavior/preference/tags?userId=${userId}&tagType=custom`,
      tagData
    )
    if (!isPreferenceSaveOk(saveRes)) {
      alert(saveRes?.msg || '保存偏好失败，请检查网络或稍后重试')
      return
    }
    await loadPreferenceTagsFromCenter()
    if (!preferenceTagsFromCenter.value.custom.length) {
      alert('保存后未从服务器读到自定义标签，请重试或到个人中心保存一次')
      return
    }
    closeTagDialog()
    await getRecommendations('custom')
  } catch (err) {
    console.error('保存自定义标签失败:', err)
    alert(err?.msg || err?.message || '保存失败，请重试')
  }
}

/** 与后端 book.othersReading 一致：最终推荐列表中协同分 Top5 才展示「别人在看」 */
const bookOthersReading = (book) => !!(book && book.othersReading)

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
  loading.value = true
  try {
    await loadPreferenceTagsFromCenter()
    const { collection, custom } = preferenceTagsFromCenter.value
    if (collection.length > 0) {
      await fetchRecommendByCollection()
    } else if (custom.length > 0) {
      await getRecommendations('custom')
    } else {
      loading.value = false
    }
  } catch (e) {
    console.error('推荐页初始化失败:', e)
    loading.value = false
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

.preference-summary {
  text-align: center;
  font-size: 14px;
  color: #606266;
  margin: -12px 0 20px;
  line-height: 1.6;
}

.preference-summary strong {
  color: #f8a555;
  font-weight: 600;
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

.books-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 24px;
  margin-top: 30px;
}

.book-card {
  position: relative;
  background: white;
  border-radius: 12px;
  overflow: visible;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.book-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.book-meta-row .book-author {
  margin-bottom: 0;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  position: relative;
  padding: 15px;
  padding-bottom: 42px;
}

.book-badges {
  position: absolute;
  left: 15px;
  bottom: 10px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  max-width: calc(100% - 30px);
}

.others-reading-badge {
  display: inline-block;
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 600;
  color: #c45c12;
  background: linear-gradient(135deg, #ffe8d4 0%, #ffd4a8 100%);
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(196, 92, 18, 0.2);
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

/* ========== 移动端响应式 ========== */
@media (max-width: 768px) {
  .recommend-content {
    margin: 64px auto 0;
    padding: 24px 16px;
  }

  h1 {
    font-size: 24px;
  }

  .recommend-as-library .page-title {
    font-size: 22px;
  }

  .books-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 16px;
  }

  .book-cover {
    height: 220px;
  }

  .dialog-content {
    padding: 28px 20px;
  }
}

@media (max-width: 480px) {
  .recommend-content {
    margin: 56px auto 0;
    padding: 20px 12px;
  }

  h1 {
    font-size: 20px;
  }

  .recommend-as-library .page-title {
    font-size: 20px;
  }

  .books-grid {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    gap: 12px;
  }

  .book-cover {
    height: 180px;
  }

  .book-info {
    padding: 12px;
    padding-bottom: 40px;
  }

  .book-title {
    font-size: 14px;
  }

  .sort-option {
    padding: 14px;
    gap: 12px;
  }

  .option-icon-wrap {
    width: 42px;
    height: 42px;
  }

  .dialog-content {
    padding: 24px 16px;
  }
}
</style>
