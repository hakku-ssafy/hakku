import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/vue'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import LoginView from '../LoginView.vue'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/auth')

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: '/login', component: LoginView },
    { path: '/home', component: { template: '<div>Home</div>' } }
  ]
})

describe('LoginView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('이메일과 비밀번호 입력 폼이 렌더링된다', () => {
    render(LoginView, {
      global: { plugins: [createPinia(), router] }
    })

    expect(screen.getByLabelText(/이메일/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/비밀번호/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /로그인/i })).toBeInTheDocument()
  })

  it('입력값이 없으면 버튼이 비활성화된다', () => {
    render(LoginView, {
      global: { plugins: [createPinia(), router] }
    })

    expect(screen.getByRole('button', { name: /로그인/i })).toBeDisabled()
  })

  it('이메일과 비밀번호 입력 시 버튼이 활성화된다', async () => {
    render(LoginView, {
      global: { plugins: [createPinia(), router] }
    })

    await fireEvent.update(screen.getByLabelText(/이메일/i), 'test@example.com')
    await fireEvent.update(screen.getByLabelText(/비밀번호/i), 'password123')

    expect(screen.getByRole('button', { name: /로그인/i })).not.toBeDisabled()
  })

  it('로그인 실패 시 오류 메시지가 표시된다', async () => {
    const pinia = createPinia()
    render(LoginView, {
      global: { plugins: [pinia, router] }
    })
    const store = useAuthStore(pinia)
    vi.spyOn(store, 'loginAction').mockRejectedValueOnce(new Error('이메일 또는 비밀번호가 틀렸습니다'))

    await fireEvent.update(screen.getByLabelText(/이메일/i), 'bad@example.com')
    await fireEvent.update(screen.getByLabelText(/비밀번호/i), 'wrong')
    await fireEvent.click(screen.getByRole('button', { name: /로그인/i }))

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument()
    })
  })
})
