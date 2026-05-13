<template>
  <div class="user-manage">
    <h2 class="page-heading">用户管理</h2>
    <p class="lead">
      全部用户；在「编辑」中可将「角色」设为管理员，保存后该用户具备管理端权限。
    </p>
    <div class="toolbar">
      <input v-model="keyword" type="text" placeholder="搜索用户名/邮箱" @keyup.enter="loadList" />
      <select v-model="filterStatus" class="admin-select" @change="loadList">
        <option :value="null">全部状态</option>
        <option :value="0">正常</option>
        <option :value="1">禁用</option>
      </select>
      <button type="button" class="admin-btn admin-btn--primary" @click="loadList">查询</button>
    </div>
    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>编号</th>
            <th>用户名</th>
            <th>管理员</th>
            <th>邮箱</th>
            <th>手机</th>
            <th>状态</th>
            <th>注册时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id">
            <td>{{ row.id }}</td>
            <td>{{ row.username }}</td>
            <td>
              <span class="admin-badge" :class="row.isAdmin === 1 ? 'yes' : 'no'">
                {{ row.isAdmin === 1 ? '管理员' : '普通用户' }}
              </span>
            </td>
            <td>{{ row.email || '-' }}</td>
            <td>{{ row.phone || '-' }}</td>
            <td>
              <span :class="['status-tag', row.status === 0 ? 'normal' : 'disabled']">
                {{ row.status === 0 ? '正常' : '禁用' }}
              </span>
            </td>
            <td class="cell-time">{{ formatTime(row.createdAt) }}</td>
            <td class="cell-actions admin-ops">
              <button type="button" class="admin-btn admin-btn--secondary admin-btn--sm" @click="openEdit(row)">
                编辑
              </button>
              <button
                v-if="row.status === 0"
                type="button"
                class="admin-btn admin-btn--warn admin-btn--sm"
                @click="setStatus(row.id, 1)"
              >
                禁用
              </button>
              <button v-else type="button" class="admin-btn admin-btn--success admin-btn--sm" @click="setStatus(row.id, 0)">
                启用
              </button>
              <button type="button" class="admin-btn admin-btn--danger admin-btn--sm" @click="confirmDelete(row)">
                删除
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
        :disabled="page >= maxPage"
        @click="page++; loadList()"
      >
        下一页
      </button>
    </div>

    <!-- 编辑用户 -->
    <div v-if="editVisible" class="modal-overlay" @click.self="closeEdit">
      <div class="modal-panel" role="dialog" aria-modal="true" aria-labelledby="edit-title" @click.stop>
        <h3 id="edit-title" class="modal-title">编辑用户</h3>
        <p class="modal-hint">
          对应数据库 <code>user</code> 表：可编辑用户名、邮箱、手机、是否管理员、账号状态；新密码将使用 BCrypt 加密后写入。编号与注册时间为只读。
        </p>
        <div class="form-grid">
          <label class="ro"><span>编号</span><input :value="editForm.id" type="text" readonly class="inp-ro" /></label>
          <label
            ><span>用户名</span
            ><input v-model="editForm.username" type="text" class="inp" autocomplete="username"
          /></label>
          <label
            ><span>是否管理员</span>
            <select v-model.number="editForm.isAdmin" class="inp admin-form-select">
              <option :value="0">否（普通用户）</option>
              <option :value="1">是（管理员）</option>
            </select>
          </label>
          <label
            ><span>邮箱</span><input v-model="editForm.email" type="text" class="inp" placeholder="可留空"
          /></label>
          <label
            ><span>手机</span><input v-model="editForm.phone" type="text" class="inp" placeholder="可留空"
          /></label>
          <label
            ><span>账号状态</span>
            <select v-model.number="editForm.status" class="inp admin-form-select">
              <option :value="0">正常</option>
              <option :value="1">禁用</option>
            </select>
          </label>
          <label class="ro"
            ><span>注册时间</span><input :value="formatTime(editForm.createdAt)" type="text" readonly class="inp-ro"
          /></label>
          <label class="full"
            ><span>新密码</span
            ><input
              v-model="editForm.password"
              type="password"
              class="inp"
              placeholder="留空则不修改密码"
              autocomplete="new-password"
          /></label>
        </div>
        <div class="modal-actions">
          <button type="button" class="admin-btn admin-btn--secondary" @click="closeEdit">取消</button>
          <button type="button" class="admin-btn admin-btn--primary" :disabled="editSaving" @click="saveEdit">
            {{ editSaving ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import adminHttp from '@/utils/adminHttp'

const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const keyword = ref('')
const filterStatus = ref(null)

const maxPage = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

const editVisible = ref(false)
const editSaving = ref(false)
const editForm = ref({
  id: null,
  username: '',
  email: '',
  phone: '',
  isAdmin: 0,
  status: 0,
  password: '',
  createdAt: ''
})

const formatTime = (v) => {
  if (v == null || v === '') return '-'
  try {
    const d = new Date(v)
    if (Number.isNaN(d.getTime())) return String(v)
    return d.toLocaleString('zh-CN', { hour12: false })
  } catch {
    return String(v)
  }
}

const loadList = async () => {
  try {
    const res = await adminHttp.get('/admin/users', {
      params: {
        page: page.value,
        pageSize,
        keyword: keyword.value || undefined,
        status: filterStatus.value
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
    await loadList()
  } catch (e) {
    alert(e?.msg || e?.message || '操作失败')
  }
}

const openEdit = (row) => {
  editForm.value = {
    id: row.id,
    username: row.username || '',
    email: row.email ?? '',
    phone: row.phone ?? '',
    isAdmin: row.isAdmin === 1 ? 1 : 0,
    status: row.status === 1 ? 1 : 0,
    password: '',
    createdAt: row.createdAt
  }
  editVisible.value = true
}

const closeEdit = () => {
  editVisible.value = false
}

const saveEdit = async () => {
  const f = editForm.value
  if (!f.id) return
  editSaving.value = true
  try {
    const body = {
      username: f.username.trim(),
      email: f.email.trim() === '' ? null : f.email.trim(),
      phone: f.phone.trim() === '' ? null : f.phone.trim(),
      isAdmin: f.isAdmin,
      status: f.status
    }
    if (f.password.trim()) {
      body.password = f.password.trim()
    }
    const res = await adminHttp.put(`/admin/users/${f.id}`, body)
    if (res.data?.code === 200) {
      closeEdit()
      await loadList()
    } else {
      alert(res.data?.msg || '保存失败')
    }
  } catch (e) {
    alert(e?.msg || e?.message || '保存失败')
  } finally {
    editSaving.value = false
  }
}

const confirmDelete = (row) => {
  const name = row.username || `id=${row.id}`
  const ok = window.confirm(
    `确定删除用户「${name}」？将删除其评论、阅读记录、完读与偏好等关联数据，且不可恢复。`
  )
  if (!ok) return
  doDelete(row.id)
}

const doDelete = async (id) => {
  try {
    const res = await adminHttp.delete(`/admin/users/${id}`)
    if (res.data?.code === 200) {
      if (list.value.length === 1 && page.value > 1) {
        page.value--
      }
      await loadList()
    } else {
      alert(res.data?.msg || '删除失败')
    }
  } catch (e) {
    alert(e?.msg || e?.message || '删除失败')
  }
}

onMounted(loadList)
</script>

<style scoped>
.user-manage {
  padding: 0;
}
.page-heading {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}
.lead {
  margin: 0 0 16px 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.55;
  max-width: 720px;
}
.toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}
.toolbar input {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  width: 200px;
}
.table-wrap {
  overflow-x: auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.data-table th,
.data-table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
  vertical-align: middle;
}
.data-table th {
  background: #f9fafb;
  font-weight: 600;
  color: #374151;
}
.cell-time {
  white-space: nowrap;
  color: #64748b;
}
.cell-actions {
  white-space: nowrap;
}
.admin-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}
.admin-badge.no {
  background: #f1f5f9;
  color: #475569;
}
.admin-badge.yes {
  background: #ede9fe;
  color: #5b21b6;
}
.status-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}
.status-tag.normal {
  background: #d1fae5;
  color: #065f46;
}
.status-tag.disabled {
  background: #fee2e2;
  color: #991b1b;
}
.pagination {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

/* 编辑弹窗 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 24px;
}
.modal-panel {
  width: 100%;
  max-width: 520px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.18);
  padding: 22px 24px 20px;
  max-height: 90vh;
  overflow-y: auto;
}
.modal-title {
  margin: 0 0 10px 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}
.modal-hint {
  margin: 0 0 18px 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.55;
}
.modal-hint code {
  font-size: 11px;
  background: #f1f5f9;
  padding: 1px 5px;
  border-radius: 4px;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 14px;
}
.form-grid label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #475569;
}
.form-grid label.ro span,
.form-grid label.full span {
  font-weight: 600;
}
.form-grid label.full {
  grid-column: 1 / -1;
}
.inp,
.inp-ro {
  padding: 8px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
}
.inp-ro {
  background: #f8fafc;
  color: #64748b;
}
.modal-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
