<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 px-4">
    <div class="w-full max-w-lg">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-purple-600">학꾸</h1>
        <p class="text-gray-500 mt-2">선호하는 컬러를 골라주세요</p>
      </div>

      <div class="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
        <h2 class="text-xl font-semibold text-gray-900 mb-2">컬러 취향 설정</h2>
        <p class="text-sm text-gray-500 mb-6">마음에 드는 컬러를 여러 개 선택할 수 있어요</p>

        <div v-if="errorMessage" role="alert" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
          {{ errorMessage }}
        </div>

        <div class="flex flex-wrap gap-2 mb-8">
          <button
            v-for="color in selectableColors"
            :key="color.value"
            type="button"
            class="px-4 py-2 rounded-full text-sm font-medium border transition-colors"
            :class="selectedColors.includes(color.value)
              ? 'border-purple-500 bg-purple-50 text-purple-700'
              : 'border-gray-200 text-gray-600 hover:border-purple-300 hover:bg-purple-50/50'"
            @click="toggleColor(color.value)"
          >
            {{ color.label }}
          </button>
        </div>

        <button
          type="button"
          :disabled="loading"
          class="w-full py-3 bg-purple-600 text-white rounded-lg font-medium hover:bg-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          @click="handleSubmit"
        >
          <span v-if="loading">저장 중...</span>
          <span v-else>시작하기</span>
        </button>

        <p class="mt-4 text-center">
          <button type="button" class="text-sm text-gray-400 hover:text-purple-600 transition-colors" @click="handleSkip">건너뛰기</button>
        </p>
      </div>
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
