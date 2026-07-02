<template>
  <main class="login-sky">
    <nav class="login-nav" aria-label="Login navigation">
      <router-link to="/" class="brand-mark">
        <span class="brand-icon"><i class="bi bi-book"></i></span>
        <span>智慧学习平台</span>
      </router-link>
      <router-link to="/" class="nav-back">
        <i class="bi bi-arrow-left"></i>
        <span>{{ $t('common.back') }}</span>
      </router-link>
    </nav>

    <section class="login-hero">
      <div class="hero-copy">
        <p class="eyebrow">AI 智慧学习平台</p>
        <h1>把目标写下来，今天就不再只是想想。</h1>
        <p class="hero-text">
          登录后查看学习计划、今日任务和进度复盘，也可以让 AI 继续帮你细化任务、调整节奏。
        </p>
      </div>

      <div class="login-panel-wrap">
        <div class="floating-plan" aria-hidden="true">
          <span class="plan-pin"></span>
          <strong>Today</strong>
          <small>Focus session</small>
          <div class="plan-lines">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>

        <div class="login-panel">
          <div class="panel-heading">
            <span class="panel-icon"><i class="bi bi-stars"></i></span>
            <div>
              <h2>{{ $t('auth.login.title') }}</h2>
              <p>{{ $t('auth.login.welcome') }}</p>
            </div>
          </div>

          <form @submit.prevent="handleLogin" class="login-form">
            <div class="field-group">
              <label for="username">{{ $t('auth.login.username') }}</label>
              <div class="input-shell">
                <i class="bi bi-person"></i>
                <input
                  type="text"
                  id="username"
                  v-model="username"
                  required
                  autocomplete="username"
                />
              </div>
            </div>

            <div class="field-group">
              <label for="password">{{ $t('auth.login.password') }}</label>
              <div class="input-shell">
                <i class="bi bi-lock"></i>
                <input
                  type="password"
                  id="password"
                  v-model="password"
                  required
                  autocomplete="current-password"
                />
              </div>
            </div>

            <button type="submit" class="login-submit" :disabled="loading">
              <span v-if="loading" class="spinner-border spinner-border-sm"></span>
              <span>{{ $t('auth.login.submit') }}</span>
              <i class="bi bi-arrow-right"></i>
            </button>
          </form>

          <p class="register-link">
            {{ $t('auth.login.noAccount') }}
            <router-link to="/register">{{ $t('auth.login.registerNow') }}</router-link>
          </p>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '../stores/user'
import { showToast } from '../utils/toast'

const { t } = useI18n()

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const username = ref('')
const password = ref('')
const loading = ref(false)

