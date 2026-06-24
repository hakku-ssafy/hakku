import { describe, it, expect, vi, beforeEach } from 'vitest'
import apiClient from '../client'
import { createOrder, getMyOrders } from '../orders'
import type { ShippingAddress } from '@/types'

vi.mock('../client', () => ({
  default: { post: vi.fn(), get: vi.fn() },
}))

const address: ShippingAddress = {
  recipientName: '홍길동',
  phone: '010-1234-5678',
  postalCode: '06236',
  address1: '서울 강남구 테헤란로 1',
  address2: '101동 1001호',
}

describe('orders api', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('createOrder: POST /orders 에 배송지를 보내고 생성된 주문을 반환한다', async () => {
    const created = { id: 1, status: 'CREATED', totalAmount: 9800, items: [], ...address }
    vi.mocked(apiClient.post).mockResolvedValue({ data: created })

    const result = await createOrder(address)

    expect(apiClient.post).toHaveBeenCalledWith('/orders', address)
    expect(result).toEqual(created)
  })

  it('getMyOrders: GET /orders 로 내 주문 목록을 반환한다', async () => {
    const orders = [{ id: 1, status: 'PAID', totalAmount: 9800, items: [] }]
    vi.mocked(apiClient.get).mockResolvedValue({ data: orders })

    const result = await getMyOrders()

    expect(apiClient.get).toHaveBeenCalledWith('/orders')
    expect(result).toEqual(orders)
  })
})
