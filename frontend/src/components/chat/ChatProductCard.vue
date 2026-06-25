<template>
  <router-link :to="`/products/${product.id}`" class="cpc">
    <div class="cpc__thumb" :class="{ 'cpc__thumb--placeholder': !product.imageUrl }">
      <img
        v-if="product.imageUrl"
        :src="product.imageUrl"
        :alt="product.name"
        loading="lazy"
        class="cpc__img"
      />
      <span v-else class="cpc__glyph" aria-hidden="true">✦</span>
    </div>
    <div class="cpc__body">
      <p class="cpc__name">{{ product.name }}</p>
      <p class="cpc__price">{{ formatPrice(product.price) }}<span class="cpc__won">원</span></p>
    </div>
  </router-link>
</template>

<script setup lang="ts">
import type { ChatProduct } from './chatTypes'

defineProps<{ product: ChatProduct }>()

function formatPrice(price: number): string {
  return price.toLocaleString('ko-KR')
}
</script>

<style scoped>
/* 답변 아래 가로 스크롤 줄에 놓이는 세로형 상품 카드 */
.cpc {
  display: flex;
  flex-direction: column;
  flex: 0 0 auto;
  width: 132px;
  border: 1px solid var(--hk-border);
  border-radius: var(--hk-radius-md);
  background: var(--hk-surface);
  overflow: hidden;
  text-decoration: none;
  transition: border-color 0.2s ease, transform 0.2s var(--ease-out-expo),
    box-shadow 0.2s ease;
}
.cpc:hover {
  border-color: var(--hk-border-control);
  transform: translateY(-2px);
  box-shadow: 0 10px 24px -18px rgba(22, 20, 15, 0.5);
}

.cpc__thumb {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--hk-cream);
}
.cpc__thumb--placeholder {
  background: var(--hk-cream);
}
.cpc__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s var(--ease-out-expo);
}
.cpc:hover .cpc__img {
  transform: scale(1.05);
}
.cpc__glyph {
  font-size: 26px;
  color: rgba(22, 20, 15, 0.22);
}

.cpc__body {
  padding: 8px 10px 10px;
  min-width: 0;
}
.cpc__name {
  font-size: 12.5px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--hk-ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.cpc__price {
  margin-top: 2px;
  font-size: 12.5px;
  font-weight: 700;
  color: var(--hk-ink);
}
.cpc__won {
  font-weight: 400;
  color: var(--hk-text-muted, var(--hk-text-quiet));
  margin-left: 1px;
}
</style>
