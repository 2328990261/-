<template>
  <div class="book-manage">
    <h2 class="page-heading">书籍管理</h2>
    
    <!-- 标签筛选栏 -->
    <div class="filter-bar">
      <div class="filter-tabs">
        <button 
          :class="['filter-tab', { active: selectedTag === null }]" 
          @click="selectTag(null)"
        >
          全部
        </button>
        <button 
          v-for="tag in firstRowTags" 
          :key="tag"
          :class="['filter-tab', { active: selectedTag === tag }]" 
          @click="selectTag(tag)"
        >
          {{ tag }}
        </button>
      </div>
      
      <div class="search-bar">
        <input 
          v-model="searchKeyword" 
          type="text" 
          placeholder="搜索书名或作者..." 
          class="search-input"
          @keyup.enter="loadList"
        />
        <button class="btn-search" @click="loadList">查询</button>
        <button class="btn-reset" @click="resetFilter">重置</button>
      </div>
    </div>
    
    <div class="toolbar">
      <select v-model="filterStatus" @change="loadList">
        <option :value="null">全部状态</option>
        <option :value="1">上架</option>
        <option :value="0">下架</option>
      </select>
      <button class="btn-refresh" @click="loadList">刷新</button>
    </div>
    
    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>封面</th>
            <th>书名</th>
            <th>作者</th>
            <th>标签</th>
            <th>阅读量</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id">
            <td>{{ row.id }}</td>
            <td>
              <img v-if="row.cover" :src="coverUrl(row.cover)" class="cover-thumb" alt="封面" />
              <span v-else>-</span>
            </td>
            <td>{{ row.bookMainName }}</td>
            <td>{{ row.author }}</td>
            <td>{{ row.label || '-' }}</td>
            <td>{{ row.readCount }}</td>
            <td><span :class="['status-tag', row.status === 1 ? 'online' : 'offline']">{{ row.status === 1 ? '上架' : '下架' }}</span></td>
            <td>
              <button class="btn-sm" @click="openEdit(row)">编辑</button>
              <button v-if="row.status === 1" class="btn-sm btn-warn" @click="setStatus(row.id, 0)">下架</button>
              <button v-else class="btn-sm btn-ok" @click="setStatus(row.id, 1)">上架</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <div class="pagination">
      <button :disabled="page <= 1" @click="page--; loadList()">上一页</button>
      <span>第 {{ page }} 页 / 共 {{ total }} 条</span>
      <button :disabled="page * pageSize >= total" @click="page++; loadList()">下一页</button>
    </div>
    
    <div v-if="showEdit" class="modal-mask" @click.self="showEdit = false">
      <div class="modal">
        <h3>编辑书籍</h3>
        <div class="form-item"><label>书名</label><input v-model="editForm.bookMainName" /></div>
        <div class="form-item"><label>作者</label><input v-model="editForm.author" /></div>
        <div class="form-item"><label>标签（逗号分隔）</label><input v-model="editForm.label" /></div>
        <div class="form-item"><label>封面文件名</label><input v-model="editForm.cover" /></div>
        <div class="form-item"><label>状态</label><select v-model.number="editForm.status"><option :value="1">上架</option><option :value="0">下架</option></select></div>
        <div class="modal-actions">
          <button class="btn-ok" @click="submitEdit">保存</button>
          <button @click="showEdit = false">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import adminHttp from '@/utils/adminHttp'
import { getTags } from '@/api/novel'
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const filterStatus = ref(null)
const showEdit = ref(false)
const editForm = ref({ id: null, bookMainName: '', author: '', label: '', cover: '', status: 1 })

// 标签筛选相关
const firstRowTags = ref([])
const selectedTag = ref(null)
const searchKeyword = ref('')

const coverUrl = (name) => name ? `http://localhost:8081/novel/cover/${encodeURIComponent(name)}` : ''

// 加载第一行标签
const loadFirstRowTags = async () => {
  try {
    const res = await getTags()
    if (res && res.code === 200 && res.data) {
      // 只取前5个标签（第一行）
      firstRowTags.value = res.data.slice(0, 5).map(t => t.name)
    }
  } catch (e) {
    console.error('加载标签失败', e)
  }
}

