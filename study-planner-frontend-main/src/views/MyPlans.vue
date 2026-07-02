<template>
  <div class="my-plans-page app-shell d-flex flex-column min-vh-100">
    <Navbar />

    <main class="container page-surface my-5 flex-grow-1">
      <section class="plans-hero">
        <div class="hero-copy">
          <p class="page-kicker">Your roadmap</p>
          <h1 class="page-title">
            <i class="bi bi-journal-text"></i>
            {{ $t('myPlans.title') }}
          </h1>
          <p class="hero-subtitle">
            管理个人学习路径，加入学习小组的入口已经整合到学习聊天室里。
          </p>
        </div>

        <router-link to="/create-plan" class="btn btn-primary hero-action">
          <i class="bi bi-plus-lg"></i>
          {{ $t('myPlans.createNew') }}
        </router-link>
      </section>

      <div v-if="loading" class="state-shell">
        <div class="spinner-border text-primary" role="status"></div>
        <p class="mt-3 text-muted">{{ $t('myPlans.loading') }}</p>
      </div>

      <template v-else>
        <section v-if="plans.length > 0" class="toolbar-panel surface-panel mb-4">
          <div class="search-box">
            <i class="bi bi-search"></i>
            <input
              v-model="searchKeyword"
              type="text"
              class="form-control"
              :placeholder="$t('myPlans.searchPlaceholder')"
            >
            <button
              v-if="searchKeyword"
              type="button"
              class="btn btn-sm btn-link clear-search"
              @click="searchKeyword = ''"
            >
              <i class="bi bi-x-lg"></i>
            </button>
          </div>

          <div class="status-filter" role="group" aria-label="计划状态筛选">
            <button
              type="button"
              class="filter-chip"
              :class="{ active: selectedStatus === '' }"
              @click="selectedStatus = ''"
            >
              <i class="bi bi-list-ul"></i>
              {{ $t('myPlans.all') }}
            </button>
            <button
              type="button"
              class="filter-chip"
              :class="{ active: selectedStatus === t('plan.inProgress') }"
              @click="selectedStatus = t('plan.inProgress')"
            >
              <i class="bi bi-play-circle"></i>
              {{ $t('myPlans.inProgress') }}
            </button>
            <button
              type="button"
              class="filter-chip"
              :class="{ active: selectedStatus === t('plan.completed') }"
              @click="selectedStatus = t('plan.completed')"
            >
              <i class="bi bi-check-circle"></i>
              {{ $t('myPlans.completed') }}
            </button>
          </div>

          <div v-if="selectedStatus || searchKeyword" class="filter-note">
            <span>{{ $t('myPlans.filterResult', { filtered: filteredPlans.length, total: plans.length }) }}</span>
            <button class="btn btn-link btn-sm p-0" @click="clearFilters">
              {{ $t('myPlans.clearFilter') }}
            </button>
          </div>
        </section>

        <section v-if="plans.length === 0" class="empty-panel surface-panel">
          <div class="empty-illustration">
            <i class="bi bi-journal-x"></i>
          </div>
          <h3>{{ $t('myPlans.noPlans') }}</h3>
          <p>{{ $t('myPlans.noPlansDesc') }}</p>
          <router-link to="/create-plan" class="btn btn-primary btn-lg empty-action">
            <i class="bi bi-plus-lg"></i>
            {{ $t('myPlans.createNew') }}
          </router-link>
        </section>

        <section v-else-if="filteredPlans.length === 0" class="empty-panel surface-panel">
          <div class="empty-illustration subtle">
            <i class="bi bi-search"></i>
          </div>
          <h3>{{ $t('myPlans.noMatch') }}</h3>
          <p>{{ $t('myPlans.noMatchDesc') }}</p>
          <button class="btn btn-primary" @click="clearFilters">
            <i class="bi bi-arrow-counterclockwise"></i>
            {{ $t('myPlans.clearFilter') }}
          </button>
        </section>

        <section v-else class="plans-grid">
          <article v-for="plan in filteredPlans" :key="plan.id" class="plan-card surface-panel">
            <div class="plan-card-head">
              <div class="plan-title-wrap">
                <span v-if="isPinned(plan.id)" class="pin-mark" :title="$t('myPlans.pinned')">
                  <i class="bi bi-pin-angle-fill"></i>
                </span>
                <h3>{{ plan.subject || plan.title }}</h3>
              </div>

              <span :class="['status-pill', getStatusClass(plan.status)]">
                {{ getStatusText(plan.status) }}
              </span>
            </div>

            <div class="plan-meta">
              <span><i class="bi bi-calendar3"></i> {{ formatDate(plan.createTime) }}</span>
              <span><i class="bi bi-clock"></i> {{ formatNumber(plan.duration || plan.totalDays || 0) }}{{ $t('myPlans.days') }}</span>
            </div>

            <p class="plan-goal">{{ truncateText(plan.goal || $t('forum.myContent.noTarget'), 88) }}</p>

            <div class="progress-block">
              <div class="progress-row">
                <span>{{ $t('myPlans.learningProgress') }}</span>
                <strong>{{ formatNumber(plan.progress || 0) }}%</strong>
              </div>
              <div class="progress plan-progress">
                <div class="progress-bar" :style="{ width: `${plan.progress || 0}%` }"></div>
              </div>
            </div>

            <div class="plan-actions">
              <button class="btn btn-sm action-btn action-btn-view" @click="viewDetail(plan.id)">
                <i class="bi bi-eye"></i>
                {{ $t('myPlans.viewDetail') }}
              </button>
              <button class="btn btn-sm action-btn action-btn-edit" @click="openEditModal(plan)">
                <i class="bi bi-pencil"></i>
                {{ $t('myPlans.edit') }}
              </button>
              <button class="btn btn-sm action-btn action-btn-workbench" @click="openWorkbench(plan.id)">
                <i class="bi bi-calendar3"></i>
                编排
              </button>
              <button
                class="btn btn-sm action-btn action-btn-pin"
                :title="isPinned(plan.id) ? $t('myPlans.unpin') : $t('myPlans.pin')"
                @click="togglePin(plan.id)"
              >
                <i :class="isPinned(plan.id) ? 'bi bi-pin-angle-fill' : 'bi bi-pin-angle'"></i>
                {{ isPinned(plan.id) ? $t('myPlans.pinned') : $t('myPlans.pin') }}
              </button>
              <button class="btn btn-sm action-btn action-btn-delete" @click="confirmDeletePlan(plan)">
                <i class="bi bi-trash"></i>
                {{ $t('myPlans.delete') }}
              </button>
            </div>
          </article>
        </section>

        <section class="bottom-summary-grid">
          <div class="summary-panel surface-panel join-group-panel">
            <div class="join-group-copy">
              <span class="summary-label">加入他人的学习小组</span>
              <strong>通过邀请码复制同款计划</strong>
              <p class="mb-0">加入后会自动创建一份相同的学习计划，并从今天开始安排。</p>
            </div>
            <div class="join-group-form">
              <input
                v-model.trim="groupInviteCode"
                type="text"
                class="form-control"
                placeholder="输入邀请码"
                @keyup.enter="joinGroupAndClonePlan"
              >
              <button
                type="button"
                class="btn btn-primary"
                :disabled="joiningGroup"
                @click="joinGroupAndClonePlan"
              >
                <span v-if="joiningGroup" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                <i v-else class="bi bi-box-arrow-in-right"></i>
                {{ joiningGroup ? '加入中...' : '加入并创建计划' }}
              </button>
            </div>
          </div>
          <div class="summary-panel surface-panel" aria-label="计划概览">
            <div class="summary-item">
              <span class="summary-label">计划总数</span>
              <strong>{{ plans.length }}</strong>
            </div>
            <div class="summary-item">
              <span class="summary-label">进行中</span>
              <strong>{{ activePlanCount }}</strong>
            </div>
            <div class="summary-item">
              <span class="summary-label">已完成</span>
              <strong>{{ completedPlanCount }}</strong>
            </div>
          </div>
        </section>
      </template>
    </main>

    <div class="modal fade" id="editPlanModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">{{ $t('myPlans.editPlan') }}</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <label for="planTitle" class="form-label">{{ $t('myPlans.planName') }}</label>
              <input id="planTitle" v-model="editingPlan.title" type="text" class="form-control">
            </div>
            <div class="mb-3">
              <label for="planGoal" class="form-label">{{ $t('plan.target') }}</label>
              <textarea id="planGoal" v-model="editingPlan.goal" class="form-control" rows="3"></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">{{ $t('common.cancel') }}</button>
            <button type="button" class="btn btn-primary" @click="savePlan">{{ $t('common.save') }}</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="deleteDialog.visible" class="delete-dialog-backdrop" @click.self="closeDeleteDialog">
      <div class="delete-dialog" role="dialog" aria-modal="true" aria-labelledby="deletePlanDialogTitle">
        <div class="delete-dialog-icon">
          <i class="bi bi-trash3"></i>
        </div>
        <div class="delete-dialog-content">
          <h5 id="deletePlanDialogTitle">删除学习计划</h5>
          <p>
            确定要删除
            <strong>{{ deleteDialog.title || '这个计划' }}</strong>
            吗？删除后将无法恢复。
          </p>
        </div>
        <div class="delete-dialog-actions">
          <button type="button" class="btn delete-cancel-btn" :disabled="deleteDialog.loading" @click="closeDeleteDialog">
            {{ $t('common.cancel') }}
          </button>
          <button type="button" class="btn delete-confirm-btn" :disabled="deleteDialog.loading" @click="deletePlan">
            <span v-if="deleteDialog.loading" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
            <i v-else class="bi bi-trash3"></i>
            {{ deleteDialog.loading ? '删除中...' : '确认删除' }}
          </button>
        </div>
      </div>
    </div>

    <Footer />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { Modal } from 'bootstrap'
