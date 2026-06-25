import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/vue'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory, type Router } from 'vue-router'
import ChatWindow from '../ChatWindow.vue'
import { useAuthStore } from '@/stores/auth'

const { compressImage } = vi.hoisted(() => ({ compressImage: vi.fn() }))
vi.mock('@/lib/compressImage', () => ({ compressImage }))

const blank = { template: '<div />' }

function makeRouter(): Router {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: blank },
      { path: '/login', component: blank },
      { path: '/products/:id', component: blank }
    ]
  })
}

/** [DONE] 한 줄만 보내고 닫는 최소 SSE 스트림. send() 가 예외 없이 끝나게 한다. */
function doneStream(): ReadableStream<Uint8Array> {
  const enc = new TextEncoder()
  return new ReadableStream<Uint8Array>({
    start(controller) {
      controller.enqueue(enc.encode('data: [DONE]\n\n'))
      controller.close()
    }
  })
}

async function renderAuthenticated(router: Router) {
  localStorage.setItem('accessToken', 'test-token')
  useAuthStore().init()
  await router.push('/')
  await router.isReady()
  return render(ChatWindow, { global: { plugins: [router] } })
}

describe('ChatWindow 첨부 이미지 자동 압축', () => {
  let router: Router

  beforeEach(() => {
    setActivePinia(createPinia())
    router = makeRouter()
    vi.clearAllMocks()
    vi.stubGlobal('URL', {
      ...URL,
      createObjectURL: vi.fn(() => 'blob:preview'),
      revokeObjectURL: vi.fn()
    })
    compressImage.mockImplementation((file: File) => Promise.resolve(file))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('첨부 이미지는 compressImage 로 줄인 뒤 업로드한다', async () => {
    const compressed = new File([new Uint8Array([9])], 'shot.webp', { type: 'image/webp' })
    compressImage.mockResolvedValue(compressed)
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 200, body: doneStream() })
    vi.stubGlobal('fetch', fetchMock)

    const { container } = await renderAuthenticated(router)

    const input = container.querySelector('input[type="file"]') as HTMLInputElement
    const original = new File([new Uint8Array([1, 2, 3])], 'shot.png', { type: 'image/png' })
    Object.defineProperty(input, 'files', { value: [original], configurable: true })
    await fireEvent.change(input)

    await fireEvent.click(screen.getByLabelText('전송'))

    await waitFor(() => expect(fetchMock).toHaveBeenCalled())
    expect(compressImage).toHaveBeenCalledWith(original)
    const body = fetchMock.mock.calls[0][1].body as FormData
    expect((body.get('image') as File).name).toBe('shot.webp')
    expect((body.get('image') as File).type).toBe('image/webp')
  })
})
