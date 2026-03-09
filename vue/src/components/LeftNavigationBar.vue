<template>
  <div class="left-navigation-bar" :class="{ 'eye-protection-mode': eyeProtection }">
    <!-- 垂直导航容器 -->
    <div class="nav-vertical-container">
      <!-- 设置按钮 -->
      <button
        class="nav-btn settings-btn"
        @click="toggleSettingsPanel"
        title="设置"
      >
        <span class="btn-icon">
          <img src="../assets/设置.png" alt="设置" class="settings-icon">
        </span>
      </button>

      <!-- 返回小说详情按钮 -->
      <button
        class="nav-btn detail-btn"
        @click="handleReturnDetail"
        title="返回小说详情"
      >
        <span class="btn-icon">
          <img src="../assets/返回.png" alt="返回" class="detail-icon">
        </span>
      </button>

      <!--  上一章按钮    -->
      <button
        class="nav-btn prev-chapter-btn"
        @click="handlePrevChapterClick"
        title="上一章"
        style="z-index: 1001; cursor: pointer;"
      >
        <span class="btn-icon">
          <img src="../assets/上一章.png" alt="上一章" class="prev-chapter-icon">
        </span>
      </button>

      <!--   下一章按钮   -->
      <button
        class="nav-btn next-chapter-btn"
        @click="handleNextChapterClick"
        title="下一章"
        style="z-index: 1001; cursor: pointer;"
      >
        <span class="btn-icon">
          <img src="../assets/下一章.png" alt="下一章" class="next-chapter-icon">
        </span>
      </button>

    </div>

    <!-- 设置面板 -->
    <div class="settings-panel" v-if="showSettings" :class="{ 'eye-protection-mode': eyeProtection }">
      <div class="settings-header">
        <h3>设置</h3>
        <button class="close-btn" @click="toggleSettingsPanel">×</button>
      </div>

      <div class="settings-content">
        <!-- 护眼模式 -->
        <div class="setting-item">
          <label>护眼模式:</label>
          <div class="eye-protection-options">
            <button class="eye-protection-btn" @click="toggleEyeProtection" :class="{ 'active': eyeProtection }">
              {{ eyeProtection ? '✓ 已开启' : '✗ 未开启' }}
            </button>
          </div>
        </div>

        <!-- 字体大小 -->
        <div class="setting-item">
          <label>字体大小:</label>
          <div class="font-size-options">
            <button class="font-size-btn" :class="{ 'active': fontSize === 'small' }" @click="setFontSize('small')" data-size="small">小</button>
            <button class="font-size-btn" :class="{ 'active': fontSize === 'medium' }" @click="setFontSize('medium')" data-size="medium">中</button>
            <button class="font-size-btn" :class="{ 'active': fontSize === 'large' }" @click="setFontSize('large')" data-size="large">大</button>
          </div>
        </div>

        <!-- 页面宽度 -->
        <div class="setting-item">
          <label>页面宽度:</label>
          <div class="page-width-options">
            <button class="page-width-btn" :class="{ 'active': pageWidth === 'narrow' }" @click="setPageWidth('narrow')" data-width="narrow">窄</button>
            <button class="page-width-btn" :class="{ 'active': pageWidth === 'medium' }" @click="setPageWidth('medium')" data-width="medium">中</button>
            <button class="page-width-btn" :class="{ 'active': pageWidth === 'wide' }" @click="setPageWidth('wide')" data-width="wide">宽</button>
          </div>
        </div>

        <!-- 每页行数 -->
        <div class="setting-item">
          <label>每页行数:</label>
          <div class="page-size-options">
            <button class="page-size-btn" :class="{ 'active': pageSize === 50 }" @click="setPageSize(50)">50行</button>
            <button class="page-size-btn" :class="{ 'active': pageSize === 100 }" @click="setPageSize(100)">100行</button>
            <button class="page-size-btn" :class="{ 'active': pageSize === 150 }" @click="setPageSize(150)">150行</button>
          </div>
        </div>
      </div>

      <div class="settings-footer">
        <button class="save-btn" @click="saveSettings">保存</button>
        <button class="cancel-btn" @click="toggleSettingsPanel">取消</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'

