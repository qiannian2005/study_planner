<template>
  <div class="create-plan-page app-shell">
    <Navbar />

    <main class="container page-surface my-5">
      <div class="create-heading">
        <p class="page-kicker">{{ t('createPlan.kicker') }}</p>
        <h1 class="page-title">{{ t('createPlan.title') }}</h1>
        <p class="page-lead">
          {{ t('createPlan.lead') }}
        </p>
      </div>

      <div class="planner-grid">
        <section class="planner-main">
          <form @submit.prevent="handleSubmit">
            <div class="card cp-card">
              <div class="panel-title">
                <span class="step-badge">1</span>
                <div>
                  <h2>{{ t('createPlan.basicInfo') }}</h2>
                  <p>{{ t('createPlan.basicInfoDesc') }}</p>
                </div>
              </div>

              <div class="form-block">
                <label for="goal">{{ t('createPlan.goal') }} <span>*</span></label>
                <div class="goal-input-wrap" :class="{ 'has-ghost': hasGhostSuggestion }">
                  <div ref="goalMirror" class="goal-mirror" aria-hidden="true">
                    <span class="goal-mirror-text">{{ goalSuggestion.base }}</span>
                    <span v-if="hasGhostSuggestion" ref="goalGhost" class="goal-ghost">{{ goalSuggestion.suffix }}</span>
                  </div>
                  <textarea
                    id="goal"
                    ref="goalTextarea"
                    v-model="formData.goal"
                    class="form-control goal-textarea"
                    :class="{ 'is-ghosting': hasGhostSuggestion }"
                    required
                    maxlength="500"
                    rows="4"
                    :placeholder="t('createPlan.goalPlaceholder')"
                    @scroll="syncMirrorScroll"
                    @compositionstart="onGoalCompositionStart"
                    @compositionend="onGoalCompositionEnd"
                    @keydown.tab="onGoalTabKey"
                    @click="onGoalGhostClick"
                  ></textarea>
                </div>
                <div class="input-foot">
                  <span class="goal-auto-tip">
                    <span v-if="goalSuggestionAccepted">{{ t('createPlan.goalSuggestionAccepted') }}</span>
                    <span v-else-if="goalOptimizing">{{ t('createPlan.goalOptimizing') }}</span>
                    <span v-else-if="goalSuggestion.suffix">{{ t('createPlan.acceptGoalSuggestion') }}</span>
                    <span v-else>{{ t('createPlan.goalAutoTip') }}</span>
                  </span>
                  <span>{{ formData.goal.length }}/500</span>
                </div>
              </div>

              <div class="form-block">
                <label for="level">{{ t('createPlan.level') }} <span>*</span></label>
                <select id="level" v-model="formData.level" class="form-select" required>
                  <option disabled value="">{{ t('createPlan.selectLevel') }}</option>
                  <option value="零基础">{{ t('createPlan.levelZero') }}</option>
                  <option value="初级">{{ t('createPlan.levelBeginner') }}</option>
                  <option value="中级">{{ t('createPlan.levelIntermediate') }}</option>
                  <option value="高级">{{ t('createPlan.levelAdvanced') }}</option>
                </select>
              </div>

              <div class="form-block">
                <label>{{ t('createPlan.studyMode') }} <span>*</span></label>
                <div class="segmented-control mode-segmented">
                  <button
                    v-for="mode in studyModes"
                    :key="mode.value"
                    type="button"
                    class="segmented-option"
                    :class="{ active: formData.studyMode === mode.value }"
                    @click="selectStudyMode(mode)"
                  >
                    <i :class="mode.icon"></i>
                    <span>
                      <strong>{{ mode.label }}</strong>
                      <small>{{ mode.desc }}</small>
                    </span>
                  </button>
                </div>
              </div>

              <div class="form-block">
                <label>{{ t('createPlan.studyPreference') }} <span>*</span></label>
                <div class="preference-grid">
                  <button
                    v-for="preference in studyPreferences"
                    :key="preference.value"
                    type="button"
                    class="preference-card"
                    :class="{ active: formData.studyPreference === preference.value }"
                    @click="selectStudyPreference(preference)"
                  >
                    <i :class="preference.icon"></i>
                    <span>
                      <strong>{{ preference.label }}</strong>
                      <small>{{ preference.desc }}</small>
                    </span>
                  </button>
                </div>
              </div>

              <div class="field-row">
                <div class="form-block">
                  <label for="dailyHours">{{ t('createPlan.dailyHours') }} <span>*</span></label>
                  <div class="unit-input">
                    <input
                      id="dailyHours"
                      v-model.number="formData.dailyHours"
                      class="form-control"
                      type="number"
                      :min="MIN_DAILY_HOURS"
                      :max="MAX_DAILY_HOURS"
                      step="0.5"
                      @input="limitDailyHoursInput"
                      required
                    />
                    <span>{{ t('createPlan.hours') }}</span>
                  </div>
                </div>

                <div class="form-block">
                  <label for="totalDays">{{ t('createPlan.totalDays') }} <span>*</span></label>
                  <div class="unit-input">
                    <input
                      id="totalDays"
                      v-model.number="formData.totalDays"
                      class="form-control"
                      type="number"
                      min="1"
                      :max="MAX_TOTAL_DAYS"
                      @input="limitTotalDaysInput"
                      required
                    />
                    <span>{{ t('createPlan.days') }}</span>
                  </div>
                </div>

                <div class="form-block">
                  <label for="expectedEndDate">{{ t('createPlan.expectedEndDate') }}</label>
                  <input id="expectedEndDate" v-model="formData.expectedEndDate" class="form-control" type="date" readonly />
                </div>
              </div>

              <div class="field-row two">
                <div class="form-block">
                  <label for="title">{{ t('createPlan.planTitle') }}</label>
                  <input id="title" v-model.trim="formData.title" class="form-control" type="text" :placeholder="t('createPlan.planTitlePlaceholder')" />
                </div>

                <div class="form-block">
                  <label for="notes">{{ t('createPlan.notes') }}</label>
                  <input id="notes" v-model.trim="formData.notes" class="form-control" type="text" :placeholder="t('createPlan.notesPlaceholder')" />
                </div>
              </div>
            </div>

            <div class="card cp-card generate-panel">
              <div class="panel-title compact">
                <span class="step-badge blue">2</span>
                <div>
                  <h2>{{ t('createPlan.generateTitle') }}</h2>
                  <p>{{ t('createPlan.generateDesc') }}</p>
                </div>
              </div>
              <button type="submit" class="btn btn-primary btn-lg generate-button" :disabled="loading">
                <span v-if="loading" class="spinner-border spinner-border-sm"></span>
                <i v-else class="bi bi-sparkles"></i>
                {{ loading ? t('createPlan.generating') : t('createPlan.generateMyPlan') }}
              </button>
            </div>
          </form>

          <div v-if="showResult" class="card result-panel">
            <i class="bi bi-check-circle-fill"></i>
            <div>
              <strong>{{ resultPlan.title || resultPlan.goal || formData.title || t('createPlan.planCreated') }}</strong>
              <p>{{ t('createPlan.resultDesc') }}</p>
            </div>
            <router-link to="/my-plans">{{ t('createPlan.viewMyPlans') }}</router-link>
          </div>
        </section>

        <aside class="planner-aside">
          <section class="card cp-side-card">
            <div class="side-title">
              <i class="bi bi-cpu"></i>
              <h3>{{ t('createPlan.analysisTitle') }}</h3>
            </div>
            <div v-if="analysisLoading" class="side-state">
              <span class="spinner-border spinner-border-sm"></span>
              {{ t('createPlan.analysisLoading') }}
            </div>
            <div v-else-if="hasAnalysis" class="analysis-box">
              <div v-for="item in analysisCards" :key="item.key" class="analysis-row" :class="`tone-${item.tone}`">
                <span class="analysis-label"><i :class="item.icon"></i>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
                <small>{{ item.hint }}</small>
              </div>
            </div>
            <p v-if="analysis.tip" class="analysis-tip">{{ analysis.tip }}</p>
            <p v-else class="empty-hint">{{ t('createPlan.analysisEmpty') }}</p>
          </section>

          <section class="card cp-side-card">
            <div class="side-title">
              <i class="bi bi-diagram-3"></i>
              <h3>{{ t('createPlan.roadmapTitle') }}</h3>
            </div>
            <div class="roadmap-context">
              <span v-if="activeStudyMode"><i :class="activeStudyMode.icon"></i>{{ activeStudyMode.label }}</span>
              <span v-if="activeStudyPreference"><i :class="activeStudyPreference.icon"></i>{{ activeStudyPreference.label }}</span>
            </div>
            <div v-if="roadmapLoading" class="side-state">
              <span class="spinner-border spinner-border-sm"></span>
              {{ t('createPlan.roadmapLoading') }}
            </div>
            <div v-else-if="roadmap.length" class="timeline">
              <div v-for="(item, index) in roadmap" :key="`${item.title || 'step'}-${index}`" class="timeline-item">
                <span>{{ formatRoadmapLabel(item, index) }}</span>
                <div>
                  <strong>{{ item.title || t('createPlan.stage', { index: index + 1 }) }}</strong>
                  <p>{{ item.desc || item.description || t('createPlan.stageFallback') }}</p>
                </div>
              </div>
            </div>
            <p v-else class="empty-hint">{{ t('createPlan.roadmapEmpty') }}</p>
          </section>
        </aside>
      </div>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import Navbar from '../components/Navbar.vue'
