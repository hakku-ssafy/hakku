import apiClient from './client'
import type { Order, ShippingAddress } from '@/types'

/** 현재 장바구니를 스냅샷해 배송지와 함께 주문을 생성한다(인증 필요). */
export async function createOrder(address: ShippingAddress): Promise<Order> {
  const { data } = await apiClient.post<Order>('/orders', address)
  return data
}

/** 내 주문 내역을 최신순으로 조회한다(인증 필요). */
export async function getMyOrders(): Promise<Order[]> {
  const { data } = await apiClient.get<Order[]>('/orders')
  return data
}
