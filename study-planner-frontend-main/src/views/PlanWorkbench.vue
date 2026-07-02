<template>
  <div class="plan-workbench-page app-shell">
    <Navbar />

    <main class="container page-surface my-4">
      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border text-primary" role="status"></div>
        <p class="mt-2 text-muted">正在加载计划工作台...</p>
      </div>

      <template v-else-if="plan">
        <section class="workbench-heading">
          <div>
            <p class="page-kicker">Planner board</p>
            <h1 class="page-title">{{ plan.title }}</h1>
            <p class="page-lead">{{ plan.goal }}</p>
          </div>
          <div class="heading-actions">
            <router-link :to="detailRoute" class="btn btn-outline-primary">
              <i class="bi bi-arrow-left"></i> 返回详情
            </router-link>
          </div>
        </section>

        <section class="workbench-stats">
          <div>
            <strong>{{ details.length }}</strong>
            <span>任务数</span>
          </div>
          <div>
            <strong>{{ scheduledDayCount }}</strong>
            <span>排期天数</span>
          </div>
          <div :class="{ warning: conflicts.length > 0 }">
            <strong>{{ conflicts.length }}</strong>
            <span>日期冲突</span>
          </div>
        </section>

        <section v-if="conflicts.length" class="conflict-strip">
          <i class="bi bi-exclamation-triangle"></i>
          <span>有 {{ conflicts.length }} 天安排了多个任务，可保留，也可以拖拽到其他日期分散压力。</span>
        </section>

        <section class="workbench-grid">
          <div class="calendar-card">
            <div class="section-title">
              <div>
                <h2>日历排期</h2>
                <p>把任务拖到目标日期，保存后同步到后端。</p>
              </div>
              <div class="calendar-tools">
                <button class="btn btn-sm btn-outline-secondary" @click="shiftCalendar(-7)">
                  <i class="bi bi-chevron-left"></i>
                </button>
                <button class="btn btn-sm btn-outline-secondary" @click="resetCalendarWindow">回到计划</button>
                <button class="btn btn-sm btn-outline-secondary" @click="shiftCalendar(7)">
                  <i class="bi bi-chevron-right"></i>
                </button>
              </div>
            </div>

            <div class="calendar-grid">
              <article
                v-for="day in calendarDays"
                :key="day.key"
                class="calendar-day"
                :class="{ today: day.isToday, conflict: tasksByDate[day.key]?.length > 1 }"
                @dragover.prevent
                @drop="dropTask(day.key)"
              >
                <header>
                  <span>{{ day.label }}</span>
                </header>
                <div class="day-tasks">
                  <button
                    v-for="task in tasksByDate[day.key]"
                    :key="task.id"
                    type="button"
                    class="task-chip"
                    :draggable="!task.isCompleted"
                    :title="task.content"
                    :class="{ done: task.isCompleted }"
                    @dragstart="dragTask(task)"
                  >
                    <strong>D{{ task.dayNumber }}</strong>
                    <span>{{ task.content }}</span>
                  </button>
                </div>
              </article>
            </div>
          </div>

        </section>

        <section class="editor-card">
          <div class="section-title">
            <div>
              <h2>AI 计划二次编辑器</h2>
              <p>像表格一样修改 AI 生成的每日任务，支持新增、删除和保存版本。</p>
            </div>
            <div class="editor-tools">
              <button class="btn btn-outline-secondary" @click="addLocalTask">
                <i class="bi bi-plus-lg"></i> 添加任务
              </button>
            </div>
          </div>

          <div class="task-table-wrap">
            <table class="task-table">
              <thead>
                <tr>
                  <th>日期</th>
                  <th>任务内容</th>
                  <th>时长</th>
                  <th>状态</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="task in sortedDetails" :key="task.localKey">
                  <td>
                    <input v-model="task.scheduledDate" type="date" class="form-control form-control-sm" @change="markDirty">
                  </td>
                  <td>
                    <textarea v-model.trim="task.content" class="form-control form-control-sm content-input" rows="2" @input="markDirty"></textarea>
                  </td>
                  <td>
                    <input v-model.number="task.duration" type="number" min="0.5" max="24" step="0.5" class="form-control form-control-sm duration-input" @change="markDirty">
                  </td>
                  <td>
                    <select v-model.number="task.isCompleted" class="form-select form-select-sm" @change="markDirty">
                      <option :value="0">未完成</option>
                      <option :value="1">已完成</option>
                    </select>
                  </td>
                  <td>
                    <button class="btn btn-sm btn-outline-danger icon-only" title="删除任务" @click="removeTask(task)">
                      <i class="bi bi-trash"></i>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
        <button class="btn btn-primary floating-save-button" :disabled="saving" @click="saveAll">
          <i class="bi bi-cloud-check"></i> {{ saving ? '保存中...' : '保存编排' }}
        </button>
      </template>

      <div v-else class="empty-state">
        <i class="bi bi-exclamation-circle"></i>
        <h4>计划不存在</h4>
        <router-link to="/my-plans" class="btn btn-primary">返回我的计划</router-link>
      </div>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import Navbar from '../components/Navbar.vue'