import Footer from '../components/Footer.vue'
import { planApi } from '../api/plan'
import { showToast } from '../utils/toast'

const { t, locale } = useI18n()
const MIN_GOAL_OPTIMIZE_LENGTH = 6
const GOAL_OPTIMIZE_DELAY = 700
const GOAL_ACCEPTED_TIP_DURATION = 800
const MIN_DAILY_HOURS = 0.5
const MAX_DAILY_HOURS = 4
const MAX_TOTAL_DAYS = 30
const TRAILING_GOAL_PUNCTUATION = /[\s,.;:!?\u3001\uff0c\u3002\uff01\uff1f\uff1b\uff1a]+$/
const LEADING_GOAL_PUNCTUATION = /^[\s,.;:!?\u3001\uff0c\u3002\uff01\uff1f\uff1b\uff1a]+/
const GOAL_ENDS_WITH_PUNCTUATION = /[,.;:!?\u3001\uff0c\u3002\uff01\uff1f\uff1b\uff1a]$/

const studyModes = computed(() => [
  { value: 'relaxed', label: t('createPlan.modes.relaxed'), desc: t('createPlan.modes.relaxedDesc'), hours: 1, icon: 'bi bi-feather' },
  { value: 'standard', label: t('createPlan.modes.standard'), desc: t('createPlan.modes.standardDesc'), hours: 2, icon: 'bi bi-bookmark-check' },
  { value: 'sprint', label: t('createPlan.modes.sprint'), desc: t('createPlan.modes.sprintDesc'), hours: 4, icon: 'bi bi-lightning-charge' }
])

const studyPreferences = computed(() => [
  { value: 'theory', label: t('createPlan.preferences.theory'), desc: t('createPlan.preferences.theoryDesc'), icon: 'bi bi-journal-text' },
  { value: 'practice', label: t('createPlan.preferences.practice'), desc: t('createPlan.preferences.practiceDesc'), icon: 'bi bi-tools' },
  { value: 'exam', label: t('createPlan.preferences.exam'), desc: t('createPlan.preferences.examDesc'), icon: 'bi bi-patch-check' },
  { value: 'project', label: t('createPlan.preferences.project'), desc: t('createPlan.preferences.projectDesc'), icon: 'bi bi-kanban' }
])

