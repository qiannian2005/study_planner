<template>
  <div class="ask-question">
    <Navbar />
    <div class="container py-4">
      <div class="row justify-content-center">
        <div class="col-lg-8">
          <div class="card">
            <div class="card-header">
              <h4 class="mb-0">{{ $t('forum.askQuestion.title') }}</h4>
            </div>
            <div class="card-body">
              <form @submit.prevent="submitQuestion">
                <!-- 鏍囬 -->
                <div class="mb-3">
                  <label for="title" class="form-label">{{ $t('forum.askQuestion.questionTitle') }}</label>
                  <input 
                    type="text" 
                    class="form-control" 
                    id="title"
                    v-model="form.title"
                    :placeholder="$t('forum.askQuestion.titlePlaceholder')"
                    required
                  >
                </div>

                <!-- 闂鎻忚堪 -->
                <div class="mb-3">
                  <label for="content" class="form-label">{{ $t('forum.askQuestion.questionContent') }}</label>
                  <div class="d-flex gap-2 mb-2">
                    <button 
                      type="button"
                      class="btn btn-sm btn-outline-secondary"
                      @click="showPreview = !showPreview"
                    >
                      {{ showPreview ? $t('common.edit') : $t('common.preview') }}
                    </button>
                  </div>
                  <textarea 
                    class="form-control" 
                    id="content"
                    rows="12"
                    v-model="form.content"
                    :placeholder="$t('forum.askQuestion.contentPlaceholder')"
                    v-if="!showPreview"
                  ></textarea>
                  <div 
                    v-else
                    class="border rounded p-3"
                    style="min-height: 300px;"
                    v-html="previewContent"
                  ></div>
                </div>

                <!-- 璇濋/鏍囩 -->
                <div class="mb-3">
                  <label class="form-label">{{ $t('forum.askQuestion.selectTopics') }}</label>
                  <div class="d-flex flex-wrap gap-2 mb-2" v-if="selectedTopics.length > 0">
                    <span 
                      v-for="topic in selectedTopics" 
                      :key="topic.id"
                      class="selected-topic-badge"
                    >
                      <i class="bi bi-check-circle-fill"></i>
                      {{ displayTopicName(topic, locale) }}
                      <button 
                        type="button"
                        class="selected-topic-remove"
                        @click="removeTopic(topic.id)"
                        aria-label="绉婚櫎"
                      >
                        <i class="bi bi-x-lg"></i>
                      </button>
                    </span>
                  </div>
                  <div v-else class="text-muted small mb-2">
                    <i class="bi bi-info-circle"></i> {{ $t('forum.askQuestion.noTopicsSelected') }}
                  </div>
                  <div class="input-group">
                    <input 
                      type="text" 
                      class="form-control"
                      v-model="topicInput"
                      :placeholder="$t('forum.askQuestion.topicSuggestionPlaceholder')"
                      @keyup.enter="suggestTopic"
                    >
                    <button 
                      type="button"
                      class="btn btn-outline-secondary"
                      :disabled="suggestingTopic"
                      @click="suggestTopic"
                    >
                      <span v-if="suggestingTopic" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                      {{ $t('forum.askQuestion.suggestTopic') }}
                    </button>
                  </div>
                  <div class="form-text">{{ $t('forum.askQuestion.topicSuggestionHint') }}</div>
                  <div class="mt-2">
                    <small class="text-muted">{{ $t('forum.askQuestion.hotTopics') }}</small>
                    <div class="d-flex flex-wrap gap-1 mt-1">
                      <button 
                        type="button"
                        class="btn btn-sm"
                        :class="selectedTopics.find(t => t.id === topic.id) ? 'btn-primary' : 'btn-outline-primary'"
                        v-for="topic in hotTopics"
                        :key="topic.id"
                        @click="selectTopic(topic)"
                      >
                        <i 
                          class="bi"
                          :class="selectedTopics.find(t => t.id === topic.id) ? 'bi-check-circle-fill' : 'bi-circle'"
                        ></i>
                        {{ displayTopicName(topic, locale) }}
                      </button>
                    </div>
                  </div>
                </div>

                <!-- 鎿嶄綔鎸夐挳 -->
                <div class="d-flex justify-content-between">
                  <button 
                    type="button"
                    class="btn btn-outline-secondary"
                    @click="saveDraft"
                  >
                    {{ $t('common.saveDraft') }}
                  </button>
                  <div>
                    <button 
                      type="button"
                      class="btn btn-outline-secondary me-2"
                      @click="$router.back()"
                    >
                      {{ $t('common.cancel') }}
                    </button>
                    <button 
                      type="submit"
                      class="btn btn-primary"
                      :disabled="!form.title.trim() || submitting"
                    >
                      {{ submitting ? $t('forum.askQuestion.submitting') : $t('forum.askQuestion.submit') }}
                    </button>
                  </div>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
    <Footer />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import Navbar from '../../components/Navbar.vue'
