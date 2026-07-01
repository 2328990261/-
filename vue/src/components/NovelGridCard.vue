<template>
  <div
    class="novel-grid-card__shell"
    :class="{ 'is-hover': isHover }"
    @mouseenter="isHover = true"
    @mouseleave="isHover = false"
    @click="emit('select', book)"
  >
    <article class="novel-grid-card__inner">
      <div class="novel-grid-card__media">
        <img
          class="novel-grid-card__cover"
          :src="coverSrc"
          :alt="displayTitle"
          loading="lazy"
          @error="onImgError"
        />
      </div>
      <div class="novel-grid-card__info">
        <h3 class="novel-grid-card__title" :title="displayTitle">{{ displayTitle }}</h3>
        <div class="novel-grid-card__footer">
          <p class="novel-grid-card__author">作者：{{ displayAuthor }}</p>
          <BookCardMoreMenu
            v-if="showMoreMenu"
            :book="book"
            class="novel-grid-card__more"
            @open-dislike="(b) => emit('open-dislike', b)"
          />
        </div>
      </div>
    </article>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { backendUrl } from '@/config/env'
import BookCardMoreMenu from '@/components/BookCardMoreMenu.vue'

const props = defineProps({
  book: {
    type: Object,
    required: true
  },
  /** 首页登录后显示“更多”（不感兴趣等） */
  showMoreMenu: { type: Boolean, default: false }
})

const emit = defineEmits(['select', 'open-dislike'])

const isHover = ref(false)

const displayTitle = computed(() => props.book?.bookMainName || '未知书名')
const displayAuthor = computed(() => props.book?.author || '未知作者')

const coverSrc = computed(() => {
  const c = props.book?.cover
  if (!c) return ''
  return `${backendUrl('/novel/cover')}/${encodeURIComponent(c)}`
})

function onImgError(e) {
  e.target.src = 'data:image/svg+xml,' + encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="200" height="267"><rect fill="#eee" width="100%" height="100%"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="#999" font-size="14">无封面</text></svg>'
  )
}
</script>

<style scoped>
/* 外壳：圆角、浅粉描边、整体投影、顶条装饰 */
.novel-grid-card__shell {
  --novel-accent: #e91e8c;

  width: 100%;
  max-width: 172px;
  margin-inline: auto;
  height: 100%;
  background: #fff;
  border-radius: 15px;
  border: 1px solid #fce4ec;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  position: relative;
  box-sizing: border-box;
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}

.novel-grid-card__shell::before {
  content: '';
  position: absolute;
  left: 8px;
  right: 8px;
  top: 0;
  height: 3px;
  background: var(--novel-accent);
  border-radius: 15px 15px 0 0;
  opacity: 0.35;
  transition: opacity 0.2s ease;
  z-index: 2;
  pointer-events: none;
}

.novel-grid-card__shell.is-hover {
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.08);
  border-color: #f8bbd0;
}

.novel-grid-card__shell.is-hover::before {
  opacity: 1;
}

/* 内胆：与外壳留出边距，封面不贴边 */
.novel-grid-card__inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  padding: 13px 11px 0;
  margin: 0;
  box-sizing: border-box;
}

.novel-grid-card__media {
  width: 100%;
  aspect-ratio: 3 / 4;
  overflow: hidden;
  background: #f3f4f6;
  flex-shrink: 0;
  border-radius: 9px;
}

.novel-grid-card__cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.35s ease;
}

.novel-grid-card__shell.is-hover .novel-grid-card__cover {
  transform: scale(1.03);
}

.novel-grid-card__info {
  padding: 9px 0 11px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  flex: 1;
  min-height: 0;
}

.novel-grid-card__title {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: #ad1457;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.2s ease;
}

.novel-grid-card__shell.is-hover .novel-grid-card__title {
  color: var(--novel-accent);
}

.novel-grid-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
  margin-top: auto;
}

.novel-grid-card__author {
  margin: 0;
  font-size: 11px;
  color: #757575;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.novel-grid-card__more {
  flex-shrink: 0;
}

@media (max-width: 520px) {
  .novel-grid-card__shell {
    max-width: none;
  }
}

@media (max-width: 768px) {
  .novel-grid-card__inner {
    padding: 12px 10px 0;
  }

  .novel-grid-card__info {
    padding: 8px 0 10px;
  }

  .novel-grid-card__title {
    font-size: 12px;
  }
}
</style>