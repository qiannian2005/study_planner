import request from './request'

export const userApi = {
  // 用户登录
  login(username, password) {
    return request({
      url: '/user/login',
      method: 'POST',
      data: { username, password }
    })
  },

  // 用户注册
  register(username, password, email) {
    return request({
      url: '/user/register',
      method: 'POST',
      data: { username, password, email }
    })
  },

  // 用户登出
  logout() {
    return request({
      url: '/user/logout',
      method: 'POST'
    })
  },

  // 获取用户信息
  getUserInfo() {
    return request({
      url: '/user/info',
      method: 'GET'
    })
  },

  // 更新用户资料
  updateProfile(data) {
    return request({
      url: '/user/profile',
      method: 'PUT',
      data
    })
  },

  // 修改密码
  changePassword(oldPassword, newPassword) {
    return request({
      url: '/user/password',
      method: 'PUT',
      data: { oldPassword, newPassword }
    })
  },

  // 上传头像
  uploadAvatar(formData) {
    return request({
      url: '/user/avatar',
      method: 'POST',
      data: formData
      // 不设置 Content-Type，让 axios 自动处理 FormData（包括 boundary）
    })
  }
}

