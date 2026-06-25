import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/vue'
import { createRouter, createMemoryHistory } from 'vue-router'
import AdminProductsView from '../AdminProductsView.vue'
import * as productsApi from '@/api/products'
import type { Product } from '@/types'

vi.mock('@/api/products')
const mockList = vi.mocked(productsApi.listAdminProducts)
const mockEdit = vi.mocked(productsApi.editAdminProduct)

function product(id: number, name: string, active = true): Product {
  return {
    id,
    name,
    description: '',
    price: 1000,
    category: '키링',
    imageUrl: `/product-images/${id}.jpg`,
    purchaseUrl: null,
    keyColor: null,
    subColor: null,
    colors: [],
    styles: ['기본'],
    sellerId: 1,
    active,
  }
}

let ioCallback: IntersectionObserverCallback | null = null

async function renderView() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/admin/products', component: AdminProductsView },
    ],
  })
  await router.push('/admin/products')
  await router.isReady()
  return render(AdminProductsView, { global: { plugins: [router] } })
}

describe('AdminProductsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    ioCallback = null
    vi.stubGlobal(
      'IntersectionObserver',
      class {
        constructor(cb: IntersectionObserverCallback) {
          ioCallback = cb
        }
        observe() {}
        unobserve() {}
        disconnect() {}
      },
    )
  })

  it('초기 로드 시 상품 목록과 이미지를 렌더한다', async () => {
    mockList.mockResolvedValue({ items: [product(3, '키링A'), product(2, '배지B')], nextCursor: 2, hasMore: true })

    await renderView()

    expect(await screen.findByDisplayValue('키링A')).toBeInTheDocument()
    expect(screen.getByDisplayValue('배지B')).toBeInTheDocument()
    expect(screen.getByAltText('키링A')).toHaveAttribute('src', '/product-images/3.jpg')
  })

  it('활성화 토글을 끄고 저장하면 active=false 로 수정 요청한다', async () => {
    mockList.mockResolvedValue({ items: [product(3, '키링A')], hasMore: false })
    mockEdit.mockResolvedValue({ ...product(3, '키링A'), active: false })

    await renderView()
    await screen.findByDisplayValue('키링A')

    await fireEvent.click(screen.getByLabelText('활성화'))
    await fireEvent.click(screen.getByRole('button', { name: '저장' }))

    await waitFor(() => {
      expect(mockEdit).toHaveBeenCalledWith(3, expect.objectContaining({ active: false }))
    })
  })

  it('이름을 인라인 수정하고 저장하면 새 이름으로 수정 요청한다', async () => {
    mockList.mockResolvedValue({ items: [product(3, '키링A')], hasMore: false })
    mockEdit.mockResolvedValue({ ...product(3, '바뀐이름') })

    await renderView()
    const nameInput = await screen.findByDisplayValue('키링A')

    await fireEvent.update(nameInput, '바뀐이름')
    await fireEvent.click(screen.getByRole('button', { name: '저장' }))

    await waitFor(() => {
      expect(mockEdit).toHaveBeenCalledWith(3, expect.objectContaining({ name: '바뀐이름' }))
    })
  })

  it('하단 센티넬이 보이면 커서로 다음 페이지를 이어 불러온다(무한 스크롤)', async () => {
    mockList
      .mockResolvedValueOnce({ items: [product(3, '키링A'), product(2, '배지B')], nextCursor: 2, hasMore: true })
      .mockResolvedValueOnce({ items: [product(1, '스티커C')], hasMore: false })

    await renderView()
    await screen.findByDisplayValue('키링A')

    expect(ioCallback).not.toBeNull()
    ioCallback?.([{ isIntersecting: true } as IntersectionObserverEntry], {} as IntersectionObserver)

    expect(await screen.findByDisplayValue('스티커C')).toBeInTheDocument()
    expect(mockList).toHaveBeenCalledTimes(2)
    expect(mockList.mock.calls[1][0]).toBe(2)
  })
})
