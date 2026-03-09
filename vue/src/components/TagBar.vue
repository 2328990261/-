<template>
  <div class="tag-bar">
    <!-- 标签按钮区域（日常/恋爱/校园等） -->
    <div class="tag-nav">
      <button
        v-for="tag in tags"
        :key="tag.id"
        :class="{ active: currentTagId === tag.id }"
        @click="switchTag(tag.id)"
      >
        {{ tag.name }}
      </button>
    </div>
  </div>
</template>

<script setup>
// 接收父组件传过来的标签数据、当前选中标签、切换方法
defineProps({
  tags: {
    type: Array,
    required: true
  },
  currentTagId: {
    type: String,
    required: true
  }
})
// 向父组件传递切换标签的事件
const emit = defineEmits(['switchTag'])
const switchTag = (tagId) => {
  emit('switchTag', tagId)
}
</script>

<style scoped>
/* 你原来的标签栏容器样式（保留） */
.tag-bar {
  display: flex;
  justify-content: space-between;
  padding: 16px;
  background-color: #ffffff;
  border-radius: 8px;
  margin-bottom: 24px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  flex-wrap: wrap;
  gap: 16px;
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;
}

/* 标签按钮样式（和你原来的tag-nav一致） */
.tag-nav {
  display: flex;
  justify-content: center;
  gap: 12px;
  width: 100%; /* 按钮区域占满容器宽度 */
}

.tag-nav button {
  padding: 12px 24px;
  border: 1px solid #e5e7eb;
  border-radius: 24px;
  background-color: #fff;
  cursor: pointer;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.2s ease;
  min-width: 100px;
  text-align: center;
}

.tag-nav button.active {
  background-color: #f8a555;
  color: #fff;
  border-color: #f8a555;
  box-shadow: 0 2px 8px rgba(248, 165, 85, 0.3);
}

.tag-nav button:hover:not(.active) {
  border-color: #f8a555;
  color: #f8a555;
}

</style>
