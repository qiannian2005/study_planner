<template>
  <footer class="app-footer py-4 mt-5">
    <div class="container text-center">
      <p class="mb-0">{{ $t('footer.copyright') }}</p>
    </div>
  </footer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<style scoped>
.app-footer {
  background: transparent;
  color: #667085;
}

.app-footer .container {
  padding-top: 22px;
}

[data-bs-theme='dark'] .app-footer {
  color: #a7b4c6;
}

</style>
