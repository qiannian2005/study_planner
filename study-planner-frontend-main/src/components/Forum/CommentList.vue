<template>
  <section class="bili-comments">
    <div v-if="comments.length === 0 && !loading" class="empty-comments">
      {{ $t('forum.comment.noComments') }}
    </div>

    <article v-for="comment in comments" :key="comment.id" class="comment-thread">
      <div class="main-comment" :class="{ selected: replyingTo?.id === comment.id }">
        <div class="avatar avatar-lg">
          <img v-if="comment.author?.avatar" :src="comment.author.avatar" alt="">
          <span v-else>{{ avatarText(comment.author?.username) }}</span>
        </div>

        <div class="comment-main">
          <div class="uname">{{ comment.author?.username || '匿名用户' }}</div>
          <div class="content">{{ comment.content }}</div>
          <div class="meta">
            <span>{{ formatTime(comment.created_at) }}</span>
            <button type="button" class="meta-action" :class="{ active: comment.is_voted }" @click="handleVote(comment)">
              <i class="bi" :class="comment.is_voted ? 'bi-hand-thumbs-up-fill' : 'bi-hand-thumbs-up'"></i>
              <span>{{ comment.vote_count || 0 }}</span>
            </button>
            <button type="button" class="meta-action" @click="handleReply(comment)">
              {{ replyingTo?.id === comment.id ? '正在回复' : '回复' }}
            </button>
          </div>

          <div v-if="comment.replies?.length" class="reply-block">
            <div
              v-for="reply in comment.replies"
              :key="reply.id"
              class="reply-item"
              :class="{ selected: replyingTo?.id === reply.id }"
            >
              <div class="avatar avatar-sm">
                <img v-if="reply.author?.avatar" :src="reply.author.avatar" alt="">
                <span v-else>{{ avatarText(reply.author?.username) }}</span>
              </div>

              <div class="reply-main">
                <div class="reply-content">
                  <span class="uname inline">{{ reply.author?.username || '匿名用户' }}</span>
                  <span v-if="reply.parent?.author?.username" class="reply-at">
                    回复 @{{ reply.parent.author.username }}
                  </span>
                  <span>{{ reply.content }}</span>
                </div>
                <div class="meta">
                  <span>{{ formatTime(reply.created_at) }}</span>
                  <button type="button" class="meta-action" :class="{ active: reply.is_voted }" @click="handleVote(reply)">
                    <i class="bi" :class="reply.is_voted ? 'bi-hand-thumbs-up-fill' : 'bi-hand-thumbs-up'"></i>
                    <span>{{ reply.vote_count || 0 }}</span>
                  </button>
                  <button type="button" class="meta-action" @click="handleReply(reply)">
                    {{ replyingTo?.id === reply.id ? '正在回复' : '回复' }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </article>

    <div class="composer">
      <div class="avatar avatar-lg self">
        <span>我</span>
      </div>
      <div class="composer-body">
        <div v-if="replyingTo" class="reply-tip">
          <span>回复 @{{ replyTargetName }}</span>
          <button type="button" @click="cancelReply">取消</button>
        </div>
        <div class="send-line">
          <input
            ref="commentInput"
            v-model="newComment"
            type="text"
            :placeholder="commentPlaceholder"
            @keyup.enter="submitComment"
          >
          <button type="button" :disabled="!newComment.trim()" @click="submitComment">
            发布
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { commentApi } from '../../api/forum'
import { formatTime } from '../../utils/format'
import { showToast } from '../../utils/toast'

const { t } = useI18n()

const props = defineProps({
  answerId: {
    type: [Number, String],
    required: true
  }
})

const emit = defineEmits(['comment-added'])

const comments = ref([])
const loading = ref(false)
const newComment = ref('')
const replyingTo = ref(null)
const voting = ref(null)
const commentInput = ref(null)

const replyTargetName = computed(() => replyingTo.value?.author?.username || '匿名用户')
const commentPlaceholder = computed(() => {
  return replyingTo.value ? `回复 @${replyTargetName.value}:` : '写下你的评论...'
})

onMounted(fetchComments)

async function fetchComments() {
  try {
    loading.value = true
    const response = await commentApi.getComments({ answer_id: props.answerId })
    if (response.code === 200) {
      comments.value = response.data || []
    }
  } catch (error) {
    console.error('Failed to load comments:', error)
  } finally {
    loading.value = false
  }
}

async function submitComment() {
  if (!newComment.value.trim()) return

  try {
    const response = await commentApi.createComment({
      answer_id: props.answerId,
      content: newComment.value,
      parent_id: replyingTo.value?.id || null
    })

    if (response.code === 200) {
      showToast(t('common.success'), 'success')
      newComment.value = ''
      replyingTo.value = null
      await fetchComments()
      emit('comment-added')
    } else {
      showToast(response.message || t('errors.unknown'), 'error')
    }
  } catch (error) {
    console.error('Failed to comment:', error)
    showToast(t('errors.unknown'), 'error')
  }
}

function handleReply(comment) {
  replyingTo.value = comment
  nextTick(() => commentInput.value?.focus())
}

function cancelReply() {
  replyingTo.value = null
  nextTick(() => commentInput.value?.focus())
}

async function handleVote(comment) {
  if (voting.value === comment.id) return

  try {
    voting.value = comment.id
    const response = await commentApi.voteComment(comment.id)
    if (response.code === 200 && response.data) {
      comment.vote_count = response.data.vote_count || comment.vote_count
      comment.is_voted = response.data.is_voted !== undefined ? response.data.is_voted : !comment.is_voted
    } else {
      showToast(response.message || t('errors.unknown'), 'error')
    }
  } catch (error) {
    console.error('Failed to vote:', error)
    showToast(t('errors.unknown'), 'error')
  } finally {
    voting.value = null
  }
}

function avatarText(username) {
  const text = String(username || '匿').trim()
  return text ? text.slice(0, 1).toUpperCase() : '匿'
}
</script>

<style scoped>
.bili-comments {
  background: #fff;
  color: #18191c;
  font-size: 14px;
}

.empty-comments {
  color: #9499a0;
  padding: 12px 0;
}

.comment-thread {
  border-bottom: 1px solid #e3e5e7;
  padding: 18px 0 14px;
}

.main-comment {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  column-gap: 12px;
  border-radius: 8px;
  padding: 0 2px;
  transition: background-color 0.18s ease;
}

.main-comment.selected,
.reply-item.selected {
  background: #f1f9ff;
}

.avatar {
  align-items: center;
  background: #f1f2f3;
  border-radius: 50%;
  color: #6d757a;
  display: flex;
  font-weight: 700;
  justify-content: center;
  overflow: hidden;
}

.avatar img {
  height: 100%;
  object-fit: cover;
  width: 100%;
}

.avatar-lg {
  height: 48px;
  width: 48px;
}

.avatar-sm {
  height: 32px;
  width: 32px;
  font-size: 12px;
}

.self {
  background: linear-gradient(135deg, #8ec5fc, #e0c3fc);
  color: #fff;
}

.comment-main,
.reply-main {
  min-width: 0;
}

.uname {
  color: #61666d;
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
}

.uname.inline {
  display: inline;
  margin-right: 8px;
}

.content {
  color: #18191c;
  font-size: 15px;
  line-height: 24px;
  margin-top: 4px;
  white-space: pre-wrap;
  word-break: break-word;
}

.meta {
  align-items: center;
  color: #9499a0;
  display: flex;
  gap: 18px;
  line-height: 18px;
  margin-top: 8px;
}

.meta-action {
  appearance: none;
  background: none;
  border: 0;
  color: #9499a0;
  cursor: pointer;
  display: inline-flex;
  gap: 5px;
  line-height: 18px;
  padding: 0;
}

.meta-action:hover,
.meta-action.active {
  color: #00aeec;
}

.reply-block {
  margin-top: 14px;
}

.reply-item {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr);
  column-gap: 10px;
  border-radius: 8px;
  padding: 9px 0;
}

.reply-content {
  color: #18191c;
  font-size: 14px;
  line-height: 22px;
  white-space: pre-wrap;
  word-break: break-word;
}

.reply-at {
  color: #008ac5;
  margin-right: 8px;
}

.composer {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  column-gap: 12px;
  padding-top: 18px;
}

.composer-body {
  min-width: 0;
}

.reply-tip {
  align-items: center;
  background: #f6fbff;
  border: 1px solid #b8e2f8;
  border-bottom: 0;
  border-radius: 6px 6px 0 0;
  color: #008ac5;
  display: flex;
  justify-content: space-between;
  padding: 7px 10px;
}

.reply-tip button {
  appearance: none;
  background: none;
  border: 0;
  color: #61666d;
  cursor: pointer;
  padding: 0;
}

.reply-tip button:hover {
  color: #00aeec;
}

.send-line {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 78px;
  gap: 10px;
}

.send-line input {
  border: 1px solid #c9ccd0;
  border-radius: 6px;
  color: #18191c;
  min-height: 46px;
  outline: none;
  padding: 0 12px;
}

.reply-tip + .send-line input {
  border-top-left-radius: 0;
}

.send-line input:focus {
  border-color: #00aeec;
}

.send-line button {
  appearance: none;
  background: #00aeec;
  border: 0;
  border-radius: 6px;
  color: #fff;
  cursor: pointer;
  font-weight: 600;
}

.reply-tip + .send-line button {
  border-top-right-radius: 0;
}

.send-line button:disabled {
  background: #80d7f5;
  cursor: not-allowed;
}
</style>
