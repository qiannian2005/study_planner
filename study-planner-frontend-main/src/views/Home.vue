<template>
  <div class="home-page app-shell">
    <Navbar />

    <main>
      <section class="home-hero">
        <div class="container hero-grid">
          <div class="hero-copy">
            <p class="page-kicker">{{ copy.hero.kicker }}</p>
            <h1>{{ copy.hero.title }}</h1>
            <p class="hero-lead typewriter-lead">
              <span class="typewriter-text">{{ displayedQuote }}</span>
              <span class="typewriter-cursor">|</span>
            </p>
            <div class="hero-actions">
              <router-link :to="primaryAction.to" class="btn btn-primary btn-lg">
                <i class="bi bi-magic"></i>
                {{ primaryAction.label }}
              </router-link>
              <router-link to="/forum" class="btn btn-outline-primary btn-lg">
                <i class="bi bi-people"></i>
                {{ copy.hero.secondary }}
              </router-link>
            </div>
            <div class="hero-proof" aria-label="Product highlights">
              <span v-for="item in copy.hero.proof" :key="item">
                <i class="bi bi-check2-circle"></i>
                {{ item }}
              </span>
            </div>
          </div>
        </div>
      </section>

      <section class="process-section">
        <div class="container">
          <div class="section-heading">
            <p class="page-kicker">{{ copy.process.kicker }}</p>
            <h2>{{ copy.process.title }}</h2>
            <p>{{ copy.process.lead }}</p>
          </div>

          <div class="process-rail">
            <article v-for="step in copy.process.steps" :key="step.title" class="process-step">
              <div class="step-icon">
                <i :class="step.icon"></i>
              </div>
              <span>{{ step.stage }}</span>
              <h3>{{ step.title }}</h3>
              <p>{{ step.text }}</p>
            </article>
          </div>
        </div>
      </section>

      <section class="feature-section">
        <div class="container">
          <div class="section-heading compact">
            <p class="page-kicker">{{ copy.features.kicker }}</p>
            <h2>{{ copy.features.title }}</h2>
          </div>

          <div class="feature-grid">
            <article v-for="feature in copy.features.items" :key="feature.title" class="feature-card">
              <div class="feature-icon">
                <i :class="feature.icon"></i>
              </div>
              <h3>{{ feature.title }}</h3>
              <p>{{ feature.text }}</p>
            </article>
          </div>
        </div>
      </section>

      <section class="showcase-section">
        <div class="container showcase-grid">
          <div class="showcase-copy">
            <p class="page-kicker">{{ copy.showcase.kicker }}</p>
            <h2>{{ copy.showcase.title }}</h2>
            <p>{{ copy.showcase.lead }}</p>
            <div class="showcase-actions">
              <router-link to="/ai-assistant" class="btn btn-primary">
                <i class="bi bi-robot"></i>
                {{ copy.showcase.ai }}
              </router-link>
              <router-link to="/chat" class="btn btn-outline-primary">
                <i class="bi bi-chat-dots"></i>
                {{ copy.showcase.chat }}
              </router-link>
            </div>
          </div>

          <div class="learning-map">
            <div v-for="item in copy.showcase.map" :key="item.title" class="map-item">
              <span :class="item.tone"></span>
              <div>
                <strong>{{ item.title }}</strong>
                <p>{{ item.text }}</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="testimonials-section">
        <div class="container">
          <div class="section-heading">
            <p class="page-kicker">{{ copy.testimonials.kicker }}</p>
            <h2>{{ copy.testimonials.title }}</h2>
            <p>{{ copy.testimonials.lead }}</p>
          </div>
          <div class="testimonials-grid">
            <div v-for="item in copy.testimonials.items" :key="item.name" class="testimonial-card">
              <div class="testimonial-stars">
                <i v-for="n in 5" :key="n" class="bi bi-star-fill"></i>
              </div>
              <p class="testimonial-text">"{{ item.text }}"</p>
              <div class="testimonial-author">
                <span class="author-avatar">{{ item.avatar }}</span>
                <div>
                  <strong>{{ item.name }}</strong>
                  <small>{{ item.role }}</small>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import Navbar from '../components/Navbar.vue'
import Footer from '../components/Footer.vue'
import { useUserStore } from '../stores/user'
import { aiApi } from '../api/ai'