import Navbar from '../components/Navbar.vue'
import Footer from '../components/Footer.vue'
import { planApi } from '../api/plan'
import { showToast } from '../utils/toast'
import { formatDate, formatNumber, truncateText } from '../utils/format'

const { t } = useI18n()
const router = useRouter()

const loading = ref(true)
const plans = ref([])
const searchKeyword = ref('')
const selectedStatus = ref('')
const groupInviteCode = ref('')
const joiningGroup = ref(false)
const editingPlan = ref({ id: null, title: '', goal: '' })
const deleteDialog = ref({
  visible: false,
  loading: false,
  planId: null,
  title: ''
})

let editModalInstance = null

const PINNED_PLANS_KEY = 'pinned_plans'
const pinnedPlanIds = ref([])

const activePlanCount = computed(() =>
  plans.value.filter((plan) => ['active', '进行中', t('plan.inProgress')].includes(plan.status)).length
)

const completedPlanCount = computed(() =>
  plans.value.filter((plan) => ['completed', '已完成', t('plan.completed')].includes(plan.status)).length
)

function getPinnedPlans() {
  try {
    const pinned = localStorage.getItem(PINNED_PLANS_KEY)
    const parsedPinned = pinned ? JSON.parse(pinned) : []
    return Array.isArray(parsedPinned) ? parsedPinned : []
  } catch {
    return []
  }
}

