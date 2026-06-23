<template>
  <div class="rcard">
    <div class="rcard__head">
      <span class="rcard__eyebrow">For You · {{ personalColorLabel }}</span>
      <h2 class="rcard__title">맞춤 추천 상품</h2>
    </div>

    <!-- 추천 상품 썸네일 — 항상 4칸(모자라면 플레이스홀더로 채워 그리드 안정) -->
    <div class="rcard__grid">
      <router-link
        v-for="(product, i) in slots"
        :key="product ? product.id : `empty-${i}`"
        :to="product ? `/products/${product.id}` : '/recommendations'"
        class="rcard__cell"
        :class="{ 'rcard__cell--empty': !product }"
        :aria-label="product ? product.name : '추천 상품 보기'"
      >
        <img
          v-if="product && product.imageUrl"
          :src="product.imageUrl"
          :alt="product.name"
          loading="lazy"
          class="rcard__thumb"
        />
        <span v-else class="rcard__glyph" aria-hidden="true">✦</span>
      </router-link>
    </div>

    <router-link to="/recommendations" class="rcard__cta">추천 전체보기 →</router-link>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Product } from '@/types'

interface Props {
  /** 사용자의 퍼스널컬러 라벨(예: "웜 스프링"). */
  personalColorLabel?: string
  /** 추천 상품(상위 N개). 4개를 넘으면 잘라 쓴다. */
  products?: Product[]
}
const props = withDefaults(defineProps<Props>(), {
  personalColorLabel: '',
  products: () => [],
})

const SLOT_COUNT = 4

// 항상 4칸을 보장한다 — 추천이 모자라면 null(플레이스홀더)로 채운다.
const slots = computed<(Product | null)[]>(() => {
  const out: (Product | null)[] = props.products.slice(0, SLOT_COUNT)
  while (out.length < SLOT_COUNT) out.push(null)
  return out
})
</script>

<style scoped>
.rcard {
  position: relative;
  height: 100%;
  box-sizing: border-box;
  border-radius: var(--hk-radius-md);
  border: 1px solid var(--accent, #16140f);
  background: linear-gradient(155deg, var(--accent-soft, #f1efec), var(--hk-surface-warm));
  padding: clamp(18px, 5.5%, 28px);
  display: flex;
  flex-direction: column;
  gap: clamp(10px, 3%, 16px);
}

.rcard__eyebrow {
  font-size: 10.5px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--accent-ink, #16140f);
}
.rcard__title {
  margin: 9px 0 0;
  font-size: clamp(20px, 6.5%, 27px);
  line-height: 1.12;
  letter-spacing: -0.02em;
  font-weight: 800;
  color: var(--accent-ink, #16140f);
}

/* 2×2 정사각 썸네일 그리드 — 남는 세로 공간을 채운다. */
.rcard__grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, 1fr);
  gap: 8px;
}
.rcard__cell {
  position: relative;
  overflow: hidden;
  border-radius: var(--hk-radius-img);
  background: var(--hk-cream);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s var(--ease-out-expo);
}
.rcard__cell:hover {
  transform: translateY(-2px);
}
.rcard__cell--empty {
  background-image: linear-gradient(140deg, var(--accent-soft, #f1efec), var(--hk-cream));
}
.rcard__thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.rcard__glyph {
  font-size: 20px;
  color: var(--accent, #16140f);
  opacity: 0.4;
}

.rcard__cta {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  height: clamp(42px, 13%, 48px);
  border-radius: var(--hk-radius-pill);
  background: var(--accent, #16140f);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  transition: transform 0.18s var(--ease-out-expo), opacity 0.18s ease;
}
.rcard__cta:hover {
  transform: translateY(-1px);
  opacity: 0.94;
}
</style>
