<template>
  <div class="banner-manage">
    <div class="header">
      <h2>轮播图管理</h2>
      <button type="button" class="admin-btn admin-btn--primary" @click="showAddDialog">新增轮播图</button>
    </div>

    <div class="table-container">
      <table class="banner-table">
        <thead>
          <tr>
            <th>排序</th>
            <th>轮播图</th>
            <th>标题</th>
            <th>链接类型</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="banner in bannerList" :key="banner.id">
            <td>{{ banner.sortOrder }}</td>
            <td>
              <img 
                :src="novelCover(banner.imageUrl || banner.cover)" 
                class="banner-preview"
                @error="handleImageError"
              />
            </td>
            <td>{{ banner.title || '未设置' }}</td>
            <td>
              <span class="link-type">{{ banner.linkUrl ? '自定义链接' : '小说详情' }}</span>
            </td>
            <td>
              <span :class="['status-badge', banner.status === 1 ? 'active' : 'inactive']">
                {{ banner.status === 1 ? '启用' : '禁用' }}
              </span>
            </td>
            <td class="actions admin-ops">
              <button type="button" class="admin-btn admin-btn--secondary admin-btn--sm" @click="editBanner(banner)">编辑</button>
              <button type="button" class="admin-btn admin-btn--danger admin-btn--sm" @click="deleteBanner(banner.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="bannerList.length === 0" class="empty-state">
        暂无轮播图数据
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <div v-if="showDialog" class="dialog-overlay" @click="closeDialog">
      <div class="dialog-content" @click.stop>
        <h3>{{ isEdit ? '编辑轮播图' : '新增轮播图' }}</h3>
        
        <div class="form-group">
          <label>关联小说ID（可选）</label>
          <input v-model.number="formData.novelId" type="number" placeholder="请输入小说ID" />
          <p class="field-tip">不填则仅展示自定义图片</p>
        </div>

        <div class="form-group">
          <label>标题（可选）</label>
          <input v-model="formData.title" type="text" placeholder="不填则使用小说名" />
        </div>

        <div class="form-group">
          <label>自定义图片（可选）</label>
          <div class="upload-area">
            <input 
              type="file" 
              ref="fileInput" 
              @change="handleFileChange" 
              accept="image/*"
              style="display: none"
            />
            <div v-if="!imagePreview" class="upload-placeholder" @click="$refs.fileInput.click()">
              <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                <circle cx="8.5" cy="8.5" r="1.5"></circle>
                <polyline points="21 15 16 10 5 21"></polyline>
              </svg>
              <p>点击上传图片</p>
              <p class="upload-tip">不上传则使用小说封面</p>
            </div>
            <div v-else class="image-preview">
              <img :src="imagePreview" alt="预览图" />
              <button type="button" class="admin-btn admin-btn--danger banner-remove-image" @click="removeImage">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
              </button>
            </div>
          </div>
        </div>

        <div class="form-group">
          <label>跳转链接（可选）</label>
          <input v-model="formData.linkUrl" type="text" placeholder="不填则跳转到小说详情" />
        </div>

        <div class="form-group">
          <label>排序权重（可选）</label>
          <input v-model.number="formData.sortOrder" type="number" placeholder="数字越小越靠前，默认0" />
        </div>

        <div class="form-group">
          <label>状态（可选）</label>
          <select v-model.number="formData.status" class="admin-form-select">
            <option :value="1">启用</option>
            <option :value="0">禁用</option>
          </select>
        </div>

        <div class="form-group">
          <label>开始时间（可选）</label>
          <input v-model="formData.startTime" type="datetime-local" />
        </div>

        <div class="form-group">
          <label>结束时间（可选）</label>
          <input v-model="formData.endTime" type="datetime-local" />
        </div>

        <div class="dialog-actions">
          <button type="button" class="admin-btn admin-btn--secondary" @click="closeDialog">取消</button>
          <button type="button" class="admin-btn admin-btn--primary" @click="submitForm">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import adminHttp from '@/utils/adminHttp'
import { backendUrl } from '@/config/env'

const novelCover = (name) => (name ? `${backendUrl('/novel/cover')}/${encodeURIComponent(name)}` : '')

const bannerList = ref([])
const showDialog = ref(false)
const isEdit = ref(false)
const fileInput = ref(null)
const imagePreview = ref('')
const uploadedFile = ref(null)
const formData = ref({
  id: null,
  novelId: null,
  title: '',
  imageUrl: '',
  linkUrl: '',
  sortOrder: 0,
  status: 1,
  startTime: '',
  endTime: ''
})

// 加载轮播图列表
const loadBanners = async () => {
  try {
    const res = await adminHttp.get('/admin/banner/list')
    if (res.data?.code === 200) {
      bannerList.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载轮播图失败:', error)
    alert('加载轮播图失败')
  }
}

// 显示新增弹窗
const showAddDialog = () => {
  isEdit.value = false
  imagePreview.value = ''
  uploadedFile.value = null
  formData.value = {
    id: null,
    novelId: null,
    title: '',
    imageUrl: '',
    linkUrl: '',
    sortOrder: 0,
    status: 1,
    startTime: '',
    endTime: ''
  }
  showDialog.value = true
}

// 处理文件选择
const handleFileChange = (event) => {
  const file = event.target.files[0]
  if (!file) return

  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    alert('请选择图片文件')
    return
  }

  // 验证文件大小（限制5MB）
  if (file.size > 5 * 1024 * 1024) {
    alert('图片大小不能超过5MB')
    return
  }

  uploadedFile.value = file
  
  // 生成预览
  const reader = new FileReader()
  reader.onload = (e) => {
    imagePreview.value = e.target.result
  }
  reader.readAsDataURL(file)
}

