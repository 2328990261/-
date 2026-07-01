<template>
  <div class="behavior-config">
    <h2 class="page-heading">评论热度与最近阅读 · 行为计分</h2>
    <p class="lead">
      对应库表 <code>recommend_comment_config</code>：兜底推荐里按<strong>评论条数</strong>、<strong>是否出现在最近阅读</strong>微调排序。
      下方「主推荐 · 阅读记录计分」对应 <code>recommend_score_debug.read_record_unit_bonus</code>，作用于已有画像用户的主推荐路径（与兜底「最近读过」一次性加分可并存）。
    </p>

    <div class="cards-row">
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
        </div>
        <p class="foot-hint">超过部分不再加分</p>
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
        </div>
        <p class="foot-hint">默认 5 条即可</p>
      </section>
    </div>

    <section class="card card-debug">
      <h3>主推荐 · 阅读记录计分（调试）</h3>
      <p class="desc">
        与上栏「最近记录取多少条」同一窗口：窗口内某书出现 k 次阅读记录，则在主路径 rawTag 侧额外加
        <code>k × read_record_unit_bonus</code>。默认 0 表示不叠加。
      </p>
      <div class="form-row">
        <label class="label-block">
          <span class="label-title">每条阅读记录加分系数</span>
          <span class="label-sub">允许范围 0～1，建议 0～0.05</span>
        </label>
        <input v-model.number="cfg.readRecordUnitBonus" type="number" min="0" max="1" step="0.001" class="field" />
      </div>
    </section>

    <p v-if="cfg.updatedAt" class="meta">行为配置最近更新：{{ cfg.updatedAt }}</p>
    <p v-if="cfg.scoreDebugUpdatedAt" class="meta">计分调试表最近更新：{{ cfg.scoreDebugUpdatedAt }}</p>
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
  readRecordUnitBonus: 0,
  updatedAt: null,
  scoreDebugUpdatedAt: null
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
        readRecordUnitBonus: d.readRecordUnitBonus != null ? Number(d.readRecordUnitBonus) : 0,
        updatedAt: d.updatedAt || null,
        scoreDebugUpdatedAt: d.scoreDebugUpdatedAt || null
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
      readHistoryRecentLimit: cfg.value.readHistoryRecentLimit,
      readRecordUnitBonus: cfg.value.readRecordUnitBonus
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
.behavior-config {
  padding: 0;
  max-width: 960px;
}
.page-heading {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}
.lead {
  margin: 0 0 20px 0;
  color: #4b5563;
  font-size: 14px;
  line-height: 1.65;
}
.lead code {
  font-size: 12px;
  background: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
}
.cards-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}
.card {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 18px 20px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.card-debug {
  background: #fafafa;
}
.card h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #111827;
}
.desc {
  margin: 0 0 14px 0;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.55;
}
.form-row {
  margin-bottom: 12px;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 10px;
}
.form-row label {
  width: 220px;
  flex-shrink: 0;
  font-size: 14px;
  color: #374151;
}
.label-block {
  width: 220px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.label-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}
.label-sub {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.4;
}
.field {
  flex: 1;
  min-width: 120px;
  padding: 8px 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
}
.field.narrow {
  max-width: 100px;
  flex: none;
}
.foot-hint {
  margin: 0;
  font-size: 12px;
  color: #9ca3af;
}
.meta {
  font-size: 12px;
  color: #9ca3af;
  margin: 4px 0 0 0;
}
.btn-save {
  margin-top: 16px;
  padding: 10px 28px;
  border: none;
  border-radius: 8px;
  background: #2563eb;
  color: #fff;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
}
.btn-save:hover {
  background: #1d4ed8;
}

@media (max-width: 860px) {
  .cards-row {
    grid-template-columns: 1fr;
  }
  .form-row label,
  .label-block {
    width: 100%;
  }
}
</style>
