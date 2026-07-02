<template>
  <div class="search-page">
    <Navbar />
    <div class="container py-4">
      <div class="row">
        <div class="col-lg-8">
          <!-- 搜索框 -->
          <div class="card mb-4">
            <div class="card-body">
              <div class="input-group">
                <input 
                  type="text" 
                  class="form-control"
                  v-model="keyword"
                  :placeholder="$t('forum.search.placeholder')"
                  @keyup.enter="performSearch(1)"
                >
                <button 
                  class="btn btn-primary"
                  @click="performSearch(1)"
                >
                  <i class="bi bi-search"></i> {{ $t('forum.search.search') }}
                </button>
              </div>
            </div>
          </div>

          <!-- 筛选条件 -->
          <div class="d-flex align-items-center mb-3 gap-3">
            <div class="btn-group" role="group">
              <button 
                class="btn btn-sm btn-outline-secondary"
                :class="{ active: searchType === 'all' }"
                @click="changeType('all')"
              >
                {{ $t('forum.search.all') }}
              </button>
              <button 
                class="btn btn-sm btn-outline-secondary"
                :class="{ active: searchType === 'question' }"
                @click="changeType('question')"
              >
                {{ $t('forum.search.question') }}
              </button>
              <button 
                class="btn btn-sm btn-outline-secondary"
                :class="{ active: searchType === 'user' }"
                @click="changeType('user')"
              >
                {{ $t('forum.search.user') }}
              </button>
              <button 
                class="btn btn-sm btn-outline-secondary"
                :class="{ active: searchType === 'topic' }"
                @click="changeType('topic')"
              >
                {{ $t('forum.search.topic') }}
              </button>
            </div>
            <div class="btn-group" role="group">
              <button 
                class="btn btn-sm btn-outline-secondary"
                :class="{ active: sortBy === 'relevance' }"
                @click="changeSort('relevance')"
              >
                {{ $t('forum.search.relevance') }}
              </button>
              <button 
                class="btn btn-sm btn-outline-secondary"
                :class="{ active: sortBy === 'time' }"
                @click="changeSort('time')"
              >
                {{ $t('forum.search.time') }}
              </button>
            </div>
          </div>

          <!-- 搜索结果 -->
          <div v-if="showInitialLoading" class="text-center py-5">
            <div class="spinner-border" role="status">
              <span class="visually-hidden">{{ $t('common.loading') }}</span>
            </div>
          </div>

          <div v-else-if="!hasSearched" class="text-center py-5 text-muted">
            {{ $t('forum.search.enterKeyword') }}
          </div>

          <div v-else-if="hasResults" class="search-results" :class="{ refreshing: loading }">
            <!-- 问题结果 -->
            <div v-if="(searchType === 'all' || searchType === 'question') && results.questions?.length > 0">
              <h5 class="mb-3">{{ $t('forum.search.questions', { count: resultCount('questions') }) }}</h5>
              <QuestionCard 
                v-for="question in results.questions" 
                :key="question.id"
                :question="question"
              />
            </div>

            <!-- 用户结果 -->
            <div v-if="(searchType === 'all' || searchType === 'user') && results.users?.length > 0" class="mt-4">
              <h5 class="mb-3">{{ $t('forum.search.users', { count: resultCount('users') }) }}</h5>
              <div 
                v-for="user in results.users" 
                :key="user.id"
                class="card mb-3"
              >
                <div class="card-body">
                  <div class="d-flex align-items-center">
                    <img 
                      v-if="user.avatar" 
                      :src="user.avatar" 
                      class="rounded-circle me-3"
                      style="width: 50px; height: 50px;"
                      alt="avatar"
                    >
                    <div class="flex-grow-1">
                      <router-link 
                        :to="`/forum/user/${user.id}`"
                        class="text-decoration-none fw-bold"
                      >
                        {{ user.username }}
                      </router-link>
                      <p class="text-muted small mb-0" v-if="user.bio">
                        {{ user.bio }}
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 话题结果 -->
            <div v-if="(searchType === 'all' || searchType === 'topic') && results.topics?.length > 0" class="mt-4">
              <h5 class="mb-3">{{ $t('forum.search.topics', { count: resultCount('topics') }) }}</h5>
              <div 
                v-for="topic in results.topics" 
                :key="topic.id"
                class="card mb-3"
              >
                <div class="card-body">
                  <router-link 
                    :to="`/forum/topic/${topic.id}`"
                    class="text-decoration-none fw-bold"
                  >
                    {{ displayTopicName(topic, locale) }}
                  </router-link>
                  <p class="text-muted small mb-0" v-if="topic.description">
                    {{ displayTopicDescription(topic, locale) }}
                  </p>
                  <div class="text-muted small mt-2">
                    {{ topic.follow_count || 0 }} {{ $t('forum.topic.followers') }} · {{ topic.question_count || 0 }} {{ $t('forum.topic.questions') }}
                  </div>
                </div>
              </div>
            </div>

            <ForumPagination
              :pagination="activePagination"
              :loading="loading"
              @change="changePage"
            />
          </div>

          <div v-else class="text-center py-5 text-muted">
            {{ $t('forum.search.noResults') }}
          </div>
        </div>
      </div>
    </div>
    <Footer />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import Navbar from '../../components/Navbar.vue'
