<template>
  <div class="u-container u-container--onboard onboard u-rise">
    <div class="onboard__head">
      <span class="u-eyebrow">Welcome</span>
      <h1 class="onboard__title">어떤 컬러를 좋아하세요?</h1>
      <p class="onboard__sub">선호 컬러를 고르면 더 잘 맞는 아이템을 추천해드려요.</p>
    </div>

    <div v-if="errorMessage" role="alert" class="onboard__error">{{ errorMessage }}</div>

    <!-- B3. 컬러 스와치 칩 — 좌측 원형 스와치(inset 링) + 이름 -->
    <div class="onboard__chips">
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

    <div class="onboard__actions">
      <AppButton variant="accent" size="lg" class="flex-1" :disabled="loading" :loading="loading" @click="handleSubmit">
        시작하기
      </AppButton>
      <button type="button" class="onboard__skip" :disabled="loading" @click="handleSkip">건너뛰기</button>
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
.onboard {
  padding-top: 56px;
  padding-bottom: 90px;
}
.onboard__head {
  text-align: center;
  margin-bottom: 34px;
}
.onboard__title {
  margin: 14px 0 10px;
  font-size: clamp(1.5rem, 1.25rem + 1.2vw, 1.75rem);
  font-weight: 700;
  letter-spacing: -0.03em;
}
.onboard__sub {
  margin: 0;
  font-size: 14px;
  color: var(--hk-text-muted);
}
.onboard__error {
  margin-bottom: 20px;
  padding: 0.75rem 0.875rem;
  background: var(--accent-soft, #f1efec);
  border: 1px solid var(--hk-border);
  border-radius: var(--hk-radius-md);
  color: var(--hk-ink);
  font-size: 0.875rem;
  text-align: center;
}
.onboard__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  margin-bottom: 34px;
}
.onboard__actions {
  display: flex;
  gap: 11px;
}
.onboard__skip {
  height: 54px;
  padding: 0 24px;
  border-radius: var(--hk-radius-md);
  background: var(--hk-surface);
  border: 1px solid var(--hk-border-control);
  color: var(--hk-text-muted);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.18s ease, color 0.18s ease;
}
.onboard__skip:hover:not(:disabled) {
  border-color: var(--hk-ink);
  color: var(--hk-ink);
}
.onboard__skip:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

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