const formData = reactive({
  goal: '',
  level: '',
  dailyHours: 2,
  totalDays: null,
  expectedEndDate: '',
  title: '',
  notes: '',
  studyMode: '',
  studyPreference: ''
})

const loading = ref(false)
const showResult = ref(false)
const resultPlan = ref({})
const analysisLoading = ref(false)
const roadmapLoading = ref(false)
const goalOptimizing = ref(false)
const analysis = reactive({})
const roadmap = ref([])
const goalSuggestion = reactive({ base: '', suffix: '', full: '' })
const goalSuggestionAccepted = ref(false)
const goalTextarea = ref(null)
const goalMirror = ref(null)
const goalGhost = ref(null)
const goalComposing = ref(false)

let analyzeTimer = null
let roadmapTimer = null
let optimizeTimer = null
let acceptedTipTimer = null
let analyzeRequestId = 0
let roadmapRequestId = 0
let optimizeRequestId = 0
let lastAcceptedGoal = ''

const hasAnalysis = computed(() => Object.keys(analysis).length > 0)
const hasGhostSuggestion = computed(() => !!goalSuggestion.suffix && goalSuggestion.base === formData.goal.trimEnd())

const analysisItems = computed(() => [
  { label: t('createPlan.analysis.keySkills'), value: Array.isArray(analysis.keySkills) ? analysis.keySkills.join('、') : '-', icon: 'bi bi-lightbulb' },
  { label: t('createPlan.analysis.recommendedMode'), value: analysis.recommendedMode ? t(`createPlan.modes.${analysis.recommendedMode}`) : '-', icon: 'bi bi-activity' },
  { label: t('createPlan.analysis.completionRate'), value: analysis.completionRate != null ? `${analysis.completionRate}%` : '-', icon: 'bi bi-graph-up-arrow' }
])

const clarityLabelMap = {
  clear: '清晰',
  focused: '较清晰',
  broad: '偏宽泛',
  vague: '需补充'
}

const difficultyLabelMap = {
  easy: '入门',
  medium: '中等',
  hard: '偏难'
}

const analysisCards = computed(() => {
  const clarity = analysis.clarity || inferGoalClarity(formData.goal)
  const difficulty = analysis.difficulty || 'medium'
  return [
    {
      key: 'skills',
      label: '核心技能',
      value: formatKeySkills(analysis.keySkills),
      hint: '本计划优先覆盖的知识点',
      icon: 'bi bi-lightbulb',
      tone: 'blue'
    },
    {
      key: 'clarity',
      label: '目标清晰度',
      value: formatAnalysisValue(clarity, clarityLabelMap, 'broad'),
      hint: getClarityHint(clarity),
      icon: 'bi bi-bullseye',
      tone: clarity === 'vague' || clarity === 'broad' ? 'amber' : 'green'
    },
    {
      key: 'difficulty',
      label: '难度等级',
      value: formatAnalysisValue(difficulty, difficultyLabelMap, 'medium'),
      hint: getDifficultyHint(difficulty),
      icon: 'bi bi-speedometer2',
      tone: difficulty === 'hard' ? 'red' : 'violet'
    },
    {
      key: 'pace',
      label: '推荐节奏',
      value: analysis.recommendedMode ? t(`createPlan.modes.${analysis.recommendedMode}`) : '-',
      hint: getPaceHint(analysis.recommendedMode),
      icon: 'bi bi-activity',
      tone: 'cyan'
    }
  ]
})

const activeStudyMode = computed(() => studyModes.value.find((mode) => mode.value === formData.studyMode) || null)
const activeStudyPreference = computed(() => studyPreferences.value.find((preference) => preference.value === formData.studyPreference) || null)

watch(
  () => formData.goal,
  () => {
    Object.keys(analysis).forEach((key) => delete analysis[key])
    if (!formData.goal.trim()) {
      clearAcceptedGoalState()
    } else if (goalSuggestionAccepted.value && formData.goal !== lastAcceptedGoal) {
      clearAcceptedGoalState()
    }
    if (goalSuggestion.base && formData.goal.trimEnd() !== goalSuggestion.base) {
      clearGoalSuggestion()
    }
    scheduleGoalAnalysis()
    scheduleGoalOptimization()
    scheduleRoadmapGeneration()
  }
)

watch(
  () => [formData.totalDays, formData.level, formData.dailyHours],
  () => {
    updateExpectedEndDate()
    scheduleGoalOptimization()
    scheduleRoadmapGeneration()
  }
)

watch(
  () => formData.studyPreference,
  () => {
    scheduleRoadmapGeneration()
  }
)

watch(hasGhostSuggestion, () => nextTick(syncMirrorScroll))

function formatKeySkills(skills) {
  if (!Array.isArray(skills) || !skills.length) return '-'
  return skills.slice(0, 4).join('、')
}

function formatAnalysisValue(value, labelMap, fallback) {
  return labelMap[value] || labelMap[fallback] || '-'
}

function inferGoalClarity(goal) {
  const text = (goal || '').trim()
  if (text.length < 8) return 'vague'
  if (text.length < 18) return 'broad'
  const hasOutcome = /项目|系统|应用|Demo|报告|考试|证书|面试|算法|接口|模型|数据|可视化|部署|实战/i.test(text)
  return hasOutcome ? 'clear' : 'focused'
}

function getClarityHint(clarity) {
  const hints = {
    clear: '方向和产出都比较明确',
    focused: '方向明确，可继续补充成果',
    broad: '建议补充具体产出',
    vague: '目标还需要再具体一点'
  }
  return hints[clarity] || hints.broad
}

function getDifficultyHint(difficulty) {
  const hints = {
    easy: '适合从基础快速起步',
    medium: '需要稳定练习和复盘',
    hard: '建议拆阶段推进'
  }
  return hints[difficulty] || hints.medium
}

