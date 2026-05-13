<template>
  <div class="page">
    <Navbar />

    <div class="content">
      <div class="header">
        <h2>标签权重配置（个人）</h2>
        <div class="header-actions">
          <button class="btn" @click="goBack">返回个人中心</button>
          <button class="btn btn-primary" @click="save">保存</button>
        </div>
      </div>

      <div class="desc">
        不配置时，系统会根据你的 <b>评论/阅读/收藏</b> 动态计算标签占比；你在这里保存的标签将作为“覆盖项”优先生效，未配置的标签仍会用动态值补齐。
      </div>

      <section v-if="!loading" class="rec-strategy card-block">
        <h3 class="sub-title">推荐策略</h3>
        <p class="sub-desc">
          主推荐为<strong>基于标签的内容匹配</strong>；开启「物品协同」后会在标签分上叠加<strong>Item-Based 协同过滤</strong>（与你看过/收藏的书在用户行为上共现的其他书）。
        </p>
        <div class="strategy-row">
          <label class="strategy-label">启用物品协同（Item-CF 混合）</label>
          <select v-model.number="recProfile.cfEnabled" class="strategy-field">
            <option :value="1">开启</option>
            <option :value="0">关闭（仅标签）</option>
          </select>
        </div>
        <div class="strategy-row">
          <label class="strategy-label">返回推荐解释（接口 _explain）</label>
          <select v-model.number="recProfile.explainEnabled" class="strategy-field">
            <option :value="0">关闭</option>
            <option :value="1">开启</option>
          </select>
        </div>
        <div class="strategy-row">
          <label class="strategy-label">MMR 多样性重排</label>
          <select v-model.number="recProfile.mmrEnabled" class="strategy-field">
            <option :value="1">开启</option>
            <option :value="0">关闭（按分数直接截取）</option>
          </select>
        </div>
        <div class="strategy-actions">
          <button type="button" class="btn btn-primary" @click="saveRecProfile">保存推荐策略</button>
        </div>
      </section>

      <div v-if="loading" class="loading">加载中...</div>

      <div v-else class="table">
        <div class="row row-head">
          <div class="cell">标签名</div>
          <div class="cell">当前权重(%)</div>
          <div class="cell">自定义权重(%)</div>
          <div class="cell">操作</div>
        </div>

        <div v-for="r in rows" :key="r.tagName" class="row">
          <div class="cell tag">{{ r.tagName }}</div>
          <div class="cell">{{ (r.weight * 100).toFixed(2) }}</div>
          <div class="cell">
            <input
              class="input"
              type="number"
              min="0"
              max="100"
              step="0.01"
              v-model.number="r.customPct"
              :placeholder="(r.weight * 100).toFixed(2)"
            />
          </div>
          <div class="cell">
            <button class="btn btn-small" @click="clearOne(r)">清除</button>
          </div>
        </div>
      </div>

      <div class="footer">
        <button class="btn" @click="clearAll">清空全部自定义</button>
        <div class="hint">提示：保存后会自动归一化（总和=100%）。</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Navbar from '@/components/Navbar.vue'
import { getUserTagWeights, saveUserTagWeights, getRecommendProfile, saveRecommendProfile } from '@/api/recommendProfile'

const router = useRouter()
const loading = ref(false)
const rows = ref([])

const defaultRecProfile = () => ({
  cfEnabled: 1,
  explainEnabled: 0,
  mmrEnabled: 1,
  wCollection: 1,
  wFinished: 1,
  wReadProgress: 0.6,
  wReadDuration: 0.4,
  wComment: 0.7,
  coldStartCollectionThreshold: 3,
  recentReadLimit: 30,
  topkSimilarPerSeed: 50,
  candidateLimit: 200,
  recommendLimit: 20
})

const recProfile = ref(defaultRecProfile())

const getUserId = () => {
  try {
    const userInfoStr = localStorage.getItem('userInfo')
    if (userInfoStr && userInfoStr !== 'undefined') {
      const userInfo = JSON.parse(userInfoStr)
      if (userInfo && userInfo.id) return userInfo.id
    }
    const uid = localStorage.getItem('userId')
    if (uid) return Number(uid)
  } catch (e) {}
  return null
}

const goBack = () => router.push('/user/center')

const mergeRecProfile = (d) => {
  const base = defaultRecProfile()
  if (!d || typeof d !== 'object') return base
  return {
    ...base,
    cfEnabled: d.cfEnabled === 0 ? 0 : 1,
    explainEnabled: d.explainEnabled === 1 ? 1 : 0,
    mmrEnabled: d.mmrEnabled === 0 ? 0 : 1,
    wCollection: Number(d.wCollection ?? base.wCollection),
    wFinished: Number(d.wFinished ?? base.wFinished),
    wReadProgress: Number(d.wReadProgress ?? base.wReadProgress),
    wReadDuration: Number(d.wReadDuration ?? base.wReadDuration),
    wComment: Number(d.wComment ?? base.wComment),
    coldStartCollectionThreshold: Number(d.coldStartCollectionThreshold ?? base.coldStartCollectionThreshold),
    recentReadLimit: Number(d.recentReadLimit ?? base.recentReadLimit),
    topkSimilarPerSeed: Number(d.topkSimilarPerSeed ?? base.topkSimilarPerSeed),
    candidateLimit: Number(d.candidateLimit ?? base.candidateLimit),
    recommendLimit: Number(d.recommendLimit ?? base.recommendLimit)
  }
}

