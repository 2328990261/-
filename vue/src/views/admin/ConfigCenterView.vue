<template>
  <div class="config-center">
    <h2 class="page-title">配置管理中心</h2>
    
    <div class="config-cards">
      <div class="config-card" @click="goToBannerManage">
        <div class="card-icon">
          <svg viewBox="0 0 1024 1024" width="48" height="48">
            <path d="M928 160H96c-17.7 0-32 14.3-32 32v640c0 17.7 14.3 32 32 32h832c17.7 0 32-14.3 32-32V192c0-17.7-14.3-32-32-32z m-40 632H136V232h752v560z" fill="#409EFF"/>
            <path d="M304 472c0 4.4 3.6 8 8 8h56c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8h-56c-4.4 0-8 3.6-8 8v56z m0 160c0 4.4 3.6 8 8 8h56c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8h-56c-4.4 0-8 3.6-8 8v56z" fill="#409EFF"/>
          </svg>
        </div>
        <div class="card-content">
          <h3 class="card-title">轮播图管理</h3>
          <p class="card-desc">首页轮播图配置，设置图片、链接和排序</p>
        </div>
        <div class="card-arrow">→</div>
      </div>

      <!-- 预留其他配置项 -->
      <div class="config-card disabled">
        <div class="card-icon">
          <svg viewBox="0 0 1024 1024" width="48" height="48">
            <path d="M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448S759.4 64 512 64z m0 820c-205.4 0-372-166.6-372-372s166.6-372 372-372 372 166.6 372 372-166.6 372-372 372z" fill="#DCDFE6"/>
            <path d="M464 336a48 48 0 1 0 96 0 48 48 0 1 0-96 0z m72 112h-48c-4.4 0-8 3.6-8 8v272c0 4.4 3.6 8 8 8h48c4.4 0 8-3.6 8-8V456c0-4.4-3.6-8-8-8z" fill="#DCDFE6"/>
          </svg>
        </div>
        <div class="card-content">
          <h3 class="card-title">隐私协议</h3>
          <p class="card-desc">用户隐私政策和服务协议配置</p>
        </div>
        <div class="card-badge">即将上线</div>
      </div>

      <div class="config-card disabled">
        <div class="card-icon">
          <svg viewBox="0 0 1024 1024" width="48" height="48">
            <path d="M880 112H144c-17.7 0-32 14.3-32 32v736c0 17.7 14.3 32 32 32h736c17.7 0 32-14.3 32-32V144c0-17.7-14.3-32-32-32z m-40 728H184V184h656v656z" fill="#DCDFE6"/>
            <path d="M492 400h184c4.4 0 8-3.6 8-8v-48c0-4.4-3.6-8-8-8H492c-4.4 0-8 3.6-8 8v48c0 4.4 3.6 8 8 8z m0 144h184c4.4 0 8-3.6 8-8v-48c0-4.4-3.6-8-8-8H492c-4.4 0-8 3.6-8 8v48c0 4.4 3.6 8 8 8z m0 144h184c4.4 0 8-3.6 8-8v-48c0-4.4-3.6-8-8-8H492c-4.4 0-8 3.6-8 8v48c0 4.4 3.6 8 8 8z" fill="#DCDFE6"/>
          </svg>
        </div>
        <div class="card-content">
          <h3 class="card-title">收益配置</h3>
          <p class="card-desc">通过收益、分成规则、支付配置</p>
        </div>
        <div class="card-badge">即将上线</div>
      </div>

      <div class="config-card disabled">
        <div class="card-icon">
          <svg viewBox="0 0 1024 1024" width="48" height="48">
            <path d="M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448S759.4 64 512 64z m0 820c-205.4 0-372-166.6-372-372s166.6-372 372-372 372 166.6 372 372-166.6 372-372 372z" fill="#DCDFE6"/>
            <path d="M686.7 638.6L544.1 535.5V288c0-4.4-3.6-8-8-8h-48c-4.4 0-8 3.6-8 8v275.4c0 2.6 1.2 5 3.3 6.5l165.4 120.6c3.6 2.6 8.6 1.8 11.2-1.7l28.6-39c2.6-3.7 1.8-8.7-1.9-11.2z" fill="#DCDFE6"/>
          </svg>
        </div>
        <div class="card-content">
          <h3 class="card-title">联系客服</h3>
          <p class="card-desc">客服联系方式、工作时间、客服团队配置</p>
        </div>
        <div class="card-badge">即将上线</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goToBannerManage = () => {
  router.push({ name: 'AdminBannerManage' })
}
</script>

<style scoped>
.config-center {
  padding: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0 0 24px 0;
}

.config-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.config-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
  cursor: pointer;
  position: relative;
}

.config-card:not(.disabled):hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
}

.config-card.disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.card-icon {
  flex-shrink: 0;
  width: 64px;
  height: 64px;
  background: #f0f9ff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.config-card.disabled .card-icon {
  background: #f5f7fa;
}

.card-content {
  flex: 1;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.card-desc {
  font-size: 14px;
  color: #909399;
  margin: 0;
  line-height: 1.5;
}

.card-arrow {
  font-size: 24px;
  color: #409eff;
  font-weight: bold;
}

.config-card.disabled .card-arrow {
  display: none;
}

.card-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 12px;
  background: #f0f9ff;
  color: #409eff;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

@media (max-width: 768px) {
  .config-cards {
    grid-template-columns: 1fr;
  }
}
</style>