function normalizePlanId(planId) {
  return String(planId)
}

function savePinnedPlans(pinnedList) {
  const normalizedPinnedList = pinnedList.map(normalizePlanId)
  pinnedPlanIds.value = normalizedPinnedList
  try {
    localStorage.setItem(PINNED_PLANS_KEY, JSON.stringify(normalizedPinnedList))
  } catch (error) {
    console.error('保存置顶计划列表失败:', error)
  }
}

function isPinned(planId) {
  return pinnedPlanIds.value.includes(normalizePlanId(planId))
}

function togglePin(planId) {
  const normalizedPlanId = normalizePlanId(planId)
  const pinnedList = [...pinnedPlanIds.value]
  const index = pinnedList.indexOf(normalizedPlanId)

  if (index > -1) {
    pinnedList.splice(index, 1)
    showToast(`${t('myPlans.unpin')} ${t('common.success')}`, 'success')
  } else {
    pinnedList.push(normalizedPlanId)
    showToast(t('myPlans.pinned'), 'success')
  }

  savePinnedPlans(pinnedList)
  sortPlans()
}

function sortPlans() {
  const pinnedList = pinnedPlanIds.value
  plans.value.sort((a, b) => {
    const aPinned = pinnedList.includes(normalizePlanId(a.id))
    const bPinned = pinnedList.includes(normalizePlanId(b.id))

    if (aPinned && !bPinned) return -1
    if (!aPinned && bPinned) return 1
    return new Date(b.createTime) - new Date(a.createTime)
  })
}

const filteredPlans = computed(() => {
  let result = [...plans.value]

  if (selectedStatus.value) {
    result = result.filter((plan) => {
      const statusMap = {
        [t('plan.inProgress')]: ['进行中', 'active', t('plan.inProgress')],
        [t('plan.completed')]: ['已完成', 'completed', t('plan.completed')]
      }
      const statusValues = statusMap[selectedStatus.value] || [selectedStatus.value]
      return statusValues.includes(plan.status)
    })
  }

  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.trim().toLowerCase()
    result = result.filter((plan) => {
      const title = String(plan.subject || plan.title || '').toLowerCase()
      const goal = String(plan.goal || '').toLowerCase()
      return title.includes(keyword) || goal.includes(keyword)
    })
  }

  const pinnedList = pinnedPlanIds.value
  result.sort((a, b) => {
    const aPinned = pinnedList.includes(normalizePlanId(a.id))
    const bPinned = pinnedList.includes(normalizePlanId(b.id))

    if (aPinned && !bPinned) return -1
    if (!aPinned && bPinned) return 1
    return new Date(b.createTime) - new Date(a.createTime)
  })

  return result
})

function clearFilters() {
  searchKeyword.value = ''
  selectedStatus.value = ''
}

onMounted(() => {
  pinnedPlanIds.value = getPinnedPlans().map(normalizePlanId)
  loadPlans()
  editModalInstance = new Modal(document.getElementById('editPlanModal'))
})

function openEditModal(plan) {
  editingPlan.value = { ...plan }
  editModalInstance.show()
}