import Footer from '../../components/Footer.vue'
import { useForumStore } from '../../stores/forum'
import QuestionCard from '../../components/Forum/QuestionCard.vue'
import ForumPagination from '../../components/Forum/ForumPagination.vue'
import { displayTopicName, displayTopicDescription } from '../../utils/forumTopic'

const { t, locale } = useI18n()
const route = useRoute()
const forumStore = useForumStore()

const keyword = ref('')
const searchType = ref('all')
const sortBy = ref('relevance')
const loading = ref(false)
const hasSearched = ref(false)
const hasLoadedSearch = ref(false)
const currentPage = ref(1)
const pageSize = 10

const results = computed(() => forumStore.searchResults || {})
const showInitialLoading = computed(() => loading.value && !hasLoadedSearch.value)
const activePagination = computed(() => {
  const pagination = forumStore.searchPagination || {}
  if (searchType.value === 'question') return pagination.questions || {}
  if (searchType.value === 'user') return pagination.users || {}
  if (searchType.value === 'topic') return pagination.topics || {}
  return [pagination.questions, pagination.users, pagination.topics]
    .filter(Boolean)
    .sort((a, b) => (b.totalPages || 0) - (a.totalPages || 0))[0] || {}
})
const hasResults = computed(() => {
  const r = results.value
  return (r.questions && r.questions.length > 0) ||
         (r.users && r.users.length > 0) ||
         (r.topics && r.topics.length > 0)
})

onMounted(() => {
  // 从路由参数获取搜索关键词
  if (route.query.keyword) {
    keyword.value = route.query.keyword
    performSearch()
  }
})

watch(
  () => route.query.keyword,
  (nextKeyword) => {
    const nextValue = String(nextKeyword || '')
    if (nextValue && nextValue !== keyword.value) {
      keyword.value = nextValue
      performSearch(1)
    }
  }
)

async function changeType(type) {
  if (searchType.value === type) return
  if (hasSearched.value) {
    await performSearch(1, { type })
  } else {
    searchType.value = type
  }
}

async function changeSort(sort) {
  if (sortBy.value === sort) return
  if (hasSearched.value) {
    await performSearch(1, { sort })
  } else {
    sortBy.value = sort
  }
}

function resultCount(type) {
  return forumStore.searchPagination?.[type]?.total || results.value[type]?.length || 0
}

async function performSearch(page = 1, options = {}) {
  if (!keyword.value.trim()) {
    return
  }
  
  const nextType = options.type || searchType.value
  const nextSort = options.sort || sortBy.value
  loading.value = true
  hasSearched.value = true
  currentPage.value = page
  
  try {
    await forumStore.search({
      keyword: keyword.value,
      type: nextType === 'all' ? undefined : nextType,
      sort: nextSort,
      page,
      size: pageSize
    })
    searchType.value = nextType
    sortBy.value = nextSort
    hasLoadedSearch.value = true
  } finally {
    loading.value = false
  }
}

function changePage(page) {
  performSearch(page)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
</script>

<style scoped>
.search-page {
  min-height: calc(100vh - 200px);
}

.btn-group .btn.active {
  background: linear-gradient(135deg, #72c4ff 0%, #3e93f5 100%);
  border-color: #63b8ff;
  color: white;
  box-shadow: 0 0 0 4px rgba(99, 184, 255, 0.18);
}

.search-results {
  transition: opacity 0.15s ease;
}

.search-results.refreshing {
  opacity: 0.82;
}
</style>
