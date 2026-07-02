<template>
  <div class="forum-home">
    <Navbar />
    <div class="container py-4 forum-container">
      <div class="row">
        <!-- 左侧内容区 -->
        <div class="col-lg-8">
          <!-- 顶部导航 -->
          <div class="forum-tabs d-flex align-items-center mb-4 pb-2">
            <button 
              class="btn forum-tab-btn"
              :class="tabButtonClass('all')"
              @click="switchTab('all')"
              :disabled="forumStore.loading && activeTab === 'all'"
            >
              {{ $t('forum.home') }}
            </button>
            <button 
              class="btn forum-tab-btn"
              :class="tabButtonClass('recommend')"
              @click="switchTab('recommend')"
              :disabled="forumStore.loading && activeTab === 'recommend'"
            >
              {{ $t('forum.recommend') }}
            </button>
            <button 
              class="btn forum-tab-btn"
              :class="tabButtonClass('hot')"
              @click="switchTab('hot')"
              :disabled="forumStore.loading && activeTab === 'hot'"
            >
              {{ $t('forum.hot') }}
            </button>
            <form class="forum-search ms-auto" @submit.prevent="submitForumSearch">
              <i class="bi bi-search"></i>
              <input
                v-model.trim="searchKeyword"
                type="search"
                class="form-control"
                :placeholder="$t('forum.search.placeholder')"
                :aria-label="$t('forum.search.search')"
              >
              <button class="btn btn-primary" type="submit" :disabled="!searchKeyword">
                {{ $t('forum.search.search') }}
              </button>
            </form>
            <div>
              <router-link to="/forum/ask" class="btn btn-primary">
                <i class="bi bi-plus-circle"></i> {{ $t('forum.askQuestion.title') }}
              </router-link>
            </div>
          </div>

          <!-- 错误提示 -->
          <div v-if="error" class="alert alert-danger mb-3">
            <strong>{{ $t('forum.error') }}：</strong>{{ error }}
            <button class="btn btn-sm btn-outline-danger ms-2" @click="error = null; loadQuestions(); loadHotTopics()">{{ $t('forum.retry') }}</button>
          </div>
          
          <!-- 问题列表 -->
          <div v-if="forumStore.loading && (!forumStore.questions || forumStore.questions.length === 0)" class="text-center py-5">
            <div class="spinner-border" role="status">
              <span class="visually-hidden">{{ $t('forum.loading') }}</span>
            </div>
          </div>
          
          <div v-else-if="!forumStore.questions || forumStore.questions.length === 0" class="text-center py-5">
            <p class="text-muted">{{ $t('forum.question.noQuestions') }}</p>
            <router-link to="/forum/ask" class="btn btn-primary">{{ $t('forum.askQuestion.title') }}</router-link>
          </div>
          
          <div v-else class="question-list-wrap" :class="{ refreshing: forumStore.loading }">
            <div v-if="forumStore.loading" class="list-loading-bar"></div>
            <QuestionCard
              v-for="question in forumStore.questions"
              :key="question.id"
              :question="question"
            />
          </div>

          <ForumPagination
            :pagination="forumStore.questionsPagination"
            :loading="forumStore.loading"
            @change="changePage"
          />
        </div>

        <!-- 右侧边栏 -->
        <div class="col-lg-4">
          <div class="sticky-top" style="top: 20px; z-index: 100;">
            <!-- 热门话题 -->
            <div class="card mb-3">
              <div class="card-header">
                <h6 class="mb-0">{{ hotTopicsTitle }}</h6>
              </div>
              <div class="card-body">
                <div v-if="hotTopics.length === 0" class="text-muted small">
                  {{ $t('forum.noTopics') }}
                </div>
                <div v-else>
                  <div 
                    v-for="topic in hotTopics" 
                    :key="topic.id"
                    class="mb-2"
                  >
                    <router-link 
                      :to="`/forum/topic/${topic.id}`"
                      class="text-decoration-none"
                    >
                      {{ displayTopicName(topic, locale) }}
                    </router-link>
                    <span class="text-muted small ms-2">
                      {{ topic.question_count || 0 }} {{ $t('forum.topic.questions') }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 学习统计卡片 -->
            <div class="card">
              <div class="card-header">
                <h6 class="mb-0">{{ $t('forum.stats.title') }}</h6>
              </div>
              <div class="card-body">
                <template v-if="isLoggedIn">
                  <router-link
                    :to="{ name: 'my-content', query: { tab: 'questions' } }"
                    class="forum-stat-link d-flex justify-content-between mb-2"
                  >
                    <span><i class="bi bi-question-circle me-1"></i>{{ $t('forum.stats.myQuestions') }}</span>
                    <span class="fw-bold">{{ stats.questionCount }} {{ $t('forum.stats.countUnit') }}</span>
                  </router-link>
                  <router-link
                    :to="{ name: 'my-content', query: { tab: 'answers' } }"
                    class="forum-stat-link d-flex justify-content-between mb-2"
                  >
                    <span><i class="bi bi-chat-left-text me-1"></i>{{ $t('forum.stats.myAnswers') }}</span>
                    <span class="fw-bold">{{ stats.answerCount }} {{ $t('forum.stats.countUnit') }}</span>
                  </router-link>
                  <div class="d-flex justify-content-between">
                    <span><i class="bi bi-hand-thumbs-up me-1"></i>{{ $t('forum.stats.totalVotes') }}</span>
                    <span class="fw-bold">{{ stats.voteCount }} {{ $t('forum.stats.timesUnit') }}</span>
                  </div>
                </template>
                <div v-else class="forum-stats-login text-center">
                  <i class="bi bi-person-lock stats-login-icon"></i>
                  <p class="mb-3">{{ $t('forum.stats.loginPrompt') }}</p>
                  <router-link
                    :to="{ path: '/login', query: { redirect: '/forum' } }"
                    class="btn btn-primary btn-sm"
                  >
                    {{ $t('forum.stats.loginAction') }}
                  </router-link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <Footer />
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onActivated, onErrorCaptured, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import Navbar from '../../components/Navbar.vue'
import Footer from '../../components/Footer.vue'
import { useForumStore } from '../../stores/forum'
import { useUserStore } from '../../stores/user'
import { forumUserApi } from '../../api/forum'
import QuestionCard from '../../components/Forum/QuestionCard.vue'
import ForumPagination from '../../components/Forum/ForumPagination.vue'
import { displayTopicName } from '../../utils/forumTopic'

const { t, locale } = useI18n()
const router = useRouter()
const forumStore = useForumStore()
const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)

