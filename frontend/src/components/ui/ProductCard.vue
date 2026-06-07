<template>
  <router-link :to="`/products/${product.id}`" class="group block u-pop rounded-2xl">
    <article class="bg-surface rounded-2xl border border-line overflow-hidden">
      <div
        class="aspect-square overflow-hidden flex items-center justify-center"
        :class="product.imageUrl ? 'bg-surface-sunken' : 'u-gradient-accent'"
      >
        <img
          v-if="product.imageUrl"
          :src="product.imageUrl"
          :alt="product.name"
          loading="lazy"
          class="w-full h-full object-cover transition-transform duration-500 ease-out group-hover:scale-110"
        />
        <span v-else class="text-4xl text-accent-ink/90 drop-shadow-sm" aria-hidden="true">{{ glyph }}</span>
      </div>
      <div class="p-3.5">
        <p
          v-if="product.category"
          class="text-[0.6875rem] uppercase tracking-[0.14em] font-semibold text-accent mb-1.5 truncate"
        >
          {{ product.category }}
        </p>
        <h3 class="text-sm font-medium text-ink truncate">{{ product.name }}</h3>
        <p class="text-sm font-bold text-ink mt-1">
          {{ formatPrice(product.price) }}<span class="font-normal text-ink-muted">원</span>
        </p>
        <slot name="meta" />
      </div>
    </article>
  </router-link>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Product } from '@/types'

const props = defineProps<{ product: Product }>()

const GLYPHS: Record<string, string> = {
  핀뱃지: '✸',
  키링: '✦',
  '꾸미기 스티커': '❀',
}
const glyph = computed(() => (props.product.category && GLYPHS[props.product.category]) || '✦')

function formatPrice(price: number): string {
  return price.toLocaleString('ko-KR')
}
</script>