const { locale } = useI18n()
const userStore = useUserStore()

const zhQuotes = ref([
  '坚持的秘诀不是意志力，是好的计划。',
  '学习不怕慢，只怕站。',
  '每天进步一点点，终将遇见更好的自己。',
  '把大目标拆成小步骤，行动就不再困难。',
  '最好的学习时间是现在。'
])

const enQuotes = ref([
  'The secret to consistency isn\'t willpower — it\'s a good plan.',
  'A little progress each day adds up to big results.',
  'Break big goals into small steps, and action follows.',
  'The best time to learn is now.',
  'Don\'t wait for motivation. Start, and it will follow.'
])

const QUOTES_CACHE_KEY = 'homeAiQuotes'
const QUOTES_CACHE_TTL = 24 * 60 * 60 * 1000

const displayedQuote = ref('')
let typeTimer = null
let pauseTimer = null
let quoteIndex = 0
let charIndex = 0
let isDeleting = false

function getQuotes() {
  return locale.value.startsWith('zh') ? zhQuotes.value : enQuotes.value
}

function tick() {
  const quotes = getQuotes()
  const current = quotes[quoteIndex % quotes.length]

  if (!isDeleting) {
    charIndex++
    displayedQuote.value = current.slice(0, charIndex)
    if (charIndex >= current.length) {
      pauseTimer = setTimeout(() => { isDeleting = true; tick() }, 2200)
      return
    }
    typeTimer = setTimeout(tick, 80)
  } else {
    charIndex--
    displayedQuote.value = current.slice(0, charIndex)
    if (charIndex <= 0) {
      isDeleting = false
      quoteIndex = (quoteIndex + 1) % quotes.length
      pauseTimer = setTimeout(tick, 400)
      return
    }
    typeTimer = setTimeout(tick, 40)
  }
}

function startTypewriter() {
  stopTypewriter()
  quoteIndex = 0
  charIndex = 0
  isDeleting = false
  displayedQuote.value = ''
  tick()
}

function stopTypewriter() {
  clearTimeout(typeTimer)
  clearTimeout(pauseTimer)
}

function loadCachedQuotes() {
  try {
    const raw = sessionStorage.getItem(QUOTES_CACHE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (!parsed?.ts || Date.now() - parsed.ts > QUOTES_CACHE_TTL) return null
    return parsed
  } catch {
    return null
  }
}

function applyAiQuotes(zh, en) {
  if (Array.isArray(zh) && zh.length) zhQuotes.value = zh
  if (Array.isArray(en) && en.length) enQuotes.value = en
}

function parseQuotesFromAi(text) {
  if (!text) return []
  return String(text)
    .split(/\r?\n/)
    .map(line => line.replace(/^\s*[\d一二三四五六七八九十]+[.、)）:：\-\s]+/, '').trim())
    .map(line => line.replace(/^["“”'']+|["“”'']+$/g, '').trim())
    .filter(line => line.length >= 6 && line.length <= 60)
    .slice(0, 6)
}

async function fetchAiQuotes() {
  const cached = loadCachedQuotes()
  if (cached) {
    applyAiQuotes(cached.zh, cached.en)
    return
  }

  try {
    const prompt = `请生成 5 句鼓励学习的简短中文金句和 5 句英文金句。要求：
1. 每句不超过 25 个字（英文不超过 80 字符）
2. 风格积极、具体、不空洞
3. 严格按以下 JSON 格式返回，不要解释、不要 Markdown：
{"zh":["...","...","...","...","..."],"en":["...","...","...","...","..."]}`
    const result = await aiApi.chat([{ role: 'user', content: prompt }])
    if (result?.code !== 200) return
    const text = result.data?.content || result.data || ''
    const match = String(text).match(/\{[\s\S]*"zh"[\s\S]*"en"[\s\S]*\}/)
    let zh = [], en = []
    if (match) {
      try {
        const parsed = JSON.parse(match[0])
        zh = Array.isArray(parsed.zh) ? parsed.zh.filter(s => typeof s === 'string' && s.trim()) : []
        en = Array.isArray(parsed.en) ? parsed.en.filter(s => typeof s === 'string' && s.trim()) : []
      } catch {
        // fallthrough to line parsing
      }
    }
    if (!zh.length && !en.length) {
      const lines = parseQuotesFromAi(text)
      zh = lines.filter(l => /[一-龥]/.test(l))
      en = lines.filter(l => !/[一-龥]/.test(l))
    }
    if (!zh.length && !en.length) return
    applyAiQuotes(zh, en)
    try {
      sessionStorage.setItem(QUOTES_CACHE_KEY, JSON.stringify({ ts: Date.now(), zh, en }))
    } catch {}
  } catch (error) {
    console.warn('AI 金句获取失败，使用本地金句:', error)
  }
}

