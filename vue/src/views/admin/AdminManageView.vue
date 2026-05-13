<template>
  <div class="admin-manage">
    <h2 class="page-heading">管理员管理</h2>
    <p class="page-hint">仅展示管理员（is_admin = 1）。编辑中可将角色改为普通用户以取消后台权限。</p>
    <div class="toolbar">
      <input v-model="keyword" type="text" placeholder="搜索用户名/邮箱" @keyup.enter="loadList" />
      <select v-model="filterStatus" @change="loadList">
        <option :value="null">全部状态</option>
        <option :value="0">正常</option>
        <option :value="1">禁用</option>
      </select>
      <button class="btn-refresh" @click="loadList">查询</button>
    </div>
    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>id</th>
            <th>username</th>
            <th>is_admin</th>
            <th>email</th>
            <th>phone</th>
            <th>status</th>
            <th>created_at</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id">
            <td>{{ row.id }}</td>
            <td>{{ row.username }}</td>
            <td>{{ row.isAdmin === 1 ? '1（管理员）' : '0（普通）' }}</td>
            <td>{{ row.email || '-' }}</td>
            <td>{{ row.phone || '-' }}</td>
            <td><span :class="['status-tag', row.status === 0 ? 'normal' : 'disabled']">{{ row.status === 0 ? '0 正常' : '1 禁用' }}</span></td>
            <td>{{ formatDateTime(row.createdAt) }}</td>
            <td class="ops">
              <button type="button" class="btn-sm" @click="openEdit(row)">编辑</button>
              <button v-if="row.status === 0" type="button" class="btn-sm btn-warn" @click="setStatus(row.id, 1)">禁用</button>
              <button v-else type="button" class="btn-sm btn-ok" @click="setStatus(row.id, 0)">启用</button>
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
        <h3>编辑管理员</h3>
        <p class="modal-tip">对应 <code>novel_db.sql</code> 表 <code>user</code>：可编辑 username、email、phone、is_admin、status；password 经新密码写入。id、created_at 只读。</p>
        <div class="form-row">
          <label>username</label>
          <input v-model="editForm.username" type="text" maxlength="50" autocomplete="off" />
        </div>
        <div class="form-row">
          <label>is_admin</label>
          <select v-model.number="editForm.isAdmin">
            <option :value="0">0 否（普通用户）</option>
            <option :value="1">1 是（管理员）</option>
          </select>
        </div>
        <div class="form-row">
          <label>email</label>
          <input v-model="editForm.email" type="text" maxlength="100" placeholder="NULL / 空串" />
        </div>
        <div class="form-row">
          <label>phone</label>
          <input v-model="editForm.phone" type="text" maxlength="20" placeholder="NULL / 空串" />
        </div>
        <div class="form-row">
          <label>status</label>
          <select v-model.number="editForm.status">
            <option :value="0">0 正常</option>
            <option :value="1">1 禁用</option>
          </select>
        </div>
        <div class="form-row">
          <label>password</label>
          <input v-model="editForm.newPassword" type="password" maxlength="60" placeholder="留空则不修改（BCrypt 存入 password）" autocomplete="new-password" />
        </div>
        <div class="modal-actions">
          <button type="button" class="btn-refresh" @click="showEdit = false">取消</button>
          <button type="button" class="btn-save" @click="saveEdit">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import adminHttp from '@/utils/adminHttp'
import { formatDateTime } from '@/utils/formatDateTime.js'

const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const keyword = ref('')
const filterStatus = ref(null)

const showEdit = ref(false)
const editForm = ref({
  id: null,
  username: '',
  isAdmin: 1,
  email: '',
  phone: '',
  status: 0,
  newPassword: ''
})

const loadList = async () => {
  try {
    const res = await adminHttp.get('/admin/users', {
      params: {
        page: page.value,
        pageSize,
        keyword: keyword.value || undefined,
        status: filterStatus.value,
        isAdmin: 1
      }
    })
    if (res.data?.code === 200 && res.data?.data) {
      list.value = res.data.data.list || []
      total.value = res.data.data.total || 0
    }
  } catch (e) {
    console.error(e)
  }
}

const setStatus = async (id, status) => {
  try {
    await adminHttp.put(`/admin/users/${id}/status`, { status })
    loadList()
  } catch (e) {
    alert(e?.message || e?.msg || '操作失败')
  }
}

