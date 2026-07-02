<template>
  <div class="profile-page app-shell d-flex flex-column min-vh-100">
    <Navbar />

    <main class="container page-surface my-5 flex-grow-1">
      <section class="profile-hero">
        <div class="hero-copy">
          <p class="page-kicker">Personal hub</p>
          <h1 class="page-title">
            <i class="bi bi-person-badge"></i>
            {{ $t('profile.title') }}
          </h1>
          <p class="hero-subtitle">
            {{ $t('profile.subtitle') }}
          </p>
        </div>
      </section>

      <AvatarCropper
        :show="showCropper"
        :imageSrc="cropperImageSrc"
        @confirm="handleCropperConfirm"
        @cancel="showCropper = false"
      />

      <div class="profile-layout">
        <aside class="surface-panel profile-summary">
          <div class="summary-top">
            <div class="avatar-wrap">
              <div
                v-if="!user?.avatar"
                class="avatar-fallback"
              >
                <i class="bi bi-person-circle"></i>
              </div>
              <img
                v-else
                :src="user.avatar"
                class="avatar-image"
                alt="User Avatar"
              >

              <label
                for="avatar-upload"
                class="avatar-upload-button"
                :title="$t('profile.changeAvatarHint')"
              >
                <i class="bi bi-camera-fill"></i>
              </label>
              <input
                id="avatar-upload"
                type="file"
                class="d-none"
                accept="image/*"
                @change="handleAvatarSelect"
              >
            </div>

            <h2>{{ user?.username || '-' }}</h2>
            <p class="summary-email">{{ profileData.email || user?.email || $t('profile.emailNotFilled') }}</p>
            <p class="summary-hint">{{ $t('profile.changeAvatarHint') }}</p>
          </div>

          <div class="summary-stats">
            <div class="summary-stat">
              <span>{{ $t('profile.completeness') }}</span>
              <strong>{{ profileCompletion }}%</strong>
            </div>
            <div class="summary-stat">
              <span>{{ $t('profile.registerTime') }}</span>
              <strong>{{ registerTimeText }}</strong>
            </div>
          </div>

          <div class="info-list">
            <div class="info-item">
              <span>{{ $t('profile.username') }}</span>
              <strong>{{ user?.username || '-' }}</strong>
            </div>
            <div class="info-item">
              <span>{{ $t('profile.email') }}</span>
              <strong>{{ profileData.email || $t('profile.notFilled') }}</strong>
            </div>
          </div>
        </aside>

        <div class="content-stack">
          <section class="surface-panel section-panel">
            <div class="panel-heading">
              <div class="panel-icon">
                <i class="bi bi-person-lines-fill"></i>
              </div>
              <div>
                <h2>{{ $t('profile.basicInfo') }}</h2>
                <p>{{ $t('profile.basicInfoDesc') }}</p>
              </div>
            </div>

            <div class="form-grid">
              <div class="field-block">
                <label>{{ $t('profile.username') }}</label>
                <input type="text" class="form-control" :value="user?.username" disabled>
              </div>

              <div class="field-block">
                <label>{{ $t('profile.email') }}</label>
                <input v-model="profileData.email" type="email" class="form-control">
              </div>

              <div class="field-block field-block-full">
                <label>{{ $t('profile.registerTime') }}</label>
                <input type="text" class="form-control" :value="registerTimeText" disabled>
              </div>
            </div>

            <div class="actions-row">
              <button class="btn btn-primary action-button" @click="updateProfile" :disabled="loading">
                <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
                <i v-else class="bi bi-check2-circle"></i>
                {{ $t('profile.saveChanges') }}
              </button>
            </div>
          </section>

          <section class="surface-panel section-panel">
            <div class="panel-heading">
              <div class="panel-icon warning">
                <i class="bi bi-shield-lock"></i>
              </div>
              <div>
                <h2>{{ $t('profile.changePassword') }}</h2>
                <p>{{ $t('profile.passwordDesc') }}</p>
              </div>
            </div>

            <form class="form-grid" @submit.prevent="changePassword">
              <div class="field-block">
                <label>{{ $t('profile.oldPassword') }}</label>
                <input
                  v-model="passwordData.oldPassword"
                  type="password"
                  class="form-control"
                  required
                >
              </div>

              <div class="field-block">
                <label>{{ $t('profile.newPassword') }}</label>
                <input
                  v-model="passwordData.newPassword"
                  type="password"
                  class="form-control"
                  minlength="6"
                  required
                >
              </div>

              <div class="field-block field-block-full">
                <label>{{ $t('profile.confirmPassword') }}</label>
                <input
                  v-model="passwordData.confirmPassword"
                  type="password"
                  class="form-control"
                  required
                >
              </div>

              <div class="actions-row">
                <button type="submit" class="btn btn-warning action-button warning" :disabled="passwordLoading">
                  <span v-if="passwordLoading" class="spinner-border spinner-border-sm me-2"></span>
                  <i v-else class="bi bi-key"></i>
                  {{ $t('profile.changePassword') }}
                </button>
              </div>
            </form>
          </section>
        </div>
      </div>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import Navbar from '../components/Navbar.vue'
