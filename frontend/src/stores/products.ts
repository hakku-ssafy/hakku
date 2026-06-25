import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getProducts, getProduct } from '@/api/products'
import { getEntry, setEntry, isFresh, invalidate, dedupe } from '@/lib/resourceCache'
import type { Product } from '@/types'

const PRODUCTS_KEY = 'products'
const STALE_TIME = 30_000

export const useProductStore = defineStore('products', () => {
  const products = ref<Product[]>([])
  const currentProduct = ref<Product | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  /**
   * 상품 목록 — SWR. 캐시가 있으면 즉시 보여주고(스피너 X), staleTime 이내면 네트워크 생략,
   * 지났으면 캐시를 보여준 채 백그라운드로 갱신한다. 캐시가 없을 때만 loading 을 띄운다.
   */
  async function fetchProducts() {
    const cached = getEntry<Product[]>(PRODUCTS_KEY)
    if (cached) {
      products.value = cached.data
      error.value = null
      if (isFresh(PRODUCTS_KEY, STALE_TIME)) return
    } else {
      loading.value = true
      error.value = null
    }
    try {
      const data = await dedupe(PRODUCTS_KEY, getProducts)
      products.value = data
      setEntry(PRODUCTS_KEY, data)
      error.value = null
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '오류가 발생했습니다'
    } finally {
      loading.value = false
    }
  }

  /** 상품이 추가/수정/삭제된 뒤 호출 → 다음 목록 방문 시 최신을 받는다. */
  function invalidateProducts() {
    invalidate(PRODUCTS_KEY)
  }

  async function fetchProduct(id: number) {
    loading.value = true
    error.value = null
    try {
      currentProduct.value = await getProduct(id)
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '오류가 발생했습니다'
    } finally {
      loading.value = false
    }
  }

  return { products, currentProduct, loading, error, fetchProducts, fetchProduct, invalidateProducts }
})
