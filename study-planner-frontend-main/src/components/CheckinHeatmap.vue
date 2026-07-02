<template>
  <div class="card h-100">
    <div class="card-header d-flex justify-content-between align-items-center bg-white">
      <h6 class="mb-0"><i class="bi bi-calendar2-check"></i> {{ $t('dashboard.heatmapTitle') }}</h6>
      <div class="d-flex align-items-center gap-2">
        <button type="button" class="btn btn-sm btn-outline-secondary" @click="changeMonth(-1)">
          <i class="bi bi-chevron-left"></i>
        </button>
        <span class="month-label">{{ displayMonth }}</span>
        <button type="button" class="btn btn-sm btn-outline-secondary" :disabled="isCurrentMonth" @click="changeMonth(1)">
          <i class="bi bi-chevron-right"></i>
        </button>
      </div>
    </div>
    <div class="card-body">
      <div class="calendar-grid">
        <div class="weekday-header" v-for="label in weekdayLabels" :key="label">{{ label }}</div>
        <div
          v-for="day in calendarDays"
          :key="day.key"
          class="calendar-cell"
          :class="day.class"
          :title="day.tooltip"
        >
          <span v-if="day.date">{{ day.date }}</span>
        </div>
      </div>
      <div class="heatmap-legend">
        <small>{{ $t('dashboard.less') }}</small>
        <span class="legend-dot empty"></span>
        <span class="legend-dot level-1"></span>
        <span class="legend-dot level-2"></span>
        <span class="legend-dot level-3"></span>
        <span class="legend-dot level-4"></span>
        <small>{{ $t('dashboard.more') }}</small>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { t, locale } = useI18n()

const props = defineProps({
  calendarData: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['month-change'])

const now = new Date()
const currentYear = ref(now.getFullYear())
const currentMonth = ref(now.getMonth() + 1)

const isCurrentMonth = computed(() => {
  return currentYear.value === now.getFullYear() && currentMonth.value === now.getMonth() + 1
})

const displayMonth = computed(() => {
  return `${currentYear.value}-${String(currentMonth.value).padStart(2, '0')}`
})

const weekdayLabels = computed(() => {
  return locale.value === 'zh-CN'
    ? ['一', '二', '三', '四', '五', '六', '日']
    : ['Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa', 'Su']
})

const calendarDays = computed(() => {
  const year = currentYear.value
  const month = currentMonth.value
  const firstDay = new Date(year, month - 1, 1)
  const lastDay = new Date(year, month, 0)
  const daysInMonth = lastDay.getDate()
  let startWeekday = firstDay.getDay()
  startWeekday = startWeekday === 0 ? 6 : startWeekday - 1

  const cells = []

  for (let i = 0; i < startWeekday; i++) {
    cells.push({ key: `pad-${i}`, class: 'placeholder', date: null, tooltip: '' })
  }

  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    const hours = props.calendarData[dateStr] || 0
    const level = hours === 0 ? 'empty' : hours < 1 ? 'level-1' : hours < 2 ? 'level-2' : hours < 4 ? 'level-3' : 'level-4'
    const tooltip = hours > 0 ? `${dateStr}: ${hours}${t('dashboard.hours')}` : dateStr
    cells.push({ key: dateStr, class: level, date: d, tooltip })
  }

  return cells
})

function changeMonth(delta) {
  let m = currentMonth.value + delta
  let y = currentYear.value
  if (m > 12) { m = 1; y++ }
  if (m < 1) { m = 12; y-- }
  currentYear.value = y
  currentMonth.value = m
  emit('month-change', { year: y, month: m })
}
</script>

<style scoped>
.month-label {
  min-width: 80px;
  text-align: center;
  font-weight: 600;
  font-size: 0.9rem;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.weekday-header {
  text-align: center;
  font-size: 0.75rem;
  color: #888;
  padding-bottom: 4px;
  font-weight: 500;
}

.calendar-cell {
  aspect-ratio: 1;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 500;
  cursor: default;
  transition: transform 0.15s ease;
}

.calendar-cell:hover:not(.placeholder) {
  transform: scale(1.1);
}

.calendar-cell.placeholder {
  background: transparent;
}

.calendar-cell.empty {
  background: #ebedf0;
  color: #666;
}

.calendar-cell.level-1 {
  background: #9be9a8;
  color: #1a3a1a;
}

.calendar-cell.level-2 {
  background: #40c463;
  color: #fff;
}

.calendar-cell.level-3 {
  background: #30a14e;
  color: #fff;
}

.calendar-cell.level-4 {
  background: #216e39;
  color: #fff;
}

.heatmap-legend {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 12px;
  justify-content: flex-end;
}

.heatmap-legend small {
  color: #888;
  font-size: 0.75rem;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 3px;
  display: inline-block;
}

.legend-dot.empty {
  background: #ebedf0;
}

.legend-dot.level-1 {
  background: #9be9a8;
}

.legend-dot.level-2 {
  background: #40c463;
}

.legend-dot.level-3 {
  background: #30a14e;
}

.legend-dot.level-4 {
  background: #216e39;
}

:global([data-bs-theme='dark'] .dashboard-page .calendar-grid) {
  background: transparent;
}

:global([data-bs-theme='dark'] .dashboard-page .month-label),
:global([data-bs-theme='dark'] .dashboard-page .weekday-header),
:global([data-bs-theme='dark'] .dashboard-page .heatmap-legend small) {
  color: #aebdd1 !important;
}

:global([data-bs-theme='dark'] .dashboard-page .calendar-cell.empty),
:global([data-bs-theme='dark'] .dashboard-page .legend-dot.empty) {
  background: #182337 !important;
  color: #8fa0b7 !important;
  box-shadow: inset 0 0 0 1px rgba(188, 213, 241, 0.08);
}

:global([data-bs-theme='dark'] .dashboard-page .calendar-cell.level-1),
:global([data-bs-theme='dark'] .dashboard-page .legend-dot.level-1) {
  background: #214a3d !important;
  color: #d9ffe9 !important;
}

:global([data-bs-theme='dark'] .dashboard-page .calendar-cell.level-2),
:global([data-bs-theme='dark'] .dashboard-page .legend-dot.level-2) {
  background: #2f7b5a !important;
  color: #f0fff7 !important;
}

:global([data-bs-theme='dark'] .dashboard-page .calendar-cell.level-3),
:global([data-bs-theme='dark'] .dashboard-page .legend-dot.level-3) {
  background: #54ad7f !important;
  color: #071b11 !important;
}

:global([data-bs-theme='dark'] .dashboard-page .calendar-cell.level-4),
:global([data-bs-theme='dark'] .dashboard-page .legend-dot.level-4) {
  background: #8be0ad !important;
  color: #071b11 !important;
}
</style>
