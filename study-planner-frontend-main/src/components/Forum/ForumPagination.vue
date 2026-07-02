<template>
  <nav
    v-if="pagination && pagination.totalPages > 1"
    class="forum-pagination"
    :aria-label="$t('forum.pagination.label')"
  >
    <div class="pagination-summary">
      {{ $t('forum.pagination.summary', summaryValues) }}
    </div>
    <ul class="pagination mb-0">
      <li class="page-item" :class="{ disabled: isFirstPage || loading }">
        <button
          class="page-link"
          type="button"
          :aria-label="$t('forum.pagination.previous')"
          :disabled="isFirstPage || loading"
          @click="goToPage(page - 1)"
        >
          <i class="bi bi-chevron-left"></i>
        </button>
      </li>

      <li
        v-for="item in visiblePages"
        :key="item.key"
        class="page-item"
        :class="{ active: item.page === page, disabled: item.ellipsis || loading }"
      >
        <span v-if="item.ellipsis" class="page-link">...</span>
        <button
          v-else
          class="page-link"
          type="button"
          :disabled="loading || item.page === page"
          @click="goToPage(item.page)"
        >
          {{ item.page }}
        </button>
      </li>

      <li class="page-item" :class="{ disabled: isLastPage || loading }">
        <button
          class="page-link"
          type="button"
          :aria-label="$t('forum.pagination.next')"
          :disabled="isLastPage || loading"
          @click="goToPage(page + 1)"
        >
          <i class="bi bi-chevron-right"></i>
        </button>
      </li>
    </ul>
  </nav>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  pagination: {
    type: Object,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['change'])

const page = computed(() => Number(props.pagination?.page || 1))
const size = computed(() => Number(props.pagination?.size || 10))
const total = computed(() => Number(props.pagination?.total || 0))
const totalPages = computed(() => Number(props.pagination?.totalPages || 0))
const isFirstPage = computed(() => page.value <= 1)
const isLastPage = computed(() => page.value >= totalPages.value)

const summaryValues = computed(() => {
  const start = total.value === 0 ? 0 : (page.value - 1) * size.value + 1
  const end = Math.min(page.value * size.value, total.value)
  return { start, end, total: total.value }
})

const visiblePages = computed(() => {
  const pages = []
  const last = totalPages.value
  const current = page.value

  if (last <= 7) {
    for (let index = 1; index <= last; index += 1) {
      pages.push({ key: `page-${index}`, page: index })
    }
    return pages
  }

  pages.push({ key: 'page-1', page: 1 })

  const start = Math.max(2, current - 1)
  const end = Math.min(last - 1, current + 1)

  if (start > 2) {
    pages.push({ key: 'ellipsis-start', ellipsis: true })
  }

  for (let index = start; index <= end; index += 1) {
    pages.push({ key: `page-${index}`, page: index })
  }

  if (end < last - 1) {
    pages.push({ key: 'ellipsis-end', ellipsis: true })
  }

  pages.push({ key: `page-${last}`, page: last })
  return pages
})

function goToPage(targetPage) {
  if (props.loading) return
  if (targetPage < 1 || targetPage > totalPages.value || targetPage === page.value) return
  emit('change', targetPage)
}
</script>

<style scoped>
.forum-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 18px;
  padding: 10px 0 4px;
}

.pagination-summary {
  color: #6c7a91;
  font-size: 0.9rem;
}

.page-link {
  min-width: 38px;
  height: 38px;
  color: #2d7eca;
  border-color: #d8e5f2;
  text-align: center;
}

.page-item.active .page-link {
  color: #fff;
  background: #3e93f5;
  border-color: #3e93f5;
}

.page-item.disabled .page-link {
  color: #9aa7b7;
  background: #f4f7fb;
}

[data-bs-theme='dark'] .pagination-summary {
  color: #b8c4d6;
}

[data-bs-theme='dark'] .page-link {
  color: #d7e9ff;
  background: #111827;
  border-color: #374151;
}

[data-bs-theme='dark'] .page-item.active .page-link {
  color: #061b36;
  background: #7fd2ff;
  border-color: #7fd2ff;
}

[data-bs-theme='dark'] .page-item.disabled .page-link {
  color: #718096;
  background: #1f2937;
  border-color: #374151;
}

@media (max-width: 576px) {
  .forum-pagination {
    align-items: stretch;
    flex-direction: column;
  }

  .pagination {
    justify-content: center;
  }

  .pagination-summary {
    text-align: center;
  }
}
</style>
