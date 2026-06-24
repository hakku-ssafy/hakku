import type { Product } from '@/types'

/**
 * 상품 목록을 검색어로 필터링한다(클라이언트 사이드).
 * 상품명·카테고리·설명에 대해 대소문자 무시 부분 일치로 매칭하며,
 * 빈/공백 검색어는 전체 목록을 그대로 반환한다.
 */
export function filterProductsByQuery(products: Product[], query: string): Product[] {
  const needle = query.trim().toLowerCase()
  if (needle === '') return products

  return products.filter((product) => {
    const haystack = [product.name, product.category ?? '', product.description ?? '']
      .join(' ')
      .toLowerCase()
    return haystack.includes(needle)
  })
}
