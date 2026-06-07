<template>
  <header class="sticky top-0 z-50 border-b border-line bg-canvas/80 backdrop-blur-xl">
    <div class="u-container h-16 flex items-center justify-between gap-4">
      <!-- 로고 -->
      <router-link to="/" class="flex items-center gap-2 shrink-0" aria-label="학꾸 홈">
        <img src="/logo.png" alt="" class="w-8 h-8 rounded-lg object-contain" />
        <span class="u-serif text-2xl text-ink">학꾸</span>
      </router-link>

      <!-- 데스크탑 텍스트 메뉴 -->
      <nav class="hidden md:flex items-center gap-1" aria-label="주 메뉴">
        <router-link to="/products" class="nav-link" active-class="nav-link--active">상품</router-link>
        <router-link to="/community" class="nav-link" active-class="nav-link--active">커뮤니티</router-link>
        <template v-if="authStore.isAuthenticated">
          <router-link to="/diagnosis" class="nav-link" active-class="nav-link--active">진단</router-link>
          <router-link
            v-if="authStore.user?.role === 'SELLER'"
            to="/seller/products"
            class="nav-link"
            active-class="nav-link--active"
          >판매</router-link>
        </template>
      </nav>

      <!-- 우측 액션: 모든 화면에서 오른쪽 고정 -->
      <div class="flex items-center gap-1">
        <template v-if="authStore.isAuthenticated">
          <!-- 알림·장바구니 아이콘 (모바일·데스크탑 항상 노출) -->
          <router-link to="/notifications" class="icon-btn relative" aria-label="알림" active-class="icon-btn--active">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" class="w-5 h-5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M14.857 17.082a23.8 23.8 0 0 0 5.454-1.31A8.97 8.97 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.97 8.97 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.3 24.3 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0" />
            </svg>
            <span v-if="notificationStore.hasUnread" class="badge-dot" aria-label="새 알림" />
          </router-link>
          <router-link to="/cart" class="icon-btn" aria-label="장바구니" active-class="icon-btn--active">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" class="w-5 h-5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 0 0-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 0 0-16.536-1.84M7.5 14.25 5.106 5.272M6 20.25a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Zm12.75 0a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Z" />
            </svg>
          </router-link>
          <!-- 마이·로그아웃: 데스크탑 전용 (모바일은 하단 탭바의 '마이'로 이동) -->
          <span class="hidden md:block w-px h-5 bg-line mx-1.5" aria-hidden="true" />
          <router-link to="/my" class="nav-link hidden md:inline-flex" active-class="nav-link--active">마이</router-link>
          <button type="button" class="nav-link hidden md:inline-flex" @click="handleLogout">로그아웃</button>
        </template>
        <template v-else>
          <router-link to="/login" class="nav-link hidden md:inline-flex" active-class="nav-link--active">로그인</router-link>
          <AppButton to="/signup" size="sm">회원가입</AppButton>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notifications'
import AppButton from '@/components/ui/AppButton.vue'

const authStore = useAuthStore()
const notificationStore = useNotificationStore()
const router = useRouter()

function handleLogout() {
  authStore.logout()
  router.push('/')
}
</script>

<style scoped>
.nav-link {
  display: inline-flex;
  align-items: center;
  padding: 0.5rem 0.75rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-ink-soft);
  border-radius: 9999px;
  white-space: nowrap;
  background: none;
  border: none;
  cursor: pointer;
  transition: color 0.18s ease, background 0.18s ease;
}
.nav-link:hover {
  color: var(--color-accent);
  background: var(--color-accent-soft);
}
.nav-link--active {
  color: var(--color-accent);
}
.icon-btn {
  display: inline-grid;
  place-items: center;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 9999px;
  color: var(--color-ink-soft);
  background: none;
  border: none;
  cursor: pointer;
  transition: color 0.18s ease, background 0.18s ease;
}
.icon-btn:hover,
.icon-btn--active {
  color: var(--color-accent);
  background: var(--color-accent-soft);
}
.badge-dot {
  position: absolute;
  top: 0.45rem;
  right: 0.5rem;
  width: 0.5rem;
  height: 0.5rem;
  border-radius: 9999px;
  background: var(--color-accent);
  box-shadow: 0 0 0 2px var(--color-canvas);
}
</style>
