<template>
  <div class="recommend-config">
    <h2 class="page-heading">推荐配置</h2>
    <p class="desc">调整标签的推荐权重，数值越大在推荐中优先级越高。</p>
    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>标签名</th>
            <th>推荐权重</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="tag in tags" :key="tag.id">
            <td>{{ tag.name }}</td>
            <td>
              <input v-model.number="tag.recommendWeight" type="number" min="0" max="1" step="0.05" class="weight-input" />
            </td>
            <td>
              <button class="btn-sm btn-ok" @click="saveWeight(tag)">保存</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <p v-if="tags.length === 0" class="empty-tip">暂无标签，请先在「系统设置」中添加标签。</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const API = 'http://localhost:8081/api/admin'
const tags = ref([])

const loadTags = async () => {
  try {
    const res = await axios.get(`${API}/tags`)
    if (res.data?.code === 200 && res.data?.data) {
      tags.value = (res.data.data || []).map(t => ({ ...t, recommendWeight: t.recommendWeight != null ? Number(t.recommendWeight) : 0.1 }))
    }
  } catch (e) {
    console.error(e)
  }
}

const saveWeight = async (tag) => {
  try {
    await axios.put(`${API}/tags/${tag.id}`, { recommendWeight: tag.recommendWeight, name: tag.name, sortOrder: tag.sortOrder ?? 0 })
    alert('已保存')
  } catch (e) {
    alert(e.response?.data?.msg || '保存失败')
  }
}

onMounted(loadTags)
</script>

<style scoped>
.recommend-config { padding: 0; }
.page-heading { margin: 0 0 8px 0; font-size: 20px; }
.desc { margin: 0 0 16px 0; color: #6b7280; font-size: 14px; }
.table-wrap { border: 1px solid #e5e7eb; border-radius: 8px; overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #e5e7eb; }
.data-table th { background: #f9fafb; font-weight: 600; }
.weight-input { width: 80px; padding: 6px 8px; border: 1px solid #d1d5db; border-radius: 4px; }
.btn-sm { padding: 4px 12px; font-size: 12px; border-radius: 4px; cursor: pointer; border: none; }
.btn-sm.btn-ok { background: #10b981; color: #fff; }
.empty-tip { color: #6b7280; margin-top: 16px; }
</style>
