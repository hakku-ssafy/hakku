<template>
  <div id="app-root" class="flex min-h-screen flex-col bg-canvas pb-16 md:pb-0">
    <MarqueeStrip />
    <AppHeader />
    <main class="flex-1">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
    <AppFooter />
    <AppBottomNav />
    <ChatFab />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notifications'
import MarqueeStrip from '@/components/layout/MarqueeStrip.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import AppBottomNav from '@/components/layout/AppBottomNav.vue'
import ChatFab from '@/components/chat/ChatFab.vue'
import { applyPersonalColorTheme } from '@/composables/usePersonalColorTheme'

const authStore = useAuthStore()
const notificationStore = useNotificationStore()
const route = useRoute()

// 진단 완료/새 알림을 새로고침 없이 반영하기 위한 주기적 폴링 간격(ms).
const POLL_INTERVAL_MS = 10000
let pollTimer: ReturnType<typeof setInterval> | undefined

async function refreshNotifications() {
  if (!authStore.isAuthenticated) return
  await notificationStore.fetchNotifications()
}

// 인증 상태에서 알림을 갱신하고, 진단이 진행 중이면 사용자 정보를 다시 받아
// PENDING → COMPLETED 전환을 실시간으로 스토어에 반영한다.
async function poll() {
  if (!authStore.isAuthenticated) return
  const tasks: Promise<unknown>[] = [notificationStore.fetchNotifications()]
  if (authStore.user?.diagnosisStatus === 'PENDING') {
    tasks.push(authStore.fetchMe())
  }
  await Promise.allSettled(tasks)
}

function startPolling() {
  stopPolling()
  if (!authStore.isAuthenticated) return
  pollTimer = setInterval(poll, POLL_INTERVAL_MS)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = undefined
  }
}

onMounted(() => {
  refreshNotifications()
  startPolling()
})

onUnmounted(stopPolling)

watch(() => authStore.isAuthenticated, (ok) => {
  if (ok) {
    refreshNotifications()
    startPolling()
  } else {
    stopPolling()
  }
})

// 퍼스널컬러 → 동적 액센트 주입. 진단 결과가 있으면 전체 UI가 그 색으로 물든다.
watch(
  () => authStore.user?.personalColor ?? null,
  (color) => applyPersonalColorTheme(color),
  { immediate: true },
)

watch(() => route.path, (path) => {
  if (path === '/notifications') {
    notificationStore.markAllSeen()
  } else if (authStore.isAuthenticated) {
    refreshNotifications()
  }
})
</script>

<style scoped>
.page-enter-active,
.page-leave-active {
  transition: opacity 0.22s ease, transform 0.22s var(--ease-out-expo);
}
.page-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.page-leave-to {
  opacity: 0;
}
</style>
