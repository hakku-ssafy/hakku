import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, signup, getMe, logout as apiLogout } from '@/api/auth'
import type { User } from '@/types'
import type { LoginRequest, SignupRequest } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const user = ref<User | null>(null)

  const isAuthenticated = computed(() => token.value !== null)

  function init() {
    const saved = localStorage.getItem('accessToken')
    if (saved) {
      token.value = saved
    }
  }

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('accessToken', newToken)
  }

  function logout() {
    // 서버 리프레시 쿠키 제거(베스트에포트) — 실패해도 로컬 상태는 항상 정리한다.
    void Promise.resolve(apiLogout()).catch(() => {})
    token.value = null
    user.value = null
    localStorage.removeItem('accessToken')
  }

  async function loginAction(request: LoginRequest) {
    const tokens = await login(request)
    setToken(tokens.accessToken)
  }

  async function signupAction(request: SignupRequest) {
    const tokens = await signup(request)
    setToken(tokens.accessToken)
  }

  async function fetchMe() {
    user.value = await getMe()
  }

  return {
    token,
    user,
    isAuthenticated,
    init,
    logout,
    loginAction,
    signupAction,
    fetchMe
  }
})
