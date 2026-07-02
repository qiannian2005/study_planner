<template>
  <div class="chat-page">
    <Navbar />

    <div class="chat-shell container my-4">
      <aside class="chat-sidebar">
        <div class="sidebar-card">
          <div class="sidebar-title">
            <h5 class="mb-1">
              <i class="bi bi-chat-dots"></i>
              {{ t('chat.studyRoom') }}
            </h5>
            <p class="mb-0 text-muted">{{ t('chat.roomIntro') }}</p>
          </div>

          <button
            class="room-item"
            :class="{ active: isGlobalRoom(currentRoom) }"
            @click="selectRoom(globalRoom)"
          >
            <span class="room-icon"><i class="bi bi-globe2"></i></span>
            <span class="room-copy">
              <strong>{{ t('chat.globalRoom') }}</strong>
              <small>{{ t('chat.globalRoomDesc') }}</small>
            </span>
          </button>

          <div class="room-section">
            <div class="room-section-title">
              <span>{{ t('chat.myGroups') }}</span>
              <button class="btn btn-sm btn-link px-0" @click="loadGroupPlans">
                {{ t('common.refresh') }}
              </button>
            </div>

            <div v-if="groupPlansLoading" class="text-muted small py-2">
              {{ t('chat.loadingGroupPlans') }}
            </div>

            <div v-else-if="groupPlans.length === 0" class="empty-rooms">
              <i class="bi bi-people"></i>
              <span>{{ t('chat.noGroups') }}</span>
            </div>

            <button
              v-for="plan in groupPlans"
              :key="plan.id"
              class="room-item room-item-plan"
              :class="{ active: isPlanRoom(currentRoom, plan.id) }"
              @click="selectPlanRoom(plan)"
            >
              <span class="room-icon"><i class="bi bi-people-fill"></i></span>
              <span class="room-copy">
                <strong>{{ plan.title }}</strong>
                <small>
                  {{ t('chat.groupMeta', { count: plan.member_count || 0, progress: formatProgress(plan.progress) }) }}
                </small>
              </span>
            </button>
          </div>
        </div>
      </aside>

      <section class="chat-main">
        <div class="chat-header">
          <div>
            <h4 class="mb-1">{{ roomTitle }}</h4>
            <p class="mb-0 text-muted">{{ roomSubtitle }}</p>
          </div>
          <div class="chat-header-actions">
            <span v-if="chatStore.isConnected" class="badge bg-success">
              <i class="bi bi-circle-fill"></i>
              {{ t('chat.onlineCount', { count: chatStore.onlineCount }) }}
            </span>
            <span v-else-if="chatStore.isLoading" class="badge bg-warning text-dark">
              <i class="bi bi-hourglass-split"></i>
              {{ t('chat.connecting') }}
            </span>
            <span v-else class="badge bg-danger">
              <i class="bi bi-x-circle"></i>
              {{ t('chat.disconnected') }}
            </span>

            <button class="btn btn-outline-secondary btn-sm" @click="showOnlineUsers = !showOnlineUsers">
              <i class="bi bi-people"></i>
              {{ t('chat.members') }}
            </button>
            <button
              v-if="canLeaveCurrentGroup"
              class="btn btn-outline-danger btn-sm"
              @click="openLeaveGroupDialog"
            >
              <i class="bi bi-box-arrow-left"></i>
              {{ t('chat.leaveGroup') }}
            </button>
          </div>
        </div>

        <div class="chat-body" :class="{ 'show-members': showOnlineUsers }">
          <div class="chat-messages" ref="messagesContainer">
            <div v-if="messagesLoading" class="state-block">
              <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
              <span>{{ t('chat.loadingMessages') }}</span>
            </div>

            <div v-else-if="messages.length === 0" class="state-block">
              <i class="bi bi-chat-square-text"></i>
              <span>{{ t('chat.noRoomMessages') }}</span>
            </div>

            <div
              v-for="message in messages"
              :key="message.id"
              class="message-item"
              :class="{ own: message.is_own }"
            >
              <button
                class="message-avatar"
                :class="{ clickable: canOpenUserProfile(message) }"
                type="button"
                :disabled="!canOpenUserProfile(message)"
                :title="canOpenUserProfile(message) ? t('chat.viewUserProfile', { username: message.username }) : ''"
                @click="openUserProfile(message)"
              >
                <img
                  v-if="message.avatar"
                :src="message.avatar"
                :alt="message.username"
                >
                <i v-else class="bi bi-person-circle"></i>
              </button>
              <div class="message-bubble">
                <div class="message-meta">
                  <strong>{{ message.username }}</strong>
                  <span>{{ chatStore.formatTime(message.created_at) }}</span>
                </div>
                <div class="message-text" v-html="formatMessageContent(message.content)"></div>
              </div>
            </div>
          </div>

          <aside v-if="showOnlineUsers" class="online-panel">
            <div class="online-panel-header">
              <strong>{{ isPlanRoom(currentRoom) ? t('chat.memberList') : t('chat.onlineMembers') }}</strong>
              <button class="btn btn-sm btn-link px-0" type="button" @click="showOnlineUsers = false">
                <i class="bi bi-x-lg"></i>
              </button>
            </div>

            <div v-if="membersLoading" class="state-block online-state">
              <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
              <span>{{ t('chat.loadingMembers') }}</span>
            </div>

            <div v-else-if="displayedMembers.length === 0" class="state-block online-state">
              <i class="bi bi-people"></i>
              <span>{{ isPlanRoom(currentRoom) ? t('chat.noMemberInfo') : t('chat.noOnlineMembers') }}</span>
            </div>

            <button
              v-for="member in displayedMembers"
              v-else
              :key="member.userId || member.id || member.username"
              class="online-user online-user-button"
              type="button"
              :disabled="!canOpenMemberProfile(member)"
              :title="canOpenMemberProfile(member) ? t('chat.viewUserProfile', { username: member.username }) : ''"
              @click="openMemberProfile(member)"
            >
              <span class="online-user-main">
                <img
                  v-if="member.avatar"
                  class="online-user-avatar"
                  :src="member.avatar"
                  :alt="member.username"
                >
                <i v-else class="bi bi-person-circle online-user-fallback"></i>
                <span class="online-user-copy">
                  <span>{{ member.username || t('chat.defaultUser') }}</span>
                  <small>{{ member.role ? t(`chat.${member.role}`) : t('chat.member') }}</small>
                </span>
              </span>
              <span class="member-status" :class="member.isOnline ? 'online' : 'offline'">
                {{ member.isOnline ? t('chat.online') : t('chat.offline') }}
              </span>
            </button>
          </aside>
        </div>

        <div class="chat-input">
          <textarea
            v-model="inputMessage"
            class="form-control"
            rows="2"
            :placeholder="t('chat.inputPlaceholder')"
            :disabled="!chatStore.isConnected"
            @keydown.enter.exact.prevent="handleSendMessage"
            @keydown.enter.shift.exact="inputMessage += '\n'"
          ></textarea>
          <div class="chat-input-actions">
            <button class="btn chat-tool-btn" :title="t('chat.scrollToBottom')" @click="scrollToBottom">
              <i class="bi bi-arrow-down-circle"></i>
            </button>
            <button
              class="btn chat-tool-btn chat-tool-btn-danger"
              :disabled="messages.length === 0"
              :title="t('chat.clearCurrentMessages')"
              @click="showClearConfirm = true"
            >
              <i class="bi bi-trash3"></i>
            </button>
            <button
              class="btn chat-send-btn"
              :disabled="!chatStore.isConnected || !inputMessage.trim()"
              @click="handleSendMessage"
            >
              <i class="bi bi-send"></i>
              {{ t('chat.send') }}
            </button>
          </div>
        </div>
      </section>
    </div>

    <div v-if="showClearConfirm" class="chat-modal-backdrop" @click.self="showClearConfirm = false">
      <div class="chat-confirm-dialog" role="dialog" aria-modal="true">
        <div class="confirm-icon">
          <i class="bi bi-trash3"></i>
        </div>
        <div class="confirm-content">
          <h5>{{ t('chat.clearCurrentMessages') }}</h5>
          <p>{{ t('chat.clearCurrentMessagesConfirm') }}</p>
        </div>
        <div class="confirm-actions">
          <button class="btn btn-outline-secondary" @click="showClearConfirm = false">
            {{ t('common.cancel') }}
          </button>
          <button class="btn btn-danger" @click="clearCurrentMessages">
            <i class="bi bi-trash3"></i>
            {{ t('chat.clear') }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="leaveGroupDialog.visible" class="chat-modal-backdrop" @click.self="closeLeaveGroupDialog">
      <div class="chat-confirm-dialog" role="dialog" aria-modal="true">
        <div class="confirm-icon leave-icon">
          <i class="bi bi-box-arrow-left"></i>
        </div>
        <div class="confirm-content">
          <h5>{{ t('chat.leaveCurrentGroup') }}</h5>
          <p>{{ t('chat.leaveGroupDesc') }}</p>
        </div>
        <div class="confirm-actions confirm-actions-stack">
          <button class="btn btn-outline-secondary" :disabled="leaveGroupDialog.loading" @click="closeLeaveGroupDialog">
            {{ t('common.cancel') }}
          </button>
          <button class="btn btn-outline-primary" :disabled="leaveGroupDialog.loading" @click="leaveGroup(false)">
            {{ t('chat.keepPlanAndLeave') }}
          </button>
          <button class="btn btn-danger" :disabled="leaveGroupDialog.loading" @click="leaveGroup(true)">
            <span v-if="leaveGroupDialog.loading" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
            <i v-else class="bi bi-trash3"></i>
            {{ t('chat.deletePlanAndLeave') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import Navbar from '../components/Navbar.vue'
import { chatApi } from '../api/chat'
import { planApi } from '../api/plan'
import { useChatStore } from '../stores/chat'
import { useUserStore } from '../stores/user'
import { showToast } from '../utils/toast'
import { wsManager } from '../utils/websocket'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const chatStore = useChatStore()
const userStore = useUserStore()

const globalRoom = Object.freeze({ roomType: 'global', roomId: null })

const inputMessage = ref('')
const messages = ref([])
const groupPlans = ref([])
const groupPlansLoading = ref(false)
const messagesLoading = ref(false)
const messagesContainer = ref(null)
const showOnlineUsers = ref(false)
const showClearConfirm = ref(false)
const currentRoom = ref({ ...globalRoom })
const isSending = ref(false)
const currentRoomMembers = ref([])
const membersLoading = ref(false)
const leaveGroupDialog = ref({
  visible: false,
  loading: false
})

const currentPlanSummary = computed(() => {
  if (!isPlanRoom(currentRoom.value)) {
    return null
  }
  return groupPlans.value.find((plan) => Number(plan.id) === Number(currentRoom.value.roomId)) || null
})

const canLeaveCurrentGroup = computed(() => {
  return Boolean(currentPlanSummary.value && currentPlanSummary.value.member_role !== 'owner')
})

const displayedMembers = computed(() => {
  const onlineUserIds = new Set(
    (chatStore.onlineUsers || [])
      .map(user => Number(user.userId ?? user.id))
      .filter(Number.isFinite)
  )

  if (!isPlanRoom(currentRoom.value)) {
    return (chatStore.onlineUsers || []).map(user => ({
      ...user,
      isOnline: true
    }))
  }

  return (currentRoomMembers.value || []).map(member => ({
    ...member,
    isOnline: onlineUserIds.has(Number(member.userId ?? member.id))
  }))
})

const roomTitle = computed(() => {
  if (isGlobalRoom(currentRoom.value)) {
    return t('chat.globalRoom')
  }
  return currentPlanSummary.value?.title || t('chat.groupStudyRoom')
})

const roomSubtitle = computed(() => {
  if (isGlobalRoom(currentRoom.value)) {
    return t('chat.globalRoomSubtitle')
  }
  return t('chat.groupRoomSubtitle')
})

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    showToast(t('chat.loginRequired'), 'warning')
    router.push('/login?redirect=/chat')
    return
  }

  chatStore.initConnection()
  wsManager.on('message', handleIncomingMessage)

  await Promise.all([loadGroupPlans(), loadCurrentRoomMessages(resolveRoomFromQuery())])
})

