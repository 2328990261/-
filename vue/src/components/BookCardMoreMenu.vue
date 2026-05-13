<template>
  <div
    class="card-more"
    :class="{ 'is-open': clickOpen, 'card-more--footer': variant === 'footer' }"
    @click.stop
    @mousedown.stop
  >
    <button
      type="button"
      class="card-more-dots"
      aria-haspopup="menu"
      :aria-expanded="clickOpen"
      aria-label="更多操作"
      @click.stop="onDotsClick"
    >
      <span class="dot" />
      <span class="dot" />
      <span class="dot" />
    </button>
    <div class="card-more-menu" role="menu">
      <button type="button" class="card-more-item" role="menuitem" @click.stop="onCollect">
        收藏
      </button>
      <button type="button" class="card-more-item" role="menuitem" @click.stop="onDislike">
        不感兴趣
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

/** overlay：封面角标；footer：作者行右侧，菜单在 ⋮ 下方展开 */
defineProps({
  variant: {
    type: String,
    default: 'overlay',
    validator: (v) => ['overlay', 'footer'].includes(v)
  }
})

const emit = defineEmits(['dislike', 'collect'])

/** 仅触控/无悬停设备：用点击开关菜单；有鼠标的设备只靠 CSS :hover + :focus-within */
const clickOpen = ref(false)
let clickMode = false

function syncClickMode() {
  clickMode = window.matchMedia('(hover: none)').matches
}

function onDotsClick() {
  if (!clickMode) return
  clickOpen.value = !clickOpen.value
}

function onDocClick() {
  if (clickMode) clickOpen.value = false
}

function onCollect() {
  clickOpen.value = false
  emit('collect')
}

function onDislike() {
  clickOpen.value = false
  emit('dislike')
}

onMounted(() => {
  syncClickMode()
  window.addEventListener('resize', syncClickMode)
  document.addEventListener('click', onDocClick)
})

onUnmounted(() => {
  window.removeEventListener('resize', syncClickMode)
  document.removeEventListener('click', onDocClick)
})
</script>

<style scoped>
.card-more {
  position: absolute;
  top: 6px;
  right: 6px;
  z-index: 20;
  padding-bottom: 8px;
  margin-bottom: -8px;
}

.card-more-dots {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  width: 28px;
  height: 28px;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease, background 0.2s, transform 0.2s;
}

.card-more:hover .card-more-dots,
.card-more:focus-within .card-more-dots,
.card-more.is-open .card-more-dots {
  opacity: 1;
  pointer-events: auto;
}

.card-more-dots:hover,
.card-more-dots:focus-visible {
  background: #fff;
  transform: scale(1.06);
}

.dot {
  display: block;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #4b5563;
}

.card-more-menu {
  position: absolute;
  top: calc(100% + 2px);
  right: 0;
  min-width: 136px;
  padding: 6px 0;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.14), 0 0 1px rgba(15, 23, 42, 0.06);
  z-index: 25;
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
  transform: translateY(-2px);
  transition: opacity 0.15s ease, visibility 0.15s, transform 0.15s ease;
}

/* 菜单在按钮下方：仅在 ⋮ 下方一条窄带上接鼠，避免挡住卡片其它区域点击 */
.card-more::after {
  content: '';
  position: absolute;
  top: 100%;
  right: 0;
  width: 36px;
  height: 16px;
  pointer-events: auto;
}

.card-more:hover .card-more-menu,
.card-more:focus-within .card-more-menu,
.card-more.is-open .card-more-menu {
  opacity: 1;
  visibility: visible;
  pointer-events: auto;
  transform: translateY(0);
}

.card-more-item {
  display: block;
  width: 100%;
  padding: 10px 16px;
  border: none;
  background: none;
  text-align: left;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.card-more-item:hover {
  background: #f3f4f6;
  color: #111827;
}

.card-more--footer {
  position: relative;
  top: auto;
  right: auto;
  padding-bottom: 0;
  margin-bottom: 0;
  display: inline-flex;
  justify-content: flex-end;
  vertical-align: middle;
}
</style>
