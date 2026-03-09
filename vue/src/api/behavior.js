import request from '@/utils/request'

const BASE = '/user/behavior'

/** 获取用户完读记录列表 */
export function getFinishedNovels(userId) {
  return request({
    url: `${BASE}/finished/novels`,
    method: 'get',
    params: { userId }
  })
}

/** 标记小说为完读 */
export function markNovelAsFinished(userId, novelId) {
  return request({
    url: `${BASE}/finished/novels`,
    method: 'post',
    params: { userId, novelId }
  })
}

/** 取消完读 */
export function cancelFinishedNovel(userId, novelId) {
  return request({
    url: `${BASE}/finished/novels`,
    method: 'delete',
    params: { userId, novelId }
  })
}

/** 保存/更新阅读历史（含阅读时长） */
export function saveReadingHistory(data) {
  return request({
    url: `${BASE}/reading/history`,
    method: 'post',
    data
  })
}

/** 获取用户所有小说阅读总时长（秒） */
export function getTotalReadingDuration(userId) {
  return request({
    url: `${BASE}/reading/total-duration`,
    method: 'get',
    params: { userId }
  })
}

/** 获取某本小说的评论列表 */
export function getNovelComments(novelId) {
  return request({
    url: `${BASE}/comments/novel`,
    method: 'get',
    params: { novelId }
  })
}

/** 发表评论 */
export function addComment(data) {
  return request({
    url: `${BASE}/comments`,
    method: 'post',
    data
  })
}