function getPaceHint(mode) {
  const hints = {
    relaxed: '每天约 1 小时，压力较低',
    standard: '每天约 2 小时，节奏均衡',
    sprint: '每天约 4 小时，强度较高'
  }
  return hints[mode] || '根据目标自动推荐'
}

function selectStudyMode(mode) {
  formData.studyMode = mode.value
  formData.dailyHours = mode.hours
}

function selectStudyPreference(preference) {
  formData.studyPreference = preference.value
}

function limitDailyHoursInput(event) {
  const value = Number(event.target.value)
  if (!Number.isFinite(value)) return
  if (value < MIN_DAILY_HOURS) {
    formData.dailyHours = MIN_DAILY_HOURS
    event.target.value = MIN_DAILY_HOURS
  } else if (value > MAX_DAILY_HOURS) {
    formData.dailyHours = MAX_DAILY_HOURS
    event.target.value = MAX_DAILY_HOURS
  }
}

function limitTotalDaysInput(event) {
  const value = Number(event.target.value)
  if (!Number.isFinite(value) || value <= MAX_TOTAL_DAYS) return
  formData.totalDays = MAX_TOTAL_DAYS
  event.target.value = MAX_TOTAL_DAYS
}

function clearGoalSuggestion() {
  goalSuggestion.base = ''
  goalSuggestion.suffix = ''
  goalSuggestion.full = ''
}

function clearAcceptedGoalState() {
  clearTimeout(acceptedTipTimer)
  acceptedTipTimer = null
  goalSuggestionAccepted.value = false
  lastAcceptedGoal = ''
}

function scheduleAcceptedGoalRelease(acceptedGoal) {
  clearTimeout(acceptedTipTimer)
  acceptedTipTimer = setTimeout(() => {
    acceptedTipTimer = null
    if (formData.goal !== acceptedGoal) return
    goalSuggestionAccepted.value = false
    lastAcceptedGoal = ''
    scheduleGoalOptimization()
  }, GOAL_ACCEPTED_TIP_DURATION)
}

function buildGoalSuggestion(originalGoal, optimizedGoal) {
  const base = (originalGoal || '').trimEnd()
  const optimized = (optimizedGoal || '').trim()

  if (!base || !optimized || optimized === base) {
    return { base, suffix: '', full: base }
  }

  const punct = /[，,。.!！?？；;、:\s]+$/
  const leadPunct = /^[，,。.!！?？；;、:\s]+/
  const baseCore = base.replace(punct, '')
  const optimizedCore = optimized.replace(punct, '')
  const baseEndsWithPunct = /[，,。.!！?？；;、:]$/.test(base)

  let full
  let suffix

  if (optimized.startsWith(base)) {
    full = optimized
    suffix = optimized.slice(base.length)
  } else if (optimized.startsWith(baseCore)) {
    suffix = optimized.slice(baseCore.length)
    if (baseEndsWithPunct) {
      suffix = suffix.replace(leadPunct, '').replace(/^\s+/, '')
      if (suffix) {
        suffix = ` ${suffix}`
      }
    }
    full = base + suffix
  } else if (base.startsWith(optimized) || baseCore.startsWith(optimizedCore)) {
    return { base, suffix: '', full: base }
  } else {
    const cleaned = optimized.replace(leadPunct, '')
    suffix = `${baseEndsWithPunct ? ' ' : '，'}${cleaned}`
    full = base + suffix
  }

  full = full.slice(0, 500)
  if (!suffix) {
    suffix = full.slice(base.length)
  }

  if (!suffix || full === base) {
    return { base, suffix: '', full: base }
  }

  return {
    base,
    suffix: suffix.slice(0, Math.max(0, 500 - base.length)),
    full
  }
}

function buildFastGoalSuggestion(originalGoal, optimizedGoal) {
  const base = (originalGoal || '').trimEnd()
  const optimized = (optimizedGoal || '').trim()

  if (!base || !optimized || optimized === base) {
    return { base, suffix: '', full: base }
  }

  const baseCore = getOptimizableGoal(base)
  const optimizedCore = getOptimizableGoal(optimized)
  const baseEndsWithPunct = GOAL_ENDS_WITH_PUNCTUATION.test(base)
  let full = ''
  let suffix = ''

  if (optimized.startsWith(base)) {
    full = optimized
    suffix = optimized.slice(base.length)
  } else if (baseCore && optimized.startsWith(baseCore)) {
    suffix = optimized.slice(baseCore.length)
    if (baseEndsWithPunct) {
      suffix = suffix.replace(LEADING_GOAL_PUNCTUATION, '')
      suffix = suffix ? ` ${suffix}` : ''
    }
    full = base + suffix
  } else if (optimizedCore && (base.startsWith(optimized) || baseCore.startsWith(optimizedCore))) {
    return { base, suffix: '', full: base }
  } else {
    const cleaned = optimized.replace(LEADING_GOAL_PUNCTUATION, '')
    suffix = `${baseEndsWithPunct ? ' ' : ', '}${cleaned}`
    full = base + suffix
  }

  full = full.slice(0, 500)
  if (!suffix) {
    suffix = full.slice(base.length)
  }

  if (!suffix || full === base) {
    return { base, suffix: '', full: base }
  }

  return {
    base,
    suffix: suffix.slice(0, Math.max(0, 500 - base.length)),
    full
  }
}

function acceptGoalSuggestion() {
  if (!goalSuggestion.full || goalSuggestionAccepted.value) return
  const accepted = goalSuggestion.full.slice(0, 500)
  goalSuggestionAccepted.value = true
  lastAcceptedGoal = accepted
  scheduleAcceptedGoalRelease(accepted)
  clearTimeout(optimizeTimer)
  optimizeRequestId += 1
  goalOptimizing.value = false
  formData.goal = accepted
  clearGoalSuggestion()
  nextTick(() => {
    const el = goalTextarea.value
    if (!el) return
    el.focus()
    const end = accepted.length
    el.setSelectionRange(end, end)
    syncMirrorScroll()
  })
}

