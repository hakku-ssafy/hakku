<template>
  <div class="u-container shop">
    <!-- 헤더 -->
    <header class="shop__head">
      <span class="u-eyebrow">Shop</span>
      <h1 class="shop__title">꾸미기 아이템</h1>
    </header>

    <!-- For You 추천 섹션 -->
    <section v-if="showRecommendations" class="foryou">
      <div class="foryou__head">
        <span class="foryou__title">{{ recommendationTitle }}</span>
        <span class="foryou__badge">추천 ✦</span>
      </div>
      <div v-if="recLoading" class="shop__center"><Spinner /></div>
      <div v-else class="product-grid">
        <ProductCard
          v-for="item in recommendedProducts"
          :key="'rec-' + item.product.id"
          :product="item.product"
        >
          <template #meta>
            <AppBadge variant="accent" class="mt-2">추천 ✦</AppBadge>
          </template>
        </ProductCard>
      </div>
    </section>

    <!-- 카테고리 칩 -->
    <div class="chips">
      <button type="button" :class="chipClass(selectedCategory === null)" @click="selectedCategory = null">
        전체
      </button>
      <button
        v-for="cat in PRODUCT_CATEGORIES"
        :key="cat"
        type="button"
        :class="chipClass(selectedCategory === cat)"
        @click="selectedCategory = cat"
      >{{ cat }}</button>
    </div>

    <!-- 상태 분기 -->
    <div v-if="store.loading" role="status" class="shop__center"><Spinner /></div>

    <div v-else-if="store.error" class="shop__error">{{ store.error }}</div>

    <div v-else-if="filteredProducts.length > 0" class="product-grid">
      <ProductCard v-for="product in filteredProducts" :key="product.id" :product="product">
        <template v-if="product.colors.length > 0" #meta>
          <div class="color-dots">
            <span
              v-for="color in product.colors.slice(0, 3)"
              :key="color"
              class="color-dot"
              role="img"
              :aria-label="getColorLabel(color)"
              :title="getColorLabel(color)"
              :style="{ background: colorHex(color) }"
            />
          </div>
        </template>
      </ProductCard>
    </div>

    <EmptyState v-else icon="◍" title="텅 비어 있어요" description="등록된 상품이 아직 없습니다." />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useProductStore } from '@/stores/products'
import apiClient from '@/api/client'
import { COLOR_OPTIONS, PRODUCT_CATEGORIES } from '@/types'
import type { RecommendationItem } from '@/types'
import ProductCard from '@/components/ui/ProductCard.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

// 로딩 스피너 (role="status" 컨테이너는 호출부에서 부여)
const Spinner = () =>
  h(
    'svg',
    { class: 'animate-spin h-7 w-7 text-ink', fill: 'none', viewBox: '0 0 24 24' },
    [
      h('circle', { class: 'opacity-20', cx: 12, cy: 12, r: 10, stroke: 'currentColor', 'stroke-width': 3 }),
      h('path', { class: 'opacity-80', fill: 'currentColor', d: 'M4 12a8 8 0 018-8v3a5 5 0 00-5 5H4z' }),
    ],
  )

// 명명 컬러 → hex (도트 표시용). 미정의 컬러는 중립 톤.
const COLOR_HEX: Record<string, string> = {
  red: '#d98b80',
  orange: '#e79b82',
  yellow: '#f0de9e',
  green: '#afd8c8',
  blue: '#a9c8e0',
  purple: '#c7bce6',
  pink: '#e7a6be',
  brown: '#9a7b63',
}
function colorHex(value: string): string {
  return COLOR_HEX[value] ?? 'var(--hk-border-dashed)'
}

const authStore = useAuthStore()
const store = useProductStore()
const selectedCategory = ref<string | null>(null)
const recommendations = ref<RecommendationItem[]>([])
const recLoading = ref(false)

const recommendedIds = computed(() => new Set(recommendedProducts.value.map((r) => r.product.id)))

const recommendedProducts = computed(() => {
  const meaningful = recommendations.value.filter(
    (r) => r.breakdown.personalColor + r.breakdown.preferredColor > 0,
  )
  const source = meaningful.length > 0 ? meaningful : recommendations.value
  return source.slice(0, 8)
})

const showRecommendations = computed(
  () => authStore.isAuthenticated && recommendedProducts.value.length > 0,
)

const recommendationTitle = computed(() => {
  const nick = authStore.user?.nickname ?? '회원'
  return `${nick}님을 위한 추천!`
})

const filteredProducts = computed(() => {
  let list = store.products
  if (recommendedIds.value.size > 0) {
    list = list.filter((p) => !recommendedIds.value.has(p.id))
  }
  if (selectedCategory.value === null) return list
  return list.filter((p) => p.category === selectedCategory.value)
})

function getColorLabel(value: string): string {
  return COLOR_OPTIONS.find((c) => c.value === value)?.label ?? value
}

function chipClass(active: boolean): string {
  return [
    'inline-flex items-center h-[38px] px-[18px] rounded-full text-[13px] font-semibold border transition-colors whitespace-nowrap',
    active
      ? 'border-ink bg-ink text-white'
      : 'border-line bg-surface text-ink-soft hover:border-ink hover:text-ink',
  ].join(' ')
}

async function loadRecommendations() {
  if (!authStore.isAuthenticated) return
  recLoading.value = true
  try {
    if (!authStore.user) await authStore.fetchMe()
    const { data } = await apiClient.get<RecommendationItem[]>('/recommendations')
    recommendations.value = data
  } catch {
    recommendations.value = []
  } finally {
    recLoading.value = false
  }
}

onMounted(async () => {
  await store.fetchProducts()
  if (authStore.isAuthenticated) {
    if (!authStore.user) await authStore.fetchMe()
    await loadRecommendations()
  }
})
</script>

<style scoped>
.shop {
  padding-top: 40px;
  padding-bottom: 90px;
}
.shop__head {
  margin-bottom: 30px;
}
.shop__title {
  margin-top: 12px;
  font-size: clamp(1.65rem, 1.3rem + 1.6vw, 2rem);
  font-weight: 700;
  letter-spacing: -0.03em;
}

/* For You */
.foryou {
  margin-bottom: 46px;
  padding: 28px 30px;
  border-radius: var(--hk-radius-cta);
  background: linear-gradient(118deg, var(--accent-soft, #f1efec), var(--hk-surface-warm));
  border: 1px solid var(--hk-border-foryou);
}
.foryou__head {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 20px;
}
.foryou__title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.foryou__badge {
  height: 22px;
  padding: 0 9px;
  display: inline-flex;
  align-items: center;
  border-radius: var(--hk-radius-pill);
  background: var(--accent, #16140f);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

/* 칩 */
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 28px;
}

/* 그리드 — 4열(1024+) → 3열(768) → 2열(모바일) */
.product-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
}
@media (min-width: 768px) {
  .product-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (min-width: 1024px) {
  .product-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

.color-dots {
  display: flex;
  gap: 5px;
  margin-top: 8px;
}
.color-dot {
  width: 11px;
  height: 11px;
  border-radius: var(--hk-radius-pill);
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.12);
}

.shop__center {
  display: flex;
  justify-content: center;
  padding: 80px 0;
}
.shop__error {
  text-align: center;
  padding: 80px 0;
  font-size: 0.875rem;
  color: #c0392b;
}

/* 칩 가로 스크롤(모바일) */
@media (max-width: 639.98px) {
  .chips {
    flex-wrap: nowrap;
    overflow-x: auto;
    scrollbar-width: none;
  }
  .chips::-webkit-scrollbar {
    display: none;
  }
}
</style>
