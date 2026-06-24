<template>
  <header class="sticky top-0 z-50 border-b border-line app-header">
    <div class="u-container h-[72px] flex items-center gap-6 md:gap-10">
      <!-- 로고 — hakku. 워드마크(마침표 accent) -->
      <router-link to="/" class="logo-word text-ink shrink-0" aria-label="학꾸 홈">
        hakku<span class="text-accent">.</span>
      </router-link>

      <!-- 데스크탑 언더라인 내비 (홈·상품·커뮤니티 + 로그인 시 진단·추천 + SELLER 시 판매) -->
      <nav class="hidden md:flex items-center gap-7 flex-1" aria-label="주 메뉴">
        <router-link
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          custom
          v-slot="{ navigate, isActive, isExactActive }"
        >
          <button
            type="button"
            class="nav-link"
            :class="{ 'nav-link--active': item.to === '/' ? isExactActive : isActive }"
            @click="navigate"
          >{{ item.label }}</button>
        </router-link>
      </nav>

      <!-- 우측 액션 — 항상 오른쪽 정렬 -->
      <div class="flex items-center gap-3 md:gap-4 ml-auto md:ml-0">
        <!-- 검색 (≥1024) — 제출 시 /products?q= 로 이동 -->
        <form class="search-pill hidden lg:flex" role="search" @submit.prevent="submitSearch">
          <span class="search-pill__icon" aria-hidden="true">⌕</span>
          <input
            v-model="searchInput"
            type="search"
            class="search-pill__input"
            aria-label="상품 검색"
            placeholder="키링, 퍼스널컬러, 다꾸…"
          />
        </form>

        <template v-if="authStore.isAuthenticated">
          <!-- 진단 완료 시 퍼스널컬러 배지 (accent 로 물듦) → 마이 -->
          <router-link
            v-if="personalColorLabel"
            to="/my"
            class="pc-badge"
            :aria-label="`내 퍼스널컬러: ${personalColorLabel}`"
          >
            <span class="pc-badge__dot" aria-hidden="true" />{{ personalColorLabel }}
          </router-link>

          <!-- 알림 (미읽음 점 배지 보존) -->
          <router-link to="/notifications" class="icon-btn relative" aria-label="알림" active-class="icon-btn--active">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" class="w-5 h-5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M14.857 17.082a23.8 23.8 0 0 0 5.454-1.31A8.97 8.97 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.97 8.97 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.3 24.3 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0" />
            </svg>
            <span v-if="notificationStore.hasUnread" class="badge-dot" aria-label="새 알림" />
          </router-link>

          <!-- 마이페이지 — 진단 여부와 무관하게 항상 노출 -->
          <router-link to="/my" class="icon-btn" aria-label="마이페이지" active-class="icon-btn--active">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" class="w-5 h-5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0A17.933 17.933 0 0 1 12 21.75c-2.676 0-5.216-.584-7.499-1.632Z" />
            </svg>
          </router-link>

          <!-- 위시리스트 (마이페이지 찜 탭) -->
          <router-link to="/my" class="icon-btn hide-mobile" aria-label="찜 목록" active-class="icon-btn--active">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" class="w-5 h-5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z" />
            </svg>
          </router-link>

          <!-- 장바구니 -->
          <router-link to="/cart" class="icon-btn" aria-label="장바구니" active-class="icon-btn--active">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" class="w-5 h-5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 0 0-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 0 0-16.536-1.84M7.5 14.25 5.106 5.272M6 20.25a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Zm12.75 0a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Z" />
            </svg>
          </router-link>

          <!-- 로그아웃: 데스크탑 전용 (모바일은 하단 탭바의 '마이'에서 처리) -->
          <span class="hide-mobile w-px h-5 bg-line mx-1" aria-hidden="true" />
          <button type="button" class="nav-link hide-mobile" @click="handleLogout">로그아웃</button>
        </template>

        <template v-else>
          <router-link to="/login" class="nav-link hidden sm:inline-flex" active-class="nav-link--active">로그인</router-link>
          <AppButton to="/signup" size="sm">회원가입</AppButton>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notifications'
import { formatPersonalColor } from '@/types'
import AppButton from '@/components/ui/AppButton.vue'

interface NavItem {
  to: string
  label: string
}

const authStore = useAuthStore()
const notificationStore = useNotificationStore()
const router = useRouter()

