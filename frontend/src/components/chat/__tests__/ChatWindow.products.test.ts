import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/vue'
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

function sseStream(chunks: string[]): ReadableStream<Uint8Array> {
  const enc = new TextEncoder()
  return new ReadableStream<Uint8Array>({
    start(controller) {
      for (const c of chunks) controller.enqueue(enc.encode(c))
      controller.close()
    },
  })
}

async function renderAuthenticated(router: Router) {
  localStorage.setItem('accessToken', 'test-token')
  useAuthStore().init()
  await router.push('/')
  await router.isReady()
  return render(ChatWindow, { global: { plugins: [router] } })
}

const emptyHistory = { ok: true, status: 200, json: async () => ({ messages: [] }) }

describe('ChatWindow 상품 카드 · 이름', () => {
  let router: Router

  beforeEach(() => {
    setActivePinia(createPinia())
    router = makeRouter()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('스트림의 products 이벤트를 받아 답변 아래 상품 카드를 렌더한다', async () => {
    const fetchMock = vi.fn((url: string) => {
      if (url === '/chat/history') return Promise.resolve(emptyHistory)
      return Promise.resolve({
        ok: true,
        status: 200,
        body: sseStream([
          'data: {"text":"이런 상품 어때요?"}\n\n',
          'data: {"products":[{"id":9,"name":"그립톡","price":5900,"imageUrl":"/img/9.jpg"}]}\n\n',
          'data: [DONE]\n\n',
        ]),
      })
    })
    vi.stubGlobal('fetch', fetchMock)

    await renderAuthenticated(router)
    await fireEvent.update(screen.getByPlaceholderText('질문을 입력하세요…'), '상품 추천')
    await fireEvent.click(screen.getByLabelText('전송'))

    await waitFor(() => expect(screen.getByText('그립톡')).toBeInTheDocument())
    expect(screen.getByText(/5,900/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /그립톡/ })).toHaveAttribute('href', '/products/9')
  })

  it('헤더에 학꾸AI 이름을 보여준다', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(emptyHistory))
    await renderAuthenticated(router)
    expect(screen.getByText('학꾸AI')).toBeInTheDocument()
  })
})
