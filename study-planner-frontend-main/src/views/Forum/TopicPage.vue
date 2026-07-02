<template>
  <div class="topic-page">
    <Navbar />
    <div class="container py-4">
      <div class="row">
        <div class="col-lg-8">
          <!-- 话题信息 -->
          <div v-if="loading" class="text-center py-5">
            <div class="spinner-border" role="status">
              <span class="visually-hidden">{{ $t('common.loading') }}</span>
            </div>
          </div>

          <div v-else-if="topic" class="card mb-4">
            <div class="card-body">
              <div class="d-flex justify-content-between align-items-start">
                <div>
                  <h2 class="mb-2">{{ displayTopicName(topic, locale) }}</h2>
                  <p class="text-muted mb-3" v-if="topic.description">
                    {{ displayTopicDescription(topic, locale) }}
                  </p>
                  <div class="d-flex align-items-center text-muted small">
                    <span class="me-3">
                      <i class="bi bi-people"></i>
                      {{ topic.follow_count || 0 }} {{ $t('forum.topic.followers') }}
                    </span>
                    <span>
                      <i class="bi bi-question-circle"></i>
                      {{ topic.question_count || 0 }} {{ $t('forum.topic.questions') }}
                    </span>
                  </div>
                </div>
                <button 
                  class="btn"
                  :class="topic.is_followed ? 'btn-primary' : 'btn-outline-primary'"
                  @click="handleFollow"
                  :disabled="followingTopic"
                  v-if="userStore.isLoggedIn"
                >
                  <i class="bi" :class="topic.is_followed ? 'bi-heart-fill' : 'bi-heart'"></i>
                  {{ topic.is_followed ? $t('forum.topic.followed') : $t('forum.topic.follow') }}
                </button>
              </div>
            </div>
          </div>

          <!-- 问题列表 -->
          <div>
            <h5 class="mb-3">{{ $t('forum.relatedQuestions') }}</h5>
            <div v-if="questionsLoading" class="text-center py-3">
              <div class="spinner-border spinner-border-sm" role="status">
                <span class="visually-hidden">{{ $t('common.loading') }}</span>
              </div>
            </div>
            <div v-else-if="questions.length === 0" class="text-center py-5 text-muted">
              {{ $t('forum.topic.noQuestions') }}
            </div>
            <QuestionCard 
              v-for="question in questions" 
              :key="question.id"
              :question="question"
            />
            <ForumPagination
              :pagination="questionsPagination"
              :loading="questionsLoading"
              @change="changeQuestionsPage"
            />
          </div>
        </div>

        <!-- 右侧边栏 -->
        <div class="col-lg-4">
          <div class="sticky-top" style="top: 20px;">
            <div class="card">
              <div class="card-header">
                <h6 class="mb-0">{{ $t('forum.topicPage.topAnswers') }}</h6>
              </div>
              <div class="card-body">
                <div v-if="hotQuestions.length === 0" class="text-muted small">
                  {{ $t('forum.topicPage.noData') }}
                </div>
                <div v-else>
                  <div
                    v-for="question in hotQuestions"
                    :key="question.id"
                    class="hot-question-item"
                  >
                    <router-link
                      :to="`/forum/question/${question.id}`"
                      class="hot-question-title"
                    >
                      {{ question.title }}
                    </router-link>
                    <p v-if="question.content" class="hot-question-desc">
                      {{ truncateContent(question.content) }}
                    </p>
                    <div class="hot-question-meta">
                      <span><i class="bi bi-chat-dots"></i> {{ question.answer_count || 0 }}</span>
                      <span><i class="bi bi-eye"></i> {{ question.view_count || 0 }}</span>
                      <span><i class="bi bi-heart"></i> {{ question.follow_count || 0 }}</span>
                    </div>
                  </div>
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
import { ref, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import Navbar from '../../components/Navbar.vue'
import Footer from '../../components/Footer.vue'
import { topicApi } from '../../api/forum'
import { useUserStore } from '../../stores/user'
import { useForumStore } from '../../stores/forum'
import { showToast } from '../../utils/toast'
import QuestionCard from '../../components/Forum/QuestionCard.vue'
import ForumPagination from '../../components/Forum/ForumPagination.vue'
import { displayTopicName, displayTopicDescription } from '../../utils/forumTopic'

const { t, locale } = useI18n()

const route = useRoute()
const userStore = useUserStore()
const forumStore = useForumStore()

const topic = ref(null)
const loading = ref(false)
const questions = ref([])
const questionsLoading = ref(false)
const questionsPagination = ref({
  page: 1,
  size: 10,
  total: 0,
  totalPages: 0
})
const pageSize = 10
const hotQuestions = ref([])

onMounted(() => {
  loadTopic()
  loadQuestions()
  loadHotQuestions()
})

watch(() => route.params.id, () => {
  loadTopic()
  loadQuestions(1)
  loadHotQuestions()
})

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
    size: Number(data?.size || fallback.size || pageSize),
    total: Number(data?.total || 0),
    totalPages: Number(data?.totalPages || 0)
  }
}

