import { createI18n } from 'vue-i18n'
import zhCN from './locales/zh-CN.json'
import enUS from './locales/en-US.json'

const savedLocale = localStorage.getItem('locale') || 'zh-CN'

const forumZhCN = {
  admin: {
    title: '管理员控制台',
    subtitle: '管理用户、话题和论坛内容可见性。',
    tabs: {
      users: '用户管理',
      topics: '话题管理',
      content: '内容管理'
    },
    contentTypes: {
      questions: '问题',
      answers: '回答',
      comments: '评论'
    },
    searchUser: '搜索用户名或邮箱',
    user: '用户',
    email: '邮箱',
    role: '角色',
    roleAdmin: '管理员',
    roleUser: '用户',
    status: '状态',
    content: '内容',
    action: '操作',
    contentStats: '计划 {plans} / 提问 {questions} / 回答 {answers}',
    enable: '解禁',
    disable: '禁用',
    resetPassword: '重置密码',
    resetPasswordFor: '为 {username} 设置新密码',
    newPassword: '新密码',
    newPasswordPlaceholder: '请输入至少 6 位新密码',
    passwordHint: '重置后，该用户需要使用新密码登录。',
    confirmResetPassword: '确认重置',
    passwordResetSuccess: '密码已重置',
    passwordResetFailed: '密码重置失败',
    noUsers: '暂无用户',
    topicName: '话题名称',
    topicDescription: '话题描述',
    saveTopic: '保存话题',
    addTopic: '新增话题',
    topic: '话题',
    description: '描述',
    data: '数据',
    topicStats: '问题 {questions} / 关注 {follows}',
    restore: '恢复',
    hide: '隐藏',
    noTopics: '暂无话题',
    author: '作者',
    noContent: '暂无内容',
    statusActive: '正常',
    statusDisabled: '已禁用',
    statusHidden: '已隐藏',
    questionWithId: '问题 {id}',
    loadUsersFailed: '加载用户失败',
    loadTopicsFailed: '加载话题失败',
    loadContentFailed: '加载内容失败',
    userStatusUpdated: '用户状态已更新',
    topicUpdated: '话题已更新',
    topicCreated: '话题已创建',
    topicStatusUpdated: '话题状态已更新',
    contentStatusUpdated: '内容状态已更新'
  },
  forum: {
    title: '论坛',
    home: '首页',
    hot: '热门',
    recommend: '推荐',
    following: '关注',
    favorite: '收藏',
    myContent: '我的内容',
    search: '搜索',
    noResults: '暂无结果',
    loading: '加载中...',
    error: '加载失败',
    retry: '重试',
    relatedQuestions: '相关问题',
    relatedQuestionsEmpty: '暂无相关问题',
    question: {
      title: '标题',
      content: '内容',
      topics: '话题',
      anonymous: '匿名',
      submit: '发布',
      edit: '编辑',
      delete: '删除',
      confirmDelete: '确定要删除这个问题吗？',
      noQuestions: '暂无问题',
      views: '浏览',
      answers: '回答',
      favorites: '收藏',
      votes: '点赞',
      follows: '关注',
      createdAt: '发布于',
      author: '作者',
      anonymousUser: '匿名用户'
    },
    answer: {
      title: '回答',
      content: '回答内容',
      submit: '发布回答',
      edit: '编辑',
      delete: '删除',
      confirmDelete: '确定要删除这条回答吗？',
      noAnswers: '暂无回答，成为第一个回答者吧',
      count: '{count} 个回答',
      sortDefault: '默认排序',
      sortTime: '时间排序',
      sortVote: '点赞排序',
      votes: '点赞',
      comments: '评论',
      collect: '收藏',
      reply: '回复',
      createdAt: '发布于'
    },
    comment: {
      title: '评论',
      content: '内容',
      submit: '发送',
      placeholder: '写下你的评论...',
      noComments: '暂无评论',
      reply: '回复',
      votes: '点赞',
      createdAt: '发布于'
    },
    topic: {
      title: '话题',
      follow: '关注',
      followed: '已关注',
      followers: '关注',
      questions: '问题',
      noQuestions: '该话题下暂无问题',
      description: '描述'
    },
    topicPage: {
      followers: '关注',
      questions: '问题',
      follow: '关注',
      followed: '已关注',
      relatedQuestions: '相关问题',
      noQuestions: '该话题下暂无问题',
      topAnswerers: '优秀回答者',
      topAnswers: '热门讨论',
      noData: '暂无数据',
      answers: '回答',
      votes: '获赞',
      comments: '评论'
    },
    userProfile: {
      questions: '提问',
      answers: '回答',
      votes: '获赞',
      collections: '收藏',
      following: '关注',
      followers: '粉丝',
      follow: '关注',
      followed: '已关注',
      registeredAt: '注册于',
      noQuestions: '暂无提问',
      noAnswers: '暂无回答',
      noCollections: '暂无收藏',
      noFollowing: '暂无关注',
      noFollowers: '暂无粉丝'
    },
    user: {
      profile: '个人资料',
      questions: '提问',
      answers: '回答',
      collections: '收藏',
      following: '关注',
      followers: '粉丝',
      follow: '关注',
      followed: '已关注',
      unfollow: '取消关注',
      registerTime: '注册于',
      statistics: '统计',
      posts: '帖子',
      comments: '评论',
      likes: '点赞',
      favorites: '收藏'
    },
    questionCard: {
      anonymousUser: '匿名用户',
      answers: '回答',
      views: '浏览',
      follows: '关注',
      favorites: '收藏',
      favorite: '收藏'
    },
    actions: {
      favorite: '收藏',
      unfavorite: '取消收藏',
      vote: '点赞',
      unvote: '取消点赞',
      follow: '关注',
      unfollow: '取消关注',
      collect: '收藏',
      uncollect: '取消收藏'
    },
    search: {
      placeholder: '搜索问题、用户、话题...',
      search: '搜索',
      all: '全部',
      question: '问题',
      user: '用户',
      topic: '话题',
      relevance: '相关度',
      time: '时间',
      enterKeyword: '请输入关键词进行搜索',
      questions: '问题 ({count})',
      users: '用户 ({count})',
      topics: '话题 ({count})',
      noResults: '没有找到相关结果'
    },
    myContent: {
      title: '我的内容',
      myQuestions: '我的提问',
      myAnswers: '我的回答',
      myCollections: '我的收藏',
      myFollowing: '我的关注',
      myFollowers: '我的粉丝',
      noQuestions: '暂无提问',
      noAnswers: '暂无回答',
      noCollections: '暂无收藏',
      noFollowing: '暂无关注',
      noFollowers: '暂无粉丝',
      goToAsk: '去提问',
      answers: '回答',
      views: '浏览',
      favorites: '收藏',
      votes: '点赞',
      comments: '评论',
      collected: '已收藏',
      notCollected: '未收藏',
      viewDetail: '查看详情',
      delete: '删除',
      learningProgress: '学习进度',
      noTarget: '暂无目标描述'
    }
  },
  dashboard: {
    studyStats: '学习统计'
  }
}

