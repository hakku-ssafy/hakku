<template>
  <div class="u-container flex items-center justify-center py-16 sm:py-24 min-h-[70vh]">
    <div class="w-full max-w-sm u-rise">
      <div class="text-center mb-9">
        <router-link to="/" class="u-serif text-3xl font-bold text-ink">학꾸</router-link>
        <p class="text-ink-muted text-sm mt-2">AI 퍼스널컬러 꾸미기 플랫폼</p>
      </div>

      <AppCard>
        <h1 class="u-serif text-2xl text-ink mb-6">회원가입</h1>

        <div
          v-if="errorMessage"
          role="alert"
          class="mb-5 px-3.5 py-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm"
        >
          {{ errorMessage }}
        </div>

        <form class="space-y-4" @submit.prevent="handleSignup">
          <AppInput v-model="email" label="이메일" type="email" autocomplete="email" />
          <AppInput v-model="nickname" label="닉네임" type="text" />
          <AppInput v-model="password" label="비밀번호" type="password" autocomplete="new-password" />
          <div>
            <AppInput
              v-model="passwordConfirm"
              label="비밀번호 확인"
              type="password"
              autocomplete="new-password"
              placeholder="비밀번호를 한 번 더 입력하세요"
            />
            <p v-if="passwordMismatch" class="mt-1.5 text-xs text-red-500">비밀번호가 일치하지 않아요.</p>
          </div>

          <div>
            <span class="block text-sm font-medium text-ink mb-2">가입 유형</span>
            <div class="grid grid-cols-2 gap-3">
              <label
                v-for="opt in roleOptions"
                :key="opt.value"
                class="flex items-center gap-2 p-3.5 border rounded-lg cursor-pointer transition-colors"
                :class="role === opt.value
                  ? 'border-ink bg-ink text-canvas'
                  : 'border-line-strong text-ink-soft hover:border-ink-muted'"
              >
                <input v-model="role" type="radio" :value="opt.value" class="sr-only" />
                <span class="text-lg" aria-hidden="true">{{ opt.icon }}</span>
                <span class="text-sm font-medium">{{ opt.label }}</span>
              </label>
            </div>
          </div>

          <AppButton type="submit" block size="lg" :disabled="!canSubmit || loading" :loading="loading">
            회원가입
          </AppButton>
        </form>

        <p class="mt-6 text-center text-sm text-ink-muted">
          이미 계정이 있으신가요?
          <router-link to="/login" class="text-ink font-medium hover:underline underline-offset-4">로그인</router-link>
        </p>
      </AppCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authErrorMessage, isValidEmail } from '@/lib/authError'
import type { UserRole } from '@/types'
import AppCard from '@/components/ui/AppCard.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppButton from '@/components/ui/AppButton.vue'

const router = useRouter()
const authStore = useAuthStore()

const email = ref('')
const nickname = ref('')
const password = ref('')
const passwordConfirm = ref('')
const role = ref<UserRole>('NORMAL')
const loading = ref(false)
const errorMessage = ref('')

const passwordMismatch = computed(
  () => passwordConfirm.value !== '' && password.value !== passwordConfirm.value,
)

const roleOptions = [
  { value: 'NORMAL' as UserRole, label: '일반 회원', icon: '🛍️' },
  { value: 'SELLER' as UserRole, label: '판매자', icon: '🏪' }
]

const canSubmit = computed(
  () =>
    email.value.trim() !== '' &&
    nickname.value.trim() !== '' &&
    password.value.trim() !== '' &&
    passwordConfirm.value.trim() !== '' &&
    password.value === passwordConfirm.value,
)

async function handleSignup() {
  if (!canSubmit.value) return
  if (!isValidEmail(email.value)) {
    errorMessage.value = '올바른 이메일 형식이 아니에요. 예: name@example.com'
    return
  }
  if (password.value !== passwordConfirm.value) {
    errorMessage.value = '비밀번호가 일치하지 않아요. 두 비밀번호를 같게 입력해 주세요.'
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    await authStore.signupAction({
      email: email.value,
      nickname: nickname.value,
      password: password.value,
      role: role.value
    })
    await authStore.fetchMe()
    if (role.value === 'SELLER') {
      router.push('/seller/products')
    } else {
      router.push('/onboarding')
    }
  } catch (e: unknown) {
    errorMessage.value = authErrorMessage(e, 'signup')
  } finally {
    loading.value = false
  }
}
</script>
