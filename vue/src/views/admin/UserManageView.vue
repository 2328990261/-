<template>
  <div class="user-manage">
    <h2 class="page-heading">用户管理</h2>
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
            <th>ID</th>
            <th>用户名</th>
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
            <td>{{ row.email || '-' }}</td>
            <td>{{ row.phone || '-' }}</td>
            <td><span :class="['status-tag', row.status === 0 ? 'normal' : 'disabled']">{{ row.status === 0 ? '正常' : '禁用' }}</span></td>
            <td>{{ row.createdAt || '-' }}</td>
            <td>
              <button v-if="row.status === 0" class="btn-sm btn-warn" @click="setStatus(row.id, 1)">禁用</button>
              <button v-else class="btn-sm btn-ok" @click="setStatus(row.id, 0)">启用</button>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const API = 'http://localhost:8081/api/admin'
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const keyword = ref('')
const filterStatus = ref(null)

const loadList = async () => {
  try {
    const res = await axios.get(`${API}/users`, { params: { page: page.value, pageSize, keyword: keyword.value || undefined, status: filterStatus.value } })
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
    await axios.put(`${API}/users/${id}/status`, { status })
    loadList()
  } catch (e) {
    alert(e.response?.data?.msg || '操作失败')
  }
}

onMounted(loadList)
</script>

<style scoped>
.user-manage { padding: 0; }
.page-heading { margin: 0 0 20px 0; font-size: 20px; }
.toolbar { margin-bottom: 16px; display: flex; gap: 12px; align-items: center; }
.toolbar input { padding: 8px 12px; border: 1px solid #d1d5db; border-radius: 6px; width: 200px; }
.table-wrap { overflow-x: auto; border: 1px solid #e5e7eb; border-radius: 8px; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #e5e7eb; }
.data-table th { background: #f9fafb; font-weight: 600; }
.status-tag { padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.status-tag.normal { background: #d1fae5; color: #065f46; }
.status-tag.disabled { background: #fee2e2; color: #991b1b; }
.btn-sm { padding: 4px 10px; font-size: 12px; border-radius: 4px; cursor: pointer; border: 1px solid #d1d5db; background: #fff; }
.btn-sm.btn-ok { background: #10b981; color: #fff; border-color: #10b981; }
.btn-sm.btn-warn { background: #f59e0b; color: #fff; border-color: #f59e0b; }
.pagination { margin-top: 16px; display: flex; align-items: center; gap: 16px; }
.btn-refresh { padding: 6px 14px; border: 1px solid #d1d5db; border-radius: 6px; cursor: pointer; background: #fff; }
</style>