const activeTab = ref('all')
const searchKeyword = ref('')
const pageSize = 10
const hotTopics = ref([])
const hotTopicsTitle = computed(() => (
  String(locale.value || '').toLowerCase().startsWith('en') ? 'Hot Topics' : '热门话题'
))
const stats = ref({
  questionCount: 0,
  answerCount: 0,
  voteCount: 0
})
const error = ref(null)

// 捕获组件错误
onErrorCaptured((err, instance, info) => {
  console.error('组件错误:', err, info)
  error.value = err.message
  return false // 阻止错误继续传播
})

onMounted(async () => {
  try {
    console.log('ForumHome 页面加载开始')
    await Promise.all([
      loadQuestions(),
      loadHotTopics(),
      isLoggedIn.value ? loadStats() : Promise.resolve()
    ])
    console.log('ForumHome 页面加载完成')
  } catch (err) {
    console.error('ForumHome 页面初始化失败:', err)
    error.value = err.message
  }
  
  // 监听问题创建事件，自动刷新列表
  window.addEventListener('forum-question-created', () => {
    loadQuestions(1)
    loadHotTopics()
  })
  
  // 监听话题更新事件
  window.addEventListener('topic-updated', () => {
    loadHotTopics()
  })
})

// 当页面激活时刷新（从其他页面返回时，如果使用了keep-alive）
onActivated(() => {
  loadQuestions()
  loadHotTopics()
  if (isLoggedIn.value) {
    loadStats()
  }
})

