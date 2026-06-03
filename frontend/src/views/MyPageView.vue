<template>
  <div class="max-w-2xl mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold text-gray-900 mb-6">마이페이지</h1>

    <div v-if="loading" class="bg-white rounded-xl border border-gray-100 p-6 animate-pulse space-y-4">
      <div class="flex items-center gap-4">
        <div class="w-16 h-16 bg-gray-100 rounded-full" />
        <div class="space-y-2">
          <div class="h-5 bg-gray-100 rounded w-32" />
          <div class="h-4 bg-gray-100 rounded w-48" />
        </div>
      </div>
      <div class="h-4 bg-gray-100 rounded w-full mt-4" />
      <div class="h-4 bg-gray-100 rounded w-2/3" />
    </div>

    <div
      v-else-if="errorMessage"
      role="alert"
      class="p-4 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm"
    >
      {{ errorMessage }}
    </div>

    <template v-else-if="user">
      <div class="bg-white rounded-xl border border-gray-100 p-6 mb-4">
        <div class="flex items-center gap-4 mb-6">
          <div class="w-16 h-16 bg-purple-100 rounded-full flex items-center justify-center shrink-0">
            <span class="text-2xl font-bold text-purple-600">{{ userInitial }}</span>
          </div>
          <div>
            <h2 class="text-lg font-bold text-gray-900">{{ user.nickname }}</h2>
            <p class="text-sm text-gray-400">{{ user.email }}</p>
          </div>
        </div>

        <dl class="space-y-0">
          <div class="flex justify-between items-center py-3 border-t border-gray-50">
            <dt class="text-sm text-gray-500">역할</dt>
            <dd>
              <span
                class="inline-block px-2.5 py-0.5 text-xs font-medium rounded-full"
                :class="roleBadge(user.role)"
              >
                {{ roleLabel(user.role) }}
              </span>
            </dd>
          </div>

          <div class="flex justify-between items-center py-3 border-t border-gray-50">
            <dt class="text-sm text-gray-500">퍼스널컬러</dt>
            <dd class="text-sm font-medium text-gray-800">
              <button
                v-if="user.personalColor"
                type="button"
                class="text-purple-600 hover:underline cursor-pointer"
                @click="showDiagnosisModal = true"
              >
                {{ formatPersonalColor(user.personalColor) }}
              </button>
              <span v-else class="text-gray-400">미진단</span>
            </dd>
          </div>

          <div v-if="user.preferredColors.length > 0" class="py-3 border-t border-gray-50">
            <dt class="text-sm text-gray-500 mb-2">선호 컬러</dt>
            <dd class="flex flex-wrap gap-1.5">
              <span
                v-for="color in user.preferredColors"
                :key="color"
                class="inline-block px-2.5 py-0.5 bg-purple-50 text-purple-600 text-xs font-medium rounded-full"
              >
                {{ getColorLabel(color) }}
              </span>
            </dd>
          </div>

          <div v-if="user.preferredStyles.length > 0" class="py-3 border-t border-gray-50">
            <dt class="text-sm text-gray-500 mb-2">선호 스타일</dt>
            <dd class="flex flex-wrap gap-1.5">
              <span
                v-for="style in user.preferredStyles"
                :key="style"
                class="inline-block px-2.5 py-0.5 bg-purple-50 text-purple-600 text-xs font-medium rounded-full"
              >
                {{ style }}
              </span>
            </dd>
          </div>
        </dl>
      </div>

      <div class="space-y-2">
        <router-link
          v-if="!user.personalColor"
          to="/diagnosis"
          class="flex items-center justify-between w-full bg-gradient-to-r from-purple-600 to-purple-500 text-white rounded-xl px-5 py-4 hover:from-purple-700 hover:to-purple-600 transition-all"
        >
          <div>
            <p class="font-medium text-sm">AI 퍼스널컬러 진단</p>
            <p class="text-purple-200 text-xs mt-0.5">나에게 맞는 컬러를 찾아보세요</p>
          </div>
          <svg class="w-5 h-5 text-purple-200 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
          </svg>
        </router-link>

        <router-link
          v-else
          to="/diagnosis"
          class="flex items-center justify-between w-full bg-white border border-gray-100 rounded-xl px-5 py-4 hover:bg-gray-50 transition-colors"
        >
          <p class="text-sm font-medium text-gray-700">퍼스널컬러 재진단</p>
          <svg class="w-5 h-5 text-gray-300 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
          </svg>
        </router-link>

        <button
          type="button"
          class="flex items-center justify-between w-full bg-white border border-gray-100 rounded-xl px-5 py-4 hover:bg-gray-50 transition-colors"
          @click="handleLogout"
        >
          <p class="text-sm font-medium text-red-500">로그아웃</p>
          <svg class="w-5 h-5 text-gray-300 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"
            />
          </svg>
        </button>
      </div>
    </template>

    <Teleport to="body">
      <div
        v-if="showDiagnosisModal && diagnosisPreviewUrl"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60"
        @click.self="showDiagnosisModal = false"
      >
        <div class="bg-white rounded-2xl max-w-lg w-full overflow-hidden shadow-xl">
          <div class="flex items-center justify-between px-5 py-4 border-b border-gray-100">
            <h3 class="font-semibold text-gray-900">진단 이미지</h3>
            <button
              type="button"
              class="text-gray-400 hover:text-gray-600 transition-colors"
              @click="showDiagnosisModal = false"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <div class="p-4">
            <img
              :src="diagnosisPreviewUrl"
              alt="퍼스널컬러 진단 이미지"
              class="w-full rounded-lg"
            />
            <p v-if="user?.personalColor" class="text-center text-sm text-purple-600 font-medium mt-3">
              {{ formatPersonalColor(user?.personalColor ?? null) }}
            </p>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { COLOR_OPTIONS, formatPersonalColor } from '@/types'
import type { User, UserRole } from '@/types'

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
    case 'ADMIN': return 'bg-red-50 text-red-600'
    case 'SELLER': return 'bg-blue-50 text-blue-600'
    default: return 'bg-gray-100 text-gray-600'
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
