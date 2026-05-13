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
          <button type="button" class="admin-btn admin-btn--secondary admin-btn--sm" @click="selectedLabelTags = []">清空</button>
        </div>
      </div>

      <div class="form-item">
        <label>封面上传：</label>
        <input
          ref="coverInputRef"
          type="file"
          class="sr-only-file"
          accept="image/jpeg,image/png,image/gif,image/webp"
          @change="onCoverChange"
        />
        <div
          class="file-upload-card"
          role="button"
          tabindex="0"
          @click="triggerCoverPick"
          @keydown.enter.prevent="triggerCoverPick"
        >
          <div v-if="mainBookData.cover" class="cover-preview-box">
            <img :src="coverPreviewUrl" alt="封面预览" />
            <span class="upload-overlay-hint">{{ coverUploading ? '上传中…' : '点击更换封面' }}</span>
          </div>
          <div v-else class="file-upload-placeholder">
            <span class="upload-icon" aria-hidden="true">+</span>
            <p class="upload-title">点击选择封面图片</p>
            <p class="cover-tip text-muted">支持 jpg / png / gif / webp，上传后自动保存文件名</p>
            <button type="button" class="admin-btn admin-btn--secondary admin-btn--sm" @click.stop="triggerCoverPick">选择文件</button>
          </div>
        </div>
        <p v-if="mainBookData.cover" class="cover-tip">已保存：{{ mainBookData.cover }}</p>
      </div>

      <div class="form-actions">
        <button type="button" class="admin-btn admin-btn--primary admin-btn--lg" @click="createMainBook" :disabled="uploading">
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
        <label>选择 EPUB 文件（分卷）：</label>
        <input
          ref="fileInput"
          type="file"
          class="sr-only-file"
          accept=".epub"
          multiple
          @change="handleFileChange"
        />
        <div
          class="file-upload-card file-upload-card--epub"
          role="button"
          tabindex="0"
          @click="triggerEpubPick"
          @keydown.enter.prevent="triggerEpubPick"
        >
          <div class="file-upload-placeholder">
            <span class="upload-icon upload-icon--epub" aria-hidden="true">EPUB</span>
            <p class="upload-title">点击或按钮选择 EPUB（可多选）</p>
            <p class="cover-tip text-muted">选择后将显示在下方列表，确认后点「上传分卷」</p>
            <button type="button" class="admin-btn admin-btn--primary admin-btn--sm" @click.stop="triggerEpubPick">选择 EPUB</button>
          </div>
        </div>
        <div v-if="selectedFiles.length > 0" class="file-list">
          <div v-for="(file, index) in selectedFiles" :key="index" class="file-item">
            <span>{{ file.name }}</span>
            <button type="button" class="admin-btn admin-btn--danger remove-file-x" @click="removeFile(index)">×</button>
          </div>
        </div>
      </div>

      <div class="form-actions">
        <button type="button" class="admin-btn admin-btn--primary admin-btn--lg" @click="uploadVolumes" :disabled="uploading">
          {{ uploading ? '上传中...' : '上传分卷' }}
        </button>
        <button type="button" class="admin-btn admin-btn--reset admin-btn--lg" @click="resetForm">
          重新开始
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import adminHttp from '@/utils/adminHttp'
import { getTags } from '@/api/novel'
import { backendUrl } from '@/config/env'
const fileInput = ref(null)
const coverInputRef = ref(null)
const coverUploading = ref(false)
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

const coverPreviewUrl = computed(() => {
  const c = mainBookData.value.cover
  return c ? `${backendUrl('/novel/cover')}/${encodeURIComponent(c)}` : ''
})

function triggerCoverPick() {
  coverInputRef.value?.click()
}

function triggerEpubPick() {
  fileInput.value?.click()
}

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

const onCoverChange = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  coverUploading.value = true
  try {
    const res = await adminHttp.post('/admin/uploadCover', formData)
    if (res.data?.code === 200 && res.data?.data) {
      mainBookData.value.cover = res.data.data
    } else {
      alert(res.data?.msg || '封面上传失败')
    }
  } catch (err) {
    console.error(err)
    alert('封面上传失败')
  } finally {
    coverUploading.value = false
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
    const response = await adminHttp.post('/admin/createMainBook', {
      bookName: mainBookData.value.bookName,
      author: mainBookData.value.author,
      label: mainBookData.value.label,
      cover: mainBookData.value.cover
    })

    if (response.data?.code === 200) {
      mainBookId.value = response.data.data
      alert('主卷创建成功！ID: ' + mainBookId.value)
      step.value = 2
    } else {
      alert('创建失败：' + (response.data?.msg || ''))
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

    const response = await adminHttp.post('/admin/uploadVolumes', formDataToSend)

    if (response.data?.code === 200) {
      alert('分卷上传成功！')
      resetForm()
    } else {
      alert('上传失败：' + (response.data?.msg || ''))
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
  if (coverInputRef.value) {
    coverInputRef.value.value = ''
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
  position: relative;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  color: #606266;
  font-weight: 500;
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

.file-upload-card {
  position: relative;
  border: 1px dashed #c7cdd5;
  border-radius: 10px;
  background: #fafbfc;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
  outline: none;
}

.file-upload-card:hover,
.file-upload-card:focus-visible {
  border-color: #409eff;
  background: #f0f9ff;
}

.file-upload-placeholder {
  padding: 28px 20px;
  text-align: center;
}

.upload-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  margin-bottom: 10px;
  border-radius: 50%;
  background: #e8f4ff;
  color: #409eff;
  font-size: 28px;
  font-weight: 300;
  line-height: 1;
}

.upload-icon--epub {
  border-radius: 10px;
  width: auto;
  min-width: 72px;
  padding: 0 14px;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.upload-title {
  margin: 0 0 6px 0;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.cover-preview-box {
  position: relative;
  max-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  background: #f5f7fa;
}

.cover-preview-box img {
  max-width: 100%;
  max-height: 200px;
  object-fit: contain;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.upload-overlay-hint {
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 12px;
  padding: 8px 10px;
  text-align: center;
  font-size: 13px;
  color: #fff;
  background: rgba(0, 0, 0, 0.55);
  border-radius: 6px;
  pointer-events: none;
}

.file-upload-card--epub .file-upload-placeholder {
  padding: 22px 16px;
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

.remove-file-x {
  width: 28px;
  height: 28px;
  min-height: 28px;
  padding: 0;
  border-radius: 50%;
  font-size: 18px;
  line-height: 1;
}

.form-actions {
  text-align: center;
  margin-top: 30px;
  display: flex;
  gap: 15px;
  justify-content: center;
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
</style>
