<template>
  <div class="u-container max-w-2xl py-10 sm:py-12">
    <div class="border-b border-line pb-4 mb-7">
      <span class="u-eyebrow">My Page</span>
      <h1 class="u-serif text-title text-ink mt-2.5">마이페이지</h1>
    </div>

    <div v-if="loading" class="rounded-xl border border-line p-6 space-y-4">
      <div class="flex items-center gap-4">
        <SkeletonBlock height="4rem" width="4rem" class="!rounded-full" />
        <div class="space-y-2">
          <SkeletonBlock height="1.25rem" width="8rem" />
          <SkeletonBlock height="1rem" width="12rem" />
        </div>
      </div>
    </div>

    <div v-else-if="errorMessage" role="alert" class="px-4 py-4 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm">
      {{ errorMessage }}
    </div>

    <template v-else-if="user">
      <div class="rounded-xl border border-line bg-surface p-6 mb-4">
        <div class="flex items-center gap-4 mb-6">
          <div class="w-16 h-16 rounded-full u-gradient-accent text-accent-ink grid place-items-center shrink-0">
            <span class="text-2xl font-bold">{{ userInitial }}</span>
          </div>
          <div class="min-w-0">
            <h2 class="u-serif text-lg text-ink">{{ user.nickname }}</h2>
            <p class="text-sm text-ink-muted truncate">{{ user.email }}</p>
          </div>
        </div>

        <dl>
          <div class="flex justify-between items-center py-3 border-t border-line">
            <dt class="text-sm text-ink-soft">역할</dt>
            <dd><span class="inline-block px-2.5 py-0.5 text-xs font-medium rounded-full" :class="roleBadge(user.role)">{{ roleLabel(user.role) }}</span></dd>
          </div>

          <div class="flex justify-between items-center py-3 border-t border-line">
            <dt class="text-sm text-ink-soft">퍼스널컬러</dt>
            <dd class="text-sm font-medium">
              <button
                v-if="user.personalColor"
                type="button"
                class="hover:underline underline-offset-4 cursor-pointer"
                style="color: var(--color-accent)"
                @click="showDiagnosisModal = true"
              >
                {{ formatPersonalColor(user.personalColor) }}
              </button>
              <span v-else class="text-ink-muted">미진단</span>
            </dd>
          </div>

          <div v-if="user.preferredColors.length > 0" class="py-3 border-t border-line">
            <dt class="text-sm text-ink-soft mb-2">선호 컬러</dt>
            <dd class="flex flex-wrap gap-1.5">
              <AppBadge v-for="color in user.preferredColors" :key="color">{{ getColorLabel(color) }}</AppBadge>
            </dd>
          </div>

          <div v-if="user.preferredStyles.length > 0" class="py-3 border-t border-line">
            <dt class="text-sm text-ink-soft mb-2">선호 스타일</dt>
            <dd class="flex flex-wrap gap-1.5">
              <AppBadge v-for="style in user.preferredStyles" :key="style">{{ style }}</AppBadge>
            </dd>
          </div>
        </dl>
      </div>

      <div class="space-y-2">
        <router-link
          v-if="!user.personalColor"
          to="/diagnosis"
          class="flex items-center justify-between w-full u-gradient-accent text-accent-ink rounded-xl px-5 py-4 u-pop"
        >
          <div>
            <p class="font-semibold text-sm">AI 퍼스널컬러 진단</p>
            <p class="text-xs mt-0.5" style="color: rgba(255,255,255,0.75)">나에게 맞는 컬러를 찾아보세요</p>
          </div>
          <svg class="w-5 h-5 shrink-0" style="color: rgba(255,255,255,0.85)" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" /></svg>
        </router-link>

        <router-link
          v-else
          to="/diagnosis"
          class="flex items-center justify-between w-full bg-surface border border-line rounded-xl px-5 py-4 hover:bg-surface-soft hover:border-accent-line transition-colors"
        >
          <p class="text-sm font-medium text-ink">퍼스널컬러 재진단</p>
          <svg class="w-5 h-5 text-ink-muted shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" /></svg>
        </router-link>

        <button
          type="button"
          class="flex items-center justify-between w-full bg-surface border border-line rounded-xl px-5 py-4 hover:bg-surface-soft transition-colors"
          @click="handleLogout"
        >
          <p class="text-sm font-medium text-red-500">로그아웃</p>
          <svg class="w-5 h-5 text-ink-muted shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" /></svg>
        </button>
      </div>
    </template>

    <AppModal v-model:open="showDiagnosisModal" title="진단 이미지" max-width="md">
      <img v-if="diagnosisPreviewSrc" :src="diagnosisPreviewSrc" alt="퍼스널컬러 진단 이미지" class="w-full rounded-lg" />
      <p v-if="user?.personalColor" class="text-center text-sm font-semibold mt-4 u-gradient-text">
        {{ formatPersonalColor(user?.personalColor ?? null) }}
      </p>
    </AppModal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { COLOR_OPTIONS, formatPersonalColor } from '@/types'
import type { User, UserRole } from '@/types'
import { useAuthedImage } from '@/composables/useAuthedImage'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppModal from '@/components/ui/AppModal.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const user = ref<User | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const showDiagnosisModal = ref(false)

const diagnosisPreviewUrl = computed(() =>
  user.value?.diagnosisImageUrl ?? user.value?.profileImageUrl ?? null
)

// 진단 사진은 인증이 필요한 storage 리소스 → 토큰 붙여 fetch 후 object URL 로 표시
const { objectUrl: diagnosisPreviewSrc } = useAuthedImage(diagnosisPreviewUrl)

const userInitial = computed(() =>
  user.value?.nickname?.charAt(0).toUpperCase() ?? '?'
)


function getColorLabel(value: string): string {
  return COLOR_OPTIONS.find((c) => c.value === value)?.label ?? value
}

function roleLabel(role: UserRole): string {
  switch (role) {
    case 'ADMIN': return '관리자'
    case 'SELLER': return '판매자'
    default: return '일반 회원'
  }
}

function roleBadge(role: UserRole): string {
  switch (role) {
    case 'ADMIN': return 'bg-ink text-canvas'
    case 'SELLER': return 'border border-line-strong text-ink'
    default: return 'bg-surface-sunken text-ink-soft'
  }
}

function handleLogout() {
  authStore.logout()
  router.push('/')
}

watch(
  () => route.query.view,
  (view) => {
    if (view === 'diagnosis' && user.value?.personalColor) {
      showDiagnosisModal.value = true
    }
  }
)

onMounted(async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    await authStore.fetchMe()
    user.value = authStore.user
    if (route.query.view === 'diagnosis' && user.value?.personalColor) {
      showDiagnosisModal.value = true
    }
  } catch {
    errorMessage.value = '사용자 정보를 불러오는데 실패했습니다.'
  } finally {
    loading.value = false
  }
})
</script>
