<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from './stores/user'

const userStore = useUserStore()
const route = useRoute()

onMounted(() => {
  if (route.meta.requiresAuth) {
    userStore.checkLoginStatus()
  }
})
</script>

<style>
/* Global styles live in assets/style.css */
</style>
