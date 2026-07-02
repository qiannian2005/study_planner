import request from './request'

export const adminApi = {
  getUsers(params = {}) {
    return request({
      url: '/admin/users',
      method: 'GET',
      params
    })
  },

  updateUserStatus(id, status) {
    return request({
      url: `/admin/users/${id}/status`,
      method: 'PUT',
      data: { status }
    })
  },

  resetUserPassword(id, password) {
    return request({
      url: `/admin/users/${id}/password`,
      method: 'PUT',
      data: { password }
    })
  },

  getTopics() {
    return request({
      url: '/admin/topics',
      method: 'GET'
    })
  },

  createTopic(data) {
    return request({
      url: '/admin/topics',
      method: 'POST',
      data
    })
  },

  updateTopic(id, data) {
    return request({
      url: `/admin/topics/${id}`,
      method: 'PUT',
      data
    })
  },

  updateTopicStatus(id, status) {
    return request({
      url: `/admin/topics/${id}/status`,
      method: 'PUT',
      data: { status }
    })
  },

  deleteTopic(id) {
    return request({
      url: `/admin/topics/${id}`,
      method: 'DELETE'
    })
  },

  getTopicSuggestions() {
    return request({
      url: '/admin/topic-suggestions',
      method: 'GET'
    })
  },

  approveTopicSuggestion(id) {
    return request({
      url: `/admin/topic-suggestions/${id}/approve`,
      method: 'PUT'
    })
  },

  rejectTopicSuggestion(id) {
    return request({
      url: `/admin/topic-suggestions/${id}/reject`,
      method: 'PUT'
    })
  },

  deleteTopicSuggestion(id) {
    return request({
      url: `/admin/topic-suggestions/${id}`,
      method: 'DELETE'
    })
  },

  getQuestions() {
    return request({
      url: '/admin/questions',
      method: 'GET'
    })
  },

  updateQuestionStatus(id, status) {
    return request({
      url: `/admin/questions/${id}/status`,
      method: 'PUT',
      data: { status }
    })
  },

  getAnswers() {
    return request({
      url: '/admin/answers',
      method: 'GET'
    })
  },

  updateAnswerStatus(id, status) {
    return request({
      url: `/admin/answers/${id}/status`,
      method: 'PUT',
      data: { status }
    })
  },

  getComments() {
    return request({
      url: '/admin/comments',
      method: 'GET'
    })
  },

  updateCommentStatus(id, status) {
    return request({
      url: `/admin/comments/${id}/status`,
      method: 'PUT',
      data: { status }
    })
  }
}
