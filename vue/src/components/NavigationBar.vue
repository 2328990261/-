<template>
  <div class="navigation-bar" :class="{ 'eye-protection-mode': eyeProtection }">
    <!-- 垂直导航容器 -->
    <div class="nav-vertical-container">
      <!-- 上一页按钮 -->
      <button
        class="nav-btn back-btn"
        @click="$emit('prev-page')"
        :disabled="!canGoPrev"
        title="上一页"
      >
        <img src="../assets/上一页.png" alt="上一页" />
        <span class="btn-text">上一页</span>
      </button>

      <!-- 下一页按钮 -->
      <button
        class="nav-btn forward-btn"
        @click="$emit('next-page')"
        :disabled="!canGoNext"
        title="下一页"
      >
        <img src="../assets/下一页.png" alt="下一页" />
        <span class="btn-text">下一页</span>
      </button>

      <!-- 收藏按钮 -->
      <button
        class="nav-btn favorite-btn"
        @click="$emit('toggle-favorite')"
        :class="{ 'favorited': isFavorited }"
        title="收藏"
      >
        <span class="btn-icon">
          <span v-if="isFavorited">★</span>
          <span v-else>☆</span>
        </span>
        <span class="btn-text">收藏</span>
      </button>

      <!--  回到顶部按钮    -->
      <button
        class="nav-btn return-top"
        @click="scrollToTop"
        title="回到顶部"
      >
        <img src="../assets/回到顶部.png" alt="回到顶部" />
        <span class="btn-text">回到顶部</span>
      </button>
    </div>
  </div>
</template>

<script setup>
// 定义组件属性
defineProps({
  // 是否可以上一页
  canGoPrev: {
    type: Boolean,
    default: false
  },
  // 是否可以下一页
  canGoNext: {
    type: Boolean,
    default: false
  },
  // 是否已收藏
  isFavorited: {
    type: Boolean,
    default: false
  },
  // 护眼模式状态
  eyeProtection: {
    type: Boolean,
    default: false
  }
})

// 定义事件
const emit = defineEmits([
  'prev-page',   // 上一页事件
  'next-page',   // 下一页事件
  'toggle-favorite'  // 收藏事件
])
// 回到顶部函数
const scrollToTop = () => {
  window.scrollTo({
    top: 0,
    behavior: 'auto' // 直接跳转，没有平滑滚动效果
  })
}
</script>

<style scoped>
.navigation-bar {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: fixed;
  left: 112px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 1000;
  background-color: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

/* 护眼模式下的导航栏 */
.navigation-bar.eye-protection-mode {
  background-color: rgba(18, 18, 18, 0.95);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  border: 1px solid #333;
}

/* 护眼模式下的导航按钮 */
.navigation-bar.eye-protection-mode .nav-btn {
  background-color: #1e1e1e;
  border-color: #444;
  color: #e0e0e0;
}

.navigation-bar.eye-protection-mode .nav-btn:hover:not(:disabled) {
  background-color: #3a3a3a;
  border-color: #666;
  color: #fff;
}

.navigation-bar.eye-protection-mode .nav-btn:disabled {
  background-color: #1e1e1e;
  border-color: #333;
  color: #666;
  cursor: not-allowed;
}

/* 护眼模式下的收藏按钮 */
.navigation-bar.eye-protection-mode .favorite-btn.favorited {
  background-color: #1e1e1e;
  border-color: #ff8c00;
  color: #ff8c00;
}

.navigation-bar.eye-protection-mode .favorite-btn.favorited:hover:not(:disabled) {
  background-color: #3a3a3a;
  border-color: #ff6b00;
  color: #ff6b00;
}



.nav-vertical-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  align-items: center;
}

.nav-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 24px;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  background-color: #fff;
  color: #333;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  min-width: 150px;
  width: 150px;
  justify-content: center;
  box-sizing: border-box;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

/* 按钮悬停效果 */
.nav-btn:hover:not(:disabled) {
  background-color: #f8f9fa;
  border-color: #409eff;
  color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  transform: translateX(0);
  transform: scale(1.05);
}

.nav-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  background-color: #f5f5f5;
  box-shadow: none;
}

/* 按钮点击效果 */
.nav-btn:active:not(:disabled) {
  transform: scale(0.95);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

/* 护眼模式下的按钮点击效果 */
.navigation-bar.eye-protection-mode .nav-btn:active:not(:disabled) {
  transform: scale(0.95);
  box-shadow: 0 1px 4px rgba(255, 255, 255, 0.1);
}

/* 按钮图标（图片）样式 */
.nav-btn img {
  width: 18px;
  height: 18px;
  object-fit: contain;
  transition: all 0.3s ease;
}

/* 显示文字，方便用户使用 */
.btn-text {
  display: inline;
}

/* 收藏状态特殊样式 */
.favorite-btn.favorited {
  background-color: #fff;
  border-color: #ff8c00;
  color: #ff8c00;
}

.favorite-btn.favorited:hover:not(:disabled) {
  background-color: #fff8f0;
  border-color: #ff6b00;
  color: #ff6b00;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .navigation-bar {
    left: 72px;
    padding: 8px;
  }

  .nav-btn {
    padding: 8px 12px;
    min-width: 80px;
    font-size: 13px;
  }

  .btn-icon {
    font-size: 13px;
  }
}
</style>