const openEdit = (row) => {
  const ia = row.isAdmin
  const isAdmin = ia === 1 || ia === '1' ? 1 : 0
  editForm.value = {
    id: row.id,
    username: row.username ?? '',
    isAdmin,
    email: row.email ?? '',
    phone: row.phone ?? '',
    status: row.status === 1 ? 1 : 0,
    newPassword: ''
  }
  showEdit.value = true
}

const saveEdit = async () => {
  const { id, username, email, phone, status, isAdmin, newPassword } = editForm.value
  if (!username.trim()) {
    alert('用户名不能为空')
    return
  }
  try {
    const body = {
      username: username.trim(),
      email: email ?? '',
      phone: phone ?? '',
      status,
      isAdmin: isAdmin === 1 ? 1 : 0
    }
    if (newPassword && String(newPassword).trim()) {
      body.newPassword = String(newPassword).trim()
    }
    const res = await adminHttp.put(`/admin/users/${id}`, body)
    if (res.data?.code === 200) {
      showEdit.value = false
      loadList()
    } else {
      alert(res.data?.msg || '保存失败')
    }
  } catch (e) {
    alert(e?.message || e?.msg || '保存失败')
  }
}

onMounted(loadList)
</script>

<style scoped>
.admin-manage { padding: 0; }
.page-heading { margin: 0 0 8px 0; font-size: 20px; }
.page-hint { margin: 0 0 16px 0; font-size: 13px; color: #6b7280; }
.toolbar { margin-bottom: 16px; display: flex; gap: 12px; align-items: center; }
.toolbar input { padding: 8px 12px; border: 1px solid #d1d5db; border-radius: 6px; width: 200px; }
.table-wrap { overflow-x: auto; border: 1px solid #e5e7eb; border-radius: 8px; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #e5e7eb; }
.data-table th { background: #f9fafb; font-weight: 600; }
.ops { display: flex; flex-wrap: wrap; gap: 8px; }
.status-tag { padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.status-tag.normal { background: #d1fae5; color: #065f46; }
.status-tag.disabled { background: #fee2e2; color: #991b1b; }
.btn-sm { padding: 4px 10px; font-size: 12px; border-radius: 4px; cursor: pointer; border: 1px solid #d1d5db; background: #fff; }
.btn-sm.btn-ok { background: #10b981; color: #fff; border-color: #10b981; }
.btn-sm.btn-warn { background: #f59e0b; color: #fff; border-color: #f59e0b; }
.pagination { margin-top: 16px; display: flex; align-items: center; gap: 16px; }
.btn-refresh { padding: 6px 14px; border: 1px solid #d1d5db; border-radius: 6px; cursor: pointer; background: #fff; }
.btn-save { padding: 6px 14px; border: 1px solid #2563eb; border-radius: 6px; cursor: pointer; background: #2563eb; color: #fff; }

.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1100; padding: 12px; box-sizing: border-box; }
.modal { background: #fff; padding: 24px; border-radius: 12px; min-width: 0; width: 100%; max-width: 420px; box-sizing: border-box; }
.modal h3 { margin: 0 0 8px 0; }
.modal-tip { margin: 0 0 16px 0; font-size: 12px; color: #6b7280; }
.modal-tip code { font-size: 11px; background: #f3f4f6; padding: 2px 6px; border-radius: 4px; }
.form-row { margin-bottom: 12px; display: flex; align-items: center; gap: 12px; }
.form-row label { width: 110px; flex-shrink: 0; font-size: 14px; color: #374151; }
.form-row input, .form-row select { flex: 1; padding: 8px 10px; border: 1px solid #d1d5db; border-radius: 6px; }
.modal-actions { margin-top: 20px; display: flex; gap: 10px; justify-content: flex-end; }

@media (max-width: 768px) {
  .toolbar { flex-wrap: wrap; align-items: stretch; }
  .toolbar input { width: 100%; max-width: none; }
  .data-table th, .data-table td { padding: 8px 10px; font-size: 13px; }
  .pagination { flex-wrap: wrap; gap: 10px; font-size: 14px; }
  .form-row { flex-direction: column; align-items: stretch; }
  .form-row label { width: 100%; }
}

@media (max-width: 480px) {
  .page-heading { font-size: 18px; }
  .page-hint { font-size: 12px; }
}
</style>