import Footer from '../components/Footer.vue'
import { planApi } from '../api/plan'
import { showToast } from '../utils/toast'

const route = useRoute()
const planId = route.params.id

const loading = ref(true)
const saving = ref(false)
const dirty = ref(false)
const plan = ref(null)
const details = ref([])
const conflicts = ref([])
const calendarStart = ref(null)
const draggedTask = ref(null)
const lastSavedAt = ref(null)

const sortedDetails = computed(() => {
  return [...details.value].sort((a, b) => {
    const dateA = a.scheduledDate || ''
    const dateB = b.scheduledDate || ''
    if (dateA !== dateB) return dateA.localeCompare(dateB)
    return Number(a.sortOrder || 0) - Number(b.sortOrder || 0)
  })
})

const tasksByDate = computed(() => {
  const map = {}
  for (const task of sortedDetails.value) {
    const key = task.scheduledDate
    if (!key) continue
    if (!map[key]) map[key] = []
    map[key].push(task)
  }
  return map
})

const calendarAnchorDate = computed(() => {
  const scheduledDates = sortedDetails.value
    .map(task => normalizeDateValue(task.scheduledDate))
    .filter(Boolean)
    .sort()

  if (scheduledDates.length) return scheduledDates[0]
  return normalizeDateValue(plan.value?.startDate) || toDateInputValue(new Date())
})

const calendarDays = computed(() => {
  const start = calendarStart.value
    ? parseDate(calendarStart.value)
    : startOfWeek(parseDate(calendarAnchorDate.value))
  const days = []
  for (let index = 0; index < 42; index++) {
    const date = new Date(start)
    date.setDate(start.getDate() + index)
    const key = toDateInputValue(date)
    days.push({
      key,
      label: `${date.getMonth() + 1}/${date.getDate()}`,
      isToday: key === toDateInputValue(new Date())
    })
  }
  return days
})

const scheduledDayCount = computed(() => Object.keys(tasksByDate.value).length)

const detailRoute = computed(() => ({
  path: `/my-plans/${planId}`,
  query: lastSavedAt.value ? { refresh: lastSavedAt.value } : {}
}))

onMounted(() => {
  loadWorkbench()
})

async function loadWorkbench() {
  loading.value = true
  try {
    const result = await planApi.getSchedule(planId)
    if (result?.code === 200) {
      plan.value = result.data.plan
      details.value = (result.data.details || []).map(normalizeTask)
      conflicts.value = result.data.conflicts || []
      resetCalendarWindow()
      dirty.value = false
    } else {
      showToast(result?.message || '加载计划工作台失败', 'error')
    }
  } catch (error) {
    console.error('加载计划工作台失败:', error)
    showToast('加载计划工作台失败', 'error')
  } finally {
    loading.value = false
  }
}

function normalizeTask(task) {
  return {
    ...task,
    localKey: task.id || `new-${Date.now()}-${Math.random().toString(36).slice(2)}`,
    scheduledDate: normalizeDateValue(task.scheduledDate) || fallbackDate(task.dayNumber),
    sortOrder: task.sortOrder || task.dayNumber || 1,
    duration: Number(task.duration || plan.value?.dailyHours || 1),
    isCompleted: Number(task.isCompleted || 0)
  }
}

function fallbackDate(dayNumber) {
  const date = parseDate(calendarAnchorDate.value)
  date.setDate(date.getDate() + Math.max(0, Number(dayNumber || 1) - 1))
  return toDateInputValue(date)
}

