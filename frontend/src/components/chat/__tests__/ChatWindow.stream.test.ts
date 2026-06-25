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

/**
 * SSE 본문을 게이트로 제어한다: 첫 청크는 즉시 방출하고, 나머지는 gate 가 풀린 뒤 방출한다.
 * 이렇게 하면 "스트림이 아직 끝나지 않은 상태"에서 첫 토큰이 화면에 보이는지 검증할 수 있다.
 */
function gatedStream(first: string[], gate: Promise<void>, rest: string[]): ReadableStream<Uint8Array> {
  const enc = new TextEncoder()
  return new ReadableStream<Uint8Array>({
    async start(controller) {
      for (const c of first) controller.enqueue(enc.encode(c))
      await gate
      for (const c of rest) controller.enqueue(enc.encode(c))
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

describe('ChatWindow 스트리밍 렌더', () => {
  let router: Router

  beforeEach(() => {
    setActivePinia(createPinia())
    router = makeRouter()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('토큰이 도착하면 스트림 종료 전에도 즉시 렌더된다', async () => {
    let release!: () => void
    const gate = new Promise<void>((resolve) => {
      release = resolve
    })
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      body: gatedStream(
        ['data: {"text":"답변"}\n\n'],
        gate,
        ['data: {"text":"완료"}\n\n', 'data: [DONE]\n\n'],
      ),
    })
    vi.stubGlobal('fetch', fetchMock)

    await renderAuthenticated(router)

    await fireEvent.update(screen.getByPlaceholderText('질문을 입력하세요…'), '질문있어요')
    await fireEvent.click(screen.getByLabelText('전송'))

    // 게이트가 아직 닫혀 있어 스트림은 미완료 상태다. 그래도 첫 토큰은 보여야 한다.
    // 버그(원시 객체 참조 변이)에서는 반응성이 트리거되지 않아 여기서 타임아웃 → RED.
    await waitFor(() => expect(screen.getByText('답변')).toBeInTheDocument())

    release()

    // 게이트 해제 후 누적된 전체 응답이 보인다.
    await waitFor(() => expect(screen.getByText('답변완료')).toBeInTheDocument())
  })
})
