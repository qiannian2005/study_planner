<template>
  <div class="ai-assistant-page app-shell">
    <Navbar />

    <main class="container page-surface my-5">
      <div class="assistant-layout">
        <section class="assistant-main">
          <div class="assistant-header">
            <div>
              <p class="assistant-kicker">{{ t('aiAssistant.kicker') }}</p>
              <h1><i class="bi bi-stars"></i> {{ t('aiAssistant.title') }}</h1>
            </div>
            <button
              type="button"
              class="btn btn-outline-secondary btn-sm"
              :disabled="contextLoading"
              @click="refreshAssistant"
            >
              <i class="bi bi-arrow-clockwise"></i>
              {{ t('aiAssistant.refreshToday') }}
            </button>
          </div>

          <div class="chat-container" ref="chatContainer">
            <div
              v-for="(msg, index) in messages"
              :key="index"
              :class="['chat-message', msg.role === 'user' ? 'user' : 'ai', 'd-flex']"
              :style="msg.role === 'user' ? 'justify-content: flex-end' : ''"
            >
              <div>
                <div v-if="msg.role === 'assistant'" class="small text-muted mb-1">
                  <i class="bi bi-mortarboard"></i> {{ t('aiAssistant.title') }}
                </div>
                <div
                  v-if="msg.role === 'assistant'"
                  class="ai-response-content markdown-content"
                  v-html="renderMarkdown(msg.content)"
                ></div>
                <div v-else>{{ msg.content }}</div>
              </div>
            </div>

            <div v-if="loading" class="chat-message ai">
              <div class="loading-spinner"></div>
              <span class="ms-2">{{ t('aiAssistant.thinkingWithPlan') }}</span>
            </div>
          </div>

          <form @submit.prevent="sendMessage" class="assistant-input">
            <div class="input-group">
              <input
                type="text"
                class="form-control"
                v-model="userInput"
                :placeholder="t('aiAssistant.inputPlaceholder')"
                :disabled="loading"
                required
              />
              <button
                type="submit"
                class="btn btn-primary"
                :disabled="loading || !userInput.trim()"
              >
                <i class="bi bi-send"></i> {{ t('aiAssistant.send') }}
              </button>
            </div>
          </form>

          <div class="suggestion-row">
            <button
              v-for="question in suggestedQuestions"
              :key="question"
              class="btn btn-sm btn-outline-secondary"
              type="button"
              @click="askQuestion(question)"
              :disabled="loading"
            >
              {{ question }}
            </button>
          </div>
        </section>

        <aside class="assistant-context-panel">
          <div class="context-head">
            <div>
              <p class="assistant-kicker">{{ t('aiAssistant.contextKicker') }}</p>
              <h2>{{ t('aiAssistant.todayStatus') }}</h2>
            </div>
            <span v-if="contextLoading" class="status-pill muted">
              <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
              {{ t('aiAssistant.loadingContext') }}
            </span>
            <span v-else class="status-pill">{{ completionLabel }}</span>
          </div>

          <div class="stats-grid">
            <div>
              <strong>{{ assistantContext.activePlans.length }}</strong>
              <span>{{ t('aiAssistant.activePlans') }}</span>
            </div>
            <div>
              <strong>{{ assistantContext.todayTasks.length }}</strong>
              <span>{{ t('aiAssistant.todayTasks') }}</span>
            </div>
            <div>
              <strong>{{ completedTodayCount }}</strong>
              <span>{{ t('aiAssistant.completed') }}</span>
            </div>
            <div>
              <strong>{{ averageProgress }}%</strong>
              <span>{{ t('aiAssistant.averageProgress') }}</span>
            </div>
          </div>

          <div v-if="primaryTask" class="priority-block">
            <span class="block-label">{{ t('aiAssistant.suggestedFirst') }}</span>
            <h3>{{ primaryTask.content }}</h3>
            <p>{{ t('aiAssistant.taskMeta', { plan: primaryTask.planTitle, day: primaryTask.dayNumber || '-', hours: formatHours(primaryTask.duration) }) }}</p>
            <button
              type="button"
              class="btn btn-primary btn-sm"
              :disabled="loading"
              @click="askQuestion(primaryBreakdownQuestion)"
            >
              <i class="bi bi-list-check"></i>
              {{ t('aiAssistant.breakIntoSteps') }}
            </button>
          </div>

          <div v-else class="empty-context">
            <i class="bi bi-calendar2-check"></i>
            <p>{{ assistantContext.plans.length ? t('aiAssistant.noScheduledTask') : t('aiAssistant.noPlansHint') }}</p>
          </div>

          <div v-if="assistantContext.overdueTasks.length" class="overdue-list">
            <button type="button" class="section-title overdue collapsible" @click="overdueCollapsed = !overdueCollapsed">
              <i class="bi bi-exclamation-triangle"></i>
              <span>{{ t('aiAssistant.overdueTaskList', { count: assistantContext.overdueTasks.length }) }}</span>
              <i class="bi collapse-chevron" :class="overdueCollapsed ? 'bi-chevron-down' : 'bi-chevron-up'"></i>
            </button>
            <div
              v-show="!overdueCollapsed"
              v-for="task in assistantContext.overdueTasks"
              :key="`overdue-${task.planId}-${task.id}`"
              class="today-task-row overdue-row"
            >
              <span class="task-state">
                <i class="bi bi-clock-history"></i>
              </span>
              <div>
                <strong>{{ task.content }}</strong>
                <small>{{ t('aiAssistant.overdueMeta', { plan: task.planTitle, date: task.scheduledDate, hours: formatHours(task.duration) }) }}</small>
              </div>
            </div>
          </div>

          <div class="today-list">
            <button type="button" class="section-title collapsible" @click="todayCollapsed = !todayCollapsed">
              <i class="bi bi-calendar-event"></i>
              <span>{{ t('aiAssistant.todayTaskList') }}</span>
              <i class="bi collapse-chevron" :class="todayCollapsed ? 'bi-chevron-down' : 'bi-chevron-up'"></i>
            </button>
            <template v-if="!todayCollapsed">
              <div
                v-for="task in assistantContext.todayTasks"
                :key="`${task.planId}-${task.id}`"
                class="today-task-row"
                :class="{ done: isTaskCompleted(task) }"
              >
                <span class="task-state">
                  <i class="bi" :class="isTaskCompleted(task) ? 'bi-check-circle-fill' : 'bi-circle'"></i>
                </span>
                <div>
                  <strong>{{ task.content }}</strong>
                  <small>{{ t('aiAssistant.taskHoursMeta', { plan: task.planTitle, hours: formatHours(task.duration) }) }}</small>
                </div>
              </div>
              <p v-if="!assistantContext.todayTasks.length" class="muted-text">{{ t('aiAssistant.noTodayTasks') }}</p>
            </template>
          </div>
        </aside>
      </div>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import Navbar from '../components/Navbar.vue'
