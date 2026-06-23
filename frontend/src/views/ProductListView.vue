<template>
  <div class="u-container py-10 sm:py-12">
    <!-- 추천 섹션 -->
    <section v-if="showRecommendations" class="mb-14 rounded-block bg-accent-soft border border-accent px-5 py-7 sm:px-8 sm:py-9">
      <SectionHeader eyebrow="For You" :title="recommendationTitle" />
      <div v-if="recLoading" class="flex justify-center py-12">
        <Spinner />
      </div>
      <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3.5 sm:gap-4">
        <ProductCard v-for="item in recommendedProducts" :key="'rec-' + item.product.id" :product="item.product">
          <template #meta>
            <AppBadge variant="accent" class="mt-2">추천 ✦</AppBadge>
          </template>
        </ProductCard>
      </div>
    </section>

    <!-- 메인 헤더 -->
    <div class="flex items-end justify-between gap-4 border-b border-line pb-4 mb-6">
      <div>
        <span class="u-eyebrow">Shop</span>
        <h1 class="u-serif text-title text-ink mt-2.5">{{ showRecommendations ? '전체 상품' : '상품' }}</h1>
      </div>
    </div>

    <!-- 카테고리 칩 -->
    <div class="flex flex-wrap gap-2 mb-8">
      <button type="button" :class="chipClass(selectedCategory === null)" @click="selectedCategory = null">전체</button>
      <button
        v-for="cat in PRODUCT_CATEGORIES"
        :key="cat"
        type="button"
        :class="chipClass(selectedCategory === cat)"
        @click="selectedCategory = cat"
      >{{ cat }}</button>
    </div>

    <div v-if="store.loading" role="status" class="flex justify-center py-20">
      <Spinner />
    </div>

    <div v-else-if="store.error" class="text-center py-20 text-red-600 text-sm">{{ store.error }}</div>

    <div v-else-if="filteredProducts.length > 0" class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3.5 sm:gap-4">
      <ProductCard v-for="product in filteredProducts" :key="product.id" :product="product">
        <template #meta>
          <div v-if="product.colors.length > 0" class="flex flex-wrap gap-1 mt-2">
            <span
              v-for="color in product.colors.slice(0, 3)"
              :key="color"
              class="inline-block px-2 py-0.5 bg-cream text-ink-soft text-[10px] font-medium rounded-full border border-line"
            >{{ getColorLabel(color) }}</span>
          </div>
        </template>
      </ProductCard>
    </div>

    <EmptyState v-else icon="◍" title="텅 비어 있어요" description="등록된 상품이 아직 없습니다." />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useProductStore } from '@/stores/products'
import apiClient from '@/api/client'
import { COLOR_OPTIONS, PRODUCT_CATEGORIES } from '@/types'
import type { RecommendationItem } from '@/types'
import ProductCard from '@/components/ui/ProductCard.vue'
import SectionHeader from '@/components/ui/SectionHeader.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

// 로딩 스피너 (role="status" 컨테이너는 호출부에서 부여)
const Spinner = () =>
  h(
    'svg',
    { class: 'animate-spin h-7 w-7 text-ink', fill: 'none', viewBox: '0 0 24 24' },
    [
      h('circle', { class: 'opacity-20', cx: 12, cy: 12, r: 10, stroke: 'currentColor', 'stroke-width': 3 }),
      h('path', { class: 'opacity-80', fill: 'currentColor', d: 'M4 12a8 8 0 018-8v3a5 5 0 00-5 5H4z' }),
    ],
  )

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

function getColorLabel(value: string): string {
  return COLOR_OPTIONS.find((c) => c.value === value)?.label ?? value
}

function chipClass(active: boolean): string {
  return [
    'inline-flex items-center h-[38px] px-4 rounded-full text-sm font-medium border transition-colors',
    active
      ? 'border-ink bg-ink text-white'
      : 'border-line bg-surface text-ink-soft hover:border-ink hover:text-ink',
  ].join(' ')
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
