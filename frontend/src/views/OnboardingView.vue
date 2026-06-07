<template>
  <div class="u-container flex items-center justify-center py-16 sm:py-24 min-h-[70vh]">
    <div class="w-full max-w-lg u-rise">
      <div class="text-center mb-9">
        <router-link to="/" class="u-serif text-3xl font-bold text-ink">학꾸</router-link>
        <p class="text-ink-muted text-sm mt-2">선호하는 컬러를 골라주세요</p>
      </div>

      <AppCard>
        <h1 class="u-serif text-2xl text-ink mb-1.5">컬러 취향 설정</h1>
        <p class="text-sm text-ink-soft mb-6">마음에 드는 컬러를 여러 개 선택할 수 있어요.</p>

        <div v-if="errorMessage" role="alert" class="mb-4 px-3.5 py-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
          {{ errorMessage }}
        </div>

        <div class="flex flex-wrap gap-2 mb-8">
          <button
            v-for="color in selectableColors"
            :key="color.value"
            type="button"
            class="px-4 py-2 rounded-full text-sm font-medium border transition-colors"
            :class="selectedColors.includes(color.value)
              ? 'border-ink bg-ink text-canvas'
              : 'border-line-strong text-ink-soft hover:border-ink-muted hover:text-ink'"
            @click="toggleColor(color.value)"
          >
            {{ color.label }}
          </button>
        </div>

        <AppButton block size="lg" :disabled="loading" :loading="loading" @click="handleSubmit">
          시작하기
        </AppButton>

        <p class="mt-4 text-center">
          <button type="button" class="text-sm text-ink-muted hover:text-ink transition-colors" @click="handleSkip">건너뛰기</button>
        </p>
      </AppCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import apiClient from '@/api/client'
import { COLOR_OPTIONS } from '@/types'
import type { User } from '@/types'
import AppCard from '@/components/ui/AppCard.vue'
import AppButton from '@/components/ui/AppButton.vue'

const router = useRouter()
const authStore = useAuthStore()

const selectableColors = COLOR_OPTIONS.filter((c) => c.value !== 'ALL')
const selectedColors = ref<string[]>([])
const loading = ref(false)
const errorMessage = ref('')

function toggleColor(value: string) {
  const idx = selectedColors.value.indexOf(value)
  if (idx >= 0) {
    selectedColors.value.splice(idx, 1)
  } else {
    selectedColors.value.push(value)
  }
}

async function handleSkip() {
  loading.value = true
  errorMessage.value = ''
  try {
    if (!authStore.user) {
      await authStore.fetchMe()
    }
    const nickname = authStore.user?.nickname ?? ''
    await apiClient.put<User>('/users/me', {
      nickname,
      preferredColors: authStore.user?.preferredColors ?? [],
      preferredStyles: authStore.user?.preferredStyles ?? [],
      onboardingCompleted: true
    })
    await authStore.fetchMe()
    router.push('/')
  } catch {
    errorMessage.value = '설정 저장에 실패했습니다.'
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  loading.value = true
  errorMessage.value = ''
  try {
    if (!authStore.user) {
      await authStore.fetchMe()
    }
    const nickname = authStore.user?.nickname ?? ''
    await apiClient.put<User>('/users/me', {
      nickname,
      preferredColors: selectedColors.value,
      preferredStyles: [],
      onboardingCompleted: true
    })
    await authStore.fetchMe()
    router.push('/')
  } catch {
    errorMessage.value = '설정 저장에 실패했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  if (!authStore.user) {
    await authStore.fetchMe()
  }
})
</script>