import Footer from '../components/Footer.vue'
import { aiApi } from '../api/ai'
import { planApi } from '../api/plan'
import { useUserStore } from '../stores/user'
import { showToast } from '../utils/toast'
import { renderMarkdown } from '../utils/markdown'

const { t, locale } = useI18n()
const { user } = useUserStore()

const HISTORY_KEY = 'studyAssistantChatHistory'
const CONTEXT_KEY = 'studyAssistantContextSignature'

const messages = ref([])
const userInput = ref('')
const loading = ref(false)
const contextLoading = ref(false)
const chatContainer = ref(null)
const overdueCollapsed = ref(false)
const todayCollapsed = ref(false)
const assistantContext = ref({
  plans: [],
  activePlans: [],
  todayTasks: [],
  overdueTasks: [],
  completedPlans: 0
})

const primaryTask = computed(() => {
  return assistantContext.value.todayTasks.find((task) => !isTaskCompleted(task))
    || assistantContext.value.todayTasks[0]
    || null
})

const completedTodayCount = computed(() =>
  assistantContext.value.todayTasks.filter((task) => isTaskCompleted(task)).length
)

const averageProgress = computed(() => {
  const plans = assistantContext.value.activePlans
  if (!plans.length) return 0
  const total = plans.reduce((sum, plan) => sum + Number(plan.progress || 0), 0)
  return Math.round(total / plans.length)
})

