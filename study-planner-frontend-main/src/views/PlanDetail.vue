<template>
  <div class="plan-detail-page app-shell">
    <Navbar />
    
    <div class="container page-surface my-5">
      <div v-if="showCelebration" class="checkin-celebration" aria-hidden="true">
        <div class="celebration-card">
          <i class="bi bi-stars"></i>
          <span>{{ $t('planDetail.checkInCelebration') }}</span>
        </div>
        <span v-for="n in 18" :key="n" class="confetti" :style="confettiStyle(n)"></span>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border text-primary" role="status"></div>
        <p class="mt-2 text-muted">{{ $t('planDetail.loading') }}</p>
      </div>

      <div v-else-if="!plan" class="text-center py-5">
        <i class="bi bi-exclamation-circle fs-1 text-muted"></i>
        <p class="mt-3">{{ $t('planDetail.notFound') }}</p>
        <button class="btn btn-primary" @click="router.push('/my-plans')">{{ $t('planDetail.returnToList') }}</button>
      </div>

      <div v-else>
        <!-- 计划头部信息 -->
        <div class="card mb-4 plan-hero-card">
          <div class="card-body">
            <div class="plan-hero-grid">
              <div class="plan-hero-main">
                <div class="hero-eyebrow">当前计划</div>
                <h2 class="card-title hero-title mb-2">{{ plan.title }}</h2>
                <div class="hero-badges">
                  <span class="hero-badge">{{ plan.level }}</span>
                  <span class="hero-badge">{{ plan.totalDays }}{{ $t('planDetail.day') }}</span>
                  <span class="hero-badge">{{ formatNumber(plan.dailyHours) }}{{ $t('planDetail.hoursPerDay') }}</span>
                </div>
                <p class="card-text hero-description">{{ plan.goal }}</p>

                <div class="hero-stats">
                  <div class="hero-stat">
                    <span class="hero-stat-label">{{ $t('planDetail.completedTasks') }}</span>
                    <strong>{{ completedCount }} / {{ details.length }}</strong>
                  </div>
                  <div class="hero-stat">
                    <span class="hero-stat-label">{{ $t('planDetail.remainingTasks') }}</span>
                    <strong>{{ remainingCount }}</strong>
                  </div>
                  <div class="hero-stat">
                    <span class="hero-stat-label">{{ $t('planDetail.completionRate') }}</span>
                    <strong>{{ formatNumber(completionRate) }}%</strong>
                  </div>
                </div>
              </div>

              <div class="plan-hero-side">
                <div class="hero-status-panel">
                  <div class="hero-status-top">
                    <span :class="['badge', getStatusClass(plan.status)]" class="fs-6">
                      {{ plan.status }}
                    </span>
                    <span class="hero-progress-text">{{ $t('planDetail.progress', { progress: formatNumber(progress) }) }}</span>
                  </div>

                  <div class="progress hero-progress-bar">
                    <div class="progress-bar hero-progress-fill" :style="{ width: progress + '%' }"></div>
                  </div>

                  <div class="focus-card" :class="{ done: !nextPendingDetail }">
                    <span class="focus-label">当前建议</span>
                    <strong v-if="nextPendingDetail">先完成第 {{ nextPendingDetail.dayNumber }} 天任务</strong>
                    <strong v-else>本计划任务已全部完成</strong>
                    <p v-if="nextPendingDetail">
                      {{ getTaskContent(nextPendingDetail) }}
                    </p>
                    <p v-else>
                      可以进入编排计划继续延展后续内容，或开启小组计划一起学习。
                    </p>
                  </div>

                  <router-link :to="`/my-plans/${planId}/workbench`" class="btn btn-sm btn-outline-primary hero-workbench-btn">
                    <i class="bi bi-calendar3"></i> 编排计划
                  </router-link>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 每日任务区域 -->
        <div class="row mb-4">
          <div class="col-12">
            <div class="daily-task-panel">
              <div class="daily-task-panel-header">
                <div>
                  <h4 class="mb-1">{{ $t('planDetail.dailyTasks') }}</h4>
                  <p class="daily-task-hint mb-0">
                    <span v-if="nextPendingDetail">下一步建议：第 {{ nextPendingDetail.dayNumber }} 天，{{ getTaskContent(nextPendingDetail) }}</span>
                    <span v-else>当前计划已经全部完成，可以复盘或继续扩展学习内容。</span>
                  </p>
                </div>
                <div class="btn-group btn-group-sm" role="group">
                  <button
                    type="button"
                    class="btn btn-outline-primary"
                    :class="{ active: viewMode === 'list' }"
                    @click="viewMode = 'list'"
                  >
                    <i class="bi bi-list"></i> {{ $t('planDetail.listView') }}
                  </button>
                  <button
                    type="button"
                    class="btn btn-outline-primary"
                    :class="{ active: viewMode === 'grid' }"
                    @click="viewMode = 'grid'"
                  >
                    <i class="bi bi-grid"></i> {{ $t('planDetail.gridView') }}
                  </button>
                </div>
              </div>

              <div class="study-stats-inline">
                <div class="study-stat-tile">
                  <span>{{ $t('planDetail.completedTasks') }}</span>
                  <strong>{{ completedCount }} / {{ details.length }}</strong>
                </div>
                <div class="study-stat-tile">
                  <span>{{ $t('planDetail.remainingTasks') }}</span>
                  <strong>{{ remainingCount }}</strong>
                </div>
                <div class="study-stat-tile">
                  <span>{{ $t('planDetail.completionRate') }}</span>
                  <strong>{{ formatNumber(completionRate) }}%</strong>
                </div>
              </div>
            </div>

            <!-- 列表视图 -->
            <div v-if="viewMode === 'list'" class="accordion task-accordion" id="planAccordion">
              <div
                v-for="detail in details"
                :key="detail.id"
                class="accordion-item task-list-item"
                :class="{
                  completed: detail.isCompleted,
                  active: expandedDetailId === detail.id,
                  current: nextPendingDetail && nextPendingDetail.id === detail.id
                }"
              >
                <h2 class="accordion-header">
                  <button
                    class="accordion-button task-list-trigger"
                    :class="{ collapsed: expandedDetailId !== detail.id }"
                    type="button"
                    @click="toggleDetail(detail)"
                  >
                    <div class="task-list-leading">
                      <span class="task-day-chip">DAY {{ detail.dayNumber }}</span>
                      <div class="task-list-copy">
                        <strong>{{ getTaskContent(detail) }}</strong>
                        <div class="task-list-meta">
                          <span><i class="bi bi-clock"></i>{{ formatNumber(detail.duration) }} {{ $t('planDetail.hours') }}</span>
                          <span><i class="bi bi-calendar3"></i>{{ formatDisplayDate(getExpectedDate(detail)) }}</span>
                        </div>
                      </div>
                    </div>
                    <div class="task-list-trailing">
                      <span v-if="nextPendingDetail && nextPendingDetail.id === detail.id && !detail.isCompleted" class="task-state-badge current">
                        <i class="bi bi-lightning-charge-fill"></i>
                        当前任务
                      </span>
                      <span v-else-if="detail.isCompleted" class="task-state-badge done">
                        <i class="bi bi-check-circle-fill"></i>
                        {{ $t('planDetail.completedDays') }}
                      </span>
                      <span v-else class="task-state-badge pending">
                        <i class="bi bi-hourglass-split"></i>
                        待完成
                      </span>
                    </div>
                  </button>
                </h2>
                <div
                  :id="'collapse' + detail.id"
                  class="accordion-collapse collapse"
                  :class="{ show: expandedDetailId === detail.id }"
                >
                  <div class="accordion-body task-detail-body">
                    <div class="task-detail-summary">
                      <div class="task-detail-block">
                        <span class="summary-label">{{ $t('planDetail.learningContent') }}</span>
                        <p>{{ detail.content }}</p>
                      </div>
                      <div class="task-detail-inline">
                        <div class="task-detail-block compact">
                          <span class="summary-label">{{ $t('planDetail.estimatedDuration') }}</span>
                          <p>{{ formatNumber(detail.duration) }} {{ $t('planDetail.hours') }}</p>
                        </div>
                        <div class="task-detail-block compact">
                          <span class="summary-label">{{ $t('planDetail.expectedCompleteDate') }}</span>
                          <p>{{ formatDisplayDate(getExpectedDate(detail)) }}</p>
                        </div>
                      </div>
                    </div>

                    <div class="mt-3 checkin-row">
                      <div v-if="!detail.isCompleted" class="study-hours-control">
                        <label :for="'studyHours' + detail.id" class="form-label mb-1">
                          {{ $t('planDetail.actualStudyHours') }}
                        </label>
                        <div class="input-group input-group-sm">
                          <button class="btn btn-outline-secondary" type="button" @click="adjustStudyHours(detail, -0.5)">
                            <i class="bi bi-dash"></i>
                          </button>
                          <input
                            :id="'studyHours' + detail.id"
                            v-model.number="studyHoursByDetail[detail.id]"
                            class="form-control text-center"
                            type="number"
                            min="0.1"
                            max="24"
                            step="0.5"
                          >
                          <button class="btn btn-outline-secondary" type="button" @click="adjustStudyHours(detail, 0.5)">
                            <i class="bi bi-plus"></i>
                          </button>
                          <span class="input-group-text">{{ $t('planDetail.hours') }}</span>
                        </div>
                      </div>
                      <button
                        v-if="!detail.isCompleted"
                        class="btn btn-success btn-sm"
                        @click="checkIn(detail)"
                      >
                        <i class="bi bi-check-circle"></i>
                        {{ isEarlyCheckIn(detail) ? $t('planDetail.earlyCheckIn') : $t('planDetail.checkIn') }}
                      </button>
                      <span v-else class="completed-info text-success">
                        <i class="bi bi-check-circle-fill"></i>
                        {{ $t('planDetail.completedWithHours', { date: formatDisplayDate(detail.checkInDate || detail.updateTime), hours: formatNumber(detail.studyHours || studyHoursByDetail[detail.id] || detail.duration) }) }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 网格视图 -->
            <div v-else class="task-grid task-grid-three-cols">
              <div
                v-for="detail in details"
                :key="detail.id"
                class="task-card"
                :class="{
                  'task-completed': detail.isCompleted,
                  'task-current': nextPendingDetail && nextPendingDetail.id === detail.id
                }"
                @click="toggleGridDetail(detail)"
              >
                <div class="task-header">
                  <span class="day-badge">第 {{ detail.dayNumber }} 天</span>
                  <span
                    class="task-status"
                    :class="{
                      done: detail.isCompleted,
                      current: nextPendingDetail && nextPendingDetail.id === detail.id && !detail.isCompleted,
                      pending: !detail.isCompleted && (!nextPendingDetail || nextPendingDetail.id !== detail.id)
                    }"
                  >
                    <template v-if="nextPendingDetail && nextPendingDetail.id === detail.id && !detail.isCompleted">
                      <i class="bi bi-lightning-charge-fill"></i>
                      当前任务
                    </template>
                    <template v-else-if="detail.isCompleted">
                      <i class="bi bi-check-circle-fill"></i>
                      已完成
                    </template>
                    <template v-else>
                      <i class="bi bi-hourglass-split"></i>
                      待完成
                    </template>
                  </span>
                </div>
                <div class="task-content">
                  <span v-if="nextPendingDetail && nextPendingDetail.id === detail.id && !detail.isCompleted" class="grid-focus-tag">
                    <i class="bi bi-lightning-charge-fill"></i>
                    当前任务
                  </span>
                  <span class="task-content-label">学习内容</span>
                  <p class="task-title">{{ getTaskContent(detail) }}</p>
                  <div class="task-meta">
                    <span><i class="bi bi-clock"></i>{{ formatNumber(detail.duration) }} 小时</span>
                    <span><i class="bi bi-calendar3"></i>{{ formatDisplayDate(getExpectedDate(detail)) }}</span>
                  </div>
                </div>
                <div class="task-footer">
                  <div v-if="!detail.isCompleted" class="footer-actions">
                    <span class="hours-label">本次学习</span>
                    <div class="hours-input">
                      <button class="btn btn-xs btn-outline-secondary" @click.stop="adjustStudyHours(detail, -0.5)">
                        <i class="bi bi-dash"></i>
                      </button>
                      <input
                        v-model.number="studyHoursByDetail[detail.id]"
                        class="form-control form-control-xs"
                        type="number"
                        min="0.1"
                        max="24"
                        step="0.5"
                        @click.stop
                      >
                      <button class="btn btn-xs btn-outline-secondary" @click.stop="adjustStudyHours(detail, 0.5)">
                        <i class="bi bi-plus"></i>
                      </button>
                      <span class="hours-unit">小时</span>
                    </div>
                    <button
                      class="btn btn-success btn-xs"
                      @click.stop="checkIn(detail)"
                    >
                      <i class="bi bi-check-circle"></i>
                      {{ isEarlyCheckIn(detail) ? '提前打卡' : '打卡' }}
                    </button>
                  </div>
                  <div v-else class="completed-badge">
                    <i class="bi bi-check-circle-fill"></i>
                    <span>{{ formatDisplayDate(detail.checkInDate || detail.updateTime) }} 完成</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="card mb-4 group-plan-card">
          <div class="card-header d-flex justify-content-between align-items-center">
            <h5 class="mb-0"><i class="bi bi-people"></i> 小组学习</h5>
            <div class="d-flex gap-2">
              <button v-if="groupInfo?.isOwner && !groupInfo?.isGroupPlan" class="btn btn-sm btn-primary" @click="enableGroupPlan">
                <i class="bi bi-person-plus"></i> 开启小组计划</button>
              <button v-if="groupInfo?.isGroupPlan && !groupInfo?.isMember" class="btn btn-sm btn-success" @click="joinGroupPlan">
                <i class="bi bi-box-arrow-in-right"></i> 加入
              </button>
              <button v-if="groupInfo?.isGroupPlan && groupInfo?.isMember && !groupInfo?.isOwner" class="btn btn-sm btn-outline-danger" @click="leaveGroupPlan">
                <i class="bi bi-box-arrow-right"></i> 退出</button>
            </div>
          </div>
          <div class="card-body">
            <div v-if="!groupInfo?.isGroupPlan" class="text-muted">
              开启后，其他同学可以通过邀请码加入这个计划，并在同一个任务节奏下分别打卡、查看进度和讨论。
            </div>
            <div v-else>
              <div class="group-meta mb-3">
                <span class="badge bg-primary">组员 {{ groupInfo.memberCount || 0 }}</span>
                <span v-if="groupInfo.inviteCode" class="invite-code">邀请码: <strong>{{ groupInfo.inviteCode }}</strong></span>
                <div v-if="!groupInfo.isMember" class="join-box">
                  <input v-model.trim="inviteCodeInput" class="form-control form-control-sm" placeholder="输入邀请码">
                </div>
              </div>

              <div class="member-progress-list">
                <div v-for="member in groupInfo.members || []" :key="member.userId" class="member-progress-item">
                  <div class="member-avatar">
                    <img v-if="member.avatar" :src="member.avatar" :alt="member.username">
                    <i v-else class="bi bi-person-circle"></i>
                  </div>
                  <div class="member-main">
                    <div class="d-flex justify-content-between align-items-center">
                      <strong>{{ member.username }}</strong>
                      <span class="text-muted small">{{ formatNumber(member.progress || 0) }}%</span>
                    </div>
                    <div class="progress member-progress">
                      <div class="progress-bar" :style="{ width: `${member.progress || 0}%` }"></div>
                    </div>
                    <div class="small text-muted">
                      已完成 {{ member.completedTasks }} / {{ member.totalTasks }}，学习 {{ formatNumber(member.studyHours || 0) }} 小时
                    </div>
                  </div>
                </div>
              </div>

              <div v-if="groupInfo.isMember" class="group-chat-entry mt-4">
                <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3">
                  <div>
                    <strong><i class="bi bi-chat-dots"></i> 小组讨论已合并到聊天室</strong>
                    <div class="small text-muted mt-1">进入聊天室后会自动切换到这个学习计划对应的小组房间。</div>
                  </div>
                  <button class="btn btn-outline-primary" @click="goToGroupChat">
                    <i class="bi bi-box-arrow-up-right"></i> 进入小组聊天室
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="plan" class="copilot-widget" :class="{ open: copilotOpen }">
      <button type="button" class="btn btn-light copilot-toggle" @click="copilotOpen = !copilotOpen">
        <i class="bi bi-robot"></i>
        <span>AI学习副驾</span>
      </button>

      <div v-if="copilotOpen" class="copilot-panel">
        <div class="copilot-head">
          <strong>AI学习副驾</strong>
          <button type="button" @click="copilotOpen = false"><i class="bi bi-x"></i></button>
        </div>

        <div class="copilot-messages">
          <p class="ai">我会结合当前计划、今日任务、打卡记录和延误情况，给你下一步建议。</p>
          <p
            v-for="(message, index) in advisorMessages"
            :key="index"
            :class="message.role"
          >
            {{ message.content }}
          </p>
          <p v-if="advisorLoading" class="ai">正在分析当前计划...</p>
        </div>

        <form class="copilot-form" @submit.prevent="askPlanAdvisor">
          <input
            v-model.trim="advisorQuestion"
            class="form-control"
            type="text"
            placeholder="例如：今天任务太难怎么办？"
          >
          <button class="btn btn-primary" type="submit" :disabled="advisorLoading">
            <span v-if="advisorLoading" class="spinner-border spinner-border-sm"></span>
            <i v-else class="bi bi-send"></i>
          </button>
        </form>
      </div>
    </div>

    <Footer />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import Navbar from '../components/Navbar.vue'
