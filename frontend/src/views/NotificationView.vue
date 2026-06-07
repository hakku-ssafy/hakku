<template>
  <div class="u-container max-w-2xl py-10 sm:py-12">
    <div class="border-b border-line pb-4 mb-7">
      <span class="u-eyebrow">Notifications</span>
      <h1 class="u-serif text-title text-ink mt-2.5">알림</h1>
    </div>

    <div v-if="loading" class="space-y-2">
      <div v-for="n in 5" :key="n" class="rounded-xl border border-line p-4 flex gap-3">
        <SkeletonBlock height="2.25rem" width="2.25rem" class="shrink-0 !rounded-full" />
        <div class="flex-1 space-y-2 pt-1">
          <SkeletonBlock height="0.875rem" width="75%" />
          <SkeletonBlock height="0.75rem" width="25%" />
        </div>
      </div>
    </div>

    <div
      v-else-if="errorMessage"
      role="alert"
      class="px-4 py-4 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm"
    >
      {{ errorMessage }}
    </div>

    <EmptyState
      v-else-if="notifications.length === 0"
      icon="🔔"
      title="새 알림이 없어요"
      description="진단 완료, 댓글, 좋아요 소식이 여기에 표시돼요."
    />

    <ul v-else class="space-y-2">
      <li
        v-for="(notification, index) in notifications"
        :key="`${notification.type}-${notification.createdAt}-${index}`"
      >
        <button
          type="button"
          class="w-full text-left rounded-xl border border-line bg-surface p-4 flex items-start gap-3 transition-colors"
          :class="isClickable(notification) ? 'hover:border-line-strong hover:bg-surface-soft' : 'cursor-default'"
          @click="handleNotificationClick(notification)"
        >
          <div class="w-9 h-9 rounded-full bg-surface-sunken grid place-items-center shrink-0 text-base">
            {{ typeIcon(notification.type) }}
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-sm text-ink">{{ notification.message }}</p>
            <time class="text-xs text-ink-muted mt-1 block">{{ formatDate(notification.createdAt) }}</time>
          </div>
        </button>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '@/stores/notifications'
import apiClient from '@/api/client'
import type { Notification } from '@/types'
import EmptyState from '@/components/ui/EmptyState.vue'
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue'

const router = useRouter()
const notificationStore = useNotificationStore()
const notifications = ref<Notification[]>([])
const loading = ref(false)
const errorMessage = ref('')

function typeIcon(type: string): string {
  switch (type) {
    case 'COMMENT': return '💬'
    case 'LIKE': return '❤️'
    case 'DIAGNOSIS_COMPLETE': return '🎨'
    default: return '🔔'
  }
}

function formatDate(createdAt: number): string {
  const date = new Date(createdAt)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMin = Math.floor(diffMs / 60_000)
  const diffHour = Math.floor(diffMin / 60)
  const diffDay = Math.floor(diffHour / 24)

  if (diffMin < 1) return '방금 전'
  if (diffMin < 60) return `${diffMin}분 전`
  if (diffHour < 24) return `${diffHour}시간 전`
  if (diffDay < 7) return `${diffDay}일 전`

  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

function isClickable(notification: Notification): boolean {
  if (notification.type === 'DIAGNOSIS_COMPLETE') return true
  return notification.postId != null
}

function handleNotificationClick(notification: Notification) {
  if (notification.type === 'DIAGNOSIS_COMPLETE') {
    router.push({ path: '/my', query: { view: 'diagnosis' } })
    return
  }
  if (notification.postId != null) {
    router.push(`/community/${notification.postId}`)
  }
}

async function fetchNotifications() {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await apiClient.get<Notification[]>('/notifications')
    notifications.value = data
  } catch {
    errorMessage.value = '알림을 불러오는데 실패했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchNotifications()
  notificationStore.markAllSeen()
})
</script>
