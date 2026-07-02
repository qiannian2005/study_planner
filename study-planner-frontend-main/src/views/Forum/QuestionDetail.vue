<template>
  <div class="question-detail">
    <Navbar />
    <div class="container py-4">
      <div class="row">
        <!-- 左侧内容区 -->
        <div class="col-lg-8">
          <!-- 加载状态 -->
          <div v-if="forumStore.loading && !question" class="text-center py-5">
            <div class="spinner-border" role="status">
              <span class="visually-hidden">{{ $t('common.loading') }}</span>
            </div>
          </div>

          <!-- 问题展示区 -->
          <div v-else-if="question" class="card mb-4">
            <div class="card-body">
              <h1 class="card-title mb-3">{{ question.title }}</h1>
              
              <div class="d-flex flex-wrap gap-2 mb-3" v-if="question.topics && question.topics.length > 0">
                <TopicTag 
                  v-for="topic in question.topics" 
                  :key="topic.id" 
                  :topic="topic"
                />
              </div>

              <div class="question-content mb-3" v-html="renderedContent"></div>

              <div class="d-flex align-items-center justify-content-between border-top pt-3">
                <div class="d-flex align-items-center text-muted small">
                  <img 
                    v-if="question.author?.avatar" 
                    :src="question.author.avatar" 
                    class="rounded-circle me-2"
                    style="width: 32px; height: 32px;"
                    alt="avatar"
                  >
                  <div class="d-flex align-items-center gap-2">
                    <router-link 
                      :to="`/forum/user/${question.author?.id}`"
                      class="text-decoration-none"
                    >
                      {{ question.author?.username || $t('forum.questionCard.anonymousUser') }}
                    </router-link>
                    <span class="ms-2">{{ formatTime(questionTime) }}</span>
                    <button 
                      v-if="userStore.isLoggedIn && question.author && question.author.id !== userStore.user?.id"
                      class="btn btn-sm"
                      :class="question.author.is_following ? 'btn-primary' : 'btn-outline-primary'"
                      @click="handleFollowAuthor"
                      :disabled="followingAuthor"
                    >
                      <i class="bi" :class="question.author.is_following ? 'bi-person-check-fill' : 'bi-person-plus'"></i>
                      <span class="ms-1">{{ question.author.is_following ? $t('forum.userProfile.followed') : $t('forum.userProfile.follow') }}</span>
                    </button>
                  </div>
                </div>
                <div class="d-flex align-items-center gap-2">
                  <button 
                    class="btn btn-sm"
                    :class="question.is_followed ? 'btn-success' : 'btn-outline-success'"
                    @click="handleFollow"
                    :disabled="following"
                    :title="question.is_followed ? $t('forum.actions.unfollow') : $t('forum.actions.follow')"
                  >
                    <i class="bi" :class="question.is_followed ? 'bi-bell-fill' : 'bi-bell'"></i>
                    <span class="ms-1">{{ question.follow_count || 0 }}</span>
                  </button>
                  <button 
                    class="btn btn-sm"
                    :class="question.is_favorited ? 'btn-warning' : 'btn-outline-warning'"
                    @click="handleFavorite"
                    :disabled="favoriting"
                  >
                    <i class="bi" :class="question.is_favorited ? 'bi-star-fill' : 'bi-star'"></i>
                    <span class="ms-1">{{ question.favorite_count || 0 }}</span>
                  </button>
                  <button 
                    class="btn btn-sm"
                    :class="question.is_voted ? 'btn-primary' : 'btn-outline-primary'"
                    @click="handleVoteQuestion"
                    :disabled="votingQuestion"
                  >
                    <i class="bi" :class="question.is_voted ? 'bi-heart-fill' : 'bi-heart'"></i>
                    <span class="ms-1">{{ question.vote_count || 0 }}</span>
                  </button>
                  <button class="btn btn-sm btn-outline-secondary" @click="openForwardModal">
                    <i class="bi bi-share"></i>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- 回答列表 -->
          <div id="answers" class="mb-4">
            <div class="d-flex justify-content-between align-items-center mb-3">
              <h5>{{ $t('forum.answer.count', { count: answerCount }) }}</h5>
              <div class="btn-group" role="group">
                <button 
                  class="btn btn-sm btn-outline-secondary"
                  :class="{ active: sortBy === 'default' }"
                  @click="changeSort('default')"
                >
                  {{ $t('forum.answer.sortDefault') }}
                </button>
                <button 
                  class="btn btn-sm btn-outline-secondary"
                  :class="{ active: sortBy === 'time' }"
                  @click="changeSort('time')"
                >
                  {{ $t('forum.answer.sortTime') }}
                </button>
                <button 
                  class="btn btn-sm btn-outline-secondary"
                  :class="{ active: sortBy === 'vote' }"
                  @click="changeSort('vote')"
                >
                  {{ $t('forum.answer.sortVote') }}
                </button>
              </div>
            </div>

            <div v-if="answersLoading" class="text-center py-3">
              <div class="spinner-border spinner-border-sm" role="status">
                <span class="visually-hidden">{{ $t('common.loading') }}</span>
              </div>
            </div>

            <div v-else-if="forumStore.answers.length === 0" class="text-center py-5 text-muted">
              {{ $t('forum.answer.noAnswers') }}
            </div>

            <AnswerCard 
              v-for="answer in forumStore.answers" 
              :key="answer.id"
              :answer="answer"
              @vote="handleVote"
              @collect="handleCollect"
              @edit="handleEditAnswer"
              @delete="handleDeleteAnswer"
              @comment-added="loadAnswers"
            />
          </div>

          <!-- 回答编辑区 -->
          <div class="card" v-if="userStore.isLoggedIn">
            <div class="card-header">
              <h6 class="mb-0">{{ $t('forum.answer.submit') }}</h6>
            </div>
            <div class="card-body">
              <div class="mb-3">
                <textarea 
                  class="form-control" 
                  rows="8"
                  v-model="answerContent"
                  :placeholder="$t('forum.answer.content') + ' (支持 Markdown)...'"
                ></textarea>
              </div>
              <div class="d-flex justify-content-between">
                <div>
                  <button class="btn btn-sm btn-outline-secondary" @click="showPreview = !showPreview">
                    {{ showPreview ? $t('common.edit') : $t('common.preview') }}
                  </button>
                </div>
                <div>
                  <button class="btn btn-outline-secondary me-2" @click="saveDraft">{{ $t('common.saveDraft') }}</button>
                  <button class="btn btn-primary" @click="submitAnswer" :disabled="!answerContent.trim() || submitting">
                    {{ submitting ? $t('common.submitting') : $t('forum.answer.submit') }}
                  </button>
                </div>
              </div>
              
              <!-- 预览区 -->
              <div v-if="showPreview && answerContent" class="mt-3 p-3 border rounded">
                <div v-html="previewContent"></div>
              </div>
            </div>
          </div>

          <div v-else class="card">
            <div class="card-body text-center">
              <p class="text-muted">{{ $t('auth.loginRequired') }}</p>
              <router-link to="/login" class="btn btn-primary">{{ $t('nav.login') }}</router-link>
            </div>
          </div>
        </div>

        <!-- 右侧边栏 -->
        <div class="col-lg-4">
          <div class="sticky-top" style="top: 20px;">
            <!-- 相关问题 -->
            <div class="card mb-3">
              <div class="card-header">
                <h6 class="mb-0">{{ $t('forum.relatedQuestions') }}</h6>
              </div>
              <div class="card-body">
                <div v-if="relatedQuestions.length === 0" class="text-muted small">
                  {{ $t('forum.relatedQuestionsEmpty') }}
                </div>
                <div v-else>
                  <div 
                    v-for="q in relatedQuestions" 
                    :key="q.id"
                    class="mb-2"
                  >
                    <router-link 
                      :to="`/forum/question/${q.id}`"
                      class="text-decoration-none small"
                    >
                      {{ q.title }}
                    </router-link>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showForwardModal" class="forward-modal-backdrop" @click.self="closeForwardModal">
      <div class="forward-modal" role="dialog" aria-modal="true">
        <div class="forward-modal-header">
          <div>
            <h5 class="mb-1">转发到学习聊天室</h5>
            <p class="mb-0 text-muted small">把这条论坛问题分享到公共聊天室或你的小组聊天室。</p>
          </div>
          <button class="btn btn-sm btn-link text-secondary" @click="closeForwardModal">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>

        <div class="forward-preview">
          <span class="preview-label">转发内容</span>
          <strong>{{ question?.title }}</strong>
          <p>{{ forwardPreview }}</p>
        </div>

        <div class="room-list">
          <button
            class="room-option"
            :class="{ active: selectedForwardRoom.roomType === 'global' }"
            @click="selectForwardRoom('global', null)"
          >
            <span class="room-option-icon"><i class="bi bi-globe2"></i></span>
            <span class="room-option-copy">
              <strong>公共学习聊天室</strong>
              <small>适合把问题抛给所有同学一起讨论</small>
            </span>
          </button>

          <div class="room-option-group">
            <div class="room-option-title">我的小组聊天室</div>
            <div v-if="forwardRoomsLoading" class="text-muted small py-2">正在加载小组列表...</div>
            <div v-else-if="groupChatRooms.length === 0" class="text-muted small py-2">
              你还没有加入小组学习计划
            </div>
            <button
              v-for="plan in groupChatRooms"
              :key="plan.id"
              class="room-option"
              :class="{ active: selectedForwardRoom.roomType === 'plan' && Number(selectedForwardRoom.roomId) === Number(plan.id) }"
              @click="selectForwardRoom('plan', plan.id)"
            >
              <span class="room-option-icon"><i class="bi bi-people-fill"></i></span>
              <span class="room-option-copy">
                <strong>{{ plan.title }}</strong>
                <small>{{ plan.member_count || 0 }} 人 · {{ formatTime(plan.createTime || plan.created_at) }}</small>
              </span>
            </button>
          </div>
        </div>

        <div class="mb-3">
          <label class="form-label">附言（可选）</label>
          <textarea
            v-model="forwardNote"
            class="form-control"
            rows="3"
            maxlength="120"
            placeholder="比如：这个问题挺适合我们今天一起讨论"
          ></textarea>
        </div>

        <div class="forward-actions">
          <button class="btn btn-outline-secondary" @click="closeForwardModal">取消</button>
          <button class="btn btn-primary" :disabled="forwarding" @click="forwardToChatRoom">
            <span v-if="forwarding" class="spinner-border spinner-border-sm me-2"></span>
            <i v-else class="bi bi-send"></i>
            转发到聊天室
          </button>
        </div>
      </div>
    </div>

    <Footer />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import Navbar from '../../components/Navbar.vue'