import Footer from '../components/Footer.vue'
import { planApi } from '../api/plan'
import { checkinApi } from '../api/checkin'
import { showToast } from '../utils/toast'
import { formatNumber } from '../utils/format'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const planId = route.params.id

const loading = ref(true)
const plan = ref(null)
const details = ref([])
const progress = ref(0)
const expandedDetailId = ref(null)
const studyHoursByDetail = ref({})
const showCelebration = ref(false)
const viewMode = ref('list') // 'list' or 'grid'
const groupInfo = ref(null)
const inviteCodeInput = ref('')
const copilotOpen = ref(false)
const advisorQuestion = ref('')
const advisorMessages = ref([])
const advisorLoading = ref(false)
const latestCheckIn = ref(null)

const completedCount = computed(() => {
  return details.value.filter(d => d.isCompleted).length
})

const remainingCount = computed(() => {
  return Math.max(details.value.length - completedCount.value, 0)
})

const completionRate = computed(() => {
  if (!details.value.length) return 0
  return (completedCount.value / details.value.length) * 100
})

const nextPendingDetail = computed(() => {
  return details.value.find(detail => !detail.isCompleted) || null
})

onMounted(() => {
  loadPlanDetail()
})

watch(() => route.query.refresh, () => {
  loadPlanDetail()
})

