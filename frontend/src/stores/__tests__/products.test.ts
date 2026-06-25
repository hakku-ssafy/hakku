import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useProductStore } from '../products'
import { clearAll } from '@/lib/resourceCache'
import * as apiModule from '@/api/products'

vi.mock('@/api/products')

const mockGetProducts = vi.mocked(apiModule.getProducts)
const mockGetProduct = vi.mocked(apiModule.getProduct)

const mockProduct = {
  id: 1,
  name: '봄 스티커 세트',
  description: '라이트 스프링을 위한 파스텔 스티커',
  price: 5900,
  category: '꾸미기 스티커',
  imageUrl: null,
  keyColor: 'SPRING',
  subColor: null,
  colors: ['blue'],
  styles: ['CUTE'],
  sellerId: 10,
  purchaseUrl: null
}

describe('useProductStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    clearAll() // 모듈 레벨 SWR 캐시 격리
  })

  it('초기 상태는 빈 목록이다', () => {
    const store = useProductStore()
    expect(store.products).toHaveLength(0)
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  it('상품 목록을 불러온다', async () => {
    mockGetProducts.mockResolvedValueOnce([mockProduct])
    const store = useProductStore()

    await store.fetchProducts()

    expect(store.products).toHaveLength(1)
    expect(store.products[0].name).toBe('봄 스티커 세트')
    expect(store.loading).toBe(false)
  })

  it('상품 목록 로딩 중 loading이 true이다', async () => {
    let resolve: (v: typeof mockProduct[]) => void
    mockGetProducts.mockReturnValueOnce(new Promise(r => { resolve = r }))
    const store = useProductStore()

    const fetchPromise = store.fetchProducts()
    expect(store.loading).toBe(true)

    resolve!([])
    await fetchPromise
    expect(store.loading).toBe(false)
  })

  it('상품 상세를 불러온다', async () => {
    mockGetProduct.mockResolvedValueOnce(mockProduct)
    const store = useProductStore()

    await store.fetchProduct(1)

    expect(store.currentProduct).toEqual(mockProduct)
  })

  it('API 오류 시 error 상태를 설정한다', async () => {
    mockGetProducts.mockRejectedValueOnce(new Error('서버 오류'))
    const store = useProductStore()

    await store.fetchProducts()

    expect(store.error).toBe('서버 오류')
    expect(store.products).toHaveLength(0)
  })

  it('재방문 시 캐시를 즉시 보여주고, staleTime 이내면 다시 받지 않는다(SWR)', async () => {
    vi.useFakeTimers()
    mockGetProducts.mockResolvedValue([mockProduct])
    const store = useProductStore()

    await store.fetchProducts() // 첫 방문 → 네트워크
    await store.fetchProducts() // 재방문 → 캐시(fresh)

    expect(mockGetProducts).toHaveBeenCalledTimes(1)
    expect(store.loading).toBe(false)
    expect(store.products).toHaveLength(1)
    vi.useRealTimers()
  })

  it('staleTime 이 지나면 캐시를 보여준 뒤 백그라운드로 다시 받는다(SWR)', async () => {
    vi.useFakeTimers()
    mockGetProducts.mockResolvedValue([mockProduct])
    const store = useProductStore()

    await store.fetchProducts()
    vi.advanceTimersByTime(31_000) // staleTime(30s) 초과
    await store.fetchProducts()

    expect(mockGetProducts).toHaveBeenCalledTimes(2)
    vi.useRealTimers()
  })
})
