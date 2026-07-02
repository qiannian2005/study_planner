import { Toast } from 'bootstrap'
import i18n from '../i18n'

const SILENT_TOAST_MESSAGES = new Set([
  'chat.connected',
  'chat.connectionFailed',
  '已连接到聊天室',
  '连接失败，请刷新页面重试',
  'Connected to chat room',
  'Connection failed, please refresh the page'
])

/**
 * 显示提示消息
 * @param {string} message - 提示消息（可以是i18n键或普通文本）
 * @param {string} type - 消息类型: success, error, warning, info
 */
export function showToast(message, type = 'info') {
  if (SILENT_TOAST_MESSAGES.has(message)) {
    return
  }

  // 如果message看起来像i18n键（包含点号），尝试翻译
  let displayMessage = message
  if (message && message.includes('.')) {
    try {
      const translated = i18n.global.t(message)
      // 如果翻译结果和原文本不同，说明翻译成功
      if (translated !== message) {
        displayMessage = translated
      }
    } catch (e) {
      // 翻译失败，使用原文本
      displayMessage = message
    }
  }

  if (SILENT_TOAST_MESSAGES.has(displayMessage)) {
    return
  }

  let toastContainer = document.getElementById('toastContainer')
  if (!toastContainer) {
    toastContainer = document.createElement('div')
    toastContainer.id = 'toastContainer'
    toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3'
    toastContainer.style.zIndex = '9999'
    document.body.appendChild(toastContainer)
  }
  
  const toastId = 'toast-' + Date.now()
  const bgClass = {
    'success': 'bg-success',
    'error': 'bg-danger',
    'warning': 'bg-warning',
    'info': 'bg-info'
  }[type] || 'bg-info'
  
  const toastHtml = `
    <div id="${toastId}" class="toast ${bgClass} text-white" role="alert">
      <div class="toast-body d-flex justify-content-between align-items-center">
        ${displayMessage}
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="toast"></button>
      </div>
    </div>
  `
  
  toastContainer.insertAdjacentHTML('beforeend', toastHtml)
  
  const toastElement = document.getElementById(toastId)
  const toast = new Toast(toastElement, { delay: 3000 })
  toast.show()
  
  toastElement.addEventListener('hidden.bs.toast', () => {
    toastElement.remove()
  })
}
