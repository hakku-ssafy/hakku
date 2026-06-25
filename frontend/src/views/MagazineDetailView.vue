<template>
  <div class="u-container mag">
    <router-link to="/" class="mag__back">← 홈으로</router-link>

    <div v-if="loading" role="status" class="mag__center">
      <svg class="animate-spin h-7 w-7 text-ink" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-20" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" />
        <path class="opacity-80" fill="currentColor" d="M4 12a8 8 0 018-8v3a5 5 0 00-5 5H4z" />
      </svg>
    </div>

    <div v-else-if="error" class="mag__center mag__error">{{ error }}</div>

    <article v-else-if="magazine" class="mag__article">
      <header class="mag__head">
        <span v-if="magazine.kicker" class="mag__kicker">{{ magazine.kicker }}</span>
        <h1 class="mag__title">{{ magazine.title }}</h1>
        <p v-if="magazine.subtitle" class="mag__sub">{{ magazine.subtitle }}</p>
      </header>

      <img
        v-if="magazine.coverImageUrl"
        :src="magazine.coverImageUrl"
        :alt="magazine.title"
        class="mag__hero"
        loading="eager"
      />

      <MagazineBody
        v-if="magazine.content"
        :content="magazine.content"
        :products-by-id="productsById"
      />
    </article>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getMagazine } from '@/api/magazine'
import { getProduct } from '@/api/products'
import { parseMagazineBlocks } from '@/lib/magazineContent'
import type { Magazine, Product } from '@/types'
import MagazineBody from '@/components/magazine/MagazineBody.vue'

const route = useRoute()
const magazine = ref<Magazine | null>(null)
const productsById = ref<Record<number, Product>>({})
const loading = ref(true)
const error = ref<string | null>(null)

/** 본문에서 임베드된 상품 id 를 모아 병렬로 조회한다. 일부 실패는 폴백 링크로 처리. */
async function loadEmbeddedProducts(content: string): Promise<void> {
  const ids = [
    ...new Set(
      parseMagazineBlocks(content).flatMap((b) => (b.type === 'product' ? [b.productId] : [])),
    ),
  ]
  if (ids.length === 0) return

  const results = await Promise.allSettled(ids.map((id) => getProduct(id)))
  const map: Record<number, Product> = {}
  results.forEach((result, idx) => {
    if (result.status === 'fulfilled') map[ids[idx]] = result.value
  })
  productsById.value = map
}

onMounted(async () => {
  const id = Number(route.params.id)
  if (!Number.isFinite(id)) {
    error.value = '잘못된 주소입니다.'
    loading.value = false
    return
  }
  try {
    const loaded = await getMagazine(id)
    magazine.value = loaded
    if (loaded.content) await loadEmbeddedProducts(loaded.content)
  } catch {
    error.value = '콘텐츠를 찾을 수 없습니다.'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.mag {
  padding-top: 28px;
  padding-bottom: 90px;
}
.mag__back {
  display: inline-block;
  margin-bottom: 24px;
  font-size: 13px;
  font-weight: 600;
  color: var(--hk-text-quiet);
  transition: color 0.18s ease;
}
.mag__back:hover {
  color: var(--hk-ink);
}
.mag__center {
  display: flex;
  justify-content: center;
  padding: 90px 0;
}
.mag__error {
  font-size: 0.9rem;
  color: var(--hk-text-quiet);
}
.mag__article {
  max-width: 720px;
  margin: 0 auto;
}
.mag__kicker {
  display: inline-block;
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.2em;
  color: var(--accent, #16140f);
  margin-bottom: 14px;
}
.mag__title {
  font-size: clamp(1.9rem, 1.3rem + 2.4vw, 3rem);
  font-weight: 800;
  line-height: 1.12;
  letter-spacing: -0.03em;
  white-space: pre-line;
}
.mag__sub {
  margin-top: 14px;
  font-size: 1.05rem;
  line-height: 1.5;
  color: var(--hk-text-quiet);
}
.mag__hero {
  display: block;
  width: 100%;
  margin: 32px 0;
  border-radius: var(--hk-radius-md);
  aspect-ratio: 16 / 10;
  object-fit: cover;
  background: var(--hk-cream);
}
</style>
