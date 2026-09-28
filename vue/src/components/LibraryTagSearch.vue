<template>
  <div class="library-tag-search">
    <div class="search-header">
      <div class="search-title">标签分类</div>
      <button class="toggle-btn" @click="toggleTags">
        {{ isExpanded ? '收起' : '展开' }}
        <span class="arrow" :class="{ expanded: isExpanded }">▼</span>
      </button>
    </div>

    <div class="tag-search-box">
      <img src="../assets/搜索.png" alt="搜索" class="tag-search-icon" />
      <input
        v-model="searchInput"
        type="text"
        class="tag-search-input"
        placeholder="输入关键词搜索标签"
        @focus="showSuggestions = true"
        @blur="hideSuggestions"
        @input="showSuggestions = true"
        @keydown.enter.prevent="selectFirstSuggestion"
      />
      <button v-if="searchInput" type="button" class="tag-search-clear" @click="clearSearchInput" title="清空">×</button>
      <div v-if="showSuggestions" class="tag-suggestions">
        <div v-if="suggestionTags.length === 0" class="suggestion-empty">没有匹配的标签</div>
        <button
          v-for="tag in suggestionTags"
          :key="tag"
          type="button"
          class="suggestion-item"
          @mousedown.prevent="pickSuggestion(tag)"
        >
          {{ tag }}
        </button>
      </div>
    </div>

    <div class="tags-container" :class="{ expanded: isExpanded }">
      <div v-if="tagRows.length === 0" class="tag-loading">加载标签中…</div>
      <div class="tag-row" v-for="(row, rowIndex) in tagRows" :key="rowIndex">
        <button
          v-for="tag in row"
          :key="tag"
          :class="{ active: selectedTags.includes(tag) }"
          @click="selectTag(tag)"
          class="tag-btn"
        >
          {{ tag }}
        </button>
      </div>
    </div>

    <div v-if="selectedTags.length > 0" class="selected-tags">
      <span class="selected-label">已选标签：</span>
      <span
        v-for="tag in selectedTags"
        :key="tag"
        class="selected-tag"
        @click="removeTag(tag)"
      >
        {{ tag }} ×
      </span>
      <button class="clear-btn" @click="clearTags">清空</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { getTags } from '@/api/novel'

const props = defineProps({
  /** 落地时由 URL label 参数带入的预选标签 */
  initialTags: { type: Array, default: () => [] }
})
const emit = defineEmits(['selectTags'])

const isExpanded = ref(false)
const selectedTags = ref([])
const searchInput = ref('')
const showSuggestions = ref(false)
const pendingInitial = ref([])
let blurTimer = null
/** 标签行数据：从后端 tag 表拉取，按 sort_order 排序后按行分组，后台修改后前台同步 */
const tagRows = ref([])

/** 可搜索标签：排除第一行（第一行已以按钮展示，避免重复） */
const searchableTags = computed(() => {
  const firstRow = tagRows.value[0] || []
  return tagRows.value.flat().filter(t => !firstRow.includes(t))
})

/** 搜索建议：按输入过滤、排除已选，最多显示 20 条 */
const suggestionTags = computed(() => {
  const kw = searchInput.value.trim().toLowerCase()
  const base = searchableTags.value.filter(t => !selectedTags.value.includes(t))
  const matched = kw ? base.filter(t => t.toLowerCase().includes(kw)) : base
  return matched.slice(0, 20)
})

/** 应用外部预选（URL label 落地 / 前进后退），与按钮选择规则一致 */
function applyInitialTags(tags) {
  if (!Array.isArray(tags) || tags.length === 0) return
  const allNames = tagRows.value.flat()
  if (allNames.length === 0) return // 标签行尚未加载完，等 loadTags 后再应用
  const firstRow = tagRows.value[0] || []
  const first = tags.filter(t => firstRow.includes(t)).slice(0, 1)
  const others = tags.filter(t => !firstRow.includes(t)).slice(0, 4)
  const next = [...first, ...others]
  const same = next.length === selectedTags.value.length && next.every((t, i) => selectedTags.value[i] === t)
  if (same) return
  selectedTags.value = next
  emit('selectTags', next)
}

watch(() => props.initialTags, (tags) => {
  pendingInitial.value = Array.isArray(tags) ? tags : []
  applyInitialTags(pendingInitial.value)
}, { immediate: true })

