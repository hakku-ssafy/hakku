<template>
  <div class="max-w-7xl mx-auto px-4 py-8">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-gray-900">상품 목록</h1>
    </div>

    <section v-if="showRecommendations" class="mb-10">
      <h2 class="text-lg font-bold text-gray-900 mb-4">
        {{ recommendationTitle }}
      </h2>
      <div v-if="recLoading" class="flex justify-center py-12">
        <svg class="animate-spin h-8 w-8 text-purple-600" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
        </svg>
      </div>
      <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        <router-link
          v-for="item in recommendedProducts"
          :key="'rec-' + item.product.id"
          :to="`/products/${item.product.id}`"
          class="group block"
        >
          <article class="bg-white rounded-xl border-2 border-purple-100 overflow-hidden hover:shadow-md transition-shadow">
            <div class="aspect-square bg-gray-50 flex items-center justify-center relative">
              <img v-if="item.product.imageUrl" :src="item.product.imageUrl" :alt="item.product.name" class="w-full h-full object-cover" />
              <span v-else class="text-4xl">🎨</span>
              <span class="absolute top-2 right-2 px-2 py-0.5 bg-purple-600 text-white text-[10px] font-semibold rounded-full">추천</span>
            </div>
            <div class="p-3">
              <h3 class="text-sm font-medium text-gray-900 truncate">{{ item.product.name }}</h3>
              <p class="text-sm font-bold text-purple-600 mt-1">{{ formatPrice(item.product.price) }}원</p>
            </div>
          </article>
        </router-link>
      </div>
    </section>

    <div class="flex flex-wrap gap-2 mb-6">
      <button type="button" class="px-4 py-2 rounded-full text-sm font-medium border transition-colors" :class="selectedCategory === null ? 'border-purple-500 bg-purple-50 text-purple-700' : 'border-gray-200 text-gray-600 hover:border-purple-300'" @click="selectedCategory = null">전체</button>
      <button v-for="cat in PRODUCT_CATEGORIES" :key="cat" type="button" class="px-4 py-2 rounded-full text-sm font-medium border transition-colors" :class="selectedCategory === cat ? 'border-purple-500 bg-purple-50 text-purple-700' : 'border-gray-200 text-gray-600 hover:border-purple-300'" @click="selectedCategory = cat">{{ cat }}</button>
    </div>

    <h2 v-if="showRecommendations" class="text-lg font-semibold text-gray-800 mb-4">전체 상품</h2>

    <div v-if="store.loading" role="status" class="flex justify-center py-20">
      <svg class="animate-spin h-8 w-8 text-purple-600" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
      </svg>
    </div>

    <div v-else-if="store.error" class="text-center py-20 text-red-500">{{ store.error }}</div>

    <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      <router-link v-for="product in filteredProducts" :key="product.id" :to="`/products/${product.id}`" class="group block">
        <article class="bg-white rounded-xl border border-gray-100 overflow-hidden hover:shadow-md transition-shadow">
          <div class="aspect-square bg-gray-50 flex items-center justify-center relative">
            <img v-if="product.imageUrl" :src="product.imageUrl" :alt="product.name" class="w-full h-full object-cover" />
            <span v-else class="text-4xl">🎨</span>
            <span v-if="product.category" class="absolute top-2 left-2 px-2 py-0.5 bg-white/90 text-purple-600 text-xs font-medium rounded-full">{{ product.category }}</span>
          </div>
          <div class="p-3">
            <h2 class="text-sm font-medium text-gray-900 truncate">{{ product.name }}</h2>
            <p class="text-sm font-bold text-purple-600 mt-1">{{ formatPrice(product.price) }}원</p>
            <div v-if="product.colors.length > 0" class="flex flex-wrap gap-1 mt-2">
              <span v-for="color in product.colors.slice(0, 3)" :key="color" class="inline-block px-1.5 py-0.5 bg-gray-100 text-gray-500 text-[10px] font-medium rounded-full">{{ getColorLabel(color) }}</span>
            </div>
          </div>
        </article>
      </router-link>
    </div>

    <div v-if="!store.loading && filteredProducts.length === 0" class="text-center py-20 text-gray-400">등록된 상품이 없습니다</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useProductStore } from '@/stores/products'
import apiClient from '@/api/client'
import { COLOR_OPTIONS, PRODUCT_CATEGORIES } from '@/types'
import type { RecommendationItem } from '@/types'

const authStore = useAuthStore()
const store = useProductStore()
const selectedCategory = ref<string | null>(null)
const recommendations = ref<RecommendationItem[]>([])
const recLoading = ref(false)

const recommendedIds = computed(() => new Set(recommendedProducts.value.map((r) => r.product.id)))

const recommendedProducts = computed(() => {
  const meaningful = recommendations.value.filter(
    (r) => r.breakdown.personalColor + r.breakdown.preferredColor > 0
  )
  const source = meaningful.length > 0 ? meaningful : recommendations.value
  return source.slice(0, 8)
})

const showRecommendations = computed(
  () => authStore.isAuthenticated && recommendedProducts.value.length > 0
)

const recommendationTitle = computed(() => {
  const nick = authStore.user?.nickname ?? '회원'
  return `${nick}님을 위한 추천!`
})

const filteredProducts = computed(() => {
  let list = store.products
  if (recommendedIds.value.size > 0) {
    list = list.filter((p) => !recommendedIds.value.has(p.id))
  }
  if (selectedCategory.value === null) return list
  return list.filter((p) => p.category === selectedCategory.value)
})

function formatPrice(price: number): string {
  return price.toLocaleString('ko-KR')
}

function getColorLabel(value: string): string {
  return COLOR_OPTIONS.find((c) => c.value === value)?.label ?? value
}

async function loadRecommendations() {
  if (!authStore.isAuthenticated) return
  recLoading.value = true
  try {
    if (!authStore.user) await authStore.fetchMe()
    const { data } = await apiClient.get<RecommendationItem[]>('/recommendations')
    recommendations.value = data
  } catch {
    recommendations.value = []
  } finally {
    recLoading.value = false
  }
}

onMounted(async () => {
  await store.fetchProducts()
  if (authStore.isAuthenticated) {
    if (!authStore.user) await authStore.fetchMe()
    await loadRecommendations()
  }
})
</script>
