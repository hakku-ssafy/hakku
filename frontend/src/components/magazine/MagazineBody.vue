<template>
  <div class="mbody">
    <template v-for="(block, i) in blocks" :key="i">
      <!-- 마크다운(사진·글)은 살균된 HTML 로 렌더 -->
      <div
        v-if="block.type === 'markdown'"
        class="mbody__md"
        v-html="renderMarkdown(block.markdown)"
      />
      <!-- 단독 상품 링크 → 가로 상품 카드 임베드 -->
      <MagazineProductCard
        v-else-if="productFor(block.productId)"
        :product="productFor(block.productId)!"
        class="mbody__product"
      />
      <!-- 상품 데이터 로딩 전/실패 시 → 상세 링크 폴백 -->
      <router-link
        v-else
        :to="`/products/${block.productId}`"
        class="mbody__fallback"
      >
        학꾸 상품 보러가기 →
      </router-link>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Product } from '@/types'
import { parseMagazineBlocks } from '@/lib/magazineContent'
import { renderMarkdown } from '@/lib/markdown'
import MagazineProductCard from './MagazineProductCard.vue'

const props = defineProps<{
  content: string | null
  productsById: Record<number, Product>
}>()

const blocks = computed(() => parseMagazineBlocks(props.content ?? ''))

function productFor(id: number): Product | undefined {
  return props.productsById[id]
}
</script>

<style scoped>
.mbody {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

/* 임베드 카드 위아래 여백은 gap 으로 처리되므로 카드 자체는 추가 마진 없음 */
.mbody__product {
  margin: 2px 0;
}

.mbody__fallback {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  height: 40px;
  padding: 0 18px;
  border-radius: var(--hk-radius-pill);
  border: 1px solid var(--hk-border-control);
  font-size: 13px;
  font-weight: 700;
  color: var(--hk-ink);
  transition: background 0.18s ease, color 0.18s ease;
}
.mbody__fallback:hover {
  background: var(--hk-ink);
  color: var(--hk-on-dark);
}

/* ── 마크다운 본문 타이포 ── */
.mbody__md {
  font-size: 1.0625rem;
  line-height: 1.85;
  color: var(--hk-ink);
  word-break: break-word;
}
.mbody__md :deep(h2) {
  font-size: 1.45rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  margin: 0.4em 0 0.4em;
}
.mbody__md :deep(h3) {
  font-size: 1.18rem;
  font-weight: 700;
  margin: 0.6em 0 0.3em;
}
.mbody__md :deep(p) {
  margin: 0 0 0.9em;
}
.mbody__md :deep(p:last-child) {
  margin-bottom: 0;
}
.mbody__md :deep(a) {
  color: var(--accent, #16140f);
  font-weight: 600;
  text-decoration: underline;
  text-underline-offset: 2px;
}
.mbody__md :deep(img) {
  display: block;
  width: 100%;
  margin: 0.6em 0;
  border-radius: var(--hk-radius-md);
  background: var(--hk-cream);
}
.mbody__md :deep(ul),
.mbody__md :deep(ol) {
  padding-left: 1.3em;
  margin: 0 0 0.9em;
}
.mbody__md :deep(li) {
  margin: 0.2em 0;
}
.mbody__md :deep(blockquote) {
  margin: 0 0 0.9em;
  padding: 0.2em 0 0.2em 1em;
  border-left: 3px solid var(--hk-border-control);
  color: var(--hk-text-quiet);
}
.mbody__md :deep(code) {
  background: var(--hk-cream);
  padding: 0.1em 0.4em;
  border-radius: 4px;
  font-size: 0.9em;
}
</style>