const completionLabel = computed(() => {
  const total = assistantContext.value.todayTasks.length
  if (!total) return t('aiAssistant.noTaskTodayShort')
  return t('aiAssistant.completionLabel', { completed: completedTodayCount.value, total })
})

const primaryBreakdownQuestion = computed(() => {
  return t('aiAssistant.questions.breakdown')
})

const suggestedQuestions = computed(() => {
  const hasOverdue = assistantContext.value.overdueTasks.length > 0
  const overdueQuestion = t('aiAssistant.questions.overdue')
  const base = [
    primaryBreakdownQuestion.value,
    t('aiAssistant.questions.priority'),
    t('aiAssistant.questions.compress')
  ]
  if (!primaryTask.value) {
    const fallback = [
      t('aiAssistant.questions.planFocus'),
      t('aiAssistant.questions.adjustment'),
      t('aiAssistant.questions.review')
    ]
    return hasOverdue ? [overdueQuestion, ...fallback] : fallback
  }
  return hasOverdue ? [overdueQuestion, ...base] : base
})

onMounted(async () => {
  await refreshAssistant()
})

async function refreshAssistant() {
  contextLoading.value = true
  try {
    await loadStudyContext()
    initializeConversation()
  } catch (error) {
    console.error('加载学习助手上下文失败:', error)
    showToast(t('aiAssistant.loadContextFailed'), 'error')
    initializeConversation()
  } finally {
    contextLoading.value = false
    scrollToBottom()
  }
}

async function loadStudyContext() {
  const planResult = await planApi.getPlans()
  const plans = Array.isArray(planResult?.data) ? planResult.data : []
  const activePlans = plans.filter((plan) => isActivePlan(plan))
  const todayResults = await Promise.allSettled(
    activePlans.map(async (plan) => {
      const result = await planApi.getTodayTask(plan.id)
      if (result?.code !== 200 || !result.data) return null
      return {
        ...result.data,
        planId: plan.id,
        planTitle: plan.title,
        planGoal: plan.goal,
        planProgress: plan.progress,
        planStatus: plan.status
      }
    })
  )

  const overdueResults = await Promise.allSettled(
    activePlans.map((plan) => collectOverdueTasks(plan))
  )

  assistantContext.value = {
    plans,
    activePlans,
    todayTasks: todayResults
      .filter((item) => item.status === 'fulfilled' && item.value)
      .map((item) => item.value),
    overdueTasks: overdueResults
      .filter((item) => item.status === 'fulfilled' && Array.isArray(item.value))
      .flatMap((item) => item.value),
    completedPlans: plans.filter((plan) => isCompletedPlan(plan)).length
  }
}

// 读取某个计划的全部明细，筛出排期早于今天且尚未完成的逾期任务
async function collectOverdueTasks(plan) {
  const result = await planApi.getPlanDetail(plan.id)
  const details = Array.isArray(result?.data?.details) ? result.data.details : []
  const today = new Date().toISOString().slice(0, 10)

  return details
    .filter((detail) => {
      if (isTaskCompleted(detail)) return false
      const scheduled = detail.scheduledDate ? String(detail.scheduledDate).slice(0, 10) : ''
      return scheduled && scheduled < today
    })
    .map((detail) => ({
      ...detail,
      planId: plan.id,
      planTitle: plan.title,
      scheduledDate: detail.scheduledDate ? String(detail.scheduledDate).slice(0, 10) : ''
    }))
    .sort((a, b) => a.scheduledDate.localeCompare(b.scheduledDate))
}