const adminZhCNExtra = {
  admin: {
    statusDeleted: '已删除',
    topicDeleted: '话题已删除',
    deleteTopicFailed: '删除话题失败',
    deleteTopicTitle: '删除话题',
    deleteTopicConfirm: '确定要删除话题「{name}」吗？',
    deleteTopicHint: '删除后会移除这个话题以及相关关注、问题关联，问题本身不会被删除。',
    contentDeleted: '内容已删除'
  }
}

const forumEnUS = {
  admin: {
    title: 'Admin Console',
    subtitle: 'Manage users, topics, and forum content visibility.',
    tabs: {
      users: 'Users',
      topics: 'Topics',
      content: 'Content'
    },
    contentTypes: {
      questions: 'Questions',
      answers: 'Answers',
      comments: 'Comments'
    },
    searchUser: 'Search username or email',
    user: 'User',
    email: 'Email',
    role: 'Role',
    roleAdmin: 'Admin',
    roleUser: 'User',
    status: 'Status',
    content: 'Content',
    action: 'Action',
    contentStats: 'Plans {plans} / Questions {questions} / Answers {answers}',
    enable: 'Enable',
    disable: 'Disable',
    resetPassword: 'Reset Password',
    resetPasswordFor: 'Set a new password for {username}',
    newPassword: 'New password',
    newPasswordPlaceholder: 'Enter at least 6 characters',
    passwordHint: 'After reset, this user must log in with the new password.',
    confirmResetPassword: 'Confirm Reset',
    passwordResetSuccess: 'Password reset successfully',
    passwordResetFailed: 'Failed to reset password',
    noUsers: 'No users',
    topicName: 'Topic name',
    topicDescription: 'Topic description',
    saveTopic: 'Save Topic',
    addTopic: 'Add Topic',
    topic: 'Topic',
    description: 'Description',
    data: 'Data',
    topicStats: 'Questions {questions} / Follows {follows}',
    restore: 'Restore',
    hide: 'Hide',
    noTopics: 'No topics',
    author: 'Author',
    noContent: 'No content',
    statusActive: 'Active',
    statusDisabled: 'Disabled',
    statusHidden: 'Hidden',
    statusDeleted: 'Deleted',
    questionWithId: 'Question {id}',
    loadUsersFailed: 'Failed to load users',
    loadTopicsFailed: 'Failed to load topics',
    loadContentFailed: 'Failed to load content',
    userStatusUpdated: 'User status updated',
    topicUpdated: 'Topic updated',
    topicCreated: 'Topic created',
    topicStatusUpdated: 'Topic status updated',
    topicDeleted: 'Topic deleted',
    deleteTopicFailed: 'Failed to delete topic',
    deleteTopicTitle: 'Delete Topic',
    deleteTopicConfirm: 'Delete topic "{name}"?',
    deleteTopicHint: 'This removes the topic plus related follows and question links. Questions themselves are not deleted.',
    contentStatusUpdated: 'Content status updated',
    contentDeleted: 'Content deleted'
  },
  forum: {
    title: 'Forum',
    home: 'Home',
    hot: 'Hot',
    recommend: 'Recommended',
    following: 'Following',
    favorite: 'Favorites',
    myContent: 'My Content',
    search: 'Search',
    noResults: 'No results',
    loading: 'Loading...',
    error: 'Failed to load',
    retry: 'Retry',
    relatedQuestions: 'Related Questions',
    relatedQuestionsEmpty: 'No related questions',
    question: {
      title: 'Title',
      content: 'Content',
      topics: 'Topics',
      anonymous: 'Anonymous',
      submit: 'Post',
      edit: 'Edit',
      delete: 'Delete',
      confirmDelete: 'Are you sure you want to delete this question?',
      noQuestions: 'No questions yet',
      views: 'Views',
      answers: 'Answers',
      favorites: 'Favorites',
      votes: 'Votes',
      follows: 'Follows',
      createdAt: 'Posted',
      author: 'Author',
      anonymousUser: 'Anonymous User'
    },
    answer: {
      title: 'Answer',
      content: 'Answer content',
      submit: 'Post Answer',
      edit: 'Edit',
      delete: 'Delete',
      confirmDelete: 'Are you sure you want to delete this answer?',
      noAnswers: 'No answers yet. Be the first to answer!',
      count: '{count} answers',
      sortDefault: 'Default',
      sortTime: 'Time',
      sortVote: 'Votes',
      votes: 'Votes',
      comments: 'Comments',
      collect: 'Collect',
      reply: 'Reply',
      createdAt: 'Posted'
    },
    comment: {
      title: 'Comment',
      content: 'Content',
      submit: 'Send',
      placeholder: 'Write your comment...',
      noComments: 'No comments yet',
      reply: 'Reply',
      votes: 'Votes',
      createdAt: 'Posted'
    },
    topic: {
      title: 'Topic',
      follow: 'Follow',
      followed: 'Following',
      followers: 'Followers',
      questions: 'Questions',
      noQuestions: 'No questions under this topic',
      description: 'Description'
    },
    topicPage: {
      followers: 'Followers',
      questions: 'Questions',
      follow: 'Follow',
      followed: 'Following',
      relatedQuestions: 'Related Questions',
      noQuestions: 'No questions under this topic',
      topAnswerers: 'Top Answerers',
      topAnswers: 'Hot Discussions',
      noData: 'No data',
      answers: 'Answers',
      votes: 'Likes',
      comments: 'Comments'
    },
    userProfile: {
      questions: 'Questions',
      answers: 'Answers',
      votes: 'Likes',
      collections: 'Favorites',
      following: 'Following',
      followers: 'Followers',
      follow: 'Follow',
      followed: 'Following',
      registeredAt: 'Registered',
      noQuestions: 'No questions',
      noAnswers: 'No answers',
      noCollections: 'No favorites',
      noFollowing: 'No following',
      noFollowers: 'No followers'
    },
    user: {
      profile: 'Profile',
      questions: 'Questions',
      answers: 'Answers',
      collections: 'Collections',
      following: 'Following',
      followers: 'Followers',
      follow: 'Follow',
      followed: 'Following',
      unfollow: 'Unfollow',
      registerTime: 'Registered',
      statistics: 'Statistics',
      posts: 'Posts',
      comments: 'Comments',
      likes: 'Likes',
      favorites: 'Favorites'
    },
    questionCard: {
      anonymousUser: 'Anonymous User',
      answers: 'Answers',
      views: 'Views',
      follows: 'Follows',
      favorites: 'Favorites',
      favorite: 'Favorite'
    },
    actions: {
      favorite: 'Favorite',
      unfavorite: 'Unfavorite',
      vote: 'Vote',
      unvote: 'Unvote',
      follow: 'Follow',
      unfollow: 'Unfollow',
      collect: 'Collect',
      uncollect: 'Uncollect'
    },
    search: {
      placeholder: 'Search questions, users, topics...',
      search: 'Search',
      all: 'All',
      question: 'Questions',
      user: 'Users',
      topic: 'Topics',
      relevance: 'Relevance',
      time: 'Time',
      enterKeyword: 'Please enter keyword to search',
      questions: 'Questions ({count})',
      users: 'Users ({count})',
      topics: 'Topics ({count})',
      noResults: 'No results found'
    },
    myContent: {
      title: 'My Content',
      myQuestions: 'My Questions',
      myAnswers: 'My Answers',
      myCollections: 'My Favorites',
      myFollowing: 'My Following',
      myFollowers: 'My Followers',
      noQuestions: 'No questions',
      noAnswers: 'No answers',
      noCollections: 'No favorites',
      noFollowing: 'No following',
      noFollowers: 'No followers',
      goToAsk: 'Ask Question',
      answers: 'Answers',
      views: 'Views',
      favorites: 'Favorites',
      votes: 'Votes',
      comments: 'Comments',
      collected: 'Collected',
      notCollected: 'Not Collected',
      viewDetail: 'View Detail',
      delete: 'Delete',
      learningProgress: 'Learning Progress',
      noTarget: 'No target description'
    }
  },
  dashboard: {
    studyStats: 'Study Statistics'
  }
}

