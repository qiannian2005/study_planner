import request from './request'

export const aiApi = {
  // 发送聊天消息
  chat(messages) {
    // 转换消息格式为后端期望的格式
    const formattedMessages = messages.map(msg => ({
      role: msg.role,
      content: msg.content
    }))
    
    return request({
      url: '/ai/chat',
      method: 'POST',
      data: { 
        messages: formattedMessages
      }
    })
  }
}

