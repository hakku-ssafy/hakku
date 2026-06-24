import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/vue'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory, type Router } from 'vue-router'
import ProductDetailView from '../ProductDetailView.vue'
import { useAuthStore } from '@/stores/auth'
import * as productsApi from '@/api/products'
import * as wishlistApi from '@/api/wishlist'
import * as cartApi from '@/api/cart'
import type { Product } from '@/types'

vi.mock('@/api/products')
vi.mock('@/api/wishlist')
vi.mock('@/api/cart')

const blank = { template: '<div />' }

const product: Product = {
  id: 1,
  name: '다꾸 키링',
  description: '귀여운 키링',
  price: 4900,
  category: '키링',
  imageUrl: null,
  purchaseUrl: 'https://example.com/buy', // 예전 '구매처로 이동' 링크가 노출될 조건
  keyColor: null,
  subColor: null,
  colors: [],
  styles: [],
  sellerId: 9,
}

function makeRouter(): Router {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/products/:id', component: ProductDetailView },
      { path: '/products', component: blank },
      { path: '/login', component: blank },
      { path: '/cart', component: blank },
      { path: '/payments/checkout', component: blank },
    ],
  })
}

describe('ProductDetailView 장바구니 담기', () => {
  let router: Router

  beforeEach(async () => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.mocked(productsApi.getProduct).mockResolvedValue(product)
    vi.mocked(productsApi.getReviews).mockResolvedValue([])
    vi.mocked(wishlistApi.getWishlistStatus).mockResolvedValue({ wishlisted: false, wishlistCount: 0 })
    vi.mocked(cartApi.addCartItem).mockResolvedValue({
      id: 11, productId: 1, productName: '다꾸 키링', price: 4900, quantity: 1,
    })
    router = makeRouter()
    await router.push('/products/1')
    await router.isReady()
  })

  it("'구매처로 이동' 버튼이 더 이상 없다", async () => {
    useAuthStore().token = 'test-token'
    render(ProductDetailView, { global: { plugins: [router] } })

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: '다꾸 키링' })).toBeInTheDocument()
    })
    expect(screen.queryByText(/구매처로 이동/)).not.toBeInTheDocument()
  })

  it("'장바구니 담기' 버튼 클릭 시 addCartItem 을 호출한다", async () => {
    useAuthStore().token = 'test-token'
    render(ProductDetailView, { global: { plugins: [router] } })

    const button = await screen.findByRole('button', { name: /장바구니 담기/ })
    await fireEvent.click(button)

    await waitFor(() => {
      expect(cartApi.addCartItem).toHaveBeenCalledWith(1)
    })
  })

  it('비로그인 상태에서 장바구니 담기를 누르면 로그인으로 이동한다', async () => {
    render(ProductDetailView, { global: { plugins: [router] } })

    const button = await screen.findByRole('button', { name: /장바구니 담기/ })
    await fireEvent.click(button)

    await waitFor(() => {
      expect(router.currentRoute.value.path).toBe('/login')
    })
    expect(cartApi.addCartItem).not.toHaveBeenCalled()
  })
})
