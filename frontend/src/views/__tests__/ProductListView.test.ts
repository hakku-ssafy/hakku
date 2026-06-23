import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/vue'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import ProductListView from '../ProductListView.vue'
import * as productsApi from '@/api/products'

vi.mock('@/api/products')
const mockGetProducts = vi.mocked(productsApi.getProducts)

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: '/products', component: ProductListView },
    { path: '/products/:id', component: { template: '<div>Detail</div>' } }
  ]
})

const mockProducts = [
  {
    id: 1,
    name: '봄 스티커 세트',
    description: '파스텔 스티커',
    price: 5900,
    category: '꾸미기 스티커',
    imageUrl: null,
    keyColor: 'SPRING',
    subColor: null,
    colors: ['pink'],
    styles: ['CUTE'],
    sellerId: 10,
  purchaseUrl: null
  },
  {
    id: 2,
    name: '겨울 배지 세트',
    description: '쿨톤 배지',
    price: 7900,
    category: '핀뱃지',
    imageUrl: null,
    keyColor: 'WINTER',
    subColor: null,
    colors: ['blue'],
    styles: ['COOL'],
    sellerId: 10,
  purchaseUrl: null
  }
]

describe('ProductListView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    mockGetProducts.mockResolvedValue([])
  })

  it('상품 목록 제목이 렌더링된다', async () => {
    render(ProductListView, {
      global: { plugins: [createPinia(), router] }
    })

    expect(screen.getByRole('heading', { name: /꾸미기 아이템/ })).toBeInTheDocument()
  })

  it('로딩 중에 스피너가 표시된다', async () => {
    let resolve: (v: typeof mockProducts) => void
    mockGetProducts.mockReturnValueOnce(new Promise(r => { resolve = r }))
    render(ProductListView, {
      global: { plugins: [createPinia(), router] }
    })

    await waitFor(() => {
      expect(screen.getByRole('status')).toBeInTheDocument()
    })
    resolve!([])
  })

  it('상품 목록이 렌더링된다', async () => {
    mockGetProducts.mockResolvedValueOnce(mockProducts)
    render(ProductListView, {
      global: { plugins: [createPinia(), router] }
    })

    await waitFor(() => {
      expect(screen.getByText('봄 스티커 세트')).toBeInTheDocument()
      expect(screen.getByText('겨울 배지 세트')).toBeInTheDocument()
    })
  })

  it('상품 가격이 포맷팅되어 표시된다', async () => {
    mockGetProducts.mockResolvedValueOnce([mockProducts[0]])
    render(ProductListView, {
      global: { plugins: [createPinia(), router] }
    })

    await waitFor(() => {
      expect(screen.getByText(/5,900/)).toBeInTheDocument()
    })
  })
})
