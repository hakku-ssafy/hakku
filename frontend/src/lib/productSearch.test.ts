import { describe, it, expect } from 'vitest'
import { filterProductsByQuery } from './productSearch'
import type { Product } from '@/types'

function product(overrides: Partial<Product> & Pick<Product, 'id' | 'name'>): Product {
  return {
    description: '',
    price: 1000,
    category: null,
    imageUrl: null,
    purchaseUrl: null,
    keyColor: null,
    subColor: null,
    colors: [],
    styles: [],
    sellerId: 1,
    ...overrides,
  }
}

const products: Product[] = [
  product({ id: 1, name: '봄 스티커 세트', category: '꾸미기 스티커' }),
  product({ id: 2, name: '겨울 배지 세트', category: '핀뱃지' }),
  product({ id: 3, name: '키링 - 다꾸 키트', category: '키링', description: '퍼스널컬러 추천' }),
]

describe('filterProductsByQuery', () => {
  it('빈 검색어면 전체 목록을 그대로 반환한다', () => {
    expect(filterProductsByQuery(products, '')).toEqual(products)
    expect(filterProductsByQuery(products, '   ')).toEqual(products)
  })

  it('상품명으로 부분 일치(대소문자 무시) 필터링한다', () => {
    const result = filterProductsByQuery(products, '스티커')
    expect(result).toHaveLength(1)
    expect(result[0].id).toBe(1)
  })

  it('검색어 앞뒤 공백을 무시한다', () => {
    const result = filterProductsByQuery(products, '  배지  ')
    expect(result.map((p) => p.id)).toEqual([2])
  })

  it('카테고리와 설명으로도 매칭한다', () => {
    expect(filterProductsByQuery(products, '키링').map((p) => p.id)).toEqual([3])
    expect(filterProductsByQuery(products, '퍼스널컬러').map((p) => p.id)).toEqual([3])
  })

  it('일치하는 항목이 없으면 빈 배열을 반환한다', () => {
    expect(filterProductsByQuery(products, '존재하지않는상품')).toEqual([])
  })
})
