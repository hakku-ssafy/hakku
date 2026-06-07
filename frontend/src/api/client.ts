import axios from 'axios'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
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

// 로그인/회원가입 등 인증 시도 자체의 401은 리다이렉트하지 않는다.
// (잘못된 자격증명으로 인한 401은 화면에서 에러 메시지로 처리되어야 한다)
const AUTH_ENDPOINTS = ['/auth/login', '/auth/signup']

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const requestUrl: string = error.config?.url ?? ''
    const isAuthAttempt = AUTH_ENDPOINTS.some((path) => requestUrl.includes(path))

    // 인증된 요청이 만료/무효 토큰으로 401을 받은 경우에만 로그인으로 보낸다.
    if (status === 401 && !isAuthAttempt && localStorage.getItem('accessToken')) {
      localStorage.removeItem('accessToken')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default apiClient