onMounted(() => {
  startTypewriter()
  fetchAiQuotes()
})
onUnmounted(stopTypewriter)
watch(locale, startTypewriter)

const weekBlocks = [1, 2, 3, 4, 5, 6, 7]

const zhCopy = {
  hero: {
    kicker: 'AI study cockpit',
    title: '把学习目标变成每天能完成的行动。',
    lead: '输入目标、基础和时间，系统会生成可执行计划；每天按任务学习、打卡复盘，再用 AI 和同伴一起把节奏调顺。',
    secondary: '看看学习社区',
    proof: ['AI 拆解任务', '每日打卡追踪', '计划工作台协作']
  },
  preview: {
    label: '当前计划',
    plan: '30 天 Python 入门',
    today: '今日焦点',
    task: '函数与模块练习',
    detail: '完成 3 个小练习，整理一个可复用工具函数，并记录卡住的地方。',
    time: '预计 2 小时',
    streak: '连续打卡',
    progress: '总体进度',
    next: '下次提醒'
  },
  process: {
    kicker: 'Usage flow',
    title: '使用流程',
    lead: '从目标到复盘只保留四个关键动作，新用户一眼就能知道下一步。',
    steps: [
      { stage: '01', icon: 'bi bi-bullseye', title: '设定目标', text: '写下学习内容、当前基础、每天可投入时间和计划周期。' },
      { stage: '02', icon: 'bi bi-magic', title: '生成计划', text: 'AI 自动拆成每日任务，并给出资源、时长和学习顺序。' },
      { stage: '03', icon: 'bi bi-check2-square', title: '执行打卡', text: '进入今日任务，完成后记录学习时长，进度会自动更新。' },
      { stage: '04', icon: 'bi bi-graph-up-arrow', title: '复盘调整', text: '查看统计、连续打卡和计划详情，必要时让 AI 辅助调整节奏。' }
    ]
  },
  features: {
    kicker: 'Core features',
    title: '核心功能',
    items: [
      { icon: 'bi bi-calendar2-week', title: '计划生成', text: '按目标、基础、时长生成每日学习路线，减少从零规划的负担。' },
      { icon: 'bi bi-clipboard2-check', title: '今日任务', text: '仪表盘直接显示当前最该做的任务，避免在多个页面里寻找入口。' },
      { icon: 'bi bi-bar-chart-line', title: '数据统计', text: '用学习时长、打卡天数和趋势图呈现真实推进情况。' },
      { icon: 'bi bi-kanban', title: '计划工作台', text: '支持查看、编辑和协作推进计划，适合小组学习或共同监督。' },
      { icon: 'bi bi-robot', title: 'AI 学习助手', text: '随时提问、复盘卡点、让系统给出下一步学习建议。' },
      { icon: 'bi bi-chat-heart', title: '论坛与聊天室', text: '把问题、经验和阶段成果沉淀到社区，学习不只靠独自坚持。' }
    ]
  },
  showcase: {
    kicker: 'What else matters',
    title: '补上“看得见的学习状态”',
    lead: '除了功能入口，首页加入一个示例计划状态，让用户在注册前就知道产品会如何陪他们推进。',
    ai: '问 AI 助手',
    chat: '进入聊天室',
    map: [
      { tone: 'blue', title: '目标清晰', text: '每个计划都有明确主题、周期和日均学习时长。' },
      { tone: 'green', title: '节奏稳定', text: '连续打卡和完成率帮助判断计划是否可持续。' },
      { tone: 'rose', title: '问题有出口', text: '卡住时可以问 AI，也可以去社区发帖或和同伴讨论。' }
    ]
  },
  testimonials: {
    kicker: 'User stories',
    title: '他们在用这个工具学习',
    lead: '来自真实用户的学习体验分享。',
    items: [
      { avatar: '🎓', name: '小王', role: '大三 · Java 后端方向', text: '之前学 Spring Boot 总是三天打鱼两天晒网，用了这个之后每天任务很明确，连续打卡了 21 天，第一次觉得学编程也能有节奏感。' },
      { avatar: '💻', name: '陈同学', role: '转行自学前端', text: '零基础最怕的就是不知道先学什么，AI 帮我拆好了每天的任务，我只要跟着做就行。遇到不懂的直接问学习助手，比自己查资料快多了。' },
      { avatar: '📊', name: '林sir', role: '在职 · 备考软考', text: '每天只有 1 小时学习时间，轻松模式帮我把复习量控制得刚好。打卡日历上一排绿色看着就有成就感，考前复盘也很方便。' }
    ]
  }
}