async function savePlan() {
  try {
    const result = await planApi.updatePlan(editingPlan.value.id, {
      title: editingPlan.value.title,
      goal: editingPlan.value.goal
    })
    if (result?.code === 200) {
      showToast(t('myPlans.planUpdateSuccess'), 'success')
      editModalInstance.hide()
      loadPlans()
    } else {
      showToast(result?.message || t('profile.updateFailed'), 'error')
    }
  } catch (error) {
    console.error('更新失败:', error)
    showToast(t('profile.updateFailed'), 'error')
  }
}

async function loadPlans() {
  loading.value = true
  try {
    const result = await planApi.getPlans()
    if (result?.code === 200) {
      plans.value = result.data || []
      sortPlans()
    } else {
      showToast(result?.message || t('myPlans.loadFailed'), 'error')
    }
  } catch (error) {
    console.error('加载计划失败:', error)
    showToast(t('myPlans.loadFailed'), 'error')
  } finally {
    loading.value = false
  }
}

function getStatusClass(status) {
  const map = {
    active: 'status-active',
    进行中: 'status-active',
    completed: 'status-completed',
    已完成: 'status-completed',
    paused: 'status-paused',
    已暂停: 'status-paused'
  }
  return map[status] || 'status-default'
}

function getStatusText(status) {
  const map = {
    active: t('plan.inProgress'),
    completed: t('plan.completed'),
    paused: t('plan.notStarted')
  }
  return map[status] || status
}

function viewDetail(planId) {
  router.push(`/my-plans/${planId}`)
}

function openWorkbench(planId) {
  router.push(`/my-plans/${planId}/workbench`)
}

function confirmDeletePlan(plan) {
  deleteDialog.value = {
    visible: true,
    loading: false,
    planId: plan.id,
    title: plan.subject || plan.title || ''
  }
}

function closeDeleteDialog() {
  if (deleteDialog.value.loading) {
    return
  }

  resetDeleteDialog()
}

function resetDeleteDialog() {
  deleteDialog.value = {
    visible: false,
    loading: false,
    planId: null,
    title: ''
  }
}

async function deletePlan() {
  const planId = deleteDialog.value.planId
  if (!planId) {
    return
  }

  try {
    deleteDialog.value.loading = true
    const result = await planApi.deletePlan(planId)
    if (result?.code === 200) {
      const normalizedPlanId = normalizePlanId(planId)
      const pinnedList = [...pinnedPlanIds.value]
      const index = pinnedList.indexOf(normalizedPlanId)
      if (index > -1) {
        pinnedList.splice(index, 1)
        savePinnedPlans(pinnedList)
      }
      showToast(t('myPlans.planDeleted'), 'success')
      resetDeleteDialog()
      await loadPlans()
    } else {
      showToast(result?.message || t('myPlans.deleteFailed'), 'error')
      deleteDialog.value.loading = false
    }
  } catch (error) {
    console.error('删除失败:', error)
    showToast(t('myPlans.deleteFailed'), 'error')
    deleteDialog.value.loading = false
  }
}

async function joinGroupAndClonePlan() {
  if (!groupInviteCode.value) {
    showToast('请输入邀请码', 'warning')
    return
  }

  joiningGroup.value = true
  try {
    const result = await planApi.joinGroupPlanByCode(groupInviteCode.value)
    if (result?.code === 200) {
      const personalPlanId = result.data?.personalPlanId
      groupInviteCode.value = ''
      showToast(result?.message || '已加入学习小组并创建学习计划', 'success')
      await loadPlans()
      if (personalPlanId) {
        router.push(`/my-plans/${personalPlanId}`)
      }
    } else {
      showToast(result?.message || '加入小组失败', 'error')
    }
  } catch (error) {
    console.error('加入小组失败:', error)
    showToast('加入小组失败', 'error')
  } finally {
    joiningGroup.value = false
  }
}
</script>

<style scoped>
.my-plans-page .page-surface {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  position: relative;
}

.my-plans-page {
  background: #f6f8fb;
}

.plans-hero {
  align-items: flex-end;
  display: flex;
  gap: 1.5rem;
  justify-content: space-between;
}

.hero-copy {
  max-width: 760px;
}

.page-kicker {
  color: #1c6fbe;
  font-size: 0.9rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  margin: 0 0 0.65rem;
  text-transform: uppercase;
}

.page-title {
  align-items: center;
  color: #183153;
  display: flex;
  font-size: clamp(2.5rem, 5vw, 4rem);
  font-weight: 800;
  gap: 1rem;
  line-height: 1.05;
  letter-spacing: -0.04em;
  margin: 0;
}

.page-title i {
  color: #2b79ca;
  font-size: 0.95em;
}

.hero-subtitle {
  color: #62758f;
  font-size: 1.02rem;
  line-height: 1.75;
  margin: 1rem 0 0;
}

