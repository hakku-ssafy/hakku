import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../auth'
import { setEntry, getEntry, clearAll } from '@/lib/resourceCache'
import * as apiModule from '@/api/auth'

vi.mock('@/api/auth')

const mockLogin = vi.mocked(apiModule.login)
const mockSignup = vi.mocked(apiModule.signup)

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
    clearAll() // 모듈 레벨 SWR 캐시 격리
  })

  it('초기 상태는 미인증이다', () => {
    const store = useAuthStore()
    expect(store.isAuthenticated).toBe(false)
    expect(store.token).toBeNull()
    expect(store.user).toBeNull()
  })

  it('로컬스토리지에 토큰이 있으면 초기화 시 인증 상태가 된다', () => {
    localStorage.setItem('accessToken', 'saved-token')
    const store = useAuthStore()
    store.init()
    expect(store.isAuthenticated).toBe(true)
    expect(store.token).toBe('saved-token')
  })

  it('로그인 성공 시 토큰을 저장하고 인증 상태가 된다', async () => {
    mockLogin.mockResolvedValueOnce({ accessToken: 'new-token' })
    const store = useAuthStore()

    await store.loginAction({ email: 'test@example.com', password: 'pass123' })

    expect(store.isAuthenticated).toBe(true)
    expect(store.token).toBe('new-token')
    expect(localStorage.getItem('accessToken')).toBe('new-token')
  })

  it('로그인 실패 시 에러를 반환한다', async () => {
    mockLogin.mockRejectedValueOnce(new Error('Invalid credentials'))
    const store = useAuthStore()

    await expect(
      store.loginAction({ email: 'bad@example.com', password: 'wrong' })
    ).rejects.toThrow('Invalid credentials')

    expect(store.isAuthenticated).toBe(false)
  })

  it('회원가입 성공 시 토큰을 저장하고 인증 상태가 된다', async () => {
    mockSignup.mockResolvedValueOnce({ accessToken: 'signup-token' })
    const store = useAuthStore()

    await store.signupAction({
      email: 'new@example.com',
      password: 'pass123',
      nickname: '학꾸유저',
      role: 'NORMAL'
    })

    expect(store.isAuthenticated).toBe(true)
    expect(store.token).toBe('signup-token')
  })

  it('로그아웃 시 토큰을 제거하고 미인증 상태가 된다', async () => {
    mockLogin.mockResolvedValueOnce({ accessToken: 'token' })
    const store = useAuthStore()
    await store.loginAction({ email: 'test@example.com', password: 'pass' })

    store.logout()

    expect(store.isAuthenticated).toBe(false)
    expect(store.token).toBeNull()
    expect(localStorage.getItem('accessToken')).toBeNull()
  })

  it('로그아웃 시 SWR 캐시를 비운다(다른 계정 데이터 노출 방지)', () => {
    const store = useAuthStore()
    setEntry('products', [{ id: 1 }])
    setEntry('mypage:orders:5', [{ id: 99 }])

    store.logout()

    expect(getEntry('products')).toBeUndefined()
    expect(getEntry('mypage:orders:5')).toBeUndefined()
  })
})
