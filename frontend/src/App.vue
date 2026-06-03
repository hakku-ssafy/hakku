<template>
  <div id="app-root">
    <nav class="bg-white border-b border-gray-100 sticky top-0 z-50">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
        <router-link to="/" class="flex items-center">
          <span class="text-xl font-bold bg-gradient-to-r from-purple-600 to-pink-500 bg-clip-text text-transparent">학꾸</span>
        </router-link>

        <div class="flex items-center gap-0.5">
          <router-link to="/products" class="px-3 py-2 text-sm text-gray-600 hover:text-purple-600 rounded-lg hover:bg-purple-50 transition-colors" active-class="text-purple-600 bg-purple-50 font-medium">상품</router-link>
          <router-link to="/community" class="px-3 py-2 text-sm text-gray-600 hover:text-purple-600 rounded-lg hover:bg-purple-50 transition-colors" active-class="text-purple-600 bg-purple-50 font-medium">커뮤니티</router-link>

          <template v-if="authStore.isAuthenticated">
            <router-link v-if="authStore.user?.role === 'SELLER'" to="/seller/products" class="px-3 py-2 text-sm text-gray-600 hover:text-purple-600 rounded-lg hover:bg-purple-50 transition-colors" active-class="text-purple-600 bg-purple-50 font-medium">상품 올리기</router-link>
            <router-link to="/cart" class="px-3 py-2 text-sm text-gray-600 hover:text-purple-600 rounded-lg hover:bg-purple-50 transition-colors" active-class="text-purple-600 bg-purple-50 font-medium">장바구니</router-link>
            <router-link to="/notifications" class="relative px-3 py-2 text-sm text-gray-600 hover:text-purple-600 rounded-lg hover:bg-purple-50 transition-colors" active-class="text-purple-600 bg-purple-50 font-medium">
              알림
              <span v-if="notificationStore.hasUnread" class="absolute top-1.5 right-1.5 w-2 h-2 bg-red-500 rounded-full ring-2 ring-white" aria-label="새 알림" />
            </router-link>
            <router-link to="/my" class="ml-1 px-4 py-2 text-sm bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors font-medium">마이페이지</router-link>
          </template>
          <template v-else>
            <router-link to="/login" class="px-3 py-2 text-sm text-gray-600 hover:text-purple-600 rounded-lg hover:bg-purple-50 transition-colors">로그인</router-link>
            <router-link to="/signup" class="ml-1 px-4 py-2 text-sm bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors font-medium">회원가입</router-link>
          </template>
        </div>
      </div>
    </nav>

    <main>
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notifications'

const authStore = useAuthStore()
const notificationStore = useNotificationStore()
const route = useRoute()

async function refreshNotifications() {
  if (!authStore.isAuthenticated) return
  await notificationStore.fetchNotifications()
}

onMounted(refreshNotifications)

watch(() => authStore.isAuthenticated, (ok) => {
  if (ok) refreshNotifications()
})

watch(() => route.path, (path) => {
  if (path === '/notifications') {
    notificationStore.markAllSeen()
  } else if (authStore.isAuthenticated) {
    refreshNotifications()
  }
})
</script>