watch(isLoggedIn, (loggedIn) => {
  if (loggedIn) {
    loadStats()
  } else {
    resetStats()
  }
})


function switchTab(tab) {
  if (activeTab.value === tab && forumStore.loading) return
  activeTab.value = tab
  loadQuestions(1)
}

function tabButtonClass(tab) {
  return {
    active: activeTab.value === tab,
    loading: forumStore.loading && activeTab.value === tab
  }
}

function submitForumSearch() {
  const keyword = searchKeyword.value.trim()
  if (!keyword) return
  router.push({
    name: 'search',
    query: { keyword }
  })
}

async function loadQuestions(page = forumStore.questionsPagination.page || 1) {
  try {
    const params = {
      page,
      size: pageSize
    }
    if (activeTab.value === 'hot') {
      params.sort = 'hot'
    } else if (activeTab.value === 'recommend') {
      params.sort = 'recommend'
    } else if (activeTab.value === 'following') {
      params.following = true
    }
    
    await forumStore.fetchQuestions(params)
  } catch (error) {
    console.error('加载问题列表失败:', error)
  }
}

function changePage(page) {
  loadQuestions(page)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function loadHotTopics() {
  try {
    const topics = await forumStore.fetchHotTopics()
    hotTopics.value = topics || []
  } catch (error) {
    console.error('加载热门话题失败:', error)
    hotTopics.value = []
  }
}

async function loadStats() {
  if (!isLoggedIn.value) {
    resetStats()
    return
  }

  try {
    const response = await forumUserApi.getUserStats()
    if (response.code === 200 && response.data) {
      stats.value = response.data
    }
  } catch (error) {
    console.error('加载学习统计失败:', error)
  }
}

function resetStats() {
  stats.value = {
    questionCount: 0,
    answerCount: 0,
    voteCount: 0
  }
}
</script>

<style scoped>
.forum-home {
  min-height: 100vh;
}

.forum-tabs {
  gap: 0.5rem;
  background: #fff;
  border: 1px solid #e3e9f1;
  border-radius: 8px;
  box-shadow: 0 14px 38px rgba(54, 83, 116, 0.08);
  margin-bottom: 18px !important;
  padding: 10px !important;
}

.forum-tab-btn {
  color: #2d7eca;
  background: #eef8ff;
  border: 1px solid #cfeaff;
  border-radius: 8px;
  min-width: 72px;
  font-weight: 700;
  box-shadow: 0 8px 18px rgba(62, 147, 245, 0.08);
  transition: background 0.15s ease, border-color 0.15s ease, color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.forum-tab-btn:hover {
  color: #1f75c7;
  background: #d8f0ff;
  border-color: #a8dbff;
  box-shadow: 0 10px 22px rgba(62, 147, 245, 0.12);
}

.forum-tab-btn.active {
  color: #fff;
  background: linear-gradient(135deg, #72c4ff 0%, #3e93f5 100%);
  border-color: #63b8ff;
  box-shadow: 0 0 0 4px rgba(99, 184, 255, 0.24), 0 12px 26px rgba(62, 147, 245, 0.2);
}

.forum-tab-btn.loading {
  opacity: 0.75;
}

.forum-search {
  align-items: center;
  background: #f8fbff;
  border: 1px solid #d6eaff;
  border-radius: 8px;
  display: flex;
  gap: 0.45rem;
  min-width: 280px;
  padding: 0.25rem;
}

.forum-search i {
  color: #3e93f5;
  flex: 0 0 auto;
  margin-left: 0.45rem;
}

.forum-search .form-control {
  background: transparent;
  border: 0;
  box-shadow: none;
  min-width: 0;
  padding-inline: 0.25rem;
}

.forum-search .btn {
  border-radius: 7px;
  flex: 0 0 auto;
  font-weight: 700;
  padding-inline: 0.85rem;
}

.question-list-wrap {
  position: relative;
  min-height: 120px;
}

.question-list-wrap.refreshing {
  opacity: 0.88;
  transition: opacity 0.15s ease;
}

.sticky-top .card {
  box-shadow: 0 14px 38px rgba(54, 83, 116, 0.09);
}

.forum-stat-link {
  color: inherit;
  text-decoration: none;
  border-radius: 8px;
  padding: 0.25rem 0.35rem;
  margin-inline: -0.35rem;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.forum-stat-link:hover {
  color: var(--bs-primary);
  background: rgba(var(--bs-primary-rgb), 0.08);
}

.forum-stats-login {
  color: #6b7280;
  padding: 0.45rem 0.25rem 0.2rem;
}

.stats-login-icon {
  color: var(--bs-primary);
  display: inline-block;
  font-size: 1.6rem;
  margin-bottom: 0.4rem;
}

:deep(.question-card) {
  margin-bottom: 14px !important;
}

:deep(.question-card.card) {
  box-shadow: 0 14px 38px rgba(54, 83, 116, 0.09);
}

:deep(.question-card.card:hover) {
  box-shadow: 0 18px 44px rgba(54, 83, 116, 0.12) !important;
}

[data-bs-theme='dark'] .forum-tabs {
  background: #1f2937;
  border-color: #374151;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.22);
}

[data-bs-theme='dark'] .forum-tab-btn {
  color: #d1d5db;
  background: #111827;
  border-color: #374151;
  box-shadow: none;
}

[data-bs-theme='dark'] .forum-tab-btn:hover {
  color: #f9fafb;
  background: #263244;
  border-color: #4b5563;
  box-shadow: none;
}

[data-bs-theme='dark'] .forum-tab-btn.active {
  color: #061b36;
  background: linear-gradient(135deg, #7fd2ff 0%, #55b7ff 100%);
  border-color: #74c7ff;
  box-shadow: 0 0 0 4px rgba(85, 183, 255, 0.24), 0 14px 28px rgba(85, 183, 255, 0.2);
}

[data-bs-theme='dark'] .forum-search {
  background: #111827;
  border-color: #374151;
}

[data-bs-theme='dark'] .forum-search i {
  color: #7fd2ff;
}

[data-bs-theme='dark'] .forum-search .form-control {
  color: #f9fafb;
}

[data-bs-theme='dark'] .forum-search .form-control::placeholder {
  color: #9ca3af;
}

[data-bs-theme='dark'] .sticky-top .card,
[data-bs-theme='dark'] :deep(.question-card.card) {
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.24);
}

@media (max-width: 991.98px) {
  .forum-tabs {
    flex-wrap: wrap;
  }

  .forum-search {
    flex: 1 1 100%;
    margin-left: 0 !important;
    order: 2;
  }
}

@media (max-width: 575.98px) {
  .forum-tab-btn {
    flex: 1 1 0;
  }

  .forum-search {
    min-width: 0;
  }

  .forum-search .btn {
    width: auto;
  }
}

.list-loading-bar {
  position: sticky;
  top: 72px;
  z-index: 5;
  height: 3px;
  margin-bottom: 0.75rem;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(var(--bs-primary-rgb), 0.14);
}

.list-loading-bar::after {
  content: '';
  display: block;
  width: 42%;
  height: 100%;
  border-radius: inherit;
  background: var(--bs-primary);
  animation: list-loading 0.9s ease-in-out infinite;
}

@keyframes list-loading {
  0% {
    transform: translateX(-110%);
  }

  100% {
    transform: translateX(260%);
  }
}
</style>
