<template>
  <div v-if="show" class="avatar-cropper-modal" @click.self="cancel">
    <div class="avatar-cropper-container">
      <div class="avatar-cropper-header">
        <h5 class="mb-0">{{ $t('profile.cropAvatar') }}</h5>
        <button type="button" class="btn-close" @click="cancel" :aria-label="$t('common.close')">
          <i class="bi bi-x-lg"></i>
        </button>
      </div>

      <div class="avatar-cropper-body">
        <div class="cropper-workspace">
          <div ref="cropperHost" class="cropper-main-area">
            <img
              ref="imageRef"
              class="cropper-source-image"
              :src="imageSrc"
              alt="Avatar"
              @error="onImageError"
            />
          </div>

          <div class="cropper-preview-section">
            <div class="preview-label">{{ $t('common.preview') }}</div>
            <div class="preview-box">
              <canvas ref="previewCanvas" class="preview-canvas"></canvas>
            </div>
          </div>
        </div>

        <div class="cropper-controls">
          <div class="zoom-control">
            <button
              class="btn btn-sm btn-outline-secondary"
              type="button"
              :title="$t('profile.zoomOut')"
              @click="zoomOut"
            >
              <i class="bi bi-dash-lg"></i>
            </button>
            <button
              class="btn btn-sm btn-outline-secondary"
              type="button"
              :title="$t('profile.zoomIn')"
              @click="zoomIn"
            >
              <i class="bi bi-plus-lg"></i>
            </button>
            <button class="btn btn-sm btn-outline-secondary" type="button" @click="resetCropper">
              <i class="bi bi-arrow-counterclockwise"></i>
              {{ $t('common.retry') }}
            </button>
          </div>

          <div class="control-buttons">
            <button class="btn btn-secondary" type="button" @click="cancel">{{ $t('common.cancel') }}</button>
            <button class="btn btn-primary" type="button" :disabled="confirming" @click="confirm">
              {{ confirming ? $t('common.submitting') : $t('profile.confirmCrop') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onUnmounted, ref, watch } from 'vue'
import Cropper from 'cropperjs'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  imageSrc: {
    type: String,
    required: true
  }
})

const emit = defineEmits(['confirm', 'cancel'])

const imageRef = ref(null)
const cropperHost = ref(null)
const previewCanvas = ref(null)
const confirming = ref(false)

let cropper = null
let previewTimer = null

const cropperTemplate = `
  <cropper-canvas background>
    <cropper-image rotatable scalable skewable translatable></cropper-image>
    <cropper-shade hidden></cropper-shade>
    <cropper-handle action="select" plain></cropper-handle>
    <cropper-selection
      id="avatar-cropper-selection"
      initial-coverage="0.72"
      aspect-ratio="1"
      movable
      resizable
      zoomable
      outlined
    >
      <cropper-grid covered></cropper-grid>
      <cropper-crosshair centered></cropper-crosshair>
      <cropper-handle action="move" theme-color="transparent"></cropper-handle>
      <cropper-handle action="n-resize"></cropper-handle>
      <cropper-handle action="e-resize"></cropper-handle>
      <cropper-handle action="s-resize"></cropper-handle>
      <cropper-handle action="w-resize"></cropper-handle>
      <cropper-handle action="ne-resize"></cropper-handle>
      <cropper-handle action="nw-resize"></cropper-handle>
      <cropper-handle action="se-resize"></cropper-handle>
      <cropper-handle action="sw-resize"></cropper-handle>
    </cropper-selection>
  </cropper-canvas>
`

watch(
  () => props.show,
  (visible) => {
    if (visible) {
      nextTick(initCropper)
    } else {
      cleanup()
    }
  }
)

watch(
  () => props.imageSrc,
  () => {
    if (props.show) {
      nextTick(initCropper)
    }
  }
)

async function initCropper() {
  cleanup()
  if (!imageRef.value || !cropperHost.value || !props.imageSrc) return

  confirming.value = false
  cropper = new Cropper(imageRef.value, {
    container: cropperHost.value,
    template: cropperTemplate
  })

  const cropperImage = cropper.getCropperImage()
  if (cropperImage) {
    await cropperImage.$ready()
    cropperImage.$center('contain')
  }

  bindCropperEvents()
  schedulePreviewUpdate()
}

function bindCropperEvents() {
  const canvas = cropper?.getCropperCanvas()
  if (!canvas) return

  canvas.addEventListener('change', schedulePreviewUpdate)
  canvas.addEventListener('actionend', schedulePreviewUpdate)
  canvas.addEventListener('wheel', schedulePreviewUpdate)
}

function unbindCropperEvents() {
  const canvas = cropper?.getCropperCanvas()
  if (!canvas) return

  canvas.removeEventListener('change', schedulePreviewUpdate)
  canvas.removeEventListener('actionend', schedulePreviewUpdate)
  canvas.removeEventListener('wheel', schedulePreviewUpdate)
}

function schedulePreviewUpdate() {
  if (previewTimer) {
    window.clearTimeout(previewTimer)
  }
  previewTimer = window.setTimeout(updatePreview, 60)
}

