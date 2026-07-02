import request from './request'

// 问题相关 API
export const questionApi = {
  // 获取问题列表
  getQuestions(params = {}) {
    return request({
      url: '/forum/question',
      method: 'get',
      params
    })
  },
  
  // 获取问题详情
  getQuestionDetail(id) {
    return request({
      url: `/forum/question/${id}`,
      method: 'get'
    })
  },
  
  // 创建问题
  createQuestion(data) {
    return request({
      url: '/forum/question',
      method: 'post',
      data
    })
  },
  
  // 更新问题
  updateQuestion(id, data) {
    return request({
      url: `/forum/question/${id}`,
      method: 'put',
      data
    })
  },
  
  // 删除问题
  deleteQuestion(id) {
    return request({
      url: `/forum/question/${id}`,
      method: 'delete'
    })
  },
  
  // 关注/取消关注问题
  followQuestion(id) {
    return request({
      url: `/forum/question/${id}/follow`,
      method: 'post'
    })
  },
  
  // 收藏/取消收藏帖子
  favoriteQuestion(id) {
    return request({
      url: `/forum/question/${id}/favorite`,
      method: 'post'
    })
  },
  
  // 点赞/取消点赞帖子
  voteQuestion(id) {
    return request({
      url: `/forum/question/${id}/vote`,
      method: 'post'
    })
  },
  
  // 获取问题的回答列表
  getAnswers(questionId, params = {}) {
    return request({
      url: `/forum/question/${questionId}/answers`,
      method: 'get',
      params
    })
  }
}

// 回答相关 API
export const answerApi = {
  // 获取回答详情
  getAnswerDetail(id) {
    return request({
      url: `/forum/answer/${id}`,
      method: 'get'
    })
  },
  
  // 创建回答
  createAnswer(data) {
    return request({
      url: '/forum/answer',
      method: 'post',
      data
    })
  },
  
  // 更新回答
  updateAnswer(id, data) {
    return request({
      url: `/forum/answer/${id}`,
      method: 'put',
      data
    })
  },
  
  // 删除回答
  deleteAnswer(id) {
    return request({
      url: `/forum/answer/${id}`,
      method: 'delete'
    })
  },
  
  // 点赞/取消点赞回答
  voteAnswer(id) {
    return request({
      url: `/forum/answer/${id}/vote`,
      method: 'post'
    })
  },
  
  // 收藏/取消收藏回答
  collectAnswer(id) {
    return request({
      url: `/forum/answer/${id}/collect`,
      method: 'post'
    })
  }
}

// 评论相关 API
export const commentApi = {
  // 获取评论列表
  getComments(params = {}) {
    return request({
      url: '/forum/comment',
      method: 'get',
      params
    })
  },
  
  // 创建评论
  createComment(data) {
    return request({
      url: '/forum/comment',
      method: 'post',
      data
    })
  },
  
  // 更新评论
  updateComment(id, data) {
    return request({
      url: `/forum/comment/${id}`,
      method: 'put',
      data
    })
  },
  
  // 删除评论
  deleteComment(id) {
    return request({
      url: `/forum/comment/${id}`,
      method: 'delete'
    })
  },
  
  // 点赞评论
  voteComment(id) {
    return request({
      url: `/forum/comment/${id}/vote`,
      method: 'post'
    })
  }
}

// 话题相关 API
export const topicApi = {
  // 获取话题列表
  getTopics(params = {}) {
    return request({
      url: '/forum/topic',
      method: 'get',
      params
    })
  },
  
  // 获取话题详情
  getTopicDetail(id) {
    return request({
      url: `/forum/topic/${id}`,
      method: 'get'
    })
  },
  
  // 获取话题下的问题
  getTopicQuestions(id, params = {}) {
    return request({
      url: `/forum/topic/${id}/questions`,
      method: 'get',
      params
    })
  },

  getTopAnswerers(id) {
    return request({
      url: `/forum/topic/${id}/top-answerers`,
      method: 'get'
    })
  },

  getTopAnswers(id) {
    return request({
      url: `/forum/topic/${id}/top-answers`,
      method: 'get'
    })
  },

  getHotTopicQuestions(id) {
    return request({
      url: `/forum/topic/${id}/questions`,
      method: 'get',
      params: { sort: 'hot' }
    })
  },
  
  // 关注/取消关注话题
  followTopic(id) {
    return request({
      url: `/forum/topic/${id}/follow`,
      method: 'post'
    })
  },
  
  // 获取热门话题
  getHotTopics() {
    return request({
      url: '/forum/topic/hot',
      method: 'get'
    })
  },
  
  // 创建话题
  suggestTopic(data) {
    return request({
      url: '/forum/topic/suggestion',
      method: 'post',
      data
    })
  },

  createTopic(data) {
    return request({
      url: '/forum/topic',
      method: 'post',
      data
    })
  }
}

// 用户相关 API
export const forumUserApi = {
  // 获取用户信息
  getUserInfo(id) {
    return request({
      url: `/forum/user/${id}`,
      method: 'get'
    })
  },
  
  // 获取用户的提问
  getUserQuestions(id, params = {}) {
    return request({
      url: `/forum/user/${id}/questions`,
      method: 'get',
      params
    })
  },
  
  // 获取用户的回答
  getUserAnswers(id, params = {}) {
    return request({
      url: `/forum/user/${id}/answers`,
      method: 'get',
      params
    })
  },
  
  // 获取用户的收藏
  getUserCollections(id, params = {}) {
    return request({
      url: `/forum/user/${id}/collections`,
      method: 'get',
      params
    })
  },
  
  // 关注/取消关注用户
  followUser(id) {
    return request({
      url: `/forum/user/${id}/follow`,
      method: 'post'
    })
  },
  
  // 获取粉丝列表
  getFollowers(id, params = {}) {
    return request({
      url: `/forum/user/${id}/followers`,
      method: 'get',
      params
    })
  },

  // 获取关注列表
  getFollowing(id, params = {}) {
    return request({
      url: `/forum/user/${id}/following`,
      method: 'get',
      params
    })
  },

  // 获取当前用户的论坛统计
  getUserStats() {
    return request({
      url: '/forum/user/stats',
      method: 'get'
    })
  }
}

// 搜索相关 API
export const searchApi = {
  // 搜索
  search(params) {
    return request({
      url: '/forum/search',
      method: 'get',
      params
    })
  },
  
  // 搜索建议
  getSuggestions(keyword) {
    return request({
      url: '/forum/search/suggest',
      method: 'get',
      params: { keyword }
    })
  }
}

// 互动相关 API
export const interactionApi = {
  // 我的提问
  getMyQuestions(params = {}) {
    return request({
      url: '/forum/my/questions',
      method: 'get',
      params
    })
  },
  
  // 我的回答
  getMyAnswers(params = {}) {
    return request({
      url: '/forum/my/answers',
      method: 'get',
      params
    })
  },
  
  // 我的收藏
  getMyCollections(params = {}) {
    return request({
      url: '/forum/my/collections',
      method: 'get',
      params
    })
  },
  
  // 我关注的内容
  getMyFollowing(params = {}) {
    return request({
      url: '/forum/my/following',
      method: 'get',
      params
    })
  },
  
  // 我的粉丝
  getMyFollowers(params = {}) {
    return request({
      url: '/forum/my/followers',
      method: 'get',
      params
    })
  }
}