function syncMirrorScroll() {
  const el = goalTextarea.value
  const mirror = goalMirror.value
  if (!el || !mirror) return
  mirror.scrollTop = el.scrollTop
  mirror.scrollLeft = el.scrollLeft
}

function onGoalTabKey(event) {
  if (!hasGhostSuggestion.value) return
  event.preventDefault()
  acceptGoalSuggestion()
}

function onGoalGhostClick(event) {
  if (!hasGhostSuggestion.value) return
  const ghost = goalGhost.value
  if (!ghost) return
  const rects = ghost.getClientRects()
  const x = event.clientX
  const y = event.clientY
  for (const rect of rects) {
    if (rect.width === 0 && rect.height === 0) continue
    if (x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom) {
      acceptGoalSuggestion()
      return
    }
  }
}

function onGoalCompositionStart() {
  goalComposing.value = true
  if (goalSuggestionAccepted.value) {
    clearAcceptedGoalState()
  }
  clearTimeout(optimizeTimer)
}

function onGoalCompositionEnd() {
  goalComposing.value = false
  scheduleGoalOptimization()
}

function scheduleGoalOptimization() {
  clearTimeout(optimizeTimer)
  if (goalSuggestionAccepted.value) {
    clearGoalSuggestion()
    goalOptimizing.value = false
    return
  }
  if (goalComposing.value || !isGoalReadyForFastOptimization(formData.goal)) {
    clearGoalSuggestion()
    return
  }
  optimizeTimer = setTimeout(fetchGoalOptimization, GOAL_OPTIMIZE_DELAY)
}

async function fetchGoalOptimization() {
  if (goalSuggestionAccepted.value) {
    clearGoalSuggestion()
    return
  }
  const baseGoal = formData.goal.trimEnd()
  if (goalComposing.value || !isGoalReadyForFastOptimization(baseGoal)) {
    clearGoalSuggestion()
    return
  }

  const requestId = ++optimizeRequestId
  goalOptimizing.value = true
  try {
    const result = await planApi.optimizeGoal({
      goal: baseGoal,
      level: formData.level,
      totalDays: formData.totalDays,
      dailyHours: formData.dailyHours,
      studyMode: formData.studyMode,
      studyPreference: formData.studyPreference
    })

    if (goalSuggestionAccepted.value || requestId !== optimizeRequestId || formData.goal.trimEnd() !== baseGoal) {
      return
    }

    if (result?.code === 200 && result.data?.goal) {
      const suggestion = buildFastGoalSuggestion(baseGoal, result.data.goal)
      if (!suggestion.suffix) {
        clearGoalSuggestion()
        return
      }
      goalSuggestion.base = suggestion.base
      goalSuggestion.suffix = suggestion.suffix
      goalSuggestion.full = suggestion.full
    } else {
      clearGoalSuggestion()
    }
  } catch (error) {
    console.error('AI 优化目标失败:', error)
    clearGoalSuggestion()
  } finally {
    if (requestId === optimizeRequestId) {
      goalOptimizing.value = false
    }
  }
}

function isGoalReadyForOptimization(goal) {
  const text = (goal || '').trim()
  if (text.length < MIN_GOAL_OPTIMIZE_LENGTH || text.length >= 500) {
    return false
  }
  if (/[,，、;；:：]$/.test(text)) {
    return false
  }
  return true
}

function isGoalReadyForFastOptimization(goal) {
  const text = getOptimizableGoal(goal)
  return text.length >= MIN_GOAL_OPTIMIZE_LENGTH && text.length < 500
}

function getOptimizableGoal(goal) {
  return (goal || '').trim().replace(TRAILING_GOAL_PUNCTUATION, '')
}

function updateExpectedEndDate() {
  if (!formData.totalDays) {
    formData.expectedEndDate = ''
    return
  }
  const date = new Date()
  date.setDate(date.getDate() + Number(formData.totalDays) - 1)
  formData.expectedEndDate = date.toISOString().slice(0, 10)
}

function scheduleGoalAnalysis() {
  clearTimeout(analyzeTimer)
  if (!formData.goal || formData.goal.length < 3) return
  analyzeTimer = setTimeout(fetchGoalAnalysis, 600)
}

function scheduleRoadmapGeneration() {
  clearTimeout(roadmapTimer)
  if (!formData.goal || !formData.totalDays) return
  if (Number(formData.dailyHours) < MIN_DAILY_HOURS || Number(formData.dailyHours) > MAX_DAILY_HOURS) return
  if (Number(formData.totalDays) < 1 || Number(formData.totalDays) > MAX_TOTAL_DAYS) return
  roadmapTimer = setTimeout(fetchRoadmap, 600)
}

function formatRoadmapLabel(item, index) {
  if (item?.label) return item.label
  if (item?.dayStart && item?.dayEnd) {
    return item.dayStart === item.dayEnd
      ? t('createPlan.dayValue', { day: item.dayStart })
      : t('createPlan.dayRangeValue', { start: item.dayStart, end: item.dayEnd })
  }
  if (item?.week) return t('createPlan.weekValue', { week: item.week })
  return t('createPlan.stage', { index: index + 1 })
}

async function fetchGoalAnalysis() {
  const requestId = ++analyzeRequestId
  analysisLoading.value = true
  try {
    const result = await planApi.analyzeGoal({ goal: formData.goal, level: formData.level })
    if (requestId !== analyzeRequestId) return
    if (result?.code === 200) {
      Object.keys(analysis).forEach((key) => delete analysis[key])
      Object.assign(analysis, result.data || {})
    } else {
      showToast(result?.message || t('createPlan.analysisFailed'), 'error')
    }
  } catch (error) {
    console.error('AI 目标分析失败:', error)
    showToast(t('createPlan.analysisServiceFailed'), 'error')
  } finally {
    if (requestId === analyzeRequestId) analysisLoading.value = false
  }
}

