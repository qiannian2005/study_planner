<template>
  <router-link
    :to="`/forum/topic/${topic.id}`"
    class="badge bg-primary text-decoration-none"
    style="cursor: pointer;"
  >
    {{ displayTopicName(topic, locale) }}: {{ topic.follow_count || 0 }} {{ $t('forum.topic.followers') }}
  </router-link>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { displayTopicName } from '../../utils/forumTopic'

const { locale } = useI18n()

defineProps({
  topic: {
    type: Object,
    required: true
  }
})
</script>

<style scoped>
.badge {
  font-weight: normal;
  padding: 0.35em 0.65em;
}
</style>
