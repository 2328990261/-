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
        <button type="button" class="admin-btn admin-btn--primary" @click="loadList">查询</button>
        <button type="button" class="admin-btn admin-btn--reset" @click="resetFilter">重置</button>
      </div>
    </div>
    
    <div class="toolbar">
      <select v-model="filterStatus" class="admin-select" @change="loadList">
        <option :value="null">全部状态</option>
        <option :value="1">上架</option>
        <option :value="0">下架</option>
      </select>
      <button type="button" class="admin-btn admin-btn--secondary" @click="loadList">刷新</button>
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
            <td class="admin-ops">
              <button type="button" class="admin-btn admin-btn--secondary admin-btn--sm" @click="openEdit(row)">编辑</button>
              <button
                v-if="row.status === 1"
                type="button"
                class="admin-btn admin-btn--warn admin-btn--sm"
                @click="setStatus(row.id, 0)"
              >
                下架
              </button>
              <button v-else type="button" class="admin-btn admin-btn--success admin-btn--sm" @click="setStatus(row.id, 1)">
                上架
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <div class="pagination">
      <button
        type="button"
        class="admin-btn admin-btn--secondary admin-btn--sm"
        :disabled="page <= 1"
        @click="page--; loadList()"
      >
        上一页
      </button>
      <span>第 {{ page }} 页 / 共 {{ total }} 条</span>
      <button
        type="button"
        class="admin-btn admin-btn--secondary admin-btn--sm"
        :disabled="page * pageSize >= total"
        @click="page++; loadList()"
      >
        下一页
      </button>
    </div>
    
    <div v-if="showEdit" class="modal-mask" @click.self="closeEdit">
      <div class="modal modal--wide">
        <h3>编辑书籍</h3>
        <div class="form-item"><label>书名</label><input v-model="editForm.bookMainName" type="text" /></div>
        <div class="form-item"><label>作者</label><input v-model="editForm.author" type="text" /></div>
        <div class="form-item"><label>标签（逗号分隔）</label><input v-model="editForm.label" type="text" /></div>
        <div class="form-item form-item-cover">
          <label>封面</label>
          <input
            ref="editCoverInput"
            type="file"
            class="sr-only-file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            @change="onEditCoverChange"
          />
          <div
            class="edit-cover-zone"
            role="button"
            tabindex="0"
            @click="pickEditCover"
            @keydown.enter.prevent="pickEditCover"
          >
            <img v-if="editForm.cover" :src="coverUrl(editForm.cover)" alt="封面" class="edit-cover-img" />
            <div v-else class="edit-cover-empty">点击上传封面</div>
            <span class="edit-cover-hint">{{ coverUploading ? '上传中…' : (editForm.cover ? '点击更换封面' : '') }}</span>
          </div>
        </div>
        <div class="form-item">
          <label>分卷（EPUB）</label>
          <p class="form-muted">以下为已入库分卷；可删除。新增请选择 EPUB 后点「上传新增分卷」。</p>
          <div v-if="editVolumes.length === 0" class="volume-empty">暂无分卷</div>
          <ul v-else class="volume-list">
            <li v-for="v in editVolumes" :key="v.id" class="volume-row">
              <span class="volume-name">{{ v.volumeName }}</span>
              <span class="volume-time">{{ formatVolTime(v.createTime) }}</span>
              <button type="button" class="admin-btn admin-btn--danger admin-btn--sm" @click="tryDeleteVolume(v)">删除</button>
            </li>
          </ul>
          <input
            ref="editVolumeFileInput"
            type="file"
            class="sr-only-file"
            accept=".epub"
            multiple
            @change="onEditVolumeFilesChange"
          />
          <div class="volume-toolbar">
            <button type="button" class="admin-btn admin-btn--secondary admin-btn--sm" @click="pickVolumeFiles">选择 EPUB</button>
            <button
              type="button"
              class="admin-btn admin-btn--primary admin-btn--sm"
              :disabled="volumeBatchUploading || pendingVolumeFiles.length === 0"
              @click="uploadPendingVolumesToBook"
            >
              {{ volumeBatchUploading ? '上传中…' : '上传新增分卷' }}
            </button>
          </div>
          <ul v-if="pendingVolumeFiles.length" class="pending-files">
            <li v-for="(f, i) in pendingVolumeFiles" :key="`${f.name}-${i}`" class="pending-row">
              <span class="pending-name">{{ f.name }}</span>
              <button type="button" class="admin-btn admin-btn--secondary admin-btn--sm" @click="removePendingVolumeFile(i)">移除</button>
            </li>
          </ul>
        </div>
        <div class="form-item"><label>状态</label><select v-model.number="editForm.status" class="admin-form-select"><option :value="1">上架</option><option :value="0">下架</option></select></div>
        <div class="modal-actions">
          <button type="button" class="admin-btn admin-btn--primary" @click="submitEdit">保存</button>
          <button type="button" class="admin-btn admin-btn--secondary" @click="closeEdit">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import adminHttp from '@/utils/adminHttp'
