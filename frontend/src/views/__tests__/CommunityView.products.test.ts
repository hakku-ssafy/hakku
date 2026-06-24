import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/vue'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory, type Router } from 'vue-router'
import CommunityView from '../CommunityView.vue'
import { useAuthStore } from '@/stores/auth'
import * as postsApi from '@/api/posts'
import * as productsApi from '@/api/products'
import * as storageApi from '@/api/storage'
import type { Product } from '@/types'

vi.mock('@/api/posts')
vi.mock('@/api/products')
vi.mock('@/api/storage')

const blank = { template: '<div />' }

function makeRouter(): Router {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: blank },
      { path: '/community', component: blank },
      { path: '/community/:id', component: blank },
      { path: '/products/:id', component: blank },
      { path: '/seller/products', component: blank },
      { path: '/login', component: blank },
    ],
  })
}

function product(id: number, name: string): Product {
  return {
    id, name, description: '', price: 4900, category: '키링', imageUrl: 'https://img/' + id + '.png',
    purchaseUrl: null, keyColor: null, subColor: null, colors: [], styles: [], sellerId: 1,
  }
}

describe('CommunityView 학생증 자랑 연관 상품 첨부', () => {
  let router: Router

  beforeEach(async () => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.stubGlobal('URL', { ...URL, createObjectURL: vi.fn(() => 'blob:preview'), revokeObjectURL: vi.fn() })
    vi.mocked(postsApi.getPosts).mockResolvedValue([])
    vi.mocked(postsApi.createPost).mockResolvedValue({
      id: 99, title: '가을 다꾸', content: '가을 다꾸', board: 'STUDENT_ID',
      imageUrl: 'https://img/card.png', authorId: 1, authorNickname: 'me',
      likeCount: 0, commentCount: 0, liked: false, createdAt: '2026-06-24T00:00:00Z',
      relatedProducts: [],
    })
    vi.mocked(productsApi.getProducts).mockResolvedValue([product(7, '다꾸 키링'), product(8, '미니 스티커')])
    vi.mocked(storageApi.uploadShowcaseImage).mockResolvedValue('https://img/card.png')

    router = makeRouter()
    await router.push('/community?board=showcase')
    await router.isReady()
  })

  it('상품을 검색해 추가하고 등록하면 createPost 에 productIds 가 포함된다', async () => {
    const auth = useAuthStore()
    auth.token = 'test-token'
    render(CommunityView, { global: { plugins: [router] } })

    // 작성 폼 열기 → 연관 상품 후보 로드
    await fireEvent.click(screen.getByRole('button', { name: '학생증 자랑하기' }))
    await waitFor(() => expect(productsApi.getProducts).toHaveBeenCalled())

    // 제목
    await fireEvent.update(screen.getByPlaceholderText(/한 줄 소개/), '가을 다꾸 학생증')

    // 학생증 이미지
    const file = new File(['x'], 'card.png', { type: 'image/png' })
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement
    Object.defineProperty(fileInput, 'files', { value: [file], configurable: true })
    await fireEvent.change(fileInput)

    // 연관 상품 검색 → 결과에서 추가
    await fireEvent.update(screen.getByPlaceholderText(/상품명으로 검색/), '키링')
    await fireEvent.click(await screen.findByRole('button', { name: /다꾸 키링/ }))

    // 선택 목록에 노출
    expect(screen.getByText('다꾸 키링')).toBeInTheDocument()

    // 등록
    await fireEvent.click(screen.getByRole('button', { name: '등록' }))

    await waitFor(() =>
      expect(postsApi.createPost).toHaveBeenCalledWith(
        expect.objectContaining({ board: 'STUDENT_ID', productIds: [7] }),
      ),
    )
  })
})
