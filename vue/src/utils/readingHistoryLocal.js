/**
 * 本地阅读历史（按当前用户隔离的 localStorage key）
 * 约定：数组中越靠前表示越最近打开/阅读，与详情页「开始阅读」「继续阅读」及阅读页保存逻辑一致。
 */

import { getReadingHistoryStorageKey } from '@/utils/userBrowserCache'

/** 个人中心「阅读历史」只保留最近 N 本（越靠前越新） */
export const READING_HISTORY_MAX = 10

function storageKey() {
  return getReadingHistoryStorageKey()
}

function trimToMax(list) {
  if (!Array.isArray(list) || list.length <= READING_HISTORY_MAX) return list
  return list.slice(0, READING_HISTORY_MAX)
}

export function getReadingHistoryList() {
  try {
    const key = storageKey()
    const raw = localStorage.getItem(key)
    const list = raw ? JSON.parse(raw) : []
    if (!Array.isArray(list)) return []
    const trimmed = trimToMax(list)
    if (trimmed.length !== list.length) {
      localStorage.setItem(key, JSON.stringify(trimmed))
    }
    return trimmed
  } catch {
    return []
  }
}

/**
 * 写入或更新一条阅读进度，并移到列表最前（最近优先）。
 * @param {object} entry — 至少含 novelId；可含 chapterId、page、progressPct、bookMainName、cover 等
 */
export function upsertReadingHistoryProgress(entry) {
  if (entry == null || entry.novelId == null) return
  const list = getReadingHistoryList()
  const id = String(entry.novelId)
  const idx = list.findIndex((x) => String(x.novelId) === id)
  if (idx >= 0) {
    list.splice(idx, 1)
  }
  const merged = {
    ...entry,
    novelId: entry.novelId,
    timestamp: Date.now()
  }
  list.unshift(merged)
  localStorage.setItem(storageKey(), JSON.stringify(trimToMax(list)))
}

/**
 * 仅把某本书标为「最近打开」（详情页进入阅读前调用）；无记录时插入占位，有则更新时间并置顶。
 */
export function touchReadingHistoryRecency(novelId, patch = {}) {
  const list = getReadingHistoryList()
  const id = String(novelId)
  const idx = list.findIndex((x) => String(x.novelId) === id)
  const now = Date.now()
  if (idx >= 0) {
    const prev = list[idx]
    list.splice(idx, 1)
    list.unshift({
      ...prev,
      ...patch,
      novelId: prev.novelId,
      timestamp: now
    })
  } else {
    list.unshift({
      novelId,
      chapterId: patch.chapterId ?? null,
      page: patch.page ?? 1,
      progressPct: patch.progressPct ?? 0,
      bookMainName: patch.bookMainName,
      cover: patch.cover,
      timestamp: now
    })
  }
  localStorage.setItem(storageKey(), JSON.stringify(trimToMax(list)))
}