onUnmounted(() => {
  wsManager.off('message', handleIncomingMessage)
})

watch(
  () => [route.query.roomType, route.query.roomId],
  async () => {
    const targetRoom = resolveRoomFromQuery()
    if (!isSameRoom(targetRoom, currentRoom.value)) {
      await loadCurrentRoomMessages(targetRoom)
    }
  }
)

watch(
  () => messages.value.length,
  async () => {
    await nextTick()
    scrollToBottom()
  }
)

async function loadGroupPlans() {
  groupPlansLoading.value = true
  try {
    const result = await planApi.getMyGroupPlans()
    if (result?.code === 200) {
      groupPlans.value = Array.isArray(result.data) ? result.data : []
    }
  } catch (error) {
    console.error('加载小组计划失败:', error)
    showToast(t('chat.loadGroupPlansFailed'), 'error')
  } finally {
    groupPlansLoading.value = false
  }
}

function resolveRoomFromQuery() {
  const roomType = route.query.roomType === 'plan' ? 'plan' : 'global'
  const roomId = roomType === 'plan' ? Number(route.query.roomId) : null
  if (roomType === 'plan' && Number.isFinite(roomId) && roomId > 0) {
    return { roomType, roomId }
  }
  return { ...globalRoom }
}

async function loadCurrentRoomMessages(room) {
  currentRoom.value = { roomType: room.roomType, roomId: room.roomId ?? null }
  syncRouteQuery()
  await loadCurrentRoomMembers()

  messagesLoading.value = true
  try {
    const result = await chatApi.getMessages(1, 50, null, currentRoom.value)
    messages.value = result?.code === 200 ? (result.data?.list || []).map(normalizeMessage) : []
  } catch (error) {
    console.error('加载聊天记录失败:', error)
    messages.value = []
    showToast(t('chat.loadMessagesFailed'), 'error')
  } finally {
    messagesLoading.value = false
  }
}