// 移除图片
const removeImage = () => {
  imagePreview.value = ''
  uploadedFile.value = null
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

// 编辑轮播图
const editBanner = (banner) => {
  isEdit.value = true
  uploadedFile.value = null
  
  // 如果有自定义图片，显示预览
  if (banner.imageUrl) {
    imagePreview.value = novelCover(banner.imageUrl)
  } else {
    imagePreview.value = ''
  }
  
  formData.value = {
    id: banner.id,
    novelId: banner.novelId,
    title: banner.title || '',
    imageUrl: banner.imageUrl || '',
    linkUrl: banner.linkUrl || '',
    sortOrder: banner.sortOrder,
    status: banner.status,
    startTime: banner.startTime ? formatDateTime(banner.startTime) : '',
    endTime: banner.endTime ? formatDateTime(banner.endTime) : ''
  }
  showDialog.value = true
}

// 格式化日期时间
const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}`
}

// 提交表单
const submitForm = async () => {
  try {
    let imageUrl = formData.value.imageUrl

    // 如果有新上传的文件，先上传图片
    if (uploadedFile.value) {
      const uploadFormData = new FormData()
      uploadFormData.append('file', uploadedFile.value)

      const uploadRes = await adminHttp.post('/admin/banner/upload-image', uploadFormData)

      if (uploadRes.data?.code === 200) {
        imageUrl = uploadRes.data.data
      } else {
        alert('图片上传失败：' + (uploadRes.data?.msg || ''))
        return
      }
    }

    // 构建提交数据，只包含有值的字段
    const data = {
      sortOrder: formData.value.sortOrder || 0,
      status: formData.value.status
    }
    
    // 只添加有值的字段
    if (isEdit.value && formData.value.id) {
      data.id = formData.value.id
    }
    if (formData.value.novelId) {
      data.novelId = formData.value.novelId
    }
    if (formData.value.title) {
      data.title = formData.value.title
    }
    if (imageUrl) {
      data.imageUrl = imageUrl
    }
    if (formData.value.linkUrl) {
      data.linkUrl = formData.value.linkUrl
    }
    if (formData.value.startTime) {
      data.startTime = formData.value.startTime
    }
    if (formData.value.endTime) {
      data.endTime = formData.value.endTime
    }

    const url = isEdit.value ? '/admin/banner/update' : '/admin/banner/add'

    const method = isEdit.value ? 'put' : 'post'

    const res = method === 'put'
      ? await adminHttp.put(url, data)
      : await adminHttp.post(url, data)

    if (res.data?.code === 200) {
      alert(isEdit.value ? '更新成功' : '新增成功')
      closeDialog()
      loadBanners()
    } else {
      alert(res.data?.msg || '操作失败')
    }
  } catch (error) {
    console.error('提交失败:', error)
    alert('操作失败：' + (e?.msg || e?.message || ''))
  }
}

// 删除轮播图
const deleteBanner = async (id) => {
  if (!confirm('确定要删除这个轮播图吗？')) {
    return
  }

  try {
    const res = await adminHttp.delete(`/admin/banner/${id}`)
    if (res.data?.code === 200) {
      alert('删除成功')
      loadBanners()
    } else {
      alert(res.data?.msg || '删除失败')
    }
  } catch (error) {
    console.error('删除失败:', error)
    alert('删除失败')
  }
}

// 关闭弹窗
const closeDialog = () => {
  showDialog.value = false
  imagePreview.value = ''
  uploadedFile.value = null
}

// 图片加载失败处理
const handleImageError = (e) => {
  e.target.src = '/src/assets/default-cover.jpg'
}

onMounted(() => {
  loadBanners()
})
</script>

<style scoped>
.banner-manage {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.table-container {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.banner-table {
  width: 100%;
  border-collapse: collapse;
}

.banner-table thead {
  background: #f5f7fa;
}

.banner-table th {
  padding: 12px 16px;
  text-align: left;
  font-weight: 600;
  color: #606266;
  font-size: 14px;
  border-bottom: 1px solid #ebeef5;
}

.banner-table td {
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  color: #606266;
  font-size: 14px;
}

.banner-preview {
  width: 120px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
}

.link-type {
  padding: 4px 8px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 4px;
  font-size: 12px;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.active {
  background: #f0f9ff;
  color: #67c23a;
}

.status-badge.inactive {
  background: #fef0f0;
  color: #f56c6c;
}

.actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.empty-state {
  padding: 60px 20px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}

/* 弹窗样式 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog-content {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  width: 500px;
  max-height: 80vh;
  overflow-y: auto;
}

.dialog-content h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  color: #333;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
}

.required {
  color: #f56c6c;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #409eff;
}

.field-tip {
  margin: 4px 0 0 0;
  font-size: 12px;
  color: #909399;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  flex-wrap: wrap;
}

/* 图片上传样式 */
.upload-area {
  width: 100%;
}

.upload-placeholder {
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-placeholder:hover {
  border-color: #409eff;
  background: #f5f7fa;
}

.upload-placeholder svg {
  color: #c0c4cc;
  margin-bottom: 12px;
}

.upload-placeholder p {
  margin: 8px 0;
  color: #606266;
  font-size: 14px;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
}

.image-preview {
  position: relative;
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
}

.image-preview img {
  width: 100%;
  height: 300px;
  object-fit: contain;
  display: block;
  background: #f5f7fa;
}

.banner-remove-image {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 34px;
  height: 34px;
  min-height: 34px;
  padding: 0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.banner-remove-image:hover:not(:disabled) {
  filter: brightness(1.06);
}

.banner-remove-image svg {
  color: #fff;
}
</style>
