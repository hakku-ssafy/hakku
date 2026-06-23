<template>
  <article class="group">
    <router-link :to="`/products/${item.productId}`" class="block">
      <div class="relative">
        <div
          class="flex aspect-square items-center justify-center overflow-hidden rounded-[5px]"
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
        </div>
        <span class="u-mono absolute bottom-2 left-2 text-[10px] text-ink/45">✦ NO.{{ code }}</span>
      </div>
    </router-link>

    <div class="pt-2.5">
      <router-link
        :to="`/products/${item.productId}`"
        class="block truncate text-sm font-medium text-ink transition-colors hover:text-accent"
      >
        {{ item.productName }}
      </router-link>
      <p class="mt-1 text-sm font-bold text-ink tabular-nums">
        {{ formatPrice(item.productPrice) }}<span class="font-normal text-ink-muted">원</span>
      </p>

      <router-link
        v-if="showOwner"
        :to="`/users/${item.ownerId}`"
        class="mt-1.5 inline-block text-xs text-ink-muted transition-colors hover:text-ink"
      >
        {{ item.ownerNickname }}님의 찜
      </router-link>

      <div class="mt-2.5 flex items-center justify-between">
        <button
          type="button"
          :disabled="!likable"
          class="inline-flex items-center gap-1.5 text-sm transition-transform duration-150 ease-out"
          :class="likable ? 'cursor-pointer hover:scale-110 focus-visible:scale-110' : 'cursor-default'"
          :aria-pressed="item.likedByMe"
          @click="likable && emit('like', item)"
        >
          <span
            class="leading-none"
            :class="item.likedByMe ? 'text-accent' : 'heart-off'"
            aria-hidden="true"
          >
            {{ item.likedByMe ? '♥' : '♡' }}
          </span>
          <span class="text-ink-soft tabular-nums">{{ item.likeCount }}</span>
        </button>
      </div>
    </div>
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

/** productId 로 안정적인 톤 그라데이션·코드 라벨 도출(ProductCard 와 동일 룩). */
const idNum = computed(() => Math.abs(Math.trunc(props.item.productId)) || 0)
const toneClass = computed(() => `u-tone-${idNum.value % 8}`)
const code = computed(() => String(idNum.value % 100).padStart(2, '0'))

function formatPrice(price: number): string {
  return price.toLocaleString('ko-KR')
}
</script>

<style scoped>
/* 미찜(빈 하트) — 토큰 var(--hk-heart-off) (rgba ink .28). */
.heart-off {
  color: var(--hk-heart-off);
}
</style>