async function loadCurrentRoomMembers() {
  if (!isPlanRoom(currentRoom.value)) {
    currentRoomMembers.value = []
    membersLoading.value = false
    return
  }

  membersLoading.value = true
  try {
    const result = await planApi.getGroupSummary(currentRoom.value.roomId)
    currentRoomMembers.value = Array.isArray(result?.data?.members) ? result.data.members : []
  } catch (error) {
    console.error('加载成员列表失败:', error)
    currentRoomMembers.value = []
    showToast(t('chat.loadMembersFailed'), 'error')
  } finally {
    membersLoading.value = false
  }
}

function normalizeMessage(message) {
  const currentUserId = Number(userStore.user?.id)
  const messageUserId = Number(message.user_id)
  const explicitOwn = message.is_own === true || message.is_own === 1 || message.is_own === '1'

  return {
    id: message.id || `${Date.now()}-${Math.random()}`,
    user_id: message.user_id,
    username: message.username || t('chat.defaultUser'),
    avatar: message.avatar || '',
    content: message.content || '',
    created_at: message.created_at || new Date().toISOString(),
    room_type: message.room_type || 'global',
    room_id: message.room_id ?? null,
    is_own: explicitOwn || (Number.isFinite(currentUserId) && Number.isFinite(messageUserId) && currentUserId === messageUserId)
  }
}

