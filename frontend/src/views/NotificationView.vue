<template>
  <div class="max-w-2xl mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold text-gray-900 mb-6">알림</h1>

    <div v-if="loading" class="space-y-2">
      <div
        v-for="n in 5"
        :key="n"
        class="bg-white rounded-xl border border-gray-100 p-4 animate-pulse flex gap-3"
      >
        <div class="w-9 h-9 bg-gray-100 rounded-full shrink-0" />
        <div class="flex-1 space-y-2">
          <div class="h-4 bg-gray-100 rounded w-3/4" />
          <div class="h-3 bg-gray-100 rounded w-1/4" />
        </div>
      </div>
    </div>

    <div
      v-else-if="errorMessage"
      role="alert"
      class="p-4 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm"
    >
      {{ errorMessage }}
    </div>

    <div v-else-if="notifications.length === 0" class="py-20 text-center">
      <div class="w-16 h-16 bg-gray-50 rounded-full flex items-center justify-center mx-auto mb-4">
        <svg class="w-8 h-8 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="1.5"
            d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
          />
        </svg>
      </div>
      <p class="text-gray-500 font-medium">새 알림이 없어요</p>
    </div>

    <ul v-else class="space-y-2">
      <li
        v-for="(notification, index) in notifications"
        :key="index"
        class="bg-white rounded-xl border border-gray-100 p-4 flex items-start gap-3"
      >
        <div
          class="w-9 h-9 rounded-full flex items-center justify-center shrink-0 text-base"
          :class="iconBg(notification.type)"
        >
          {{ typeIcon(notification.type) }}
        </div>
        <div class="flex-1 min-w-0">
          <p class="text-sm text-gray-800">{{ notification.message }}</p>
          <time
            class="text-xs text-gray-400 mt-1 block"
            :datetime="notification.createdAt"
          >
            {{ formatDate(notification.createdAt) }}
          </time>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import apiClient from '@/api/client'
import type { Notification } from '@/types'

const notifications = ref<Notification[]>([])
const loading = ref(false)
const errorMessage = ref('')

function typeIcon(type: string): string {
  switch (type) {
    case 'COMMENT': return '💬'
    case 'LIKE': return '❤️'
    case 'PRODUCT': return '📦'
    default: return '🔔'
  }
}

function iconBg(type: string): string {
  switch (type) {
    case 'COMMENT': return 'bg-blue-50'
    case 'LIKE': return 'bg-red-50'
    case 'PRODUCT': return 'bg-purple-50'
    default: return 'bg-gray-50'
  }
}

function formatDate(dateString: string): string {
  const date = new Date(dateString)
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

onMounted(fetchNotifications)
</script>