function initializeConversation() {
  const signature = buildContextSignature()
  const savedSignature = localStorage.getItem(CONTEXT_KEY)
  const savedHistory = localStorage.getItem(HISTORY_KEY)

  if (savedHistory && savedSignature === signature) {
    try {
      const parsed = JSON.parse(savedHistory)
      if (Array.isArray(parsed) && parsed.length) {
        messages.value = parsed
        return
      }
    } catch (error) {
      console.warn('读取学习助手历史失败:', error)
    }
  }

  messages.value = [{
    role: 'assistant',
    content: buildOpeningMessage()
  }]
  localStorage.setItem(CONTEXT_KEY, signature)
  persistMessages()
}

async function sendMessage() {
  if (!userInput.value.trim()) return

  const question = userInput.value.trim()
  userInput.value = ''
  messages.value.push({ role: 'user', content: question })
  persistMessages()
  scrollToBottom()
  loading.value = true

  try {
    const result = await aiApi.chat([
      {
        role: 'system',
        content: buildSystemPrompt()
      },
      ...messages.value
    ])

    if (result && result.code === 200) {
      const aiResponse = result.data?.content || result.data || t('aiAssistant.cannotUnderstand')
      messages.value.push({
        role: 'assistant',
        content: aiResponse
      })
      persistMessages()
      scrollToBottom()
    } else {
      const errorMsg = result?.message || t('aiAssistant.requestFailed')
      showToast(errorMsg, 'error')
      removeLastUserMessage()
    }
  } catch (error) {
    console.error('发送学习助手消息失败:', error)
    showToast(t('aiAssistant.networkError', { message: error.message || t('errors.unknown') }), 'error')
    removeLastUserMessage()
  } finally {
    loading.value = false
  }
}

function askQuestion(question) {
  userInput.value = question
  sendMessage()
}