import Footer from '../components/Footer.vue'
import AvatarCropper from '../components/AvatarCropper.vue'
import { useUserStore } from '../stores/user'
import { userApi } from '../api/user'
import { showToast } from '../utils/toast'
import { formatDateTime } from '../utils/format'

const { t } = useI18n()
const userStore = useUserStore()
const user = computed(() => userStore.user)

const loading = ref(false)
const passwordLoading = ref(false)
const showCropper = ref(false)
const cropperImageSrc = ref('')

const profileData = reactive({
  email: ''
})

const passwordData = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const registerTimeText = computed(() => {
  return user.value?.createTime ? formatDateTime(user.value.createTime) : '-'
})

const profileCompletion = computed(() => {
  let score = 40

  if (user.value?.username) {
    score += 20
  }
  if (profileData.email || user.value?.email) {
    score += 20
  }
  if (user.value?.avatar) {
    score += 20
  }

  return score
})

onMounted(() => {
  if (user.value) {
    profileData.email = user.value.email || ''
  }
})

function compressImage(file, maxWidth = 2000, maxHeight = 2000, quality = 0.8) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        let width = img.width
        let height = img.height

        if (width > maxWidth || height > maxHeight) {
          if (width > height) {
            height = (height * maxWidth) / width
            width = maxWidth
          } else {
            width = (width * maxHeight) / height
            height = maxHeight
          }
        }

        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        ctx.drawImage(img, 0, 0, width, height)

        canvas.toBlob((blob) => {
          if (blob) {
            resolve(blob)
          } else {
            reject(new Error(t('profile.imageCompressionFailed')))
          }
        }, file.type || 'image/jpeg', quality)
      }
      img.onerror = () => reject(new Error(t('profile.imageLoadFailed')))
      img.src = e.target.result
    }
    reader.onerror = () => reject(new Error(t('profile.fileReadFailed')))
    reader.readAsDataURL(file)
  })
}

async function handleAvatarSelect(event) {
  const file = event.target.files[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    showToast(t('profile.pleaseSelectImage'), 'error')
    event.target.value = ''
    return
  }

  const maxSize = 5 * 1024 * 1024
  if (file.size > maxSize) {
    showToast(t('profile.imageSizeExceeded'), 'error')
    event.target.value = ''
    return
  }

  try {
    showToast(t('profile.processingImage'), 'info')

    let processedFile = file
    if (file.size > 1024 * 1024) {
      const compressedBlob = await compressImage(file, 2000, 2000, 0.85)
      processedFile = new File([compressedBlob], file.name, { type: compressedBlob.type })
    }

    const reader = new FileReader()
    reader.onload = (e) => {
      cropperImageSrc.value = e.target.result
      showCropper.value = true
      showToast(t('profile.adjustCropArea'), 'info')
    }
    reader.onerror = () => showToast(t('profile.imageReadFailed'), 'error')
    reader.readAsDataURL(processedFile)
  } catch (error) {
    console.error('处理图片失败:', error)
    showToast(`${t('profile.imageProcessFailed')}: ${error.message}`, 'error')
  } finally {
    event.target.value = ''
  }
}

async function handleCropperConfirm(blob) {
  if (!blob) {
    showToast(t('profile.cropFailed'), 'error')
    return
  }

  showCropper.value = false
  showToast(t('profile.uploadingAvatar'), 'info')

  const formData = new FormData()
  formData.append('file', blob, `avatar_${Date.now()}.jpg`)

  try {
    const result = await userApi.uploadAvatar(formData)
    if (result && result.code === 200) {
      showToast(t('profile.avatarUploadSuccess'), 'success')

      if (result.data) {
        if (result.data.avatar) {
          userStore.user = { ...userStore.user, avatar: result.data.avatar }
        } else if (result.data.id) {
          userStore.user = result.data
        }
        localStorage.setItem('user', JSON.stringify(userStore.user))
      }

      await userStore.checkLoginStatus()
    } else {
      showToast(result?.message || t('profile.uploadFailed'), 'error')
    }
  } catch (error) {
    console.error('上传头像失败:', error)
    const errorMessage = error.response?.data?.message || error.message || t('profile.uploadFailed')
    showToast(errorMessage, 'error')
  }
}

