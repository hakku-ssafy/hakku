<template>
  <div class="max-w-7xl mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold text-gray-900 mb-6">맞춤 추천 상품</h1>

    <div v-if="loading" role="status" class="flex justify-center py-20">
      <svg class="animate-spin h-8 w-8 text-purple-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
      </svg>
    </div>

    <div v-else-if="error" class="text-center py-20 text-red-500">{{ error }}</div>

    <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      <router-link
        v-for="item in recommendations"
        :key="item.product.id"
        :to="`/products/${item.product.id}`"
        class="block"
      >
        <article class="bg-white rounded-xl border border-gray-100 overflow-hidden hover:shadow-md transition-shadow">
          <div class="aspect-square bg-gray-50 flex items-center justify-center">
            <img v-if="item.product.imageUrl" :src="item.product.imageUrl" :alt="item.product.name" class="w-full h-full object-cover" />
            <span v-else class="text-4xl">🎨</span>
          </div>
          <div class="p-3">
            <h2 class="text-sm font-medium text-gray-900 truncate">{{ item.product.name }}</h2>
            <p class="text-sm font-bold text-purple-600 mt-1">{{ formatPrice(item.product.price) }}원</p>
            <div class="flex items-center gap-1 mt-2">
              <span class="text-xs text-gray-400">추천점수</span>
              <span class="text-xs font-semibold text-purple-600">{{ item.score.toFixed(1) }}</span>
            </div>
          </div>
        </article>
      </router-link>
    </div>

    <div v-if="!loading && recommendations.length === 0" class="text-center py-20">
      <p class="text-gray-400 mb-4">아직 추천 상품이 없습니다</p>
      <router-link to="/diagnosis" class="px-6 py-3 bg-purple-600 text-white rounded-xl font-medium hover:bg-purple-700">
        퍼스널컬러 진단받기
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import apiClient from '@/api/client'
import type { RecommendationItem } from '@/types'

const recommendations = ref<RecommendationItem[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

function formatPrice(price: number): string {
  return price.toLocaleString('ko-KR')
}

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await apiClient.get<RecommendationItem[]>('/recommendations')
    recommendations.value = data
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : '추천 상품을 불러올 수 없습니다'
  } finally {
    loading.value = false
  }
})
</script>
