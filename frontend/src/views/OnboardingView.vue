<template>
  <div class="u-container u-container--onboard flex items-center justify-center py-16 sm:py-24 min-h-[70vh]">
    <div class="w-full u-rise">
      <div class="text-center mb-8">
        <router-link to="/" class="u-serif text-[1.75rem] text-ink">학꾸</router-link>
        <p class="u-eyebrow mt-3 text-ink-muted">Color Preference</p>
      </div>

      <AppCard>
        <h1 class="u-serif text-[1.5rem] text-ink mb-1.5">컬러 취향 설정</h1>
        <p class="text-sm text-ink-soft mb-6">마음에 드는 컬러를 여러 개 선택할 수 있어요.</p>

        <div v-if="errorMessage" role="alert" class="mb-4 px-3.5 py-3 bg-accent-soft border border-line rounded-md text-ink text-sm">
          {{ errorMessage }}
        </div>

        <!-- B3. 컬러 스와치 칩 — 좌측 원형 스와치(inset 링) + 이름 -->
        <div class="flex flex-wrap gap-2 mb-8">
          <button
            v-for="color in selectableColors"
            :key="color.value"
            type="button"
            class="hk-swatch-chip"
            :class="selectedColors.includes(color.value) ? 'is-selected' : ''"
            :aria-pressed="selectedColors.includes(color.value)"
            @click="toggleColor(color.value)"
          >
            <span class="hk-swatch" :style="{ background: swatchHex[color.value] ?? 'var(--hk-cream)' }" aria-hidden="true" />
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

// 스와치 칩의 좌측 원형 도트 색(표현용) — 웜 뉴트럴 팔레트에 맞춘 톤. 로직과 무관.
const swatchHex: Record<string, string> = {
  red: '#d98a82',
  orange: '#e79b82',
  yellow: '#f0de9e',
  green: '#afd8c8',
  blue: '#a9c8e0',
  purple: '#c7bce6',
  pink: '#e7a6be',
  brown: '#9a7b63',
}

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

<style scoped>
/* B3. 컬러 스와치 칩 — 알약, 좌측 원형 스와치 + 이름. 선택=1.5px 먹색 + paper-selected */
.hk-swatch-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 16px 0 12px;
  border: 1.5px solid var(--hk-border);
  border-radius: var(--hk-radius-pill);
  background: var(--hk-surface);
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--hk-ink);
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease;
}
.hk-swatch-chip:hover {
  border-color: var(--hk-border-control);
}
.hk-swatch-chip.is-selected {
  border-color: var(--hk-border-strong);
  background: var(--hk-paper-selected);
  font-weight: 600;
}
.hk-swatch {
  width: 16px;
  height: 16px;
  border-radius: var(--hk-radius-pill);
  box-shadow: var(--hk-ring-inset);
  flex: 0 0 auto;
}
</style>