import { getTags } from '@/api/novel'
import { backendUrl } from '@/config/env'

const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const filterStatus = ref(null)
const showEdit = ref(false)
const editForm = ref({ id: null, bookMainName: '', author: '', label: '', cover: '', status: 1 })
const editVolumes = ref([])
const pendingVolumeFiles = ref([])
const editCoverInput = ref(null)
const editVolumeFileInput = ref(null)
const coverUploading = ref(false)
const volumeBatchUploading = ref(false)

// 标签筛选相关
const firstRowTags = ref([])
const selectedTag = ref(null)
const searchKeyword = ref('')

const coverUrl = (name) => (name ? `${backendUrl('/novel/cover')}/${encodeURIComponent(name)}` : '')

const formatVolTime = (t) => {
  if (t == null || t === '') return '-'
  const d = new Date(t)
  return Number.isNaN(d.getTime()) ? String(t) : d.toLocaleString('zh-CN')
}

const loadEditVolumes = async (novelId) => {
  editVolumes.value = []
  try {
    const res = await adminHttp.get(`/admin/novels/${novelId}/volumes`)
    if (res.data?.code === 200) {
      editVolumes.value = res.data.data || []
    }
  } catch (e) {
    console.error(e)
    editVolumes.value = []
  }
}

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

const pickEditCover = () => editCoverInput.value?.click()

const onEditCoverChange = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  coverUploading.value = true
  try {
    const res = await adminHttp.post('/admin/uploadCover', formData)
    if (res.data?.code === 200 && res.data?.data) {
      editForm.value.cover = res.data.data
    } else {
      alert(res.data?.msg || '封面上传失败')
    }
  } catch (err) {
    console.error(err)
    alert(err?.msg || '封面上传失败')
  } finally {
    coverUploading.value = false
  }
  e.target.value = ''
}

const pickVolumeFiles = () => editVolumeFileInput.value?.click()

const onEditVolumeFilesChange = (e) => {
  const files = Array.from(e.target.files || [])
  pendingVolumeFiles.value = [...pendingVolumeFiles.value, ...files]
  e.target.value = ''
}

const removePendingVolumeFile = (index) => {
  pendingVolumeFiles.value.splice(index, 1)
}

const uploadPendingVolumesToBook = async () => {
  if (!editForm.value.id || pendingVolumeFiles.value.length === 0) return
  volumeBatchUploading.value = true
  try {
    const fd = new FormData()
    pendingVolumeFiles.value.forEach((f) => fd.append('files', f))
    fd.append('mainBookId', String(editForm.value.id))
    const res = await adminHttp.post('/admin/uploadVolumes', fd)
    if (res.data?.code === 200) {
      pendingVolumeFiles.value = []
      await loadEditVolumes(editForm.value.id)
      await loadList()
    } else {
      alert(res.data?.msg || '分卷上传失败')
    }
  } catch (e) {
    console.error(e)
    alert(e?.msg || '分卷上传失败')
  } finally {
    volumeBatchUploading.value = false
  }
}