async function updateProfile() {
  loading.value = true

  try {
    const result = await userApi.updateProfile(profileData)
    if (result && result.code === 200) {
      showToast(t('profile.profileUpdateSuccess'), 'success')
      await userStore.checkLoginStatus()
      profileData.email = userStore.user?.email || profileData.email
    } else {
      showToast(result?.message || t('profile.updateFailed'), 'error')
    }
  } catch (error) {
    console.error('更新资料失败:', error)
    showToast(t('profile.updateFailed'), 'error')
  } finally {
    loading.value = false
  }
}

async function changePassword() {
  if (passwordData.newPassword !== passwordData.confirmPassword) {
    showToast(t('errors.passwordMismatch'), 'error')
    return
  }

  passwordLoading.value = true

  try {
    const result = await userApi.changePassword(
      passwordData.oldPassword,
      passwordData.newPassword
    )

    if (result && result.code === 200) {
      showToast(t('profile.passwordChangeSuccess'), 'success')
      passwordData.oldPassword = ''
      passwordData.newPassword = ''
      passwordData.confirmPassword = ''

      setTimeout(() => {
        userStore.logout()
        window.location.href = '/login'
      }, 1500)
    } else {
      showToast(result?.message || t('profile.passwordChangeFailed'), 'error')
    }
  } catch (error) {
    console.error('修改密码失败:', error)
    showToast(t('profile.passwordChangeFailed'), 'error')
  } finally {
    passwordLoading.value = false
  }
}
</script>

<style scoped>
.profile-hero {
  align-items: end;
  display: flex;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}

.page-kicker {
  color: #5b83b1;
  font-size: 0.88rem;
  font-weight: 700;
  letter-spacing: 0;
  margin-bottom: 0.5rem;
  text-transform: uppercase;
}

.page-title {
  align-items: center;
  color: #203456;
  display: flex;
  font-size: clamp(2rem, 4vw, 2.7rem);
  font-weight: 800;
  gap: 0.75rem;
  margin: 0;
}

.hero-subtitle {
  color: #68809b;
  line-height: 1.75;
  margin: 0.85rem 0 0;
  max-width: 720px;
}

.profile-layout {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: minmax(280px, 340px) minmax(0, 1fr);
}

.surface-panel {
  backdrop-filter: blur(14px);
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(93, 126, 168, 0.14);
  border-radius: 20px;
  box-shadow: 0 16px 34px rgba(42, 69, 110, 0.1);
}

.profile-summary,
.section-panel {
  padding: 1.4rem;
}

.profile-summary {
  align-self: start;
  position: sticky;
  top: 96px;
}

.summary-top {
  text-align: center;
}

.avatar-wrap {
  display: inline-flex;
  margin-bottom: 1rem;
  position: relative;
}

.avatar-fallback,
.avatar-image {
  background: linear-gradient(135deg, rgba(79, 153, 255, 0.16), rgba(141, 198, 255, 0.24));
  border: 4px solid rgba(255, 255, 255, 0.92);
  border-radius: 50%;
  box-shadow: 0 18px 30px rgba(64, 122, 196, 0.18);
  height: 128px;
  object-fit: cover;
  width: 128px;
}

.avatar-fallback {
  align-items: center;
  color: #5b7ea8;
  display: flex;
  font-size: 4.6rem;
  justify-content: center;
}

