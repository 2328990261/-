<template>
  <el-dialog
    v-model="visible"
    :show-close="true"
    width="500px"
    align-center
    destroy-on-close
    class="dislike-book-dialog"
    @closed="onClosed"
  >
    <template #header>
      <div class="dlg-header">
        <div class="dlg-header-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <path d="M4.93 4.93l14.14 14.14" />
          </svg>
        </div>
        <div class="dlg-header-text">
          <span class="dlg-title">不感兴趣</span>
          <span class="dlg-sub">优化「为你推荐」，可多选</span>
        </div>
      </div>
    </template>

    <div class="dlg-body">
      <p class="dlg-intro">
        标签降权会减弱该标签在推荐打分中的贡献；屏蔽作者后其作品将不再出现在推荐列表。
      </p>

      <div class="dlg-section">
        <div class="dlg-section-title">快速选项</div>
        <div class="dlg-checks">
          <label class="dlg-check-row">
            <input v-model="blockNovel" type="checkbox" class="dlg-native-check" />
            <span class="dlg-check-label">不再推荐这本书</span>
          </label>
          <label class="dlg-check-row" :class="{ disabled: !author }">
            <input v-model="blockAuthor" type="checkbox" class="dlg-native-check" :disabled="!author" />
            <span class="dlg-check-label">不再推荐作者：{{ author || '未知作者' }}</span>
          </label>
          <label class="dlg-check-row">
            <input v-model="allBookTags" type="checkbox" class="dlg-native-check" />
            <span class="dlg-check-label">将本书全部标签加入降权</span>
          </label>
        </div>
      </div>

      <div v-if="tagList.length" class="dlg-section">
        <div class="dlg-section-title">单独降权标签</div>
        <p class="dlg-hint">点击选择（可多选）</p>
        <div class="dlg-tag-scroll">
          <button
            v-for="tag in tagList"
            :key="tag"
            type="button"
            class="dlg-tag-chip"
            :class="{ on: selectedTags.includes(tag) }"
            @click="toggleTag(tag)"
          >
            {{ tag }}
          </button>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dlg-footer">
        <button type="button" class="dlg-btn dlg-btn-ghost" @click="visible = false">取消</button>
        <button type="button" class="dlg-btn dlg-btn-primary" :disabled="submitting" @click="submit">
          {{ submitting ? '保存中…' : '确定' }}
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { saveUserDislike } from '@/api/dislike'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  novelId: { type: [Number, String], default: null },
  author: { type: String, default: '' },
  label: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const blockNovel = ref(false)
const blockAuthor = ref(false)
const allBookTags = ref(false)
const selectedTags = ref([])
const submitting = ref(false)

const tagList = computed(() => {
  if (!props.label) return []
  return props.label
    .split(/[,，]/)
    .map((t) => t.trim())
    .filter(Boolean)
})

function getUserId() {
  try {
    const userInfoStr = localStorage.getItem('userInfo')
    if (userInfoStr) {
      const u = JSON.parse(userInfoStr)
      if (u?.id) return u.id
    }
    const uid = localStorage.getItem('userId')
    return uid ? JSON.parse(uid) : null
  } catch {
    return null
  }
}

function resetForm() {
  blockNovel.value = false
  blockAuthor.value = false
  allBookTags.value = false
  selectedTags.value = []
}

function toggleTag(tag) {
  const i = selectedTags.value.indexOf(tag)
  if (i >= 0) selectedTags.value.splice(i, 1)
  else selectedTags.value.push(tag)
}

function onClosed() {
  resetForm()
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) resetForm()
  }
)

async function submit() {
  const uid = getUserId()
  if (!uid) {
    alert('请先登录')
    return
  }
  const nid = Number(props.novelId)
  if (!nid) {
    alert('无效的书籍')
    return
  }
  const hasAny =
    blockNovel.value || blockAuthor.value || allBookTags.value || selectedTags.value.length > 0
  if (!hasAny) {
    alert('请至少选择一种反馈方式')
    return
  }
  submitting.value = true
  try {
    const res = await saveUserDislike(uid, {
      novelId: nid,
      blockNovel: blockNovel.value,
      blockAuthor: blockAuthor.value,
      allBookTags: allBookTags.value,
      tagNames: [...selectedTags.value]
    })
    if (res.code === 200) {
      visible.value = false
      emit('saved')
      alert('已保存，将用于后续推荐')
    } else {
      alert(res.msg || '保存失败')
    }
  } catch (e) {
    alert(e?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.dlg-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-right: 8px;
}

.dlg-header-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #fff0eb 0%, #ffe4d6 100%);
  color: #e85d4c;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.dlg-header-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.dlg-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
  letter-spacing: 0.02em;
}

.dlg-sub {
  font-size: 12px;
  color: #909399;
}

.dlg-body {
  padding: 4px 2px 8px;
}

.dlg-intro {
  margin: 0 0 20px;
  font-size: 13px;
  line-height: 1.65;
  color: #606266;
  padding: 12px 14px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 12px;
  border: 1px solid #e8ecf1;
}

.dlg-section {
  margin-bottom: 20px;
}

.dlg-section:last-child {
  margin-bottom: 0;
}

.dlg-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 10px;
  padding-left: 2px;
}

.dlg-hint {
  font-size: 12px;
  color: #9ca3af;
  margin: -4px 0 10px;
}

.dlg-checks {
  display: flex;
  flex-direction: column;
  gap: 0;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
}

.dlg-check-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f0f2f5;
  transition: background 0.15s;
}

.dlg-check-row:last-child {
  border-bottom: none;
}

.dlg-check-row:hover:not(.disabled) {
  background: #fafbfc;
}

.dlg-check-row.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.dlg-native-check {
  margin-top: 2px;
  width: 16px;
  height: 16px;
  accent-color: #667eea;
  flex-shrink: 0;
}

.dlg-check-label {
  font-size: 14px;
  color: #303133;
  line-height: 1.45;
}

.dlg-tag-scroll {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 200px;
  overflow-y: auto;
  padding: 4px 2px 8px;
}

.dlg-tag-chip {
  padding: 7px 14px;
  border: 1px solid #e4e7ed;
  border-radius: 20px;
  background: #fff;
  color: #606266;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dlg-tag-chip:hover {
  border-color: #a5b4fc;
  color: #4f46e5;
  background: #eef2ff;
}

.dlg-tag-chip.on {
  border-color: #667eea;
  background: linear-gradient(135deg, #eef2ff 0%, #e0e7ff 100%);
  color: #4338ca;
  font-weight: 600;
}

.dlg-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.dlg-btn {
  padding: 10px 22px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.dlg-btn-ghost {
  background: #f3f4f6;
  color: #4b5563;
}

.dlg-btn-ghost:hover {
  background: #e5e7eb;
}

.dlg-btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  box-shadow: 0 4px 14px rgba(102, 126, 234, 0.35);
}

.dlg-btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(102, 126, 234, 0.45);
}

.dlg-btn-primary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}
</style>

<style>
.dislike-book-dialog.el-dialog {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 24px 64px rgba(15, 23, 42, 0.18), 0 0 1px rgba(15, 23, 42, 0.08);
}

.dislike-book-dialog .el-dialog__header {
  margin: 0;
  padding: 20px 22px 12px;
  border-bottom: 1px solid #f0f2f5;
}

.dislike-book-dialog .el-dialog__body {
  padding: 8px 22px 16px;
}

.dislike-book-dialog .el-dialog__footer {
  padding: 12px 22px 20px;
  border-top: 1px solid #f0f2f5;
}
</style>
