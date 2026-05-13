<template>
  <div class="tag-manage">
    <h2 class="page-heading">标签管理（系统设置）</h2>
    <div class="toolbar">
      <button type="button" class="admin-btn admin-btn--primary" @click="openAdd">新增标签</button>
    </div>
    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>标签名</th>
            <th>排序</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="tag in tags" :key="tag.id">
            <td>{{ tag.id }}</td>
            <td>{{ tag.name }}</td>
            <td>{{ tag.sortOrder }}</td>
            <td class="admin-ops">
              <button type="button" class="admin-btn admin-btn--secondary admin-btn--sm" @click="openEdit(tag)">编辑</button>
              <button type="button" class="admin-btn admin-btn--danger admin-btn--sm" @click="doDelete(tag)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-if="showModal" class="modal-mask" @click.self="showModal = false">
      <div class="modal">
        <h3>{{ editId ? '编辑标签' : '新增标签' }}</h3>
        <div class="form-item"><label>标签名</label><input v-model="form.name" placeholder="如：奇幻" /></div>
        <div class="form-item"><label>排序（数字越小越靠前）</label><input v-model.number="form.sortOrder" type="number" /></div>
        <div class="modal-actions">
          <button type="button" class="admin-btn admin-btn--primary" @click="submit">{{ editId ? '保存' : '添加' }}</button>
          <button type="button" class="admin-btn admin-btn--secondary" @click="showModal = false">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import adminHttp from '@/utils/adminHttp'

const tags = ref([])
const showModal = ref(false)
const editId = ref(null)
const form = ref({ name: '', recommendWeight: 0.1, sortOrder: 0 })

const loadTags = async () => {
  try {
    const res = await adminHttp.get('/admin/tags')
    if (res.data?.code === 200 && res.data?.data) {
      tags.value = res.data.data || []
    }
  } catch (e) {
    console.error(e)
  }
}

const openAdd = () => {
  editId.value = null
  form.value = { name: '', recommendWeight: 0.1, sortOrder: 0 }
  showModal.value = true
}

const openEdit = (tag) => {
  editId.value = tag.id
  form.value = { name: tag.name, recommendWeight: Number(tag.recommendWeight) || 0.1, sortOrder: tag.sortOrder ?? 0 }
  showModal.value = true
}

const submit = async () => {
  if (!form.value.name?.trim()) {
    alert('请输入标签名')
    return
  }
  try {
    if (editId.value) {
      await adminHttp.put(`/admin/tags/${editId.value}`, form.value)
    } else {
      await adminHttp.post('/admin/tags', form.value)
    }
    showModal.value = false
    loadTags()
  } catch (e) {
    alert(e?.message || e?.msg || '操作失败')
  }
}

const doDelete = async (tag) => {
  if (!confirm(`确定删除标签「${tag.name}」？`)) return
  try {
    await adminHttp.delete(`/admin/tags/${tag.id}`)
    loadTags()
  } catch (e) {
    alert(e?.message || e?.msg || '删除失败')
  }
}

onMounted(loadTags)
</script>

<style scoped>
.tag-manage { padding: 0; }
.page-heading { margin: 0 0 20px 0; font-size: 20px; }
.toolbar { margin-bottom: 16px; }
.table-wrap { border: 1px solid #e5e7eb; border-radius: 8px; overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #e5e7eb; }
.data-table th { background: #f9fafb; font-weight: 600; }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal { background: #fff; padding: 24px; border-radius: 12px; min-width: 320px; }
.modal h3 { margin: 0 0 16px 0; }
.form-item { margin-bottom: 12px; }
.form-item label { display: block; margin-bottom: 4px; font-size: 13px; }
.form-item input { width: 100%; padding: 8px; border: 1px solid #d1d5db; border-radius: 4px; }
.modal-actions { margin-top: 20px; display: flex; gap: 10px; justify-content: flex-end; flex-wrap: wrap; }
</style>
