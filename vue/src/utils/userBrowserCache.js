/**
 * 与账号相关的浏览器缓存：阅读历史、本地完读列表、书架排序等必须按用户隔离，
 * 否则换号登录/注册后会把上一用户的数据写进新账号（或显示错乱）。
 */

/** 旧版未加用户后缀的 key（登录/注册成功后应清除，避免串号） */
const LEGACY_KEYS = ['readingHistory', 'finishedNovels', 'bookshelfOrder']

/**
 * 当前登录用户 ID；未登录返回 null（游客用 _guest 后缀）。
 */
export function getScopedUserId() {
  try {
    const userInfoStr = localStorage.getItem('userInfo')
    if (userInfoStr && userInfoStr !== 'undefined') {
      const u = JSON.parse(userInfoStr)
      if (u != null && u.id != null && u.id !== '') {
        const n = Number(u.id)
        return Number.isFinite(n) ? n : null
      }
    }
    const uid = localStorage.getItem('userId')
    if (uid != null && uid !== '') {
      const n = Number(uid)
      return Number.isFinite(n) ? n : null
    }
  } catch {
    /* ignore */
  }
  return null
}

export function getReadingHistoryStorageKey() {
  const id = getScopedUserId()
  return id == null ? 'readingHistory_guest' : `readingHistory_u${id}`
}

export function getFinishedNovelsStorageKey() {
  const id = getScopedUserId()
  return id == null ? 'finishedNovels_guest' : `finishedNovels_u${id}`
}

export function getBookshelfOrderStorageKey() {
  const id = getScopedUserId()
  return id == null ? 'bookshelfOrder_guest' : `bookshelfOrder_u${id}`
}

/** 登录/注册成功后调用：丢弃旧的全局未隔离数据，防止写入新用户。 */
export function clearLegacyGlobalReadingCaches() {
  for (const k of LEGACY_KEYS) {
    try {
      localStorage.removeItem(k)
    } catch {
      /* ignore */
    }
  }
}

/** 退出登录前清除指定用户在浏览器中的隔离缓存。 */
export function clearScopedUserReadingCaches(userId) {
  if (userId == null || !Number.isFinite(Number(userId))) return
  const id = Number(userId)
  const keys = [`readingHistory_u${id}`, `finishedNovels_u${id}`, `bookshelfOrder_u${id}`]
  for (const k of keys) {
    try {
      localStorage.removeItem(k)
    } catch {
      /* ignore */
    }
  }
}

/** 退出登录或 token 失效时调用：清除当前用户在浏览器中的阅读相关隔离缓存及旧版全局 key。 */
export function clearReadingCachesForLogout() {
  const uid = getScopedUserId()
  if (uid != null) clearScopedUserReadingCaches(uid)
  clearLegacyGlobalReadingCaches()
}


export function getFinishedNovelIdsFromLocal() {
  try {
    const raw = localStorage.getItem(getFinishedNovelsStorageKey())
    const list = raw ? JSON.parse(raw) : []
    if (!Array.isArray(list)) return []
    return list.map((x) => String(x))
  } catch {
    return []
  }
}

export function appendFinishedNovelIdToLocal(novelId) {
  if (novelId == null) return
  const key = getFinishedNovelsStorageKey()
  const idStr = String(novelId)
  const list = getFinishedNovelIdsFromLocal()
  if (list.includes(idStr)) return
  list.push(idStr)
  localStorage.setItem(key, JSON.stringify(list))
}
