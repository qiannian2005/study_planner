import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { wsManager } from '../utils/websocket'
import { useUserStore } from './user'
import { showToast } from '../utils/toast'
import i18n from '../i18n'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

export const useChatStore = defineStore('chat', () => {
  const userStore = useUserStore()

  const messages = ref([])
  const onlineUsers = ref([])
  const isConnected = ref(false)
  const isLoading = ref(false)
  const hiddenMessageIds = ref(new Set())
  const onlineCount = computed(() => onlineUsers.value.length)

  const eventListeners = {
    connected: null,
    disconnected: null,
    message: null,
    user_joined: null,
    user_left: null,
    online_users: null
  }

  function initConnection() {
    if (!userStore.isLoggedIn) {
      showToast(i18n.global.t('auth.loginRequired'), 'warning')
      return
    }

    if (isConnected.value) {
      disconnect()
      setTimeout(() => {
        doConnect()
      }, 1000)
      return
    }

    doConnect()
  }

  function doConnect() {
    removeEventListeners()

    const token = userStore.user?.id ? String(userStore.user.id) : ''
    loadHiddenMessageIds()

    eventListeners.connected = async () => {
      isConnected.value = true
      await loadOnlineUsers()
      await loadHistoryMessages()
    }
    wsManager.on('connected', eventListeners.connected)

    eventListeners.disconnected = () => {
      isConnected.value = false
    }
    wsManager.on('disconnected', eventListeners.disconnected)

    eventListeners.message = (messageData) => {
      if (messageData?.room_type && messageData.room_type !== 'global') {
        return
      }
      if (messageData && messageData.content) {
        addMessage(messageData)
      }
    }
    wsManager.on('message', eventListeners.message)

    eventListeners.user_joined = (userData) => {
      if (userData.user_id !== userStore.user?.id) {
        showToast(i18n.global.t('chat.userJoined', { username: userData.username }), 'info')
      }
      updateOnlineUsers(userData.users || [])
    }
    wsManager.on('user_joined', eventListeners.user_joined)

    eventListeners.user_left = (userData) => {
      if (userData.user_id !== userStore.user?.id) {
        showToast(i18n.global.t('chat.userLeft', { username: userData.username }), 'info')
      }
      updateOnlineUsers(userData.users || [])
    }
    wsManager.on('user_left', eventListeners.user_left)

    eventListeners.online_users = (users) => {
      updateOnlineUsers(users)
    }
    wsManager.on('online_users', eventListeners.online_users)

    wsManager.connect(token)
  }

  function removeEventListeners() {
    Object.keys(eventListeners).forEach((event) => {
      if (eventListeners[event]) {
        wsManager.off(event, eventListeners[event])
        eventListeners[event] = null
      }
    })
  }

  function disconnect() {
    removeEventListeners()
    wsManager.disconnect()
    isConnected.value = false
    messages.value = []
    onlineUsers.value = []
  }

  if (typeof window !== 'undefined') {
    window.addEventListener('user-changed', () => {
      if (isConnected.value) {
        disconnect()
        setTimeout(() => {
          if (userStore.isLoggedIn) {
            initConnection()
          }
        }, 1000)
      }
    })

    window.addEventListener('user-logged-out', () => {
      disconnect()
    })
  }

  function addMessage(messageData) {
    const messageId = messageData.id || Date.now() + Math.random()
    if (hiddenMessageIds.value.has(String(messageId))) {
      return
    }

    const existingMessage = messages.value.find((msg) =>
      msg.id === messageId ||
      (msg.user_id === messageData.user_id &&
        msg.content === messageData.content &&
        Math.abs(new Date(msg.created_at) - new Date(messageData.created_at || new Date())) < 1000)
    )
    if (existingMessage) {
      return
    }

    messages.value.push({
      id: messageId,
      user_id: messageData.user_id,
      username: messageData.username || '',
      avatar: messageData.avatar || '',
      content: messageData.content,
      created_at: messageData.created_at || new Date().toISOString(),
      is_own: messageData.user_id === userStore.user?.id
    })

    if (messages.value.length > 500) {
      messages.value = messages.value.slice(-500)
    }
  }

  function updateOnlineUsers(users) {
    onlineUsers.value = users || []
  }

  function sendMessage(content) {
    if (!content || !content.trim()) {
      return false
    }

    if (!isConnected.value) {
      showToast(i18n.global.t('chat.connectionNotEstablished'), 'warning')
      return false
    }

    return wsManager.sendMessage(content)
  }

  async function loadHistoryMessages() {
    try {
      isLoading.value = true
      const { chatApi } = await import('../api/chat')
      const response = await chatApi.getMessages(1, 50)

      if (response.code === 200 && response.data?.list) {
        messages.value = []
        response.data.list.forEach(addMessage)
      }
    } catch (error) {
      console.error('Failed to load chat history:', error)
    } finally {
      isLoading.value = false
    }
  }

  async function loadOnlineUsers() {
    try {
      const { chatApi } = await import('../api/chat')
      const response = await chatApi.getOnlineUsers()
      if (response.code === 200 && Array.isArray(response.data)) {
        updateOnlineUsers(response.data)
      }
    } catch (error) {
      console.error('Failed to load online users:', error)
    }
  }

  function formatTime(timestamp) {
    const date = dayjs(timestamp)
    const now = dayjs()
    const diff = now.diff(date, 'minute')

    if (diff < 60) {
      return date.fromNow()
    }
    if (diff < 1440) {
      return date.format('HH:mm')
    }
    return date.format('YYYY-MM-DD HH:mm')
  }

  function clearMessages() {
    loadHiddenMessageIds()
    messages.value.forEach((message) => hiddenMessageIds.value.add(String(message.id)))
    saveHiddenMessageIds()
    messages.value = []
  }

  function getHiddenMessagesKey() {
    const userId = userStore.user?.id
    return userId ? `chat_hidden_message_ids_${userId}` : null
  }

  function loadHiddenMessageIds() {
    const key = getHiddenMessagesKey()
    if (!key || typeof localStorage === 'undefined') {
      hiddenMessageIds.value = new Set()
      return
    }

    try {
      const saved = JSON.parse(localStorage.getItem(key) || '[]')
      hiddenMessageIds.value = new Set(Array.isArray(saved) ? saved.map(String) : [])
    } catch (error) {
      console.warn('Failed to load hidden chat messages:', error)
      hiddenMessageIds.value = new Set()
    }
  }

  function saveHiddenMessageIds() {
    const key = getHiddenMessagesKey()
    if (!key || typeof localStorage === 'undefined') {
      return
    }
    localStorage.setItem(key, JSON.stringify([...hiddenMessageIds.value]))
  }

  return {
    messages,
    onlineUsers,
    isConnected,
    isLoading,
    onlineCount,
    initConnection,
    disconnect,
    sendMessage,
    loadHistoryMessages,
    loadOnlineUsers,
    formatTime,
    clearMessages
  }
})