const enCopy = {
  hero: {
    kicker: 'AI study cockpit',
    title: 'Turn a learning goal into actions you can finish today.',
    lead: 'Enter your goal, level, and schedule. The app builds a plan, surfaces today\'s task, tracks check-ins, and keeps AI help and peers close by.',
    secondary: 'Explore community',
    proof: ['AI task breakdown', 'Daily check-in tracking', 'Collaborative plan workbench']
  },
  preview: {
    label: 'Current plan',
    plan: '30-day Python starter',
    today: 'Today focus',
    task: 'Functions and modules',
    detail: 'Finish three exercises, extract one reusable helper, and note what slowed you down.',
    time: 'About 2 hours',
    streak: 'Streak',
    progress: 'Progress',
    next: 'Next reminder'
  },
  process: {
    kicker: 'Usage flow',
    title: 'The homepage now explains the path',
    lead: 'Four concrete steps take a new learner from intent to review.',
    steps: [
      { stage: '01', icon: 'bi bi-bullseye', title: 'Set the goal', text: 'Describe the topic, current level, daily time, and plan length.' },
      { stage: '02', icon: 'bi bi-magic', title: 'Generate the plan', text: 'AI breaks the goal into daily tasks with order, resources, and time estimates.' },
      { stage: '03', icon: 'bi bi-check2-square', title: 'Check in daily', text: 'Open today\'s task, study, record your hours, and keep progress moving.' },
      { stage: '04', icon: 'bi bi-graph-up-arrow', title: 'Review and adapt', text: 'Use stats, details, and AI guidance to adjust the pace when needed.' }
    ]
  },
  features: {
    kicker: 'Core features',
    title: 'The core loop is now front and center',
    items: [
      { icon: 'bi bi-calendar2-week', title: 'Plan generation', text: 'Create a daily roadmap from your goal, level, available hours, and duration.' },
      { icon: 'bi bi-clipboard2-check', title: 'Today\'s task', text: 'The dashboard shows what matters now, so the next action is never buried.' },
      { icon: 'bi bi-bar-chart-line', title: 'Study stats', text: 'Track hours, check-in days, streaks, and trends as real feedback.' },
      { icon: 'bi bi-kanban', title: 'Plan workbench', text: 'Review, edit, and coordinate plan progress with a more structured workspace.' },
      { icon: 'bi bi-robot', title: 'AI assistant', text: 'Ask questions, review blockers, and get suggestions for the next study move.' },
      { icon: 'bi bi-chat-heart', title: 'Forum and chat', text: 'Capture questions and share momentum with other learners.' }
    ]
  },
  showcase: {
    kicker: 'What else matters',
    title: 'Show the learning state before signup',
    lead: 'The homepage adds a concrete plan preview, so users understand how the product helps them move day by day.',
    ai: 'Ask AI',
    chat: 'Open chat',
    map: [
      { tone: 'blue', title: 'Clear goals', text: 'Every plan carries a topic, duration, and daily time commitment.' },
      { tone: 'green', title: 'Stable rhythm', text: 'Streak and completion rate make sustainability visible.' },
      { tone: 'rose', title: 'Help paths', text: 'When stuck, learners can ask AI, post in the forum, or talk with peers.' }
    ]
  },
  testimonials: {
    kicker: 'User stories',
    title: 'Learners using this tool',
    lead: 'Real experiences from users who turned goals into daily habits.',
    items: [
      { avatar: '🎓', name: 'Wang', role: 'Junior · Java Backend', text: 'I used to quit after a few days every time I tried to learn Spring Boot. With daily tasks laid out, I hit a 21-day streak — first time coding ever felt sustainable.' },
      { avatar: '💻', name: 'Chen', role: 'Career switcher · Frontend', text: 'As a total beginner, the hardest part was knowing where to start. AI broke it down day by day, and the assistant answered questions faster than searching on my own.' },
      { avatar: '📊', name: 'Lin', role: 'Working pro · Certification prep', text: 'Only had one hour a day. Relaxed mode kept the workload just right. Seeing a row of green on the calendar is genuinely motivating.' }
    ]
  }
}

