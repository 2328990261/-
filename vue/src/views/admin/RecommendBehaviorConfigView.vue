<template>
  <div class="behavior-config">
    <h2 class="page-heading">新用户兜底 · 行为加分</h2>
    <p class="lead">
      仅当用户<strong>几乎没有阅读/收藏数据</strong>、系统只能按 Ta 选的偏好标签凑推荐时使用本页。
      在「标签是否对得上」之外，再按<strong>全站评论多少</strong>、<strong>是否出现在 Ta 最近读过的书里</strong>加一点分。
      这与「老用户」推荐里用的热度、协同推荐不是同一套规则；收藏也不会在这里单独加权（有行为后兴趣会从收藏等自动算出来）。
    </p>

    <section class="card">
      <h3>评论热度</h3>
      <p class="desc">讨论多的书在兜底列表里略微靠前（可设单书评论参与加分的条数上限）。</p>
      <div class="form-row">
        <label>是否启用评论加分</label>
        <select v-model.number="cfg.enabled" class="field">
          <option :value="1">启用</option>
          <option :value="0">关闭</option>
        </select>
      </div>
      <div class="form-row">
        <label>每条评论贡献的权重</label>
        <input v-model.number="cfg.commentWeight" type="number" min="0" step="0.001" class="field" />
      </div>
      <div class="form-row">
        <label>单书最多计几条评论</label>
        <input v-model.number="cfg.commentCountCap" type="number" min="1" class="field narrow" />
        <span class="hint">超过部分不再加分</span>
      </div>
    </section>

    <section class="card">
      <h3>最近阅读</h3>
      <p class="desc">
        只看该用户<strong>最近 N 次打开阅读</strong>记录里出现过的书；若候选书在其中，则额外加一次分（同一本书只加一次）。
      </p>
      <div class="form-row">
        <label>是否启用「最近读过」加分</label>
        <select v-model.number="cfg.readHistoryEnabled" class="field">
          <option :value="1">启用</option>
          <option :value="0">关闭</option>
        </select>
      </div>
      <div class="form-row">
        <label>命中「最近读过」时的加分系数</label>
        <input v-model.number="cfg.readHistoryWeight" type="number" min="0" step="0.001" class="field" />
      </div>
      <div class="form-row">
        <label>最近记录取多少条</label>
        <input v-model.number="cfg.readHistoryRecentLimit" type="number" min="1" max="50" class="field narrow" />
        <span class="hint">默认 5 条即可</span>
      </div>
    </section>

    <p v-if="cfg.updatedAt" class="meta">最近更新：{{ cfg.updatedAt }}</p>
    <button type="button" class="btn-save" @click="save">保存全部</button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import adminHttp from '@/utils/adminHttp'

const cfg = ref({
  enabled: 1,
  commentWeight: 0.02,
  commentCountCap: 50,
  readHistoryEnabled: 1,
  readHistoryWeight: 0.03,
  readHistoryRecentLimit: 5,
  updatedAt: null
})

const load = async () => {
  try {
    const res = await adminHttp.get('/admin/recommend-comment-config')
    if (res.data?.code === 200 && res.data?.data) {
      const d = res.data.data
      cfg.value = {
        enabled: d.enabled === 0 ? 0 : 1,
        commentWeight: d.commentWeight != null ? Number(d.commentWeight) : 0.02,
        commentCountCap: d.commentCountCap != null ? Number(d.commentCountCap) : 50,
        readHistoryEnabled: d.readHistoryEnabled === 0 ? 0 : 1,
        readHistoryWeight: d.readHistoryWeight != null ? Number(d.readHistoryWeight) : 0.03,
        readHistoryRecentLimit: d.readHistoryRecentLimit != null ? Number(d.readHistoryRecentLimit) : 5,
        updatedAt: d.updatedAt || null
      }
    }
  } catch (e) {
    console.error(e)
  }
}

const save = async () => {
  try {
    await adminHttp.put('/admin/recommend-comment-config', {
      enabled: cfg.value.enabled,
      commentWeight: cfg.value.commentWeight,
      commentCountCap: cfg.value.commentCountCap,
      readHistoryEnabled: cfg.value.readHistoryEnabled,
      readHistoryWeight: cfg.value.readHistoryWeight,
      readHistoryRecentLimit: cfg.value.readHistoryRecentLimit
    })
    alert('已保存')
    await load()
  } catch (e) {
    alert(e?.msg || e?.message || '保存失败')
  }
}

onMounted(load)
</script>

<style scoped>
.behavior-config { padding: 0; max-width: 720px; }
.page-heading { margin: 0 0 8px 0; font-size: 20px; }
.lead { margin: 0 0 20px 0; color: #4b5563; font-size: 14px; line-height: 1.6; }
.lead code { font-size: 12px; background: #f3f4f6; padding: 2px 6px; border-radius: 4px; }
.card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 18px 20px;
  margin-bottom: 16px;
  background: #fafafa;
}
.card h3 { margin: 0 0 8px 0; font-size: 15px; }
.card h3 code { font-size: 12px; font-weight: normal; }
.desc { margin: 0 0 14px 0; font-size: 13px; color: #6b7280; line-height: 1.5; }
.form-row { margin-bottom: 12px; display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
.form-row label { width: 240px; flex-shrink: 0; font-size: 14px; color: #374151; }
.field { flex: 1; min-width: 140px; padding: 8px 10px; border: 1px solid #d1d5db; border-radius: 6px; }
.field.narrow { max-width: 100px; flex: none; }
.hint { font-size: 12px; color: #6b7280; flex-basis: 100%; margin-left: 250px; }
.meta { font-size: 12px; color: #9ca3af; margin: 8px 0 14px 0; }
.btn-save {
  padding: 10px 22px;
  border: none;
  border-radius: 6px;
  background: #2563eb;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}
.btn-save:hover { background: #1d4ed8; }

@media (max-width: 640px) {
  .behavior-config { max-width: 100%; }
  .form-row label { width: 100%; }
  .hint { margin-left: 0; }
  .card { padding: 14px 12px; }
  .btn-save { width: 100%; }
}
</style>
