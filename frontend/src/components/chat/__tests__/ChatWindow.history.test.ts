import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/vue'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory, type Router } from 'vue-router'
import ChatWindow from '../ChatWindow.vue'
import { useAuthStore } from '@/stores/auth'

const blank = { template: '<div />' }

function makeRouter(): Router {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: blank },
      { path: '/login', component: blank },
      { path: '/products/:id', component: blank },
    ],
  })
}

async function renderAuthenticated(router: Router) {
  localStorage.setItem('accessToken', 'test-token')
  useAuthStore().init()
  await router.push('/')
  await router.isReady()
  return render(ChatWindow, { global: { plugins: [router] } })
}

describe('ChatWindow 대화 복원', () => {
  let router: Router

  beforeEach(() => {
    setActivePinia(createPinia())
    router = makeRouter()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('열릴 때 서버의 최근 대화(/chat/history)를 불러와 복원한다', async () => {
    const fetchMock = vi.fn((url: string) => {
      if (url === '/chat/history') {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => ({
            messages: [
              { role: 'user', content: '이전 질문' },
              { role: 'assistant', content: '이전 답변' },
            ],
          }),
        })
      }
      return Promise.reject(new Error(`unmocked: ${url}`))
    })
    vi.stubGlobal('fetch', fetchMock)

    await renderAuthenticated(router)

    await waitFor(() => expect(screen.getByText('이전 질문')).toBeInTheDocument())
    expect(screen.getByText('이전 답변')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/chat/history', expect.objectContaining({ method: 'GET' }))
  })

  it('히스토리가 비어 있으면 빈 상태로 시작한다', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ messages: [] }),
    })
    vi.stubGlobal('fetch', fetchMock)

    await renderAuthenticated(router)

    await waitFor(() =>
      expect(screen.getByText(/꾸미기 방법을 추천해드려요/)).toBeInTheDocument(),
    )
  })
})
