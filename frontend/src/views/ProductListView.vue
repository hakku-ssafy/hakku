<template>
  <div class="max-w-7xl mx-auto px-4 py-8">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-gray-900">상품 목록</h1>
    </div>

    <div v-if="store.loading" role="status" class="flex justify-center py-20">
      <svg class="animate-spin h-8 w-8 text-purple-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
      </svg>
    </div>

    <div v-else-if="store.error" class="text-center py-20 text-red-500">
      {{ store.error }}
    </div>

    <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      <router-link
        v-for="product in store.products"
        :key="product.id"
        :to="`/products/${product.id}`"
        class="group block"
      >
        <article class="bg-white rounded-xl border border-gray-100 overflow-hidden hover:shadow-md transition-shadow">
          <div class="aspect-square bg-gray-50 flex items-center justify-center">
            <img
              v-if="product.imageUrl"
              :src="product.imageUrl"
              :alt="product.name"
              class="w-full h-full object-cover"
            />
            <span v-else class="text-4xl">🎨</span>
          </div>
          <div class="p-3">
            <h2 class="text-sm font-medium text-gray-900 truncate">{{ product.name }}</h2>
            <p class="text-sm font-bold text-purple-600 mt-1">{{ formatPrice(product.price) }}원</p>
          </div>
        </article>
      </router-link>
    </div>

    <div v-if="!store.loading && store.products.length === 0" class="text-center py-20 text-gray-400">
      등록된 상품이 없습니다
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useProductStore } from '@/stores/products'

const store = useProductStore()

function formatPrice(price: number): string {
  return price.toLocaleString('ko-KR')
}

onMounted(() => {
  store.fetchProducts()
})
</script>