const load = async () => {
  const userId = getUserId()
  if (!userId) return
  loading.value = true
  try {
    const [tw, pr] = await Promise.all([getUserTagWeights(userId), getRecommendProfile(userId)])
    const list = Array.isArray(tw?.data) ? tw.data : []
    rows.value = list.map((x) => ({
      tagName: x.tagName,
      weight: Number(x.weight || 0),
      overridden: !!x.overridden,
      customPct: x.overridden ? Number(x.weight || 0) * 100 : null
    }))
    recProfile.value = mergeRecProfile(pr?.data)
  } finally {
    loading.value = false
  }
}

const saveRecProfile = async () => {
  const userId = getUserId()
  if (!userId) return
  try {
    await saveRecommendProfile(userId, { ...recProfile.value })
    alert('推荐策略已保存')
    const pr = await getRecommendProfile(userId)
    recProfile.value = mergeRecProfile(pr?.data)
  } catch (e) {
    console.error(e)
    alert(
      e?.message ||
        '保存失败，请确认已执行 user_recommend_profile 建表脚本；若缺 mmr_enabled 列请执行 sql/alter_user_recommend_profile_mmr.sql'
    )
  }
}

const clearOne = (r) => {
  r.customPct = null
}

const clearAll = () => {
  for (const r of rows.value) r.customPct = null
}

const save = async () => {
  const userId = getUserId()
  if (!userId) return

  // 只提交有自定义值的行；后端会“先清空再写入”
  const selected = rows.value
    .filter((r) => r.customPct != null && !Number.isNaN(Number(r.customPct)) && Number(r.customPct) >= 0)
    .map((r) => ({
      tagName: r.tagName,
      weight: Number(r.customPct) / 100
    }))

  const totalPct = selected.reduce((acc, x) => acc + Number(x.weight || 0), 0) * 100
  if (totalPct > 100.0001) {
    alert(`自定义权重总和不能超过 100%。当前总和：${totalPct.toFixed(2)}%`)
    return
  }

  try {
    await saveUserTagWeights(userId, selected)
    alert('保存成功')
    await load()
  } catch (e) {
    console.error('保存失败', e)
    alert('保存失败，请确认已执行 sql/user_tag_weight.sql 建表脚本')
  }
}

onMounted(load)
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-top: 70px;
}
.content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 10px;
}
.header h2 {
  margin: 0;
  font-size: 20px;
  color: #111827;
}
.header-actions {
  display: flex;
  gap: 10px;
}
.desc {
  background: #fff;
  border-radius: 10px;
  padding: 12px 14px;
  color: #4b5563;
  font-size: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  margin-bottom: 14px;
}
.loading {
  padding: 20px 0;
  color: #6b7280;
}
.table {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
.row {
  display: grid;
  grid-template-columns: 200px 160px 200px 120px;
  border-bottom: 1px solid #eef2f7;
}
.row:last-child {
  border-bottom: none;
}
.row-head {
  background: #f9fafb;
  font-weight: 700;
  color: #374151;
}
.cell {
  padding: 12px 14px;
  font-size: 14px;
  color: #374151;
  display: flex;
  align-items: center;
}
.tag {
  font-weight: 600;
}
.input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
}
.btn {
  padding: 8px 14px;
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #374151;
}
.btn:hover {
  background: #f3f4f6;
}
.btn-primary {
  border-color: #f8a555;
  background: #f8a555;
  color: #fff;
}
.btn-primary:hover {
  background: #e79544;
}
.btn-small {
  padding: 6px 10px;
  font-size: 13px;
}
.footer {
  margin-top: 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
}
.hint {
  color: #6b7280;
  font-size: 13px;
}
.card-block {
  background: #fff;
  border-radius: 10px;
  padding: 16px 18px;
  margin-bottom: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
.sub-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #111827;
}
.sub-desc {
  margin: 0 0 14px 0;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.55;
}
.strategy-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.strategy-label {
  flex: 1;
  min-width: 200px;
  font-size: 14px;
  color: #374151;
}
.strategy-field {
  min-width: 180px;
  padding: 8px 10px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
}
.strategy-actions {
  margin-top: 8px;
}

@media (max-width: 991px) {
  .page {
    padding-top: 64px;
  }

  .content {
    padding: 16px 14px;
  }

  .header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .footer {
    flex-direction: column;
    align-items: stretch;
  }

  .table {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  .row,
  .row-head {
    min-width: 600px;
  }
}

@media (max-width: 520px) {
  .strategy-row {
    flex-direction: column;
    align-items: stretch;
  }

  .strategy-label {
    min-width: 0;
  }

  .strategy-field {
    width: 100%;
    min-width: 0;
  }
}
</style>