async function fetchRoadmap() {
  const requestId = ++roadmapRequestId
  roadmapLoading.value = true
  try {
    const result = await planApi.generateRoadmap({
      goal: formData.goal,
      level: formData.level,
      dailyHours: formData.dailyHours,
      totalDays: formData.totalDays,
      studyMode: formData.studyMode,
      studyPreference: formData.studyPreference,
      goalAnalysis: { ...analysis }
    })
    if (requestId !== roadmapRequestId) return
    if (result?.code === 200) {
      roadmap.value = result.data || []
    } else {
      showToast(result?.message || t('createPlan.roadmapFailed'), 'error')
    }
  } catch (error) {
    console.error('学习路线生成失败:', error)
    showToast(t('createPlan.roadmapServiceFailed'), 'error')
  } finally {
    if (requestId === roadmapRequestId) roadmapLoading.value = false
  }
}

async function handleSubmit() {
  if (formData.dailyHours === null || formData.dailyHours === '' || Number(formData.dailyHours) < MIN_DAILY_HOURS || Number(formData.dailyHours) > MAX_DAILY_HOURS) {
    showToast('每日学习时间只能在0.5-4小时之间', 'warning')
    return
  }

  if (!formData.totalDays || Number(formData.totalDays) < 1 || Number(formData.totalDays) > MAX_TOTAL_DAYS) {
    showToast('计划天数不能超过30天', 'warning')
    return
  }

  loading.value = true
  showResult.value = false

  try {
    const requestData = {
      ...formData,
      language: locale.value,
      timeSlots: [],
      goalAnalysis: { ...analysis },
      roadmap: roadmap.value.map((item, index) => `${formatRoadmapLabel(item, index)} ${item.title || ''}`.trim())
    }
    const result = await planApi.generatePlan(requestData)

    if (result?.code === 200) {
      showToast(t('createPlan.planGeneratedSuccess'), 'success')
      resultPlan.value = result.data || {}
      showResult.value = true
    } else {
      showToast(result?.message || t('createPlan.generateFailed'), 'error')
    }
  } catch (error) {
    console.error('生成计划失败:', error)
    showToast(t('createPlan.networkError'), 'error')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.create-plan-page {
  --sp-bg: #f3f7fb;
  --sp-card: rgba(255, 255, 255, 0.94);
  --sp-ink: #1d3148;
  --sp-muted: #6a7f96;
  --sp-line: rgba(87, 116, 148, 0.16);
  --sp-blue: #2f6fc5;
  min-height: 100vh;
  background: #f6f8fb;
}

.page-surface {
  position: relative;
}

.create-heading {
  margin-bottom: 28px;
}

.page-kicker {
  color: var(--sp-blue);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  margin-bottom: 8px;
  text-transform: uppercase;
}

.page-title {
  color: var(--sp-ink);
  font-size: clamp(30px, 4vw, 42px);
  font-weight: 800;
  letter-spacing: -0.03em;
  margin: 0 0 10px;
}

.page-lead {
  color: var(--sp-muted);
  font-size: 15px;
  line-height: 1.8;
  margin: 0;
  max-width: 760px;
}

.planner-grid {
  align-items: start;
  display: grid;
  gap: 22px;
  grid-template-columns: minmax(0, 1.45fr) minmax(300px, 0.95fr);
}

.planner-main,
.planner-aside {
  display: grid;
  gap: 18px;
}

.planner-aside {
  align-self: start;
}

.cp-card,
.cp-side-card,
.result-panel {
  background: var(--sp-card);
  border: 1px solid var(--sp-line);
  border-radius: 24px;
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.85) inset,
    0 18px 48px rgba(31, 50, 73, 0.08);
  backdrop-filter: blur(10px);
}

.cp-card {
  padding: 24px;
}

.cp-side-card {
  padding: 18px;
}

.result-panel {
  align-items: center;
  display: grid;
  gap: 16px;
  grid-template-columns: auto 1fr auto;
  padding: 18px 22px;
}

.result-panel i {
  color: #2ca56f;
  font-size: 28px;
}

.result-panel strong,
.panel-title h2,
.side-title h3 {
  color: var(--sp-ink);
}

.result-panel p,
.panel-title p,
.empty-hint,
.side-state,
.analysis-row,
.analysis-tip,
.timeline-item p,
.input-foot {
  color: var(--sp-muted);
}

.result-panel a {
  color: var(--sp-blue);
  font-weight: 700;
  text-decoration: none;
}

.panel-title,
.side-title {
  align-items: center;
  display: flex;
  gap: 12px;
}

.panel-title {
  margin-bottom: 20px;
}

.side-title {
  margin-bottom: 12px;
}

.compact {
  margin-bottom: 14px;
}

.step-badge,
.side-title i {
  align-items: center;
  background: rgba(62, 141, 247, 0.16);
  border: 1px solid rgba(62, 141, 247, 0.26);
  border-radius: 50%;
  color: #2f6fc5;
  display: inline-flex;
  flex: 0 0 auto;
  font-weight: 800;
  height: 30px;
  justify-content: center;
  width: 30px;
}

.step-badge.blue {
  background: rgba(23, 126, 98, 0.12);
  border-color: rgba(23, 126, 98, 0.24);
  color: #177e62;
}

.form-block {
  margin-bottom: 20px;
}

.form-block label {
  color: var(--sp-ink);
  display: block;
  font-weight: 700;
  margin-bottom: 10px;
}

.form-block label span {
  color: var(--sp-blue);
}

.preference-grid,
.field-row {
  display: grid;
  gap: 12px;
}

.preference-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.field-row {
  grid-template-columns: repeat(3, 1fr);
}

.field-row.two {
  grid-template-columns: repeat(2, 1fr);
}

.segmented-control {
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(80, 116, 152, 0.16);
  border-radius: 14px;
  display: grid;
  gap: 4px;
  padding: 4px;
}

.mode-segmented {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.segmented-option,
.preference-card {
  border: 1px solid transparent;
  color: var(--sp-ink);
  transition: 180ms ease;
  white-space: normal;
}

.segmented-option {
  align-items: center;
  background: transparent;
  border-radius: 10px;
  display: flex;
  gap: 10px;
  justify-content: center;
  min-height: 54px;
  padding: 8px 10px;
  text-align: left;
}

.preference-card {
  align-items: center;
  background: rgba(255, 255, 255, 0.7);
  border-color: rgba(80, 116, 152, 0.16);
  border-radius: 12px;
  display: flex;
  gap: 12px;
  min-height: 82px;
  padding: 12px 14px;
  text-align: left;
  transition: 180ms ease;
}

.segmented-option i,
.preference-card i {
  color: var(--sp-blue);
  font-size: 20px;
}

.preference-card i {
  align-items: center;
  background: rgba(62, 141, 247, 0.1);
  border-radius: 10px;
  display: inline-flex;
  flex: 0 0 34px;
  height: 34px;
  justify-content: center;
  width: 34px;
}

.segmented-option span,
.preference-card span {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.segmented-option strong,
.preference-card strong {
  line-height: 1.3;
}

.segmented-option small,
.preference-card small {
  color: var(--sp-muted);
  font-size: 12px;
  line-height: 1.35;
}

.segmented-option:hover,
.preference-card:hover {
  border-color: rgba(47, 111, 197, 0.32);
  box-shadow: 0 10px 24px rgba(47, 111, 197, 0.08);
}

.preference-card:hover {
  transform: translateY(-1px);
}

.segmented-option.active,
.preference-card.active {
  background: rgba(62, 141, 247, 0.12);
  border-color: rgba(62, 141, 247, 0.46);
  box-shadow:
    inset 0 0 0 1px rgba(62, 141, 247, 0.18),
    0 12px 24px rgba(47, 111, 197, 0.08);
  color: #2f6fc5;
}

.create-plan-page .form-control,
.create-plan-page .form-select {
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid var(--sp-line);
  border-radius: 12px;
  color: var(--sp-ink);
  min-height: 46px;
}

.create-plan-page .form-control::placeholder {
  color: #8aa0b7;
}

.create-plan-page .form-control:focus,
.create-plan-page .form-select:focus {
  background: #fff;
  border-color: rgba(47, 111, 197, 0.45);
  box-shadow: 0 0 0 0.2rem rgba(47, 111, 197, 0.12);
  color: var(--sp-ink);
}

.unit-input {
  display: flex;
}

.unit-input .form-control {
  border-radius: 12px 0 0 12px;
}

.unit-input span {
  align-items: center;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid var(--sp-line);
  border-left: 0;
  border-radius: 0 12px 12px 0;
  color: #58708c;
  display: inline-flex;
  justify-content: center;
  min-width: 58px;
}

.generate-button {
  align-items: center;
  display: flex;
  gap: 10px;
  justify-content: center;
  min-height: 54px;
  border-radius: 16px;
  box-shadow: 0 14px 30px rgba(47, 111, 197, 0.16);
  width: 100%;
}

.goal-input-wrap {
  position: relative;
  border: 1px solid var(--sp-line);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.7);
}

.goal-input-wrap:focus-within {
  border-color: rgba(62, 141, 247, 0.55);
  box-shadow: 0 0 0 0.2rem rgba(62, 141, 247, 0.15);
}

.goal-textarea,
.goal-mirror {
  box-sizing: border-box;
  width: 100%;
  min-height: 128px;
  margin: 0;
  padding: 0.5rem 0.75rem;
  border: 0;
  font-family: inherit;
  font-size: 1rem;
  line-height: 1.7;
  letter-spacing: normal;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: break-word;
}

.goal-textarea {
  position: relative;
  z-index: 2;
  display: block;
  resize: vertical;
  background: transparent;
  outline: none;
  box-shadow: none;
}

.create-plan-page .goal-textarea,
.create-plan-page .goal-textarea:focus {
  background: transparent;
  box-shadow: none;
}

.goal-textarea.is-ghosting {
  color: transparent;
  -webkit-text-fill-color: transparent;
  caret-color: var(--sp-ink);
}

.goal-mirror {
  position: absolute;
  inset: 0;
  z-index: 1;
  overflow: hidden;
  color: var(--sp-ink);
  pointer-events: none;
  background: transparent;
}

.goal-mirror-text {
  color: var(--sp-ink);
}

.goal-ghost {
  color: rgba(29, 49, 72, 0.68);
}

.goal-auto-tip {
  align-items: center;
  display: inline-flex;
  gap: 6px;
}

.input-foot {
  display: flex;
  font-size: 12px;
  justify-content: space-between;
  margin-top: 8px;
}

.analysis-box {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.76), rgba(247, 251, 255, 0.62));
  border: 1px solid rgba(80, 116, 152, 0.14);
  border-radius: 18px;
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  padding: 12px;
}

