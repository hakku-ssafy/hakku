<template>
  <div class="u-container u-container--reading py-10 sm:py-12">
    <div class="border-b border-line pb-4 mb-7">
      <span class="u-eyebrow">Notifications</span>
      <h1 class="u-serif text-title text-ink mt-2.5">알림</h1>
    </div>

    <div v-if="loading" class="divide-y divide-line-soft border-y border-line-soft">
      <div v-for="n in 5" :key="n" class="py-4 flex gap-3">
        <SkeletonBlock height="2.5rem" width="2.5rem" class="shrink-0 !rounded-full" />
        <div class="flex-1 space-y-2 pt-1">
          <SkeletonBlock height="0.875rem" width="75%" />
          <SkeletonBlock height="0.75rem" width="25%" />
        </div>
      </div>
    </div>

    <div
      v-else-if="errorMessage"
      role="alert"
      class="px-4 py-4 bg-coral/10 border border-coral/30 rounded-md text-coral text-sm"
    >
      {{ errorMessage }}
    </div>

    <EmptyState
      v-else-if="notifications.length === 0"
      icon="🔔"
      title="새 알림이 없어요"
      description="진단 완료, 댓글, 좋아요 소식이 여기에 표시돼요."
    />

    <ul v-else class="divide-y divide-line-soft border-y border-line-soft">
      <li
        v-for="(notification, index) in notifications"
        :key="`${notification.type}-${notification.createdAt}-${index}`"
      >
        <button
          type="button"
          class="notif-row"
          :class="isClickable(notification) ? 'notif-row--clickable' : 'notif-row--static'"
          @click="handleNotificationClick(notification)"
        >
          <span class="notif-icon" aria-hidden="true">{{ typeIcon(notification.type) }}</span>
          <span class="flex-1 min-w-0">
            <span class="block text-sm text-ink">{{ notification.message }}</span>
            <time class="block text-xs text-ink-faint mt-1">{{ formatDate(notification.createdAt) }}</time>
          </span>
          <svg
            v-if="isClickable(notification)"
            class="notif-chevron"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
          </svg>
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
    case 'FOLLOW': return '👤'
    case 'WISHLIST_LIKE': return '💖'
    case 'ORDER': return '📦'
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
  if (notification.type === 'ORDER') return true
  if (notification.type === 'DIAGNOSIS_COMPLETE') return true
  if (notification.type === 'FOLLOW') return notification.actorId != null
  if (notification.type === 'WISHLIST_LIKE') return notification.productId != null
  return notification.postId != null
}

function handleNotificationClick(notification: Notification) {
  if (notification.type === 'ORDER') {
    router.push({ path: '/my', query: { tab: 'orders' } })
    return
  }
  if (notification.type === 'DIAGNOSIS_COMPLETE') {
    router.push({ path: '/my', query: { view: 'diagnosis' } })
    return
  }
  if (notification.type === 'FOLLOW' && notification.actorId != null) {
    router.push(`/users/${notification.actorId}`)
    return
  }
  if (notification.type === 'WISHLIST_LIKE' && notification.productId != null) {
    router.push(`/products/${notification.productId}`)
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

<style scoped>
/* 알림 행 — 좌측 글리프 원 + 텍스트 + 시간. 클릭 가능 항목만 호버/좌측 강조 바 */
.notif-row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0.875rem;
  width: 100%;
  padding: 1rem 0.25rem 1rem 1rem;
  text-align: left;
  background: transparent;
  border: 0;
  transition: background 0.18s ease;
}
.notif-row::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0.5rem;
  bottom: 0.5rem;
  width: 2px;
  border-radius: 2px;
  background: transparent;
  transition: background 0.18s ease;
}
.notif-row--clickable {
  cursor: pointer;
}
.notif-row--clickable:hover {
  background: var(--color-surface-soft);
}
.notif-row--clickable:hover::before {
  background: var(--color-accent);
}
.notif-row--clickable:focus-visible {
  outline: 2px solid var(--color-ink);
  outline-offset: -2px;
  border-radius: 6px;
}
.notif-row--static {
  cursor: default;
  padding-left: 1rem;
}

.notif-icon {
  display: grid;
  place-items: center;
  width: 2.5rem;
  height: 2.5rem;
  flex-shrink: 0;
  font-size: 1.05rem;
  border-radius: 9999px;
  background: var(--color-cream);
}
.notif-row--clickable:hover .notif-icon {
  background: var(--color-accent-soft);
}

.notif-chevron {
  width: 1rem;
  height: 1rem;
  flex-shrink: 0;
  color: var(--color-ink-faint);
  transition: transform 0.2s var(--ease-out-expo), color 0.18s ease;
}
.notif-row--clickable:hover .notif-chevron {
  transform: translateX(2px);
  color: var(--color-ink);
}
</style>
