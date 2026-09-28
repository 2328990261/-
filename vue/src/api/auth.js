import request from '@/utils/request'

export function login(username, password) {
  return request({
    url: '/auth/login',
    method: 'post',
    data: { username, password }
  })
}

/** 发送手机验证码 */
export function sendCode(phone) {
  return request({
    url: '/auth/sendCode',
    method: 'post',
    data: { phone }
  })
}

/** 手机号+验证码登录 */
export function loginByPhone(phone, code) {
  return request({
    url: '/auth/loginByPhone',
    method: 'post',
    data: { phone, code }
  })
}

/** 发送邮箱验证码 */
export function sendEmailCode(email) {
  return request({
    url: '/auth/sendEmailCode',
    method: 'post',
    data: { email }
  })
}

/** 邮箱+验证码登录 */
export function loginByEmail(email, code) {
  return request({
    url: '/auth/loginByEmail',
    method: 'post',
    data: { email, code }
  })
}

export function register(username, password, email, code) {
  return request({
    url: '/auth/register',
    method: 'post',
    data: { username, password, email, code }
  })
}

export function getUserInfo(username) {
  return request({
    url: '/auth/userinfo',
    method: 'get',
    params: { username }
  })
}

export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

export function updateUserInfo(userId, username, email, phone) {
  return request({
    url: '/auth/updateUserInfo',
    method: 'post',
    data: { userId, username, email, phone }
  })
}