const copy = computed(() => (locale.value.startsWith('zh') ? zhCopy : enCopy))

const primaryAction = computed(() => {
  if (userStore.isLoggedIn) {
    return {
      to: '/create-plan',
      label: locale.value.startsWith('zh') ? '创建学习计划' : 'Create study plan'
    }
  }

  return {
    to: '/register',
    label: locale.value.startsWith('zh') ? '注册并开始' : 'Sign up and start'
  }
})
</script>

<style scoped>
.home-page {
  background:
    linear-gradient(180deg, rgba(246, 251, 255, 0.86) 0%, rgba(255, 255, 255, 0.94) 46%, rgba(238, 248, 255, 0.9) 100%),
    repeating-linear-gradient(90deg, rgba(37, 84, 132, 0.055) 0 1px, transparent 1px 88px),
    repeating-linear-gradient(0deg, rgba(37, 84, 132, 0.045) 0 1px, transparent 1px 88px);
  color: #172033;
  min-height: 100vh;
}

.home-hero {
  overflow: hidden;
  padding: 72px 0 56px;
}

.hero-grid,
.showcase-grid {
  align-items: center;
  display: grid;
  gap: 44px;
  grid-template-columns: minmax(0, 1.04fr) minmax(320px, 0.96fr);
}

.hero-grid {
  grid-template-columns: minmax(0, 1fr);
}

.hero-copy h1 {
  color: #111827;
  font-family: Georgia, "Times New Roman", "SimSun", serif;
  font-size: clamp(2.55rem, 6vw, 5.4rem);
  font-weight: 500;
  letter-spacing: 0;
  line-height: 0.96;
  margin: 0;
  max-width: 780px;
}

.hero-lead,
.section-heading p,
.showcase-copy > p {
  color: #556276;
  font-size: 1.08rem;
  line-height: 1.8;
}

.hero-lead {
  margin: 24px 0 0;
  max-width: 720px;
}

.hero-actions,
.showcase-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}

