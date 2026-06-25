<template>
  <div class="u-container u-container--auth flex items-center justify-center py-16 sm:py-24 min-h-[70vh]">
    <div class="w-full u-rise">
      <div class="text-center mb-8">
        <router-link to="/" class="auth-logo">hakku<span class="text-accent">.</span></router-link>
        <p class="auth-subtitle">다시 만나서 반가워요</p>
      </div>

      <AppCard>
        <!-- B5. 세그먼트 토글 — 로그인/회원가입 전환 -->
        <nav class="hk-seg mb-7" aria-label="인증 전환">
          <span class="hk-seg__item is-active" aria-current="page">로그인</span>
          <router-link to="/signup" class="hk-seg__item">회원가입</router-link>
        </nav>

        <h1 class="u-serif text-[1.5rem] text-ink mb-6">로그인</h1>

        <div
          v-if="errorMessage"
          role="alert"
          class="mb-5 px-3.5 py-3 bg-accent-soft border border-line rounded-md text-ink text-sm"
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
            latin-only
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
import { authErrorMessage, isValidEmail } from '@/lib/authError'
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
  if (!isValidEmail(email.value)) {
    errorMessage.value = '올바른 이메일 형식이 아니에요. 예: name@example.com'
    return
  }
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
    errorMessage.value = authErrorMessage(e, 'login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-logo {
  font-weight: 800;
  font-size: 1.875rem;
  letter-spacing: -0.04em;
  color: var(--hk-ink);
}
.auth-subtitle {
  margin-top: 12px;
  font-size: 14px;
  color: var(--hk-text-muted);
}

/* B5. 세그먼트 토글 — #f0ebe2 트랙(알약, 패딩 4px), 활성=흰 배경+먹색, 비활성=투명+ink-muted */
.hk-seg {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  padding: 4px;
  background: var(--hk-track);
  border-radius: var(--hk-radius-pill);
}
.hk-seg__item {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 38px;
  border-radius: var(--hk-radius-pill);
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--hk-text-muted-2);
  text-decoration: none;
  transition:
    color 0.18s ease,
    background-color 0.18s ease;
}
.hk-seg__item:hover {
  color: var(--hk-ink);
}
.hk-seg__item.is-active {
  background: var(--hk-surface);
  color: var(--hk-ink);
}
</style>