import Footer from '../../components/Footer.vue'
import { renderMarkdown } from '../../utils/markdown'
import { useForumStore } from '../../stores/forum'
import { useUserStore } from '../../stores/user'
import { showToast } from '../../utils/toast'
import { topicApi } from '../../api/forum'
import { displayTopicName, topicMatchesInput } from '../../utils/forumTopic'

const { t, locale } = useI18n()
const router = useRouter()
const forumStore = useForumStore()
const userStore = useUserStore()
const LEGACY_DRAFT_KEY = 'question_draft'

const form = ref({
  title: '',
  content: ''
})

const selectedTopics = ref([])
const topicInput = ref('')
const hotTopics = ref([])
const showPreview = ref(false)
const submitting = ref(false)
const suggestingTopic = ref(false)

const previewContent = computed(() => {
  if (!form.value.content) return ''
  return renderMarkdown(form.value.content)
})

onMounted(() => {
  loadHotTopics()
  loadDraft()
})

function getCurrentUserId() {
  if (userStore.user?.id) {
    return userStore.user.id
  }

  try {
    const cachedUser = JSON.parse(localStorage.getItem('user') || 'null')
    return cachedUser?.id || null
  } catch (error) {
    return null
  }
}

function getDraftKey() {
  const userId = getCurrentUserId()
  return userId ? `question_draft_user_${userId}` : null
}

async function loadHotTopics() {
  const topics = await forumStore.fetchHotTopics()
  hotTopics.value = topics || []
}

function selectTopic(topic) {
  const existing = selectedTopics.value.find(t => t.id === topic.id)
  if (existing) {
    // 濡傛灉宸查€夋嫨锛屽垯鍙栨秷閫夋嫨
    removeTopic(topic.id)
    showToast(t('forum.askQuestion.topicUnselected') + `: ${displayTopicName(topic, locale.value)}`, 'info')
  } else {
    // 濡傛灉鏈€夋嫨锛屽垯娣诲姞
    selectedTopics.value.push(topic)
    showToast(t('forum.askQuestion.topicSelected') + `: ${displayTopicName(topic, locale.value)}`, 'success')
  }
}

async function suggestTopic() {
  const name = topicInput.value.trim()
  const submitSuggestion = async () => {
    const existingTopic = hotTopics.value.find(topic => topicMatchesInput(topic, name, locale.value))
    if (existingTopic) {
      selectTopic(existingTopic)
      topicInput.value = ''
      return
    }

    if (suggestingTopic.value) {
      return
    }

    suggestingTopic.value = true
    try {
      const response = await topicApi.suggestTopic({ name, description: '', reason: '' })
      if (response.code === 200) {
        showToast(t('forum.askQuestion.topicSuggestionSubmitted'), 'success')
        topicInput.value = ''
      } else {
        showToast(response.message || t('forum.askQuestion.topicSuggestionFailed'), 'error')
      }
    } catch (error) {
      showToast(error.response?.data?.message || t('forum.askQuestion.topicSuggestionFailed'), 'error')
    } finally {
      suggestingTopic.value = false
    }
  }
  if (!name) {
    showToast(t('forum.askQuestion.pleaseEnterTopicName'), 'warning')
    return
  }
  await submitSuggestion()
}

