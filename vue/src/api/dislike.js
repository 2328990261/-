import request from '@/utils/request'

const BASE = '/user/behavior/dislike'

export function saveUserDislike(userId, data) {
  return request({
    url: BASE,
    method: 'post',
    params: { userId },
    data
  })
}

export function getUserDislike(userId) {
  return request({
    url: BASE,
    method: 'get',
    params: { userId }
  })
}
