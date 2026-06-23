/** 배송비 규칙(components.md C10): 3만원 미만 3,000원, 이상 무료. */
export const FREE_SHIPPING_THRESHOLD = 30000
export const SHIPPING_FEE = 3000

export interface CartLineItem {
  price: number
  quantity: number
}

export interface CartSummary {
  itemCount: number
  subtotal: number
  shipping: number
  total: number
}

/** 장바구니 항목들의 수량·상품금액·배송비·합계를 계산한다. 빈 카트는 배송비 0. */
export function calcCartSummary(items: CartLineItem[]): CartSummary {
  const itemCount = items.reduce((sum, item) => sum + item.quantity, 0)
  const subtotal = items.reduce((sum, item) => sum + item.price * item.quantity, 0)
  const shipping = subtotal === 0 || subtotal >= FREE_SHIPPING_THRESHOLD ? 0 : SHIPPING_FEE
  const total = subtotal + shipping
  return { itemCount, subtotal, shipping, total }
}