.hero-action {
  align-items: center;
  border-radius: 999px;
  display: inline-flex;
  gap: 0.5rem;
  min-height: 48px;
  padding: 0.78rem 1.3rem;
  box-shadow: 0 14px 28px rgba(48, 132, 242, 0.2);
  white-space: nowrap;
}

.surface-panel {
  backdrop-filter: blur(14px);
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(93, 126, 168, 0.12);
  border-radius: 24px;
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.85) inset,
    0 18px 38px rgba(42, 69, 110, 0.09);
}

.bottom-summary-grid {
  display: grid;
  gap: 0.8rem;
  grid-template-columns: 1fr;
  margin-top: 0.9rem;
}

.join-group-panel {
  align-items: center;
  display: grid;
  gap: 0.9rem;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 0.55fr);
  padding: 0.9rem 1rem;
}

.join-group-copy p {
  color: #5f7390;
  font-size: 0.92rem;
  line-height: 1.55;
  margin-top: 0.25rem;
  max-width: 44ch;
}

.join-group-copy strong {
  color: #183153;
  display: block;
  font-size: 0.98rem;
  font-weight: 800;
  line-height: 1.35;
}

.join-group-form {
  display: grid;
  gap: 0.55rem;
}

.join-group-form .form-control,
.join-group-form .btn {
  min-height: 40px;
}

.summary-panel,
.toolbar-panel,
.empty-panel,
.plan-card {
  padding: 1.25rem;
}

.summary-panel {
  display: grid;
  gap: 0.7rem;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.bottom-summary-grid > .summary-panel:not(.join-group-panel) {
  padding: 0.8rem;
}

.summary-item {
  background: linear-gradient(180deg, rgba(246, 250, 255, 0.95), rgba(239, 246, 255, 0.92));
  border: 1px solid rgba(93, 126, 168, 0.1);
  border-radius: 14px;
  box-shadow: 0 8px 18px rgba(42, 69, 110, 0.04);
  padding: 0.7rem 0.8rem;
}

.summary-label {
  color: #74859c;
  display: block;
  font-size: 0.8rem;
  margin-bottom: 0.2rem;
}

.summary-item strong {
  color: #1c3157;
  font-size: 1.28rem;
  font-weight: 800;
}

.toolbar-panel {
  display: grid;
  gap: 0.95rem;
}

.search-box {
  align-items: center;
  background: rgba(248, 251, 255, 0.98);
  border: 1px solid rgba(93, 126, 168, 0.13);
  border-radius: 18px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.92);
  display: flex;
  gap: 0.75rem;
  padding: 0.35rem 0.95rem;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
}

.search-box:focus-within {
  background: rgba(255, 255, 255, 1);
  border-color: rgba(52, 126, 218, 0.34);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.96),
    0 0 0 4px rgba(47, 127, 206, 0.09);
}

.search-box i {
  color: #6b7d95;
}

.search-box .form-control {
  background: transparent;
  border: 0;
  box-shadow: none;
  min-height: 44px;
  padding: 0;
}

.clear-search {
  color: #7b8ca3;
  text-decoration: none;
}

.clear-search:hover,
.clear-search:focus-visible {
  color: #436c9f;
}

.status-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.filter-chip {
  align-items: center;
  background: rgba(247, 250, 255, 0.94);
  border: 1px solid rgba(93, 126, 168, 0.12);
  border-radius: 999px;
  color: #47617f;
  display: inline-flex;
  font-weight: 700;
  gap: 0.45rem;
  min-height: 40px;
  padding: 0 1rem;
  transition: transform 0.18s ease, border-color 0.18s ease, background-color 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}

.filter-chip:hover,
.filter-chip:focus-visible {
  background: rgba(255, 255, 255, 0.98);
  border-color: rgba(72, 130, 203, 0.24);
  box-shadow: 0 10px 18px rgba(42, 69, 110, 0.08);
  color: #294f80;
  transform: translateY(-1px);
}

.filter-chip.active {
  background: linear-gradient(135deg, #2f9ff2, #3298f6);
  border-color: transparent;
  box-shadow: 0 12px 24px rgba(48, 132, 242, 0.22);
  color: #fff;
}

.filter-note {
  align-items: center;
  color: #6f7f95;
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  justify-content: space-between;
}

.filter-note .btn-link {
  color: #2f7fce;
  text-decoration: none;
}

.state-shell,
.empty-panel {
  align-items: center;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 380px;
  text-align: center;
}

.empty-panel h3 {
  color: #203456;
  font-size: 2rem;
  font-weight: 800;
  margin: 1rem 0 0.5rem;
}

.empty-panel p {
  color: #74849a;
  line-height: 1.75;
  margin-bottom: 1.35rem;
  max-width: 560px;
}

.empty-illustration {
  align-items: center;
  background: linear-gradient(180deg, rgba(236, 245, 255, 0.95), rgba(244, 249, 255, 0.95));
  border-radius: 28px;
  color: #73839b;
  display: inline-flex;
  font-size: 4.1rem;
  height: 116px;
  justify-content: center;
  width: 116px;
}

.empty-illustration.subtle {
  font-size: 3.2rem;
}

.empty-action {
  border-radius: 999px;
  min-height: 52px;
  padding-inline: 1.4rem;
}

.plans-grid {
  display: grid;
  gap: 1.1rem;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
}

.plan-card {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(250, 252, 255, 0.9));
  border-color: rgba(93, 126, 168, 0.11);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.9) inset,
    0 16px 32px rgba(42, 69, 110, 0.08);
  display: flex;
  flex-direction: column;
  gap: 1rem;
  min-height: 300px;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.plan-card:hover {
  border-color: rgba(60, 122, 198, 0.18);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.92) inset,
    0 24px 44px rgba(42, 69, 110, 0.12);
  transform: translateY(-3px);
}