function dragTask(task) {
  if (Number(task.isCompleted) === 1) {
    draggedTask.value = null
    return
  }
  draggedTask.value = task
}

function dropTask(dateKey) {
  if (!draggedTask.value) return
  draggedTask.value.scheduledDate = dateKey
  draggedTask.value.sortOrder = (tasksByDate.value[dateKey]?.length || 0) + 1
  draggedTask.value = null
  markDirty()
}

function markDirty() {
  dirty.value = true
  recomputeConflicts()
}

function recomputeConflicts() {
  conflicts.value = Object.entries(tasksByDate.value)
    .filter(([, items]) => items.length > 1)
    .map(([date, items]) => ({ date, count: items.length, items }))
}

function addLocalTask() {
  const nextDay = details.value.length + 1
  details.value.push(normalizeTask({
    id: null,
    dayNumber: nextDay,
    content: '新学习任务',
    duration: plan.value?.dailyHours || 2,
    scheduledDate: fallbackDate(nextDay),
    sortOrder: nextDay,
    isCompleted: 0
  }))
  markDirty()
}

async function removeTask(task) {
  if (task.id && !confirm('确定删除这个任务吗？')) return

  if (task.id) {
    try {
      const result = await planApi.deleteTask(planId, task.id)
      if (result?.code !== 200) {
        showToast(result?.message || '删除失败', 'error')
        return
      }
      showToast('任务已删除', 'success')
    } catch (error) {
      showToast('删除失败', 'error')
      return
    }
  }

  details.value = details.value.filter(item => item.localKey !== task.localKey)
  markDirty()
}

async function saveAll() {
  saving.value = true
  try {
    const dayOrderMap = new Map()
    const payload = sortedDetails.value.map((task, index) => {
      const dateKey = task.scheduledDate || ''
      const dayOrder = (dayOrderMap.get(dateKey) || 0) + 1
      dayOrderMap.set(dateKey, dayOrder)
      return {
      id: task.id,
      dayNumber: index + 1,
      content: task.content,
      duration: Number(task.duration || 1),
      scheduledDate: task.scheduledDate,
      sortOrder: dayOrder,
      isCompleted: Number(task.isCompleted || 0)
      }
    })

    const result = await planApi.batchUpdateTasks(planId, payload)
    if (result?.code === 200) {
      showToast('计划编排已保存', 'success')
      lastSavedAt.value = Date.now()
      await loadWorkbench()
    } else {
      showToast(result?.message || '保存失败', 'error')
    }
  } catch (error) {
    console.error('保存失败:', error)
    showToast('保存失败', 'error')
  } finally {
    saving.value = false
  }
}

function resetCalendarWindow() {
  calendarStart.value = toDateInputValue(startOfWeek(parseDate(calendarAnchorDate.value)))
}

function shiftCalendar(days) {
  const date = parseDate(calendarStart.value || calendarAnchorDate.value)
  date.setDate(date.getDate() + days)
  calendarStart.value = toDateInputValue(date)
}

function normalizeDateValue(value) {
  if (!value) return ''
  if (Array.isArray(value)) {
    const [year, month, day] = value
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  }
  return String(value).slice(0, 10)
}

function parseDate(value) {
  const normalized = normalizeDateValue(value) || toDateInputValue(new Date())
  return new Date(`${normalized}T00:00:00`)
}

function startOfWeek(date) {
  const result = new Date(date)
  const day = (result.getDay() + 6) % 7
  result.setDate(result.getDate() - day)
  return result
}

function toDateInputValue(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDateTime(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(parseDateTime(value))
}

function parseDateTime(value) {
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value
    return new Date(Number(year), Number(month) - 1, Number(day), Number(hour), Number(minute), Number(second))
  }
  return new Date(value)
}
</script>

<style scoped>
.plan-workbench-page {
  background: #f6f8fb;
  min-height: 100vh;
}

.plan-workbench-page .page-surface {
  padding-bottom: 96px;
  padding-top: 18px;
}

.workbench-heading {
  align-items: end;
  display: grid;
  gap: 24px;
  grid-template-columns: minmax(0, 1fr) auto;
  padding: 22px 0 24px;
}

