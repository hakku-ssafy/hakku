import apiClient from './client'
import type { CartItem } from '@/types'

/** 상품을 장바구니에 담는다(인증 필요). 같은 상품이면 서버가 수량을 합산한다. */
export async function addCartItem(productId: number, quantity = 1): Promise<CartItem> {
  const { data } = await apiClient.post<CartItem>('/cart/items', { productId, quantity })
  return data
}
