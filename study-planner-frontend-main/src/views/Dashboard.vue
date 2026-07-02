<template>
  <div class="dashboard-page app-shell">
    <Navbar />
    
    <main class="container page-surface my-4">
      <!-- 欢迎信息 -->
      <div class="dashboard-hero mb-4">
        <div class="dashboard-heading">
          <p class="page-kicker">Study cockpit</p>
          <h1 class="page-title">{{ $t('dashboard.welcomeBack', { name: user?.username }) }}</h1>
          <p class="page-lead">{{ $t('dashboard.motivation') }}</p>
        </div>
      </div>

      <!-- 统计卡片 -->
      <div class="row g-4 mb-4">
        <div class="col-md-3">
          <div class="card stat-card primary h-100">
            <div class="card-body">
              <div class="stat-card-content">
                <div>
                  <p class="text-muted mb-1">{{ $t('dashboard.activePlans') }}</p>
                  <h3 class="mb-0">{{ stats.activePlans || 0 }}</h3>
                </div>
                <span class="stat-icon"><i class="bi bi-journal-text"></i></span>
              </div>
            </div>
          </div>
        </div>
        <div class="col-md-3">
          <div class="card stat-card success h-100">
            <div class="card-body">
              <div class="stat-card-content">
                <div>
                  <p class="text-muted mb-1">{{ $t('dashboard.totalCheckIns') }}</p>
                  <h3 class="mb-0">{{ stats.totalDays || 0 }} <small class="text-muted">{{ $t('dashboard.days') }}</small></h3>
                </div>
                <span class="stat-icon"><i class="bi bi-calendar-check"></i></span>
              </div>
            </div>
          </div>
        </div>
        <div class="col-md-3">
          <div class="card stat-card warning h-100">
            <div class="card-body">
              <div class="stat-card-content">
                <div>
                  <p class="text-muted mb-1">{{ $t('dashboard.streakDays') }}</p>
                  <h3 class="mb-0">{{ stats.streakDays || 0 }} <small class="text-muted">{{ $t('dashboard.days') }}</small></h3>
                </div>
                <span class="stat-icon"><i class="bi bi-fire"></i></span>
              </div>
            </div>
          </div>
        </div>
        <div class="col-md-3">
          <div class="card stat-card info h-100">
            <div class="card-body">
              <div class="stat-card-content">
                <div>
                  <p class="text-muted mb-1">{{ $t('dashboard.myStudyPlans') }}</p>
                  <h3 class="mb-0">{{ plans.length }}</h3>
                </div>
                <span class="stat-icon"><i class="bi bi-collection"></i></span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 学习进度图表 -->
      <div class="row mb-4">
        <div class="col-md-8">
          <StudyChart :chart-data="chartData" />
        </div>
        <div class="col-md-4">
          <CheckinHeatmap :calendar-data="heatmapData" @month-change="onHeatmapMonthChange" />
        </div>
      </div>

      <div class="row g-4">
        <!-- 今日任务 -->
        <!-- 快捷操作 -->
        <div class="col-md-8">
          <div class="card today-task h-100">
            <div class="card-body">
              <h5 class="card-title"><i class="bi bi-star-fill"></i> {{ $t('dashboard.todayTask') }}</h5>
              <div v-if="loading" class="text-center py-4">
                <div class="spinner-border text-light" role="status"></div>
              </div>
              <div v-else-if="todayTasks.length" class="today-task-list">
                <div
                  v-for="task in todayTasks"
                  :key="`${task.planId}-${task.id}`"
                  class="today-task-item"
                >
                  <div class="today-task-copy">
                    <span class="badge bg-light text-dark me-2">{{ task.planTitle }}</span>
                    <span class="badge bg-light text-dark me-2">{{ $t('dashboard.dayNumber', { day: task.dayNumber }) }}</span>
                    <p class="mb-1 mt-2">{{ task.content }}</p>
                    <small class="text-muted"><i class="bi bi-clock"></i> {{ formatNumber(task.duration) }}{{ $t('dashboard.hours') }}</small>
                  </div>
                  <div class="today-task-action">
                    <span v-if="task.isCompleted" class="badge bg-success">
                      <i class="bi bi-check"></i> {{ $t('dashboard.completed') }}
                    </span>
                    <button
                      v-else
                      class="btn btn-success btn-sm"
                      :disabled="loading"
                      @click="quickCheckIn(task)"
                    >
                      <i class="bi bi-check-circle"></i> {{ $t('dashboard.checkInToday') }}
                    </button>
                  </div>
                </div>
              </div>
              <div v-else>
                <p class="mb-0">{{ $t('dashboard.noActivePlans') }}</p>
                <router-link to="/create-plan" class="btn btn-light btn-sm mt-2">{{ $t('dashboard.createPlan') }}</router-link>
              </div>
            </div>
          </div>
        </div>
        <div class="col-md-4 d-flex flex-column">
          <div class="card quick-actions-card">
            <div class="card-header">
              <h6 class="mb-0"><i class="bi bi-lightning"></i> {{ $t('dashboard.quickActions') }}</h6>
            </div>
            <div class="card-body">
              <div class="d-grid gap-2">
                <router-link to="/create-plan" class="btn btn-primary">
                  <i class="bi bi-plus-circle"></i> {{ $t('dashboard.createNewPlan') }}
                </router-link>
                <router-link to="/ai-assistant" class="btn btn-outline-primary">
                  <i class="bi bi-robot"></i> {{ $t('dashboard.askAI') }}
                </router-link>
                <router-link to="/my-plans" class="btn btn-outline-primary">
                  <i class="bi bi-journal-bookmark"></i> {{ $t('dashboard.myStudyPlans') }}
                </router-link>
              </div>
            </div>
          </div>
          <div class="card today-progress-card mt-3 flex-grow-1">
            <div class="card-body">
              <div class="progress-header">
                <h6 class="mb-0"><i class="bi bi-bar-chart-fill"></i> 今日进度</h6>
                <span class="progress-fraction">{{ completedCount }}/{{ totalCount }}</span>
              </div>
              <div class="progress mt-3" style="height: 8px;">
                <div
                  class="progress-bar"
                  :style="{ width: progressPercent + '%' }"
                  :class="progressPercent === 100 ? 'bg-success' : ''"
                ></div>
              </div>
              <p class="progress-msg mt-2 mb-0">
                <template v-if="totalCount === 0">今日暂无任务</template>
                <template v-else-if="progressPercent === 100"><i class="bi bi-emoji-smile"></i> 全部完成，干得漂亮！</template>
                <template v-else>还差 {{ totalCount - completedCount }} 项，继续加油</template>
              </p>
              <div class="daily-quote mt-3">
                <i class="bi bi-quote quote-icon"></i>
                <p class="quote-text mb-0">{{ dailyQuote.text }}</p>
                <span class="quote-author">—— {{ dailyQuote.author }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 最近计划 -->
      <div class="row mt-4">
        <div class="col-12">
          <div class="card">
            <div class="card-header d-flex justify-content-between align-items-center">
              <h6 class="mb-0"><i class="bi bi-journal-bookmark"></i> {{ $t('dashboard.myStudyPlans') }}</h6>
              <router-link to="/my-plans" class="btn btn-sm btn-outline-primary">{{ $t('dashboard.viewAll') }}</router-link>
            </div>
            <div class="card-body">
              <div v-if="plans.length === 0" class="text-center py-4">
                <i class="bi bi-inbox fs-1 text-muted"></i>
                <p class="text-muted mt-2">{{ $t('dashboard.noPlansYet') }}</p>
                <router-link to="/create-plan" class="btn btn-primary">{{ $t('dashboard.createPlan') }}</router-link>
              </div>
              <div v-else>
                <router-link
                  v-for="plan in plans.slice(0, 3)"
                  :key="plan.id"
                  class="recent-plan-item"
                  :to="`/my-plans/${plan.id}`"
                >
                  <div>
                    <h6 class="mb-1">{{ plan.title }}</h6>
                    <small class="text-muted">
                      {{ plan.startDate }} ~ {{ plan.endDate }} | {{ $t('dashboard.dailyHours', { hours: formatNumber(plan.dailyHours) }) }}
                    </small>
                  </div>
                  <span
                    :class="['badge', plan.status === t('plan.inProgress') ? 'bg-success' : 'bg-secondary']"
                  >
                    {{ plan.status }}
                  </span>
                </router-link>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="row mt-4">
        <div class="col-12">
          <div class="card rescue-card" :class="{ expanded: rescueExpanded }">
            <button
              type="button"
              class="card-header rescue-toggle"
              :aria-expanded="rescueExpanded"
              @click="rescueExpanded = !rescueExpanded"
            >
              <div>
                <h6 class="mb-1"><i class="bi bi-exclamation-triangle"></i> 待补救任务</h6>
                <small class="text-muted">集中处理逾期、未完成和连续跳过的学习任务</small>
              </div>
              <span class="rescue-toggle-side">
                <span class="rescue-count">{{ remedialTasks.length }}</span>
                <i class="bi bi-chevron-down rescue-chevron"></i>
              </span>
            </button>
            <div v-if="rescueExpanded" class="card-body">
              <div v-if="loading" class="text-center py-4">
                <div class="spinner-border text-primary" role="status"></div>
              </div>
              <div v-else-if="remedialTasks.length" class="rescue-list">
                <router-link
                  v-for="task in remedialTasks"
                  :key="`${task.planId}-${task.id}-${task.reason}`"
                  class="rescue-item"
                  :class="`severity-${task.severity}`"
                  :to="`/my-plans/${task.planId}`"
                >
                  <div class="rescue-main">
                    <div class="rescue-meta">
                      <span class="rescue-reason">{{ task.reason }}</span>
                      <span>{{ task.planTitle }}</span>
                      <span v-if="task.dayNumber">{{ $t('dashboard.dayNumber', { day: task.dayNumber }) }}</span>
                    </div>
                    <p class="mb-0">{{ task.content }}</p>
                    <small class="text-muted">
                      <i class="bi bi-calendar2-week"></i>
                      {{ formatRescueDate(task.scheduledDate) }}
                      <template v-if="task.duration">
                        · <i class="bi bi-clock"></i> {{ formatNumber(task.duration) }}{{ $t('dashboard.hours') }}
                      </template>
                    </small>
                  </div>
                  <i class="bi bi-chevron-right"></i>
                </router-link>
              </div>
              <div v-else class="rescue-empty">
                <i class="bi bi-check2-circle"></i>
                <div>
                  <strong>暂时没有待补救任务</strong>
                  <p class="mb-0">当前计划节奏保持得不错，继续按今日任务推进。</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Navbar from '../components/Navbar.vue'
import Footer from '../components/Footer.vue'
import StudyChart from '../components/StudyChart.vue'
import CheckinHeatmap from '../components/CheckinHeatmap.vue'
import { useUserStore } from '../stores/user'
import { planApi } from '../api/plan'
import { checkinApi } from '../api/checkin'
import { aiApi } from '../api/ai'
import { showToast } from '../utils/toast'
import { formatNumber } from '../utils/format'

const { t } = useI18n()
const userStore = useUserStore()
const user = computed(() => userStore.user)

const loading = ref(true)
const stats = ref({})
const todayTasks = ref([])
const plans = ref([])
const remedialTasks = ref([])
const rescueExpanded = ref(false)
const chartData = ref({
  week: { xAxis: [], series: [] },
  month: { xAxis: [], series: [] }
})
const heatmapData = ref({})

const inProgressStatuses = ['进行中', 'In Progress', t('plan.inProgress')]

const activePlanTotal = computed(() => plans.value.filter(isPlanInProgress).length)

const todayProgress = computed(() => {
  const total = todayTasks.value.length
  const done = todayTasks.value.filter(t => t.isCompleted).length
  return { total, done, pct: total ? done / total : 0 }
})

const motivationText = computed(() => {
  const { total, done, pct } = todayProgress.value
  if (!total) return t('dashboard.motivationNotStarted')
  if (done === total) return t('dashboard.motivationDone')
  if (pct >= 0.5) return t('dashboard.motivationHalfway')
  return t('dashboard.motivationBegun')
})
const completedCount = computed(() => todayTasks.value.filter(t => t.isCompleted).length)
const totalCount = computed(() => todayTasks.value.length)
const progressPercent = computed(() =>
  totalCount.value ? Math.round(completedCount.value / totalCount.value * 100) : 0
)

const dailyQuote = ref({ text: '学而不思则罔，思而不学则殆。', author: '孔子' })

async function loadDailyQuote() {
  const today = new Date().toISOString().slice(0, 10)
  const cacheKey = `daily_quote_${today}`
  const cached = localStorage.getItem(cacheKey)
  if (cached) {
    try {
      dailyQuote.value = JSON.parse(cached)
      return
    } catch {}
  }
  try {
    const result = await aiApi.chat([{
      role: 'user',
      content: '请给我一句真实的、有明确作者的关于学习或励志的名言警句，用中文返回，严格按JSON格式：{"text":"名言内容","author":"作者姓名"}，不要任何额外文字。'
    }])
    const raw = result?.data?.content || result?.data || ''
    const match = String(raw).match(/\{[\s\S]*?\}/)
    if (match) {
      const quote = JSON.parse(match[0])
      if (quote.text && quote.author) {
        dailyQuote.value = quote
        localStorage.setItem(cacheKey, JSON.stringify(quote))
      }
    }
  } catch {}
}

function isPlanInProgress(plan) {
  return inProgressStatuses.includes(plan?.status)
}

onMounted(() => {
  loadDashboard()
  loadDailyQuote()
})

async function loadDashboard() {
  loading.value = true
  todayTasks.value = []
  remedialTasks.value = []

  try {
    const statsResult = await checkinApi.getStats()
    if (statsResult && statsResult.code === 200) {
      stats.value = statsResult.data || {}
    }

    const chartResult = await checkinApi.getChartData()
    if (chartResult && chartResult.code === 200) {
      chartData.value = chartResult.data
    }

    await loadHeatmapData(new Date().getFullYear(), new Date().getMonth() + 1)

    const plansResult = await planApi.getPlans()
    if (plansResult && plansResult.code === 200) {
      plans.value = plansResult.data || []
      const activePlans = plans.value.filter(isPlanInProgress)
      stats.value.activePlans = activePlans.length

      const taskPromises = activePlans.map(async (plan) => {
        const task = await loadTodayTask(plan.id)
        return task ? { planId: plan.id, planTitle: plan.title, ...task } : null
      })
      const results = await Promise.all(taskPromises)
      todayTasks.value = results.filter(Boolean)
      remedialTasks.value = await loadRemedialTasks(activePlans)
    }
  } catch (error) {
    console.error('加载仪表盘失败:', error)
    showToast(t('dashboard.loadDataFailed'), 'error')
  } finally {
    loading.value = false
  }
}

async function loadRemedialTasks(activePlans) {
  const detailResults = await Promise.all(activePlans.map(async (plan) => {
    try {
      const result = await planApi.getPlanDetail(plan.id)
      if (result?.code === 200 && result.data?.details) {
        return { plan, details: result.data.details }
      }
    } catch (error) {
      console.error('加载待补救任务失败:', error)
    }
    return { plan, details: [] }
  }))

  return detailResults
    .flatMap(({ plan, details }) => buildPlanRemedialTasks(plan, details))
    .sort((a, b) => b.weight - a.weight || compareDate(a.scheduledDate, b.scheduledDate))
    .slice(0, 8)
}

function buildPlanRemedialTasks(plan, details) {
  const today = startOfDay(new Date())
  const normalized = (details || [])
    .map((task) => ({
      ...task,
      planId: plan.id,
      planTitle: plan.title,
      dateValue: parseDate(task.scheduledDate),
      done: isTaskDone(task)
    }))
    .filter((task) => !task.done)

  const overdueTasks = normalized.filter((task) => task.dateValue && task.dateValue < today)
  const overdueByDay = new Set(overdueTasks.map((task) => toDateKey(task.dateValue)))

  return normalized
    .filter((task) => {
      if (!task.dateValue) return false
      return task.dateValue <= today
    })
    .map((task) => {
      const overdueDays = task.dateValue < today ? diffDays(today, task.dateValue) : 0
      const skipped = overdueDays >= 2 || (overdueDays > 0 && hasPreviousOverdueDay(task.dateValue, overdueByDay))
      if (skipped) {
        return toRemedialTask(task, `连续跳过 ${Math.max(2, overdueDays)} 天`, 'high', 300 + overdueDays)
      }
      if (overdueDays > 0) {
        return toRemedialTask(task, `逾期 ${overdueDays} 天`, 'medium', 200 + overdueDays)
      }
      return toRemedialTask(task, '今日未完成', 'low', 100)
    })
}

function toRemedialTask(task, reason, severity, weight) {
  return {
    id: task.id,
    planId: task.planId,
    planTitle: task.planTitle,
    dayNumber: task.dayNumber,
    content: task.content,
    duration: task.duration,
    scheduledDate: task.scheduledDate,
    reason,
    severity,
    weight
  }
}

function isTaskDone(task) {
  return task?.isCompleted === true || task?.isCompleted === 1 || task?.completed === true || task?.completed === 1
}

function parseDate(value) {
  if (!value) return null
  const date = new Date(`${value}T00:00:00`)
  return Number.isNaN(date.getTime()) ? null : startOfDay(date)
}

function startOfDay(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

function diffDays(later, earlier) {
  return Math.max(0, Math.round((later - earlier) / 86400000))
}

function toDateKey(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function hasPreviousOverdueDay(date, overdueByDay) {
  const previous = new Date(date)
  previous.setDate(previous.getDate() - 1)
  return overdueByDay.has(toDateKey(previous))
}

function compareDate(a, b) {
  const dateA = parseDate(a)?.getTime() || 0
  const dateB = parseDate(b)?.getTime() || 0
  return dateA - dateB
}

function formatRescueDate(value) {
  if (!value) return '未排期'
  const date = parseDate(value)
  const today = startOfDay(new Date())
  if (!date) return value
  const delta = diffDays(today, date)
  if (date.getTime() === today.getTime()) return '今天'
  if (date < today) return `${value}，已过 ${delta} 天`
  return value
}

async function loadTodayTask(planId) {
  try {
    const result = await planApi.getTodayTask(planId)
    if (result && result.code === 200 && result.data) {
      return result.data
    }
  } catch (error) {
    console.error('加载今日任务失败:', error)
  }
  return null
}

async function loadHeatmapData(year, month) {
  try {
    const result = await checkinApi.getCalendar(year, month)
    if (result && result.code === 200 && result.data) {
      const records = result.data.checkIns || []
      const mapped = {}
      for (const r of records) {
        const date = r.checkDate
        if (!date) continue
        mapped[date] = (mapped[date] || 0) + (Number(r.studyHours) || 0)
      }
      heatmapData.value = mapped
    }
  } catch (error) {
    console.error('加载打卡日历失败:', error)
  }
}

function onHeatmapMonthChange({ year, month }) {
  loadHeatmapData(year, month)
}

async function quickCheckIn(task) {
  if (!task) {
    showToast(t('dashboard.noTaskToCheckIn'), 'warning')
    return
  }

  try {
    const result = await checkinApi.checkIn({
      planId: task.planId,
      detailId: task.id,
      studyHours: task.duration || 2
    })

    if (result && result.code === 200) {
      showToast(t('dashboard.checkInSuccess', { hours: task.duration }), 'success')
      loadDashboard()
    } else {
      showToast(result?.message || t('dashboard.checkInFailed'), 'error')
    }
  } catch (error) {
    console.error('打卡失败:', error)
    showToast(t('dashboard.checkInFailed'), 'error')
  }
}
</script>

<style scoped>
.dashboard-hero {
  align-items: end;
  display: grid;
  gap: 1.25rem;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 360px);
  padding: 28px 0 8px;
}

.dashboard-heading {
  min-width: 0;
}

.hero-panel {
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(87, 150, 231, 0.18);
  border-radius: 8px;
  box-shadow: 0 18px 42px rgba(54, 83, 116, 0.12);
  padding: 1rem;
}

.hero-panel-top,
.stat-card-content,
.today-task-item,
.recent-plan-item {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

.hero-panel-top {
  color: #42526b;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.hero-panel-top strong {
  color: #172033;
  font-size: 1.5rem;
  line-height: 1;
}

.hero-progress {
  background: rgba(62, 141, 247, 0.12);
  border-radius: 999px;
  height: 10px;
  overflow: hidden;
}

.hero-progress span {
  background: linear-gradient(90deg, #3e8df7, #4a9a72);
  border-radius: inherit;
  display: block;
  height: 100%;
  transition: width 0.24s ease;
}

.hero-panel-grid {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 1rem;
}

.hero-panel-grid div {
  background: rgba(62, 141, 247, 0.08);
  border-radius: 8px;
  padding: 0.75rem;
}

.hero-panel-grid small {
  color: #667085;
  display: block;
  margin-bottom: 0.25rem;
}

.hero-panel-grid strong {
  color: #172033;
  font-size: 1.35rem;
}

.stat-card {
  border-left: 0;
}

.stat-card .card-body {
  min-height: 132px;
  position: relative;
}

.stat-card-content {
  gap: 1rem;
  height: 100%;
}

.stat-icon {
  align-items: center;
  background: rgba(62, 141, 247, 0.12);
  border: 1px solid rgba(62, 141, 247, 0.18);
  border-radius: 8px;
  color: #2f6fc5;
  display: inline-flex;
  flex: 0 0 54px;
  font-size: 1.55rem;
  height: 54px;
  justify-content: center;
}

.stat-card.success .stat-icon {
  background: rgba(74, 154, 114, 0.12);
  border-color: rgba(74, 154, 114, 0.18);
  color: #3d805f;
}

.stat-card.warning .stat-icon {
  background: rgba(239, 184, 77, 0.16);
  border-color: rgba(239, 184, 77, 0.24);
  color: #b37b17;
}

.stat-card.info .stat-icon {
  background: rgba(86, 183, 217, 0.14);
  border-color: rgba(86, 183, 217, 0.22);
  color: #2785a7;
}

.today-task {
  background: #fff;
  min-height: 260px;
}

.today-task .card-body {
  padding: 28px;
}

.today-task-list {
  display: grid;
  gap: 0.75rem;
  max-height: 360px;
  overflow-y: auto;
  padding-right: 4px;
}

.today-task-item {
  background: #f8f9fc;
  border: 1px solid rgba(0, 0, 0, 0.07);
  border-radius: 8px;
  gap: 1rem;
  padding: 0.9rem;
}

.today-task-copy {
  min-width: 0;
}

.today-task-action {
  flex: 0 0 auto;
}

.quick-actions-card .d-grid {
  grid-template-columns: 1fr;
}

.progress-header {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

.progress-fraction {
  color: #667085;
  font-size: 0.85rem;
  font-weight: 600;
}

.progress-msg {
  color: #667085;
  font-size: 0.82rem;
}

.daily-quote {
  border-top: 1px dashed #e4e7ec;
  padding-top: 0.6rem;
  position: relative;
}

.quote-icon {
  color: #d0d5dd;
  font-size: 1.1rem;
  line-height: 1;
}

.quote-text {
  font-size: 0.8rem;
  color: #475467;
  line-height: 1.5;
  font-style: italic;
}

.quote-author {
  font-size: 0.75rem;
  color: #98a2b3;
}

.recent-plan-item {
  border-radius: 8px;
  color: inherit;
  gap: 1rem;
  margin-bottom: 0.6rem;
  padding: 0.9rem;
  text-decoration: none;
  transition: background 0.18s ease, transform 0.18s ease;
}

.recent-plan-item:hover {
  background: rgba(62, 141, 247, 0.08);
  color: inherit;
  transform: translateX(2px);
}

.rescue-card .card-header {
  background: rgba(255, 255, 255, 0.76);
}

.rescue-toggle {
  align-items: center;
  border: 0;
  border-bottom: 1px solid rgba(80, 116, 152, 0.12);
  color: inherit;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  text-align: left;
  width: 100%;
}

.rescue-toggle:hover {
  background: rgba(62, 141, 247, 0.05);
}

.rescue-toggle-side {
  align-items: center;
  display: inline-flex;
  flex: 0 0 auto;
  gap: 0.75rem;
}

.rescue-chevron {
  color: #667085;
  transition: transform 0.18s ease;
}

.rescue-card.expanded .rescue-chevron {
  transform: rotate(180deg);
}

.rescue-count {
  align-items: center;
  background: rgba(226, 91, 91, 0.12);
  border: 1px solid rgba(226, 91, 91, 0.18);
  border-radius: 999px;
  color: #b43d3d;
  display: inline-flex;
  font-weight: 800;
  height: 32px;
  justify-content: center;
  min-width: 32px;
  padding: 0 10px;
}

.rescue-list {
  display: grid;
  gap: 0.75rem;
}

.rescue-item {
  align-items: center;
  background: #fff;
  border: 1px solid rgba(80, 116, 152, 0.14);
  border-left: 4px solid #7aa7e8;
  border-radius: 8px;
  color: inherit;
  display: flex;
  gap: 1rem;
  justify-content: space-between;
  padding: 0.9rem 1rem;
  text-decoration: none;
  transition: background 0.18s ease, transform 0.18s ease, border-color 0.18s ease;
}

.rescue-item:hover {
  background: rgba(62, 141, 247, 0.06);
  color: inherit;
  transform: translateX(2px);
}

.rescue-item.severity-high {
  border-left-color: #e25b5b;
}

.rescue-item.severity-medium {
  border-left-color: #f2a93b;
}

.rescue-item.severity-low {
  border-left-color: #3e8df7;
}

.rescue-main {
  display: grid;
  gap: 0.35rem;
  min-width: 0;
}

.rescue-main p {
  color: #172033;
  font-weight: 700;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.rescue-meta {
  align-items: center;
  color: #667085;
  display: flex;
  flex-wrap: wrap;
  font-size: 0.78rem;
  gap: 0.45rem;
}

.rescue-meta span {
  background: rgba(62, 141, 247, 0.08);
  border-radius: 999px;
  padding: 0.2rem 0.55rem;
}

.rescue-reason {
  background: rgba(226, 91, 91, 0.12) !important;
  color: #b43d3d;
  font-weight: 800;
}

.rescue-empty {
  align-items: center;
  background: rgba(74, 154, 114, 0.08);
  border: 1px dashed rgba(74, 154, 114, 0.3);
  border-radius: 8px;
  color: #3d805f;
  display: flex;
  gap: 0.9rem;
  padding: 1rem;
}

.rescue-empty i {
  font-size: 1.8rem;
}

.rescue-empty p {
  color: #667085;
  font-size: 0.9rem;
}

:global([data-bs-theme='dark'] .dashboard-page .card),
:global([data-bs-theme='dark'] .dashboard-page .hero-panel),
:global([data-bs-theme='dark'] .dashboard-page .hero-panel-grid div) {
  background: rgba(15, 24, 38, 0.78);
  border-color: rgba(188, 213, 241, 0.14);
  box-shadow: 0 18px 46px rgba(0, 0, 0, 0.3);
}

:global([data-bs-theme='dark'] .dashboard-page .card-header) {
  background: rgba(20, 31, 48, 0.68) !important;
  border-color: rgba(188, 213, 241, 0.12);
}

:global([data-bs-theme='dark'] .dashboard-page .today-task) {
  background:
    linear-gradient(135deg, rgba(11, 25, 43, 0.96), rgba(16, 38, 58, 0.92)) !important;
  color: #edf5ff;
}

:global([data-bs-theme='dark'] .dashboard-page .today-task-item),
:global([data-bs-theme='dark'] .dashboard-page .recent-plan-item),
:global([data-bs-theme='dark'] .dashboard-page .rescue-item) {
  background: rgba(7, 15, 27, 0.42);
  border: 1px solid rgba(188, 213, 241, 0.12);
}

:global([data-bs-theme='dark'] .dashboard-page .today-task-item .badge.bg-light) {
  background: rgba(237, 245, 255, 0.12) !important;
  color: #edf5ff !important;
}

:global([data-bs-theme='dark'] .dashboard-page .recent-plan-item:hover) {
  background: rgba(62, 141, 247, 0.18);
}

:global([data-bs-theme='dark'] .dashboard-page .rescue-item:hover) {
  background: rgba(62, 141, 247, 0.14);
}

:global([data-bs-theme='dark'] .dashboard-page .rescue-item.severity-high) {
  border-left: 4px solid #e25b5b;
}

:global([data-bs-theme='dark'] .dashboard-page .rescue-item.severity-medium) {
  border-left: 4px solid #f2a93b;
}

:global([data-bs-theme='dark'] .dashboard-page .rescue-item.severity-low) {
  border-left: 4px solid #3e8df7;
}

:global([data-bs-theme='dark'] .dashboard-page .rescue-main p) {
  color: #edf5ff;
}

:global([data-bs-theme='dark'] .dashboard-page .rescue-meta),
:global([data-bs-theme='dark'] .dashboard-page .rescue-empty p) {
  color: #aebdd1;
}

:global([data-bs-theme='dark'] .dashboard-page .rescue-meta span) {
  background: rgba(141, 197, 255, 0.12);
}

:global([data-bs-theme='dark'] .dashboard-page .rescue-empty) {
  background: rgba(74, 154, 114, 0.12);
  border-color: rgba(74, 154, 114, 0.28);
  color: #7ed6a0;
}

:global([data-bs-theme='dark'] .dashboard-page .hero-panel-top),
:global([data-bs-theme='dark'] .dashboard-page .hero-panel-grid small) {
  color: #aebdd1;
}

:global([data-bs-theme='dark'] .dashboard-page .hero-panel-top strong),
:global([data-bs-theme='dark'] .dashboard-page .hero-panel-grid strong) {
  color: #edf5ff;
}

:global([data-bs-theme='dark'] .dashboard-page .hero-progress) {
  background: rgba(188, 213, 241, 0.14);
}

:global([data-bs-theme='dark'] .dashboard-page .stat-icon) {
  background: rgba(141, 197, 255, 0.12);
  border-color: rgba(141, 197, 255, 0.2);
  color: #8dc5ff;
}

@media (max-width: 991.98px) {
  .dashboard-hero {
    grid-template-columns: 1fr;
  }

}

@media (max-width: 767.98px) {
  .today-task-item,
  .recent-plan-item,
  .rescue-item {
    align-items: flex-start;
    flex-direction: column;
  }

  .today-task-action {
    width: 100%;
  }

  .today-task-action .btn {
    width: 100%;
  }
}
</style>