async function updatePreview() {
  const selection = cropper?.getCropperSelection()
  if (!selection || !previewCanvas.value) return

  const croppedCanvas = await selection.$toCanvas({
    width: 220,
    height: 220
  })
  const canvas = previewCanvas.value
  const context = canvas.getContext('2d')

  canvas.width = 220
  canvas.height = 220
  context.clearRect(0, 0, canvas.width, canvas.height)
  context.drawImage(croppedCanvas, 0, 0, canvas.width, canvas.height)
}

function zoomIn() {
  cropper?.getCropperImage()?.$zoom(0.1)
  schedulePreviewUpdate()
}

function zoomOut() {
  cropper?.getCropperImage()?.$zoom(-0.1)
  schedulePreviewUpdate()
}

async function resetCropper() {
  const image = cropper?.getCropperImage()
  const selection = cropper?.getCropperSelection()

  image?.$resetTransform()
  image?.$center('contain')
  selection?.$reset()
  schedulePreviewUpdate()
}

async function confirm() {
  const selection = cropper?.getCropperSelection()
  if (!selection || confirming.value) return

  confirming.value = true
  try {
    const canvas = await selection.$toCanvas({
      width: 320,
      height: 320
    })

    canvas.toBlob((blob) => {
      confirming.value = false
      if (!blob) {
        emit('cancel')
        return
      }
      emit('confirm', blob)
      cleanup()
    }, 'image/jpeg', 0.92)
  } catch (error) {
    confirming.value = false
    console.error('Avatar crop failed:', error)
    emit('cancel')
  }
}

function cancel() {
  cleanup()
  emit('cancel')
}

function onImageError() {
  console.error('Avatar image failed to load')
  cancel()
}

function cleanup() {
  if (previewTimer) {
    window.clearTimeout(previewTimer)
    previewTimer = null
  }
  unbindCropperEvents()
  cropper?.destroy()
  cropper = null
  confirming.value = false
}

onUnmounted(cleanup)
</script>

<style scoped>
.avatar-cropper-modal {
  align-items: center;
  background: rgba(15, 23, 42, 0.58);
  bottom: 0;
  display: flex;
  justify-content: center;
  left: 0;
  padding: 20px;
  position: fixed;
  right: 0;
  top: 0;
  z-index: 2000;
}

.avatar-cropper-container {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.28);
  display: flex;
  flex-direction: column;
  max-height: 92vh;
  max-width: 880px;
  overflow: hidden;
  width: 100%;
}

.avatar-cropper-header {
  align-items: center;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  padding: 16px 20px;
}

.avatar-cropper-header h5 {
  color: #172033;
  font-size: 16px;
  font-weight: 800;
}

.btn-close {
  align-items: center;
  background: transparent;
  border: 0;
  color: #64748b;
  display: inline-flex;
  font-size: 18px;
  justify-content: center;
  min-height: 36px;
  min-width: 36px;
}

.btn-close:hover {
  color: #0f172a;
}

.avatar-cropper-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
  overflow: auto;
  padding: 20px;
}

.cropper-workspace {
  display: grid;
  gap: 20px;
  grid-template-columns: minmax(0, 1fr) 220px;
}

.cropper-main-area {
  background: #0f172a;
  border-radius: 8px;
  height: min(56vh, 500px);
  min-height: 340px;
  overflow: hidden;
  position: relative;
}

.cropper-source-image {
  display: block;
  max-height: 100%;
  max-width: 100%;
}

.cropper-preview-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.preview-label {
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.preview-box {
  align-items: center;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  display: flex;
  height: 220px;
  justify-content: center;
  overflow: hidden;
  width: 220px;
}

.preview-canvas {
  display: block;
  height: 220px;
  width: 220px;
}

.cropper-controls {
  align-items: center;
  border-top: 1px solid #e5e7eb;
  display: flex;
  gap: 16px;
  justify-content: space-between;
  padding-top: 16px;
}

.zoom-control,
.control-buttons {
  align-items: center;
  display: flex;
  gap: 10px;
}

.control-buttons {
  justify-content: flex-end;
}

:deep(cropper-canvas) {
  height: 100%;
  width: 100%;
}

:deep(cropper-selection) {
  outline: 2px solid #38bdf8;
}

:deep(cropper-handle[action$='resize']) {
  --theme-color: #38bdf8;
}

:deep(cropper-handle[action='move']) {
  --theme-color: transparent;
}

[data-bs-theme='dark'] .avatar-cropper-container {
  background: #111827;
}

[data-bs-theme='dark'] .avatar-cropper-header {
  background: #0f172a;
  border-color: #263244;
}

[data-bs-theme='dark'] .avatar-cropper-header h5,
[data-bs-theme='dark'] .btn-close {
  color: #e5eefb;
}

[data-bs-theme='dark'] .preview-label {
  color: #cbd5e1;
}

[data-bs-theme='dark'] .preview-box {
  background: #0f172a;
  border-color: #263244;
}

[data-bs-theme='dark'] .cropper-controls {
  border-color: #263244;
}

@media (max-width: 768px) {
  .cropper-workspace {
    grid-template-columns: 1fr;
  }

  .cropper-preview-section {
    align-items: center;
  }

  .cropper-controls {
    align-items: stretch;
    flex-direction: column;
  }

  .zoom-control,
  .control-buttons {
    justify-content: flex-end;
    width: 100%;
  }
}
</style>
