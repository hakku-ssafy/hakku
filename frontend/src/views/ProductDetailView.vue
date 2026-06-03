<template>
  <div class="max-w-3xl mx-auto px-4 py-8">
    <router-link
      to="/products"
      class="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-purple-600 mb-6 transition-colors"
    >
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
      </svg>
      상품 목록
    </router-link>

    <div v-if="loading" role="status" class="flex justify-center py-20">
      <svg class="animate-spin h-8 w-8 text-purple-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
      </svg>
    </div>

    <div v-else-if="errorMessage" role="alert" class="text-center py-20 text-red-500">
      {{ errorMessage }}
    </div>

    <article v-else-if="product" class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <div class="aspect-square sm:aspect-[4/3] bg-gray-50 flex items-center justify-center">
        <img
          v-if="product.imageUrl"
          :src="product.imageUrl"
          :alt="product.name"
          class="w-full h-full object-cover"
        />
        <span v-else class="text-6xl">🎨</span>
      </div>
      <div class="p-6">
        <div class="flex items-start justify-between gap-4 mb-3">
          <h1 class="text-2xl font-bold text-gray-900">{{ product.name }}</h1>
          <span
            v-if="product.category"
            class="shrink-0 px-2.5 py-0.5 bg-purple-50 text-purple-600 text-xs font-medium rounded-full"
          >
            {{ product.category }}
          </span>
        </div>
        <p class="text-xl font-bold text-purple-600 mb-4">{{ formatPrice(product.price) }}원</p>
        <div v-if="product.colors.length > 0" class="flex flex-wrap gap-1.5 mb-4">
          <span
            v-for="color in product.colors"
            :key="color"
            class="inline-block px-2.5 py-0.5 bg-gray-100 text-gray-600 text-xs font-medium rounded-full"
          >
            {{ getColorLabel(color) }}
          </span>
        </div>
        <p class="text-gray-600 leading-relaxed whitespace-pre-wrap mb-6">{{ product.description }}</p>
        <a
          v-if="product.purchaseUrl"
          :href="product.purchaseUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="inline-flex items-center justify-center w-full py-3 bg-purple-600 text-white rounded-lg font-medium hover:bg-purple-700 transition-colors"
        >
          구매 링크 열기
        </a>
        <p v-else class="text-sm text-gray-400 text-center">구매 링크가 아직 등록되지 않았습니다.</p>
      </div>
    </article>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getProduct } from '@/api/products'
import { COLOR_OPTIONS } from '@/types'
import type { Product } from '@/types'

const route = useRoute()

const product = ref<Product | null>(null)
const loading = ref(false)
const errorMessage = ref('')

function formatPrice(price: number): string {
  return price.toLocaleString('ko-KR')
}

function getColorLabel(value: string): string {
  return COLOR_OPTIONS.find((c) => c.value === value)?.label ?? value
}

onMounted(async () => {
  const id = Number(route.params.id)
  if (Number.isNaN(id)) {
    errorMessage.value = '잘못된 상품 ID입니다.'
    return
  }
  loading.value = true
  try {
    product.value = await getProduct(id)
  } catch {
    errorMessage.value = '상품을 불러오는데 실패했습니다.'
  } finally {
    loading.value = false
  }
})
</script>
