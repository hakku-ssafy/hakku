<template>
  <div class="u-container max-w-3xl py-8 sm:py-10">
    <router-link
      to="/products"
      class="inline-flex items-center gap-1.5 text-sm text-ink-soft hover:text-ink mb-7 transition-colors"
    >
      <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
      </svg>
      상품 목록
    </router-link>

    <div v-if="loading" role="status" class="flex justify-center py-20">
      <svg class="animate-spin h-7 w-7 text-ink" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-20" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" />
        <path class="opacity-80" fill="currentColor" d="M4 12a8 8 0 018-8v3a5 5 0 00-5 5H4z" />
      </svg>
    </div>

    <div v-else-if="errorMessage" role="alert" class="text-center py-20 text-red-500 text-sm">
      {{ errorMessage }}
    </div>

    <article v-else-if="product" class="u-rise">
      <div class="aspect-square sm:aspect-[4/3] bg-surface-sunken rounded-2xl border border-line flex items-center justify-center overflow-hidden">
        <img v-if="product.imageUrl" :src="product.imageUrl" :alt="product.name" class="w-full h-full object-cover" />
        <span v-else class="text-6xl text-ink-muted" aria-hidden="true">◍</span>
      </div>

      <div class="pt-7">
        <div class="flex items-start justify-between gap-4">
          <div class="min-w-0">
            <p v-if="product.category" class="u-eyebrow mb-2">{{ product.category }}</p>
            <h1 class="u-serif text-title text-ink">{{ product.name }}</h1>
          </div>
        </div>

        <p class="text-2xl font-semibold text-ink mt-4 tabular-nums">
          {{ formatPrice(product.price) }}<span class="text-ink-muted font-normal text-lg">원</span>
        </p>

        <div v-if="product.colors.length > 0" class="flex flex-wrap gap-1.5 mt-5">
          <AppBadge v-for="color in product.colors" :key="color">{{ getColorLabel(color) }}</AppBadge>
        </div>

        <p class="text-ink-soft leading-relaxed whitespace-pre-wrap mt-6 mb-8">{{ product.description }}</p>

        <a
          v-if="product.purchaseUrl"
          :href="product.purchaseUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="inline-flex items-center justify-center w-full h-12 bg-accent text-accent-ink rounded-lg font-medium hover:opacity-90 transition-opacity"
        >
          구매 링크 열기
        </a>
        <p v-else class="text-sm text-ink-muted text-center py-2">구매 링크가 아직 등록되지 않았습니다.</p>
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
import AppBadge from '@/components/ui/AppBadge.vue'

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