import Footer from '../../components/Footer.vue'
import { renderMarkdown } from '../../utils/markdown'
import { useForumStore } from '../../stores/forum'
import { useUserStore } from '../../stores/user'
import { formatTime } from '../../utils/format'
import { showToast } from '../../utils/toast'
import TopicTag from '../../components/Forum/TopicTag.vue'
import AnswerCard from '../../components/Forum/AnswerCard.vue'
import { planApi } from '../../api/plan'
import { useChatStore } from '../../stores/chat'
import { wsManager } from '../../utils/websocket'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const forumStore = useForumStore()
const userStore = useUserStore()
const chatStore = useChatStore()

const question = computed(() => forumStore.currentQuestion)
const answerCount = computed(() => question.value?.answer_count || 0)
const questionTime = computed(() => (
  question.value?.created_at ||
  question.value?.createdAt ||
  question.value?.createTime ||
  question.value?.create_time
))
const sortBy = ref('default')
const answersLoading = ref(false)
const answerContent = ref('')
const showPreview = ref(false)
const submitting = ref(false)
const relatedQuestions = ref([])
const following = ref(false)
const favoriting = ref(false)
const followingAuthor = ref(false)
const votingQuestion = ref(false)
const showForwardModal = ref(false)
const forwardRoomsLoading = ref(false)
const forwarding = ref(false)
const groupChatRooms = ref([])
const forwardNote = ref('')
const selectedForwardRoom = ref({
  roomType: 'global',
  roomId: null
})

