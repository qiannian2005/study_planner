<template>
  <nav class="navbar navbar-expand-lg app-navbar">
    <div class="container">
      <router-link class="navbar-brand" to="/">
        <i class="bi bi-book"></i> {{ $t('nav.title') }}
      </router-link>
      <button
        class="navbar-toggler"
        type="button"
        data-bs-toggle="collapse"
        data-bs-target="#navbarNav"
        :aria-label="$t('nav.toggleMenu')"
      >
        <span class="navbar-toggler-icon"></span>
      </button>
      <div class="collapse navbar-collapse" id="navbarNav">
        <ul class="navbar-nav me-auto">
          <li class="nav-item">
            <router-link class="nav-link" to="/forum">{{ $t('nav.forum') }}</router-link>
          </li>
          <li v-if="isLoggedIn" class="nav-item">
            <router-link class="nav-link" to="/dashboard">{{ $t('nav.dashboard') }}</router-link>
          </li>
          <li v-if="isLoggedIn" class="nav-item">
            <router-link class="nav-link" to="/create-plan">{{ $t('nav.createPlan') }}</router-link>
          </li>
          <li v-if="isLoggedIn" class="nav-item">
            <router-link class="nav-link" to="/my-plans">{{ $t('nav.myPlans') }}</router-link>
          </li>
          <li v-if="isLoggedIn" class="nav-item">
            <router-link class="nav-link" to="/ai-assistant">{{ $t('nav.aiAssistant') }}</router-link>
          </li>
          <li v-if="isLoggedIn" class="nav-item">
            <router-link class="nav-link" to="/chat">{{ $t('nav.chatRoom') }}</router-link>
          </li>
        </ul>
        <ul class="navbar-nav">
          <li class="nav-item me-2">
            <button
              type="button"
              class="btn btn-sm theme-toggle"
              :title="isDarkTheme ? $t('nav.lightMode') : $t('nav.darkMode')"
              :aria-label="isDarkTheme ? $t('nav.lightMode') : $t('nav.darkMode')"
              @click="handleThemeToggle"
            >
              <i class="bi" :class="isDarkTheme ? 'bi-sun-fill' : 'bi-moon-stars-fill'"></i>
            </button>
          </li>
          <template v-if="isLoggedIn">
            <li class="nav-item dropdown" ref="dropdownRef" style="position: relative; z-index: 1050;">
              <a
                class="nav-link dropdown-toggle d-flex align-items-center"
                href="#"
                id="userDropdown"
                role="button"
                @click.prevent="toggleDropdown"
                :class="{ show: isDropdownOpen }"
                aria-expanded="false"
              >
                <img 
                  v-if="user?.avatar" 
                  :src="user.avatar" 
                  class="rounded-circle me-2" 
                  style="width: 24px; height: 24px; object-fit: cover;"
                  alt="Avatar"
                >
                <i v-else class="bi bi-person-circle me-1"></i> 
                {{ user?.username }}
              </a>
              <ul class="dropdown-menu dropdown-menu-end" :class="{ show: isDropdownOpen }">
                <li>
                  <router-link class="dropdown-item" to="/profile" @click="isDropdownOpen = false">
                    <i class="bi bi-person"></i> {{ $t('nav.profile') }}
                  </router-link>
                </li>
                <li>
                  <router-link class="dropdown-item" to="/forum/my-content">
                    <i class="bi bi-file-text"></i> {{ $t('forum.myContent.title') }}
                  </router-link>
                </li>
                <li v-if="isAdmin">
                  <router-link class="dropdown-item" to="/admin" @click="isDropdownOpen = false">
                    <i class="bi bi-shield-lock"></i> {{ $t('admin.title') }}
                  </router-link>
                </li>
                <li><hr class="dropdown-divider" /></li>
                <li>
                  <a class="dropdown-item" href="#" @click.prevent="handleLogout">
                    <i class="bi bi-box-arrow-right"></i> {{ $t('nav.logout') }}
                  </a>
                </li>
              </ul>
            </li>
          </template>
          <template v-else>
            <li class="nav-item">
              <router-link class="nav-link" to="/login">{{ $t('nav.login') }}</router-link>
            </li>
            <li class="nav-item">
              <router-link class="nav-link" to="/register">{{ $t('nav.register') }}</router-link>
            </li>
          </template>
          <li class="nav-item ms-2">
            <LanguageSwitcher />
          </li>
        </ul>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { applyTheme, getStoredTheme, toggleTheme } from '../utils/theme'
import LanguageSwitcher from './LanguageSwitcher.vue'

const router = useRouter()
const userStore = useUserStore()

const isLoggedIn = computed(() => userStore.isLoggedIn)
const user = computed(() => userStore.user)
const isAdmin = computed(() => user.value?.role === 'admin')

const isDropdownOpen = ref(false)
const dropdownRef = ref(null)
const theme = ref(getStoredTheme())
const isDarkTheme = computed(() => theme.value === 'dark')

function toggleDropdown() {
  isDropdownOpen.value = !isDropdownOpen.value
}