async function loadPlanDetail() {
  loading.value = true
  try {
    const result = await planApi.getPlanDetail(planId)
    if (result && result.code === 200) {
      plan.value = result.data.plan
      details.value = result.data.details
      progress.value = result.data.progress
      groupInfo.value = result.data.group || null
      studyHoursByDetail.value = Object.fromEntries(
        details.value.map(detail => [detail.id, Number(detail.duration || plan.value?.dailyHours || 1)])
      )
      // 进入详情页时默认全部收起，由用户手动展开需要查看的任务
      expandedDetailId.value = null
    } else {
      showToast(result?.message || t('myPlans.loadFailed'), 'error')
    }
  } catch (error) {
    console.error('加载计划详情失败:', error)
    showToast(t('myPlans.loadFailed'), 'error')
  } finally {
    loading.value = false
  }
}

async function enableGroupPlan() {
  try {
    const result = await planApi.enableGroupPlan(planId)
    if (result?.code === 200) {
      groupInfo.value = result.data
      showToast('小组计划已开启', 'success')
    } else {
      showToast(result?.message || '开启小组计划失败', 'error')
    }
  } catch (error) {
    console.error('开启小组计划失败:', error)
    showToast('开启小组计划失败', 'error')
  }
}

async function joinGroupPlan() {
  if (!inviteCodeInput.value) {
    showToast('请输入邀请码', 'warning')
    return
  }
  try {
    const result = await planApi.joinGroupPlan(planId, inviteCodeInput.value)
    if (result?.code === 200) {
      groupInfo.value = result.data
      showToast('已加入小组计划', 'success')
    } else {
      showToast(result?.message || '加入小组计划失败', 'error')
    }
  } catch (error) {
    console.error('加入小组计划失败:', error)
    showToast('加入小组计划失败', 'error')
  }
}