function buildOpeningMessage() {
  const now = new Date()
  const dateStr = new Intl.DateTimeFormat(locale.value === 'en-US' ? 'en-US' : 'zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  }).format(now)
  const name = user?.username || t('aiAssistant.defaultStudent')
  const tasks = assistantContext.value.todayTasks
  const overdue = assistantContext.value.overdueTasks

  let overdueBlock = ''
  if (overdue.length) {
    const overdueLines = overdue.map((task, index) =>
      t('aiAssistant.openingOverdueLine', {
        index: index + 1,
        plan: task.planTitle,
        content: task.content,
        date: task.scheduledDate || '-'
      })
    ).join('\n')
    overdueBlock = t('aiAssistant.openingOverdueBlock', { count: overdue.length, tasks: overdueLines })
  }

  if (tasks.length) {
    const taskLines = tasks.map((task, index) =>
      t('aiAssistant.openingTaskLine', { index: index + 1, plan: task.planTitle, content: task.content })
    ).join('\n')
    return t('aiAssistant.openingWithTasks', { name, date: dateStr, count: tasks.length, tasks: taskLines }) + overdueBlock
  }

  if (assistantContext.value.activePlans.length) {
    return t('aiAssistant.openingWithPlans', { name, date: dateStr, count: assistantContext.value.activePlans.length }) + overdueBlock
  }

  return t('aiAssistant.openingEmpty', { name, date: dateStr })
}

function buildSystemPrompt() {
  const context = assistantContext.value
  const taskLines = context.todayTasks.map((task, index) =>
    t('aiAssistant.prompt.taskLine', {
      index: index + 1,
      plan: task.planTitle,
      day: task.dayNumber || '-',
      content: task.content,
      hours: formatHours(task.duration),
      status: isTaskCompleted(task) ? t('aiAssistant.completed') : t('aiAssistant.pending')
    })
  )
  const planLines = context.activePlans.map((plan, index) =>
    t('aiAssistant.prompt.planLine', {
      index: index + 1,
      title: plan.title,
      goal: plan.goal || t('aiAssistant.notFilled'),
      progress: Math.round(Number(plan.progress || 0)),
      status: plan.status || t('aiAssistant.unknown')
    })
  )
  const overdueLines = context.overdueTasks.map((task, index) =>
    t('aiAssistant.prompt.overdueLine', {
      index: index + 1,
      plan: task.planTitle,
      date: task.scheduledDate || '-',
      content: task.content,
      hours: formatHours(task.duration)
    })
  )

  return t('aiAssistant.prompt.system', {
    activeCount: context.activePlans.length,
    completedPlans: context.completedPlans,
    completedToday: completedTodayCount.value,
    todayTotal: context.todayTasks.length,
    overdueTotal: context.overdueTasks.length,
    planLines: planLines.length ? planLines.join('\n') : t('common.none'),
    taskLines: taskLines.length ? taskLines.join('\n') : t('common.none'),
    overdueLines: overdueLines.length ? overdueLines.join('\n') : t('common.none')
  })
}

function buildContextSignature() {
  const today = new Date().toISOString().slice(0, 10)
  const tasks = assistantContext.value.todayTasks
    .map((task) => `${task.planId}:${task.id}:${task.isCompleted}`)
    .join('|')
  const overdue = assistantContext.value.overdueTasks
    .map((task) => `${task.planId}:${task.id}`)
    .join('|')
  return `${today}|${assistantContext.value.activePlans.length}|${tasks}|${overdue}`
}

function isActivePlan(plan) {
  if (!plan) return false
  if (isCompletedPlan(plan)) return false
  const status = String(plan.status || '').toLowerCase()
  return !['abandoned', 'cancelled', 'deleted'].includes(status)
}

function isCompletedPlan(plan) {
  const status = String(plan?.status || '').toLowerCase()
  return status === 'completed' || status === '已完成' || Number(plan?.progress || 0) >= 100
}

function isTaskCompleted(task) {
  return task?.isCompleted === true || Number(task?.isCompleted || 0) === 1
}

function formatHours(value) {
  const number = Number(value || 0)
  if (!Number.isFinite(number)) return '0'
  return Number.isInteger(number) ? String(number) : number.toFixed(1)
}

function persistMessages() {
  localStorage.setItem(HISTORY_KEY, JSON.stringify(messages.value))
}

function removeLastUserMessage() {
  if (messages.value.length > 0 && messages.value[messages.value.length - 1].role === 'user') {
    messages.value.pop()
    persistMessages()
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}
</script>

<style scoped>
.assistant-layout {
  align-items: start;
  display: grid;
  gap: 22px;
  grid-template-columns: minmax(0, 1fr) 360px;
}

.assistant-main,
.assistant-context-panel {
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(87, 150, 231, 0.18);
  border-radius: 8px;
  box-shadow: 0 18px 46px rgba(45, 84, 132, 0.12);
  padding: 22px;
}

.assistant-context-panel {
  max-height: calc(100vh - 120px);
  overflow-y: auto;
  position: sticky;
  top: 20px;
}

.assistant-main {
  display: flex;
  flex-direction: column;
}

.assistant-header,
.context-head {
  align-items: center;
  display: flex;
  gap: 16px;
  justify-content: space-between;
  margin-bottom: 18px;
}

.assistant-kicker {
  color: #55708f;
  font-size: 0.74rem;
  font-weight: 800;
  letter-spacing: 0;
  margin: 0 0 5px;
  text-transform: uppercase;
}

.assistant-header h1,
.context-head h2 {
  color: var(--sp-ink);
  font-size: 1.35rem;
  font-weight: 850;
  margin: 0;
}

.assistant-header h1 {
  align-items: center;
  display: flex;
  gap: 8px;
}

.ai-assistant-page .chat-container {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.5), rgba(230, 242, 255, 0.4));
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 8px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.64);
  flex: 1 1 auto;
  min-height: 460px;
}

.ai-assistant-page .chat-message {
  box-shadow: 0 10px 24px rgba(54, 83, 116, 0.1);
}