.analysis-row,
.timeline-item {
  align-items: start;
  display: grid;
  gap: 8px;
}

.analysis-row {
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(80, 116, 152, 0.14);
  border-radius: 14px;
  gap: 7px;
  min-height: 118px;
  overflow: hidden;
  padding: 12px;
  position: relative;
}

.analysis-row::before {
  border-radius: 999px;
  content: '';
  height: 3px;
  left: 12px;
  position: absolute;
  right: 12px;
  top: 0;
}

.analysis-row.tone-blue::before {
  background: #3e8df7;
}

.analysis-row.tone-green::before {
  background: #2fbf71;
}

.analysis-row.tone-amber::before {
  background: #f2a93b;
}

.analysis-row.tone-violet::before {
  background: #7c63f1;
}

.analysis-row.tone-red::before {
  background: #e25b5b;
}

.analysis-row.tone-cyan::before {
  background: #24a9c9;
}

.analysis-label,
.timeline-item span {
  align-items: center;
  color: var(--sp-muted);
  display: inline-flex;
  font-size: 13px;
  font-weight: 700;
  gap: 8px;
}

.analysis-row strong,
.timeline-item strong {
  color: var(--sp-ink);
}

.analysis-row strong {
  color: var(--sp-ink);
  font-size: clamp(17px, 1.7vw, 22px);
  line-height: 1.28;
  overflow-wrap: anywhere;
}

