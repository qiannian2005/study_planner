export function formatDate(dateString) {
  const date = parseDateTime(dateString)
  if (!date) return '-'

  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

export function formatDateTime(dateString) {
  const date = parseDateTime(dateString)
  if (!date) return '-'

  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

export function escapeHtml(text) {
  if (!text) return ''
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

export function truncateText(text, maxLength) {
  if (!text) return ''
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}

export function formatNumber(value) {
  if (value === undefined || value === null || value === '') return '0'
  const num = parseFloat(value)
  if (Number.isNaN(num)) return '0'
  return parseFloat(num.toFixed(2)).toString()
}

export function parseDateTime(value) {
  if (!value) return null

  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : value
  }

  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0, nano = 0] = value.map(Number)
    if (!year || !month || !day) return null

    const millisecond = nano > 999 ? Math.floor(nano / 1000000) : nano
    const date = new Date(year, month - 1, day, hour, minute, second, millisecond)
    return Number.isNaN(date.getTime()) ? null : date
  }

  if (typeof value === 'number') {
    const timestamp = value < 1000000000000 ? value * 1000 : value
    const date = new Date(timestamp)
    return Number.isNaN(date.getTime()) ? null : date
  }

  const text = String(value).trim()
  if (!text) return null

  if (/^\d+$/.test(text)) {
    return parseDateTime(Number(text))
  }

  const localDateTimeMatch = text.match(
    /^(\d{4})-(\d{1,2})-(\d{1,2})(?:[ T](\d{1,2}):(\d{1,2})(?::(\d{1,2})(?:\.(\d{1,9}))?)?)?$/
  )

  if (localDateTimeMatch) {
    const [, year, month, day, hour = 0, minute = 0, second = 0, fraction = '0'] = localDateTimeMatch
    const millisecond = Number(fraction.padEnd(3, '0').slice(0, 3))
    const date = new Date(
      Number(year),
      Number(month) - 1,
      Number(day),
      Number(hour),
      Number(minute),
      Number(second),
      millisecond
    )
    return Number.isNaN(date.getTime()) ? null : date
  }

  const date = new Date(text)
  return Number.isNaN(date.getTime()) ? null : date
}

export function formatTime(dateString) {
  const date = parseDateTime(dateString)
  if (!date) return '-'

  return formatDateTimeForDisplay(date)
}

function formatDateTimeForDisplay(date) {
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