function handleIncomingMessage(message) {
  const normalized = normalizeMessage(message)
  if (!matchesRoom(normalized, currentRoom.value)) {
    return
  }
  if (messages.value.some((item) => item.id === normalized.id)) {
    return
  }
  messages.value.push(normalized)
}

function canOpenUserProfile(message) {
  const currentUserId = Number(userStore.user?.id)
  const targetUserId = Number(message?.user_id)
  return Number.isFinite(targetUserId) && targetUserId > 0 && targetUserId !== currentUserId
}

function openUserProfile(message) {
  if (canOpenUserProfile(message)) {
    router.push(`/forum/user/${message.user_id}`)
  }
}

function canOpenMemberProfile(member) {
  const currentUserId = Number(userStore.user?.id)
  const targetUserId = Number(member?.userId ?? member?.id)
  return Number.isFinite(targetUserId) && targetUserId > 0 && targetUserId !== currentUserId
}

function openMemberProfile(member) {
  if (canOpenMemberProfile(member)) {
    router.push(`/forum/user/${member.userId ?? member.id}`)
  }
}

function escapeHtml(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function formatMessageContent(content) {
  const escaped = escapeHtml(content)
  const withLinks = escaped.replace(
    /(https?:\/\/[^\s<]+)/g,
    (url) => `<a href="${url}" class="message-link" target="_self" rel="noopener noreferrer">${url}</a>`
  )

  return withLinks.replace(/\n/g, '<br>')
}

function selectRoom(room) {
  loadCurrentRoomMessages(room)
}

function selectPlanRoom(plan) {
  loadCurrentRoomMessages({ roomType: 'plan', roomId: Number(plan.id) })
}

function syncRouteQuery() {
  const nextQuery = isGlobalRoom(currentRoom.value)
    ? {}
    : { roomType: currentRoom.value.roomType, roomId: String(currentRoom.value.roomId) }

  const sameType = (route.query.roomType || 'global') === (nextQuery.roomType || 'global')
  const sameId = String(route.query.roomId || '') === String(nextQuery.roomId || '')
  if (sameType && sameId) {
    return
  }

  router.replace({
    path: '/chat',
    query: nextQuery
  })
}

function isGlobalRoom(room) {
  return room?.roomType !== 'plan'
}

function isPlanRoom(room, planId = null) {
  if (room?.roomType !== 'plan') {
    return false
  }
  return planId == null ? true : Number(room.roomId) === Number(planId)
}

function isSameRoom(a, b) {
  return a?.roomType === b?.roomType && Number(a?.roomId || 0) === Number(b?.roomId || 0)
}

function matchesRoom(message, room) {
  if (isGlobalRoom(room)) {
    return (message.room_type || 'global') === 'global'
  }
  return message.room_type === 'plan' && Number(message.room_id) === Number(room.roomId)
}

function formatProgress(value) {
  const amount = Number(value || 0)
  return Number.isFinite(amount) ? amount.toFixed(0) : '0'
}

function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

function clearCurrentMessages() {
  messages.value = []
  showClearConfirm.value = false
}

function openLeaveGroupDialog() {
  if (!canLeaveCurrentGroup.value) {
    return
  }
  leaveGroupDialog.value.visible = true
}

function closeLeaveGroupDialog() {
  if (leaveGroupDialog.value.loading) {
    return
  }
  leaveGroupDialog.value.visible = false
}

async function leaveGroup(deletePlan) {
  const summary = currentPlanSummary.value
  if (!summary) {
    return
  }

  const linkedPlanId = summary.linked_personal_plan_id || summary.linkedPersonalPlanId
  leaveGroupDialog.value.loading = true

  try {
    if (deletePlan && linkedPlanId) {
      const deleteResult = await planApi.deletePlan(linkedPlanId)
      if (deleteResult?.code !== 200) {
        showToast(deleteResult?.message || t('chat.deletePlanFailed'), 'error')
        leaveGroupDialog.value.loading = false
        return
      }
    }

    const result = await planApi.leaveGroupPlan(summary.id)
    if (result?.code === 200) {
      showToast(deletePlan ? t('chat.deletedPlanAndLeft') : t('chat.keptPlanAndLeft'), 'success')
      closeLeaveGroupDialog()
      await loadGroupPlans()
      await loadCurrentRoomMessages(globalRoom)
    } else {
      showToast(result?.message || t('chat.leaveGroupFailed'), 'error')
    }
  } catch (error) {
    console.error('退出小组失败:', error)
    showToast(t('chat.leaveGroupFailed'), 'error')
  } finally {
    leaveGroupDialog.value.loading = false
  }
}

function handleSendMessage() {
  const content = inputMessage.value.trim()
  if (!content || isSending.value) {
    return
  }

  isSending.value = true
  const success = wsManager.sendMessage(content, {
    roomType: currentRoom.value.roomType,
    roomId: currentRoom.value.roomId
  })

  if (success) {
    inputMessage.value = ''
  }

  window.setTimeout(() => {
    isSending.value = false
  }, 300)
}
</script>

<style scoped>
.chat-page {
  min-height: 100vh;
}

.chat-shell {
  align-items: start;
  display: grid;
  gap: 1rem;
  grid-template-columns: 300px minmax(0, 1fr);
}

.sidebar-card,
.chat-main {
  backdrop-filter: blur(14px);
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(255, 255, 255, 0.78);
  border-radius: 18px;
  box-shadow: 0 18px 40px rgba(43, 63, 92, 0.12);
}

.sidebar-card {
  padding: 1rem;
  position: sticky;
  top: 1rem;
}

.sidebar-title {
  margin-bottom: 1rem;
}

.sidebar-title h5 {
  align-items: center;
  display: flex;
  gap: 0.5rem;
}

.room-section {
  margin-top: 1rem;
}

.room-section-title {
  align-items: center;
  color: #5b6b7c;
  display: flex;
  font-size: 0.92rem;
  font-weight: 700;
  justify-content: space-between;
  margin-bottom: 0.6rem;
}

.room-item {
  align-items: flex-start;
  background: rgba(244, 248, 253, 0.9);
  border: 1px solid rgba(87, 116, 146, 0.12);
  border-radius: 12px;
  cursor: pointer;
  display: flex;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
  padding: 0.8rem;
  text-align: left;
  transition: all 0.2s ease;
  width: 100%;
}

.room-item:hover,
.room-item.active {
  background: rgba(62, 141, 247, 0.12);
  border-color: rgba(62, 141, 247, 0.28);
}

.room-icon {
  align-items: center;
  background: rgba(62, 141, 247, 0.14);
  border-radius: 10px;
  color: #2f6fc5;
  display: inline-flex;
  flex: 0 0 38px;
  height: 38px;
  justify-content: center;
}

.room-copy {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  min-width: 0;
}

.room-copy strong,
.room-copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.room-copy small,
.empty-rooms {
  color: #6b7785;
}

.empty-rooms {
  align-items: center;
  display: flex;
  gap: 0.5rem;
  padding: 0.75rem 0.25rem;
}

.chat-main {
  display: flex;
  flex-direction: column;
  height: clamp(640px, calc(100vh - 110px), 860px);
  min-height: 640px;
  overflow: hidden;
}

.chat-header {
  align-items: flex-start;
  border-bottom: 1px solid rgba(87, 116, 146, 0.12);
  display: flex;
  gap: 1rem;
  justify-content: space-between;
  padding: 1rem 1.2rem;
  flex: 0 0 auto;
}

.chat-header-actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.room-summary {
  border-bottom: 1px solid rgba(87, 116, 146, 0.1);
  display: grid;
  flex: 0 0 auto;
  gap: 0.9rem;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  padding: 1rem 1.2rem 1rem;
}

.summary-chip {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(242, 248, 255, 0.86));
  border: 1px solid rgba(92, 126, 168, 0.1);
  border-radius: 18px;
  box-shadow: 0 10px 22px rgba(44, 73, 112, 0.06);
  min-height: 92px;
  padding: 0.95rem 1rem 0.9rem;
  position: relative;
  overflow: hidden;
}

