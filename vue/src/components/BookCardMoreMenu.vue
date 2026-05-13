<template>
  <div ref="rootRef" class="book-card-more" @click.stop>
    <button
      type="button"
      class="more-trigger"
      :class="{ open: menuOpen }"
      aria-haspopup="menu"
      :aria-expanded="menuOpen"
      aria-label="更多操作"
      @click="toggleMenu"
    >
      <span class="v-dots" aria-hidden="true">
        <span class="v-dot" />
        <span class="v-dot" />
        <span class="v-dot" />
      </span>
    </button>

    <div v-if="menuOpen" class="more-dropdown" role="menu">
      <button type="button" class="more-item" role="menuitem" @click="onFavorite">
        {{ isFavorited ? '取消收藏' : '收藏' }}
      </button>
      <button type="button" class="more-item more-item-muted" role="menuitem" @click="onDislike">
        不感兴趣
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { addCollection, removeCollection, checkCollection } from '@/api/novel'

const props = defineProps({
  book: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['open-dislike', 'favorite-changed'])

const rootRef = ref(null)
const menuOpen = ref(false)
const isFavorited = ref(false)

function getUserId() {
  try {
    const userInfoStr = localStorage.getItem('userInfo')
    if (userInfoStr && userInfoStr !== 'undefined') {
      const u = JSON.parse(userInfoStr)
      if (u?.id != null) return Number(u.id)
    }
    const uid = localStorage.getItem('userId')
    if (uid != null && uid !== '') return Number(uid)
  } catch {
    /* ignore */
  }
  return null
}

function closeMenu() {
  menuOpen.value = false
}

function toggleMenu() {
  menuOpen.value = !menuOpen.value
}

async function refreshFavorite() {
  const uid = getUserId()
  const nid = Number(props.book?.id)
  if (!uid || !nid) {
    isFavorited.value = false
    return
  }
  try {
    const res = await checkCollection(uid, nid)
    if (res.code === 200) {
      isFavorited.value = !!res.data
    }
  } catch {
    isFavorited.value = false
  }
}

watch(menuOpen, (open) => {
  if (open) {
    refreshFavorite()
  }
})

function onDocClick(e) {
  if (!menuOpen.value) return
  const el = rootRef.value
  if (el && !el.contains(e.target)) {
    closeMenu()
  }
}

onMounted(() => {
  document.addEventListener('click', onDocClick, true)
})

onUnmounted(() => {
  document.removeEventListener('click', onDocClick, true)
})

async function onFavorite() {
  const uid = getUserId()
  const nid = Number(props.book?.id)
  if (!uid || !nid) {
    alert('请先登录')
    closeMenu()
    return
  }
  try {
    if (isFavorited.value) {
      const res = await removeCollection(uid, nid)
      if (res.code === 200) {
        isFavorited.value = false
        emit('favorite-changed', { novelId: nid, favorited: false })
      } else {
        alert(res.msg || '取消收藏失败')
      }
    } else {
      const res = await addCollection(uid, nid)
      if (res.code === 200) {
        isFavorited.value = true
        emit('favorite-changed', { novelId: nid, favorited: true })
      } else {
        alert(res.msg || '收藏失败')
      }
    }
  } catch (e) {
    alert(e?.msg || e?.message || '操作失败')
  }
  closeMenu()
}

function onDislike() {
  closeMenu()
  emit('open-dislike', props.book)
}
</script>

<style scoped>
.book-card-more {
  position: relative;
  flex-shrink: 0;
  align-self: center;
}

.more-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #909399;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.more-trigger:hover,
.more-trigger.open {
  background: rgba(0, 0, 0, 0.06);
  color: #606266;
}

.v-dots {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
}

.v-dot {
  display: block;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: currentColor;
}

.more-dropdown {
  position: absolute;
  right: 0;
  top: calc(100% + 4px);
  min-width: 112px;
  padding: 6px 0;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  z-index: 20;
}

.more-item {
  display: block;
  width: 100%;
  padding: 10px 16px;
  border: none;
  background: none;
  font-size: 14px;
  color: #303133;
  text-align: left;
  cursor: pointer;
  transition: background 0.12s;
}

.more-item:hover {
  background: #f5f7fa;
}

.more-item-muted {
  color: #606266;
}
</style>