// 定义组件属性
const props = defineProps({
  // 护眼模式状态
  eyeProtection: {
    type: Boolean,
    default: false
  }
})

// 定义事件
const emit = defineEmits(['eye-protection-change', 'font-size-change', 'page-width-change', 'page-size-change', 'return-detail', 'prev-chapter', 'next-chapter', 'settings-toggle'])

// 返回详情按钮点击处理
const handleReturnDetail = () => {
  console.log('LeftNavigationBar: return-detail button clicked')
  emit('return-detail')
}

// 上一章按钮点击处理
const handlePrevChapterClick = () => {
  console.log('LeftNavigationBar: prev-chapter button clicked')
  emit('prev-chapter')
}

// 下一章按钮点击处理
const handleNextChapterClick = () => {
  console.log('LeftNavigationBar: next-chapter button clicked')
  emit('next-chapter')
}

// 设置面板显示状态
const showSettings = ref(false)

// 护眼模式状态
const eyeProtection = ref(false)
// 字体大小状态（small, medium, large）
const fontSize = ref('medium')
// 页面宽度状态（narrow, medium, wide）
const pageWidth = ref('medium')
// 每页行数状态
const pageSize = ref(100)

// 初始化设置
onMounted(() => {
  // 初始化护眼模式
  const savedEyeProtection = localStorage.getItem('eyeProtection')
  if (savedEyeProtection) {
    eyeProtection.value = JSON.parse(savedEyeProtection)
    emit('eye-protection-change', eyeProtection.value)
  }

  // 初始化字体大小
  const savedFontSize = localStorage.getItem('fontSize')
  if (savedFontSize) {
    fontSize.value = savedFontSize
    emit('font-size-change', fontSize.value)
  }

  // 初始化页面宽度
  const savedPageWidth = localStorage.getItem('pageWidth')
  if (savedPageWidth) {
    pageWidth.value = savedPageWidth
    emit('page-width-change', pageWidth.value)
  }

  // 初始化每页行数
  const savedPageSize = localStorage.getItem('pageSize')
  if (savedPageSize) {
    pageSize.value = parseInt(savedPageSize)
    emit('page-size-change', pageSize.value)
  }
})

// 切换设置面板显示（打开时通知父组件，便于隐藏右侧评论等）
const toggleSettingsPanel = () => {
  showSettings.value = !showSettings.value
  emit('settings-toggle', showSettings.value)
}

// 保存设置
const saveSettings = () => {
  // 这里可以添加保存设置的逻辑
  console.log('保存设置')
  toggleSettingsPanel()
}

// 切换护眼模式
const toggleEyeProtection = () => {
  eyeProtection.value = !eyeProtection.value
  localStorage.setItem('eyeProtection', JSON.stringify(eyeProtection.value))
  emit('eye-protection-change', eyeProtection.value)
}

// 设置字体大小
const setFontSize = (size) => {
  fontSize.value = size
  localStorage.setItem('fontSize', size)
  emit('font-size-change', size)
}

// 设置页面宽度
const setPageWidth = (width) => {
  pageWidth.value = width
  localStorage.setItem('pageWidth', width)
  emit('page-width-change', width)
}

// 设置每页行数
const setPageSize = (size) => {
  pageSize.value = size
  localStorage.setItem('pageSize', size.toString())
  emit('page-size-change', size)
}

// 监听护眼模式变化
watch(eyeProtection, (newVal) => {
  localStorage.setItem('eyeProtection', JSON.stringify(newVal))
  emit('eye-protection-change', newVal)
})

// 监听字体大小变化
watch(fontSize, (newVal) => {
  localStorage.setItem('fontSize', newVal)
  emit('font-size-change', newVal)
})

// 监听页面宽度变化
watch(pageWidth, (newVal) => {
  localStorage.setItem('pageWidth', newVal)
  emit('page-width-change', newVal)
})