/** 将扁平的标签列表按每行个数拆成多行（与原先 5 行布局一致：5,8,7,7, 其余） */
function buildTagRows(tagList) {
  const names = (tagList || []).map(t => (t.name != null ? String(t.name).trim() : '')).filter(Boolean)
  const rowSizes = [5, 8, 7, 7]
  const rows = []
  let i = 0
  for (const size of rowSizes) {
    if (i >= names.length) break
    rows.push(names.slice(i, i + size))
    i += size
  }
  if (i < names.length) rows.push(names.slice(i))
  return rows
}

/** 拉取标签并更新 tagRows；切回本页时重新拉取，后台增删标签后能实时看到 */
async function loadTags() {
  try {
    const res = await getTags()
    const list = (res && res.code === 200 && res.data) ? res.data : []
    const newRows = buildTagRows(list)
    const allNames = newRows.flat()
    tagRows.value = newRows
    // 若已选中有被后台删掉的标签，从已选里移除
    const kept = selectedTags.value.filter(t => allNames.includes(t))
    if (kept.length !== selectedTags.value.length) {
      selectedTags.value = kept
      emit('selectTags', kept)
    }
    // 标签行就绪后再应用 URL 预选
    applyInitialTags(pendingInitial.value)
  } catch (e) {
    console.error('获取标签失败，使用空标签栏', e)
    tagRows.value = []
  }
}

function onWindowFocus() {
  loadTags()
}

onMounted(() => {
  loadTags()
  window.addEventListener('focus', onWindowFocus)
})

onUnmounted(() => {
  window.removeEventListener('focus', onWindowFocus)
  if (blurTimer) clearTimeout(blurTimer)
})

const toggleTags = () => {
  isExpanded.value = !isExpanded.value
}

const selectTag = (tag) => {
  const index = selectedTags.value.indexOf(tag)
  
  if (index > -1) {
    selectedTags.value.splice(index, 1)
  } else {
    const rowIndex = tagRows.value.findIndex(row => row.includes(tag))
    
    if (rowIndex === 0) {
      const firstRowSelected = selectedTags.value.filter(t => tagRows.value[0].includes(t))
      if (firstRowSelected.length > 0) {
        selectedTags.value = selectedTags.value.filter(t => !tagRows.value[0].includes(t))
      }
      selectedTags.value.push(tag)
    } else {
      const otherRowsSelected = selectedTags.value.filter(t => !tagRows.value[0].includes(t))
      if (otherRowsSelected.length >= 4) {
        alert('其他行最多只能选择4个标签')
        return
      }
      selectedTags.value.push(tag)
    }
  }
  emit('selectTags', selectedTags.value)
}

const removeTag = (tag) => {
  const index = selectedTags.value.indexOf(tag)
  if (index > -1) {
    selectedTags.value.splice(index, 1)
    emit('selectTags', selectedTags.value)
  }
}

const clearTags = () => {
  selectedTags.value = []
  emit('selectTags', [])
}

/** 从搜索建议中选取标签：选中后清空输入，下拉保持展开便于连续选择 */
const pickSuggestion = (tag) => {
  selectTag(tag)
  searchInput.value = ''
}

const selectFirstSuggestion = () => {
  if (suggestionTags.value.length > 0) pickSuggestion(suggestionTags.value[0])
}

const clearSearchInput = () => {
  searchInput.value = ''
}

const hideSuggestions = () => {
  blurTimer = setTimeout(() => {
    showSuggestions.value = false
  }, 120)
}

/** 供父组件在关键词搜索等场景清空选中（silent 时不触发 selectTags） */
const clearSelection = (emitChange = true) => {
  selectedTags.value = []
  if (emitChange) emit('selectTags', [])
}

defineExpose({ clearSelection })
</script>

<style scoped>
.library-tag-search {
  position: relative;
  background-color: #fff;
  background-image: url('../assets/标签分类背景图.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.library-tag-search::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.2);
  z-index: 0;
  pointer-events: none;
}

.search-header {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: transparent;
}

.search-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  position: relative;
  padding-left: 12px;
}

.search-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 20px;
  background: linear-gradient(180deg, #f8a555 0%, #ff8c42 100%);
  border-radius: 2px;
}

.toggle-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #f8a555 0%, #ff8c42 100%);
  color: #fff;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(248, 165, 85, 0.3);
}

