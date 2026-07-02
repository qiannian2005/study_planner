<template>
  <button 
    class="btn btn-sm vote-button"
    :class="{
      'btn-outline-primary': !isVoted,
      'btn-primary': isVoted
    }"
    @click="handleVote"
    :disabled="loading"
  >
    <i class="bi" :class="isVoted ? 'bi-heart-fill' : 'bi-heart'"></i>
    <span class="ms-1">{{ voteCount }}</span>
  </button>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  voteCount: {
    type: Number,
    default: 0
  },
  isVoted: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['vote'])

const loading = ref(false)
const localVoteCount = ref(props.voteCount)
const localIsVoted = ref(props.isVoted)

// 监听 prop 变化，同步到本地状态
watch(() => props.voteCount, (newVal) => {
  localVoteCount.value = newVal
})

watch(() => props.isVoted, (newVal) => {
  localIsVoted.value = newVal
})

const voteCount = computed(() => localVoteCount.value)
const isVoted = computed(() => localIsVoted.value)

async function handleVote() {
  if (loading.value) return
  
  loading.value = true
  try {
    const result = await emit('vote')
    if (result && typeof result === 'object') {
      if (result.vote_count !== undefined) {
        localVoteCount.value = result.vote_count
      }
      if (result.is_voted !== undefined) {
        localIsVoted.value = result.is_voted
      }
    }
  } catch (error) {
    console.error('点赞操作失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.vote-button {
  transition: all 0.2s;
}

.vote-button:hover:not(:disabled) {
  transform: scale(1.05);
}
</style>

