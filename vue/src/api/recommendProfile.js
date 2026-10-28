import request from '@/utils/request'

// 用户推荐画像相关接口的基础路径
const BASE = '/user/recommend'

/** 获取当前用户的推荐配置画像（含行为权重、MMR 开关等） */
export function getRecommendProfile(userId) {
  return request({
    url: `${BASE}/profile`,
    method: 'get',
    params: { userId }
  })
}

/** 保存当前用户的推荐配置画像 */
export function saveRecommendProfile(userId, data) {
  return request({
    url: `${BASE}/profile`,
    method: 'put',
    params: { userId },
    data
  })
}

/** 获取当前用户的标签权重 */
export function getUserTagWeights(userId) {
  return request({
    url: `${BASE}/tag-weights`,
    method: 'get',
    params: { userId }
  })
}

/** 保存当前用户手动设置的标签权重 */
export function saveUserTagWeights(userId, rows) {
  return request({
    url: `${BASE}/tag-weights`,
    method: 'put',
    params: { userId },
    data: rows
  })
}
