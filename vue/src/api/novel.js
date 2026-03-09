import request from '@/utils/request'

// 1. 获取所有小说
export function getAllNovels() {
  return request({
    url: '/novel/list',
    method: 'get'
  })
}

// 2. 按标签获取小说（为每个标签单独写方法）
// 第1行标签
export function getNovelsByDaily() {
  return request({ url: '/novel/listByDaily', method: 'get' })
}
export function getNovelsByFantasy() {
  return request({ url: '/novel/listByFantasy', method: 'get' })
}
export function getNovelsBySchool() {
  return request({ url: '/novel/listBySchool', method: 'get' })
}
export function getNovelsByAdventure() {
  return request({ url: '/novel/listByAdventure', method: 'get' })
}
export function getNovelsByIsekai() {
  return request({ url: '/novel/listByIsekai', method: 'get' })
}

// 第2行标签
export function getNovelsByRelax() {
  return request({ url: '/novel/listByRelax', method: 'get' })
}
export function getNovelsByFunny() {
  return request({ url: '/novel/listByFunny', method: 'get' })
}
export function getNovelsByHealing() {
  return request({ url: '/novel/listByHealing', method: 'get' })
}
export function getNovelsByDepressing() {
  return request({ url: '/novel/listByDepressing', method: 'get' })
}
export function getNovelsBySweet() {
  return request({ url: '/novel/listBySweet', method: 'get' })
}
export function getNovelsByHotblood() {
  return request({ url: '/novel/listByHotblood', method: 'get' })
}
export function getNovelsByLove() {
  return request({ url: '/novel/listByLove', method: 'get' })
}
export function getNovelsByGrowth() {
  return request({ url: '/novel/listByGrowth', method: 'get' })
}

// 第3行标签
export function getNovelsByIntellect() {
  return request({ url: '/novel/listByIntellect', method: 'get' })
}
export function getNovelsBySuspense() {
  return request({ url: '/novel/listBySuspense', method: 'get' })
}
export function getNovelsByDeduction() {
  return request({ url: '/novel/listByDeduction', method: 'get' })
}
export function getNovelsByPsychological() {
  return request({ url: '/novel/listByPsychological', method: 'get' })
}
export function getNovelsByBattle() {
  return request({ url: '/novel/listByBattle', method: 'get' })
}
export function getNovelsByCompetition() {
  return request({ url: '/novel/listByCompetition', method: 'get' })
}
export function getNovelsByConstruction() {
  return request({ url: '/novel/listByConstruction', method: 'get' })
}

// 第4行标签
export function getNovelsByPalace() {
  return request({ url: '/novel/listByPalace', method: 'get' })
}
export function getNovelsByVirtualGame() {
  return request({ url: '/novel/listByVirtualGame', method: 'get' })
}
export function getNovelsByRealistic() {
  return request({ url: '/novel/listByRealistic', method: 'get' })
}
export function getNovelsByFarming() {
  return request({ url: '/novel/listByFarming', method: 'get' })
}
export function getNovelsByReincarnation() {
  return request({ url: '/novel/listByReincarnation', method: 'get' })
}
export function getNovelsByTransmigration() {
  return request({ url: '/novel/listByTransmigration', method: 'get' })
}
export function getNovelsByMagic() {
  return request({ url: '/novel/listByMagic', method: 'get' })
}

// 第5行标签
export function getNovelsByAllAges() {
  return request({ url: '/novel/listByAllAges', method: 'get' })
}
export function getNovelsByLightYuri() {
  return request({ url: '/novel/listByLightYuri', method: 'get' })
}

// 标签到API方法的映射
const tagApiMap = {
  '日常': getNovelsByDaily,
  '奇幻': getNovelsByFantasy,
  '校园': getNovelsBySchool,
  '冒险': getNovelsByAdventure,
  '异世界': getNovelsByIsekai,
  '轻松': getNovelsByRelax,
  '搞笑': getNovelsByFunny,
  '治愈': getNovelsByHealing,
  '致郁': getNovelsByDepressing,
  '甜宠': getNovelsBySweet,
  '热血': getNovelsByHotblood,
  '恋爱': getNovelsByLove,
  '成长': getNovelsByGrowth,
  '智斗': getNovelsByIntellect,
  '悬疑': getNovelsBySuspense,
  '推理': getNovelsByDeduction,
  '心理惊悚': getNovelsByPsychological,
  '战斗': getNovelsByBattle,
  '竞技': getNovelsByCompetition,
  '基建': getNovelsByConstruction,
  '宫廷': getNovelsByPalace,
  '虚拟网游': getNovelsByVirtualGame,
  '现实题材': getNovelsByRealistic,
  '种田文': getNovelsByFarming,
  '转生': getNovelsByReincarnation,
  '穿越': getNovelsByTransmigration,
  '魔法': getNovelsByMagic,
  '全年龄': getNovelsByAllAges,
  '轻百': getNovelsByLightYuri
}

// 根据标签获取小说（通用方法）
export function getNovelsByTag(tag) {
  const apiFunc = tagApiMap[tag]
  if (apiFunc) {
    return apiFunc()
  }
  return Promise.resolve({ code: 404, data: [], msg: '未知标签' })
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


