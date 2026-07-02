import { defineStore } from 'pinia'
import { ref } from 'vue'
import { 
  questionApi, 
  answerApi, 
  commentApi, 
  topicApi, 
  forumUserApi, 
  searchApi, 
  interactionApi 
} from '../api/forum'
import { showToast } from '../utils/toast'
import i18n from '../i18n'

export const useForumStore = defineStore('forum', () => {
  // 状态
  const questions = ref([])
  const currentQuestion = ref(null)
  const answers = ref([])
  const topics = ref([])
  const hotTopics = ref([])
  const questionsPagination = ref({
    page: 1,
    size: 10,
    total: 0,
    totalPages: 0
  })
  const searchResults = ref({})
  const searchPagination = ref({
    questions: { page: 1, size: 10, total: 0, totalPages: 0 },
    users: { page: 1, size: 10, total: 0, totalPages: 0 },
    topics: { page: 1, size: 10, total: 0, totalPages: 0 }
  })
  const myQuestions = ref([])
  const myQuestionsPagination = ref({
    page: 1,
    size: 10,
    total: 0,
    totalPages: 0
  })
  const myAnswers = ref([])
  const myCollections = ref([])
  const myFollowing = ref({ questions: [], topics: [], users: [] })
  const myFollowers = ref([])
  const loading = ref(false)

  function normalizeRecords(data) {
    if (Array.isArray(data)) return data
    if (Array.isArray(data?.records)) return data.records
    if (Array.isArray(data?.list)) return data.list
    if (Array.isArray(data?.content)) return data.content
    return []
  }

  function normalizePagination(data, fallback = {}) {
    return {
      page: Number(data?.page || fallback.page || 1),
      size: Number(data?.size || fallback.size || 10),
      total: Number(data?.total || 0),
      totalPages: Number(data?.totalPages || 0)
    }
  }

  // 获取问题列表
  async function fetchQuestions(params = {}) {
    try {
      loading.value = true
      const response = await questionApi.getQuestions(params)
      if (response.code === 200) {
        const records = normalizeRecords(response.data)
        questions.value = records
        questionsPagination.value = normalizePagination(response.data, params)
        return records
      } else {
        showToast(response.message || '获取问题列表失败', 'error')
        questions.value = []
        questionsPagination.value = normalizePagination(null, params)
        return []
      }
    } catch (error) {
      console.error('获取问题列表失败:', error)
      showToast('获取问题列表失败', 'error')
      questions.value = []
      questionsPagination.value = normalizePagination(null, params)
      return []
    } finally {
      loading.value = false
    }
  }

  // 获取问题详情
  async function fetchQuestionDetail(id) {
    try {
      loading.value = true
      const response = await questionApi.getQuestionDetail(id)
      if (response.code === 200) {
        currentQuestion.value = response.data
        return response.data
      } else {
        showToast(response.message || '获取问题详情失败', 'error')
        return null
      }
    } catch (error) {
      console.error('获取问题详情失败:', error)
      showToast('获取问题详情失败', 'error')
      return null
    } finally {
      loading.value = false
    }
  }

  async function fetchTopicQuestions(topicId, params = {}) {
    try {
      const response = await topicApi.getTopicQuestions(topicId, params)
      if (response.code === 200) {
        return normalizeRecords(response.data)
      }
      showToast(response.message || '鑾峰彇鐩稿叧闂澶辫触', 'error')
      return []
    } catch (error) {
      console.error('鑾峰彇鐩稿叧闂澶辫触:', error)
      showToast('鑾峰彇鐩稿叧闂澶辫触', 'error')
      return []
    }
  }

  // 创建问题
  async function createQuestion(data) {
    try {
      loading.value = true
      // 处理话题：过滤掉临时ID（大于1000000000000的可能是Date.now()生成的）
      if (data.topic_ids && Array.isArray(data.topic_ids)) {
        data.topic_ids = data.topic_ids.filter(tid => tid && tid < 1000000000000)
      }
      
      const response = await questionApi.createQuestion(data)
      if (response.code === 200) {
        showToast('问题发布成功', 'success')
        // 刷新问题列表
        await fetchQuestions()
        return response.data
      } else {
        showToast(response.message || '发布问题失败', 'error')
        return null
      }
    } catch (error) {
      console.error('发布问题失败:', error)
      showToast('发布问题失败', 'error')
      return null
    } finally {
      loading.value = false
    }
  }

  // 获取回答列表
  async function fetchAnswers(questionId, params = {}) {
    try {
      loading.value = true
      const response = await questionApi.getAnswers(questionId, params)
      if (response.code === 200) {
        answers.value = response.data || []
        return response.data
      } else {
        showToast(response.message || '获取回答列表失败', 'error')
        return []
      }
    } catch (error) {
      console.error('获取回答列表失败:', error)
      showToast('获取回答列表失败', 'error')
      return []
    } finally {
      loading.value = false
    }
  }

  // 创建回答
  async function createAnswer(data) {
    try {
      loading.value = true
      const response = await answerApi.createAnswer(data)
      if (response.code === 200) {
        showToast('回答发布成功', 'success')
        return response.data
      } else {
        showToast(response.message || '发布回答失败', 'error')
        return null
      }
    } catch (error) {
      console.error('发布回答失败:', error)
      showToast('发布回答失败', 'error')
      return null
    } finally {
      loading.value = false
    }
  }

  // 点赞回答
  async function voteAnswer(id) {
    try {
      const response = await answerApi.voteAnswer(id)
      if (response.code === 200 && response.data) {
        // 更新本地状态
        const answer = answers.value.find(a => a.id === id)
        if (answer) {
          answer.vote_count = response.data.vote_count !== undefined ? response.data.vote_count : answer.vote_count
          answer.is_voted = response.data.is_voted !== undefined ? response.data.is_voted : !answer.is_voted
        }
        // 同时更新currentQuestion中的answers（如果存在）
        if (currentQuestion.value && currentQuestion.value.answers) {
          const qAnswer = currentQuestion.value.answers.find(a => a.id === id)
          if (qAnswer) {
            qAnswer.vote_count = response.data.vote_count !== undefined ? response.data.vote_count : qAnswer.vote_count
            qAnswer.is_voted = response.data.is_voted !== undefined ? response.data.is_voted : !qAnswer.is_voted
          }
        }
        return response.data
      } else {
        showToast(response.message || '操作失败', 'error')
        return null
      }
    } catch (error) {
      console.error('点赞失败:', error)
      showToast('操作失败', 'error')
      return null
    }
  }

  // 收藏回答
  async function collectAnswer(id) {
    try {
      const response = await answerApi.collectAnswer(id)
      if (response.code === 200) {
        showToast(response.data.is_collected ? '收藏成功' : '取消收藏成功', 'success')
        const answer = answers.value.find(a => a.id === id)
        if (answer) {
          answer.is_collected = response.data.is_collected
          if (response.data.collect_count !== undefined) {
            answer.collect_count = response.data.collect_count
          }
        }
        return response.data
      } else {
        showToast(response.message || '操作失败', 'error')
        return null
      }
    } catch (error) {
      console.error('收藏失败:', error)
      showToast('操作失败', 'error')
      return null
    }
  }

  // 关注问题
  async function followQuestion(id) {
    try {
      const response = await questionApi.followQuestion(id)
      if (response.code === 200) {
        showToast(response.data.is_followed ? '关注成功' : '取消关注成功', 'success')
        if (currentQuestion.value && currentQuestion.value.id === id) {
          currentQuestion.value.follow_count = response.data.follow_count
          currentQuestion.value.is_followed = response.data.is_followed
        }
        // 更新列表中的状态
        const question = questions.value.find(q => q.id === id)
        if (question) {
          question.follow_count = response.data.follow_count
          question.is_followed = response.data.is_followed
        }
        return response.data
      } else {
        showToast(response.message || '操作失败', 'error')
        return null
      }
    } catch (error) {
      console.error('关注失败:', error)
      showToast('操作失败', 'error')
      return null
    }
  }
  
  // 收藏帖子
  async function favoriteQuestion(id) {
    try {
      const response = await questionApi.favoriteQuestion(id)
      if (response.code === 200) {
        showToast(response.data.is_favorited ? '收藏成功' : '取消收藏成功', 'success')
        // 更新列表中的状态
        const question = questions.value.find(q => q.id === id)
        if (question) {
          question.is_favorited = response.data.is_favorited
          question.favorite_count = response.data.favorite_count
        }
        // 更新详情页状态
        if (currentQuestion.value && currentQuestion.value.id === id) {
          currentQuestion.value.is_favorited = response.data.is_favorited
          currentQuestion.value.favorite_count = response.data.favorite_count
        }
        // 更新我的收藏列表
        const myCollection = myCollections.value.find(q => q.id === id)
        if (myCollection) {
          if (!response.data.is_favorited) {
            // 如果取消收藏，从列表中移除
            const index = myCollections.value.findIndex(q => q.id === id)
            if (index > -1) {
              myCollections.value.splice(index, 1)
            }
          } else {
            myCollection.is_favorited = response.data.is_favorited
            myCollection.favorite_count = response.data.favorite_count
          }
        }
        return response.data
      } else {
        showToast(response.message || '操作失败', 'error')
        return null
      }
    } catch (error) {
      console.error('收藏失败:', error)
      showToast('操作失败', 'error')
      return null
    }
  }

  // 点赞问题
  async function voteQuestion(id) {
    try {
      const response = await questionApi.voteQuestion(id)
      if (response.code === 200 && response.data) {
        showToast(response.data.is_voted ? '点赞成功' : '取消点赞成功', 'success')
        // 更新列表中的状态
        const question = questions.value.find(q => q.id === id)
        if (question) {
          question.is_voted = response.data.is_voted
          question.vote_count = response.data.vote_count
        }
        // 更新详情页状态
        if (currentQuestion.value && currentQuestion.value.id === id) {
          currentQuestion.value.is_voted = response.data.is_voted
          currentQuestion.value.vote_count = response.data.vote_count
        }
        return response.data
      } else {
        showToast(response.message || '操作失败', 'error')
        return null
      }
    } catch (error) {
      console.error('点赞失败:', error)
      showToast('操作失败', 'error')
      return null
    }
  }

  // 关注话题
  async function followTopic(id) {
    try {
      const response = await topicApi.followTopic(id)
      if (response.code === 200 && response.data) {
        const isFollowed = response.data.is_followed !== undefined ? response.data.is_followed : response.data.is_following
        showToast(isFollowed ? '关注成功' : '取消关注成功', 'success')
        return response.data
      } else {
        showToast(response.message || '操作失败', 'error')
        return null
      }
    } catch (error) {
      console.error('关注失败:', error)
      showToast('操作失败', 'error')
      return null
    }
  }

  // 关注用户
  async function followUser(id) {
    try {
      const response = await forumUserApi.followUser(id)
      if (response.code === 200) {
        showToast(response.data.is_following ? '关注成功' : '取消关注成功', 'success')
        return response.data
      } else {
        showToast(response.message || '操作失败', 'error')
        return null
      }
    } catch (error) {
      console.error('关注失败:', error)
      showToast('操作失败', 'error')
      return null
    }
  }

  // 获取热门话题
  async function fetchHotTopics() {
    try {
      const response = await topicApi.getHotTopics()
      if (response.code === 200) {
        hotTopics.value = response.data || []
        return response.data || []
      } else {
        console.warn('获取热门话题失败:', response.message)
        hotTopics.value = []
        return []
      }
    } catch (error) {
      console.error('获取热门话题失败:', error)
      hotTopics.value = []
      return []
    }
  }

  // 搜索
  async function search(params) {
    try {
      loading.value = true
      const response = await searchApi.search(params)
      if (response.code === 200) {
        const data = response.data || {}
        searchResults.value = {
          questions: normalizeRecords(data.questions),
          users: normalizeRecords(data.users),
          topics: normalizeRecords(data.topics)
        }
        searchPagination.value = {
          questions: normalizePagination(data.questions, params),
          users: normalizePagination(data.users, params),
          topics: normalizePagination(data.topics, params)
        }
        return response.data
      } else {
        showToast(response.message || '搜索失败', 'error')
        return {}
      }
    } catch (error) {
      console.error('搜索失败:', error)
      showToast('搜索失败', 'error')
      return {}
    } finally {
      loading.value = false
    }
  }

  // 获取我的提问
  async function fetchMyQuestions(params = {}) {
    try {
      loading.value = true
      console.log('开始获取我的提问，参数:', params)
      const response = await interactionApi.getMyQuestions(params)
      console.log('获取我的提问响应:', response)
      if (response.code === 200) {
        myQuestions.value = normalizeRecords(response.data)
        myQuestionsPagination.value = normalizePagination(response.data, params)
        console.log('我的提问数量:', myQuestions.value.length)
        return myQuestions.value
      } else {
        console.error('获取我的提问失败:', response.message)
        showToast(response.message || '获取失败', 'error')
        myQuestions.value = []
        myQuestionsPagination.value = normalizePagination(null, params)
        return []
      }
    } catch (error) {
      console.error('获取我的提问异常:', error)
      console.error('错误详情:', error.response || error.message)
      showToast('获取失败: ' + (error.message || '网络错误'), 'error')
      myQuestions.value = []
      myQuestionsPagination.value = normalizePagination(null, params)
      return []
    } finally {
      loading.value = false
      console.log('获取我的提问完成，loading设置为false')
    }
  }

  // 获取我的回答
  async function fetchMyAnswers(params = {}) {
    try {
      loading.value = true
      const response = await interactionApi.getMyAnswers(params)
      if (response.code === 200) {
        myAnswers.value = response.data || []
        return response.data
      } else {
        showToast(response.message || '获取失败', 'error')
        return []
      }
    } catch (error) {
      console.error('获取失败:', error)
      showToast('获取失败', 'error')
      return []
    } finally {
      loading.value = false
    }
  }

  // 获取我的收藏
  async function fetchMyCollections(params = {}) {
    try {
      loading.value = true
      const response = await interactionApi.getMyCollections(params)
      if (response.code === 200) {
        myCollections.value = response.data || []
        return response.data
      } else {
        showToast(response.message || '获取失败', 'error')
        return []
      }
    } catch (error) {
      console.error('获取失败:', error)
      showToast('获取失败', 'error')
      return []
    } finally {
      loading.value = false
    }
  }

  // 获取我的关注
  async function fetchMyFollowing(params = {}) {
    try {
      loading.value = true
      const response = await interactionApi.getMyFollowing(params)
      if (response.code === 200) {
        const data = response.data || { questions: [], topics: [], users: [] }
        myFollowing.value = {
          ...data,
          questions: normalizeRecords(data.questions),
          users: normalizeRecords(data.users),
          topics: normalizeRecords(data.topics)
        }
        return myFollowing.value
      } else {
        showToast(response.message || '获取失败', 'error')
        myFollowing.value = { questions: [], topics: [], users: [] }
        return { questions: [], topics: [], users: [] }
      }
    } catch (error) {
      console.error('获取我的关注失败:', error)
      showToast('获取失败', 'error')
      myFollowing.value = { questions: [], topics: [], users: [] }
      return { questions: [], topics: [], users: [] }
    } finally {
      loading.value = false
    }
  }

  // 获取我的粉丝
  async function fetchMyFollowers(params = {}) {
    try {
      loading.value = true
      const response = await interactionApi.getMyFollowers(params)
      if (response.code === 200) {
        myFollowers.value = response.data || []
        return response.data || []
      } else {
        showToast(response.message || '获取失败', 'error')
        myFollowers.value = []
        return []
      }
    } catch (error) {
      console.error('获取我的粉丝失败:', error)
      showToast('获取失败', 'error')
      myFollowers.value = []
      return []
    } finally {
      loading.value = false
    }
  }

  return {
    // 状态
    questions,
    questionsPagination,
    currentQuestion,
    answers,
    topics,
    hotTopics,
    searchResults,
    searchPagination,
    myQuestions,
    myQuestionsPagination,
    myAnswers,
    myCollections,
    myFollowing,
    myFollowers,
    loading,
    // 方法
    fetchQuestions,
    fetchQuestionDetail,
    fetchTopicQuestions,
    createQuestion,
    fetchAnswers,
    createAnswer,
    voteAnswer,
    collectAnswer,
    followQuestion,
    voteQuestion,
    followTopic,
    followUser,
    fetchHotTopics,
    search,
    fetchMyQuestions,
    fetchMyAnswers,
    fetchMyCollections,
    fetchMyFollowing,
    fetchMyFollowers,
    favoriteQuestion
  }
})
