import request from '@/utils/request'

const BASE = '/user/recommend'

export function getRecommendProfile(userId) {
  return request({
    url: `${BASE}/profile`,
    method: 'get',
    params: { userId }
  })
}

export function saveRecommendProfile(userId, data) {
  return request({
    url: `${BASE}/profile`,
    method: 'put',
    params: { userId },
    data
  })
}

export function getUserTagWeights(userId) {
  return request({
    url: `${BASE}/tag-weights`,
    method: 'get',
    params: { userId }
  })
}

export function saveUserTagWeights(userId, rows) {
  return request({
    url: `${BASE}/tag-weights`,
    method: 'put',
    params: { userId },
    data: rows
  })
}

