<template>
  <div class="u-container u-container--auth flex items-center justify-center py-16 sm:py-24 min-h-[70vh]">
    <div class="w-full u-rise">
      <div class="text-center mb-8">
        <router-link to="/" class="auth-logo">hakku<span class="text-accent">.</span></router-link>
        <p class="auth-subtitle">학꾸와 함께 시작해요</p>
      </div>

      <AppCard>
        <!-- B5. 세그먼트 토글 — 로그인/회원가입 전환 -->
        <nav class="hk-seg mb-7" aria-label="인증 전환">
          <router-link to="/login" class="hk-seg__item">로그인</router-link>
          <span class="hk-seg__item is-active" aria-current="page">회원가입</span>
        </nav>

        <h1 class="u-serif text-[1.5rem] text-ink mb-6">회원가입</h1>

        <div
          v-if="errorMessage"
          role="alert"
          class="mb-5 px-3.5 py-3 bg-accent-soft border border-line rounded-md text-ink text-sm"
        >
          {{ errorMessage }}
        </div>

        <form class="space-y-4" @submit.prevent="handleSignup">
          <AppInput v-model="email" label="이메일" type="email" autocomplete="email" />
          <AppInput v-model="nickname" label="닉네임" type="text" />
          <AppInput
            v-model="password"
            label="비밀번호"
            type="password"
            autocomplete="new-password"
            latin-only
          />
          <div>
            <AppInput
              v-model="passwordConfirm"
              label="비밀번호 확인"
              type="password"
              autocomplete="new-password"
              placeholder="비밀번호를 한 번 더 입력하세요"
              latin-only
            />
            <p v-if="passwordMismatch" class="mt-1.5 text-xs text-accent-ink">비밀번호가 일치하지 않아요.</p>
          </div>

          <!-- B6. 역할 선택 카드 — 선택=1.5px 먹색 보더 + paper-selected 배경 -->
          <div>
            <span class="block text-sm font-medium text-ink mb-2">가입 유형</span>
            <div class="grid grid-cols-2 gap-3">
              <label
                v-for="opt in roleOptions"
                :key="opt.value"
                class="hk-role"
                :class="role === opt.value ? 'is-selected' : ''"
              >
                <input v-model="role" type="radio" :value="opt.value" class="sr-only" />
                <span class="hk-role__icon" aria-hidden="true">{{ opt.icon }}</span>
                <span class="hk-role__title">{{ opt.label }}</span>
                <span class="hk-role__desc">
                  {{ opt.value === 'SELLER' ? '상품을 등록하고 판매해요' : '추천받고 꾸미기를 즐겨요' }}
                </span>
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

/* B5. 세그먼트 토글 */
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

/* B6. 역할 선택 카드 — 선택=1.5px 먹색 보더 + paper-selected, 제목 700/13.5px + 설명 11.5px */
.hk-role {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 14px;
  cursor: pointer;
  background: var(--hk-surface);
  border: 1.5px solid var(--hk-border);
  border-radius: var(--hk-radius-lg);
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease;
}
.hk-role:hover {
  border-color: var(--hk-border-control);
}
.hk-role.is-selected {
  border-color: var(--hk-border-strong);
  background: var(--hk-paper-selected);
}
.hk-role__icon {
  font-size: 1.125rem;
  line-height: 1;
  margin-bottom: 2px;
}
.hk-role__title {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--hk-ink);
}
.hk-role__desc {
  font-size: 11.5px;
  line-height: 1.35;
  color: var(--hk-text-muted-2);
}
</style>
