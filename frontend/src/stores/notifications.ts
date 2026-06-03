import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import apiClient from '@/api/client'
import type { Notification } from '@/types'

const LAST_SEEN_KEY = 'hakku_notifications_last_seen'

export const useNotificationStore = defineStore('notifications', () => {
  const items = ref<Notification[]>([])
  const loading = ref(false)
  const lastSeenAt = ref<number>(
    Number(localStorage.getItem(LAST_SEEN_KEY) ?? '0')
  )

  const hasUnread = computed(() =>
    items.value.some((n) => n.createdAt > lastSeenAt.value)
  )

  async function fetchNotifications() {
    loading.value = true
    try {
      const { data } = await apiClient.get<Notification[]>('/notifications')
      items.value = data
    } catch {
      items.value = []
    } finally {
      loading.value = false
    }
  }

  function markAllSeen() {
    if (items.value.length === 0) {
      lastSeenAt.value = Date.now()
    } else {
      lastSeenAt.value = Math.max(...items.value.map((n) => n.createdAt))
    }
    localStorage.setItem(LAST_SEEN_KEY, String(lastSeenAt.value))
  }

  return { items, loading, hasUnread, fetchNotifications, markAllSeen }
})