async function leaveGroupPlan() {
  try {
    const result = await planApi.leaveGroupPlan(planId)
    if (result?.code === 200) {
      groupInfo.value = result.data
      showToast('已退出小组计划', 'success')
    } else {
      showToast(result?.message || '退出小组计划失败', 'error')
    }
  } catch (error) {
    console.error('退出小组计划失败:', error)
    showToast('退出小组计划失败', 'error')
  }
}


function goToGroupChat() {
  router.push({
    path: '/chat',
    query: {
      roomType: 'plan',
      roomId: String(planId)
    }
  })
}

async function askPlanAdvisor() {
  if (!advisorQuestion.value) return
  const question = advisorQuestion.value
  advisorQuestion.value = ''
  advisorMessages.value.push({ role: 'user', content: question })
  advisorLoading.value = true
  try {
    const result = await planApi.askPlanAdvisor(planId, {
      question,
      latestCheckIn: latestCheckIn.value
    })
    if (result?.code === 200) {
      advisorMessages.value.push({
        role: 'ai',
        content: result.data?.reply || '建议先完成最近一个未完成任务，再根据实际用时微调后续安排。'
      })
    } else {
      advisorMessages.value.push({ role: 'ai', content: result?.message || 'AI学习副驾驶暂时不可用' })
      showToast(result?.message || 'AI学习副驾驶暂时不可用', 'error')
    }
  } catch (error) {
    console.error('AI学习副驾驶调用失败:', error)
    advisorMessages.value.push({ role: 'ai', content: 'AI学习副驾驶暂时不可用，请稍后再试。' })
    showToast('AI学习副驾驶暂时不可用', 'error')
  } finally {
    advisorLoading.value = false
  }
}

function getStatusClass(status) {
  const map = {
    '进行中': 'bg-success',
    '已完成': 'bg-info',
    '已暂停': 'bg-warning'
  }
  return map[status] || 'bg-secondary'
}

function toggleDetail(detail) {
  expandedDetailId.value = expandedDetailId.value === detail.id ? null : detail.id
}

function toggleGridDetail(detail) {
  if (detail.isCompleted) return
  // 网格视图里暂时不展开额外详情，保持当前交互即可。
}

function adjustStudyHours(detail, delta) {
  const current = Number(studyHoursByDetail.value[detail.id] || detail.duration || 1)
  const next = Math.min(24, Math.max(0.1, current + delta))
  studyHoursByDetail.value[detail.id] = Number(next.toFixed(1))
}

function getTaskContent(detail) {
  const content = String(detail?.content || '').trim()
  return content || '暂无学习内容，请到编排计划中补充。'
}

function getExpectedDate(detail) {
  if (detail?.scheduledDate) return detail.scheduledDate
  if (!plan.value?.startDate || !detail?.dayNumber) return null
  const date = new Date(`${plan.value.startDate}T00:00:00`)
  date.setDate(date.getDate() + Number(detail.dayNumber) - 1)
  return date
}

