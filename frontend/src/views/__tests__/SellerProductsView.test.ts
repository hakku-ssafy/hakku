import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/vue'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory, type Router } from 'vue-router'
import SellerProductsView from '../SellerProductsView.vue'
import { useAuthStore } from '@/stores/auth'
import * as productsApi from '@/api/products'
import * as storageApi from '@/api/storage'
import * as authApi from '@/api/auth'
import type { User, UserRole } from '@/types'

vi.mock('@/api/products')
vi.mock('@/api/storage')
vi.mock('@/api/auth')

const blank = { template: '<div />' }

function makeRouter(): Router {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: blank },
      { path: '/seller/products', component: SellerProductsView },
      { path: '/login', component: blank },
    ],
  })
}

function user(role: UserRole): User {
  return {
    id: 1,
    email: 'seller@hakku.com',
    nickname: '판매왕',
    role,
    personalColor: null,
    profileImageUrl: null,
    diagnosisImageUrl: null,
    preferredStyles: [],
    preferredColors: [],
    diagnosisStatus: 'NONE',
    onboardingCompleted: true,
  }
}

async function mountAs(role: UserRole): Promise<Router> {
  const router = makeRouter()
  await router.push('/seller/products')
  await router.isReady()
  const auth = useAuthStore()
  auth.token = 'test-token'
  auth.user = user(role)
  render(SellerProductsView, { global: { plugins: [router] } })
  return router
}

async function fillForm(): Promise<void> {
  await fireEvent.update(await screen.findByLabelText(/상품명/), '미니 키링')
  await fireEvent.update(screen.getByLabelText(/설명/), '귀여운 키링이에요')
  await fireEvent.update(screen.getByLabelText(/가격/), '4900')
  await fireEvent.update(screen.getByLabelText(/카테고리/), '키링')
  await fireEvent.click(screen.getByRole('button', { name: '레드' }))
  const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement
  const file = new File([new Uint8Array([1, 2, 3])], 'p.png', { type: 'image/png' })
  Object.defineProperty(fileInput, 'files', { value: [file], configurable: true })
  await fireEvent.change(fileInput)
}

describe('SellerProductsView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.stubGlobal('URL', { ...URL, createObjectURL: vi.fn(() => 'blob:preview'), revokeObjectURL: vi.fn() })
  })

  it('어드민도 상품 등록 폼에 접근할 수 있다', async () => {
    await mountAs('ADMIN')
    await waitFor(() => {
      expect(screen.getByLabelText(/상품명/)).toBeInTheDocument()
    })
    expect(screen.queryByText(/접근할 수 없습니다|권한이 없습니다/)).not.toBeInTheDocument()
  })

  it('일반(NORMAL) 회원은 홈으로 리다이렉트된다', async () => {
    const router = await mountAs('NORMAL')
    await waitFor(() => expect(router.currentRoute.value.path).toBe('/'))
  })

  it('프로필 조회에 실패하면 예외를 전파하지 않고 홈으로 리다이렉트한다', async () => {
    vi.mocked(authApi.getMe).mockRejectedValue(new Error('401'))
    const router = makeRouter()
    await router.push('/seller/products')
    await router.isReady()
    const auth = useAuthStore()
    auth.token = 'test-token'
    auth.user = null

    render(SellerProductsView, { global: { plugins: [router] } })

    await waitFor(() => expect(router.currentRoute.value.path).toBe('/'))
  })

  it('이미지 업로드 실패 시 업로드 전용 메시지를 보여주고 상품 등록은 호출하지 않는다', async () => {
    vi.mocked(storageApi.uploadProductImage).mockRejectedValue(new Error('401'))
    await mountAs('SELLER')
    await fillForm()

    await fireEvent.click(screen.getByRole('button', { name: '상품 등록' }))

    expect(await screen.findByText(/이미지 업로드에 실패/)).toBeInTheDocument()
    expect(productsApi.createProduct).not.toHaveBeenCalled()
  })

  it('상품 등록 API 실패 시 등록 전용 메시지를 보여준다', async () => {
    vi.mocked(storageApi.uploadProductImage).mockResolvedValue('https://img/p.png')
    vi.mocked(productsApi.createProduct).mockRejectedValue(new Error('400'))
    await mountAs('SELLER')
    await fillForm()

    await fireEvent.click(screen.getByRole('button', { name: '상품 등록' }))

    expect(await screen.findByText(/상품 등록에 실패/)).toBeInTheDocument()
    expect(screen.getByText(/입력 정보를 확인/)).toBeInTheDocument()
  })

  it('정상 등록 시 업로드한 이미지 URL 로 createProduct 를 호출한다', async () => {
    vi.mocked(storageApi.uploadProductImage).mockResolvedValue('https://img/p.png')
    vi.mocked(productsApi.createProduct).mockResolvedValue({} as never)
    await mountAs('SELLER')
    await fillForm()

    await fireEvent.click(screen.getByRole('button', { name: '상품 등록' }))

    await waitFor(() =>
      expect(productsApi.createProduct).toHaveBeenCalledWith(
        expect.objectContaining({ name: '미니 키링', category: '키링', imageUrl: 'https://img/p.png' }),
      ),
    )
    expect(await screen.findByText(/상품이 등록되었습니다/)).toBeInTheDocument()
  })
})