// 권한별 노출 규칙 보존: 진단·추천은 로그인 시, 판매는 SELLER 시.
const navItems = computed<NavItem[]>(() => {
  const items: NavItem[] = [
    { to: '/', label: '홈' },
    { to: '/products', label: '상품' },
    { to: '/community', label: '커뮤니티' },
  ]
  if (authStore.isAuthenticated) {
    items.push({ to: '/diagnosis', label: '진단' })
    items.push({ to: '/recommendations', label: '추천' })
    if (authStore.user?.role === 'SELLER') {
      items.push({ to: '/seller/products', label: '판매' })
    }
  }
  return items
})

const personalColorLabel = computed(() => formatPersonalColor(authStore.user?.personalColor ?? null))

// 상단 검색 — 공백만 입력하면 무시, 그 외에는 상품 목록으로 검색어를 넘긴다.
const searchInput = ref('')
function submitSearch() {
  const q = searchInput.value.trim()
  if (!q) return
  router.push({ path: '/products', query: { q } })
}

function handleLogout() {
  authStore.logout()
  router.push('/')
}
</script>

<style scoped>
/* 모바일(<768)에서 데스크톱 전용 액션 숨김.
   스코프드 .icon-btn/.nav-link 의 display 가 Tailwind .hidden(레이어드)보다
   우선하므로, 미디어쿼리 + !important 로 확실히 가린다. */
@media (max-width: 767.98px) {
  .hide-mobile {
    display: none !important;
  }
}

/* 반투명 + 블러 + 1px 보더(그림자 없음) */
.app-header {
  background: var(--hk-header-bg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.logo-word {
  font-weight: 800;
  font-size: 1.4375rem; /* 23px */
  letter-spacing: -0.04em;
  line-height: 1;
}

/* 언더라인 탭 — 활성 = 먹색 + 2px 하단 보더 */
.nav-link {
  display: inline-flex;
  align-items: center;
  padding: 0.4rem 0;
  font-size: 0.875rem;
  font-weight: 500;
  letter-spacing: 0.01em;
  color: var(--hk-text-muted);
  white-space: nowrap;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: color 0.18s ease, border-color 0.18s ease;
}
.nav-link:hover {
  color: var(--hk-ink);
}
.nav-link--active {
  color: var(--hk-ink);
  border-bottom-color: var(--hk-ink);
}

/* 검색 placeholder 알약 */
.search-pill {
  align-items: center;
  gap: 0.5rem;
  height: 38px;
  padding: 0 0.875rem;
  min-width: 200px;
  border: 1px solid var(--hk-border-control);
  border-radius: var(--hk-radius-pill);
  background: var(--hk-surface);
  color: var(--hk-text-faint);
}
.search-pill:focus-within {
  border-color: var(--hk-ink);
}
.search-pill__icon {
  font-size: 0.8125rem;
}
.search-pill__input {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  font-size: 0.78rem;
  letter-spacing: 0.01em;
  color: var(--hk-ink);
  outline: none;
}
.search-pill__input::placeholder {
  color: var(--hk-text-faint);
}
/* type=search 기본 클리어 버튼 제거(웹킷) */
.search-pill__input::-webkit-search-decoration,
.search-pill__input::-webkit-search-cancel-button {
  -webkit-appearance: none;
  appearance: none;
}

.icon-btn {
  display: inline-grid;
  place-items: center;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 9999px;
  color: var(--hk-text-muted);
  background: none;
  border: none;
  cursor: pointer;
  transition: color 0.18s ease, background 0.18s ease;
}
.icon-btn:hover,
.icon-btn--active {
  color: var(--hk-ink);
  background: var(--hk-cream);
}

/* 퍼스널컬러 배지 — accent-soft 배경 + accent 보더 + accent-ink 텍스트 */
.pc-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  height: 30px;
  padding: 0 0.7rem;
  border-radius: 9999px;
  font-size: 0.71875rem; /* 11.5px */
  font-weight: 600;
  letter-spacing: 0.01em;
  color: var(--accent-ink);
  background: var(--accent-soft);
  border: 1px solid var(--accent);
  white-space: nowrap;
  transition: filter 0.18s ease;
}
.pc-badge:hover {
  filter: brightness(0.97);
}
.pc-badge__dot {
  width: 0.6875rem;
  height: 0.6875rem;
  border-radius: 9999px;
  background: var(--accent);
}

.badge-dot {
  position: absolute;
  top: 0.45rem;
  right: 0.5rem;
  width: 0.5rem;
  height: 0.5rem;
  border-radius: 9999px;
  background: var(--accent);
  box-shadow: 0 0 0 2px var(--hk-surface-warm);
}
</style>
