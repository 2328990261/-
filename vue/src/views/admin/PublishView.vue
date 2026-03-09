<template>
  <div class="publish-content">
    <h1>书籍上架</h1>
    
    <!-- 第一步：创建主卷 -->
    <div class="upload-form" v-if="step === 1">
      <h2>第一步：创建主卷信息</h2>
      
      <div class="form-item">
        <label>书籍名称：</label>
        <input v-model="mainBookData.bookName" type="text" placeholder="请输入书籍名称" />
      </div>

      <div class="form-item">
        <label>作者名称：</label>
        <input v-model="mainBookData.author" type="text" placeholder="请输入作者名称" />
      </div>

      <div class="form-item">
        <label>标签：</label>
        <p class="form-hint">从下方选择标签（与前台标签栏一致，可多选）</p>
        <div v-if="tagRows.length === 0" class="tag-loading">加载标签中…</div>
        <div v-else class="publish-tag-rows">
          <div class="publish-tag-row" v-for="(row, rowIndex) in tagRows" :key="rowIndex">
            <button
              v-for="tag in row"
              :key="tag"
              type="button"
              :class="['publish-tag-btn', { active: selectedLabelTags.includes(tag) }]"
              @click="toggleLabelTag(tag)"
            >
              {{ tag }}
            </button>
          </div>
        </div>
        <div v-if="selectedLabelTags.length > 0" class="publish-selected-tags">
          <span class="selected-label">已选：</span>
          <span
            v-for="t in selectedLabelTags"
            :key="t"
            class="publish-selected-tag"
            @click="removeLabelTag(t)"
          >{{ t }} ×</span>
          <button type="button" class="publish-clear-tags" @click="selectedLabelTags = []">清空</button>
        </div>
      </div>

      <div class="form-item">
        <label>封面上传：</label>
        <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" @change="onCoverChange" class="file-input" />
        <p v-if="mainBookData.cover" class="cover-tip">已选封面：{{ mainBookData.cover }}</p>
        <p v-else class="cover-tip text-muted">上传后会自动填入，数据库存文件名</p>
      </div>

      <div class="form-actions">
        <button @click="createMainBook" class="upload-btn" :disabled="uploading">
          {{ uploading ? '创建中...' : '创建主卷' }}
        </button>
      </div>
    </div>

    <!-- 第二步：上传分卷 -->
    <div class="upload-form" v-if="step === 2">
      <h2>第二步：上传分卷内容</h2>
      
      <div class="main-book-info">
        <p><strong>主卷信息：</strong></p>
        <p>书名：{{ mainBookData.bookName }}</p>
        <p>作者：{{ mainBookData.author }}</p>
        <p>主卷ID：{{ mainBookId }}</p>
      </div>

      <div class="form-item">
        <label>选择EPUB文件（分卷）：</label>
        <input 
          type="file" 
          ref="fileInput"
          @change="handleFileChange" 
          accept=".epub"
          multiple
          class="file-input"
        />
        <div v-if="selectedFiles.length > 0" class="file-list">
          <div v-for="(file, index) in selectedFiles" :key="index" class="file-item">
            <span>{{ file.name }}</span>
            <button @click="removeFile(index)" class="remove-btn">×</button>
          </div>
        </div>
      </div>

      <div class="form-actions">
        <button @click="uploadVolumes" class="upload-btn" :disabled="uploading">
          {{ uploading ? '上传中...' : '上传分卷' }}
        </button>
        <button @click="resetForm" class="reset-btn">
          重新开始
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import axios from 'axios'
import { getTags } from '@/api/novel'
const fileInput = ref(null)
const selectedFiles = ref([])
const uploading = ref(false)
const step = ref(1)
const mainBookId = ref(null)

const mainBookData = ref({
  bookName: '',
  author: '',
  label: '',
  cover: ''
})

/** 标签栏数据：与前台一致，从后端 tag 表拉取 */
const tagRows = ref([])
const selectedLabelTags = ref([])

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
    selectedLabelTags.value = selectedLabelTags.value.filter(t => allNames.includes(t))
  } catch (e) {
    console.error('获取标签失败', e)
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

/** 与前台标签栏一致：第一行只能选一个，其他行最多选 4 个 */
function toggleLabelTag(tag) {
  const rowIndex = tagRows.value.findIndex(row => row.includes(tag))
  const firstRow = tagRows.value[0] || []

  if (selectedLabelTags.value.includes(tag)) {
    const i = selectedLabelTags.value.indexOf(tag)
    selectedLabelTags.value.splice(i, 1)
    return
  }

  if (rowIndex === 0) {
    selectedLabelTags.value = selectedLabelTags.value.filter(t => !firstRow.includes(t))
    selectedLabelTags.value.push(tag)
  } else {
    const otherRowsSelected = selectedLabelTags.value.filter(t => !firstRow.includes(t))
    if (otherRowsSelected.length >= 4) {
      alert('其他行最多只能选择 4 个标签')
      return
    }
    selectedLabelTags.value.push(tag)
  }
}

function removeLabelTag(tag) {
  const i = selectedLabelTags.value.indexOf(tag)
  if (i > -1) selectedLabelTags.value.splice(i, 1)
}

const API_BASE = 'http://localhost:8081'

const onCoverChange = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await axios.post(API_BASE + '/api/admin/uploadCover', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    if (res.data?.code === 200 && res.data?.data) {
      mainBookData.value.cover = res.data.data
    } else {
      alert(res.data?.msg || '封面上传失败')
    }
  } catch (err) {
    console.error(err)
    alert('封面上传失败')
  }
  e.target.value = ''
}

