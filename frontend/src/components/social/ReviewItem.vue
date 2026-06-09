<template>
  <article class="py-4 border-t border-line first:border-t-0">
    <header class="flex items-center justify-between gap-3 mb-2">
      <div class="min-w-0">
        <router-link
          v-if="showProduct"
          :to="`/products/${review.productId}`"
          class="text-sm font-semibold text-ink hover:text-accent transition-colors truncate block"
        >
          {{ review.productName }}
        </router-link>
        <router-link
          v-else
          :to="`/users/${review.authorId}`"
          class="text-sm font-semibold text-ink hover:text-accent transition-colors truncate block"
        >
          {{ review.authorNickname }}
        </router-link>
        <div class="flex items-center gap-2 mt-0.5">
          <StarRating :rating="review.rating" size="sm" />
          <time class="text-xs text-ink-muted">{{ formatDate(review.createdAt) }}</time>
        </div>
      </div>

      <div v-if="canManage" class="flex items-center gap-1 shrink-0">
        <button
          type="button"
          class="text-xs text-ink-muted hover:text-ink px-2 py-1 transition-colors"
          @click="emit('edit', review)"
        >
          수정
        </button>
        <button
          type="button"
          class="text-xs text-red-400 hover:text-red-600 px-2 py-1 transition-colors"
          @click="emit('delete', review)"
        >
          삭제
        </button>
      </div>
    </header>
    <p class="text-sm text-ink-soft leading-relaxed whitespace-pre-wrap">{{ review.content }}</p>
  </article>
</template>

<script setup lang="ts">
import type { Review } from '@/types'
import StarRating from './StarRating.vue'

defineProps<{ review: Review; showProduct?: boolean; canManage?: boolean }>()
const emit = defineEmits<{ edit: [review: Review]; delete: [review: Review] }>()

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })
}
</script>