function closeDropdown(e) {
  if (dropdownRef.value && !dropdownRef.value.contains(e.target)) {
    isDropdownOpen.value = false
  }
}

function handleThemeToggle() {
  theme.value = applyTheme(toggleTheme(theme.value), true)
}

onMounted(() => {
  document.addEventListener('click', closeDropdown)
})

onUnmounted(() => {
  document.removeEventListener('click', closeDropdown)
})

async function handleLogout() {
  isDropdownOpen.value = false
  await userStore.logout()
  router.push('/')
}
</script>

<style scoped>
.app-navbar {
  background: transparent !important;
  padding: 12px 0 8px;
  position: relative;
  z-index: 10;
}

.app-navbar :deep(.container) {
  backdrop-filter: blur(18px) saturate(132%);
  background: rgba(255, 255, 255, 0.66);
  border: 1px solid rgba(87, 150, 231, 0.2);
  border-radius: 999px;
  box-shadow: 0 12px 28px rgba(45, 84, 132, 0.1);
  min-height: 54px;
  padding: 6px 12px;
}

.navbar-brand {
  align-items: center;
  color: #172033 !important;
  display: inline-flex;
  font-size: 1.08rem;
  font-style: normal;
  font-weight: 800;
  gap: 8px;
  letter-spacing: 0;
  line-height: 1;
  margin-right: 18px;
  white-space: nowrap;
}

.navbar-brand i {
  align-items: center;
  background: rgba(47, 111, 197, 0.12);
  border: 1px solid rgba(47, 111, 197, 0.24);
  border-radius: 50%;
  color: #2f6fc5;
  display: inline-flex;
  font-size: 0.94rem;
  height: 28px;
  justify-content: center;
  width: 28px;
}

.nav-link {
  align-items: center;
  border-radius: 999px;
  color: #42526b !important;
  display: inline-flex;
  font-size: 0.96rem;
  font-weight: 720;
  gap: 5px;
  line-height: 1;
  margin: 0 2px;
  min-height: 38px;
  padding: 7px 10px !important;
  transition: background-color 0.18s ease, border-color 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
  white-space: nowrap;
}

.nav-link:hover,
.nav-link.router-link-active,
.dropdown-toggle.show {
  background: rgba(47, 111, 197, 0.1);
  box-shadow: inset 0 0 0 1px rgba(47, 111, 197, 0.16);
  color: #2f6fc5 !important;
}

.navbar-toggler {
  border-color: rgba(17, 24, 39, 0.18);
}

.navbar-toggler:focus {
  box-shadow: 0 0 0 0.18rem rgba(62, 141, 247, 0.18);
}

.theme-toggle {
  align-items: center;
  background: rgba(47, 111, 197, 0.12);
  border-color: rgba(47, 111, 197, 0.24);
  border-radius: 999px;
  box-shadow: 0 8px 18px rgba(47, 111, 197, 0.12);
  color: #2f6fc5;
  display: inline-flex;
  height: 38px;
  justify-content: center;
  min-width: 42px;
}

.theme-toggle:hover,
.theme-toggle:focus {
  background: rgba(47, 111, 197, 0.18);
  border-color: rgba(47, 111, 197, 0.36);
  color: #245fae;
}

.dropdown-menu {
  margin-top: 12px;
}

@media (max-width: 991.98px) {
  .app-navbar :deep(.container) {
    border-radius: 8px;
  }

  .navbar-collapse {
    padding-top: 12px;
  }
}

[data-bs-theme='dark'] .app-navbar :deep(.container) {
  background: rgba(8, 21, 39, 0.8);
  border-color: rgba(141, 197, 255, 0.18);
  box-shadow: 0 14px 34px rgba(0, 0, 0, 0.22), inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

[data-bs-theme='dark'] .navbar-brand {
  color: #ecf4ff !important;
}

[data-bs-theme='dark'] .navbar-brand i {
  background: rgba(74, 154, 255, 0.16);
  border-color: rgba(141, 197, 255, 0.28);
  color: #8dc5ff;
}

[data-bs-theme='dark'] .nav-link {
  color: #d9e9fb !important;
}

[data-bs-theme='dark'] .nav-link:hover,
[data-bs-theme='dark'] .nav-link.router-link-active,
[data-bs-theme='dark'] .dropdown-toggle.show {
  background: rgba(141, 197, 255, 0.14);
  box-shadow: inset 0 0 0 1px rgba(141, 197, 255, 0.18);
  color: #f5fbff !important;
}

[data-bs-theme='dark'] .theme-toggle {
  background: rgba(141, 197, 255, 0.14);
  border-color: rgba(141, 197, 255, 0.26);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.18);
  color: #d9ecff;
}

[data-bs-theme='dark'] .theme-toggle:hover,
[data-bs-theme='dark'] .theme-toggle:focus {
  background: rgba(141, 197, 255, 0.2);
  border-color: rgba(141, 197, 255, 0.36);
  color: #ffffff;
}
</style>

