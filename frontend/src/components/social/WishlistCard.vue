<template>
  <article class="wcard group relative">
    <router-link :to="`/products/${item.productId}`" class="block">
      <div
        class="relative flex aspect-square items-center justify-center overflow-hidden rounded-[5px]"
        :class="item.productImageUrl ? 'bg-cream' : toneClass"
      >
        <img
          v-if="item.productImageUrl"
          :src="item.productImageUrl"
          :alt="item.productName"
          class="h-full w-full object-cover transition-transform duration-500 ease-out group-hover:scale-105"
          loading="lazy"
        />
        <span v-else class="text-4xl text-ink/20" aria-hidden="true">◍</span>

        <!-- 정보 오버레이 (썸네일 하단, 어두운 그라데이션) -->
        <div class="wcard__overlay">
          <h3 class="wcard__name">{{ item.productName }}</h3>
          <p class="wcard__price tabular-nums">
            {{ formatPrice(item.productPrice) }}<span class="wcard__won">원</span>
          </p>
        </div>
      </div>
    </router-link>

    <!-- 좋아요 (인터랙티브 — 상품 링크 밖, 우상단) -->
    <button
      type="button"
      :disabled="!likable"
      class="wcard__like"
      :aria-pressed="item.likedByMe"
      @click="likable && emit('like', item)"
    >
      <span :class="item.likedByMe ? 'wcard__heart--on' : 'wcard__heart--off'" aria-hidden="true">
        {{ item.likedByMe ? '♥' : '♡' }}
      </span>
      <span class="tabular-nums">{{ item.likeCount }}</span>
    </button>

    <!-- 소유자 (옵션 — 좌상단) -->
    <router-link
      v-if="showOwner"
      :to="`/users/${item.ownerId}`"
      class="wcard__owner"
    >
      {{ item.ownerNickname }}님의 찜
    </router-link>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { WishlistItem } from '@/types'

const props = withDefaults(
  defineProps<{ item: WishlistItem; showOwner?: boolean; likable?: boolean }>(),
  { showOwner: false, likable: true },
)
const emit = defineEmits<{ like: [item: WishlistItem] }>()

/** productId 로 안정적인 톤 그라데이션 도출(ProductCard 와 동일 룩). */
const idNum = computed(() => Math.abs(Math.trunc(props.item.productId)) || 0)
const toneClass = computed(() => `u-tone-${idNum.value % 8}`)

function formatPrice(price: number): string {
  return price.toLocaleString('ko-KR')
}
</script>

<style scoped>
.wcard__overlay {
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
.wcard__name {
  font-size: 0.8125rem;
  font-weight: 600;
  line-height: 1.25;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.wcard__price {
  font-size: 0.875rem;
  font-weight: 700;
  margin-top: 2px;
}
.wcard__won {
  font-weight: 400;
  opacity: 0.82;
}

/* 좌·우 상단 떠있는 라이트 칩 — 이미지 위에서도 대비 확보 */
.wcard__like {
  position: absolute;
  top: 7px;
  right: 7px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 26px;
  padding: 0 9px;
  border-radius: var(--hk-radius-pill);
  background: rgba(255, 255, 255, 0.86);
  color: var(--hk-ink, #16140f);
  font-size: 12px;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(3px);
  transition: transform 0.15s ease;
}
.wcard__like:not(:disabled) {
  cursor: pointer;
}
.wcard__like:not(:disabled):hover,
.wcard__like:not(:disabled):focus-visible {
  transform: scale(1.06);
}
.wcard__heart--on {
  color: var(--accent, #16140f);
}
.wcard__heart--off {
  color: var(--hk-heart-off);
}

.wcard__owner {
  position: absolute;
  top: 7px;
  left: 7px;
  max-width: calc(100% - 86px);
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 9px;
  border-radius: var(--hk-radius-pill);
  background: rgba(255, 255, 255, 0.86);
  color: var(--hk-ink, #16140f);
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(3px);
  transition: color 0.15s ease;
}
.wcard__owner:hover {
  color: var(--accent, #16140f);
}
</style>