.plan-card-head {
  align-items: flex-start;
  display: flex;
  gap: 0.8rem;
  justify-content: space-between;
}

.plan-title-wrap {
  align-items: flex-start;
  display: flex;
  gap: 0.55rem;
  min-width: 0;
}

.plan-title-wrap h3 {
  color: #1d3156;
  font-size: 1.26rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  line-height: 1.32;
  margin: 0;
}

.pin-mark {
  color: #efb535;
  flex: 0 0 auto;
  margin-top: 0.12rem;
}

.status-pill {
  border: 1px solid transparent;
  border-radius: 999px;
  flex: 0 0 auto;
  font-size: 0.82rem;
  font-weight: 800;
  letter-spacing: 0.01em;
  padding: 0.38rem 0.72rem;
}

.status-active {
  background: rgba(54, 181, 113, 0.14);
  border-color: rgba(54, 181, 113, 0.18);
  color: #1f8c58;
}

.status-completed {
  background: rgba(55, 157, 236, 0.14);
  border-color: rgba(55, 157, 236, 0.18);
  color: #2a76be;
}

.status-paused {
  background: rgba(240, 183, 73, 0.18);
  border-color: rgba(240, 183, 73, 0.24);
  color: #a86c12;
}

.status-default {
  background: rgba(126, 141, 160, 0.14);
  border-color: rgba(126, 141, 160, 0.18);
  color: #5b6d82;
}

.plan-meta {
  color: #72839a;
  display: flex;
  flex-wrap: wrap;
  gap: 0.9rem;
  font-size: 0.95rem;
}

.plan-meta span {
  align-items: center;
  display: inline-flex;
  gap: 0.4rem;
}

.plan-goal {
  color: #42556f;
  line-height: 1.7;
  margin: 0;
  min-height: 3.4em;
}

.progress-block {
  margin-top: auto;
  padding-top: 0.15rem;
}

.progress-row {
  align-items: center;
  color: #5f7391;
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.55rem;
}

.progress-row strong {
  color: #1f3359;
}

.plan-progress {
  background: rgba(223, 233, 245, 0.92);
  border-radius: 999px;
  height: 11px;
  overflow: hidden;
}

.plan-progress .progress-bar {
  background: linear-gradient(90deg, #3fa7f5, #49c27d);
  border-radius: 999px;
  box-shadow: 0 6px 14px rgba(63, 167, 245, 0.24);
}

.plan-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.48rem;
  row-gap: 0.55rem;
}

.action-btn {
  align-items: center;
  border: 1px solid transparent;
  border-radius: 999px;
  box-shadow: none;
  display: inline-flex;
  font-size: 0.92rem;
  font-weight: 700;
  gap: 0.34rem;
  justify-content: center;
  line-height: 1;
  min-height: 36px;
  padding: 0.38rem 0.68rem;
  transition: transform 0.18s ease, box-shadow 0.18s ease, background-color 0.18s ease, border-color 0.18s ease, color 0.18s ease;
  white-space: nowrap;
}

.action-btn i {
  font-size: 0.95rem;
}

.action-btn:hover,
.action-btn:focus-visible {
  box-shadow: 0 10px 22px rgba(39, 72, 109, 0.14);
  transform: translateY(-1px);
}

.action-btn-view {
  background: linear-gradient(135deg, #2898f5, #3a7df0);
  border-color: #2b86f2;
  box-shadow: 0 12px 22px rgba(43, 134, 242, 0.2);
  color: #fff;
  padding-inline: 0.78rem;
}

.action-btn-view:hover,
.action-btn-view:focus-visible {
  background: linear-gradient(135deg, #178ced, #2c71e6);
  border-color: #2376e7;
  box-shadow: 0 16px 28px rgba(35, 118, 231, 0.24);
  color: #fff;
}

.join-group-form .form-control {
  background: rgba(248, 251, 255, 0.98);
  border: 1px solid rgba(93, 126, 168, 0.14);
  border-radius: 16px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.88);
  color: #1d3156;
  padding-inline: 0.95rem;
}

.join-group-form .form-control:focus {
  background: #fff;
  border-color: rgba(52, 126, 218, 0.34);
  box-shadow: 0 0 0 4px rgba(47, 127, 206, 0.09);
}

.join-group-form .btn {
  border-radius: 16px;
  box-shadow: 0 14px 28px rgba(48, 132, 242, 0.18);
  font-weight: 700;
}

[data-bs-theme='dark'] .my-plans-page {
  background: #111827;
}

[data-bs-theme='dark'] .surface-panel {
  background: rgba(17, 25, 37, 0.9);
  border-color: rgba(152, 177, 214, 0.14);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.03) inset,
    0 18px 40px rgba(0, 0, 0, 0.3);
}

