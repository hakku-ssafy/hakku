<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 px-4">
    <div class="w-full max-w-md">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-purple-600">학꾸</h1>
        <p class="text-gray-500 mt-2">퍼스널컬러 꾸미기 플랫폼</p>
      </div>

      <div class="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
        <h2 class="text-xl font-semibold text-gray-900 mb-6">회원가입</h2>

        <div v-if="errorMessage" role="alert" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
          {{ errorMessage }}
        </div>

        <form @submit.prevent="handleSignup" class="space-y-5">
          <div>
            <label for="email" class="block text-sm font-medium text-gray-700 mb-1">이메일</label>
            <input id="email" v-model="email" type="email" autocomplete="email"
              class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent" />
          </div>

          <div>
            <label for="nickname" class="block text-sm font-medium text-gray-700 mb-1">닉네임</label>
            <input id="nickname" v-model="nickname" type="text"
              class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent" />
          </div>

          <div>
            <label for="password" class="block text-sm font-medium text-gray-700 mb-1">비밀번호</label>
            <input id="password" v-model="password" type="password" autocomplete="new-password"
              class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent" />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">가입 유형</label>
            <div class="grid grid-cols-2 gap-3">
              <label
                v-for="opt in roleOptions"
                :key="opt.value"
                class="flex items-center gap-2 p-3 border rounded-lg cursor-pointer transition-colors"
                :class="role === opt.value ? 'border-purple-500 bg-purple-50' : 'border-gray-200 hover:border-purple-300'"
              >
                <input type="radio" :value="opt.value" v-model="role" class="sr-only" />
                <span class="text-lg">{{ opt.icon }}</span>
                <span class="text-sm font-medium">{{ opt.label }}</span>
              </label>
            </div>
          </div>

          <button
            type="submit"
            :disabled="!canSubmit || loading"
            class="w-full py-3 bg-purple-600 text-white rounded-lg font-medium hover:bg-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            <span v-if="loading">가입 중...</span>
            <span v-else>회원가입</span>
          </button>
        </form>

        <p class="mt-4 text-center text-sm text-gray-500">
          이미 계정이 있으신가요?
          <router-link to="/login" class="text-purple-600 font-medium hover:underline">로그인</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { UserRole } from '@/types'

const router = useRouter()
const authStore = useAuthStore()

const email = ref('')
const nickname = ref('')
const password = ref('')
const role = ref<UserRole>('NORMAL')
const loading = ref(false)
const errorMessage = ref('')

const roleOptions = [
  { value: 'NORMAL' as UserRole, label: '일반 회원', icon: '🛍️' },
  { value: 'SELLER' as UserRole, label: '판매자', icon: '🏪' }
]

const canSubmit = computed(
  () => email.value.trim() !== '' && nickname.value.trim() !== '' && password.value.trim() !== ''
)

async function handleSignup() {
  if (!canSubmit.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    await authStore.signupAction({
      email: email.value,
      nickname: nickname.value,
      password: password.value,
      role: role.value
    })
    router.push('/')
  } catch (e: unknown) {
    errorMessage.value = e instanceof Error ? e.message : '회원가입에 실패했습니다'
  } finally {
    loading.value = false
  }
}
</script>
