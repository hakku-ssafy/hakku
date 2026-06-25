import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, signup, getMe, logout as apiLogout } from '@/api/auth'
import { clearAll as clearResourceCache } from '@/lib/resourceCache'
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
    clearResourceCache() // 다른 계정에 이전 사용자 캐시가 노출되지 않도록 비운다.
  }

  async function loginAction(request: LoginRequest) {
    const tokens = await login(request)
    clearResourceCache() // 이전 세션 캐시 제거 후 새 세션 시작
    setToken(tokens.accessToken)
  }

  async function signupAction(request: SignupRequest) {
    const tokens = await signup(request)
    clearResourceCache()
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
