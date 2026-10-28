import request from '@/utils/request'

// 不感兴趣相关接口的基础路径
const BASE = '/user/behavior/dislike'

/** 保存用户「不感兴趣」设置（屏蔽小说/作者、标签降权） */
export function saveUserDislike(userId, data) {
  return request({
    url: BASE,
    method: 'post',
    params: { userId },
    data
  })
}

/** 获取当前用户的屏蔽/降权列表 */
export function getUserDislike(userId) {
  return request({
    url: BASE,
    method: 'get',
    params: { userId }
  })
}