.summary-chip::before {
  content: "";
  position: absolute;
  left: 0;
  top: 14px;
  bottom: 14px;
  width: 4px;
  border-radius: 999px;
  background: linear-gradient(180deg, #7bc8ff, #4d9fff);
}

.summary-label {
  color: #6f8198;
  display: block;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  margin-bottom: 0.45rem;
  padding-left: 0.35rem;
}

.summary-chip strong {
  color: #1f365b;
  display: block;
  font-size: 1.55rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  line-height: 1.1;
  padding-left: 0.35rem;
  word-break: break-word;
}

.summary-chip:nth-child(2)::before {
  background: linear-gradient(180deg, #63d59a, #2dbf7a);
}

.summary-chip:nth-child(3)::before {
  background: linear-gradient(180deg, #ffca6d, #f2a63b);
}

.summary-chip:nth-child(4)::before {
  background: linear-gradient(180deg, #7fbcff, #5c8dff);
}

.chat-body {
  align-items: stretch;
  display: grid;
  flex: 1;
  gap: 0;
  grid-template-columns: minmax(0, 1fr);
  min-height: 0;
  overflow: hidden;
}

.chat-body.show-members {
  grid-template-columns: minmax(0, 1fr) minmax(220px, 280px);
}

.chat-messages {
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 1.35rem 1.2rem 1.5rem;
  scrollbar-gutter: stable;
}

.chat-messages::-webkit-scrollbar,
.online-panel::-webkit-scrollbar {
  width: 10px;
}

.chat-messages::-webkit-scrollbar-thumb,
.online-panel::-webkit-scrollbar-thumb {
  background: rgba(112, 140, 176, 0.35);
  border: 2px solid transparent;
  border-radius: 999px;
  background-clip: padding-box;
}

.chat-messages::-webkit-scrollbar-track,
.online-panel::-webkit-scrollbar-track {
  background: transparent;
}

.state-block {
  align-items: center;
  color: #6b7785;
  display: flex;
  gap: 0.65rem;
  justify-content: center;
  min-height: 220px;
}

.message-item {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.message-item.own {
  flex-direction: row-reverse;
}

.message-avatar {
  align-items: center;
  background: transparent;
  border: 0;
  color: #7a8ca7;
  display: inline-flex;
  font-size: 2rem;
  height: 40px;
  justify-content: center;
  padding: 0;
  width: 40px;
}

.message-avatar img {
  border-radius: 50%;
  height: 40px;
  object-fit: cover;
  width: 40px;
}

.message-avatar.clickable {
  cursor: pointer;
}

.message-bubble {
  background: rgba(244, 248, 253, 0.95);
  border-radius: 16px;
  max-width: min(75%, 720px);
  padding: 0.85rem 1rem;
}

.message-item.own .message-bubble {
  background: rgba(52, 152, 219, 0.14);
}

.message-meta {
  align-items: center;
  color: #6b7785;
  display: flex;
  font-size: 0.82rem;
  gap: 0.75rem;
  margin-bottom: 0.35rem;
}

.message-text {
  color: #24364d;
  line-height: 1.7;
  word-break: break-word;
}

.online-panel {
  background: rgba(248, 250, 253, 0.88);
  border-left: 1px solid rgba(87, 116, 146, 0.12);
  min-height: 0;
  overscroll-behavior: contain;
  overflow-y: auto;
  padding: 1rem;
}

.online-panel-header {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.8rem;
}

.online-user {
  align-items: center;
  background: transparent;
  border: 0;
  color: inherit;
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.9rem;
  padding: 0;
  text-align: left;
  width: 100%;
}

.online-user-button:not(:disabled) {
  cursor: pointer;
}

.online-user-button:not(:disabled):hover .online-user-copy span {
  color: #2f6fc5;
}

.online-user-main {
  align-items: center;
  display: flex;
  gap: 0.65rem;
  min-width: 0;
}

.online-user-avatar {
  border-radius: 50%;
  height: 34px;
  object-fit: cover;
  width: 34px;
}

.online-user-fallback {
  color: #8194ac;
  font-size: 1.9rem;
}

.online-user-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.online-user-copy span,
.online-user-copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-status {
  border-radius: 999px;
  font-size: 0.75rem;
  padding: 0.2rem 0.55rem;
}

.member-status.online {
  background: rgba(46, 204, 113, 0.14);
  color: #198754;
}

.member-status.offline {
  background: rgba(108, 117, 125, 0.14);
  color: #6c757d;
}

.online-state {
  min-height: 140px;
}

.chat-input {
  align-items: flex-end;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(247, 251, 255, 0.98));
  border-top: 1px solid rgba(87, 116, 146, 0.12);
  flex: 0 0 auto;
  display: flex;
  gap: 0.85rem;
  padding: 1rem 1.2rem 1.2rem;
}

.chat-input .form-control {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(118, 145, 177, 0.22);
  border-radius: 18px;
  box-shadow: inset 0 1px 2px rgba(45, 67, 102, 0.04);
  min-height: 104px;
  padding: 0.95rem 1rem;
  resize: none;
}

.chat-input-actions {
  align-items: center;
  display: flex;
  flex-direction: row;
  gap: 0.5rem;
  justify-content: flex-end;
  padding-bottom: 0.1rem;
}

.chat-tool-btn,
.chat-send-btn {
  align-items: center;
  border: 1px solid rgba(88, 123, 161, 0.18);
  display: inline-flex;
  justify-content: center;
  transition: all 0.2s ease;
}

.chat-tool-btn {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 249, 255, 0.96));
  border-radius: 16px;
  color: #4e6a8f;
  flex: 0 0 52px;
  height: 52px;
  width: 52px;
  box-shadow: 0 8px 18px rgba(70, 103, 146, 0.08);
}

.chat-tool-btn:hover:not(:disabled) {
  background: linear-gradient(180deg, rgba(245, 250, 255, 1), rgba(236, 245, 255, 0.98));
  border-color: rgba(66, 126, 214, 0.28);
  color: #2d64b3;
  transform: translateY(-1px);
}

.chat-tool-btn-danger:hover:not(:disabled) {
  border-color: rgba(220, 53, 69, 0.25);
  color: #c53a50;
}

.chat-send-btn {
  background: linear-gradient(180deg, #7fd0ff, #5aaeff);
  border-color: rgba(73, 140, 236, 0.3);
  border-radius: 18px;
  box-shadow: 0 12px 24px rgba(74, 144, 226, 0.2);
  color: #fff;
  flex: 0 0 116px;
  font-size: 1.15rem;
  font-weight: 700;
  gap: 0.45rem;
  height: 52px;
  min-width: 116px;
  padding: 0 1rem;
  writing-mode: horizontal-tb;
}

.chat-send-btn:hover:not(:disabled) {
  box-shadow: 0 12px 22px rgba(74, 144, 226, 0.26);
  transform: translateY(-1px);
}

.chat-send-btn:disabled,
.chat-tool-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.chat-modal-backdrop {
  align-items: center;
  background: rgba(18, 28, 45, 0.4);
  display: flex;
  inset: 0;
  justify-content: center;
  padding: 1rem;
  position: fixed;
  z-index: 1050;
}

.chat-confirm-dialog {
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 24px 60px rgba(27, 40, 63, 0.18);
  max-width: 440px;
  padding: 1.4rem;
  width: 100%;
}

.confirm-icon {
  align-items: center;
  background: rgba(220, 53, 69, 0.12);
  border-radius: 14px;
  color: #dc3545;
  display: inline-flex;
  font-size: 1.35rem;
  height: 48px;
  justify-content: center;
  width: 48px;
}

.leave-icon {
  background: rgba(255, 193, 7, 0.14);
  color: #b78103;
}

.confirm-content {
  margin: 1rem 0;
}

.confirm-content h5 {
  margin-bottom: 0.45rem;
}

.confirm-content p {
  color: #607088;
  line-height: 1.7;
  margin: 0;
}

.confirm-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}

.confirm-actions-stack {
  flex-direction: column;
}

.confirm-actions-stack .btn {
  width: 100%;
}

:deep(.message-link) {
  color: #0d6efd;
  text-decoration: underline;
}

:global([data-bs-theme='dark']) .chat-page {
  background:
    radial-gradient(circle at 12% 8%, rgba(74, 144, 226, 0.18), transparent 28%),
    radial-gradient(circle at 86% 12%, rgba(117, 212, 161, 0.12), transparent 24%),
    #0b1220;
}

:global([data-bs-theme='dark']) .chat-page .sidebar-card,
:global([data-bs-theme='dark']) .chat-page .chat-main {
  background: rgba(10, 20, 35, 0.86);
  border-color: rgba(188, 213, 241, 0.14);
  box-shadow: 0 22px 56px rgba(0, 0, 0, 0.36);
}

:global([data-bs-theme='dark']) .chat-page .chat-header,
:global([data-bs-theme='dark']) .chat-page .room-summary,
:global([data-bs-theme='dark']) .chat-page .chat-input {
  border-color: rgba(188, 213, 241, 0.12);
}

:global([data-bs-theme='dark']) .chat-page .room-section-title,
:global([data-bs-theme='dark']) .chat-page .room-copy small,
:global([data-bs-theme='dark']) .chat-page .empty-rooms,
:global([data-bs-theme='dark']) .chat-page .state-block,
:global([data-bs-theme='dark']) .chat-page .message-meta,
:global([data-bs-theme='dark']) .chat-page .summary-label,
:global([data-bs-theme='dark']) .chat-page .online-user-copy small,
:global([data-bs-theme='dark']) .chat-page .confirm-content p {
  color: #aebdd1 !important;
}

:global([data-bs-theme='dark']) .chat-page .room-copy strong,
:global([data-bs-theme='dark']) .chat-page .message-text,
:global([data-bs-theme='dark']) .chat-page .message-meta strong,
:global([data-bs-theme='dark']) .chat-page .summary-chip strong,
:global([data-bs-theme='dark']) .chat-page .online-panel-header,
:global([data-bs-theme='dark']) .chat-page .online-user-copy span,
:global([data-bs-theme='dark']) .chat-page .confirm-content h5 {
  color: #edf5ff !important;
}

:global([data-bs-theme='dark']) .chat-page .room-item,
:global([data-bs-theme='dark']) .chat-page .summary-chip,
:global([data-bs-theme='dark']) .chat-page .message-bubble,
:global([data-bs-theme='dark']) .chat-page .online-panel,
:global([data-bs-theme='dark']) .chat-page .chat-confirm-dialog {
  background: rgba(15, 24, 38, 0.88);
  border-color: rgba(188, 213, 241, 0.14);
  color: #edf5ff;
}

:global([data-bs-theme='dark']) .chat-page .room-item:hover,
:global([data-bs-theme='dark']) .chat-page .room-item.active {
  background: rgba(62, 141, 247, 0.2);
  border-color: rgba(141, 197, 255, 0.36);
}

:global([data-bs-theme='dark']) .chat-page .room-icon,
:global([data-bs-theme='dark']) .chat-page .message-avatar,
:global([data-bs-theme='dark']) .chat-page .online-user-fallback {
  color: #8dc5ff;
}

:global([data-bs-theme='dark']) .chat-page .room-icon {
  background: rgba(141, 197, 255, 0.12);
  border: 1px solid rgba(141, 197, 255, 0.2);
}

:global([data-bs-theme='dark']) .chat-page .chat-messages {
  background:
    linear-gradient(180deg, rgba(7, 15, 27, 0.34), rgba(7, 15, 27, 0.12));
}

:global([data-bs-theme='dark']) .chat-page .message-item.own .message-bubble {
  background: linear-gradient(135deg, rgba(77, 166, 255, 0.92), rgba(63, 137, 226, 0.92));
  border-color: rgba(141, 197, 255, 0.46);
}

:global([data-bs-theme='dark']) .chat-page .message-item.own .message-text,
:global([data-bs-theme='dark']) .chat-page .message-item.own .message-meta,
:global([data-bs-theme='dark']) .chat-page .message-item.own .message-meta strong {
  color: #061b36 !important;
}

:global([data-bs-theme='dark']) .chat-page .online-panel {
  border-left-color: rgba(188, 213, 241, 0.12);
}

:global([data-bs-theme='dark']) .chat-page .member-status.online {
  background: rgba(117, 212, 161, 0.16);
  color: #75d4a1;
}

:global([data-bs-theme='dark']) .chat-page .member-status.offline {
  background: rgba(188, 213, 241, 0.1);
  color: #9aa8b6;
}

:global([data-bs-theme='dark']) .chat-page .chat-input {
  background: linear-gradient(180deg, rgba(10, 20, 35, 0.94), rgba(7, 15, 27, 0.98));
}

:global([data-bs-theme='dark']) .chat-page .chat-input .form-control {
  background: rgba(7, 15, 27, 0.82) !important;
  border-color: rgba(188, 213, 241, 0.18) !important;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.18);
  color: #edf5ff !important;
}

:global([data-bs-theme='dark']) .chat-page .chat-tool-btn {
  background: rgba(237, 245, 255, 0.08);
  border-color: rgba(188, 213, 241, 0.18);
  color: #d9ecff;
  box-shadow: none;
}

:global([data-bs-theme='dark']) .chat-page .chat-tool-btn:hover:not(:disabled) {
  background: rgba(237, 245, 255, 0.14);
  border-color: rgba(188, 213, 241, 0.32);
  color: #ffffff;
}

:global([data-bs-theme='dark']) .chat-page .chat-modal-backdrop {
  background: rgba(2, 8, 17, 0.68);
}

:global([data-bs-theme='dark']) .chat-page .message-link {
  color: #bfe1ff;
}

:global([data-bs-theme='dark'] .chat-page .sidebar-card),
:global([data-bs-theme='dark'] .chat-page .chat-main),
:global([data-bs-theme='dark'] .chat-page .room-item),
:global([data-bs-theme='dark'] .chat-page .summary-chip),
:global([data-bs-theme='dark'] .chat-page .message-bubble),
:global([data-bs-theme='dark'] .chat-page .online-panel),
:global([data-bs-theme='dark'] .chat-page .chat-confirm-dialog) {
  background: rgba(10, 20, 35, 0.94) !important;
  border-color: rgba(188, 213, 241, 0.14) !important;
  color: #edf5ff !important;
}

:global([data-bs-theme='dark'] .chat-page .chat-input) {
  background: linear-gradient(180deg, rgba(9, 18, 31, 0.98), rgba(6, 13, 24, 0.99)) !important;
  border-top-color: rgba(188, 213, 241, 0.14) !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04) !important;
}

