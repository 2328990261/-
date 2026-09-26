import request from '@/utils/request'

// 1. 获取所有小说
export function getAllNovels() {
  return request({
    url: '/novel/list',
    method: 'get'
  })
}

// 2. 按标签查询小说（统一参数化接口）
export function getNovelsByLabel(label) {
  return request({
    url: '/novel/listByLabel',
    method: 'get',
    params: { label }
  })
}

// 根据标签获取小说（通用方法，兼容旧调用方式）
export function getNovelsByTag(tag) {
  return getNovelsByLabel(tag)
}

/** 获取标签列表（供前台标签栏渲染，与后台 tag 表同步） */
export function getTags() {
  return request({
    url: '/novel/tags',
    method: 'get'
  })
}

// 新增：按多个标签查询小说
export function getNovelsByLabels(labels) {
  return request({
    url: '/novel/listByLabels',
    method: 'post',
    data: labels
  })
}

/** 搜索小说：支持 ID 精确、书名/作者 模糊查询 */
export function searchNovels(keyword) {
  return request({
    url: '/novel/search',
    method: 'get',
    params: { keyword: keyword != null ? String(keyword).trim() : '' }
  })
}

// 3. 获取小说分卷内容
export const getNovelVolume = async (mainBookId) => {
  if (!mainBookId) {
    return { code: 400, data: null, msg: '小说主ID不能为空' }
  }
  try {
    const res = await request.get('/novel/volume', { params: { mainBookId } })
    return {
      code: res.data?.code || res.code,
      data: res.data?.data || res.data,
      msg: res.data?.msg || '请求成功'
    }
  } catch (err) {
    console.error('获取小说分卷失败：', err.response?.data || err.message)
    return { code: 500, data: null, msg: '获取小说分卷失败' }
  }
}

// 4. 获取轮播图数据（Top5热门小说）
export function getBannerList() {
  return request({
    url: '/novel/carousel',  // 这里和后端接口路径保持一致
    method: 'get'
  })
}

// 获取小说详情（仅支持你数据库中的ID）
export const getNovelDetailById = async (novelId) => {
  const id = Number(novelId)

  if (isNaN(id)) {
    return { code: 400, data: null, msg: '小说ID格式错误' }
  }
  try {
    const res = await request.get(`/novel/detail/${id}`)
    return {
      code: res.data?.code || res.code,
      data: res.data?.data || res.data,
      msg: res.data?.msg || '请求成功'
    }
  } catch (error) {
    console.error('请求小说详情失败：', error.response?.data || error.message)
    return { code: 500, data: null,  msg: error.response?.data?.msg || '接口请求失败'  }
  }
}

// 获取所有小说列表（你的后端能正常返回）
export const getChapterContentById = async (chapterId) => {
  if (!chapterId) {
    return { code: 400, data: null, msg: '章节ID不能为空' }
  }

  try {
    const res = await request.get(`/novel/chapter/${chapterId}`)
    return {
      code: res.data?.code || res.code,
      data: res.data?.data || res.data,
      msg: res.data?.msg || '请求成功'
    }
  } catch (error) {
    console.error('请求章节内容失败：', error.response?.data || error.message)
    return { code: 500, data: null, msg: '章节请求失败' }
  }
}

// 收藏相关 API 方法
// 添加收藏
export function addCollection(userId, novelId) {
  return request({
    url: '/auth/collection/add',
    method: 'post',
    data: { userId, novelId }
  })
}

// 取消收藏
export function removeCollection(userId, novelId) {
  return request({
    url: '/auth/collection/remove',
    method: 'post',
    data: { userId, novelId }
  })
}

// 检查收藏状态
export function checkCollection(userId, novelId) {
  return request({
    url: '/auth/collection/check',
    method: 'get',
    params: { userId, novelId }
  })
}

// 获取用户收藏列表
export function getCollectionList(userId) {
  return request({
    url: '/auth/collection/list',
    method: 'get',
    params: { userId }
  })
}