[data-bs-theme='dark'] .page-kicker,
[data-bs-theme='dark'] .summary-label,
[data-bs-theme='dark'] .filter-note,
[data-bs-theme='dark'] .plan-meta,
[data-bs-theme='dark'] .join-group-copy p,
[data-bs-theme='dark'] .empty-panel p {
  color: #94a8c5;
}

[data-bs-theme='dark'] .page-title,
[data-bs-theme='dark'] .empty-panel h3,
[data-bs-theme='dark'] .summary-item strong,
[data-bs-theme='dark'] .join-group-copy strong,
[data-bs-theme='dark'] .plan-title-wrap h3,
[data-bs-theme='dark'] .progress-row strong {
  color: #ecf4ff;
}

[data-bs-theme='dark'] .page-title i {
  color: #78b9ff;
}

[data-bs-theme='dark'] .summary-item,
[data-bs-theme='dark'] .search-box,
[data-bs-theme='dark'] .filter-chip,
[data-bs-theme='dark'] .join-group-form .form-control {
  background: rgba(24, 34, 49, 0.92);
  border-color: rgba(152, 177, 214, 0.14);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
  color: #d2e2f7;
}

[data-bs-theme='dark'] .search-box:focus-within,
[data-bs-theme='dark'] .join-group-form .form-control:focus {
  background: rgba(21, 31, 45, 0.98);
  border-color: rgba(104, 167, 243, 0.36);
  box-shadow: 0 0 0 4px rgba(61, 133, 215, 0.14);
}

[data-bs-theme='dark'] .search-box i,
[data-bs-theme='dark'] .clear-search {
  color: #9eb3d1;
}

[data-bs-theme='dark'] .clear-search:hover,
[data-bs-theme='dark'] .clear-search:focus-visible {
  color: #dbe8fb;
}

[data-bs-theme='dark'] .filter-chip:hover,
[data-bs-theme='dark'] .filter-chip:focus-visible {
  background: rgba(29, 42, 60, 0.96);
  border-color: rgba(104, 167, 243, 0.26);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.2);
  color: #e7f0ff;
}

[data-bs-theme='dark'] .filter-chip.active {
  box-shadow: 0 14px 24px rgba(46, 126, 223, 0.24);
}

[data-bs-theme='dark'] .plan-card {
  background: linear-gradient(180deg, rgba(18, 27, 40, 0.94), rgba(15, 23, 35, 0.9));
  border-color: rgba(152, 177, 214, 0.12);
}

[data-bs-theme='dark'] .plan-card:hover {
  border-color: rgba(112, 171, 242, 0.2);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.03) inset,
    0 24px 46px rgba(0, 0, 0, 0.34);
}

[data-bs-theme='dark'] .plan-goal,
[data-bs-theme='dark'] .progress-row {
  color: #a8bad4;
}

[data-bs-theme='dark'] .plan-progress {
  background: rgba(50, 65, 86, 0.92);
}

.action-btn-edit {
  background: rgba(76, 143, 233, 0.1);
  border-color: rgba(76, 143, 233, 0.24);
  color: #4c79b8;
}

.action-btn-edit:hover,
.action-btn-edit:focus-visible {
  background: rgba(76, 143, 233, 0.18);
  border-color: rgba(76, 143, 233, 0.34);
  color: #335f9c;
}

.action-btn-workbench {
  background: rgba(59, 150, 241, 0.08);
  border-color: rgba(59, 150, 241, 0.2);
  color: #2f6fc5;
}

.action-btn-workbench:hover,
.action-btn-workbench:focus-visible {
  background: rgba(59, 150, 241, 0.15);
  border-color: rgba(59, 150, 241, 0.3);
  color: #255ea9;
}

.action-btn-pin {
  background: rgba(37, 111, 197, 0.08);
  border-color: rgba(37, 111, 197, 0.18);
  color: #5d84c2;
}

.action-btn-pin:hover,
.action-btn-pin:focus-visible {
  background: rgba(37, 111, 197, 0.14);
  border-color: rgba(37, 111, 197, 0.28);
  color: #3f68aa;
}

