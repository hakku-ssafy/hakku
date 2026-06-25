<template>
  <router-link :to="`/products/${product.id}`" class="pcard group block">
    <div
      class="relative aspect-square overflow-hidden rounded-[5px] flex items-center justify-center"
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

      <!-- 오버레이 모드: 정보를 썸네일 안 하단에 + 어두운 그라데이션으로 가독성 확보 -->
      <template v-if="overlay">
        <span class="u-mono pcard__code">✦ NO.{{ code }}</span>
        <div class="pcard__overlay">
          <p v-if="product.category" class="pcard__cat">{{ product.category }}</p>
          <h3 class="pcard__name">{{ product.name }}</h3>
          <p class="pcard__price">
            {{ formatPrice(product.price) }}<span class="pcard__won">원</span>
          </p>
          <p v-if="product.sellerNickname" class="pcard__seller">
            <span class="pcard__seller-by">by</span> {{ product.sellerNickname }}
          </p>
          <slot name="meta" />
        </div>
      </template>
      <!-- 스택 모드 코드 라벨 (기존 위치) -->
      <span v-else class="u-mono absolute left-2 bottom-2 text-[10px] text-ink/45">
        ✦ NO.{{ code }}
      </span>
    </div>

    <!-- 스택 모드 정보 (overlay=false): 추천 등 풍부한 메타가 필요한 화면용 -->
    <div v-if="!overlay" class="pt-2.5">
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
      <p v-if="product.sellerNickname" class="text-xs text-ink-muted mt-1 truncate">
        <span class="text-ink-faint">by</span> {{ product.sellerNickname }}
      </p>
      <slot name="meta" />
    </div>
  </router-link>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Product } from '@/types'

const props = withDefaults(defineProps<{ product: Product; overlay?: boolean }>(), {
  overlay: true,
})

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

/* ── 오버레이 모드 ── */
.pcard__overlay {
  position: absolute;
  inset-inline: 0;
  bottom: 0;
  padding: 30px 10px 9px;
  background: linear-gradient(
    to top,
    rgba(22, 20, 15, 0.86) 0%,
    rgba(22, 20, 15, 0.55) 44%,
    rgba(22, 20, 15, 0) 100%
  );
  color: var(--hk-on-dark, #f4efe6);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.35);
}
.pcard__cat {
  font-size: 0.6rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-weight: 600;
  opacity: 0.78;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.pcard__name {
  font-size: 0.8125rem;
  font-weight: 600;
  line-height: 1.25;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.pcard__price {
  font-size: 0.875rem;
  font-weight: 700;
  margin-top: 2px;
}
.pcard__won {
  font-weight: 400;
  opacity: 0.82;
}
.pcard__seller {
  margin-top: 3px;
  font-size: 0.6875rem;
  font-weight: 500;
  opacity: 0.82;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.pcard__seller-by {
  opacity: 0.6;
}
.pcard__code {
  position: absolute;
  top: 8px;
  left: 8px;
  font-size: 10px;
  color: rgba(244, 239, 230, 0.62);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.4);
}
</style>