async function handleLogin() {
  loading.value = true
  
  try {
    const result = await userStore.login(username.value, password.value)
    
    if (result.success) {
      showToast(t('auth.loginSuccess'), 'success')
      
      // 检查是否有重定向URL
      const redirect = route.query.redirect
      setTimeout(() => {
        if (redirect) {
          router.push(redirect)
        } else if (result.user?.role === 'admin') {
          router.push('/admin')
        } else {
          router.push('/')
        }
      }, 500)
    } else {
      showToast(result.message || t('errors.unknown'), 'error')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-sky {
  --ink: #172033;
  --muted: #667085;
  --glass: rgba(255, 255, 255, 0.72);
  --line: rgba(23, 32, 51, 0.12);
  --blue: #3e8df7;
  --leaf: #4a9a72;
  --rose: #f0a7b5;
  min-height: 100vh;
  overflow: hidden;
  position: relative;
  color: var(--ink);
  background:
    radial-gradient(circle at 16% 22%, rgba(255, 255, 255, 0.88) 0 7%, transparent 24%),
    radial-gradient(circle at 76% 16%, rgba(173, 216, 230, 0.38) 0 16%, transparent 34%),
    radial-gradient(circle at 36% 82%, rgba(247, 207, 216, 0.34) 0 18%, transparent 40%),
    linear-gradient(145deg, #f8fbfd 0%, #edf6f7 48%, #f8f3f6 100%);
}

.login-sky::before,
.login-sky::after {
  content: "";
  position: absolute;
  inset: auto -8% -18%;
  height: 42vh;
  pointer-events: none;
}

.login-sky::before {
  background:
    radial-gradient(ellipse at 12% 46%, rgba(120, 184, 146, 0.18) 0 15%, transparent 40%),
    radial-gradient(ellipse at 56% 54%, rgba(240, 167, 181, 0.16) 0 14%, transparent 38%),
    radial-gradient(ellipse at 86% 48%, rgba(143, 198, 236, 0.18) 0 13%, transparent 38%);
  filter: blur(10px);
}

.login-sky::after {
  background:
    radial-gradient(ellipse at 18% 70%, rgba(255, 255, 255, 0.62) 0 18%, transparent 44%),
    radial-gradient(ellipse at 48% 64%, rgba(255, 255, 255, 0.7) 0 22%, transparent 50%),
    radial-gradient(ellipse at 82% 72%, rgba(255, 255, 255, 0.56) 0 18%, transparent 46%);
}

.login-nav {
  align-items: center;
  display: flex;
  gap: 16px;
  justify-content: space-between;
  left: 50%;
  max-width: 1120px;
  padding: 22px 24px 0;
  position: relative;
  transform: translateX(-50%);
  width: 100%;
  z-index: 2;
}

.brand-mark,
.nav-back {
  align-items: center;
  backdrop-filter: blur(18px);
  background: rgba(255, 255, 255, 0.52);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 999px;
  color: var(--ink);
  display: inline-flex;
  gap: 10px;
  min-height: 44px;
  padding: 9px 16px;
  text-decoration: none;
  box-shadow: 0 16px 40px rgba(68, 103, 137, 0.12);
}

.brand-mark {
  font-family: Georgia, "Times New Roman", serif;
  font-size: 1.18rem;
  font-style: italic;
}

.brand-icon {
  align-items: center;
  background: rgba(62, 141, 247, 0.14);
  border: 1px solid rgba(62, 141, 247, 0.28);
  border-radius: 50%;
  color: #2f6fc5;
  display: inline-flex;
  height: 28px;
  justify-content: center;
  width: 28px;
}

.nav-back {
  font-size: 0.94rem;
  font-weight: 650;
}

.login-hero {
  align-items: center;
  display: grid;
  gap: 52px;
  grid-template-columns: minmax(0, 1fr) minmax(340px, 430px);
  margin: 0 auto;
  max-width: 1120px;
  min-height: calc(100vh - 70px);
  padding: 48px 24px 78px;
  position: relative;
  z-index: 1;
}

.hero-copy {
  max-width: 610px;
}

.eyebrow {
  color: #315c78;
  font-size: 0.8rem;
  font-weight: 800;
  letter-spacing: 0;
  margin-bottom: 18px;
  text-transform: uppercase;
}

.hero-copy h1 {
  color: #172f57;
  font-family: Georgia, "Times New Roman", serif;
  font-size: clamp(2.45rem, 4.8vw, 4.7rem);
  font-weight: 500;
  line-height: 1.06;
  margin: 0;
  max-width: 640px;
}

.hero-text {
  color: #42526b;
  font-size: 1.08rem;
  line-height: 1.8;
  margin: 24px 0 0;
  max-width: 560px;
}

.login-panel-wrap {
  position: relative;
}

.floating-plan {
  background: #fffdf8;
  border: 1px solid rgba(23, 32, 51, 0.08);
  border-radius: 8px;
  box-shadow: 0 28px 60px rgba(53, 72, 91, 0.2);
  color: #263247;
  padding: 22px 20px 18px;
  position: absolute;
  right: -34px;
  top: -66px;
  transform: rotate(5deg);
  width: 168px;
}

.plan-pin {
  background: var(--rose);
  border-radius: 50%;
  box-shadow: 0 2px 0 rgba(99, 52, 61, 0.18);
  display: block;
  height: 12px;
  left: 50%;
  position: absolute;
  top: 10px;
  transform: translateX(-50%);
  width: 12px;
}

.floating-plan strong,
.floating-plan small {
  display: block;
}

.floating-plan strong {
  font-family: Georgia, "Times New Roman", serif;
  font-size: 1.2rem;
  margin-top: 12px;
}

.floating-plan small {
  color: var(--muted);
  margin-top: 2px;
}

.plan-lines {
  display: grid;
  gap: 9px;
  margin-top: 18px;
}

.plan-lines span {
  background: linear-gradient(90deg, rgba(62, 141, 247, 0.32), rgba(74, 154, 114, 0.22));
  border-radius: 999px;
  height: 8px;
}

.plan-lines span:nth-child(2) {
  width: 78%;
}

.plan-lines span:nth-child(3) {
  width: 58%;
}

.login-panel {
  backdrop-filter: blur(24px);
  background: var(--glass);
  border: 1px solid rgba(255, 255, 255, 0.76);
  border-radius: 8px;
  box-shadow: 0 30px 90px rgba(54, 83, 116, 0.24);
  padding: 34px;
}

.panel-heading {
  align-items: center;
  display: flex;
  gap: 16px;
  margin-bottom: 28px;
}

.panel-icon {
  align-items: center;
  background: rgba(62, 141, 247, 0.14);
  border: 1px solid rgba(62, 141, 247, 0.28);
  border-radius: 50%;
  color: #2f6fc5;
  display: inline-flex;
  flex: 0 0 auto;
  height: 48px;
  justify-content: center;
  width: 48px;
}

.panel-heading h2 {
  font-size: 1.6rem;
  font-weight: 750;
  margin: 0;
}

.panel-heading p {
  color: var(--muted);
  margin: 4px 0 0;
}

.login-form {
  display: grid;
  gap: 18px;
}

.field-group label {
  color: #2d3748;
  display: block;
  font-size: 0.92rem;
  font-weight: 700;
  margin-bottom: 8px;
}

.input-shell {
  align-items: center;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid var(--line);
  border-radius: 8px;
  display: flex;
  gap: 12px;
  min-height: 54px;
  padding: 0 15px;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.input-shell:focus-within {
  background: rgba(255, 255, 255, 0.94);
  border-color: rgba(62, 141, 247, 0.62);
  box-shadow: 0 0 0 4px rgba(62, 141, 247, 0.14);
}

.input-shell i {
  color: #58708c;
  font-size: 1.08rem;
}

.input-shell input {
  background: transparent;
  border: 0;
  color: var(--ink);
  flex: 1;
  font: inherit;
  min-width: 0;
  outline: 0;
}

.login-submit {
  align-items: center;
  background: #3e8df7;
  border: 0;
  border-radius: 999px;
  color: #fff;
  display: inline-flex;
  font-weight: 750;
  gap: 10px;
  justify-content: center;
  margin-top: 6px;
  min-height: 56px;
  padding: 0 22px;
  transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
  width: 100%;
}

.login-submit:hover:not(:disabled),
.login-submit:focus-visible {
  background: #2f7de8;
  box-shadow: 0 18px 34px rgba(62, 141, 247, 0.24);
  transform: translateY(-1px);
}

.login-submit:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.register-link {
  border-top: 1px solid var(--line);
  color: var(--muted);
  margin: 26px 0 0;
  padding-top: 22px;
  text-align: center;
}

.register-link a {
  color: #2f6fc5;
  font-weight: 750;
  margin-left: 4px;
  text-decoration: none;
}

.register-link a:hover,
.brand-mark:hover,
.nav-back:hover {
  color: #0b5ed7;
}

[data-bs-theme='dark'] .login-sky {
  --ink: #ecf4ff;
  --muted: #a7b4c6;
  --glass: rgba(13, 20, 31, 0.74);
  --line: rgba(255, 255, 255, 0.12);
  background:
    radial-gradient(circle at 18% 24%, rgba(74, 105, 140, 0.6) 0 8%, transparent 22%),
    radial-gradient(circle at 78% 14%, rgba(46, 87, 129, 0.72) 0 18%, transparent 36%),
    linear-gradient(180deg, #101926 0%, #182434 48%, #2a2330 100%);
}

[data-bs-theme='dark'] .hero-copy h1,
[data-bs-theme='dark'] .panel-heading h2,
[data-bs-theme='dark'] .field-group label,
[data-bs-theme='dark'] .register-link a {
  color: var(--ink) !important;
}

[data-bs-theme='dark'] .hero-text,
[data-bs-theme='dark'] .panel-heading p,
[data-bs-theme='dark'] .register-link {
  color: var(--muted) !important;
}

[data-bs-theme='dark'] .brand-mark,
[data-bs-theme='dark'] .nav-back,
[data-bs-theme='dark'] .input-shell {
  background: rgba(16, 25, 38, 0.68);
  border-color: rgba(255, 255, 255, 0.14);
  color: var(--ink);
}

[data-bs-theme='dark'] .input-shell input {
  color: var(--ink);
}

@media (prefers-reduced-motion: reduce) {
  .login-submit,
  .input-shell {
    transition: none;
  }
}

@media (max-width: 900px) {
  .login-hero {
    gap: 34px;
    grid-template-columns: 1fr;
    padding-top: 34px;
  }

  .hero-copy {
    text-align: center;
  }

  .hero-text {
    margin-left: auto;
    margin-right: auto;
  }

  .login-panel-wrap {
    margin: 0 auto;
    max-width: 430px;
    width: 100%;
  }
}

@media (max-width: 560px) {
  .login-nav {
    padding-inline: 14px;
  }

  .brand-mark span:last-child {
    display: none;
  }

  .login-hero {
    padding: 30px 14px 52px;
  }

  .hero-copy h1 {
    font-size: 2.35rem;
  }

  .floating-plan {
    display: none;
  }

  .login-panel {
    padding: 24px;
  }
}
</style>
