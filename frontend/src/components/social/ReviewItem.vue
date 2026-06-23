<template>
  <article class="flex gap-3 py-4 border-b border-line-soft last:border-b-0">
    <div
      class="grid h-9 w-9 shrink-0 place-items-center rounded-full text-[13px] font-bold text-ink/70 select-none"
      :class="swatchClass"
      aria-hidden="true"
    >
      {{ initial }}
    </div>

    <div class="min-w-0 flex-1">
      <header class="flex items-start justify-between gap-3">
        <div class="min-w-0">
          <router-link
            v-if="showProduct"
            :to="`/products/${review.productId}`"
            class="block truncate text-[13px] font-semibold text-ink transition-colors hover:text-accent"
          >
            {{ review.productName }}
          </router-link>
          <router-link
            v-else
            :to="`/users/${review.authorId}`"
            class="block truncate text-[13px] font-semibold text-ink transition-colors hover:text-accent"
          >
            {{ review.authorNickname }}
          </router-link>
          <div class="mt-1 flex items-center gap-2">
            <StarRating :rating="review.rating" size="sm" />
            <time class="text-xs text-ink-faint">{{ formatDate(review.createdAt) }}</time>
          </div>
        </div>

        <div v-if="canManage" class="flex shrink-0 items-center gap-1">
          <button
            type="button"
            class="rounded-cta px-2 py-1 text-xs text-ink-muted transition-colors hover:text-ink focus-visible:text-ink"
            @click="emit('edit', review)"
          >
            수정
          </button>
          <button
            type="button"
            class="rounded-cta px-2 py-1 text-xs text-coral transition-colors hover:text-ink focus-visible:text-ink"
            @click="emit('delete', review)"
          >
            삭제
          </button>
        </div>
      </header>
      <p class="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-ink-soft">{{ review.content }}</p>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Review } from '@/types'
import StarRating from './StarRating.vue'

const props = defineProps<{ review: Review; showProduct?: boolean; canManage?: boolean }>()
const emit = defineEmits<{ edit: [review: Review]; delete: [review: Review] }>()

/** 작성자 닉네임 첫 글자 — 아바타 이니셜. */
const initial = computed(() => props.review.authorNickname?.charAt(0).toUpperCase() ?? '?')

/** authorId 로 안정적으로 도출하는 웜 스와치(명명 컬러 5종 순환). */
const SWATCHES = ['bg-cream', 'bg-pink/40', 'bg-lavender/40', 'bg-mint/40', 'bg-butter/40'] as const
const swatchClass = computed(() => SWATCHES[Math.abs(props.review.authorId) % SWATCHES.length])

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })
}
</script>
