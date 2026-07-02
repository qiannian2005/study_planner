import request from './request'

export const checkinApi = {
  // 打卡
  checkIn(data) {
    return request({
      url: '/checkin',
      method: 'POST',
      data
    })
  },

  // 获取打卡统计
  getStats() {
    return request({
      url: '/checkin/stats',
      method: 'GET'
    })
  },

  // 获取打卡日历
  getCalendar(year, month) {
    return request({
      url: '/checkin/calendar',
      method: 'GET',
      params: { year, month }
    })
  },

  // 获取图表数据
  getChartData() {
    return request({
      url: '/checkin/chart-data',
      method: 'GET'
    })
  },

  // 获取打卡记录
  getHistory(params) {
    return request({
      url: '/checkin/history',
      method: 'GET',
      params
    })
  }
}