.ai-assistant-page .chat-message.user {
  align-items: center;
  background: linear-gradient(135deg, #2478f2 0%, #0f6ae8 100%);
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-bottom-right-radius: 7px;
  border-radius: 18px 18px 7px 18px;
  box-shadow: 0 12px 28px rgba(15, 106, 232, 0.22);
  color: #fff;
  line-height: 1.65;
  margin-left: auto;
  max-width: min(620px, 86%);
  padding: 12px 16px;
  text-align: left;
  width: fit-content;
  word-break: break-word;
}

.ai-assistant-page .chat-message.ai {
  background: rgba(255, 255, 255, 0.76);
  border-color: rgba(255, 255, 255, 0.8);
}

.assistant-input {
  margin-top: 16px;
}

.suggestion-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.status-pill {
  align-items: center;
  background: rgba(47, 111, 197, 0.1);
  border: 1px solid rgba(47, 111, 197, 0.18);
  border-radius: 999px;
  color: #2f6fc5;
  display: inline-flex;
  font-size: 0.82rem;
  font-weight: 750;
  gap: 6px;
  min-height: 30px;
  padding: 5px 10px;
  white-space: nowrap;
}

.status-pill.muted,
.muted-text {
  color: var(--sp-muted);
}

.stats-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-bottom: 16px;
}

.stats-grid > div,
.priority-block,
.today-task-row,
.empty-context {
  background: rgba(247, 251, 255, 0.78);
  border: 1px solid rgba(87, 150, 231, 0.14);
  border-radius: 8px;
}

.stats-grid > div {
  padding: 12px;
}

.stats-grid strong {
  color: #1f5fa8;
  display: block;
  font-size: 1.35rem;
  line-height: 1.1;
}

.stats-grid span {
  color: var(--sp-muted);
  display: block;
  font-size: 0.82rem;
  margin-top: 4px;
}

.priority-block {
  margin-bottom: 18px;
  padding: 16px;
}

.block-label,
.section-title {
  color: #55708f;
  font-size: 0.82rem;
  font-weight: 800;
}

.priority-block h3 {
  color: var(--sp-ink);
  font-size: 1rem;
  font-weight: 850;
  line-height: 1.45;
  margin: 8px 0;
}

.priority-block p {
  color: var(--sp-muted);
  font-size: 0.9rem;
  margin-bottom: 12px;
}

.section-title {
  align-items: center;
  display: flex;
  gap: 7px;
  margin-bottom: 10px;
}

.section-title.collapsible {
  background: transparent;
  border: 0;
  cursor: pointer;
  padding: 0;
  text-align: left;
  width: 100%;
}

.section-title.collapsible:hover {
  opacity: 0.78;
}

.section-title .collapse-chevron {
  color: inherit;
  font-size: 0.78rem;
  margin-left: auto;
  opacity: 0.7;
}

.today-list {
  display: grid;
  gap: 9px;
}

.today-task-row {
  align-items: flex-start;
  display: flex;
  gap: 10px;
  padding: 11px;
}

.today-task-row.done {
  opacity: 0.72;
}

.task-state {
  color: #2f6fc5;
  line-height: 1.2;
}

.today-task-row strong {
  color: var(--sp-ink);
  display: block;
  font-size: 0.92rem;
  line-height: 1.45;
}

.today-task-row small {
  color: var(--sp-muted);
  display: block;
  margin-top: 4px;
}

.overdue-list {
  display: grid;
  gap: 9px;
  margin-bottom: 18px;
}

.section-title.overdue {
  color: #c2410c;
}

.today-task-row.overdue-row {
  background: rgba(255, 244, 237, 0.92);
  border-color: rgba(234, 130, 70, 0.3);
}

.today-task-row.overdue-row .task-state {
  color: #d9772e;
}

.empty-context {
  align-items: flex-start;
  color: var(--sp-muted);
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
  padding: 14px;
}

.empty-context i {
  color: #2f6fc5;
  font-size: 1.2rem;
}

.empty-context p {
  margin: 0;
}

.ai-response-content {
  line-height: 1.8;
}