function removeTopic(topicId) {
  const topic = selectedTopics.value.find(t => t.id === topicId)
  selectedTopics.value = selectedTopics.value.filter(t => t.id !== topicId)
  if (topic) {
    showToast(t('forum.askQuestion.topicRemoved') + `: ${displayTopicName(topic, locale.value)}`, 'info')
  }
}

function saveDraft() {
  const draftKey = getDraftKey()
  if (!draftKey) {
    showToast(t('auth.loginRequired'), 'warning')
    return
  }

  const draft = {
    userId: getCurrentUserId(),
    title: form.value.title,
    content: form.value.content,
    topics: selectedTopics.value
  }
  localStorage.setItem(draftKey, JSON.stringify(draft))
  localStorage.removeItem(LEGACY_DRAFT_KEY)
  showToast(t('forum.askQuestion.draftSaved'), 'success')
}

function loadDraft() {
  const draftKey = getDraftKey()
  localStorage.removeItem(LEGACY_DRAFT_KEY)

  if (!draftKey) {
    return
  }

  const draft = localStorage.getItem(draftKey)
  if (draft) {
    try {
      const data = JSON.parse(draft)
      const currentUserId = getCurrentUserId()
      if (data.userId && currentUserId && Number(data.userId) !== Number(currentUserId)) {
        localStorage.removeItem(draftKey)
        return
      }
      form.value.title = data.title || ''
      form.value.content = data.content || ''
      selectedTopics.value = data.topics || []
    } catch (error) {
      console.error('鍔犺浇鑽夌澶辫触:', error)
    }
  }
}

async function submitQuestion() {
  if (!form.value.title.trim()) {
    showToast(t('forum.askQuestion.pleaseEnterTitle'), 'warning')
    return
  }
  
  submitting.value = true
  try {
    const result = await forumStore.createQuestion({
      title: form.value.title,
      content: form.value.content,
      topic_ids: selectedTopics.value.map(t => t.id)
    })
    
    if (result) {
      // 娓呴櫎鑽夌
      const draftKey = getDraftKey()
      if (draftKey) {
        localStorage.removeItem(draftKey)
      }
      localStorage.removeItem(LEGACY_DRAFT_KEY)
      showToast(t('forum.askQuestion.questionPublished'), 'success')
      // 璺宠浆鍒伴棶棰樿鎯呴〉
      router.push(`/forum/question/${result.id}`).then(() => {
        // 瑙﹀彂璁哄潧棣栭〉鍒锋柊锛堥€氳繃浜嬩欢鎴栫洿鎺ヨ皟鐢級
        window.dispatchEvent(new CustomEvent('forum-question-created'))
      })
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.preview-content {
  line-height: 1.8;
}

.preview-content :deep(code) {
  background-color: #f5f5f5;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 0.9em;
}

.preview-content :deep(pre) {
  background-color: #f5f5f5;
  padding: 1em;
  border-radius: 4px;
  overflow-x: auto;
}

.selected-topic-badge {
  align-items: center;
  background: #e8f2ff;
  border: 1px solid #4da0ff;
  border-radius: 999px;
  color: #1f6fd1;
  display: inline-flex;
  font-size: 0.92rem;
  font-weight: 700;
  gap: 0.45rem;
  min-height: 2.75rem;
  padding: 0.45rem 0.55rem 0.45rem 1rem;
}

.selected-topic-remove {
  align-items: center;
  background: rgba(31, 111, 209, 0.14);
  border: 0;
  border-radius: 999px;
  color: #164d91;
  display: inline-flex;
  height: 1.6rem;
  justify-content: center;
  margin-left: 0.1rem;
  padding: 0;
  transition: background-color 0.15s ease, color 0.15s ease;
  width: 1.6rem;
}

.selected-topic-remove:hover,
.selected-topic-remove:focus-visible {
  background: #1f6fd1;
  color: #ffffff;
}

.selected-topic-remove i {
  font-size: 0.78rem;
  line-height: 1;
}
</style>
