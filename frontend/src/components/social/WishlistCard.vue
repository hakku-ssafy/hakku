<template>
  <article class="rounded-2xl border border-line bg-surface overflow-hidden u-pop hover:border-accent-line">
    <router-link :to="`/products/${item.productId}`" class="block">
      <div class="aspect-square bg-surface-sunken flex items-center justify-center overflow-hidden">
        <img
          v-if="item.productImageUrl"
          :src="item.productImageUrl"
          :alt="item.productName"
          class="w-full h-full object-cover"
          loading="lazy"
        />
        <span v-else class="text-4xl text-ink-muted" aria-hidden="true">◍</span>
      </div>
    </router-link>

    <div class="p-3">
      <router-link
        :to="`/products/${item.productId}`"
        class="text-sm font-semibold text-ink hover:text-accent transition-colors line-clamp-1 block"
      >
        {{ item.productName }}
      </router-link>
      <p class="text-sm text-ink-soft mt-0.5 tabular-nums">{{ formatPrice(item.productPrice) }}원</p>

      <router-link
        v-if="showOwner"
        :to="`/users/${item.ownerId}`"
        class="text-xs text-ink-muted mt-1.5 inline-block hover:text-ink transition-colors"
      >
        {{ item.ownerNickname }}님의 찜
      </router-link>

      <div class="flex items-center justify-between mt-2.5">
        <button
          type="button"
          :disabled="!likable"
          class="inline-flex items-center gap-1.5 text-sm transition-colors"
          :class="likable ? 'cursor-pointer hover:scale-105' : 'cursor-default'"
          :aria-pressed="item.likedByMe"
          @click="likable && emit('like', item)"
        >
          <span :style="{ color: item.likedByMe ? 'var(--color-accent)' : 'var(--color-line-strong)' }">
            {{ item.likedByMe ? '♥' : '♡' }}
          </span>
          <span class="tabular-nums text-ink-soft">{{ item.likeCount }}</span>
        </button>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import type { WishlistItem } from '@/types'

withDefaults(defineProps<{ item: WishlistItem; showOwner?: boolean; likable?: boolean }>(), {
  showOwner: false,
  likable: true,
})
const emit = defineEmits<{ like: [item: WishlistItem] }>()

function formatPrice(price: number): string {
  return price.toLocaleString('ko-KR')
}
</script>