async function loadTopic() {
  const id = route.params.id
  if (!id) return
  
  loading.value = true
  try {
    const response = await topicApi.getTopicDetail(id)
    if (response.code === 200) {
      topic.value = response.data
    } else {
      showToast(response.message || t('errors.unknown'), 'error')
    }
  } catch (error) {
    console.error('获取话题失败:', error)
    showToast(t('errors.unknown'), 'error')
  } finally {
    loading.value = false
  }
}

async function loadQuestions(page = questionsPagination.value.page || 1) {
  const id = route.params.id
  if (!id) return
  
  questionsLoading.value = true
  try {
    const response = await topicApi.getTopicQuestions(id, {
      page,
      size: pageSize
    })
    if (response.code === 200) {
      questions.value = normalizeRecords(response.data)
      questionsPagination.value = normalizePagination(response.data, { page, size: pageSize })
    }
  } catch (error) {
    console.error('获取问题列表失败:', error)
    questions.value = []
    questionsPagination.value = normalizePagination(null, { page, size: pageSize })
  } finally {
    questionsLoading.value = false
  }
}

function changeQuestionsPage(page) {
  loadQuestions(page)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function loadHotQuestions() {
  const id = route.params.id
  if (!id) return

  try {
    const response = await topicApi.getHotTopicQuestions(id)
    if (response.code === 200) {
      hotQuestions.value = normalizeRecords(response.data).slice(0, 3)
    }
  } catch (error) {
    console.error('Failed to load hot questions:', error)
    hotQuestions.value = []
  }
}

function truncateContent(content) {
  if (!content) return ''
  const text = content.replace(/[#*`\[\]()]/g, '').trim()
  return text.length > 80 ? text.substring(0, 80) + '...' : text
}

const followingTopic = ref(false)

async function handleFollow() {
  if (!userStore.isLoggedIn) {
    showToast(t('auth.loginRequired'), 'warning')
    return
  }
  
  if (followingTopic.value || !topic.value) return
  followingTopic.value = true
  try {
    const result = await forumStore.followTopic(topic.value.id)
    // 更新本地状态
    if (topic.value && result) {
      topic.value.is_followed = result.is_followed !== undefined ? result.is_followed : !topic.value.is_followed
      topic.value.follow_count = result.follow_count !== undefined ? result.follow_count : topic.value.follow_count
    }
  } catch (error) {
    console.error('关注话题失败:', error)
    showToast(t('errors.unknown'), 'error')
  } finally {
    followingTopic.value = false
  }
}
</script>

<style scoped>
.topic-page {
  min-height: calc(100vh - 200px);
}

.hot-question-item {
  padding-bottom: 14px;
  margin-bottom: 14px;
  border-bottom: 1px solid rgba(13, 110, 253, 0.12);
}

.hot-question-item:last-child {
  padding-bottom: 0;
  margin-bottom: 0;
  border-bottom: 0;
}

.hot-question-title {
  display: -webkit-box;
  overflow: hidden;
  color: #1f2a44;
  font-weight: 600;
  font-size: 0.9rem;
  line-height: 1.4;
  text-decoration: none;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.hot-question-title:hover {
  color: #0d6efd;
}

.hot-question-desc {
  overflow: hidden;
  margin: 4px 0 0;
  color: #6b7a99;
  font-size: 0.82rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-question-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 6px;
  color: #8c9cb0;
  font-size: 0.8rem;
}
</style>