const handleFileChange = (event) => {
  const files = Array.from(event.target.files)
  selectedFiles.value = [...selectedFiles.value, ...files]
}

const removeFile = (index) => {
  selectedFiles.value.splice(index, 1)
}

// 第一步：创建主卷
const createMainBook = async () => {
  if (!mainBookData.value.bookName || !mainBookData.value.author) {
    alert('请填写书籍名称和作者')
    return
  }
  if (selectedLabelTags.value.length === 0) {
    alert('请至少选择一个标签')
    return
  }
  if (!mainBookData.value.cover) {
    alert('请先上传封面')
    return
  }
  mainBookData.value.label = selectedLabelTags.value.join(',')

  uploading.value = true

  try {
    const response = await axios.post(API_BASE + '/api/admin/createMainBook', {
      bookName: mainBookData.value.bookName,
      author: mainBookData.value.author,
      label: mainBookData.value.label,
      cover: mainBookData.value.cover
    })

    if (response.data.code === 200) {
      mainBookId.value = response.data.data
      alert('主卷创建成功！ID: ' + mainBookId.value)
      step.value = 2
    } else {
      alert('创建失败：' + response.data.msg)
    }
  } catch (error) {
    console.error('创建错误：', error)
    alert('创建失败，请检查网络连接')
  } finally {
    uploading.value = false
  }
}

// 第二步：上传分卷
const uploadVolumes = async () => {
  if (selectedFiles.value.length === 0) {
    alert('请选择至少一个EPUB文件')
    return
  }

  uploading.value = true

  try {
    const formDataToSend = new FormData()
    
    selectedFiles.value.forEach(file => {
      formDataToSend.append('files', file)
    })
    
    formDataToSend.append('mainBookId', mainBookId.value)

    const response = await axios.post(API_BASE + '/api/admin/uploadVolumes', formDataToSend, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    if (response.data.code === 200) {
      alert('分卷上传成功！')
      resetForm()
    } else {
      alert('上传失败：' + response.data.msg)
    }
  } catch (error) {
    console.error('上传错误：', error)
    alert('上传失败，请检查网络连接')
  } finally {
    uploading.value = false
  }
}

const resetForm = () => {
  mainBookData.value = {
    bookName: '',
    author: '',
    label: '',
    cover: ''
  }
  selectedLabelTags.value = []
  selectedFiles.value = []
  mainBookId.value = null
  step.value = 1
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}
</script>

<style scoped>
.publish-content {
  padding: 0;
}

h1 {
  color: #303133;
  margin-bottom: 30px;
  font-size: 24px;
}

h2 {
  color: #606266;
  font-size: 18px;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 2px solid #409eff;
}

.upload-form {
  background-color: #fff;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.main-book-info {
  background-color: #f0f9ff;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
  border-left: 4px solid #409eff;
}

.main-book-info p {
  margin: 5px 0;
  color: #606266;
}

.form-item {
  margin-bottom: 25px;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  color: #606266;
  font-weight: 500;
}

.form-item input[type="text"] {
  width: 100%;
  padding: 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.form-item input[type="text"]:focus {
  outline: none;
  border-color: #409eff;
}

.file-input {
  display: block;
  padding: 10px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  width: 100%;
}

.file-list {
  margin-top: 15px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px;
  margin-bottom: 5px;
  background-color: #fff;
  border-radius: 4px;
}

.remove-btn {
  background-color: #f56c6c;
  color: #fff;
  border: none;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
}

.remove-btn:hover {
  background-color: #f78989;
}

.form-actions {
  text-align: center;
  margin-top: 30px;
  display: flex;
  gap: 15px;
  justify-content: center;
}

.upload-btn {
  padding: 12px 40px;
  background-color: #409eff;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-btn:hover:not(:disabled) {
  background-color: #66b1ff;
}

.upload-btn:disabled {
  background-color: #a0cfff;
  cursor: not-allowed;
}

.reset-btn {
  padding: 12px 40px;
  background-color: #909399;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.reset-btn:hover {
  background-color: #a6a9ad;
}

.cover-tip { margin-top: 6px; font-size: 13px; color: #606266; }
.cover-tip.text-muted { color: #909399; }

.form-hint { margin: 0 0 10px 0; font-size: 13px; color: #909399; }
.tag-loading { padding: 10px 0; color: #909399; font-size: 13px; }
.publish-tag-rows { margin-bottom: 10px; }
.publish-tag-row { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 8px; }
.publish-tag-btn {
  padding: 6px 14px;
  border: 1px solid #dcdfe6;
  border-radius: 16px;
  background: #fff;
  color: #606266;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.publish-tag-btn:hover { border-color: #409eff; color: #409eff; }
.publish-tag-btn.active { background: #409eff; color: #fff; border-color: #409eff; }
.publish-selected-tags {
  margin-top: 10px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.publish-selected-tags .selected-label { font-size: 13px; color: #606266; }
.publish-selected-tag {
  padding: 4px 10px;
  background: #409eff;
  color: #fff;
  border-radius: 12px;
  font-size: 12px;
  cursor: pointer;
}
.publish-selected-tag:hover { opacity: 0.9; }
.publish-clear-tags {
  padding: 4px 12px;
  font-size: 12px;
  color: #409eff;
  background: #fff;
  border: 1px solid #409eff;
  border-radius: 12px;
  cursor: pointer;
}
.publish-clear-tags:hover { background: #ecf5ff; }
</style>
