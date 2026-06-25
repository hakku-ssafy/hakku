import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/vue'
import { createRouter, createMemoryHistory } from 'vue-router'
import MagazineDetailView from '../MagazineDetailView.vue'
import * as magazineApi from '@/api/magazine'
import * as productsApi from '@/api/products'
import type { Magazine, Product } from '@/types'

vi.mock('@/api/magazine')
vi.mock('@/api/products')

const mockGetMagazine = vi.mocked(magazineApi.getMagazine)
const mockGetProduct = vi.mocked(productsApi.getProduct)

const product: Product = {
  id: 7,
  name: '데코 스티커',
  description: '',
  price: 4500,
  category: '꾸미기 스티커',
  imageUrl: '/img/7.jpg',
  purchaseUrl: null,
  keyColor: null,
  subColor: null,
  colors: [],
  styles: [],
  sellerId: 1,
}

const magazine: Magazine = {
  id: 1,
  kicker: 'EDITORIAL',
  title: '이주의 다꾸',
  subtitle: '겨울 감성 데코',
  content: '소개 문단\n\n/products/7',
  coverImageUrl: null,
  displayOrder: 0,
  published: true,
  createdAt: '2026-06-25T00:00:00Z',
  updatedAt: '2026-06-25T00:00:00Z',
}

async function renderView() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/magazine/:id', component: MagazineDetailView },
      { path: '/products/:id', component: { template: '<div />' } },
    ],
  })
  await router.push('/magazine/1')
  await router.isReady()
  return render(MagazineDetailView, { global: { plugins: [router] } })
}

describe('MagazineDetailView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('매거진 제목·본문과 임베드된 상품 카드를 렌더링한다', async () => {
    mockGetMagazine.mockResolvedValue(magazine)
    mockGetProduct.mockResolvedValue(product)

    await renderView()

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: '이주의 다꾸' })).toBeInTheDocument()
    })
    expect(screen.getByText('소개 문단')).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.getByText('데코 스티커')).toBeInTheDocument()
    })
    expect(mockGetProduct).toHaveBeenCalledWith(7)
  })

  it('없는 매거진이면 에러 메시지를 보여준다', async () => {
    mockGetMagazine.mockRejectedValue(new Error('404'))

    await renderView()

    await waitFor(() => {
      expect(screen.getByText('콘텐츠를 찾을 수 없습니다.')).toBeInTheDocument()
    })
  })
})