// 点击外部区域关闭设置面板
const handleClickOutside = (event) => {
  // 获取设置按钮和设置面板元素
  const settingsBtn = event.target.closest('.settings-btn')
  const settingsPanel = event.target.closest('.settings-panel')
  const leftNav = event.target.closest('.left-navigation-bar')

  // 如果点击的是设置按钮或设置面板内部，不关闭
  if (settingsBtn || settingsPanel || leftNav) {
    return
  }

  // 否则关闭设置面板
  showSettings.value = false
  emit('settings-toggle', false)
}

// 添加和移除事件监听器
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
/* 左侧导航栏容器（左移贴边） */
.left-navigation-bar {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: fixed;
  left: 16px;
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

/* 左侧导航栏护眼模式 */
.left-navigation-bar.eye-protection-mode {
  background-color: rgba(18, 18, 18, 0.95);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  border: 1px solid #333;
  backdrop-filter: blur(10px);
}

/* 左侧导航栏护眼模式下的按钮 */
.left-navigation-bar.eye-protection-mode .nav-btn {
  background-color: #1e1e1e;
  border-color: #444;
  color: #e0e0e0;
}

.left-navigation-bar.eye-protection-mode .nav-btn:hover {
  background-color: #3a3a3a;
  border-color: #666;
  color: #fff;
  transform: translateX(0);
}

/* 垂直导航容器 */
.nav-vertical-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  align-items: center;
}

/* 导航按钮 */
.nav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  background-color: #fff;
  color: #333;
  cursor: pointer;
  font-size: 20px;
  transition: all 0.3s ease;
  padding: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

/* 按钮悬停效果 */
.nav-btn:hover {
  background-color: #f8f9fa;
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  transform: translateX(0);
  transform: scale(1.05);
}

/* 设置图标和返回详情图标 */
.settings-icon,
.detail-icon,
.prev-chapter-icon,
.next-chapter-icon {
  width: 28px;
  height: 28px;
  object-fit: contain;
  transition: all 0.3s ease;
}

/* 按钮点击效果 */
.nav-btn:active {
  transform: scale(0.95);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

/* 护眼模式下的按钮点击效果 */
.left-navigation-bar.eye-protection-mode .nav-btn:active {
  transform: scale(0.95);
  box-shadow: 0 1px 4px rgba(255, 255, 255, 0.1);
}

/* 设置面板：与左侧导航栏顶部对齐（与设置按钮同高） */
.settings-panel {
  position: fixed;
  left: 96px;
  top: calc(50% - 158px);
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.15);
  width: 380px;
  z-index: 1001;
  overflow: hidden;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

/* 设置面板护眼模式 */
.settings-panel.eye-protection-mode {
  background-color: #121212;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  border: 1px solid #333;
}

/* 设置面板头部护眼模式 */
.settings-panel.eye-protection-mode .settings-header {
  background-color: #1e1e1e;
  border-color: #333;
}

.settings-panel.eye-protection-mode .settings-header h3 {
  color: #e0e0e0;
}

.settings-panel.eye-protection-mode .close-btn {
  color: #e0e0e0;
}

.settings-panel.eye-protection-mode .close-btn:hover {
  color: #fff;
}

/* 设置面板内容护眼模式 */
.settings-panel.eye-protection-mode .settings-content {
  background-color: #121212;
}

.settings-panel.eye-protection-mode .setting-item label {
  color: #e0e0e0;
}

/* 设置面板按钮护眼模式 */
.settings-panel.eye-protection-mode .font-size-btn,
.settings-panel.eye-protection-mode .page-width-btn {
  background-color: #1e1e1e;
  border-color: #444;
  color: #e0e0e0;
}

.settings-panel.eye-protection-mode .font-size-btn:hover,
.settings-panel.eye-protection-mode .page-width-btn:hover {
  background-color: #3a3a3a;
  border-color: #666;
}

.settings-panel.eye-protection-mode .font-size-btn.active,
.settings-panel.eye-protection-mode .page-width-btn.active {
  background-color: #3a3a3a;
  border-color: #666;
  color: #fff;
}

/* 设置面板底部护眼模式 */
.settings-panel.eye-protection-mode .settings-footer {
  background-color: #1e1e1e;
  border-color: #333;
}

.settings-panel.eye-protection-mode .save-btn {
  background-color: #ff8c00;
  border-color: #ff8c00;
  color: #fff;
}

.settings-panel.eye-protection-mode .save-btn:hover {
  background-color: #ff6b00;
  border-color: #ff6b00;
}

.settings-panel.eye-protection-mode .cancel-btn {
  background-color: #1e1e1e;
  border-color: #444;
  color: #e0e0e0;
}

.settings-panel.eye-protection-mode .cancel-btn:hover {
  background-color: #3a3a3a;
  border-color: #666;
}

/* 设置面板头部 */
.settings-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
  background-color: #fafafa;
}

