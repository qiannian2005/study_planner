<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from './stores/user'

const userStore = useUserStore()

onMounted(() => {
  userStore.checkLoginStatus()
})
</script>

<style>
/* Global styles live in assets/style.css */
</style>
