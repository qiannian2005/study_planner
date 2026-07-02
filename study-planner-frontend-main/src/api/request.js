import axios from 'axios'
import { useUserStore } from '../stores/user'
import { showToast } from '../utils/toast'
import i18n from '../i18n'

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api', // Vite 会代理到后端
  timeout: 120000, // 延长超时时间到 2 分钟，适应 LLM 生成
  withCredentials: true, // 携带 cookie
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'  // 添加 Accept 头，解决 406 错误
  }
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 如果是 FormData，让 axios 自动设置 Content-Type（包括 boundary）
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    // 检查响应数据格式
    if (!response || !response.data) {
      console.warn('响应数据格式异常:', response)
      return { code: 500, message: '响应数据格式异常', data: null }
    }
    
    const res = response.data
    
    // 如果返回的状态码为 401，说明未登录或登录过期
    if (res.code === 401) {
      const userStore = useUserStore()
      userStore.user = null
      localStorage.removeItem('user')
      localStorage.removeItem('token')
      
      showToast(i18n.global.t('auth.sessionExpired'), 'warning')
      
      // 跳转到登录页
      if (window.location.pathname !== '/login') {
        window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname)
      }
      return Promise.reject(new Error('未登录'))
    }
    
    return res
  },
  error => {
    console.error('响应错误:', error)
    if (error.response) {
      const status = error.response.status
      // 处理401未授权错误
      if (status === 401) {
        const userStore = useUserStore()
        userStore.user = null
        localStorage.removeItem('user')
        localStorage.removeItem('token')
        
        // 如果不在登录页，跳转到登录页
        if (window.location.pathname !== '/login') {
          showToast(i18n.global.t('auth.sessionExpired'), 'warning')
          window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname)
        }
        return Promise.reject(error)
      }
      
      // 其他HTTP错误
      const message = error.response.data?.message || `请求失败: ${status}`
      showToast(message, 'error')
    } else if (error.request) {
      showToast(i18n.global.t('errors.network'), 'error')
    } else {
      showToast(i18n.global.t('errors.requestConfig'), 'error')
    }
    return Promise.reject(error)
  }
)

export default request