function deepMerge(target, source) {
  Object.entries(source).forEach(([key, value]) => {
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      if (!target[key] || typeof target[key] !== 'object') {
        target[key] = {}
      }
      deepMerge(target[key], value)
      return
    }
    target[key] = value
  })
  return target
}

const topicSuggestionZhCN = {
  admin: {
    topicSuggestions: '话题建议',
    topicSuggestionsHint: '普通用户提交的新话题建议，通过后会进入正式话题库。',
    suggester: '建议人',
    approve: '通过',
    reject: '拒绝',
    noTopicSuggestions: '暂无话题建议',
    loadTopicSuggestionsFailed: '加载话题建议失败',
    topicSuggestionApproved: '话题建议已通过',
    topicSuggestionRejected: '话题建议已拒绝',
    reviewTopicSuggestionFailed: '审核话题建议失败',
    rejectSuggestionTitle: '拒绝话题建议',
    rejectSuggestionConfirm: '确定要拒绝并删除话题建议「{name}」吗？',
    rejectSuggestionHint: '拒绝后该话题建议将从列表中移除，建议人不会收到通知。',
    confirmReject: '确认拒绝',
    statusPending: '待审核',
    statusApproved: '已通过',
    statusRejected: '已拒绝'
  },
  forum: {
    askQuestion: {
      topicSuggestionPlaceholder: '没有合适的话题？输入名称提交建议',
      topicSuggestionHint: '普通用户只能选择已有话题；新话题需要管理员审核通过后才能使用。',
      suggestTopic: '建议话题',
      topicSuggestionSubmitted: '话题建议已提交，等待管理员审核',
      topicSuggestionFailed: '提交话题建议失败'
    }
  }
}

