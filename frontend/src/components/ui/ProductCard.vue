<template>
  <router-link :to="`/products/${product.id}`" class="pcard group block">
    <div class="relative">
      <div
        class="aspect-square overflow-hidden rounded-[5px] flex items-center justify-center"
        :class="product.imageUrl ? 'bg-cream' : toneClass"
      >
        <img
          v-if="product.imageUrl"
          :src="product.imageUrl"
          :alt="product.name"
          loading="lazy"
          class="w-full h-full object-cover transition-transform duration-500 ease-out group-hover:scale-105"
        />
        <span v-else class="text-4xl text-ink/20" aria-hidden="true">{{ glyph }}</span>
      </div>
      <!-- 모노 코드 라벨 (에디토리얼 디테일) -->
      <span class="u-mono absolute left-2 bottom-2 text-[10px] text-ink/45">✦ NO.{{ code }}</span>
    </div>
    <div class="pt-2.5">
      <p
        v-if="product.category"
        class="text-[0.65rem] uppercase tracking-[0.12em] font-semibold text-ink-faint mb-1 truncate"
      >
        {{ product.category }}
      </p>
      <h3 class="text-sm font-medium text-ink truncate">{{ product.name }}</h3>
      <p class="text-sm font-bold text-ink mt-1">
        {{ formatPrice(product.price) }}<span class="font-normal text-ink-muted">원</span>
      </p>
      <slot name="meta" />
    </div>
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

/** 상품 id 로 안정적인 톤 그라데이션·코드 라벨 도출(8종 톤). */
const idNum = computed(() => {
  const n = Number(props.product.id)
  return Number.isFinite(n) ? Math.abs(Math.trunc(n)) : String(props.product.id).length
})
const toneClass = computed(() => `u-tone-${idNum.value % 8}`)
const code = computed(() => String(idNum.value % 100).padStart(2, '0'))

function formatPrice(price: number): string {
  return price.toLocaleString('ko-KR')
}
</script>

<style scoped>
.pcard {
  animation: hk-fade 0.4s ease both;
}
</style>
