import { describe, it, expect, vi, beforeEach } from 'vitest'
import apiClient from '../client'
import { addCartItem } from '../cart'

vi.mock('../client', () => ({
  default: { post: vi.fn() },
}))

describe('addCartItem', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('POST /cart/items 에 productId/quantity 를 보내고 생성된 항목을 반환한다', async () => {
    const created = { id: 1, productId: 5, productName: '키링', price: 4900, quantity: 2 }
    vi.mocked(apiClient.post).mockResolvedValue({ data: created })

    const result = await addCartItem(5, 2)

    expect(apiClient.post).toHaveBeenCalledWith('/cart/items', { productId: 5, quantity: 2 })
    expect(result).toEqual(created)
  })

  it('수량 미지정 시 기본 1을 보낸다', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: {} })

    await addCartItem(7)

    expect(apiClient.post).toHaveBeenCalledWith('/cart/items', { productId: 7, quantity: 1 })
  })
})
