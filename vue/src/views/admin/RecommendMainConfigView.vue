<template>
  <div class="main-rec-config">
    <h2 class="page-heading">个性化推荐 · 全局参数</h2>

    <p class="lead">
      以下设置对全站生效，用于用户<strong>已有阅读/收藏等行为、能算出兴趣标签</strong>时的推荐列表（首页「猜你喜欢」等）。
      与「新用户兜底 · 行为加分」页不同：那一页只影响<strong>新用户或几乎没有行为</strong>时的兜底推荐。
      每位用户是否开启协同推荐、是否做多样性重排、每次显示几条，请在用户端的<strong>推荐策略 / 标签权重配置</strong>里调整。
    </p>

    <section v-if="loadError" class="card error-card">
      {{ loadError }}
    </section>

    <section v-else class="card">
      <h3 class="card-title">每项是什么意思（可调范围见输入框旁说明）</h3>
      <ul class="explain-list">
        <li>
          <strong>热度占比</strong>（建议 0～0.1，最大可填 1）<br />
          在「标签匹配分」之外，按全书阅读量给热门书加一点分：阅读量相对全站越高的书加分越多。填 0 表示完全不考虑热度。
        </li>
        <li>
          <strong>协同推荐占比</strong>（0～1）<br />
          仅当用户自己打开了「物品协同」且系统算得出「别人也爱看的相似书」时生效：数值越大，越参考「和大家一起看」的相似度；为 0 则只看兴趣标签（再加热度）。
        </li>
        <li>
          <strong>协同参考本数上限</strong>（1～30）<br />
          从用户最常互动的若干本书出发，去找「经常和这些书一起被读」的其他书。越大覆盖面越广，服务器计算量也略增。
        </li>
        <li>
          <strong>纯协同名额上限</strong>（0～20）<br />
          最终列表里，允许出现几本「和你的兴趣标签对不上、主要靠大家行为相似推出来」的书。用于偶尔换换口味；填 0 则列表里不会出现这类书。
        </li>
        <li>
          <strong>多样性候选池大小</strong>（10～200）<br />
          先按总分选出前若干本，再从中挑出标签不那么雷同的推荐。用户关闭「多样性重排」时此项不生效。
        </li>
        <li>
          <strong>多样性平衡</strong>（0～1）<br />
          越大越优先保留分数高的书（可能题材更集中）；越小越愿意为了「标签岔开」牺牲一点分数。
        </li>
      </ul>

      <h3 class="card-title form-section-title">调整数值</h3>

      <div class="form-row">
        <label class="label-block">
          <span class="label-title">热度占比</span>
          <span class="label-sub">允许范围 0～1，建议不超过 0.1</span>
        </label>
        <input v-model.number="cfg.popWeight" type="number" min="0" max="1" step="0.005" class="field" />
      </div>
      <div class="form-row">
        <label class="label-block">
          <span class="label-title">协同推荐占比</span>
          <span class="label-sub">允许范围 0～1</span>
        </label>
        <input v-model.number="cfg.cfBlendLambda" type="number" min="0" max="1" step="0.05" class="field" />
      </div>
      <div class="form-row">
        <label class="label-block">
          <span class="label-title">协同参考本数上限</span>
          <span class="label-sub">允许范围 1～30</span>
        </label>
        <input v-model.number="cfg.cfMaxSeeds" type="number" min="1" max="30" step="1" class="field narrow" />
      </div>
      <div class="form-row">
        <label class="label-block">
          <span class="label-title">纯协同名额上限</span>
          <span class="label-sub">允许范围 0～20</span>
        </label>
        <input v-model.number="cfg.cfPureSlotCap" type="number" min="0" max="20" step="1" class="field narrow" />
      </div>
      <div class="form-row">
        <label class="label-block">
          <span class="label-title">多样性候选池大小</span>
          <span class="label-sub">允许范围 10～200</span>
        </label>
        <input v-model.number="cfg.mmrPoolCap" type="number" min="10" max="200" step="5" class="field narrow" />
      </div>
      <div class="form-row">
        <label class="label-block">
          <span class="label-title">多样性平衡</span>
          <span class="label-sub">允许范围 0～1</span>
        </label>
        <input v-model.number="cfg.mmrLambda" type="number" min="0" max="1" step="0.05" class="field" />
      </div>

      <p v-if="cfg.updatedAt" class="meta">最近更新：{{ cfg.updatedAt }}</p>
      <button type="button" class="btn-save" @click="save">保存</button>
      <p class="footer-note">
        若保存失败，请联系技术人员检查服务端是否已初始化「推荐全局参数」存储。
      </p>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import adminHttp from '@/utils/adminHttp'