:global([data-bs-theme='dark'] .chat-page .chat-input .form-control) {
  background: rgba(5, 12, 22, 0.96) !important;
  border-color: rgba(188, 213, 241, 0.2) !important;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.28) !important;
  color: #edf5ff !important;
}

:global([data-bs-theme='dark'] .chat-page .chat-input .form-control::placeholder) {
  color: #8494aa !important;
}

:global([data-bs-theme='dark'] .chat-page .chat-tool-btn) {
  background: rgba(237, 245, 255, 0.08) !important;
  border-color: rgba(188, 213, 241, 0.2) !important;
  box-shadow: none !important;
  color: #d9ecff !important;
}

:global([data-bs-theme='dark'] .chat-page .chat-send-btn) {
  background: linear-gradient(180deg, #7fd2ff, #55b7ff) !important;
  border-color: rgba(141, 197, 255, 0.52) !important;
  color: #061b36 !important;
}

:global([data-bs-theme='dark'] .chat-page .btn-outline-secondary) {
  background: rgba(237, 245, 255, 0.04) !important;
  border-color: rgba(188, 213, 241, 0.22) !important;
  color: #dbeaff !important;
}

:global([data-bs-theme='dark'] .chat-page .chat-confirm-dialog .btn-outline-secondary) {
  background: rgba(237, 245, 255, 0.08) !important;
}

@media (max-width: 991px) {
  .chat-shell {
    grid-template-columns: 1fr;
  }

  .sidebar-card {
    position: static;
  }

  .chat-main {
    height: auto;
    min-height: 0;
  }

  .chat-body {
    grid-template-columns: 1fr;
  }

  .online-panel {
    border-left: 0;
    border-top: 1px solid rgba(87, 116, 146, 0.12);
  }
}

@media (max-width: 767px) {
  .chat-header,
  .chat-input {
    flex-direction: column;
  }

  .chat-header-actions,
  .chat-input-actions {
    width: 100%;
  }

  .chat-input-actions .btn {
    flex: 1;
  }

  .chat-input-actions {
    width: 100%;
  }

  .chat-tool-btn {
    flex: 0 0 48px;
    height: 48px;
    width: 48px;
  }

  .chat-send-btn {
    flex: 1;
    height: 48px;
    min-height: 48px;
    min-width: 0;
  }

  .room-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .message-bubble {
    max-width: 100%;
  }
}
</style>
