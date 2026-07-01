<template>
  <div class="page">
    <Navbar />

    <div class="content">
      <div class="header">
        <h2>标签权重配置（个人）</h2>
        <div class="header-actions">
          <button class="btn" @click="goBack">返回个人中心</button>
        </div>
      </div>

      <div class="desc">
        以下为系统根据你的 <b>收藏、阅读、评论</b> 等行为计算并归一化后的<strong>个人标签占比</strong>（与推荐画像一致）。推荐以该画像为主；下方可调整协同、解释与多样性等开关。
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
          <div class="cell">个人占比(%)</div>
        </div>

        <div v-for="r in rows" :key="r.tagName" class="row">
          <div class="cell tag">{{ r.tagName }}</div>
          <div class="cell">{{ (r.weight * 100).toFixed(2) }}</div>
        </div>
      </div>

      <div v-if="!loading && rows.length === 0" class="empty-hint">暂无标签画像数据，先去书库阅读或收藏几本小说吧。</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Navbar from '@/components/Navbar.vue'
import { getUserTagWeights, getRecommendProfile, saveRecommendProfile } from '@/api/recommendProfile'

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
  rows.value = []
  try {
    let tw = null
    let pr = null
    try {
      tw = await getUserTagWeights(userId)
    } catch (e) {
      console.warn('加载标签占比失败', e)
    }
    try {
      pr = await getRecommendProfile(userId)
    } catch (e) {
      console.warn('加载推荐策略失败', e)
    }
    const list = Array.isArray(tw?.data) ? tw.data : []
    rows.value = list.map((x) => ({
      tagName: x.tagName,
      weight: Number(x.weight || 0)
    }))
    recProfile.value = mergeRecProfile(pr?.data)
  } catch (e) {
    console.error(e)
    recProfile.value = defaultRecProfile()
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
  grid-template-columns: 1fr 200px;
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
.empty-hint {
  margin-top: 12px;
  color: #6b7280;
  font-size: 14px;
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

  .table {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  .row,
  .row-head {
    min-width: 320px;
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
