<template>
  <div class="admin-page">
    <Navbar />

    <main class="container py-4">
      <div class="admin-header">
        <div>
          <h2>{{ t('admin.title') }}</h2>
          <p>{{ t('admin.subtitle') }}</p>
        </div>
        <button class="btn btn-outline-primary" type="button" :disabled="isRefreshing" @click="reloadCurrentTab">
          <i class="bi bi-arrow-clockwise" :class="{ spinning: isRefreshing }"></i>
          {{ t('common.refresh') }}
        </button>
      </div>

      <div class="admin-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="admin-tab"
          :class="{ active: activeTab === tab.key }"
          type="button"
          @click="switchTab(tab.key)"
        >
          <i :class="tab.icon"></i>
          {{ t(tab.labelKey) }}
        </button>
      </div>

      <section class="admin-panel">
        <div v-if="activeTab === 'users'" class="admin-section">
          <div class="admin-toolbar">
            <div class="input-group search-box">
              <span class="input-group-text"><i class="bi bi-search"></i></span>
              <input v-model="userKeyword" class="form-control" :placeholder="t('admin.searchUser')" @keyup.enter="loadUsers">
            </div>
            <button class="btn btn-primary" type="button" :disabled="isRefreshing" @click="loadUsers">
              {{ t('common.search') }}
            </button>
            <div class="status-filter">
              <button
                v-for="option in userStatusOptions"
                :key="option.value"
                class="btn btn-sm"
                :class="userStatusFilter === option.value ? 'btn-primary' : 'btn-outline-secondary'"
                type="button"
                @click="userStatusFilter = option.value"
              >
                {{ t(option.labelKey) }}
              </button>
            </div>
          </div>

          <div class="table-responsive">
            <table class="table align-middle">
              <thead>
                <tr>
                  <th>{{ t('admin.user') }}</th>
                  <th>{{ t('admin.email') }}</th>
                  <th>{{ t('admin.role') }}</th>
                  <th>{{ t('admin.status') }}</th>
                  <th>{{ t('admin.content') }}</th>
                  <th class="text-end">{{ t('admin.action') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in filteredUsers" :key="item.id">
                  <td>
                    <strong>{{ item.username }}</strong>
                    <small>ID {{ item.id }}</small>
                  </td>
                  <td>{{ item.email || '-' }}</td>
                  <td><span class="badge text-bg-light">{{ roleText(item.role) }}</span></td>
                  <td><StatusBadge :status="item.status" /></td>
                  <td>
                    {{ t('admin.contentStats', {
                      plans: item.plan_count || 0,
                      questions: item.question_count || 0,
                      answers: item.answer_count || 0
                    }) }}
                  </td>
                  <td class="text-end">
                    <button
                      class="btn btn-sm btn-outline-primary me-2"
                      type="button"
                      @click="openResetPasswordDialog(item)"
                    >
                      {{ t('admin.resetPassword') }}
                    </button>
                    <button
                      class="btn btn-sm"
                      :class="item.status === 'disabled' ? 'btn-outline-success' : 'btn-outline-danger'"
                      type="button"
                      :disabled="item.role === 'admin'"
                      @click="toggleUserStatus(item)"
                    >
                      {{ item.status === 'disabled' ? t('admin.enable') : t('admin.disable') }}
                    </button>
                  </td>
                </tr>
                <tr v-if="filteredUsers.length === 0">
                  <td colspan="6" class="empty-cell">{{ t('admin.noUsers') }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div v-if="activeTab === 'topics'" class="admin-section">
          <form class="topic-form" @submit.prevent="saveTopic">
            <input v-model.trim="topicForm.name" class="form-control" :placeholder="t('admin.topicName')" maxlength="50" required>
            <input v-model.trim="topicForm.description" class="form-control" :placeholder="t('admin.topicDescription')" maxlength="255">
            <button class="btn btn-primary" type="submit" :disabled="isRefreshing">
              {{ topicForm.id ? t('admin.saveTopic') : t('admin.addTopic') }}
            </button>
            <button v-if="topicForm.id" class="btn btn-outline-secondary" type="button" @click="resetTopicForm">
              {{ t('common.cancel') }}
            </button>
          </form>

          <div class="suggestion-panel">
            <div class="section-heading">
              <div>
                <h5>{{ t('admin.topicSuggestions') }}</h5>
                <p>{{ t('admin.topicSuggestionsHint') }}</p>
              </div>
              <button class="btn btn-sm btn-outline-primary" type="button" :disabled="isRefreshing" @click="loadTopicSuggestions">
                {{ t('common.refresh') }}
              </button>
            </div>

            <div class="table-responsive">
              <table class="table align-middle">
                <thead>
                  <tr>
                    <th>{{ t('admin.topic') }}</th>
                    <th>{{ t('admin.description') }}</th>
                    <th>{{ t('admin.suggester') }}</th>
                    <th>{{ t('admin.status') }}</th>
                    <th class="text-end">{{ t('admin.action') }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in topicSuggestions" :key="item.id">
                    <td><strong>{{ item.name }}</strong></td>
                    <td>
                      {{ item.description || item.reason || '-' }}
                      <small>{{ item.created_at || '-' }}</small>
                    </td>
                    <td>{{ item.suggester_username || '-' }}</td>
                    <td><StatusBadge :status="item.status" /></td>
                    <td class="text-end">
                      <button
                        class="btn btn-sm btn-outline-success me-2"
                        type="button"
                        :disabled="item.status !== 'pending' || reviewingSuggestionId === item.id"
                        @click="approveTopicSuggestion(item)"
                      >
                        {{ t('admin.approve') }}
                      </button>
                      <button
                        class="btn btn-sm btn-outline-danger me-2"
                        type="button"
                        :disabled="item.status !== 'pending' || reviewingSuggestionId === item.id"
                        @click="openRejectSuggestionDialog(item)"
                      >
                        {{ t('admin.reject') }}
                      </button>
                      <button
                        class="btn btn-sm btn-outline-secondary"
                        type="button"
                        :disabled="reviewingSuggestionId === item.id"
                        @click="openDeleteSuggestionDialog(item)"
                      >
                        {{ t('common.delete') }}
                      </button>
                    </td>
                  </tr>
                  <tr v-if="topicSuggestions.length === 0">
                    <td colspan="5" class="empty-cell">{{ t('admin.noTopicSuggestions') }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="admin-toolbar">
            <div class="status-filter">
              <button
                v-for="option in topicStatusOptions"
                :key="option.value"
                class="btn btn-sm"
                :class="topicStatusFilter === option.value ? 'btn-primary' : 'btn-outline-secondary'"
                type="button"
                @click="topicStatusFilter = option.value"
              >
                {{ t(option.labelKey) }}
              </button>
            </div>
          </div>

          <div class="table-responsive">
            <table class="table align-middle">
              <thead>
                <tr>
                  <th>{{ t('admin.topic') }}</th>
                  <th>{{ t('admin.description') }}</th>
                  <th>{{ t('admin.data') }}</th>
                  <th>{{ t('admin.status') }}</th>
                  <th class="text-end">{{ t('admin.action') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in filteredTopics" :key="item.id">
                  <td><strong>{{ item.name }}</strong></td>
                  <td>{{ item.description || '-' }}</td>
                  <td>
                    {{ t('admin.topicStats', {
                      questions: item.question_count || 0,
                      follows: item.follow_count || 0
                    }) }}
                  </td>
                  <td><StatusBadge :status="item.status" /></td>
                  <td class="text-end">
                    <button class="btn btn-sm btn-outline-primary me-2" type="button" @click="editTopic(item)">
                      {{ t('common.edit') }}
                    </button>
                    <button
                      class="btn btn-sm"
                      :class="item.status === 'hidden' ? 'btn-outline-success' : 'btn-outline-danger'"
                      type="button"
                      @click="toggleTopicStatus(item)"
                    >
                      {{ item.status === 'hidden' ? t('admin.restore') : t('admin.hide') }}
                    </button>
                    <button
                      class="btn btn-sm btn-outline-danger ms-2"
                      type="button"
                      @click="openDeleteTopicDialog(item)"
                    >
                      {{ t('common.delete') }}
                    </button>
                  </td>
                </tr>
                <tr v-if="filteredTopics.length === 0">
                  <td colspan="5" class="empty-cell">{{ t('admin.noTopics') }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div v-if="activeTab === 'content'" class="admin-section">
          <div class="content-switcher">
            <button
              v-for="type in contentTypes"
              :key="type.key"
              class="btn btn-sm"
              :class="contentType === type.key ? 'btn-primary' : 'btn-outline-secondary'"
              type="button"
              @click="switchContentType(type.key)"
            >
              {{ t(type.labelKey) }}
            </button>
          </div>

          <div class="admin-toolbar">
            <div class="input-group search-box">
              <span class="input-group-text"><i class="bi bi-search"></i></span>
              <input
                v-model.trim="contentKeyword"
                class="form-control"
                :placeholder="t('admin.searchContent')"
              >
            </div>
            <button
              v-if="contentKeyword"
              class="btn btn-outline-secondary"
              type="button"
              @click="contentKeyword = ''"
            >
              {{ t('common.clear') }}
            </button>
            <div class="status-filter">
              <button
                v-for="option in topicStatusOptions"
                :key="option.value"
                class="btn btn-sm"
                :class="contentStatusFilter === option.value ? 'btn-primary' : 'btn-outline-secondary'"
                type="button"
                @click="contentStatusFilter = option.value"
              >
                {{ t(option.labelKey) }}
              </button>
            </div>
            <span class="toolbar-count">
              {{ t('admin.contentSearchCount', { count: currentContentItems.length }) }}
            </span>
          </div>

          <div class="table-responsive">
            <table class="table align-middle">
              <thead>
                <tr>
                  <th>{{ t('admin.content') }}</th>
                  <th>{{ t('admin.author') }}</th>
                  <th>{{ t('admin.status') }}</th>
                  <th class="text-end">{{ t('admin.action') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in currentContentItems" :key="item.id">
                  <td>
                    <strong>{{ contentTitle(item) }}</strong>
                    <small>{{ contentPreview(item) }}</small>
                  </td>
                  <td>{{ item.author?.username || '-' }}</td>
                  <td><StatusBadge :status="item.status" /></td>
                  <td class="text-end">
                    <button
                      class="btn btn-sm"
                      :class="item.status === 'hidden' ? 'btn-outline-success' : 'btn-outline-danger'"
                      type="button"
                      @click="toggleContentStatus(item)"
                    >
                      {{ item.status === 'hidden' ? t('admin.restore') : t('admin.hide') }}
                    </button>
                  </td>
                </tr>
                <tr v-if="currentContentItems.length === 0">
                  <td colspan="4" class="empty-cell">{{ t('admin.noContent') }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </main>

    <div v-if="resetPasswordDialog.visible" class="admin-modal-backdrop" @click.self="closeResetPasswordDialog">
      <div class="admin-dialog" role="dialog" aria-modal="true">
        <div class="dialog-header">
          <div>
            <h5>{{ t('admin.resetPassword') }}</h5>
            <p>{{ t('admin.resetPasswordFor', { username: resetPasswordDialog.user?.username || '-' }) }}</p>
          </div>
          <button class="btn btn-sm btn-outline-secondary" type="button" @click="closeResetPasswordDialog">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>

        <form @submit.prevent="submitResetPassword">
          <label class="form-label" for="admin-new-password">{{ t('admin.newPassword') }}</label>
          <input
            id="admin-new-password"
            v-model.trim="resetPasswordDialog.password"
            class="form-control"
            type="password"
            autocomplete="new-password"
            minlength="6"
            required
            :placeholder="t('admin.newPasswordPlaceholder')"
          >
          <p class="dialog-hint">{{ t('admin.passwordHint') }}</p>

          <div class="dialog-actions">
            <button class="btn btn-outline-secondary" type="button" :disabled="resetPasswordDialog.loading" @click="closeResetPasswordDialog">
              {{ t('common.cancel') }}
            </button>
            <button class="btn btn-primary" type="submit" :disabled="resetPasswordDialog.loading || resetPasswordDialog.password.length < 6">
              <span v-if="resetPasswordDialog.loading" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
              {{ t('admin.confirmResetPassword') }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="deleteTopicDialog.visible" class="admin-modal-backdrop" @click.self="closeDeleteTopicDialog">
      <div class="admin-dialog" role="dialog" aria-modal="true">
        <div class="dialog-header">
          <div>
            <h5>{{ t('admin.deleteTopicTitle') }}</h5>
            <p>{{ t('admin.deleteTopicConfirm', { name: deleteTopicDialog.topic?.name || '-' }) }}</p>
          </div>
          <button class="btn btn-sm btn-outline-secondary" type="button" :disabled="deleteTopicDialog.loading" @click="closeDeleteTopicDialog">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>

        <p class="dialog-hint">{{ t('admin.deleteTopicHint') }}</p>

        <div class="dialog-actions">
          <button class="btn btn-outline-secondary" type="button" :disabled="deleteTopicDialog.loading" @click="closeDeleteTopicDialog">
            {{ t('common.cancel') }}
          </button>
          <button class="btn btn-danger" type="button" :disabled="deleteTopicDialog.loading" @click="confirmDeleteTopic">
            <span v-if="deleteTopicDialog.loading" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
            {{ t('common.delete') }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="rejectSuggestionDialog.visible" class="admin-modal-backdrop" @click.self="closeRejectSuggestionDialog">
      <div class="admin-dialog" role="dialog" aria-modal="true">
        <div class="dialog-header">
          <div>
            <h5>{{ t('admin.rejectSuggestionTitle') }}</h5>
            <p>{{ t('admin.rejectSuggestionConfirm', { name: rejectSuggestionDialog.suggestion?.name || '-' }) }}</p>
          </div>
          <button class="btn btn-sm btn-outline-secondary" type="button" :disabled="rejectSuggestionDialog.loading" @click="closeRejectSuggestionDialog">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>

        <p class="dialog-hint">{{ t('admin.rejectSuggestionHint') }}</p>

        <div class="dialog-actions">
          <button class="btn btn-outline-secondary" type="button" :disabled="rejectSuggestionDialog.loading" @click="closeRejectSuggestionDialog">
            {{ t('common.cancel') }}
          </button>
          <button class="btn btn-danger" type="button" :disabled="rejectSuggestionDialog.loading" @click="confirmRejectSuggestion">
            <span v-if="rejectSuggestionDialog.loading" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
            {{ t('admin.confirmReject') }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="deleteSuggestionDialog.visible" class="admin-modal-backdrop" @click.self="closeDeleteSuggestionDialog">
      <div class="admin-dialog" role="dialog" aria-modal="true">
        <div class="dialog-header">
          <div>
            <h5>{{ t('admin.deleteSuggestionTitle') }}</h5>
            <p>{{ t('admin.deleteSuggestionConfirm', { name: deleteSuggestionDialog.suggestion?.name || '-' }) }}</p>
          </div>
          <button class="btn btn-sm btn-outline-secondary" type="button" :disabled="deleteSuggestionDialog.loading" @click="closeDeleteSuggestionDialog">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>

        <p class="dialog-hint">{{ t('admin.deleteSuggestionHint') }}</p>

        <div class="dialog-actions">
          <button class="btn btn-outline-secondary" type="button" :disabled="deleteSuggestionDialog.loading" @click="closeDeleteSuggestionDialog">
            {{ t('common.cancel') }}
          </button>
          <button class="btn btn-danger" type="button" :disabled="deleteSuggestionDialog.loading" @click="confirmDeleteSuggestion">
            <span v-if="deleteSuggestionDialog.loading" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
            {{ t('common.delete') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import Navbar from '../components/Navbar.vue'
import { adminApi } from '../api/admin'
import { showToast } from '../utils/toast'

const { t } = useI18n()

const StatusBadge = defineComponent({
  props: {
    status: {
      type: String,
      default: 'active'
    }
  },
  setup(props) {
    return () => h('span', {
      class: ['status-badge', props.status === 'hidden' || props.status === 'disabled' ? 'muted' : 'active']
    }, t(statusLabelKey(props.status)))
  }
})

const tabs = [
  { key: 'users', labelKey: 'admin.tabs.users', icon: 'bi bi-people' },
  { key: 'topics', labelKey: 'admin.tabs.topics', icon: 'bi bi-tags' },
  { key: 'content', labelKey: 'admin.tabs.content', icon: 'bi bi-shield-check' }
]

const contentTypes = [
  { key: 'questions', labelKey: 'admin.contentTypes.questions' },
  { key: 'answers', labelKey: 'admin.contentTypes.answers' },
  { key: 'comments', labelKey: 'admin.contentTypes.comments' }
]

const userStatusOptions = [
  { value: 'all', labelKey: 'admin.filterAll' },
  { value: 'active', labelKey: 'admin.statusActive' },
  { value: 'disabled', labelKey: 'admin.statusDisabled' }
]

const topicStatusOptions = [
  { value: 'all', labelKey: 'admin.filterAll' },
  { value: 'active', labelKey: 'admin.statusActive' },
  { value: 'hidden', labelKey: 'admin.statusHidden' }
]

const activeTab = ref('users')
const contentType = ref('questions')
const isRefreshing = ref(false)
const loadedTabs = reactive({
  users: false,
  topics: false,
  content: false
})
const userKeyword = ref('')
const userStatusFilter = ref('all')
const topicStatusFilter = ref('all')
const contentStatusFilter = ref('all')
const contentKeyword = ref('')
const users = ref([])
const topics = ref([])
const topicSuggestions = ref([])
const questions = ref([])
const answers = ref([])
const comments = ref([])
const topicForm = reactive({
  id: null,
  name: '',
  description: ''
})
const resetPasswordDialog = reactive({
  visible: false,
  loading: false,
  user: null,
  password: ''
})
const deleteTopicDialog = reactive({
  visible: false,
  loading: false,
  topic: null
})
const reviewingSuggestionId = ref(null)
const rejectSuggestionDialog = reactive({
  visible: false,
  loading: false,
  suggestion: null
})
const deleteSuggestionDialog = reactive({
  visible: false,
  loading: false,
  suggestion: null
})

const filteredUsers = computed(() => {
  return users.value.filter(item => statusMatchesFilter(item, userStatusFilter.value))
})

const filteredTopics = computed(() => {
  return topics.value.filter(item => statusMatchesFilter(item, topicStatusFilter.value))
})

const currentRawContentItems = computed(() => {
  if (contentType.value === 'answers') {
    return answers.value
  }
  if (contentType.value === 'comments') {
    return comments.value
  }
  return questions.value
})

const currentContentItems = computed(() => {
  const keyword = contentKeyword.value.trim().toLowerCase()
  return currentRawContentItems.value
    .filter(item => statusMatchesFilter(item, contentStatusFilter.value))
    .filter(item => !keyword || contentMatchesKeyword(item, keyword))
})

onMounted(() => {
  loadUsers()
})

async function switchTab(tab) {
  activeTab.value = tab
  if (!loadedTabs[tab]) {
    await reloadCurrentTab()
  }
}

async function reloadCurrentTab() {
  if (activeTab.value === 'topics') {
    await loadTopics()
  } else if (activeTab.value === 'content') {
    await loadContent()
  } else {
    await loadUsers()
  }
}

async function runRefresh(task) {
  isRefreshing.value = true
  try {
    await task()
  } finally {
    isRefreshing.value = false
  }
}

async function loadUsers() {
  await runRefresh(async () => {
    try {
      const result = await adminApi.getUsers({ keyword: userKeyword.value })
      users.value = result?.code === 200 ? result.data || [] : []
      loadedTabs.users = true
    } catch (error) {
      showToast(t('admin.loadUsersFailed'), 'error')
    }
  })
}

async function loadTopics() {
  await runRefresh(async () => {
    try {
      const result = await adminApi.getTopics()
      topics.value = result?.code === 200 ? result.data || [] : []
      await loadTopicSuggestions(false)
      loadedTabs.topics = true
    } catch (error) {
      showToast(t('admin.loadTopicsFailed'), 'error')
    }
  })
}

async function loadTopicSuggestions(showError = true) {
  try {
    const result = await adminApi.getTopicSuggestions()
    topicSuggestions.value = result?.code === 200 ? result.data || [] : []
  } catch (error) {
    if (showError) {
      showToast(t('admin.loadTopicSuggestionsFailed'), 'error')
    }
  }
}

async function loadContent() {
  await runRefresh(async () => {
    try {
      const [questionResult, answerResult, commentResult] = await Promise.all([
        adminApi.getQuestions(),
        adminApi.getAnswers(),
        adminApi.getComments()
      ])
      questions.value = questionResult?.code === 200 ? questionResult.data || [] : []
      answers.value = answerResult?.code === 200 ? answerResult.data || [] : []
      comments.value = commentResult?.code === 200 ? commentResult.data || [] : []
      loadedTabs.content = true
    } catch (error) {
      showToast(t('admin.loadContentFailed'), 'error')
    }
  })
}

async function switchContentType(type) {
  contentType.value = type
  if (!loadedTabs.content) {
    await loadContent()
  }
}

async function toggleUserStatus(item) {
  const nextStatus = item.status === 'disabled' ? 'active' : 'disabled'
  await adminApi.updateUserStatus(item.id, nextStatus)
  item.status = nextStatus
  showToast(t('admin.userStatusUpdated'), 'success')
}

function openResetPasswordDialog(user) {
  resetPasswordDialog.visible = true
  resetPasswordDialog.loading = false
  resetPasswordDialog.user = user
  resetPasswordDialog.password = ''
}

function closeResetPasswordDialog() {
  if (resetPasswordDialog.loading) {
    return
  }
  resetPasswordDialog.visible = false
  resetPasswordDialog.user = null
  resetPasswordDialog.password = ''
}

async function submitResetPassword() {
  if (!resetPasswordDialog.user || resetPasswordDialog.password.length < 6) {
    return
  }

  resetPasswordDialog.loading = true
  try {
    await adminApi.resetUserPassword(resetPasswordDialog.user.id, resetPasswordDialog.password)
    showToast(t('admin.passwordResetSuccess'), 'success')
    closeResetPasswordDialog()
  } catch (error) {
    showToast(t('admin.passwordResetFailed'), 'error')
  } finally {
    resetPasswordDialog.loading = false
  }
}

async function saveTopic() {
  if (topicForm.id) {
    await adminApi.updateTopic(topicForm.id, {
      name: topicForm.name,
      description: topicForm.description
    })
    showToast(t('admin.topicUpdated'), 'success')
  } else {
    await adminApi.createTopic({
      name: topicForm.name,
      description: topicForm.description
    })
    showToast(t('admin.topicCreated'), 'success')
  }
  resetTopicForm()
  await loadTopics()
}

function editTopic(item) {
  topicForm.id = item.id
  topicForm.name = item.name
  topicForm.description = item.description || ''
}

function resetTopicForm() {
  topicForm.id = null
  topicForm.name = ''
  topicForm.description = ''
}

async function toggleTopicStatus(item) {
  const nextStatus = item.status === 'hidden' ? 'active' : 'hidden'
  await adminApi.updateTopicStatus(item.id, nextStatus)
  item.status = nextStatus
  showToast(t('admin.topicStatusUpdated'), 'success')
}

function openDeleteTopicDialog(topic) {
  deleteTopicDialog.visible = true
  deleteTopicDialog.loading = false
  deleteTopicDialog.topic = topic
}

function closeDeleteTopicDialog() {
  if (deleteTopicDialog.loading) {
    return
  }
  deleteTopicDialog.visible = false
  deleteTopicDialog.topic = null
}

async function confirmDeleteTopic() {
  const topic = deleteTopicDialog.topic
  if (!topic) {
    return
  }

  deleteTopicDialog.loading = true
  try {
    await adminApi.deleteTopic(topic.id)
    topics.value = topics.value.filter(item => item.id !== topic.id)
    showToast(t('admin.topicDeleted'), 'success')
    deleteTopicDialog.loading = false
    closeDeleteTopicDialog()
  } catch (error) {
    showToast(t('admin.deleteTopicFailed'), 'error')
  } finally {
    deleteTopicDialog.loading = false
  }
}

async function approveTopicSuggestion(item) {
  reviewingSuggestionId.value = item.id
  try {
    await adminApi.approveTopicSuggestion(item.id)
    showToast(t('admin.topicSuggestionApproved'), 'success')
    await loadTopics()
  } catch (error) {
    showToast(t('admin.reviewTopicSuggestionFailed'), 'error')
  } finally {
    reviewingSuggestionId.value = null
  }
}

function openRejectSuggestionDialog(item) {
  rejectSuggestionDialog.visible = true
  rejectSuggestionDialog.loading = false
  rejectSuggestionDialog.suggestion = item
}

function closeRejectSuggestionDialog() {
  if (rejectSuggestionDialog.loading) {
    return
  }
  rejectSuggestionDialog.visible = false
  rejectSuggestionDialog.suggestion = null
}

async function confirmRejectSuggestion() {
  const item = rejectSuggestionDialog.suggestion
  if (!item) {
    return
  }

  rejectSuggestionDialog.loading = true
  reviewingSuggestionId.value = item.id
  try {
    await adminApi.rejectTopicSuggestion(item.id)
    item.status = 'rejected'
    showToast(t('admin.topicSuggestionRejected'), 'success')
    rejectSuggestionDialog.loading = false
    closeRejectSuggestionDialog()
  } catch (error) {
    showToast(t('admin.reviewTopicSuggestionFailed'), 'error')
  } finally {
    rejectSuggestionDialog.loading = false
    reviewingSuggestionId.value = null
  }
}

function openDeleteSuggestionDialog(item) {
  deleteSuggestionDialog.visible = true
  deleteSuggestionDialog.loading = false
  deleteSuggestionDialog.suggestion = item
}

function closeDeleteSuggestionDialog() {
  if (deleteSuggestionDialog.loading) {
    return
  }
  deleteSuggestionDialog.visible = false
  deleteSuggestionDialog.suggestion = null
}

async function confirmDeleteSuggestion() {
  const item = deleteSuggestionDialog.suggestion
  if (!item) {
    return
  }

  deleteSuggestionDialog.loading = true
  reviewingSuggestionId.value = item.id
  try {
    await adminApi.deleteTopicSuggestion(item.id)
    topicSuggestions.value = topicSuggestions.value.filter(s => s.id !== item.id)
    showToast(t('admin.topicSuggestionDeleted'), 'success')
    deleteSuggestionDialog.loading = false
    closeDeleteSuggestionDialog()
  } catch (error) {
    showToast(t('admin.deleteTopicSuggestionFailed'), 'error')
  } finally {
    deleteSuggestionDialog.loading = false
    reviewingSuggestionId.value = null
  }
}

async function toggleContentStatus(item) {
  const nextStatus = item.status === 'hidden' ? 'active' : 'hidden'
  await updateContentStatus(item, nextStatus)
  showToast(t('admin.contentStatusUpdated'), 'success')
}

async function updateContentStatus(item, nextStatus) {
  if (contentType.value === 'answers') {
    await adminApi.updateAnswerStatus(item.id, nextStatus)
  } else if (contentType.value === 'comments') {
    await adminApi.updateCommentStatus(item.id, nextStatus)
  } else {
    await adminApi.updateQuestionStatus(item.id, nextStatus)
  }
  item.status = nextStatus
}

function contentTitle(item) {
  if (contentType.value === 'questions') {
    return item.title || t('admin.questionWithId', { id: item.id })
  }
  return item.question_title || t('admin.questionWithId', { id: item.question_id || '-' })
}

function contentPreview(item) {
  const text = item.content || ''
  return text.length > 90 ? `${text.slice(0, 90)}...` : text
}

function statusMatchesFilter(item, filter) {
  return filter === 'all' || item.status === filter
}

function contentMatchesKeyword(item, keyword) {
  const fields = [item.id, item.content, item.author?.username, item.author?.id]
  if (contentType.value === 'questions') {
    fields.push(item.title)
  } else {
    fields.push(item.question_id, item.question_title)
  }
  if (contentType.value === 'comments') {
    fields.push(item.answer_id, item.parent_id)
  }
  return fields.some(value => String(value || '').toLowerCase().includes(keyword))
}

function statusLabelKey(status) {
  if (status === 'disabled') {
    return 'admin.statusDisabled'
  }
  if (status === 'hidden') {
    return 'admin.statusHidden'
  }
  if (status === 'pending') {
    return 'admin.statusPending'
  }
  if (status === 'approved') {
    return 'admin.statusApproved'
  }
  if (status === 'rejected') {
    return 'admin.statusRejected'
  }
  return 'admin.statusActive'
}

function roleText(role) {
  return role === 'admin' ? t('admin.roleAdmin') : t('admin.roleUser')
}
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
}

.admin-header {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.admin-header h2 {
  color: #172033;
  font-size: 1.7rem;
  font-weight: 800;
  margin: 0 0 0.25rem;
}

.admin-header p {
  color: #64748b;
  margin: 0;
}

.admin-tabs {
  border-bottom: 1px solid rgba(87, 116, 146, 0.14);
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.admin-tab {
  align-items: center;
  background: transparent;
  border: 0;
  border-bottom: 3px solid transparent;
  color: #5b6b7c;
  display: inline-flex;
  font-weight: 700;
  gap: 0.45rem;
  padding: 0.85rem 1rem;
}

.admin-tab.active {
  border-color: #2f6fc5;
  color: #2f6fc5;
}

.admin-panel {
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(87, 116, 146, 0.14);
  border-radius: 8px;
  box-shadow: 0 16px 34px rgba(43, 63, 92, 0.1);
  min-height: 420px;
  padding: 1rem;
}

.admin-toolbar,
.topic-form,
.content-switcher {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.search-box {
  max-width: 360px;
}

.toolbar-count {
  color: #64748b;
  font-size: 0.9rem;
  font-weight: 700;
}

.status-filter {
  align-items: center;
  display: inline-flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.topic-form .form-control {
  max-width: 300px;
}

.suggestion-panel {
  border-bottom: 1px solid rgba(87, 116, 146, 0.14);
  margin-bottom: 1rem;
  padding-bottom: 1rem;
}

.section-heading {
  align-items: flex-start;
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.5rem;
}

.section-heading h5 {
  font-size: 1rem;
  font-weight: 800;
  margin: 0 0 0.2rem;
}

.section-heading p {
  color: #64748b;
  margin: 0;
}

.table {
  margin-bottom: 0;
}

td strong,
td small {
  display: block;
}

td small {
  color: #64748b;
  margin-top: 0.2rem;
  max-width: 520px;
}

.empty-cell {
  color: #64748b;
  padding: 2.5rem 1rem !important;
  text-align: center;
}

.spinning {
  animation: admin-spin 0.9s linear infinite;
}

.admin-modal-backdrop {
  align-items: center;
  background: rgba(15, 23, 42, 0.42);
  display: flex;
  inset: 0;
  justify-content: center;
  padding: 1rem;
  position: fixed;
  z-index: 1060;
}

.admin-dialog {
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.24);
  max-width: 460px;
  padding: 1.25rem;
  width: min(100%, 460px);
}

.dialog-header {
  align-items: flex-start;
  display: flex;
  gap: 1rem;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.dialog-header h5 {
  font-weight: 800;
  margin: 0 0 0.25rem;
}

.dialog-header p,
.dialog-hint {
  color: #64748b;
  margin: 0;
}

.dialog-hint {
  font-size: 0.9rem;
  margin-top: 0.5rem;
}

.dialog-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  margin-top: 1.25rem;
}

:deep(.status-badge) {
  border-radius: 999px;
  display: inline-flex;
  font-size: 0.78rem;
  font-weight: 800;
  padding: 0.25rem 0.6rem;
}

:deep(.status-badge.active) {
  background: rgba(25, 135, 84, 0.12);
  color: #198754;
}

:deep(.status-badge.muted) {
  background: rgba(108, 117, 125, 0.14);
  color: #6c757d;
}

@keyframes admin-spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .admin-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 0.75rem;
  }

  .admin-tabs {
    overflow-x: auto;
  }
}
</style>