const renderedContent = computed(() => {
  if (!question.value?.content) return ''
  return renderMarkdown(question.value.content)
})

const previewContent = computed(() => {
  if (!answerContent.value) return ''
  return renderMarkdown(answerContent.value)
})

const forwardPreview = computed(() => {
  const title = question.value?.title || ''
  const author = question.value?.author?.username || '匿名用户'
  const content = (question.value?.content || '').replace(/[#*`\[\]()]/g, '').replace(/\s+/g, ' ').trim()
  const snippet = content.length > 72 ? `${content.slice(0, 72)}...` : content
  return `来自论坛问题《${title}》 · 作者 ${author}${snippet ? ` · ${snippet}` : ''}`
})

onMounted(() => {
  loadQuestion()
  loadAnswers()
})

// 监听路由变化，重新加载数据
watch(() => route.params.id, () => {
  loadQuestion()
  loadAnswers()
})

watch(() => route.params.id, () => {
  loadQuestion()
  loadAnswers()
})

async function loadQuestion() {
  const id = route.params.id
  if (!id) return
  const data = await forumStore.fetchQuestionDetail(id)
  await loadRelatedQuestions(data)
}

async function loadAnswers({ silent = false } = {}) {
  const id = route.params.id
  if (!id) return

  if (!silent) answersLoading.value = true
  try {
    const params = {}
    if (sortBy.value === 'time') {
      params.sort = 'created_at'
    } else if (sortBy.value === 'vote') {
      params.sort = 'vote_count'
    }
    await forumStore.fetchAnswers(id, params)
  } finally {
    if (!silent) answersLoading.value = false
  }
}

async function loadRelatedQuestions(currentQuestionData) {
  const currentId = Number(route.params.id)
  const topics = Array.isArray(currentQuestionData?.topics) ? currentQuestionData.topics : []
  if (!currentId || topics.length === 0) {
    relatedQuestions.value = []
    return
  }

  const seen = new Set([currentId])
  const related = []

  for (const topic of topics) {
    if (!topic?.id) continue
    try {
      const result = await forumStore.fetchTopicQuestions(topic.id)
      const items = Array.isArray(result) ? result : []
      for (const item of items) {
        const itemId = Number(item.id)
        if (!itemId || seen.has(itemId)) continue
        seen.add(itemId)
        related.push(item)
      }
    } catch (error) {
      console.error('加载相关问题失败:', error)
    }
  }

  relatedQuestions.value = related.slice(0, 6)
}

function changeSort(sort) {
  if (sortBy.value === sort) return
  sortBy.value = sort
  loadAnswers({ silent: true })
}

async function handleFavorite() {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  if (favoriting.value) return
  favoriting.value = true
  try {
    await forumStore.favoriteQuestion(question.value.id)
  } finally {
    favoriting.value = false
  }
}

async function handleFollow() {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  if (following.value) return
  following.value = true
  try {
    await forumStore.followQuestion(question.value.id)
  } finally {
    following.value = false
  }
}

async function handleVoteQuestion() {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  if (votingQuestion.value || !question.value) return
  votingQuestion.value = true
  try {
    const result = await forumStore.voteQuestion(question.value.id)
    if (result && question.value) {
      question.value.is_voted = result.is_voted !== undefined ? result.is_voted : !question.value.is_voted
      question.value.vote_count = result.vote_count !== undefined ? result.vote_count : question.value.vote_count
    }
  } catch (error) {
    console.error('点赞失败:', error)
    showToast(t('errors.unknown'), 'error')
  } finally {
    votingQuestion.value = false
  }
}

async function openForwardModal() {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }

  showForwardModal.value = true
  forwardNote.value = ''
  selectedForwardRoom.value = { roomType: 'global', roomId: null }
  await loadForwardRooms()
}

function closeForwardModal() {
  showForwardModal.value = false
  forwardNote.value = ''
}

function selectForwardRoom(roomType, roomId) {
  selectedForwardRoom.value = { roomType, roomId }
}

async function loadForwardRooms() {
  forwardRoomsLoading.value = true
  try {
    const result = await planApi.getMyGroupPlans()
    if (result?.code === 200) {
      groupChatRooms.value = Array.isArray(result.data) ? result.data : []
    } else {
      groupChatRooms.value = []
    }
  } catch (error) {
    console.error('加载聊天室列表失败:', error)
    groupChatRooms.value = []
  } finally {
    forwardRoomsLoading.value = false
  }
}

async function ensureChatConnected() {
  if (chatStore.isConnected) {
    return true
  }

  chatStore.initConnection()

  return await new Promise((resolve) => {
    const startedAt = Date.now()
    const timer = window.setInterval(() => {
      if (chatStore.isConnected) {
        window.clearInterval(timer)
        resolve(true)
        return
      }

      if (Date.now() - startedAt > 5000) {
        window.clearInterval(timer)
        resolve(false)
      }
    }, 200)
  })
}

function buildForwardMessage() {
  const author = question.value?.author?.username || '匿名用户'
  const title = question.value?.title || '未命名问题'
  const path = router.resolve(`/forum/question/${question.value?.id}`).href
  const absoluteUrl = typeof window !== 'undefined'
    ? `${window.location.origin}${path}`
    : path
  const note = forwardNote.value.trim()
  const summary = (question.value?.content || '')
    .replace(/[#*`\[\]()]/g, '')
    .replace(/\s+/g, ' ')
    .trim()

  const parts = [
    note ? `【附言】${note}` : '',
    `【论坛转发】${title}`,
    `作者：${author}`,
    summary ? `摘要：${summary.length > 100 ? `${summary.slice(0, 100)}...` : summary}` : '',
    `链接：${absoluteUrl}`
  ].filter(Boolean)

  return parts.join('\n')
}

async function forwardToChatRoom() {
  if (!question.value?.id || forwarding.value) {
    return
  }

  forwarding.value = true
  try {
    const connected = await ensureChatConnected()
    if (!connected) {
      showToast('聊天室连接失败，请稍后重试', 'error')
      return
    }

    const success = wsManager.sendMessage(buildForwardMessage(), {
      roomType: selectedForwardRoom.value.roomType,
      roomId: selectedForwardRoom.value.roomType === 'plan' ? selectedForwardRoom.value.roomId : null
    })

    if (!success) {
      return
    }

    showToast('已转发到学习聊天室', 'success')
    closeForwardModal()
  } catch (error) {
    console.error('转发到聊天室失败:', error)
    showToast('转发失败，请稍后重试', 'error')
  } finally {
    forwarding.value = false
  }
}

async function handleFollowAuthor() {
  if (!userStore.isLoggedIn || !question.value?.author) {
    return
  }
  if (followingAuthor.value) return
  followingAuthor.value = true
  try {
    const result = await forumStore.followUser(question.value.author.id)
    if (result && question.value.author) {
      question.value.author.is_following = result.is_following
    }
  } finally {
    followingAuthor.value = false
  }
}

async function handleVote(answerId) {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  try {
    const result = await forumStore.voteAnswer(answerId)
    return result
  } catch (error) {
    console.error('点赞失败:', error)
    return null
  }
}

async function handleCollect(answerId) {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  await forumStore.collectAnswer(answerId)
  loadAnswers()
}

function handleEditAnswer(answer) {
  answerContent.value = answer.content
  // 可以滚动到编辑器
}

async function handleDeleteAnswer(answerId) {
  if (!confirm(t('forum.answer.confirmDelete'))) return
  
  try {
    const { answerApi } = await import('../../api/forum')
    const response = await answerApi.deleteAnswer(answerId)
    if (response.code === 200) {
      showToast(t('common.success'), 'success')
      // 重新加载回答列表和问题详情（更新回答数）
      await loadAnswers()
      await loadQuestion()
    } else {
      showToast(response.message || t('errors.unknown'), 'error')
    }
  } catch (error) {
    console.error('删除失败:', error)
    showToast(t('errors.unknown'), 'error')
  }
}

async function submitAnswer() {
  if (!answerContent.value.trim()) return
  
  submitting.value = true
  try {
    const result = await forumStore.createAnswer({
      question_id: question.value.id,
      content: answerContent.value
    })
    
    if (result) {
      answerContent.value = ''
      showPreview.value = false
      loadAnswers()
      // 更新问题回答数
      if (question.value) {
        question.value.answer_count = (question.value.answer_count || 0) + 1
      }
    }
  } finally {
    submitting.value = false
  }
}

function saveDraft() {
  localStorage.setItem(`draft_answer_${question.value.id}`, answerContent.value)
  showToast(t('common.saveDraft') + ' ' + t('common.success'), 'success')
}
</script>

<style scoped>
.question-content {
  line-height: 1.8;
}

.question-content :deep(h1),
.question-content :deep(h2),
.question-content :deep(h3) {
  margin-top: 1.5em;
  margin-bottom: 0.5em;
}

.question-content :deep(code) {
  background-color: #f5f5f5;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 0.9em;
}

.question-content :deep(pre) {
  background-color: rgba(23, 32, 51, 0.06);
  padding: 1em;
  border-radius: 4px;
  overflow-x: auto;
}

.question-content :deep(blockquote) {
  border-left: 4px solid rgba(62, 141, 247, 0.24);
  padding-left: 1em;
  margin-left: 0;
  color: var(--sp-muted);
}

.btn-group .btn {
  transition: none;
}

.btn-group .btn:focus,
.btn-group .btn:active {
  box-shadow: none;
  outline: none;
}

.btn-group .btn.active {
  background: linear-gradient(135deg, #72c4ff 0%, #3e93f5 100%);
  border-color: #63b8ff;
  color: white;
  box-shadow: 0 0 0 4px rgba(99, 184, 255, 0.18);
}

.forward-modal-backdrop {
  align-items: center;
  background: rgba(15, 23, 42, 0.34);
  display: flex;
  inset: 0;
  justify-content: center;
  padding: 1rem;
  position: fixed;
  z-index: 1100;
}

.forward-modal {
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 24px 56px rgba(26, 40, 61, 0.2);
  max-height: calc(100vh - 2rem);
  max-width: 640px;
  overflow-y: auto;
  padding: 1.25rem;
  width: 100%;
}

.forward-modal-header {
  align-items: flex-start;
  display: flex;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.forward-preview {
  background: rgba(245, 249, 255, 0.92);
  border: 1px solid rgba(93, 126, 168, 0.14);
  border-radius: 16px;
  margin-bottom: 1rem;
  padding: 0.95rem 1rem;
}

.preview-label,
.room-option-title {
  color: #6f8096;
  display: block;
  font-size: 0.86rem;
  font-weight: 700;
  margin-bottom: 0.45rem;
}

.forward-preview strong {
  color: #213658;
  display: block;
  margin-bottom: 0.4rem;
}

.forward-preview p {
  color: #627993;
  line-height: 1.7;
  margin: 0;
}

.room-list {
  display: grid;
  gap: 0.85rem;
  margin-bottom: 1rem;
}

.room-option-group {
  display: grid;
  gap: 0.75rem;
}

.room-option {
  align-items: flex-start;
  background: rgba(248, 251, 255, 0.96);
  border: 1px solid rgba(93, 126, 168, 0.14);
  border-radius: 16px;
  cursor: pointer;
  display: flex;
  gap: 0.75rem;
  padding: 0.9rem 1rem;
  text-align: left;
  transition: all 0.2s ease;
  width: 100%;
}

.room-option:hover,
.room-option.active {
  background: rgba(62, 141, 247, 0.1);
  border-color: rgba(62, 141, 247, 0.34);
}

.room-option-icon {
  align-items: center;
  background: rgba(62, 141, 247, 0.14);
  border-radius: 12px;
  color: #2d79da;
  display: inline-flex;
  flex: 0 0 40px;
  height: 40px;
  justify-content: center;
}

.room-option-copy {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  min-width: 0;
}

.room-option-copy strong {
  color: #213658;
}

.room-option-copy small {
  color: #6f8096;
}

.forward-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}
</style>
