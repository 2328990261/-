<template>
  <div class="library-page">
    <Navbar />

    <div class="container">
      <h1 class="page-title">我的书库</h1>

      <!-- 搜索记录 -->
      <div v-if="searchHistory.length > 0" class="search-history">
        <span class="history-label">搜索记录：</span>
        <span
          v-for="(item, index) in searchHistory"
          :key="index"
          class="history-item"
        >
          <router-link :to="{ path: '/library', query: { keyword: item } }" class="history-link">{{ item }}</router-link>
          <button type="button" class="history-delete" @click="removeHistory(index)" title="删除">×</button>
        </span>
        <button type="button" class="history-clear" @click="clearHistory">清空</button>
      </div>

      <LibraryTagSearch @selectTags="handleTagsSelect" />

      <DailyBooks
        v-if="displayBooks.length > 0"
        :bookList="displayBooks"
        :title="searchKeyword ? `“${searchKeyword}” 的搜索结果` : (selectedTags.length > 0 ? `${selectedTags.join('、')}小说` : '全部小说')"
        :total-count="allBooks.length"
        :columns="5"
        @load-more="loadMore"
      />

      <div v-if="displayBooks.length === 0" class="empty-tip">
        {{ searchKeyword ? `未找到与“${searchKeyword}”相关的小说` : (selectedTags.length > 0 ? `暂无${selectedTags.join('、')}标签的小说` : '暂无小说') }}
      </div>
    </div>
  </div>
</template>

<script setup>
import Navbar from '@/components/Navbar.vue'
import LibraryTagSearch from '@/components/LibraryTagSearch.vue'
import DailyBooks from '@/components/DailyBooks.vue'
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getAllNovels, getNovelsByLabels, searchNovels } from '@/api/novel'

const route = useRoute()
const allBooks = ref([])
const pageSize = ref(50)
const selectedTags = ref([])
const searchKeyword = ref('')

const SEARCH_HISTORY_KEY = 'novel_search_history'
const MAX_HISTORY = 20

const searchHistory = ref([])
const loadSearchHistory = () => {
  try {
    const raw = localStorage.getItem(SEARCH_HISTORY_KEY)
    searchHistory.value = raw ? JSON.parse(raw) : []
  } catch (e) {
    searchHistory.value = []
  }
}
const saveSearchHistory = (list) => {
  searchHistory.value = list
  localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(list))
}
const addToHistory = (keyword) => {
  if (!keyword || !keyword.trim()) return
  const k = keyword.trim()
  loadSearchHistory()
  let list = searchHistory.value.filter((item) => item !== k)
  list.unshift(k)
  if (list.length > MAX_HISTORY) list = list.slice(0, MAX_HISTORY)
  saveSearchHistory(list)
}
const removeHistory = (index) => {
  const list = searchHistory.value.slice()
  list.splice(index, 1)
  saveSearchHistory(list)
}
const clearHistory = () => {
  saveSearchHistory([])
}

const displayBooks = computed(() => allBooks.value.slice(0, pageSize.value))

const handleTagsSelect = async (tags) => {
  selectedTags.value = tags
  searchKeyword.value = ''
  pageSize.value = 50

  if (tags.length === 0) {
    await loadAllNovels()
    return
  }

  try {
    const res = await getNovelsByLabels(tags)
    const novelData = res.code === 200 ? res.data : []
    allBooks.value = Array.isArray(novelData) ? novelData.sort((a, b) => (b.readCount || 0) - (a.readCount || 0)) : []
  } catch (err) {
    console.error('获取标签小说失败', err)
    allBooks.value = []
  }
}

const loadAllNovels = async () => {
  try {
    const res = await getAllNovels()
    const novelData = res.code === 200 ? res.data : []
    allBooks.value = Array.isArray(novelData) ? novelData.sort((a, b) => (b.readCount || 0) - (a.readCount || 0)) : []
  } catch (err) {
    console.error('获取所有小说失败', err)
    allBooks.value = []
  }
}

const doSearch = async (keyword) => {
  if (!keyword || !keyword.trim()) {
    await loadAllNovels()
    return
  }
  searchKeyword.value = keyword.trim()
  selectedTags.value = []
  pageSize.value = 50
  try {
    const res = await searchNovels(keyword)
    const data = res.code === 200 ? res.data : []
    allBooks.value = Array.isArray(data) ? data : []
    addToHistory(keyword.trim())
  } catch (err) {
    console.error('搜索失败', err)
    allBooks.value = []
  }
}

const loadMore = () => {
  pageSize.value += 50
}

onMounted(() => {
  loadSearchHistory()
  const q = route.query.keyword
  if (q) {
    doSearch(q)
  } else {
    loadAllNovels()
  }
})

watch(() => route.query.keyword, (newVal) => {
  if (newVal) doSearch(newVal)
  else {
    searchKeyword.value = ''
    loadAllNovels()
  }
})
</script>

<style scoped>
.library-page {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-top: 70px;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 3px solid #f8a555;
}

.search-history {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  font-size: 14px;
}
.history-label {
  color: #666;
  margin-right: 10px;
}
.history-item {
  display: inline-flex;
  align-items: center;
  margin-right: 12px;
  margin-bottom: 6px;
}
.history-link {
  color: #409eff;
  text-decoration: none;
  margin-right: 4px;
}
.history-link:hover {
  text-decoration: underline;
}
.history-delete {
  padding: 0 6px;
  line-height: 1.4;
  border: none;
  background: #f56c6c;
  color: #fff;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}
.history-delete:hover {
  background: #f78989;
}
.history-clear {
  margin-left: 8px;
  padding: 2px 10px;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #606266;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}
.history-clear:hover {
  color: #409eff;
  border-color: #409eff;
}

.welcome-tip {
  text-align: center;
  padding: 60px 20px;
  font-size: 18px;
  color: #999;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.empty-tip {
  text-align: center;
  padding: 40px 20px;
  font-size: 16px;
  color: #666;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

@media (max-width: 768px) {
  .container {
    padding: 16px 12px;
  }

  .page-title {
    font-size: 24px;
  }

  .welcome-tip {
    padding: 40px 16px;
    font-size: 16px;
  }

  .empty-tip {
    padding: 30px 16px;
    font-size: 14px;
  }
}
</style>
