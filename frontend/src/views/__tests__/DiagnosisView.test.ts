import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/vue'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory, type Router } from 'vue-router'
import DiagnosisView from '../DiagnosisView.vue'
import { useAuthStore } from '@/stores/auth'
import type { User } from '@/types'

// aiClient 는 모듈 내부에서 axios.create() 로 만든다. 그 post 를 가로채고,
// useAuthedImage 가 쓰는 axios.get / isAxiosError 도 안전하게 채운다.
const { post, compressImage } = vi.hoisted(() => ({ post: vi.fn(), compressImage: vi.fn() }))
vi.mock('axios', () => {
  // api/client.ts 가 apiClient.interceptors.request.use 를 호출하므로
  // create() 가 interceptors 까지 갖춘 클라이언트를 돌려줘야 모듈 로드가 깨지지 않는다.
  const client = {
    post,
    get: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } }
  }
  return { default: { create: () => client, isAxiosError: () => false, get: vi.fn() } }
})
vi.mock('@/lib/compressImage', () => ({ compressImage }))
vi.mock('@/api/auth')

const blank = { template: '<div />' }

function makeRouter(): Router {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: blank },
      { path: '/diagnosis', component: DiagnosisView },
      { path: '/login', component: blank }
    ]
  })
}

function user(): User {
  return {
    id: 1,
    email: 'u@hakku.com',
    nickname: '진단러',
    role: 'NORMAL',
    personalColor: null,
    profileImageUrl: null,
    diagnosisImageUrl: null,
    preferredStyles: [],
    preferredColors: [],
    diagnosisStatus: 'NONE',
    onboardingCompleted: true
  }
}

async function mountView(): Promise<void> {
  const router = makeRouter()
  await router.push('/diagnosis')
  await router.isReady()
  const auth = useAuthStore()
  auth.token = 'test-token'
  auth.user = user()
  render(DiagnosisView, { global: { plugins: [router] } })
}

async function attachFile(): Promise<File> {
  const input = document.querySelector('input[type="file"]') as HTMLInputElement
  const file = new File([new Uint8Array([1, 2, 3])], 'face.png', { type: 'image/png' })
  Object.defineProperty(input, 'files', { value: [file], configurable: true })
  await fireEvent.change(input)
  return file
}

describe('DiagnosisView 업로드 전 자동 압축', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.stubGlobal('URL', {
      ...URL,
      createObjectURL: vi.fn(() => 'blob:preview'),
      revokeObjectURL: vi.fn()
    })
    post.mockResolvedValue({})
    compressImage.mockImplementation((file: File) => Promise.resolve(file))
  })

  it('진단 사진을 compressImage 로 줄인 뒤 AI 서버로 업로드한다', async () => {
    const compressed = new File([new Uint8Array([9])], 'face.webp', { type: 'image/webp' })
    compressImage.mockResolvedValue(compressed)

    await mountView()
    const original = await attachFile()
    await fireEvent.click(await screen.findByRole('button', { name: '진단 시작' }))

    await waitFor(() => expect(post).toHaveBeenCalled())
    expect(compressImage).toHaveBeenCalledWith(original)
    const body = post.mock.calls[0][1] as FormData
    expect((body.get('image') as File).name).toBe('face.webp')
    expect((body.get('image') as File).type).toBe('image/webp')
  })
})