const selectTag = (tag) => {
  selectedTag.value = tag
  page.value = 1
  loadList()
}

const resetFilter = () => {
  selectedTag.value = null
  searchKeyword.value = ''
  filterStatus.value = null
  page.value = 1
  loadList()
}

const loadList = async () => {
  try {
    const params = { 
      page: page.value, 
      pageSize, 
      status: filterStatus.value 
    }
    
    // 添加标签筛选
    if (selectedTag.value) {
      params.tag = selectedTag.value
    }
    
    // 添加搜索关键词
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    
    const res = await adminHttp.get('/admin/novels', { params })
    if (res.data?.code === 200 && res.data?.data) {
      list.value = res.data.data.list || []
      total.value = res.data.data.total || 0
    }
  } catch (e) {
    console.error(e)
  }
}

const openEdit = (row) => {
  editForm.value = { id: row.id, bookMainName: row.bookMainName, author: row.author, label: row.label || '', cover: row.cover || '', status: row.status }
  showEdit.value = true
}

const submitEdit = async () => {
  try {
    await adminHttp.put(`/admin/novels/${editForm.value.id}`, editForm.value)
    showEdit.value = false
    loadList()
  } catch (e) {
    alert(e?.message || e?.msg || '保存失败')
  }
}

const setStatus = async (id, status) => {
  try {
    await adminHttp.put(`/admin/novels/${id}/status`, { status })
    loadList()
  } catch (e) {
    alert(e?.msg || e?.message || '操作失败')
  }
}

onMounted(() => {
  loadFirstRowTags()
  loadList()
})
</script>

<style scoped>
.book-manage { padding: 0; }
.page-heading { margin: 0 0 20px 0; font-size: 20px; }

/* 标签筛选栏 */
.filter-bar {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.filter-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.filter-tab {
  padding: 8px 16px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
  color: #374151;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.filter-tab:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.filter-tab.active {
  background: #3b82f6;
  color: #fff;
  border-color: #3b82f6;
}

.search-bar {
  display: flex;
  gap: 8px;
  align-items: center;
}

.search-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
}

.search-input:focus {
  outline: none;
  border-color: #3b82f6;
}

.btn-search {
  padding: 8px 20px;
  background: #3b82f6;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.btn-search:hover {
  background: #2563eb;
}

.btn-reset {
  padding: 8px 20px;
  background: #6b7280;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.btn-reset:hover {
  background: #4b5563;
}

.toolbar { margin-bottom: 16px; display: flex; gap: 12px; align-items: center; }
.table-wrap { overflow-x: auto; border: 1px solid #e5e7eb; border-radius: 8px; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #e5e7eb; }
.data-table th { background: #f9fafb; font-weight: 600; }
.cover-thumb { width: 40px; height: 56px; object-fit: cover; border-radius: 4px; }
.status-tag { padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.status-tag.online { background: #d1fae5; color: #065f46; }
.status-tag.offline { background: #fee2e2; color: #991b1b; }
.btn-sm { margin-right: 6px; padding: 4px 10px; font-size: 12px; border-radius: 4px; cursor: pointer; border: 1px solid #d1d5db; background: #fff; }
.btn-sm.btn-ok { background: #10b981; color: #fff; border-color: #10b981; }
.btn-sm.btn-warn { background: #f59e0b; color: #fff; border-color: #f59e0b; }
.pagination { margin-top: 16px; display: flex; align-items: center; gap: 16px; }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal { background: #fff; padding: 24px; border-radius: 12px; min-width: 360px; }
.modal h3 { margin: 0 0 16px 0; }
.form-item { margin-bottom: 12px; }
.form-item label { display: block; margin-bottom: 4px; font-size: 13px; }
.form-item input, .form-item select { width: 100%; padding: 8px; border: 1px solid #d1d5db; border-radius: 4px; }
.modal-actions { margin-top: 20px; display: flex; gap: 10px; }
.btn-refresh { padding: 6px 14px; border: 1px solid #d1d5db; border-radius: 6px; cursor: pointer; background: #fff; }
</style>