const tryDeleteVolume = async (vol) => {
  const name = vol.volumeName || ''
  if (!confirm(`确定删除分卷「${name}」？删除后无法恢复。`)) return
  try {
    const res = await adminHttp.delete(`/admin/novels/${editForm.value.id}/volumes/${vol.id}`)
    if (res.data?.code === 200) {
      await loadEditVolumes(editForm.value.id)
      await loadList()
    } else {
      alert(res.data?.msg || '删除失败')
    }
  } catch (e) {
    console.error(e)
    alert(e?.msg || '删除失败')
  }
}

const closeEdit = () => {
  pendingVolumeFiles.value = []
  showEdit.value = false
}

const openEdit = async (row) => {
  editForm.value = {
    id: row.id,
    bookMainName: row.bookMainName,
    author: row.author,
    label: row.label || '',
    cover: row.cover || '',
    status: row.status
  }
  pendingVolumeFiles.value = []
  showEdit.value = true
  await loadEditVolumes(row.id)
}

const submitEdit = async () => {
  try {
    await adminHttp.put(`/admin/novels/${editForm.value.id}`, editForm.value)
    closeEdit()
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

.toolbar { margin-bottom: 16px; display: flex; gap: 12px; align-items: center; }
.table-wrap { overflow-x: auto; border: 1px solid #e5e7eb; border-radius: 8px; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #e5e7eb; }
.data-table th { background: #f9fafb; font-weight: 600; }
.cover-thumb { width: 40px; height: 56px; object-fit: cover; border-radius: 4px; }
.status-tag { padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.status-tag.online { background: #d1fae5; color: #065f46; }
.status-tag.offline { background: #fee2e2; color: #991b1b; }
.pagination { margin-top: 16px; display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal { background: #fff; padding: 24px; border-radius: 12px; min-width: 360px; max-height: 90vh; overflow-y: auto; }
.modal--wide { min-width: 420px; max-width: 520px; }
.modal h3 { margin: 0 0 16px 0; }
.form-item { margin-bottom: 12px; position: relative; }
.form-item label { display: block; margin-bottom: 4px; font-size: 13px; }
.form-muted { margin: 0 0 8px 0; font-size: 12px; color: #6b7280; line-height: 1.5; }
.form-item input,
.form-item select {
  width: 100%;
  padding: 8px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  box-sizing: border-box;
}

.sr-only-file {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.edit-cover-zone {
  position: relative;
  border: 1px dashed #c7cdd5;
  border-radius: 10px;
  min-height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: #fafbfc;
  transition: border-color 0.2s, background 0.2s;
  outline: none;
}
.edit-cover-zone:hover,
.edit-cover-zone:focus-visible {
  border-color: #3b82f6;
  background: #f0f9ff;
}
.edit-cover-img {
  max-width: 100%;
  max-height: 180px;
  object-fit: contain;
  padding: 10px;
  border-radius: 8px;
}
.edit-cover-empty {
  padding: 32px 16px;
  color: #6b7280;
  font-size: 14px;
}
.edit-cover-hint {
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: 10px;
  text-align: center;
  font-size: 12px;
  color: #fff;
  background: rgba(0, 0, 0, 0.55);
  padding: 6px 8px;
  border-radius: 6px;
  pointer-events: none;
}

.volume-empty {
  font-size: 13px;
  color: #9ca3af;
  padding: 8px 0;
}
.volume-list {
  list-style: none;
  margin: 0 0 10px 0;
  padding: 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  max-height: 200px;
  overflow-y: auto;
}
.volume-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-bottom: 1px solid #f3f4f6;
  font-size: 13px;
}
.volume-row:last-child { border-bottom: none; }
.volume-name { flex: 1; min-width: 0; word-break: break-all; }
.volume-time { color: #6b7280; font-size: 12px; white-space: nowrap; }

.volume-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}
.pending-files {
  list-style: none;
  margin: 10px 0 0 0;
  padding: 0;
}
.pending-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 0;
  font-size: 13px;
  border-bottom: 1px solid #f3f4f6;
}
.pending-name { word-break: break-all; }
.modal-actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  flex-wrap: wrap;
}
</style>
