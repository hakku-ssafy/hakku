import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getProducts, getProduct } from '@/api/products'
import type { Product } from '@/types'

export const useProductStore = defineStore('products', () => {
  const products = ref<Product[]>([])
  const currentProduct = ref<Product | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchProducts() {
    loading.value = true
    error.value = null
    try {
      products.value = await getProducts()
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '오류가 발생했습니다'
    } finally {
      loading.value = false
    }
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

  return { products, currentProduct, loading, error, fetchProducts, fetchProduct }
})
