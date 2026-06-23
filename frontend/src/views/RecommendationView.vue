<template>
  <div class="u-container py-10 sm:py-12">
    <SectionHeader eyebrow="For You" title="맞춤 추천 상품" description="퍼스널컬러와 취향, 활동을 바탕으로 골랐어요." />

    <!-- 퍼스널컬러 컨텍스트 -->
    <div v-if="personalColorLabel" class="-mt-3 mb-7 flex flex-wrap items-center gap-2.5">
      <AppBadge variant="accent">
        <span class="w-1.5 h-1.5 rounded-full bg-accent" aria-hidden="true" />
        {{ personalColorLabel }}
      </AppBadge>
      <span class="text-xs text-ink-muted">내 퍼스널컬러를 기준으로 정렬했어요</span>
    </div>

    <div v-if="loading" role="status" class="flex justify-center py-20">
      <svg class="animate-spin h-7 w-7 text-accent" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-20" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" />
        <path class="opacity-80" fill="currentColor" d="M4 12a8 8 0 018-8v3a5 5 0 00-5 5H4z" />
      </svg>
    </div>

    <div v-else-if="error" class="text-center py-20 text-red-600 text-sm">{{ error }}</div>


    <div v-else-if="recommendations.length > 0" class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3.5 sm:gap-4">
      <ProductCard v-for="item in recommendations" :key="item.product.id" :product="item.product">
        <template #meta>
          <div class="mt-2.5 pt-2.5 border-t border-line">
            <div class="flex flex-wrap items-center gap-1.5 mb-2">
              <AppBadge variant="accent">추천 ✦</AppBadge>
              <AppBadge variant="solid"><span class="u-mono">{{ scorePercent(item.score) }}</span>점</AppBadge>
            </div>
            <div class="h-1.5 rounded-full bg-track overflow-hidden">
              <div
                class="h-full rounded-full bg-accent transition-[width] duration-500 ease-out"
                :style="{ width: scorePercent(item.score) + '%' }"
              />
            </div>
            <p class="text-[0.6875rem] text-ink-muted mt-1.5 truncate">{{ reasonLine(item) }}</p>
          </div>
        </template>
      </ProductCard>
    </div>

    <EmptyState
      v-else
      icon="✦"
      title="아직 추천 상품이 없어요"
      description="퍼스널컬러 진단을 받으면 당신에게 어울리는 상품을 골라드릴게요."
    >
      <AppButton to="/diagnosis">퍼스널컬러 진단받기</AppButton>
    </EmptyState>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import apiClient from '@/api/client'
import { formatPersonalColor, type RecommendationItem } from '@/types'
import { useAuthStore } from '@/stores/auth'
import ProductCard from '@/components/ui/ProductCard.vue'
import SectionHeader from '@/components/ui/SectionHeader.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'

const authStore = useAuthStore()
const recommendations = ref<RecommendationItem[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

// 내 퍼스널컬러 라벨 (있을 때만 컨텍스트 배지 노출)
const personalColorLabel = computed(() => formatPersonalColor(authStore.user?.personalColor ?? null))

// 게이지 정규화 — 현재 목록 내 최고 점수를 100%로 환산 (절대 스케일 비의존)
const maxScore = computed(() =>
  Math.max(1, ...recommendations.value.map((item) => item.score)),
)
function scorePercent(score: number): number {
  return Math.round(Math.min(100, Math.max(8, (score / maxScore.value) * 100)))
}

// 추천 사유 — breakdown 중 가장 크게 기여한 항목을 짧은 한 줄로 환산
const REASON_LABELS: Record<keyof RecommendationItem['breakdown'], string> = {
  personalColor: '퍼스널컬러가 잘 맞아요',
  preferredColor: '선호하는 컬러예요',
  style: '취향 스타일과 어울려요',
  popularityScore: '지금 인기 있는 상품이에요',
  reviewScore: '리뷰 평가가 좋아요',
  actionTagScore: '활동 취향에 맞췄어요',
}
function reasonLine(item: RecommendationItem): string {
  const entries = Object.entries(item.breakdown) as [keyof RecommendationItem['breakdown'], number][]
  const top = entries.reduce((best, cur) => (cur[1] > best[1] ? cur : best), entries[0])
  if (!top || top[1] <= 0) return '취향을 두루 고려해 골랐어요'
  return REASON_LABELS[top[0]]
}

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await apiClient.get<RecommendationItem[]>('/recommendations')
    recommendations.value = data
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : '추천 상품을 불러올 수 없습니다'
  } finally {
    loading.value = false
  }
})
</script>