.settings-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
  font-weight: 600;
}

/* 关闭按钮 */
.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  color: #333;
}

/* 设置面板内容 */
.settings-content {
  padding: 20px;
}

/* 设置项：标签与控件同一行、左对齐 */
.setting-item {
  display: grid;
  grid-template-columns: 90px 1fr;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.setting-item label {
  margin: 0;
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

/* 主题选项 */
.theme-options {
  display: flex;
  gap: 12px;
  align-items: center;
}

.theme-btn {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid #e0e0e0;
  cursor: pointer;
  transition: all 0.2s ease;
}

.theme-btn:nth-child(1) {
  background-color: #ffffff;
}

.theme-btn:nth-child(2) {
  background-color: #e6f7ff;
}

.theme-btn:nth-child(3) {
  background-color: #fffbe6;
}

.theme-btn:nth-child(4) {
  background-color: #f6ffed;
}

.theme-btn:nth-child(5) {
  background-color: #fff1f0;
}

.theme-btn:nth-child(6) {
  background-color: #f5f5f5;
}

.theme-btn.active {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

/* 字体大小、宽度选项 */
.font-size-options,
.page-width-options,
.page-size-options,
.eye-protection-options {
  display: flex;
  gap: 12px;
}

.font-size-btn,
.page-width-btn,
.page-size-btn,
.eye-protection-btn {
  padding: 8px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background-color: #fff;
  color: #666;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
}

/* 护眼模式按钮样式 */
.eye-protection-btn.active {
  background-color: #52c41a;
  color: #fff;
  border-color: #52c41a;
}

.eye-protection-btn:hover {
  background-color: #f5f5f5;
  border-color: #b0b0b0;
}

.eye-protection-btn.active:hover {
  background-color: #389e0d;
  border-color: #389e0d;
}

.font-size-btn:hover,
.page-width-btn:hover,
.page-size-btn:hover {
  background-color: #f5f5f5;
  border-color: #b0b0b0;
}

.font-size-btn.active,
.page-width-btn.active,
.page-size-btn.active {
  background-color: #1890ff;
  color: #fff;
  border-color: #1890ff;
}

/* 设置面板底部 */
.settings-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px;
  border-top: 1px solid #e0e0e0;
  background-color: #fafafa;
}

/* 保存和取消按钮 */
.save-btn {
  padding: 10px 24px;
  border: none;
  border-radius: 4px;
  background-color: #ff8c00;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.save-btn:hover {
  background-color: #ff6b00;
}

.cancel-btn {
  padding: 10px 24px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background-color: #fff;
  color: #666;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.cancel-btn:hover {
  background-color: #f5f5f5;
  border-color: #b0b0b0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .left-navigation-bar {
    left: 10px;
    padding: 8px;
  }

  .nav-btn {
    width: 40px;
    height: 40px;
    font-size: 16px;
  }

  .settings-icon {
    width: 20px;
    height: 20px;
  }

  .settings-panel {
    left: 72px;
    top: calc(50% - 118px);
    width: 280px;
    padding: 16px;
  }

  .settings-header,
  .settings-content,
  .settings-footer {
    padding: 16px;
  }
}
</style>
