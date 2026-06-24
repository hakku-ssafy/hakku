import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  // 리프레시 토큰 httpOnly 쿠키를 주고받기 위해 자격증명 전송을 켠다(same-origin 이지만 명시).
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
})

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 인증 시도 자체(로그인/회원가입)와 리프레시 호출의 401은 재시도/로그아웃 대상에서 제외한다.
const AUTH_ENDPOINTS = ['/auth/login', '/auth/signup', '/auth/refresh']

// 동시 401 폭주 시 리프레시는 한 번만 수행하고 나머지 요청은 그 결과를 공유한다.
let refreshPromise: Promise<string> | null = null

function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = axios
      .post<{ accessToken: string }>(`${API_BASE_URL}/auth/refresh`, null, { withCredentials: true })
      .then((response) => {
        const newToken = response.data.accessToken
        localStorage.setItem('accessToken', newToken)
        return newToken
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

function forceLogout() {
  localStorage.removeItem('accessToken')
  window.location.href = '/login'
}

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const status = error.response?.status
    const original = error.config as RetriableConfig | undefined
    const requestUrl = original?.url ?? ''
    const isAuthAttempt = AUTH_ENDPOINTS.some((path) => requestUrl.includes(path))
    const hasToken = localStorage.getItem('accessToken') !== null

    // 인증된 요청이 만료/무효 토큰으로 401 → 리프레시 후 원요청을 1회 재시도(무중단 세션).
    if (status === 401 && !isAuthAttempt && hasToken && original && !original._retried) {
      original._retried = true
      try {
        const newToken = await refreshAccessToken()
        original.headers.Authorization = `Bearer ${newToken}`
        return apiClient(original)
      } catch {
        // 리프레시도 실패 → 세션 종료. 여기서 로그아웃하고 더는 흐르지 않게 한다.
        forceLogout()
        return Promise.reject(error)
      }
    }

    // 재시도 후에도 401 이거나 리프레시 불가 → 깔끔히 로그아웃.
    if (status === 401 && !isAuthAttempt && hasToken) {
      forceLogout()
    }
    return Promise.reject(error)
  }
)

export default apiClient