.action-btn-delete {
  background: rgba(27, 102, 185, 0.06);
  border-color: rgba(27, 102, 185, 0.16);
  color: #7a9aca;
}

.action-btn-delete:hover,
.action-btn-delete:focus-visible {
  background: rgba(27, 102, 185, 0.12);
  border-color: rgba(27, 102, 185, 0.24);
  color: #4c73b0;
}

[data-bs-theme='dark'] .action-btn-edit {
  background: rgba(109, 173, 255, 0.18);
  border-color: rgba(109, 173, 255, 0.34);
  color: #d5e3ff;
}

[data-bs-theme='dark'] .action-btn-workbench {
  background: rgba(88, 164, 255, 0.14);
  border-color: rgba(88, 164, 255, 0.28);
  color: #c9e3ff;
}

[data-bs-theme='dark'] .action-btn-pin {
  background: rgba(63, 137, 226, 0.12);
  border-color: rgba(63, 137, 226, 0.24);
  color: #b9d5ff;
}

[data-bs-theme='dark'] .action-btn-delete {
  background: rgba(58, 125, 204, 0.1);
  border-color: rgba(58, 125, 204, 0.22);
  color: #9ec4ff;
}

.delete-dialog-backdrop {
  align-items: center;
  background: rgba(17, 28, 45, 0.28);
  backdrop-filter: blur(8px);
  display: flex;
  inset: 0;
  justify-content: center;
  padding: 1rem;
  position: fixed;
  z-index: 1200;
}

.delete-dialog {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(93, 126, 168, 0.14);
  border-radius: 24px;
  box-shadow: 0 24px 60px rgba(35, 58, 94, 0.18);
  max-width: 460px;
  padding: 1.4rem;
  width: min(100%, 460px);
}

.delete-dialog-icon {
  align-items: center;
  background: rgba(49, 132, 253, 0.1);
  border-radius: 16px;
  color: #2f7fce;
  display: inline-flex;
  font-size: 1.25rem;
  height: 48px;
  justify-content: center;
  width: 48px;
}

.delete-dialog-content {
  margin-top: 1rem;
}

.delete-dialog-content h5 {
  color: #19345d;
  font-size: 1.2rem;
  font-weight: 800;
  margin: 0 0 0.55rem;
}

.delete-dialog-content p {
  color: #5d6f89;
  line-height: 1.7;
  margin: 0;
}

.delete-dialog-content strong {
  color: #1f4377;
}

.delete-dialog-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  margin-top: 1.35rem;
}

.delete-cancel-btn,
.delete-confirm-btn {
  align-items: center;
  border-radius: 12px;
  display: inline-flex;
  font-weight: 700;
  gap: 0.45rem;
  justify-content: center;
  min-height: 44px;
  min-width: 112px;
  padding: 0.55rem 1rem;
}

.delete-cancel-btn {
  background: rgba(240, 246, 255, 0.95);
  border: 1px solid rgba(93, 126, 168, 0.16);
  color: #577296;
}

.delete-cancel-btn:hover,
.delete-cancel-btn:focus-visible {
  background: rgba(229, 239, 252, 0.95);
  border-color: rgba(93, 126, 168, 0.24);
  color: #426180;
}

.delete-confirm-btn {
  background: linear-gradient(135deg, #2898f5, #3a7df0);
  border: 1px solid #2b86f2;
  color: #fff;
}

.delete-confirm-btn:hover,
.delete-confirm-btn:focus-visible {
  background: linear-gradient(135deg, #178ced, #2c71e6);
  border-color: #2376e7;
  color: #fff;
}

[data-bs-theme='dark'] .delete-dialog {
  background: rgba(17, 24, 34, 0.96);
  border-color: rgba(255, 255, 255, 0.1);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.35);
}

[data-bs-theme='dark'] .delete-dialog-content h5,
[data-bs-theme='dark'] .delete-dialog-content strong {
  color: #e6f0ff;
}

[data-bs-theme='dark'] .delete-dialog-content p {
  color: #a8b8cf;
}

[data-bs-theme='dark'] .delete-cancel-btn {
  background: rgba(31, 44, 63, 0.96);
  border-color: rgba(132, 160, 204, 0.2);
  color: #c7d9f2;
}

@media (max-width: 991px) {
  .plans-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .bottom-summary-grid {
    grid-template-columns: 1fr;
  }

  .hero-action {
    width: 100%;
  }

  .summary-panel {
    grid-template-columns: 1fr;
  }

  .join-group-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 767px) {
  .page-title {
    font-size: 2.4rem;
  }

  .plan-card {
    min-height: auto;
  }

  .filter-note {
    align-items: flex-start;
    flex-direction: column;
  }

  .delete-dialog-actions {
    flex-direction: column-reverse;
  }

  .delete-cancel-btn,
  .delete-confirm-btn {
    width: 100%;
  }
}
</style>