.analysis-row small {
  color: var(--sp-muted);
  font-size: 12px;
  line-height: 1.45;
  margin-top: auto;
}

.timeline {
  display: grid;
  gap: 14px;
}

.roadmap-context {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: -2px 0 14px;
}

.roadmap-context span {
  align-items: center;
  background: rgba(62, 141, 247, 0.1);
  border: 1px solid rgba(62, 141, 247, 0.18);
  border-radius: 999px;
  color: #2f6fc5;
  display: inline-flex;
  font-size: 12px;
  font-weight: 700;
  gap: 6px;
  min-height: 28px;
  padding: 5px 10px;
}

.timeline-item {
  border-left: 2px solid rgba(62, 141, 247, 0.18);
  grid-template-columns: auto 1fr;
  padding-left: 16px;
}

.timeline-item p {
  margin: 4px 0 0;
}

.side-state {
  align-items: center;
  display: inline-flex;
  gap: 8px;
}

.analysis-tip,
.empty-hint {
  font-size: 13px;
  line-height: 1.7;
  margin: 10px 0 0;
}

@media (max-width: 991px) {
  .planner-grid,
  .field-row,
  .field-row.two {
    grid-template-columns: 1fr;
  }

  .mode-segmented,
  .preference-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .result-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 575px) {
  .mode-segmented,
  .preference-grid,
  .analysis-box {
    grid-template-columns: 1fr;
  }

  .segmented-option {
    justify-content: flex-start;
  }
}

[data-bs-theme='dark'] .page-title,
[data-bs-theme='dark'] .panel-title h2,
[data-bs-theme='dark'] .side-title h3,
[data-bs-theme='dark'] .analysis-row strong,
[data-bs-theme='dark'] .timeline-item strong,
[data-bs-theme='dark'] .result-panel strong {
  color: #f2f7ff;
}

[data-bs-theme='dark'] .page-lead,
[data-bs-theme='dark'] .empty-hint,
[data-bs-theme='dark'] .side-state,
[data-bs-theme='dark'] .analysis-row,
[data-bs-theme='dark'] .analysis-tip,
[data-bs-theme='dark'] .timeline-item p,
[data-bs-theme='dark'] .result-panel p,
[data-bs-theme='dark'] .input-foot {
  color: #9fb6cf;
}

[data-bs-theme='dark'] .create-plan-page {
  background: #111827;
}

[data-bs-theme='dark'] .cp-card,
[data-bs-theme='dark'] .cp-side-card,
[data-bs-theme='dark'] .result-panel {
  background: rgba(11, 25, 43, 0.9);
  border-color: rgba(142, 191, 240, 0.14);
}

[data-bs-theme='dark'] .create-plan-page .form-control,
[data-bs-theme='dark'] .create-plan-page .form-select,
[data-bs-theme='dark'] .unit-input span,
[data-bs-theme='dark'] .segmented-control,
[data-bs-theme='dark'] .preference-card,
[data-bs-theme='dark'] .analysis-box,
[data-bs-theme='dark'] .analysis-row {
  background: rgba(7, 18, 33, 0.78);
  border-color: rgba(142, 191, 240, 0.18);
  color: #eef7ff;
}

[data-bs-theme='dark'] .segmented-option {
  color: #eef7ff;
}

[data-bs-theme='dark'] .segmented-option.active,
[data-bs-theme='dark'] .preference-card.active {
  background: rgba(125, 191, 255, 0.16);
  border-color: rgba(125, 191, 255, 0.42);
  color: #9fcbff;
}

[data-bs-theme='dark'] .roadmap-context span {
  background: rgba(125, 191, 255, 0.12);
  border-color: rgba(125, 191, 255, 0.24);
  color: #9fcbff;
}

[data-bs-theme='dark'] .analysis-label,
[data-bs-theme='dark'] .analysis-row small {
  color: #9bb7d3;
}

[data-bs-theme='dark'] .create-plan-page .form-control::placeholder {
  color: #7190ae;
}

[data-bs-theme='dark'] .goal-input-wrap {
  background: rgba(7, 18, 33, 0.78);
  border-color: rgba(142, 191, 240, 0.18);
}

[data-bs-theme='dark'] .goal-input-wrap:focus-within {
  border-color: rgba(125, 191, 255, 0.42);
  box-shadow: 0 0 0 0.2rem rgba(125, 191, 255, 0.12);
}

[data-bs-theme='dark'] .create-plan-page .goal-textarea,
[data-bs-theme='dark'] .create-plan-page .goal-textarea:focus {
  background: transparent;
  color: #eef7ff;
}

[data-bs-theme='dark'] .goal-textarea::placeholder {
  color: #7190ae;
}

[data-bs-theme='dark'] .goal-mirror,
[data-bs-theme='dark'] .goal-mirror-text {
  color: #eef7ff;
}

[data-bs-theme='dark'] .goal-textarea.is-ghosting {
  caret-color: #eef7ff;
}

[data-bs-theme='dark'] .goal-ghost {
  color: rgba(238, 247, 255, 0.42);
}
</style>
