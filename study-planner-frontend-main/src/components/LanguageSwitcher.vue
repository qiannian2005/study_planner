<template>
  <div class="language-switcher">
    <button
      type="button"
      class="language-toggle"
      @click="toggleLanguage"
      :title="currentLocale === 'zh-CN' ? 'Switch to English' : '切换到中文'"
    >
      {{ currentLocale === 'zh-CN' ? 'EN' : '中文' }}
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { locale } = useI18n()

const currentLocale = computed(() => locale.value)

function toggleLanguage() {
  const newLocale = currentLocale.value === 'zh-CN' ? 'en-US' : 'zh-CN'
  locale.value = newLocale
  localStorage.setItem('locale', newLocale)
}
</script>

<style scoped>
.language-switcher {
  display: inline-block;
}

.language-toggle {
  align-items: center;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(47, 111, 197, 0.22);
  border-radius: 999px;
  color: #2f4f7f;
  display: inline-flex;
  font-size: 0.82rem;
  font-weight: 800;
  height: 38px;
  justify-content: center;
  min-width: 48px;
  padding: 0 12px;
  transition: background-color 0.18s ease, border-color 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}

.language-toggle:hover,
.language-toggle:focus {
  background: rgba(47, 111, 197, 0.1);
  border-color: rgba(47, 111, 197, 0.34);
  box-shadow: 0 0 0 0.18rem rgba(62, 141, 247, 0.14);
  color: #1e5cab;
}

[data-bs-theme='dark'] .language-toggle {
  background: rgba(141, 197, 255, 0.1);
  border-color: rgba(141, 197, 255, 0.24);
  color: #d9ecff;
}

[data-bs-theme='dark'] .language-toggle:hover,
[data-bs-theme='dark'] .language-toggle:focus {
  background: rgba(141, 197, 255, 0.18);
  border-color: rgba(141, 197, 255, 0.38);
  color: #ffffff;
}
</style>
