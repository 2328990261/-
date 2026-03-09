<template>
  <div class="library-tag-search">
    <div class="search-header">
      <div class="search-title">标签分类</div>
      <button class="toggle-btn" @click="toggleTags">
        {{ isExpanded ? '收起' : '展开' }}
        <span class="arrow" :class="{ expanded: isExpanded }">▼</span>
      </button>
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
import { ref, onMounted, onUnmounted } from 'vue'
import { getTags } from '@/api/novel'

const emit = defineEmits(['selectTags'])

const isExpanded = ref(false)
const selectedTags = ref([])
/** 标签行数据：从后端 tag 表拉取，按 sort_order 排序后按行分组，后台修改后前台同步 */
const tagRows = ref([])

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
    selectedTags.value = selectedTags.value.filter(t => allNames.includes(t))
    if (selectedTags.value.length > 0) emit('selectTags', selectedTags.value)
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
</script>

<style scoped>
.library-tag-search {
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.search-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 2px solid #f8f8f8;
  background: linear-gradient(135deg, #fff 0%, #fafbfc 100%);
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

.tags-container {
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.3s ease;
}

.tags-container.expanded {
  max-height: 600px;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 20px 24px;
  border-bottom: 1px solid #f5f5f5;
  background: #fafafa;
}

.tag-row:nth-child(odd) {
  background: #fff;
}

.tag-row:last-child {
  border-bottom: none;
}

.tag-loading {
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
  padding: 16px 24px;
  background: linear-gradient(135deg, #fff8f0 0%, #fffbf7 100%);
  border-top: 2px solid #f8f8f8;
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