.hero-actions .btn,
.showcase-actions .btn {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.hero-proof {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 26px;
}

.hero-proof span {
  align-items: center;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(47, 111, 197, 0.14);
  border-radius: 999px;
  color: #38506f;
  display: inline-flex;
  font-size: 0.94rem;
  font-weight: 700;
  gap: 7px;
  padding: 8px 12px;
}

.planner-preview {
  background: #fffef8;
  border: 1px solid rgba(23, 32, 51, 0.12);
  border-radius: 8px;
  box-shadow: 0 28px 70px rgba(54, 83, 116, 0.18);
  padding: 18px;
  transform: rotate(1.1deg);
}

.preview-topbar {
  align-items: center;
  border-bottom: 1px solid rgba(23, 32, 51, 0.1);
  display: flex;
  justify-content: space-between;
  padding: 0 2px 14px;
}

.preview-topbar span,
.preview-column small,
.process-step span {
  color: #6b7280;
  font-size: 0.78rem;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

.preview-topbar strong {
  color: #24446b;
}

.preview-board {
  display: grid;
  gap: 14px;
  grid-template-columns: minmax(0, 1.25fr) minmax(150px, 0.75fr);
  margin-top: 16px;
}

.preview-column {
  display: grid;
  gap: 12px;
}

.preview-column.focus {
  background:
    linear-gradient(135deg, rgba(62, 141, 247, 0.12), rgba(74, 154, 114, 0.08)),
    #f7fbff;
  border: 1px solid rgba(47, 111, 197, 0.14);
  border-radius: 8px;
  padding: 22px;
}

.preview-column h2 {
  color: #172033;
  font-size: clamp(1.4rem, 3vw, 2.1rem);
  line-height: 1.12;
  margin: 0;
}

.preview-column p,
.map-item p,
.feature-card p,
.process-step p {
  color: #607086;
  line-height: 1.7;
  margin: 0;
}

.time-pill {
  align-items: center;
  background: #172033;
  border-radius: 999px;
  color: #fff;
  display: inline-flex;
  font-weight: 800;
  gap: 8px;
  justify-self: start;
  padding: 8px 12px;
}

.mini-stat {
  background: #f4f8ff;
  border: 1px solid rgba(62, 141, 247, 0.16);
  border-radius: 8px;
  display: grid;
  gap: 4px;
  padding: 16px;
}

.mini-stat span {
  color: #65758b;
  font-size: 0.86rem;
  font-weight: 700;
}

.mini-stat strong {
  color: #2f6fc5;
  font-size: 1.55rem;
  line-height: 1;
}

.mini-stat.green strong {
  color: #3d8a66;
}

.mini-stat.amber strong {
  color: #b77a15;
}

.week-strip {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  margin-top: 16px;
}

.week-strip span {
  background: #e8eef6;
  border-radius: 999px;
  height: 10px;
}

.week-strip span.active {
  background: #4a9a72;
}

.process-section,
.feature-section,
.showcase-section,
.testimonials-section {
  padding: 54px 0;
}

.section-heading {
  margin-bottom: 28px;
  max-width: 780px;
}

.section-heading.compact {
  align-items: end;
  display: flex;
  justify-content: space-between;
  max-width: none;
}

.section-heading h2,
.showcase-copy h2 {
  color: #172033;
  font-family: Georgia, "Times New Roman", "SimSun", serif;
  font-size: clamp(2rem, 4vw, 3.4rem);
  font-weight: 500;
  letter-spacing: 0;
  line-height: 1.05;
  margin: 0 0 14px;
}

.process-rail {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.process-step,
.feature-card,
.map-item {
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(23, 32, 51, 0.1);
  border-radius: 8px;
  box-shadow: 0 18px 44px rgba(54, 83, 116, 0.09);
}

.process-step {
  padding: 22px;
}

.step-icon,
.feature-icon {
  align-items: center;
  background: #edf6ff;
  border: 1px solid rgba(62, 141, 247, 0.18);
  border-radius: 8px;
  color: #2f6fc5;
  display: inline-flex;
  font-size: 1.25rem;
  height: 44px;
  justify-content: center;
  margin-bottom: 20px;
  width: 44px;
}

.process-step h3,
.feature-card h3 {
  color: #172033;
  font-size: 1.12rem;
  margin: 8px 0 10px;
}

.feature-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.feature-card {
  padding: 24px;
}

.showcase-section {
  background: rgba(255, 255, 255, 0.42);
  border-bottom: 1px solid rgba(23, 32, 51, 0.08);
  border-top: 1px solid rgba(23, 32, 51, 0.08);
}

.learning-map {
  display: grid;
  gap: 12px;
}

.map-item {
  align-items: start;
  display: grid;
  gap: 14px;
  grid-template-columns: 12px minmax(0, 1fr);
  padding: 18px;
}

.map-item > span {
  border-radius: 999px;
  display: block;
  height: 100%;
  min-height: 48px;
}

.map-item .blue {
  background: #3e8df7;
}

.map-item .green {
  background: #4a9a72;
}

.map-item .rose {
  background: #d86f8b;
}

.map-item strong {
  color: #172033;
  display: block;
  margin-bottom: 4px;
}

.testimonials-grid {
  display: grid;
  gap: 20px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.testimonial-card {
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(23, 32, 51, 0.1);
  border-radius: 12px;
  box-shadow: 0 18px 44px rgba(54, 83, 116, 0.09);
  display: flex;
  flex-direction: column;
  padding: 24px;
}

.testimonial-stars {
  color: #f59e0b;
  display: flex;
  gap: 2px;
  margin-bottom: 14px;
}

.testimonial-text {
  color: #333;
  flex: 1;
  font-size: 0.95rem;
  line-height: 1.75;
  margin: 0 0 18px;
}

.testimonial-author {
  align-items: center;
  border-top: 1px solid rgba(23, 32, 51, 0.08);
  display: flex;
  gap: 10px;
  padding-top: 14px;
}

.author-avatar {
  align-items: center;
  background: #edf6ff;
  border-radius: 50%;
  display: inline-flex;
  font-size: 1.2rem;
  height: 40px;
  justify-content: center;
  width: 40px;
}

.testimonial-author strong {
  color: #172033;
  display: block;
  font-size: 0.9rem;
}

.testimonial-author small {
  color: #556276;
  font-size: 0.78rem;
}

[data-bs-theme='dark'] .home-page {
  background:
    linear-gradient(180deg, rgba(9, 18, 32, 0.96) 0%, rgba(12, 24, 41, 0.98) 100%),
    repeating-linear-gradient(90deg, rgba(188, 213, 241, 0.06) 0 1px, transparent 1px 88px),
    repeating-linear-gradient(0deg, rgba(188, 213, 241, 0.045) 0 1px, transparent 1px 88px);
  color: #edf5ff;
}

[data-bs-theme='dark'] .hero-copy h1,
[data-bs-theme='dark'] .section-heading h2,
[data-bs-theme='dark'] .showcase-copy h2,
[data-bs-theme='dark'] .preview-column h2,
[data-bs-theme='dark'] .process-step h3,
[data-bs-theme='dark'] .feature-card h3,
[data-bs-theme='dark'] .map-item strong {
  color: #edf5ff;
}

[data-bs-theme='dark'] .hero-lead,
[data-bs-theme='dark'] .section-heading p,
[data-bs-theme='dark'] .showcase-copy > p,
[data-bs-theme='dark'] .process-step p,
[data-bs-theme='dark'] .feature-card p,
[data-bs-theme='dark'] .map-item p {
  color: #aebdd1;
}

[data-bs-theme='dark'] .planner-preview,
[data-bs-theme='dark'] .process-step,
[data-bs-theme='dark'] .feature-card,
[data-bs-theme='dark'] .map-item {
  background: rgba(15, 24, 38, 0.82);
  border-color: rgba(188, 213, 241, 0.14);
}

[data-bs-theme='dark'] .preview-column.focus,
[data-bs-theme='dark'] .mini-stat,
[data-bs-theme='dark'] .hero-proof span {
  background: rgba(20, 31, 48, 0.88);
  border-color: rgba(188, 213, 241, 0.16);
  color: #dbeaff;
}

[data-bs-theme='dark'] .preview-topbar {
  border-bottom-color: rgba(188, 213, 241, 0.14);
}

[data-bs-theme='dark'] .preview-topbar strong,
[data-bs-theme='dark'] .mini-stat span,
[data-bs-theme='dark'] .preview-column small,
[data-bs-theme='dark'] .process-step span {
  color: #aebdd1;
}

[data-bs-theme='dark'] .showcase-section {
  background: rgba(12, 24, 41, 0.58);
  border-color: rgba(188, 213, 241, 0.1);
}

[data-bs-theme='dark'] .testimonial-card {
  background: rgba(15, 24, 38, 0.82);
  border-color: rgba(188, 213, 241, 0.14);
}

[data-bs-theme='dark'] .testimonial-text {
  color: #c8d5e3;
}

[data-bs-theme='dark'] .testimonial-author {
  border-color: rgba(188, 213, 241, 0.12);
}

[data-bs-theme='dark'] .testimonial-author strong {
  color: #edf5ff;
}

[data-bs-theme='dark'] .testimonial-author small {
  color: #aebdd1;
}

[data-bs-theme='dark'] .author-avatar {
  background: rgba(20, 31, 48, 0.88);
}

@media (max-width: 991.98px) {
  .home-hero {
    padding-top: 46px;
  }

  .hero-grid,
  .showcase-grid {
    grid-template-columns: 1fr;
  }

  .process-rail,
  .feature-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .planner-preview {
    transform: none;
  }

  .testimonials-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 575.98px) {
  .home-hero,
  .process-section,
  .feature-section,
  .showcase-section,
  .testimonials-section {
    padding: 38px 0;
  }

  .hero-copy h1 {
    font-size: 2.45rem;
  }

  .hero-actions .btn,
  .showcase-actions .btn {
    width: 100%;
  }

  .preview-board,
  .process-rail,
  .feature-grid,
  .testimonials-grid {
    grid-template-columns: 1fr;
  }

  .planner-preview {
    padding: 18px;
  }

  .section-heading.compact {
    display: block;
  }
}
</style>
