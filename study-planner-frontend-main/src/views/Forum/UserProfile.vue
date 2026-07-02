<template>
  <div class="user-profile">
    <Navbar />
    <div class="container py-4">
      <div class="row">
        <div class="col-lg-8">
          <!-- 用户信息卡片 -->
          <div v-if="loading" class="text-center py-5">
            <div class="spinner-border" role="status">
              <span class="visually-hidden">{{ $t('common.loading') }}</span>
            </div>
          </div>

          <div v-else-if="user" class="card mb-4">
            <div class="card-body">
              <div class="d-flex align-items-start">
                <div class="me-4">
                  <img 
                    v-if="user.avatar" 
                    :src="user.avatar" 
                    class="rounded-circle"
                    style="width: 120px; height: 120px; object-fit: cover; border: 3px solid #dee2e6;"
                    alt="avatar"
                  >
                  <i v-else class="bi bi-person-circle d-block" style="font-size: 120px; color: #dee2e6;"></i>
                </div>
                <div class="flex-grow-1">
                  <h2 class="mb-2">{{ user.username }}</h2>
                  <p class="text-muted mb-3" v-if="user.bio">
                    {{ user.bio }}
                  </p>
                  <div class="d-flex align-items-center gap-4 mb-3">
                    <div class="text-center" style="cursor: pointer;" @click="switchTab('questions')">
                      <div class="fw-bold fs-5">{{ user.question_count || 0 }}</div>
                      <div class="text-muted small">{{ $t('forum.userProfile.questions') }}</div>
                    </div>
                    <div class="text-center" style="cursor: pointer;" @click="switchTab('answers')">
                      <div class="fw-bold fs-5">{{ user.answer_count || 0 }}</div>
                      <div class="text-muted small">{{ $t('forum.userProfile.answers') }}</div>
                    </div>
                    <div class="text-center">
                      <div class="fw-bold fs-5">{{ user.vote_count || 0 }}</div>
                      <div class="text-muted small">{{ $t('forum.userProfile.votes') }}</div>
                    </div>
                    <div class="text-center" style="cursor: pointer;" @click="switchTab('collections')">
                      <div class="fw-bold fs-5">{{ user.favorite_count || 0 }}</div>
                      <div class="text-muted small">{{ $t('forum.userProfile.collections') }}</div>
                    </div>
                    <div class="text-center" style="cursor: pointer;" @click="switchTab('following')">
                      <div class="fw-bold fs-5">{{ user.following_count || 0 }}</div>
                      <div class="text-muted small">{{ $t('forum.userProfile.following') }}</div>
                    </div>
                    <div class="text-center" style="cursor: pointer;" @click="switchTab('followers')">
                      <div class="fw-bold fs-5">{{ user.follower_count || 0 }}</div>
                      <div class="text-muted small">{{ $t('forum.userProfile.followers') }}</div>
                    </div>
                  </div>
                  <div class="d-flex align-items-center gap-2">
                    <button 
                      v-if="userStore.isLoggedIn && userStore.user?.id !== user.id"
                      class="btn"
                      :class="user.is_following ? 'btn-primary' : 'btn-outline-primary'"
                      @click="handleFollow"
                      :disabled="following"
                    >
                      <i class="bi" :class="user.is_following ? 'bi-person-check-fill' : 'bi-person-plus'"></i>
                      {{ user.is_following ? $t('forum.userProfile.followed') : $t('forum.userProfile.follow') }}
                    </button>
                    <span v-if="user.created_at" class="text-muted small">
                      <i class="bi bi-calendar"></i>
                      {{ $t('forum.user.registerTime') }} {{ formatTime(user.created_at) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 内容标签 -->
          <div class="mb-3">
            <div class="btn-group" role="group">
              <button 
                class="btn btn-outline-secondary"
                :class="{ active: activeTab === 'questions' }"
                @click="switchTab('questions')"
              >
                {{ $t('forum.userProfile.questions') }}
              </button>
              <button 
                class="btn btn-outline-secondary"
                :class="{ active: activeTab === 'answers' }"
                @click="switchTab('answers')"
              >
                {{ $t('forum.userProfile.answers') }}
              </button>
              <button 
                class="btn btn-outline-secondary"
                :class="{ active: activeTab === 'collections' }"
                @click="switchTab('collections')"
              >
                {{ $t('forum.userProfile.collections') }}
              </button>
              <button 
                class="btn btn-outline-secondary"
                :class="{ active: activeTab === 'following' }"
                @click="switchTab('following')"
              >
                {{ $t('forum.userProfile.following') }}
              </button>
              <button 
                class="btn btn-outline-secondary"
                :class="{ active: activeTab === 'followers' }"
                @click="switchTab('followers')"
              >
                {{ $t('forum.userProfile.followers') }}
              </button>
            </div>
          </div>

          <!-- 内容列表 -->
          <div v-if="contentLoading" class="text-center py-3">
            <div class="spinner-border spinner-border-sm" role="status">
              <span class="visually-hidden">{{ $t('common.loading') }}</span>
            </div>
          </div>

          <div v-else-if="activeTab === 'questions'">
            <div v-if="questions.length === 0" class="text-center py-5 text-muted">
              {{ $t('forum.userProfile.noQuestions') }}
            </div>
            <QuestionCard 
              v-for="question in questions" 
              :key="question.id"
              :question="question"
            />
            <ForumPagination
              :pagination="questionsPagination"
              :loading="contentLoading"
              @change="changeQuestionsPage"
            />
          </div>

          <div v-else-if="activeTab === 'answers'">
            <div v-if="answers.length === 0" class="text-center py-5 text-muted">
              {{ $t('forum.userProfile.noAnswers') }}
            </div>
            <div 
              v-for="answer in answers" 
              :key="answer.id"
              class="card mb-3"
            >
              <div class="card-body">
                <div class="mb-2">
                  <router-link 
                    :to="`/forum/question/${answer.question_id}`"
                    class="text-decoration-none fw-bold"
                  >
                    {{ answer.question?.title }}
                  </router-link>
                </div>
                <div class="text-muted small mb-2" v-html="truncateContent(answer.content)"></div>
                <div class="d-flex align-items-center text-muted small">
                  <span class="me-3">
                    <i class="bi bi-hand-thumbs-up"></i>
                    {{ answer.vote_count || 0 }}
                  </span>
                  <span>
                    <i class="bi bi-clock"></i>
                    {{ formatTime(answer.created_at) }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div v-else-if="activeTab === 'collections'">
            <div v-if="collectionQuestions.length === 0" class="text-center py-5 text-muted">
              {{ $t('forum.userProfile.noCollections') }}
            </div>
            <QuestionCard 
              v-for="item in collectionQuestions" 
              :key="item.collection_key"
              :question="item"
            />
          </div>
          
          <div v-else-if="activeTab === 'following'">
            <div v-if="followingList.length === 0" class="text-center py-5 text-muted">
              {{ $t('forum.userProfile.noFollowing') }}
            </div>
            <div 
              v-for="followUser in followingList" 
              :key="followUser.id"
              class="card mb-3"
            >
              <div class="card-body d-flex align-items-center">
                <img 
                  v-if="followUser.avatar" 
                  :src="followUser.avatar" 
                  class="rounded-circle me-3"
                  style="width: 48px; height: 48px;"
                  alt="avatar"
                >
                <div class="flex-grow-1">
                  <router-link 
                    :to="`/forum/user/${followUser.id}`"
                    class="text-decoration-none fw-bold"
                  >
                    {{ followUser.username }}
                  </router-link>
                  <div class="text-muted small" v-if="followUser.created_at">
                    {{ $t('forum.user.registerTime') }} {{ formatTime(followUser.created_at) }}
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <div v-else-if="activeTab === 'followers'">
            <div v-if="followersList.length === 0" class="text-center py-5 text-muted">
              {{ $t('forum.userProfile.noFollowers') }}
            </div>
            <div 
              v-for="follower in followersList" 
              :key="follower.id"
              class="card mb-3"
            >
              <div class="card-body d-flex align-items-center">
                <img 
                  v-if="follower.avatar" 
                  :src="follower.avatar" 
                  class="rounded-circle me-3"
                  style="width: 48px; height: 48px;"
                  alt="avatar"
                >
                <div class="flex-grow-1">
                  <router-link 
                    :to="`/forum/user/${follower.id}`"
                    class="text-decoration-none fw-bold"
                  >
                    {{ follower.username }}
                  </router-link>
                  <div class="text-muted small" v-if="follower.created_at">
                    注册于 {{ formatTime(follower.created_at) }}
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
import { computed, ref, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import Navbar from '../../components/Navbar.vue'
import Footer from '../../components/Footer.vue'
import { renderMarkdown } from '../../utils/markdown'
import { forumUserApi } from '../../api/forum'
import { useUserStore } from '../../stores/user'
import { useForumStore } from '../../stores/forum'
import { formatTime } from '../../utils/format'
import { showToast } from '../../utils/toast'
import QuestionCard from '../../components/Forum/QuestionCard.vue'
import ForumPagination from '../../components/Forum/ForumPagination.vue'

const { t } = useI18n()

const route = useRoute()
const userStore = useUserStore()
const forumStore = useForumStore()

const user = ref(null)
const loading = ref(false)
const activeTab = ref('questions')
const questions = ref([])
const questionsPagination = ref({
  page: 1,
  size: 10,
  total: 0,
  totalPages: 0
})
const pageSize = 10
const answers = ref([])
const collections = ref([])
const collectionQuestions = computed(() => collections.value
  .map(normalizeCollectionQuestion)
  .filter(Boolean))
const followingList = ref([])
const followersList = ref([])
const contentLoading = ref(false)
const following = ref(false)

onMounted(() => {
  loadUser()
  loadContent()
})

watch(() => route.params.id, () => {
  loadUser()
  loadContent(1)
})

watch(activeTab, () => {
  loadContent(1)
})

async function loadUser() {
  const id = route.params.id
  if (!id) return
  
  loading.value = true
  try {
    const response = await forumUserApi.getUserInfo(id)
    if (response.code === 200) {
      user.value = response.data
    } else {
      showToast(response.message || t('errors.unknown'), 'error')
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    showToast(t('errors.unknown'), 'error')
  } finally {
    loading.value = false
  }
}

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

function normalizeAuthor(item) {
  if (item.author) return item.author
  if (item.author_username || item.author_avatar) {
    return {
      id: item.author_id,
      username: item.author_username,
      avatar: item.author_avatar
    }
  }
  return null
}

function normalizeCollectionQuestion(item) {
  if (!item) return null

  const target = item.target || {}
  const isAnswerCollection = item.type === 'answer'
  const questionId = isAnswerCollection
    ? (item.target_id || target.id || item.question_id)
    : (item.target_id || target.id || item.id)

  if (!questionId) return null

  return {
    ...item,
    collection_key: `${item.type || 'question'}-${item.id || questionId}`,
    id: questionId,
    title: item.title || item.target_title || target.title || t('forum.questionCard.untitled'),
    content: isAnswerCollection ? (item.answer_content || item.content || '') : (item.content || ''),
    created_at: item.created_at,
    answer_count: item.answer_count || 0,
    view_count: item.view_count || 0,
    follow_count: item.follow_count || 0,
    topics: item.topics || [],
    author: normalizeAuthor(item)
  }
}

async function loadContent(page = 1) {
  const id = route.params.id
  if (!id) return
  
  contentLoading.value = true
  try {
    if (activeTab.value === 'questions') {
      const response = await forumUserApi.getUserQuestions(id, {
        page,
        size: pageSize
      })
      if (response.code === 200) {
        questions.value = normalizeRecords(response.data)
        questionsPagination.value = normalizePagination(response.data, { page, size: pageSize })
      }
    } else if (activeTab.value === 'answers') {
      const response = await forumUserApi.getUserAnswers(id)
      if (response.code === 200) {
        answers.value = response.data || []
      }
    } else if (activeTab.value === 'collections') {
      const response = await forumUserApi.getUserCollections(id)
      if (response.code === 200) {
        collections.value = response.data || []
      }
    } else if (activeTab.value === 'following') {
      const response = await forumUserApi.getFollowing(id)
      if (response.code === 200) {
        followingList.value = response.data || []
      }
    } else if (activeTab.value === 'followers') {
      const response = await forumUserApi.getFollowers(id)
      if (response.code === 200) {
        followersList.value = response.data || []
      }
    }
  } catch (error) {
    console.error('获取内容失败:', error)
    if (activeTab.value === 'questions') {
      questions.value = []
      questionsPagination.value = normalizePagination(null, { page, size: pageSize })
    }
  } finally {
    contentLoading.value = false
  }
}

function switchTab(tab) {
  activeTab.value = tab
}

function changeQuestionsPage(page) {
  loadContent(page)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function handleFollow() {
  if (!userStore.isLoggedIn) {
    showToast(t('auth.loginRequired'), 'warning')
    return
  }
  
  if (following.value) return
  following.value = true
  try {
    const result = await forumStore.followUser(user.value.id)
    // 更新本地状态
    if (user.value && result) {
      user.value.is_following = result.is_following
      user.value.follower_count = result.follower_count !== undefined ? result.follower_count : user.value.follower_count
      user.value.following_count = result.following_count !== undefined ? result.following_count : user.value.following_count
    }
  } finally {
    following.value = false
  }
}

function truncateContent(content) {
  if (!content) return ''
  const text = content.replace(/[#*`\[\]()]/g, '').trim()
  const truncated = text.length > 200 ? text.substring(0, 200) + '...' : text
  return renderMarkdown(truncated)
}
</script>

<style scoped>
.user-profile {
  min-height: calc(100vh - 200px);
}

.btn-group .btn.active {
  background-color: #3e8df7;
  border-color: #3e8df7;
  color: white;
}
</style>
