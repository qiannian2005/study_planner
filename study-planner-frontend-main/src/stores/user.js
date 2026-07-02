import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { userApi } from '../api/user'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)

  const isLoggedIn = computed(() => user.value !== null)
  
  // 监听用户变化，通知聊天室重新连接
  watch(user, (newUser, oldUser) => {
    // 如果用户从登录状态变为未登录，或者用户ID发生变化，需要重新连接WebSocket
    if (oldUser && newUser && oldUser.id !== newUser.id) {
      console.log('用户切换，需要重新连接WebSocket: ', oldUser.id, ' -> ', newUser.id)
      // 触发自定义事件，让聊天室知道需要重新连接
      window.dispatchEvent(new CustomEvent('user-changed', { detail: { oldUser, newUser } }))
    }
  })

  // 检查登录状态
  async function checkLoginStatus() {
    try {
      const response = await userApi.getUserInfo()
      if (response.code === 200 && response.data) {
        user.value = response.data
        // 同步到 localStorage（仅用于显示）
        localStorage.setItem('user', JSON.stringify(response.data))
        return response.data
      } else {
        user.value = null
        localStorage.removeItem('user')
        localStorage.removeItem('token')
        return null
      }
    } catch (error) {
      console.error('检查登录状态失败:', error)
      user.value = null
      return null
    }
  }

  // 登录
  async function login(username, password) {
    try {
      const response = await userApi.login(username, password)
      if (response.code === 200) {
        user.value = response.data
        localStorage.setItem('user', JSON.stringify(response.data))
        return { success: true, user: response.data }
      } else {
        return { success: false, message: response.message || '登录失败' }
      }
    } catch (error) {
      console.error('登录失败:', error)
      return { success: false, message: '网络错误，请稍后重试' }
    }
  }

  // 注册
  async function register(username, password, email) {
    try {
      const response = await userApi.register(username, password, email)
      if (response.code === 200) {
        return { success: true, message: '注册成功' }
      } else {
        return { success: false, message: response.message || '注册失败' }
      }
    } catch (error) {
      console.error('注册失败:', error)
      return { success: false, message: '网络错误，请稍后重试' }
    }
  }

  // 登出
  async function logout() {
    try {
      await userApi.logout()
    } catch (error) {
      console.error('登出请求失败:', error)
    }
    
    const oldUser = user.value
    // 清除本地状态
    user.value = null
    localStorage.removeItem('user')
    localStorage.removeItem('token')
    localStorage.removeItem('aiChatHistory')
    
    // 通知聊天室用户已登出
    if (oldUser) {
      window.dispatchEvent(new CustomEvent('user-logged-out'))
    }
  }

  return {
    user,
    isLoggedIn,
    checkLoginStatus,
    login,
    register,
    logout
  }
})