.toggle-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(248, 165, 85, 0.4);
}

.arrow {
  transition: transform 0.3s ease;
  font-size: 10px;
}

.arrow.expanded {
  transform: rotate(180deg);
}

.tag-search-box {
  position: relative;
  z-index: 3;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 14px 24px 0;
  padding: 0 14px;
  height: 40px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid #ffe3c9;
  border-radius: 20px;
  box-shadow: 0 2px 8px rgba(248, 165, 85, 0.12);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.tag-search-box:focus-within {
  border-color: #f8a555;
  box-shadow: 0 2px 12px rgba(248, 165, 85, 0.25);
}

.tag-search-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  opacity: 0.75;
}

.tag-search-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  color: #333;
}

.tag-search-input::placeholder {
  color: #aaa;
}

.tag-search-clear {
  border: none;
  background: none;
  color: #bbb;
  font-size: 16px;
  line-height: 1;
  padding: 4px;
  cursor: pointer;
}

.tag-search-clear:hover {
  color: #f8a555;
}

.tag-suggestions {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  max-height: 260px;
  overflow-y: auto;
  background: #fff;
  border: 1px solid #f0e6db;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.14);
  padding: 6px;
  z-index: 20;
}

.suggestion-item {
  display: block;
  width: 100%;
  text-align: left;
  padding: 9px 12px;
  border: none;
  background: none;
  border-radius: 8px;
  font-size: 14px;
  color: #444;
  cursor: pointer;
  transition: background 0.15s ease;
}

.suggestion-item:hover {
  background: #fff3e8;
  color: #e07b26;
}

.suggestion-empty {
  padding: 12px;
  text-align: center;
  color: #bbb;
  font-size: 13px;
}

.tags-container {
  position: relative;
  z-index: 1;
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.3s ease;
}

.tags-container.expanded {
  max-height: 600px;
}

.tag-row {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  background: transparent;
}

.tag-row:nth-child(odd) {
  background: transparent;
}

.tag-row:last-child {
  border-bottom: none;
}

.tag-loading {
  position: relative;
  z-index: 1;
  padding: 20px 24px;
  color: #999;
  font-size: 14px;
  text-align: center;
}

.tag-btn {
  padding: 10px 20px;
  border: 1px solid #e8e8e8;
  border-radius: 20px;
  background-color: #fff;
  color: #666;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.tag-btn:hover {
  border-color: #f8a555;
  color: #f8a555;
  background-color: #fff8f0;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(248, 165, 85, 0.15);
}

.tag-btn.active {
  background: linear-gradient(135deg, #f8a555 0%, #ff8c42 100%);
  color: #fff;
  border-color: #f8a555;
  box-shadow: 0 4px 12px rgba(248, 165, 85, 0.3);
  transform: translateY(-2px);
}

.selected-tags {
  position: relative;
  z-index: 1;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.35);
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.selected-label {
  font-size: 14px;
  color: #666;
  font-weight: 600;
}

.selected-tag {
  padding: 8px 14px;
  background: linear-gradient(135deg, #f8a555 0%, #ff8c42 100%);
  color: #fff;
  border-radius: 18px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(248, 165, 85, 0.25);
}

.selected-tag:hover {
  background: linear-gradient(135deg, #ff8c42 0%, #f8a555 100%);
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(248, 165, 85, 0.35);
}

.clear-btn {
  padding: 8px 18px;
  background-color: #fff;
  color: #f8a555;
  border: 2px solid #f8a555;
  border-radius: 18px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-left: auto;
}

.clear-btn:hover {
  background: linear-gradient(135deg, #f8a555 0%, #ff8c42 100%);
  color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(248, 165, 85, 0.3);
}

@media (max-width: 768px) {
  .search-header {
    padding: 14px 18px;
  }

  .tag-search-box {
    margin: 12px 18px 0;
  }

  .search-title {
    font-size: 18px;
  }

  .tag-row {
    padding: 16px 18px;
    gap: 8px;
  }

  .tag-btn {
    padding: 8px 16px;
    font-size: 13px;
  }

  .selected-tags {
    padding: 12px 18px;
  }

  .selected-tag {
    padding: 6px 12px;
    font-size: 12px;
  }

  .clear-btn {
    padding: 6px 14px;
    font-size: 12px;
  }
}
</style>