const topicSuggestionEnUS = {
  admin: {
    topicSuggestions: 'Topic Suggestions',
    topicSuggestionsHint: 'Review topics suggested by regular users. Approved suggestions become official topics.',
    suggester: 'Suggested By',
    approve: 'Approve',
    reject: 'Reject',
    noTopicSuggestions: 'No topic suggestions',
    loadTopicSuggestionsFailed: 'Failed to load topic suggestions',
    topicSuggestionApproved: 'Topic suggestion approved',
    topicSuggestionRejected: 'Topic suggestion rejected',
    reviewTopicSuggestionFailed: 'Failed to review topic suggestion',
    rejectSuggestionTitle: 'Reject Topic Suggestion',
    rejectSuggestionConfirm: 'Reject and remove topic suggestion "{name}"?',
    rejectSuggestionHint: 'Once rejected, the suggestion will be removed from the list. The suggester will not be notified.',
    confirmReject: 'Confirm Reject',
    statusPending: 'Pending',
    statusApproved: 'Approved',
    statusRejected: 'Rejected'
  },
  forum: {
    askQuestion: {
      topicSuggestionPlaceholder: 'No matching topic? Suggest a new one',
      topicSuggestionHint: 'Regular users can only select existing topics. New topics must be approved by an admin first.',
      suggestTopic: 'Suggest Topic',
      topicSuggestionSubmitted: 'Topic suggestion submitted for admin review',
      topicSuggestionFailed: 'Failed to submit topic suggestion'
    }
  }
}

