import request from './request'

export const chatApi = {
  // 获取历史消息
  getMessages(page = 1, pageSize = 50, before = null, room = {}) {
    const params = { page, pageSize }
    if (before) {
      params.before = before
    }
    if (room.roomType) {
      params.roomType = room.roomType
    }
    if (room.roomId) {
      params.roomId = room.roomId
    }
    return request({
      url: '/chat/messages',
      method: 'get',
      params
    })
  },
  
  // 获取在线用户列表
  getOnlineUsers() {
    return request({
      url: '/chat/online-users',
      method: 'get'
    })
  }
}
