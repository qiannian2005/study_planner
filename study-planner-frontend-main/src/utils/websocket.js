import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { showToast } from './toast'
import i18n from '../i18n'

class WebSocketManager {
  constructor() {
    this.client = null
    this.subscription = null
    this.reconnectDelay = 3000
    this.listeners = new Map()
    this.isConnecting = false
    this.isConnected = false
    this.lastSentMessage = null
  }

  connect(token) {
    if (this.isConnecting || (this.isConnected && this.client?.connected)) {
      return
    }

    this.isConnecting = true

    const wsProtocol = window.location.protocol === 'https:' ? 'https:' : 'http:'
    const wsHost = import.meta.env.VITE_WS_URL || window.location.host
    let wsUrl = `${wsProtocol}//${wsHost}/api/chat/ws`
    if (token) {
      wsUrl += `?token=${encodeURIComponent(token)}`
    }

    try {
      this.client = new Client({
        webSocketFactory: () => new SockJS(wsUrl),
        reconnectDelay: this.reconnectDelay,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        debug: () => {},
        onConnect: () => {
          this.isConnected = true
          this.isConnecting = false
          this.subscribeToMessages()
          setTimeout(() => {
            this.emit('connected')
          }, 100)
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame)
          this.isConnecting = false
          this.emit('error', frame)
        },
        onWebSocketClose: () => {
          this.isConnected = false
          this.isConnecting = false
          this.subscription = null
          this.emit('disconnected')
        },
        onDisconnect: () => {
          this.isConnected = false
          this.isConnecting = false
          this.subscription = null
          this.emit('disconnected')
        }
      })

      this.client.activate()
    } catch (error) {
      console.error('WebSocket connection failed:', error)
      this.isConnecting = false
      this.isConnected = false
    }
  }

  subscribeToMessages() {
    if (!this.client || !this.client.connected || this.subscription) {
      return
    }

    this.subscription = this.client.subscribe('/topic/chat', (message) => {
      try {
        const data = JSON.parse(message.body)
        this.handleMessage(data)
      } catch (error) {
        console.error('Failed to parse websocket message:', error)
      }
    })
  }

  handleMessage(data) {
    const { type, payload } = data || {}

    switch (type) {
      case 'message':
        this.emit('message', payload)
        break
      case 'user_joined':
        this.emit('user_joined', payload)
        break
      case 'user_left':
        this.emit('user_left', payload)
        break
      case 'online_users':
        this.emit('online_users', payload)
        break
      case 'error':
        showToast(payload?.message || i18n.global.t('errors.server'), 'error')
        this.emit('error', payload)
        break
      default:
        console.log('Unknown websocket message type:', type, payload)
    }
  }

  sendMessage(content, options = {}) {
    if (!this.isConnected || !this.client?.connected) {
      showToast(i18n.global.t('chat.connectionNotEstablished'), 'warning')
      return false
    }

    const trimmedContent = content.trim()
    const messageKey = `${trimmedContent}_${Math.floor(Date.now() / 1000)}`
    if (this.lastSentMessage === messageKey) {
      return false
    }

    try {
      this.client.publish({
        destination: '/app/chat/message',
        body: JSON.stringify({
          type: 'message',
          payload: {
            content: trimmedContent,
            roomType: options.roomType || 'global',
            roomId: options.roomId || null
          }
        })
      })

      this.lastSentMessage = messageKey
      setTimeout(() => {
        if (this.lastSentMessage === messageKey) {
          this.lastSentMessage = null
        }
      }, 2000)

      return true
    } catch (error) {
      console.error('Failed to send websocket message:', error)
      showToast(i18n.global.t('chat.messageSendFailed'), 'error')
      return false
    }
  }

  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, [])
    }
    this.listeners.get(event).push(callback)
  }

  off(event, callback) {
    if (!this.listeners.has(event)) {
      return
    }
    const callbacks = this.listeners.get(event)
    const index = callbacks.indexOf(callback)
    if (index > -1) {
      callbacks.splice(index, 1)
    }
  }

  emit(event, data) {
    if (!this.listeners.has(event)) {
      return
    }
    this.listeners.get(event).forEach((callback) => {
      try {
        callback(data)
      } catch (error) {
        console.error(`WebSocket listener error (${event}):`, error)
      }
    })
  }

  disconnect() {
    if (this.subscription) {
      this.subscription.unsubscribe()
      this.subscription = null
    }

    if (this.client) {
      this.client.deactivate()
      this.client = null
    }

    this.isConnected = false
    this.isConnecting = false
    this.lastSentMessage = null
  }

  getStatus() {
    return {
      isConnected: this.isConnected && this.client?.connected,
      isConnecting: this.isConnecting,
      readyState: this.client?.connected ? 1 : 0
    }
  }
}

export const wsManager = new WebSocketManager()