.workbench-heading .page-title {
  font-size: clamp(2.4rem, 4.8vw, 4.4rem);
  line-height: 0.98;
  margin-bottom: 10px;
}

.workbench-heading .page-lead {
  color: #1f5e9f;
  font-size: 1rem;
  line-height: 1.7;
  margin-bottom: 0;
  max-width: 860px;
}

.heading-actions .btn,
.calendar-tools .btn,
.editor-tools .btn {
  align-items: center;
  background: rgba(255, 255, 255, 0.78);
  border-color: rgba(62, 141, 247, 0.22);
  border-radius: 999px;
  color: #2f6fc5;
  display: inline-flex;
  font-weight: 800;
  gap: 6px;
}

.heading-actions .btn:hover,
.calendar-tools .btn:hover,
.editor-tools .btn:hover {
  background: #fff;
  border-color: rgba(62, 141, 247, 0.42);
  color: #245fae;
  transform: translateY(-1px);
}

.heading-actions,
.editor-tools,
.calendar-tools {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.workbench-stats {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-bottom: 16px;
}

.workbench-stats > div,
.calendar-card,
.editor-card,
.conflict-strip {
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(188, 213, 241, 0.68);
  border-radius: 8px;
  box-shadow: 0 16px 42px rgba(40, 74, 110, 0.09);
}

.workbench-stats > div {
  overflow: hidden;
  padding: 18px 22px;
  position: relative;
}

.workbench-stats > div::before {
  background: linear-gradient(180deg, #3e8df7, rgba(62, 141, 247, 0.12));
  content: "";
  inset: 0 auto 0 0;
  position: absolute;
  width: 4px;
}

.workbench-stats strong {
  color: #10213c;
  display: block;
  font-family: Georgia, "Times New Roman", serif;
  font-size: 2rem;
  font-weight: 500;
  line-height: 1;
}

.workbench-stats span {
  color: #556a84;
  display: block;
  font-size: 0.82rem;
  font-weight: 800;
  margin-top: 8px;
}

.workbench-stats .warning::before {
  background: linear-gradient(180deg, #d58a1f, rgba(213, 138, 31, 0.14));
}

.workbench-stats .warning strong {
  color: #b87514;
}

.conflict-strip {
  align-items: center;
  background: #fff8e8;
  border-color: rgba(224, 158, 42, 0.28);
  color: #8a5a0a;
  display: flex;
  font-weight: 800;
  gap: 10px;
  margin-bottom: 16px;
  padding: 13px 18px;
}

.conflict-strip i {
  color: #c47b11;
}

.workbench-grid {
  display: grid;
  gap: 20px;
  grid-template-columns: minmax(0, 1fr);
  margin-bottom: 22px;
}

.calendar-card,
.editor-card {
  padding: 24px 28px 28px;
}

.adjustment-list {
  display: grid;
  gap: 8px;
  max-height: 240px;
  overflow: auto;
}

.adjustment-list > div {
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(80, 116, 152, 0.12);
  border-radius: 8px;
  padding: 10px;
}

.adjustment-list b {
  color: #2f6fc5;
  font-size: 12px;
}

.adjustment-list p {
  font-size: 13px;
  font-weight: 800;
  margin: 4px 0;
}

.adjustment-list small {
  color: #60738d;
}

.section-title {
  align-items: start;
  display: flex;
  gap: 18px;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-title.compact {
  display: block;
}

.section-title h2 {
  color: #14223a;
  font-size: 1.24rem;
  font-weight: 900;
  margin: 0 0 6px;
}

.section-title p {
  color: #60738d;
  margin: 0;
}

.calendar-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(7, minmax(0, 1fr));
}

.calendar-day {
  background: #f9fbfe;
  border: 1px solid #dfeaf5;
  border-radius: 8px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
  display: flex;
  flex-direction: column;
  min-height: 148px;
  min-width: 0;
  overflow: hidden;
  padding: 12px;
  position: relative;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

.calendar-day:hover {
  border-color: rgba(62, 141, 247, 0.34);
  box-shadow: 0 12px 28px rgba(54, 83, 116, 0.08);
  transform: translateY(-1px);
}

.calendar-day.today {
  background: #fff;
  border-color: #dfeaf5;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.calendar-day.today::before {
  background: #3e8df7;
  border-radius: 0 0 999px 999px;
  content: "";
  height: 3px;
  left: 42%;
  position: absolute;
  right: 42%;
  top: 0;
}

.calendar-day.conflict {
  background: #fffaf0;
  border-color: rgba(213, 138, 31, 0.32);
}

.calendar-day header {
  align-items: center;
  color: #315c78;
  display: flex;
  font-weight: 900;
  justify-content: center;
  margin-bottom: 14px;
  min-width: 0;
  text-align: center;
}

.calendar-day header span {
  display: block;
  width: 100%;
}

.day-tasks {
  display: grid;
  gap: 7px;
  max-height: 100px;
  min-height: 0;
  overflow-y: auto;
  padding-right: 2px;
  scrollbar-width: thin;
}

.day-tasks::-webkit-scrollbar {
  width: 4px;
}

.day-tasks::-webkit-scrollbar-thumb {
  background: rgba(80, 116, 152, 0.24);
  border-radius: 999px;
}

.task-chip {
  background: #eef6ff;
  border: 1px solid rgba(62, 141, 247, 0.24);
  border-radius: 7px;
  box-shadow: 0 8px 16px rgba(62, 141, 247, 0.07);
  color: #17324f;
  cursor: grab;
  display: grid;
  gap: 6px;
  grid-template-columns: auto minmax(0, 1fr);
  min-width: 0;
  padding: 8px;
  text-align: left;
  width: 100%;
}

.task-chip strong {
  align-self: start;
  background: #3e8df7;
  border-radius: 999px;
  color: #fff;
  font-size: 0.68rem;
  line-height: 1.35;
  padding: 2px 6px;
}

.task-chip span {
  display: -webkit-box;
  font-size: 0.8rem;
  font-weight: 800;
  line-height: 1.35;
  min-width: 0;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
  word-break: break-all;
}

.task-chip.done {
  background: #f2f5f8;
  border-color: #d6e0eb;
  cursor: default;
  opacity: 0.82;
}

.task-chip.done strong {
  background: #71839a;
}

.task-table-wrap {
  border: 1px solid #e1ebf5;
  border-radius: 8px;
  overflow-x: auto;
  padding: 10px;
}

.task-table {
  border-collapse: separate;
  border-spacing: 0 8px;
  min-width: 760px;
  width: 100%;
}

.task-table th {
  color: #667b94;
  font-size: 0.78rem;
  padding: 0 10px 2px;
  text-transform: uppercase;
}

.task-table td {
  background: #f8fbfe;
  border-bottom: 1px solid #e8f0f8;
  border-top: 1px solid #e8f0f8;
  padding: 10px;
  vertical-align: top;
}

.task-table td:first-child {
  border-left: 1px solid #e8f0f8;
  border-radius: 8px 0 0 8px;
}

.task-table td:last-child {
  border-right: 1px solid #e8f0f8;
  border-radius: 0 8px 8px 0;
}

.content-input {
  min-width: 320px;
}

.duration-input {
  width: 92px;
}

.icon-only {
  align-items: center;
  display: inline-flex;
  height: 34px;
  justify-content: center;
  padding: 0;
  width: 34px;
}

.floating-save-button {
  bottom: 28px;
  border-radius: 999px;
  box-shadow: 0 18px 36px rgba(62, 141, 247, 0.32);
  font-weight: 900;
  min-width: 142px;
  padding: 11px 18px;
  position: fixed;
  right: 28px;
  z-index: 1050;
}

[data-bs-theme='dark'] .plan-workbench-page {
  background: #111827;
}

[data-bs-theme='dark'] .workbench-heading .page-kicker {
  color: #8dc5ff;
}

[data-bs-theme='dark'] .workbench-heading .page-title {
  color: #edf5ff;
  text-shadow: none;
}

[data-bs-theme='dark'] .workbench-heading .page-lead {
  color: #b8c9df;
}

[data-bs-theme='dark'] .heading-actions .btn,
[data-bs-theme='dark'] .calendar-tools .btn,
[data-bs-theme='dark'] .editor-tools .btn {
  background: rgba(237, 245, 255, 0.08);
  border-color: rgba(141, 197, 255, 0.22);
  color: #d8eaff;
}

[data-bs-theme='dark'] .heading-actions .btn:hover,
[data-bs-theme='dark'] .calendar-tools .btn:hover,
[data-bs-theme='dark'] .editor-tools .btn:hover {
  background: rgba(141, 197, 255, 0.16);
  border-color: rgba(141, 197, 255, 0.36);
  color: #ffffff;
}

[data-bs-theme='dark'] .workbench-stats > div,
[data-bs-theme='dark'] .calendar-card,
[data-bs-theme='dark'] .editor-card,
[data-bs-theme='dark'] .conflict-strip,
[data-bs-theme='dark'] .calendar-day,
[data-bs-theme='dark'] .task-table td {
  background-color: rgba(20, 32, 49, 0.94);
  border-color: rgba(188, 213, 241, 0.16);
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.24);
}

[data-bs-theme='dark'] .workbench-stats > div::before {
  background: linear-gradient(180deg, #8dc5ff, rgba(141, 197, 255, 0.12));
}

[data-bs-theme='dark'] .workbench-stats .warning::before {
  background: linear-gradient(180deg, #f0b45b, rgba(240, 180, 91, 0.12));
}

[data-bs-theme='dark'] .workbench-stats strong,
[data-bs-theme='dark'] .task-chip {
  color: #edf5ff;
}

[data-bs-theme='dark'] .workbench-stats .warning strong,
[data-bs-theme='dark'] .conflict-strip,
[data-bs-theme='dark'] .conflict-strip i {
  color: #ffd08a;
}

[data-bs-theme='dark'] .section-title p,
[data-bs-theme='dark'] .workbench-stats span,
[data-bs-theme='dark'] .calendar-day header {
  color: #b3c0d2;
}

[data-bs-theme='dark'] .section-title h2 {
  color: #f4f8ff;
}

[data-bs-theme='dark'] .conflict-strip {
  background-color: rgba(84, 57, 23, 0.5);
  border-color: rgba(240, 180, 91, 0.2);
}

[data-bs-theme='dark'] .calendar-day {
  background: rgba(12, 23, 38, 0.74);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

[data-bs-theme='dark'] .calendar-day:hover {
  border-color: rgba(141, 197, 255, 0.28);
  box-shadow: 0 14px 30px rgba(0, 0, 0, 0.18);
}

[data-bs-theme='dark'] .calendar-day.today {
  background: rgba(16, 30, 49, 0.92);
  border-color: rgba(141, 197, 255, 0.18);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

[data-bs-theme='dark'] .calendar-day.today::before {
  background: #8dc5ff;
}

[data-bs-theme='dark'] .calendar-day.conflict {
  background: rgba(54, 39, 22, 0.72);
  border-color: rgba(240, 180, 91, 0.22);
}

[data-bs-theme='dark'] .task-chip {
  background: rgba(141, 197, 255, 0.12);
  border-color: rgba(141, 197, 255, 0.22);
  box-shadow: none;
}

[data-bs-theme='dark'] .task-chip strong {
  background: rgba(141, 197, 255, 0.2);
  color: #dceeff;
}

[data-bs-theme='dark'] .task-chip.done {
  background: rgba(120, 137, 158, 0.16);
  border-color: rgba(188, 213, 241, 0.13);
}

[data-bs-theme='dark'] .task-chip.done strong {
  background: rgba(188, 213, 241, 0.18);
}

[data-bs-theme='dark'] .task-table-wrap {
  border-color: rgba(188, 213, 241, 0.14);
}

[data-bs-theme='dark'] .task-table th {
  color: #9fb0c6;
}

[data-bs-theme='dark'] .task-table td {
  border-color: rgba(188, 213, 241, 0.12);
}

[data-bs-theme='dark'] .day-tasks::-webkit-scrollbar-thumb {
  background: rgba(188, 213, 241, 0.24);
}

[data-bs-theme='dark'] .floating-save-button {
  box-shadow: 0 18px 38px rgba(56, 189, 248, 0.24);
}

@media (max-width: 1199.98px) {
  .workbench-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 991.98px) {
  .workbench-heading,
  .workbench-stats {
    grid-template-columns: 1fr;
  }

  .calendar-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 575.98px) {
  .calendar-grid {
    grid-template-columns: 1fr;
  }

  .section-title {
    display: grid;
  }

  .floating-save-button {
    bottom: 18px;
    right: 16px;
  }
}
</style>