.avatar-upload-button {
  align-items: center;
  background: linear-gradient(135deg, #2f9ff2, #2d79da);
  border: 3px solid rgba(255, 255, 255, 0.95);
  border-radius: 50%;
  bottom: 2px;
  box-shadow: 0 8px 20px rgba(47, 112, 188, 0.26);
  color: #fff;
  cursor: pointer;
  display: inline-flex;
  height: 40px;
  justify-content: center;
  position: absolute;
  right: 2px;
  width: 40px;
}

.summary-top h2 {
  color: #1d3152;
  font-size: 1.5rem;
  font-weight: 800;
  margin: 0;
}

.summary-email {
  color: #5d7694;
  margin: 0.45rem 0 0;
  word-break: break-all;
}

.summary-hint {
  color: #7c8da4;
  font-size: 0.9rem;
  line-height: 1.6;
  margin: 1rem 0 0;
}

.summary-stats {
  display: grid;
  gap: 0.85rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 1.3rem;
}

.summary-stat,
.info-item {
  background: rgba(244, 248, 255, 0.88);
  border: 1px solid rgba(93, 126, 168, 0.1);
  border-radius: 16px;
  padding: 0.95rem 1rem;
}

.summary-stat span,
.info-item span,
.field-block label {
  color: #71839a;
  display: block;
  font-size: 0.88rem;
  font-weight: 700;
  margin-bottom: 0.4rem;
}

.summary-stat strong,
.info-item strong {
  color: #1d3152;
  display: block;
  font-size: 1.05rem;
  font-weight: 800;
  word-break: break-word;
}

.info-list {
  display: grid;
  gap: 0.85rem;
  margin-top: 0.85rem;
}

.content-stack {
  display: grid;
  gap: 1.5rem;
}

.panel-heading {
  align-items: flex-start;
  display: flex;
  gap: 0.9rem;
  margin-bottom: 1.2rem;
}

.panel-heading h2 {
  color: #203456;
  font-size: 1.2rem;
  font-weight: 800;
  margin: 0 0 0.35rem;
}

.panel-heading p {
  color: #6f7f95;
  line-height: 1.7;
  margin: 0;
}

.panel-icon {
  align-items: center;
  background: linear-gradient(135deg, rgba(51, 153, 243, 0.16), rgba(92, 193, 255, 0.16));
  border-radius: 14px;
  color: #2c78c8;
  display: inline-flex;
  flex: 0 0 44px;
  font-size: 1.15rem;
  height: 44px;
  justify-content: center;
}

.panel-icon.warning {
  background: linear-gradient(135deg, rgba(255, 193, 7, 0.18), rgba(255, 157, 0, 0.16));
  color: #a86c00;
}

.form-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field-block-full,
.actions-row {
  grid-column: 1 / -1;
}

.form-control {
  background: rgba(248, 251, 255, 0.96);
  border: 1px solid rgba(93, 126, 168, 0.16);
  border-radius: 14px;
  min-height: 48px;
  padding: 0.75rem 0.95rem;
}

.form-control:focus {
  background: #fff;
  border-color: rgba(47, 159, 242, 0.55);
  box-shadow: 0 0 0 0.2rem rgba(47, 159, 242, 0.12);
}

.actions-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.25rem;
}

.action-button {
  align-items: center;
  border-radius: 14px;
  display: inline-flex;
  gap: 0.45rem;
  min-height: 46px;
  padding: 0 1.1rem;
}

.action-button.warning {
  color: #4e3500;
}

:global([data-bs-theme='dark']) .page-title,
:global([data-bs-theme='dark']) .summary-top h2,
:global([data-bs-theme='dark']) .panel-heading h2,
:global([data-bs-theme='dark']) .summary-stat strong,
:global([data-bs-theme='dark']) .info-item strong {
  color: #edf5ff;
}

:global([data-bs-theme='dark']) .hero-subtitle,
:global([data-bs-theme='dark']) .summary-email,
:global([data-bs-theme='dark']) .summary-hint,
:global([data-bs-theme='dark']) .panel-heading p {
  color: #aebdd1;
}

:global([data-bs-theme='dark']) .summary-stat,
:global([data-bs-theme='dark']) .info-item {
  background: rgba(20, 31, 48, 0.86);
  border-color: rgba(188, 213, 241, 0.16);
}

:global([data-bs-theme='dark']) .summary-stat span,
:global([data-bs-theme='dark']) .info-item span,
:global([data-bs-theme='dark']) .field-block label {
  color: #9fb3cc;
}

:global([data-bs-theme='dark']) .avatar-fallback,
:global([data-bs-theme='dark']) .avatar-image {
  border-color: rgba(188, 213, 241, 0.2);
}

@media (max-width: 991.98px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }

  .profile-summary {
    position: static;
  }
}

@media (max-width: 767.98px) {
  .page-title {
    font-size: 2rem;
  }

  .summary-stats,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .section-panel,
  .profile-summary {
    padding: 1.1rem;
  }

  .actions-row {
    justify-content: stretch;
  }

  .action-button {
    justify-content: center;
    width: 100%;
  }
}
</style>