const defaultCfg = () => ({
  popWeight: 0.03,
  cfBlendLambda: 0.25,
  cfMaxSeeds: 8,
  cfPureSlotCap: 4,
  mmrPoolCap: 50,
  mmrLambda: 0.7,
  updatedAt: null
})

const cfg = ref(defaultCfg())
const loadError = ref('')

const mapFromApi = (d) => {
  if (!d || typeof d !== 'object') return defaultCfg()
  return {
    popWeight: d.popWeight != null ? Number(d.popWeight) : 0.03,
    cfBlendLambda: d.cfBlendLambda != null ? Number(d.cfBlendLambda) : 0.25,
    cfMaxSeeds: d.cfMaxSeeds != null ? Number(d.cfMaxSeeds) : 8,
    cfPureSlotCap: d.cfPureSlotCap != null ? Number(d.cfPureSlotCap) : 4,
    mmrPoolCap: d.mmrPoolCap != null ? Number(d.mmrPoolCap) : 50,
    mmrLambda: d.mmrLambda != null ? Number(d.mmrLambda) : 0.7,
    updatedAt: d.updatedAt || null
  }
}

const load = async () => {
  loadError.value = ''
  try {
    const res = await adminHttp.get('/admin/recommend-main-config')
    if (res.data?.code === 200) {
      cfg.value = mapFromApi(res.data.data)
    } else {
      loadError.value = res.data?.msg || '加载失败'
    }
  } catch (e) {
    console.error(e)
    loadError.value = '无法连接服务或加载失败，下方数值仅为默认示意，不会自动保存。'
    cfg.value = defaultCfg()
  }
}

const save = async () => {
  try {
    await adminHttp.put('/admin/recommend-main-config', {
      id: 1,
      popWeight: cfg.value.popWeight,
      cfBlendLambda: cfg.value.cfBlendLambda,
      cfMaxSeeds: cfg.value.cfMaxSeeds,
      cfPureSlotCap: cfg.value.cfPureSlotCap,
      mmrPoolCap: cfg.value.mmrPoolCap,
      mmrLambda: cfg.value.mmrLambda
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
.main-rec-config { padding: 0; max-width: 800px; }
.page-heading { margin: 0 0 8px 0; font-size: 20px; }
.lead {
  margin: 0 0 20px 0;
  color: #4b5563;
  font-size: 14px;
  line-height: 1.65;
}
.form-section-title { margin-top: 8px; }
.label-block {
  width: 220px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.label-title { font-size: 14px; font-weight: 600; color: #111827; }
.label-sub { font-size: 12px; color: #6b7280; line-height: 1.4; }
.card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 18px 20px;
  background: #fafafa;
}
.error-card { background: #fef2f2; border-color: #fecaca; color: #991b1b; }
.card-title { margin: 0 0 12px 0; font-size: 15px; }
.explain-list {
  margin: 0 0 20px 0;
  padding-left: 1.2rem;
  color: #374151;
  font-size: 13px;
  line-height: 1.6;
}
.explain-list li { margin-bottom: 12px; }
.form-row { margin-bottom: 14px; display: flex; flex-wrap: wrap; align-items: flex-start; gap: 12px; }
.field { flex: 1; min-width: 120px; padding: 8px 10px; border: 1px solid #d1d5db; border-radius: 6px; }
.field.narrow { max-width: 100px; flex: none; }
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
.footer-note { margin-top: 14px; font-size: 12px; color: #6b7280; }

@media (max-width: 640px) {
  .main-rec-config { max-width: 100%; }
  .form-row { flex-direction: column; align-items: stretch; }
  .label-block { width: 100%; }
  .field.narrow { max-width: none; width: 100%; }
  .card { padding: 14px 12px; }
  .explain-list { font-size: 12px; padding-left: 1rem; }
  .btn-save { width: 100%; }
}
</style>
