<template>
  <div class="u-container flex items-center justify-center py-16 sm:py-24 min-h-[70vh]">
    <div class="w-full max-w-sm u-rise">
      <div class="text-center mb-9">
        <router-link to="/" class="u-serif text-3xl font-bold text-ink">학꾸</router-link>
        <p class="text-ink-muted text-sm mt-2">AI 퍼스널컬러 꾸미기 플랫폼</p>
      </div>

      <AppCard>
        <h1 class="u-serif text-2xl text-ink mb-6">로그인</h1>

        <div
          v-if="errorMessage"
          role="alert"
          class="mb-5 px-3.5 py-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm"
        >
          {{ errorMessage }}
        </div>

        <form class="space-y-4" @submit.prevent="handleLogin">
          <AppInput
            v-model="email"
            label="이메일"
            type="email"
            autocomplete="email"
            placeholder="example@email.com"
          />
          <AppInput
            v-model="password"
            label="비밀번호"
            type="password"
            autocomplete="current-password"
            placeholder="비밀번호를 입력하세요"
          />
          <AppButton type="submit" block size="lg" :disabled="!canSubmit || loading" :loading="loading">
            로그인
          </AppButton>
        </form>

        <p class="mt-6 text-center text-sm text-ink-muted">
          계정이 없으신가요?
          <router-link to="/signup" class="text-ink font-medium hover:underline underline-offset-4">회원가입</router-link>
        </p>
      </AppCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppCard from '@/components/ui/AppCard.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppButton from '@/components/ui/AppButton.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const loading = ref(false)
const errorMessage = ref('')

const canSubmit = computed(() => email.value.trim() !== '' && password.value.trim() !== '')

async function handleLogin() {
  if (!canSubmit.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    await authStore.loginAction({ email: email.value, password: password.value })
    await authStore.fetchMe()
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : null
    if (authStore.user?.role === 'NORMAL' && !authStore.user.onboardingCompleted) {
      router.push('/onboarding')
    } else {
      router.push(redirect ?? '/')
    }
  } catch (e: unknown) {
    errorMessage.value = e instanceof Error ? e.message : '로그인에 실패했습니다'
  } finally {
    loading.value = false
  }
}
</script>
