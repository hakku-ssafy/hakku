import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, signup, getMe } from '@/api/auth'
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