.ai-response-content :deep(h1),
.ai-response-content :deep(h2),
.ai-response-content :deep(h3),
.ai-response-content :deep(h4),
.ai-response-content :deep(h5),
.ai-response-content :deep(h6) {
  font-weight: 650;
  margin-bottom: 0.5em;
  margin-top: 1.5em;
}

.ai-response-content :deep(p) {
  margin-bottom: 1em;
}

.ai-response-content :deep(code) {
  background-color: rgba(62, 141, 247, 0.08);
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
  padding: 2px 6px;
}

.ai-response-content :deep(pre) {
  background-color: rgba(23, 32, 51, 0.06);
  border-radius: 4px;
  margin: 1em 0;
  overflow-x: auto;
  padding: 1em;
}

.ai-response-content :deep(pre code) {
  background-color: transparent;
  padding: 0;
}

.ai-response-content :deep(blockquote) {
  border-left: 4px solid rgba(62, 141, 247, 0.24);
  color: var(--sp-muted);
  font-style: italic;
  margin-left: 0;
  padding-left: 1em;
}

.ai-response-content :deep(ul),
.ai-response-content :deep(ol) {
  margin: 1em 0;
  padding-left: 2em;
}

.ai-response-content :deep(li) {
  margin: 0.5em 0;
}

.ai-response-content :deep(a) {
  color: #2f6fc5;
  text-decoration: none;
}

.ai-response-content :deep(a:hover) {
  text-decoration: underline;
}

.ai-response-content :deep(table) {
  border-collapse: collapse;
  margin: 1em 0;
  width: 100%;
}

.ai-response-content :deep(th),
.ai-response-content :deep(td) {
  border: 1px solid rgba(80, 116, 152, 0.12);
  padding: 8px;
  text-align: left;
}

.ai-response-content :deep(th) {
  background-color: rgba(62, 141, 247, 0.08);
  font-weight: 650;
}

@media (max-width: 991.98px) {
  .assistant-layout {
    grid-template-columns: 1fr;
  }

  .assistant-main,
  .assistant-context-panel {
    padding: 16px;
  }
}

@media (max-width: 575.98px) {
  .assistant-header,
  .context-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}

[data-bs-theme='dark'] .assistant-main,
[data-bs-theme='dark'] .assistant-context-panel,
[data-bs-theme='dark'] .stats-grid > div,
[data-bs-theme='dark'] .priority-block,
[data-bs-theme='dark'] .today-task-row,
[data-bs-theme='dark'] .empty-context {
  background: rgba(15, 24, 38, 0.78);
  border-color: rgba(188, 213, 241, 0.14);
  color: #edf5ff;
}

[data-bs-theme='dark'] .assistant-header h1,
[data-bs-theme='dark'] .context-head h2,
[data-bs-theme='dark'] .priority-block h3,
[data-bs-theme='dark'] .today-task-row strong {
  color: #edf5ff;
}

[data-bs-theme='dark'] .assistant-kicker,
[data-bs-theme='dark'] .block-label,
[data-bs-theme='dark'] .section-title,
[data-bs-theme='dark'] .priority-block p,
[data-bs-theme='dark'] .today-task-row small {
  color: #a9bdd4;
}

[data-bs-theme='dark'] .today-task-row.overdue-row {
  background: rgba(60, 34, 22, 0.6);
  border-color: rgba(234, 130, 70, 0.34);
}

[data-bs-theme='dark'] .section-title.overdue {
  color: #f0a675;
}

[data-bs-theme='dark'] .ai-assistant-page .chat-container {
  background: rgba(17, 24, 34, 0.62);
  border-color: rgba(255, 255, 255, 0.1);
}

[data-bs-theme='dark'] .ai-assistant-page .chat-message.ai {
  background: rgba(31, 41, 55, 0.84);
  border-color: rgba(255, 255, 255, 0.1);
}

[data-bs-theme='dark'] .ai-assistant-page .chat-message.user {
  background: linear-gradient(135deg, #3a8cff 0%, #1e6fe8 100%);
  box-shadow: 0 14px 30px rgba(0, 0, 0, 0.26);
}
</style>