const deleteSuggestionZhCN = {
  admin: {
    topicSuggestionDeleted: '话题建议已删除',
    deleteTopicSuggestionFailed: '删除话题建议失败',
    deleteSuggestionTitle: '删除话题建议',
    deleteSuggestionConfirm: '确定要从数据库删除话题建议「{name}」吗？',
    deleteSuggestionHint: '删除后这条建议会从数据库中移除，刷新后也不会再出现。',
    rejectSuggestionConfirm: '确定要拒绝话题建议「{name}」吗？',
    rejectSuggestionHint: '拒绝后这条建议会保留为已拒绝状态；需要彻底移除时请使用删除。'
  }
}

const deleteSuggestionEnUS = {
  admin: {
    topicSuggestionDeleted: 'Topic suggestion deleted',
    deleteTopicSuggestionFailed: 'Failed to delete topic suggestion',
    deleteSuggestionTitle: 'Delete Topic Suggestion',
    deleteSuggestionConfirm: 'Delete topic suggestion "{name}" from the database?',
    deleteSuggestionHint: 'After deletion, this suggestion is removed from the database and will not reappear after refresh.',
    rejectSuggestionConfirm: 'Reject topic suggestion "{name}"?',
    rejectSuggestionHint: 'After rejection, this suggestion stays marked as rejected. Use delete to remove it permanently.'
  }
}

const adminContentSearchZhCN = {
  admin: {
    searchQuestion: '搜索问题标题、内容、作者或 ID',
    questionSearchCount: '共 {count} 条问题'
  }
}

const adminContentSearchEnUS = {
  admin: {
    searchQuestion: 'Search question title, content, author, or ID',
    questionSearchCount: '{count} questions'
  }
}

const adminFilterZhCN = {
  admin: {
    filterAll: '全部',
    searchContent: '搜索当前内容、作者或 ID',
    contentSearchCount: '共 {count} 条内容'
  }
}

const adminFilterEnUS = {
  admin: {
    filterAll: 'All',
    searchContent: 'Search current content, author, or ID',
    contentSearchCount: '{count} items'
  }
}

deepMerge(zhCN, forumZhCN)
deepMerge(zhCN, adminZhCNExtra)
deepMerge(zhCN, topicSuggestionZhCN)
deepMerge(zhCN, deleteSuggestionZhCN)
deepMerge(zhCN, adminContentSearchZhCN)
deepMerge(zhCN, adminFilterZhCN)
deepMerge(enUS, forumEnUS)
deepMerge(enUS, topicSuggestionEnUS)
deepMerge(enUS, deleteSuggestionEnUS)
deepMerge(enUS, adminContentSearchEnUS)
deepMerge(enUS, adminFilterEnUS)

const i18n = createI18n({
  legacy: false,
  locale: savedLocale,
  fallbackLocale: 'zh-CN',
  globalInjection: true,
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS
  }
})

export default i18n
