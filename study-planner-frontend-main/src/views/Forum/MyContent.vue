<template>
  <div class="my-content">
    <Navbar />
    <div class="container py-4">
      <div class="row">
        <div class="col-lg-10">
          <h3 class="mb-4">{{ $t('forum.myContent.title') }}</h3>

          <!-- 标签页 -->
          <ul class="nav nav-tabs mb-4">
            <li class="nav-item">
              <button 
                class="nav-link"
                :class="{ active: activeTab === 'questions' }"
                @click="switchTab('questions')"
              >
                {{ $t('forum.myContent.myQuestions') }}
              </button>
            </li>
            <li class="nav-item">
              <button 
                class="nav-link"
                :class="{ active: activeTab === 'answers' }"
                @click="switchTab('answers')"
              >
                {{ $t('forum.myContent.myAnswers') }}
              </button>
            </li>
            <li class="nav-item">
              <button 
                class="nav-link"
                :class="{ active: activeTab === 'collections' }"
                @click="switchTab('collections')"
              >
                {{ $t('forum.myContent.myCollections') }}
              </button>
            </li>
            <li class="nav-item">
              <button 
                class="nav-link"
                :class="{ active: activeTab === 'following' }"
                @click="switchTab('following')"
              >
                {{ $t('forum.myContent.myFollowing') }}
              </button>
            </li>
            <li class="nav-item">
              <button 
                class="nav-link"
                :class="{ active: activeTab === 'followers' }"
                @click="switchTab('followers')"
              >
                {{ $t('forum.myContent.myFollowers') }}
              </button>
            </li>
          </ul>

          <!-- 内容列表 -->
          <div v-if="loading" class="text-center py-5">
            <div class="spinner-border" role="status">
              <span class="visually-hidden">{{ $t('common.loading') }}</span>
            </div>
          </div>

          <!-- 我的提问 -->
          <div v-else-if="activeTab === 'questions'">
            <div v-if="myQuestionsList.length === 0" class="text-center py-5 text-muted">
              {{ $t('forum.myContent.noQuestions') }}
              <div class="mt-3">
                <router-link to="/forum/ask" class="btn btn-primary">{{ $t('forum.myContent.goToAsk') }}</router-link>
              </div>
            </div>
            <div v-else>
              <div 
                v-for="question in myQuestionsList" 
                :key="question.id"
                class="card mb-3"
              >
                <div class="card-body">
                  <div class="d-flex justify-content-between align-items-start">
                    <div class="flex-grow-1">
                      <h5 class="card-title mb-2">
                        <router-link :to="`/forum/question/${question.id}`" class="text-decoration-none">
                          {{ question.title || $t('forum.questionCard.untitled') }}
                        </router-link>
                      </h5>
                      <p class="card-text text-muted mb-2" v-if="question.content">
                        {{ truncateContent(question.content) }}
                      </p>
                      <div class="d-flex align-items-center text-muted small">
                        <span class="me-3">
                          <i class="bi bi-clock"></i>
                          {{ formatTime(question.created_at) }}
                        </span>
                        <span class="me-3">
                          <i class="bi bi-chat-dots"></i>
                          {{ question.answer_count || 0 }} {{ $t('forum.myContent.answers') }}
                        </span>
                        <span>
                          <i class="bi bi-eye"></i>
                          {{ question.view_count || 0 }} {{ $t('forum.myContent.views') }}
                        </span>
                      </div>
                    </div>
                    <div class="ms-3">
                      <button 
                        class="btn btn-sm btn-outline-danger"
                        @click="deleteQuestion(question.id)"
                      >
                        <i class="bi bi-trash"></i> {{ $t('forum.myContent.delete') }}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <ForumPagination
              :pagination="forumStore.myQuestionsPagination"
              :loading="loading"
              @change="changeMyQuestionsPage"
            />
          </div>

          <!-- 我的回答 -->
          <div v-else-if="activeTab === 'answers'">
            <div v-if="forumStore.myAnswers.length === 0" class="text-center py-5 text-muted">
              {{ $t('forum.myContent.noAnswers') }}
            </div>
            <div v-else>
              <div 
                v-for="answer in forumStore.myAnswers" 
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
                  <div class="d-flex align-items-center justify-content-between">
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
                    <div>
                      <button 
                        v-if="answer.author_id === userStore.user?.id"
                        class="btn btn-sm btn-outline-primary me-2"
                        @click="editAnswer(answer)"
                      >
                        {{ $t('common.edit') }}
                      </button>
                      <button 
                        v-if="answer.author_id === userStore.user?.id"
                        class="btn btn-sm btn-outline-danger"
                        @click="deleteAnswer(answer.id)"
                      >
                        {{ $t('forum.myContent.delete') }}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 我的收藏 -->
          <div v-else-if="activeTab === 'collections'">
            <div v-if="forumStore.myCollections.length === 0" class="text-center py-5 text-muted">
              {{ $t('forum.myContent.noCollections') }}
            </div>
            <div v-else>
              <div 
                v-for="item in forumStore.myCollections" 
                :key="item.id"
                class="card mb-3"
              >
                <div class="card-body">
                  <div class="d-flex justify-content-between align-items-start">
                    <div class="flex-grow-1">
                      <router-link 
                        :to="`/forum/question/${item.target_id}`"
                        class="text-decoration-none fw-bold"
                      >
                        {{ item.type === 'answer' ? '回答：' : '' }}{{ item.target?.title || $t('common.delete') }}
                      </router-link>
                      <div v-if="item.answer_content" class="text-muted small mt-1">
                        {{ truncateContent(item.answer_content) }}
                      </div>
                      <div class="text-muted small mt-1">
                        <i class="bi bi-clock"></i>
                        {{ formatTime(item.created_at) }}
                      </div>
                    </div>
                    <button 
                      class="btn btn-sm btn-outline-danger"
                      @click="removeCollection(item.id)"
                    >
                      {{ $t('forum.actions.unfavorite') }}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 我的关注 -->
          <div v-else-if="activeTab === 'following'">
            <div v-if="!following.users?.length && !following.questions?.length && !following.topics?.length" class="text-center py-5 text-muted">
              {{ $t('forum.myContent.noFollowing') }}
            </div>
            <div v-else>
              <!-- 关注的用户 -->
              <div class="card mb-3">
                <div class="card-header">
                  <h6 class="mb-0">{{ $t('forum.user.following') }}</h6>
                </div>
                <div class="card-body">
                  <div v-if="!following.users || following.users.length === 0" class="text-muted small">
                    {{ $t('forum.myContent.noFollowing') }}
                  </div>
                  <div v-else>
                    <div 
                      v-for="followUser in following.users" 
                      :key="followUser.id"
                      class="card mb-2"
                    >
                      <div class="card-body d-flex align-items-center">
                        <img 
                          v-if="followUser.avatar" 
                          :src="followUser.avatar" 
                          class="rounded-circle me-3"
                          style="width: 48px; height: 48px; object-fit: cover;"
                          alt="avatar"
                        >
                        <i v-else class="bi bi-person-circle me-3" style="font-size: 48px; color: #dee2e6;"></i>
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
                </div>
              </div>
              
              <!-- 关注的问题 -->
              <div class="card mb-3">
                <div class="card-header">
                  <h6 class="mb-0">{{ $t('forum.user.questions') }}</h6>
                </div>
                <div class="card-body">
                  <div v-if="!following.questions || following.questions.length === 0" class="text-muted small">
                    {{ $t('forum.myContent.noFollowing') }}
                  </div>
                  <QuestionCard 
                    v-for="question in following.questions" 
                    :key="question.id"
                    :question="question"
                  />
                  <ForumPagination
                    :pagination="followingQuestionsPagination"
                    :loading="loading"
                    @change="changeFollowingQuestionsPage"
                  />
                </div>
              </div>
              
              <!-- 关注的话题 -->
              <div class="card mb-3">
                <div class="card-header">
                  <h6 class="mb-0">{{ $t('forum.topic.title') }}</h6>
                </div>
                <div class="card-body">
                  <div v-if="!following.topics || following.topics.length === 0" class="text-muted small">
                    {{ $t('forum.myContent.noFollowing') }}
                  </div>
                  <div v-else>
                    <div 
                      v-for="topic in following.topics" 
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
            </div>
          </div>
          
          <!-- 我的粉丝 -->
          <div v-else-if="activeTab === 'followers'">
            <div v-if="followers.length === 0" class="text-center py-5 text-muted">
              {{ $t('forum.myContent.noFollowers') }}
            </div>
            <div v-else>
              <div 
                v-for="follower in followers" 
                :key="follower.id"
                class="card mb-3"
              >
                <div class="card-body d-flex align-items-center">
                  <img 
                    v-if="follower.avatar" 
                    :src="follower.avatar" 
                    class="rounded-circle me-3"
                    style="width: 48px; height: 48px; object-fit: cover;"
                    alt="avatar"
                  >
                  <i v-else class="bi bi-person-circle me-3" style="font-size: 48px; color: #dee2e6;"></i>
                  <div class="flex-grow-1">
                    <router-link 
                      :to="`/forum/user/${follower.id}`"
                      class="text-decoration-none fw-bold"
                    >
                      {{ follower.username }}
                    </router-link>
                    <div class="text-muted small" v-if="follower.created_at">
                      {{ $t('forum.user.registerTime') }} {{ formatTime(follower.created_at) }}
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
import { computed, ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import Navbar from '../../components/Navbar.vue'
import Footer from '../../components/Footer.vue'
import { renderMarkdown } from '../../utils/markdown'
import { useForumStore } from '../../stores/forum'
import { useUserStore } from '../../stores/user'
import { interactionApi, answerApi, questionApi } from '../../api/forum'
import { formatTime } from '../../utils/format'
import { showToast } from '../../utils/toast'
import QuestionCard from '../../components/Forum/QuestionCard.vue'
import ForumPagination from '../../components/Forum/ForumPagination.vue'
import { displayTopicName } from '../../utils/forumTopic'

const { t, locale } = useI18n()
const forumStore = useForumStore()
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

const allowedTabs = ['questions', 'answers', 'collections', 'following', 'followers']
const activeTab = ref(resolveTab(route.query.tab))
const loading = ref(false)
const pageSize = 10
const following = ref({
  questions: [],
  topics: [],
  users: []
})
const followingQuestionsPagination = ref({
  page: 1,
  size: 10,
  total: 0,
  totalPages: 0
})
const followers = ref([])
const myQuestionsList = computed(() => normalizeRecords(forumStore.myQuestions))

onMounted(() => {
  loadContent()
})

watch(
  () => route.query.tab,
  (tab) => {
    const nextTab = resolveTab(tab)
    if (nextTab === activeTab.value) return
    activeTab.value = nextTab
    loadContent(1)
  }
)

function resolveTab(tab) {
  return allowedTabs.includes(tab) ? tab : 'questions'
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

async function loadContent(page = 1) {
  loading.value = true
  try {
    if (activeTab.value === 'questions') {
      console.log('加载我的提问，当前用户:', userStore.user)
      await forumStore.fetchMyQuestions({ page, size: pageSize })
      console.log('我的提问加载完成，数量:', forumStore.myQuestions.length)
    } else if (activeTab.value === 'answers') {
      await forumStore.fetchMyAnswers()
    } else if (activeTab.value === 'collections') {
      await forumStore.fetchMyCollections()
    } else if (activeTab.value === 'following') {
      await loadFollowing(page)
    } else if (activeTab.value === 'followers') {
      await loadFollowers()
    }
  } catch (error) {
    console.error('加载内容失败:', error)
  } finally {
    loading.value = false
  }
}

async function loadFollowing(page = 1) {
  try {
    const response = await interactionApi.getMyFollowing({ page, size: pageSize })
    if (response.code === 200) {
      const data = response.data || { questions: [], topics: [], users: [] }
      following.value = {
        ...data,
        questions: normalizeRecords(data.questions)
      }
      followingQuestionsPagination.value = normalizePagination(data.questions, { page, size: pageSize })
    }
  } catch (error) {
    console.error('获取关注列表失败:', error)
    following.value = { questions: [], topics: [], users: [] }
    followingQuestionsPagination.value = normalizePagination(null, { page, size: pageSize })
  }
}

async function loadFollowers() {
  try {
    const response = await interactionApi.getMyFollowers()
    if (response.code === 200) {
      followers.value = response.data || []
    }
  } catch (error) {
    console.error('获取粉丝列表失败:', error)
    followers.value = []
  }
}

function switchTab(tab) {
  if (!allowedTabs.includes(tab)) return
  if (activeTab.value === tab) {
    loadContent(1)
    return
  }
  router.push({
    name: 'my-content',
    query: {
      ...route.query,
      tab
    }
  })
}

function changeMyQuestionsPage(page) {
  loadContent(page)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function changeFollowingQuestionsPage(page) {
  loadFollowing(page)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function truncateContent(content) {
  if (!content) return ''
  const text = content.replace(/[#*`\[\]()]/g, '').trim()
  return text.length > 200 ? text.substring(0, 200) + '...' : text
}

function editAnswer(answer) {
  // 跳转到问题详情页，可以在这里实现编辑功能
  window.location.href = `/forum/question/${answer.question_id}`
}

async function deleteAnswer(answerId) {
  if (!confirm(t('forum.answer.confirmDelete'))) return
  
  try {
    const response = await answerApi.deleteAnswer(answerId)
    if (response.code === 200) {
      showToast(t('common.success'), 'success')
      await forumStore.fetchMyAnswers()
    } else {
      showToast(response.message || t('errors.unknown'), 'error')
    }
  } catch (error) {
    console.error('删除失败:', error)
    showToast(t('errors.unknown'), 'error')
  }
}

async function removeCollection(collectionId) {
  // 这里需要调用取消收藏的API
  showToast(t('common.info'), 'info')
}

async function deleteQuestion(questionId) {
  if (!confirm(t('forum.question.confirmDelete'))) return
  
  try {
    const response = await questionApi.deleteQuestion(questionId)
    if (response.code === 200) {
      showToast(t('common.success'), 'success')
      await forumStore.fetchMyQuestions({
        page: forumStore.myQuestionsPagination.page,
        size: pageSize
      })
    } else {
      showToast(response.message || t('errors.unknown'), 'error')
    }
  } catch (error) {
    console.error('删除失败:', error)
    showToast(t('errors.unknown'), 'error')
  }
}
</script>

<style scoped>
.my-content {
  min-height: calc(100vh - 200px);
}

.nav-tabs .nav-link {
  cursor: pointer;
  border: 0;
  border-radius: 8px;
  color: var(--sp-muted);
}

.nav-tabs .nav-link.active {
  background: rgba(62, 141, 247, 0.12);
  color: #2f6fc5;
  box-shadow: inset 0 0 0 1px rgba(62, 141, 247, 0.18);
}
</style>