function formatDisplayDate(dateValue) {
  if (!dateValue) return '-'
  let value
  if (dateValue instanceof Date) {
    value = dateValue
  } else if (Array.isArray(dateValue)) {
    const [year, month, day] = dateValue
    value = new Date(Number(year), Number(month) - 1, Number(day))
  } else {
    value = new Date(`${String(dateValue).slice(0, 10)}T00:00:00`)
  }
  return value.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

function isEarlyCheckIn(detail) {
  const expectedValue = getExpectedDate(detail)
  if (!expectedValue) return false
  const expected = parseDateValue(expectedValue)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  expected.setHours(0, 0, 0, 0)
  return today < expected
}

function parseDateValue(dateValue) {
  if (dateValue instanceof Date) {
    return new Date(dateValue)
  }
  if (Array.isArray(dateValue)) {
    const [year, month, day] = dateValue
    return new Date(Number(year), Number(month) - 1, Number(day))
  }
  return new Date(`${String(dateValue).slice(0, 10)}T00:00:00`)
}

function playCheckInCelebration() {
  showCelebration.value = true
  window.setTimeout(() => {
    showCelebration.value = false
  }, 1800)
}

function confettiStyle(index) {
  const colors = ['#3e8df7', '#78b892', '#f3c970', '#f08a9b', '#8fc6ec', '#c7d8eb']
  return {
    left: `${8 + (index * 5) % 84}%`,
    background: colors[index % colors.length],
    animationDelay: `${(index % 6) * 0.08}s`,
    transform: `rotate(${index * 21}deg)`
  }
}

async function checkIn(detail) {
  const studyHours = Number(studyHoursByDetail.value[detail.id])
  if (!studyHours || studyHours <= 0 || studyHours > 24) {
    showToast(t('planDetail.invalidStudyHours'), 'warning')
    return
  }

  try {
    const result = await checkinApi.checkIn({
      planId: plan.value.id,
      detailId: detail.id,
      studyHours,
      note: '完成每日任务'
    })

    if (result && result.code === 200) {
      showToast(t('dashboard.checkInSuccess', { hours: studyHours }), 'success')
      // 更新本地状态
      detail.isCompleted = 1
      detail.checkInDate = result.data?.checkDate || new Date().toISOString().slice(0, 10)
      detail.studyHours = result.data?.studyHours || studyHours
      detail.checkInTime = result.data?.createTime || new Date().toISOString()
      latestCheckIn.value = {
        detailId: detail.id,
        content: detail.content,
        expectedHours: Number(detail.duration || 0),
        actualHours: studyHours,
        checkDate: detail.checkInDate
      }
      playCheckInCelebration()
      // 重新计算进度
      const completed = details.value.filter(d => d.isCompleted).length
      progress.value = (completed / details.value.length) * 100
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
.checkin-row {
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.copilot-widget {
  bottom: 28px;
  position: fixed;
  right: 28px;
  z-index: 1060;
}

.copilot-toggle {
  align-items: center;
  display: flex;
  gap: 8px;
}

.copilot-panel {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(188, 213, 241, 0.58);
  border-radius: 8px;
  box-shadow: var(--sp-shadow);
  margin-bottom: 10px;
  overflow: hidden;
  width: 360px;
}

.copilot-head {
  align-items: center;
  color: var(--sp-ink);
  display: flex;
  justify-content: space-between;
  padding: 12px 14px;
}

.copilot-head button {
  background: transparent;
  border: 0;
  color: var(--sp-muted);
}

.copilot-messages {
  display: grid;
  gap: 8px;
  max-height: 220px;
  overflow: auto;
  padding: 0 14px 12px;
}

.copilot-messages p {
  border-radius: 8px;
  font-size: 13px;
  margin: 0;
  max-width: 88%;
  padding: 10px;
  width: fit-content;
}

.copilot-messages p.ai {
  background: rgba(62, 141, 247, 0.1);
  color: var(--sp-ink);
  justify-self: start;
}

.copilot-messages p.user {
  background: #3e8df7;
  color: #fff;
  justify-self: end;
}

.copilot-form {
  display: grid;
  gap: 8px;
  grid-template-columns: minmax(0, 1fr) 42px;
  padding: 12px 14px 14px;
}

.copilot-form button {
  align-items: center;
  border-radius: 8px;
  display: inline-flex;
  justify-content: center;
  min-height: 42px;
  padding: 0;
}

.study-hours-control {
  width: 220px;
  text-align: left;
}

.study-hours-control .form-label {
  color: var(--bs-secondary-color);
  font-size: 0.875rem;
  font-weight: 600;
}

.study-hours-control input {
  min-width: 64px;
}

.completed-info {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-weight: 600;
}

.group-meta {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.invite-code {
  color: #42526b;
}

.join-box {
  width: min(220px, 100%);
}

.member-progress-list {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.member-progress-item {
  align-items: center;
  background: rgba(255, 255, 255, 0.56);
  border: 1px solid rgba(80, 116, 152, 0.12);
  border-radius: 8px;
  display: flex;
  gap: 0.75rem;
  padding: 0.75rem;
}

.member-avatar {
  align-items: center;
  border-radius: 50%;
  color: #58708c;
  display: flex;
  flex: 0 0 42px;
  height: 42px;
  justify-content: center;
  overflow: hidden;
  width: 42px;
}

.member-avatar img {
  height: 100%;
  object-fit: cover;
  width: 100%;
}

.member-avatar i {
  font-size: 2rem;
}

.member-main {
  min-width: 0;
  width: 100%;
}

.member-progress {
  height: 7px;
  margin: 0.35rem 0;
}

.group-chat-entry {
  border-top: 1px solid rgba(80, 116, 152, 0.14);
  padding-top: 1rem;
}
.checkin-celebration {
  position: fixed;
  inset: 0;
  z-index: 1200;
  pointer-events: none;
  overflow: hidden;
}

.celebration-card {
  position: absolute;
  left: 50%;
  top: 18%;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.85rem 1.15rem;
  color: #fff;
  background:
    linear-gradient(135deg, rgba(62, 141, 247, 0.94), rgba(120, 184, 146, 0.94));
  border-radius: 999px;
  box-shadow: 0 18px 44px rgba(13, 110, 253, 0.28);
  transform: translateX(-50%);
  animation: celebrationPop 1.6s ease forwards;
  font-weight: 700;
}

.plan-detail-page h2,
.plan-detail-page h4,
.plan-detail-page .card-title {
  color: var(--sp-ink);
  font-weight: 800;
}

.plan-detail-page .accordion-body {
  background: rgba(255, 255, 255, 0.42);
  color: var(--sp-ink);
}

.plan-detail-page .card-text {
  color: #42526b;
  line-height: 1.7;
}

.plan-hero-card,
.daily-task-panel,
.group-plan-card {
  backdrop-filter: blur(16px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(246, 251, 255, 0.88));
  border: 1px solid rgba(145, 186, 227, 0.28);
  box-shadow: 0 22px 52px rgba(94, 137, 187, 0.12);
}

.plan-hero-grid {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: minmax(0, 1.8fr) minmax(280px, 0.9fr);
}

.hero-eyebrow {
  color: #4580d9;
  font-size: 0.8rem;
  font-weight: 800;
  letter-spacing: 0.14em;
  margin-bottom: 0.6rem;
  text-transform: uppercase;
}

.hero-title {
  font-size: clamp(2rem, 4vw, 3rem);
  line-height: 1.08;
}

.hero-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  margin-bottom: 1rem;
}

.hero-badge {
  background: rgba(57, 135, 242, 0.1);
  border: 1px solid rgba(57, 135, 242, 0.18);
  border-radius: 999px;
  color: #2f6fc5;
  font-size: 0.92rem;
  font-weight: 700;
  padding: 0.42rem 0.85rem;
}

.hero-description {
  font-size: 1.05rem;
  margin-bottom: 1.25rem;
  max-width: 44rem;
}

.hero-stats {
  display: grid;
  gap: 0.85rem;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.hero-stat {
  background: rgba(239, 247, 255, 0.86);
  border: 1px solid rgba(130, 170, 215, 0.14);
  border-radius: 18px;
  display: grid;
  gap: 0.15rem;
  min-height: 88px;
  padding: 1rem 1.1rem;
}

.hero-stat-label {
  color: #69809f;
  font-size: 0.82rem;
  font-weight: 700;
}

.hero-stat strong {
  color: var(--sp-ink);
  font-size: 1.55rem;
  font-weight: 800;
}

.plan-hero-side {
  min-width: 0;
}

.hero-status-panel {
  background: radial-gradient(circle at top, rgba(109, 184, 255, 0.2), rgba(255, 255, 255, 0.8) 58%);
  border: 1px solid rgba(143, 180, 219, 0.2);
  border-radius: 24px;
  display: grid;
  gap: 1rem;
  padding: 1.2rem;
}

.hero-status-top {
  align-items: center;
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
}

.hero-progress-text {
  color: #58708c;
  font-size: 0.92rem;
  font-weight: 700;
}

.hero-progress-bar {
  height: 12px;
}

.hero-progress-fill {
  background: linear-gradient(90deg, #2f8dff, #78b892);
}

.focus-card {
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(97, 151, 218, 0.14);
  border-radius: 18px;
  display: grid;
  gap: 0.35rem;
  padding: 1rem 1rem 1.05rem;
}

.focus-card.done {
  background: rgba(120, 184, 146, 0.12);
  border-color: rgba(120, 184, 146, 0.22);
}

.focus-label {
  color: #2f6fc5;
  font-size: 0.8rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.focus-card strong {
  color: var(--sp-ink);
  font-size: 1.05rem;
  line-height: 1.4;
}

.focus-card p {
  color: #58708c;
  font-size: 0.92rem;
  line-height: 1.6;
  margin: 0;
}

.hero-workbench-btn {
  justify-self: start;
}

.daily-task-panel {
  border-radius: 24px;
  margin-bottom: 1rem;
  padding: 1.15rem 1.2rem;
}

.daily-task-panel-header {
  align-items: flex-start;
  display: flex;
  gap: 1rem;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.daily-task-hint {
  color: #58708c;
  line-height: 1.6;
  max-width: 52rem;
}

.study-stats-inline {
  display: grid;
  gap: 0.8rem;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.study-stat-tile {
  background: rgba(247, 251, 255, 0.92);
  border: 1px solid rgba(145, 186, 227, 0.18);
  border-radius: 16px;
  display: grid;
  gap: 0.18rem;
  padding: 0.85rem 1rem;
}

.study-stat-tile span {
  color: #6a7d95;
  font-size: 0.8rem;
  font-weight: 700;
}

.study-stat-tile strong {
  color: var(--sp-ink);
  font-size: 1.2rem;
  font-weight: 800;
}

.task-accordion {
  display: grid;
  gap: 0.9rem;
}

.task-list-item {
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(154, 193, 232, 0.22);
  border-radius: 22px;
  box-shadow: 0 14px 34px rgba(97, 132, 173, 0.08);
  overflow: hidden;
  position: relative;
}

.task-list-item::before {
  background: linear-gradient(180deg, rgba(47, 141, 255, 0.82), rgba(120, 184, 146, 0.72));
  bottom: 18px;
  border-radius: 999px;
  content: '';
  left: 14px;
  opacity: 0;
  position: absolute;
  top: 18px;
  transition: opacity 0.16s ease;
  width: 4px;
}

.task-list-item.current::before,
.task-list-item.active::before {
  opacity: 1;
}

.task-list-item.completed::before {
  background: linear-gradient(180deg, rgba(120, 184, 146, 0.8), rgba(120, 184, 146, 0.45));
  opacity: 1;
}

.task-list-trigger {
  align-items: center;
  background: transparent;
  box-shadow: none;
  gap: 1rem;
  padding: 1.15rem 1.3rem 1.15rem 1.65rem;
}

.task-list-trigger:not(.collapsed) {
  background: linear-gradient(180deg, rgba(237, 247, 255, 0.9), rgba(255, 255, 255, 0.86));
}

.task-list-trigger:focus {
  box-shadow: none;
}

.task-list-leading {
  align-items: center;
  display: grid;
  gap: 1rem;
  grid-template-columns: auto minmax(0, 1fr);
  min-width: 0;
  width: 100%;
}

.task-day-chip {
  align-self: start;
  background: rgba(47, 111, 197, 0.1);
  border: 1px solid rgba(47, 111, 197, 0.18);
  border-radius: 999px;
  color: #2f6fc5;
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  padding: 0.45rem 0.7rem;
}

.task-list-copy {
  display: grid;
  gap: 0.45rem;
  min-width: 0;
}

.task-list-copy strong {
  color: var(--sp-ink);
  display: block;
  font-size: 1.05rem;
  line-height: 1.45;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-list-meta {
  color: #6a7d95;
  display: flex;
  flex-wrap: wrap;
  gap: 0.9rem;
  font-size: 0.88rem;
}

.task-list-meta span {
  align-items: center;
  display: inline-flex;
  gap: 0.35rem;
}

.task-list-meta i {
  color: var(--sp-blue);
}

.task-list-trailing {
  align-items: center;
  display: inline-flex;
  gap: 0.75rem;
  margin-left: auto;
  padding-right: 0.65rem;
}

.task-state-badge {
  align-items: center;
  border-radius: 999px;
  display: inline-flex;
  font-size: 0.8rem;
  font-weight: 800;
  gap: 0.42rem;
  justify-content: center;
  min-width: 6.5rem;
  padding: 0.5rem 0.9rem;
  white-space: nowrap;
}

.task-state-badge.pending {
  background: rgba(91, 132, 181, 0.12);
  color: #52729b;
}

.task-state-badge.current {
  background: rgba(47, 141, 255, 0.16);
  box-shadow: inset 0 0 0 1px rgba(47, 141, 255, 0.08);
  color: #1659b4;
}

.task-state-badge.done {
  background: rgba(44, 169, 107, 0.14);
  color: #23905c;
}

.task-state-badge i {
  font-size: 0.84rem;
}

.task-detail-body {
  padding: 0.35rem 1.3rem 1.35rem 1.65rem;
}

.task-detail-summary {
  display: grid;
  gap: 1rem;
}

.task-detail-inline {
  display: grid;
  gap: 0.85rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.task-detail-block {
  background: rgba(244, 249, 255, 0.86);
  border: 1px solid rgba(145, 186, 227, 0.14);
  border-radius: 16px;
  padding: 0.9rem 1rem;
}

.task-detail-block.compact {
  min-height: 100%;
}

.summary-label {
  color: #6a7d95;
  display: block;
  font-size: 0.78rem;
  font-weight: 800;
  letter-spacing: 0.06em;
  margin-bottom: 0.35rem;
  text-transform: uppercase;
}

.task-detail-block p {
  color: var(--sp-ink);
  line-height: 1.65;
  margin: 0;
}

/* 鏍煎瓙瑙嗗浘鏍峰紡 */
.task-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
}

/* 澶氬垪甯冨眬锛堢敤浜庡揩閫熸壂瑙嗘瘡鏃ヤ换鍔★級 */
.task-grid-three-cols {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

@media (min-width: 1400px) {
  .task-grid-three-cols {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (min-width: 992px) and (max-width: 1399px) {
  .task-grid-three-cols {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

.task-card {
  background: rgba(255, 255, 255, 0.66);
  border: 1px solid rgba(62, 141, 247, 0.15);
  border-radius: 8px;
  cursor: pointer;
  display: grid;
  grid-template-rows: auto minmax(132px, 1fr) auto;
  min-height: 296px;
  overflow: hidden;
  transition: border-color 0.16s ease, background-color 0.16s ease;
}

.task-card:hover {
  background: rgba(255, 255, 255, 0.84);
  border-color: rgba(62, 141, 247, 0.3);
}

.task-card.task-completed {
  background: rgba(120, 184, 146, 0.09);
  border-color: rgba(120, 184, 146, 0.3);
}

.task-card.task-current {
  border-color: rgba(47, 141, 255, 0.4);
  box-shadow: 0 18px 36px rgba(47, 141, 255, 0.14);
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding: 12px 12px 0;
}

.day-badge {
  background: rgba(62, 141, 247, 0.12);
  border: 1px solid rgba(62, 141, 247, 0.24);
  color: #2f6fc5;
  max-width: 112px;
  overflow: hidden;
  padding: 5px 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 800;
}

.task-status {
  align-items: center;
  border-radius: 999px;
  color: #6a7d95;
  display: inline-flex;
  flex: 0 0 auto;
  font-size: 12px;
  font-weight: 800;
  gap: 4px;
  justify-content: center;
  min-width: 86px;
  padding: 0.42rem 0.72rem;
  white-space: nowrap;
}

.task-status.done {
  background: rgba(44, 169, 107, 0.14);
  color: #2ca96b;
}

.task-status.current {
  background: rgba(47, 141, 255, 0.14);
  color: #1659b4;
}

.task-status.pending {
  background: rgba(91, 132, 181, 0.12);
  color: #52729b;
}

.task-status i {
  font-size: 13px;
}

.badge-sm {
  font-size: 0.75rem;
  padding: 0.25rem 0.5rem;
}

.task-content {
  display: grid;
  gap: 8px;
  grid-template-rows: auto minmax(44px, auto) auto;
  min-height: 0;
  padding: 14px 12px 12px;
}

.task-content-label {
  color: #2f6fc5;
  font-size: 12px;
  font-weight: 800;
}

.grid-focus-tag {
  align-self: start;
  align-items: center;
  background: rgba(47, 141, 255, 0.12);
  border-radius: 999px;
  color: #1659b4;
  display: inline-flex;
  font-size: 11px;
  font-weight: 800;
  gap: 0.32rem;
  justify-self: start;
  padding: 0.28rem 0.55rem;
  white-space: nowrap;
}

.task-title {
  color: var(--sp-ink);
  display: -webkit-box;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.45;
  margin: 0;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.task-meta {
  display: grid;
  gap: 6px;
  font-size: 13px;
  color: #58708c;
}

.task-meta span {
  align-items: center;
  display: flex;
  gap: 6px;
  min-width: 0;
}

.task-meta i {
  color: var(--sp-blue);
}

.task-footer {
  border-top: 1px solid rgba(80, 116, 152, 0.12);
  padding: 10px 12px 12px;
}

.footer-actions {
  display: grid;
  gap: 8px;
}

.hours-label {
  color: #58708c;
  font-size: 12px;
  font-weight: 800;
}

.hours-input {
  align-items: center;
  display: grid;
  gap: 6px;
  grid-template-columns: 32px minmax(0, 1fr) 32px auto;
}

.hours-input input {
  min-width: 0;
  text-align: center;
}

.hours-unit {
  color: #58708c;
  font-size: 12px;
  font-weight: 700;
}

.btn-xs {
  align-items: center;
  border-radius: 6px;
  display: inline-flex;
  font-size: 12px;
  font-weight: 800;
  justify-content: center;
  min-height: 32px;
  padding: 0 8px;
}

.footer-actions .btn-success {
  width: 100%;
}

.completed-badge {
  align-items: center;
  color: #2ca96b;
  display: flex;
  font-size: 13px;
  font-weight: 800;
  gap: 7px;
  min-height: 32px;
}

[data-bs-theme='dark'] .copilot-toggle {
  background: #dff1ff;
  border-color: rgba(223, 241, 255, 0.9);
  color: #0c2b4c;
}

[data-bs-theme='dark'] .copilot-panel {
  background: rgba(15, 24, 38, 0.94);
  border-color: rgba(188, 213, 241, 0.14);
  box-shadow: none;
}

[data-bs-theme='dark'] .copilot-head,
[data-bs-theme='dark'] .copilot-messages p.ai {
  color: #eef7ff;
}

[data-bs-theme='dark'] .copilot-messages p.ai {
  background: rgba(141, 197, 255, 0.12);
  border-color: rgba(141, 197, 255, 0.18);
}

[data-bs-theme='dark'] .copilot-messages p.user {
  background: #2f6fc5;
  color: #fff;
}

[data-bs-theme='dark'] .task-card {
  background: rgba(17, 24, 34, 0.6);
  border-color: rgba(62, 141, 247, 0.2);
}

[data-bs-theme='dark'] .task-card.task-completed {
  background: rgba(120, 184, 146, 0.15);
  border-color: rgba(120, 184, 146, 0.3);
}

[data-bs-theme='dark'] .plan-hero-card,
[data-bs-theme='dark'] .daily-task-panel,
[data-bs-theme='dark'] .group-plan-card,
[data-bs-theme='dark'] .task-list-item {
  background: linear-gradient(180deg, rgba(15, 24, 38, 0.92), rgba(20, 31, 47, 0.88));
  border-color: rgba(138, 171, 208, 0.16);
  box-shadow: none;
}

[data-bs-theme='dark'] .hero-badge,
[data-bs-theme='dark'] .study-stat-tile,
[data-bs-theme='dark'] .task-detail-block,
[data-bs-theme='dark'] .focus-card,
[data-bs-theme='dark'] .hero-stat {
  background: rgba(26, 39, 57, 0.84);
  border-color: rgba(138, 171, 208, 0.14);
}

[data-bs-theme='dark'] .hero-status-panel,
[data-bs-theme='dark'] .task-list-trigger:not(.collapsed) {
  background: rgba(18, 30, 47, 0.9);
}

[data-bs-theme='dark'] .hero-description,
[data-bs-theme='dark'] .daily-task-hint,
[data-bs-theme='dark'] .focus-card p,
[data-bs-theme='dark'] .task-list-meta,
[data-bs-theme='dark'] .study-stat-tile span,
[data-bs-theme='dark'] .hero-stat-label,
[data-bs-theme='dark'] .summary-label {
  color: #a8bad1;
}

[data-bs-theme='dark'] .task-list-copy strong,
[data-bs-theme='dark'] .task-detail-block p,
[data-bs-theme='dark'] .hero-stat strong,
[data-bs-theme='dark'] .focus-card strong,
[data-bs-theme='dark'] .study-stat-tile strong {
  color: #eef7ff;
}

[data-bs-theme='dark'] .task-card.task-current {
  border-color: rgba(125, 191, 255, 0.42);
  box-shadow: none;
}

[data-bs-theme='dark'] .task-title {
  color: var(--sp-muted);
}

[data-bs-theme='dark'] .task-card:hover {
  background: rgba(17, 24, 34, 0.78);
}

[data-bs-theme='dark'] .day-badge {
  background: rgba(77, 166, 255, 0.16);
  border-color: rgba(125, 191, 255, 0.34);
  color: #8dc8ff;
}

[data-bs-theme='dark'] .task-meta,
[data-bs-theme='dark'] .task-content-label,
[data-bs-theme='dark'] .hours-label,
[data-bs-theme='dark'] .hours-unit {
  color: #9fb6cf;
}

@media (max-width: 768px) {
  .plan-hero-grid,
  .hero-stats,
  .study-stats-inline,
  .task-detail-inline {
    grid-template-columns: 1fr;
  }

  .daily-task-panel-header,
  .task-list-leading {
    grid-template-columns: 1fr;
  }

  .daily-task-panel-header {
    display: grid;
  }

  .task-list-copy strong {
    white-space: normal;
  }

  .task-list-trigger {
    align-items: flex-start;
    padding-right: 3.1rem;
  }

  .task-list-trailing {
    display: none;
  }

  .task-grid {
    grid-template-columns: 1fr;
  }
  
  .task-grid-three-cols {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 576px) {
  .plan-hero-card .card-body,
  .daily-task-panel,
  .group-plan-card .card-body,
  .group-plan-card .card-header {
    padding-left: 1rem;
    padding-right: 1rem;
  }

  .hero-title {
    font-size: 1.85rem;
  }

  .hero-status-top {
    align-items: flex-start;
    flex-direction: column;
  }

  .copilot-widget {
    bottom: 14px;
    left: 14px;
    right: 14px;
  }

  .copilot-panel {
    width: 100%;
  }
}

[data-bs-theme='dark'] .plan-detail-page .accordion-body {
  background: rgba(17, 24, 34, 0.46);
}

[data-bs-theme='dark'] .plan-detail-page .card-text {
  color: var(--sp-muted);
}

.celebration-card i {
  font-size: 1.25rem;
}

.confetti {
  position: absolute;
  top: -18px;
  width: 9px;
  height: 16px;
  border-radius: 2px;
  opacity: 0.95;
  animation: confettiFall 1.6s ease-in forwards;
}

@keyframes celebrationPop {
  0% {
    opacity: 0;
    transform: translate(-50%, 14px) scale(0.92);
  }
  18%,
  78% {
    opacity: 1;
    transform: translate(-50%, 0) scale(1);
  }
  100% {
    opacity: 0;
    transform: translate(-50%, -8px) scale(0.98);
  }
}

@keyframes confettiFall {
  0% {
    opacity: 0;
    transform: translateY(-20px) rotate(0deg);
  }
  10% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: translateY(72vh) rotate(420deg);
  }
}

@media (max-width: 576px) {
  .checkin-row {
    align-items: stretch;
    justify-content: stretch;
  }

  .task-detail-body {
    padding-left: 1rem;
    padding-right: 1rem;
  }

  .study-hours-control,
  .checkin-row .btn-success {
    width: 100%;
  }
}
</style>


