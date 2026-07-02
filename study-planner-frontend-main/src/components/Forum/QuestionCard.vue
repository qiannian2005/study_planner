<template>
  <div class="question-card card mb-3">
    <div class="card-body">
      <div class="d-flex justify-content-between align-items-start">
        <div class="flex-grow-1">
          <h5 class="card-title mb-2">
            <router-link :to="`/forum/question/${question.id}`" class="text-decoration-none">
              {{ question.title || $t('forum.questionCard.untitled') }}
            </router-link>
          </h5>
          <p class="card-text text-muted mb-2" v-if="question.content">
            {{ truncateContent(question.content) }}
          </p>
          <div class="d-flex flex-wrap gap-2 mb-2" v-if="question.topics && question.topics.length > 0">
            <TopicTag 
              v-for="topic in question.topics" 
              :key="topic.id" 
              :topic="topic"
            />
          </div>
          <div class="d-flex align-items-center text-muted small">
            <span class="me-3">
              <template v-if="authorId">
                <router-link
                  :to="{ name: 'user-profile', params: { id: authorId } }"
                  class="question-meta-link"
                >
                  <i class="bi bi-person"></i>
                  {{ question.author?.username || $t('forum.questionCard.anonymousUser') }}
                </router-link>
              </template>
              <template v-else>
                <i class="bi bi-person"></i>
                {{ question.author?.username || $t('forum.questionCard.anonymousUser') }}
              </template>
            </span>
            <span class="me-3">
              <i class="bi bi-clock"></i>
              {{ formatTime(questionTime) }}
            </span>
            <span class="me-3">
              <router-link
                :to="{ name: 'question-detail', params: { id: question.id }, hash: '#answers' }"
                class="question-meta-link"
              >
                <i class="bi bi-chat-dots"></i>
                {{ question.answer_count || 0 }} {{ $t('forum.questionCard.answers') }}
              </router-link>
            </span>
            <span class="me-3">
              <i class="bi bi-eye"></i>
              {{ question.view_count || 0 }} {{ $t('forum.questionCard.views') }}
            </span>
            <span v-if="question.follow_count" class="me-3">
              <i class="bi bi-heart"></i>
              {{ question.follow_count }} {{ $t('forum.questionCard.follows') }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import TopicTag from './TopicTag.vue'
import { formatTime } from '../../utils/format'

const { t } = useI18n()

const props = defineProps({
  question: {
    type: Object,
    required: true
  }
})

defineEmits([])

const questionTime = computed(() => (
  props.question.created_at ||
  props.question.createdAt ||
  props.question.createTime ||
  props.question.create_time
))

const authorId = computed(() => (
  props.question.author?.id ||
  props.question.author_id ||
  props.question.user_id
))

function truncateContent(content) {
  if (!content) return ''
  // 移除 Markdown 标记
  const text = content.replace(/[#*`\[\]()]/g, '').trim()
  return text.length > 150 ? text.substring(0, 150) + '...' : text
}

</script>

<style scoped>
.question-card {
  transition: box-shadow 0.2s;
}

.question-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.card-title a {
  color: #172033;
  font-weight: 700;
}

.card-title a:hover {
  color: #1890ff;
}

.question-meta-link {
  color: inherit;
  text-decoration: none;
}

.question-meta-link:hover {
  color: #1890ff;
}

[data-bs-theme='dark'] .question-card .card-title a {
  color: #eef5ff;
}

[data-bs-theme='dark'] .question-card .card-title a:hover {
  color: #8dc5ff;
}

[data-bs-theme='dark'] .question-meta-link:hover {
  color: #8dc5ff;
}

[data-bs-theme='dark'] .question-card .card-text {
  color: #b8c4d6 !important;
}

</style>
