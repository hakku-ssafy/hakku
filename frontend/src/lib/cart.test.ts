import { describe, it, expect } from 'vitest'
import { calcCartSummary } from './cart'

describe('calcCartSummary', () => {
  const items = [{ price: 10000, quantity: 2 }, { price: 5000, quantity: 1 }] // 25,000
  it('sums item totals and counts', () => {
    const s = calcCartSummary(items)
    expect(s.itemCount).toBe(3)
    expect(s.subtotal).toBe(25000)
  })
  it('charges 3000 shipping under 30000', () => {
    expect(calcCartSummary(items).shipping).toBe(3000)
    expect(calcCartSummary(items).total).toBe(28000)
  })
  it('free shipping at/over 30000', () => {
    const s = calcCartSummary([{ price: 30000, quantity: 1 }])
    expect(s.shipping).toBe(0)
    expect(s.total).toBe(30000)
  })
  it('handles empty cart', () => {
    const s = calcCartSummary([])
    expect(s).toEqual({ itemCount: 0, subtotal: 0, shipping: 0, total: 0 })
  })
})
