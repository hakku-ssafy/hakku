import { describe, it, expect, vi, beforeEach } from 'vitest'
import apiClient from '../client'
import { listAdminProducts, editAdminProduct } from '../products'
import type { Product } from '@/types'

vi.mock('../client', () => ({
  default: { get: vi.fn(), patch: vi.fn() },
}))

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

describe('admin products api', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('listAdminProducts: 커서·limit 을 쿼리로 보내고 페이지를 반환한다', async () => {
    const page = { items: [product(3, '셋'), product(2, '둘')], nextCursor: 2, hasMore: true }
    vi.mocked(apiClient.get).mockResolvedValue({ data: page })

    const result = await listAdminProducts(5, 2)

    expect(apiClient.get).toHaveBeenCalledWith('/admin/products', { params: { limit: 2, cursorId: 5 } })
    expect(result.items).toHaveLength(2)
    expect(result.nextCursor).toBe(2)
    expect(result.hasMore).toBe(true)
  })

  it('listAdminProducts: 커서 없이 호출하면 cursorId 를 보내지 않는다', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { items: [], hasMore: false } })

    await listAdminProducts()

    expect(apiClient.get).toHaveBeenCalledWith('/admin/products', { params: { limit: 20 } })
  })

  it('editAdminProduct: PATCH /admin/products/:id 에 편집 필드를 보낸다', async () => {
    const edit = { name: '새이름', category: '핀뱃지', styles: ['빈티지'], active: false }
    vi.mocked(apiClient.patch).mockResolvedValue({ data: { ...product(7, '새이름', false), category: '핀뱃지', styles: ['빈티지'] } })

    const result = await editAdminProduct(7, edit)

    expect(apiClient.patch).toHaveBeenCalledWith('/admin/products/7', edit)
    expect(result.name).toBe('새이름')
    expect(result.active).toBe(false)
  })
})
