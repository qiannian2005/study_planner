<template>
  <div class="answer-card card mb-3">
    <div class="card-body">
      <div class="d-flex align-items-start mb-3">
        <div class="flex-grow-1">
          <div class="d-flex align-items-center mb-2">
            <img 
              v-if="answer.author?.avatar" 
              :src="answer.author.avatar" 
              class="rounded-circle me-2"
              style="width: 32px; height: 32px;"
              alt="avatar"
            >
            <div>
              <router-link 
                :to="`/forum/user/${answer.author?.id}`"
                class="text-decoration-none fw-bold"
              >
                {{ answer.author?.username || '匿名用户' }}
              </router-link>
              <span class="text-muted small ms-2">
                {{ formatTime(answer.created_at) }}
              </span>
            </div>
          </div>
        </div>
        <div v-if="isAuthor" class="dropdown">
          <button class="btn btn-sm btn-outline-secondary" type="button" data-bs-toggle="dropdown">
            <i class="bi bi-three-dots"></i>
          </button>
          <ul class="dropdown-menu">
            <li><a class="dropdown-item" href="#" @click.prevent="handleEdit">编辑</a></li>
            <li><a class="dropdown-item text-danger" href="#" @click.prevent="handleDelete">删除</a></li>
          </ul>
        </div>
      </div>
      
      <div class="answer-content mb-3" v-html="renderedContent"></div>
      
      <div class="d-flex align-items-center gap-2">
        <VoteButton 
          :vote-count="answer.vote_count || 0"
          :is-voted="answer.is_voted || false"
          @vote="handleVote"
        />
        <button 
          class="btn btn-sm btn-outline-secondary"
          @click="toggleComments"
          :title="$t('forum.answer.comments')"
        >
          <i class="bi bi-chat-dots"></i>
          <span class="ms-1">{{ answer.comment_count || 0 }}</span>
        </button>
        <button 
          class="btn btn-sm"
          :class="answer.is_collected ? 'btn-warning' : 'btn-outline-warning'"
          @click="handleCollect"
          :title="$t('forum.answer.collect')"
        >
          <i class="bi" :class="answer.is_collected ? 'bi-star-fill' : 'bi-star'"></i>
        </button>
      </div>
      
      <div v-if="showComments" class="forum-comment-panel mt-3">
        <CommentList 
          :answer-id="answer.id"
          @comment-added="handleCommentAdded"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { renderMarkdown } from '../../utils/markdown'
import { useUserStore } from '../../stores/user'
import { formatTime } from '../../utils/format'
import VoteButton from './VoteButton.vue'
import CommentList from './CommentList.vue'

const { t } = useI18n()

const props = defineProps({
  answer: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['vote', 'collect', 'edit', 'delete', 'comment-added'])

const userStore = useUserStore()
const showComments = ref(false)

const isAuthor = computed(() => {
  return userStore.user && userStore.user.id === props.answer.author_id
})

const renderedContent = computed(() => {
  if (!props.answer.content) return ''
  return renderMarkdown(props.answer.content)
})

function toggleComments() {
  showComments.value = !showComments.value
}

async function handleVote() {
  return await emit('vote', props.answer.id)
}

function handleCollect() {
  emit('collect', props.answer.id)
}

function handleEdit() {
  emit('edit', props.answer)
}

function handleDelete() {
  if (confirm(t('forum.answer.confirmDelete'))) {
    emit('delete', props.answer.id)
  }
}

function handleCommentAdded() {
  emit('comment-added')
}
</script>

<style scoped>
.answer-card {
  border-left: 3px solid #1890ff;
}

.answer-content {
  line-height: 1.8;
}

.answer-content :deep(h1),
.answer-content :deep(h2),
.answer-content :deep(h3) {
  margin-top: 1.5em;
  margin-bottom: 0.5em;
}

.answer-content :deep(code) {
  background-color: #f5f5f5;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 0.9em;
}

.answer-content :deep(pre) {
  background-color: #f5f5f5;
  padding: 1em;
  border-radius: 4px;
  overflow-x: auto;
}

.answer-content :deep(blockquote) {
  border-left: 4px solid #ddd;
  padding-left: 1em;
  margin-left: 0;
  color: #666;
}

.forum-comment-panel {
  border-top: 1px solid #e3e5e7;
  margin-left: -0.25rem;
  margin-right: -0.25rem;
  padding: 14px 0.25rem 0;
}
</style>
