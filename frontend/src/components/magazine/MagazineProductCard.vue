<template>
  <router-link :to="`/products/${product.id}`" class="mpc group">
    <div class="mpc__thumb" :class="product.imageUrl ? 'mpc__thumb--cream' : toneClass">
      <img
        v-if="product.imageUrl"
        :src="product.imageUrl"
        :alt="product.name"
        loading="lazy"
        class="mpc__img"
      />
      <span v-else class="mpc__glyph" aria-hidden="true">{{ glyph }}</span>
    </div>

    <div class="mpc__body">
      <p class="mpc__eyebrow">
        <span class="mpc__dot" aria-hidden="true">✦</span>학꾸 상품
      </p>
      <h3 class="mpc__name">{{ product.name }}</h3>
      <p class="mpc__price">{{ formatPrice(product.price) }}<span class="mpc__won">원</span></p>
    </div>

    <span class="mpc__cta" aria-hidden="true">보러가기<span class="mpc__arrow"> →</span></span>
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

/** 상품 id 로 안정적인 톤 그라데이션 도출(8종 톤). 이미지 없을 때 폴백 배경. */
const toneClass = computed(() => {
  const n = Number(props.product.id)
  const idNum = Number.isFinite(n) ? Math.abs(Math.trunc(n)) : 0
  return `u-tone-${idNum % 8}`
})

function formatPrice(price: number): string {
  return price.toLocaleString('ko-KR')
}
</script>

<style scoped>
/* 가로로 긴 임베드 카드: [썸네일] [상품명·가격] [보러가기] */
.mpc {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px 12px 12px;
  border: 1px solid var(--hk-border);
  border-radius: var(--hk-radius-md);
  background: var(--hk-surface);
  transition: border-color 0.2s ease, transform 0.2s var(--ease-out-expo),
    box-shadow 0.2s ease;
}
.mpc:hover {
  border-color: var(--hk-border-control);
  transform: translateY(-2px);
  box-shadow: 0 10px 24px -18px rgba(22, 20, 15, 0.5);
}

.mpc__thumb {
  position: relative;
  flex: 0 0 auto;
  width: 84px;
  height: 84px;
  border-radius: var(--hk-radius-cta);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mpc__thumb--cream {
  background: var(--hk-cream);
}
.mpc__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s var(--ease-out-expo);
}
.mpc:hover .mpc__img {
  transform: scale(1.05);
}
.mpc__glyph {
  font-size: 28px;
  color: rgba(22, 20, 15, 0.22);
}

.mpc__body {
  flex: 1 1 auto;
  min-width: 0;
}
.mpc__eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--hk-text-quiet);
  margin-bottom: 5px;
}
.mpc__dot {
  color: var(--accent, #16140f);
}
.mpc__name {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--hk-ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.mpc__price {
  margin-top: 3px;
  font-size: 14px;
  font-weight: 700;
  color: var(--hk-ink);
}
.mpc__won {
  font-weight: 400;
  color: var(--hk-text-muted, var(--hk-text-quiet));
  margin-left: 1px;
}

.mpc__cta {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  height: 34px;
  padding: 0 14px;
  border-radius: var(--hk-radius-pill);
  background: var(--hk-ink);
  color: var(--hk-on-dark);
  font-size: 12.5px;
  font-weight: 700;
  white-space: nowrap;
}
.mpc__arrow {
  display: inline-block;
  transition: transform 0.2s var(--ease-out-expo);
}
.mpc:hover .mpc__arrow {
  transform: translateX(3px);
}

/* 모바일: CTA 여백 축소 */
@media (max-width: 479.98px) {
  .mpc__cta {
    padding: 0 12px;
  }
}
</style>
