<template>
  <div>
    <!-- ===== 꾸미기 키워드 마퀴 (다크 프로모 띠) ===== -->
    <div class="flex overflow-hidden bg-dark text-cream py-2.5">
      <div class="u-marquee text-[11px] font-semibold uppercase tracking-[0.16em]">
        <span class="px-4">핀뱃지 <span class="text-accent">✦</span></span>
        <span class="px-4">키링 <span class="text-accent">✦</span></span>
        <span class="px-4">꾸미기 스티커 <span class="text-accent">✦</span></span>
        <span class="px-4">퍼스널컬러 <span class="text-accent">✦</span></span>
        <span class="px-4">그립톡 <span class="text-accent">✦</span></span>
        <span class="px-4">키캡 <span class="text-accent">✦</span></span>
        <span class="px-4">다꾸 <span class="text-accent">✦</span></span>
        <span class="px-4">마스킹테이프 <span class="text-accent">✦</span></span>
      </div>
      <div class="u-marquee text-[11px] font-semibold uppercase tracking-[0.16em]" aria-hidden="true">
        <span class="px-4">핀뱃지 <span class="text-accent">✦</span></span>
        <span class="px-4">키링 <span class="text-accent">✦</span></span>
        <span class="px-4">꾸미기 스티커 <span class="text-accent">✦</span></span>
        <span class="px-4">퍼스널컬러 <span class="text-accent">✦</span></span>
        <span class="px-4">그립톡 <span class="text-accent">✦</span></span>
        <span class="px-4">키캡 <span class="text-accent">✦</span></span>
        <span class="px-4">다꾸 <span class="text-accent">✦</span></span>
        <span class="px-4">마스킹테이프 <span class="text-accent">✦</span></span>
      </div>
    </div>

    <!-- ===== Hero / 컨텍스트 배너 ===== -->
    <section class="u-container pt-10 sm:pt-16 pb-12 sm:pb-16">
      <!-- 로딩 스켈레톤 -->
      <div v-if="profileLoading" class="space-y-4">
        <SkeletonBlock height="2.75rem" width="60%" />
        <SkeletonBlock height="1rem" width="40%" />
        <SkeletonBlock height="2.75rem" width="11rem" />
      </div>

      <!-- 비로그인: 웰컴 -->
      <template v-else-if="!authStore.isAuthenticated">
        <div class="grid md:grid-cols-[1.2fr_0.8fr] gap-10 lg:gap-14 items-center">
          <div class="u-rise">
            <span class="u-eyebrow">AI Personal Color</span>
            <h1 class="u-serif text-display text-ink mt-5">
              AI 퍼스널컬러로<br />학생증을 꾸며보자!
            </h1>
            <p class="text-ink-soft mt-6 text-base sm:text-lg leading-relaxed max-w-md">
              얼굴 사진 한 장이면 충분합니다. AI가 16종 퍼스널컬러를 진단하고,
              당신에게 꼭 맞는 학생증 꾸미기 아이템을 골라드려요.
            </p>
            <div class="flex flex-col sm:flex-row gap-3 mt-9">
              <AppButton to="/diagnosis" size="lg">퍼스널컬러 진단 받기</AppButton>
              <AppButton to="/products" variant="secondary" size="lg">상품 둘러보기</AppButton>
            </div>
          </div>
          <!-- 퍼스널컬러 스와치 — 웜 톤 4종 (에디토리얼) -->
          <div class="hidden md:grid grid-cols-2 gap-3" aria-hidden="true">
            <div class="aspect-square rounded-md u-tone-2 border border-line" />
            <div class="aspect-square rounded-md u-tone-4 border border-line translate-y-5" />
            <div class="aspect-square rounded-md u-tone-3 border border-line -translate-y-5" />
            <div class="aspect-square rounded-md u-tone-1 border border-line" />
          </div>
        </div>
      </template>

      <!-- NONE: 진단 필요 — 다크 에디토리얼 패널 -->
      <template v-else-if="diagnosisStatus === 'NONE'">
        <div class="overflow-hidden rounded-modal bg-dark text-on-dark px-8 py-12 sm:px-14 sm:py-16 u-rise">
          <span class="u-eyebrow" style="color: rgba(244, 239, 230, 0.55)">Personal Color Diagnosis</span>
          <h2 class="u-serif text-headline mt-4 max-w-xl">
            퍼스널컬러 진단받고<br />맞춤 상품을 만나보세요
          </h2>
          <p class="mt-5 text-base leading-relaxed" style="color: rgba(244, 239, 230, 0.7)">
            얼굴 사진 한 장으로 AI가 당신의 계절 타입을 분석해드려요.
          </p>
          <AppButton to="/diagnosis" variant="on-dark" size="lg" class="mt-9">
            지금 진단받기
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" /></svg>
          </AppButton>
        </div>
      </template>

      <!-- PENDING: 분석 중 -->
      <template v-else-if="diagnosisStatus === 'PENDING'">
        <div class="rounded-modal border border-line bg-surface-soft px-8 py-10 flex flex-col sm:flex-row items-center gap-6 u-rise">
          <span class="shrink-0 grid place-items-center w-14 h-14 rounded-full border-2 border-accent">
            <svg class="w-6 h-6 text-accent animate-spin" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-20" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" />
              <path class="opacity-80" fill="currentColor" d="M4 12a8 8 0 018-8v3a5 5 0 00-5 5H4z" />
            </svg>
          </span>
          <div class="text-center sm:text-left">
            <h2 class="u-serif text-xl text-ink">AI가 퍼스널컬러를 분석하고 있어요</h2>
            <p class="text-ink-soft text-sm mt-2">완료되면 알림으로 알려드릴게요. 어떻게 학생증을 꾸밀지 고민해보세요.</p>
          </div>
        </div>
      </template>

      <!-- COMPLETED: 결과 — 액센트(퍼스널컬러)로 물든 패널 -->
      <template v-else-if="diagnosisStatus === 'COMPLETED'">
        <div class="overflow-hidden rounded-modal border border-accent bg-accent-soft px-8 py-10 sm:px-12 sm:py-12 u-rise">
          <div class="flex flex-col sm:flex-row sm:items-center gap-8">
            <div class="shrink-0 grid place-items-center w-20 h-20 rounded-md bg-accent text-white text-3xl">
              ✦
            </div>
            <div class="min-w-0">
              <span class="u-eyebrow">My Personal Color</span>
              <h2 class="u-serif text-headline text-ink mt-2.5">{{ formatPersonalColor(personalColor) }}</h2>
              <p class="text-ink-soft text-sm mt-3 mb-7">당신의 계절 타입에 어울리는 상품을 골라드릴게요.</p>
              <div class="flex flex-wrap gap-3">
                <AppButton v-if="diagnosisImageSrc" variant="secondary" size="sm" @click="showDiagnosisModal = true">
                  진단 이미지 보기
                </AppButton>
                <AppButton to="/recommendations" variant="accent" size="sm">
                  맞춤 상품 보기
                </AppButton>
              </div>
            </div>
          </div>
        </div>
      </template>
    </section>

    <!-- ===== 상품 / For You ===== -->
    <section
      v-if="diagnosisStatus === 'COMPLETED' && recommendations.length > 0"
      class="bg-accent-soft py-12 sm:py-14"
    >
      <div class="u-container">
        <SectionHeader eyebrow="For You" title="맞춤 추천 상품">
          <template #action>
            <router-link to="/recommendations" class="text-sm text-ink-soft hover:text-ink transition-colors">전체보기 →</router-link>
          </template>
        </SectionHeader>

        <div v-if="productStore.loading" class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-5">
          <div v-for="i in 8" :key="i">
            <SkeletonBlock height="auto" width="100%" class="aspect-square mb-2.5" />
            <SkeletonBlock height="0.875rem" width="80%" class="mb-1.5" />
            <SkeletonBlock height="0.875rem" width="50%" />
          </div>
        </div>

        <div v-else-if="displayProducts.length > 0" class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-5">
          <ProductCard v-for="product in displayProducts" :key="product.id" :product="product">
            <template #meta>
              <AppBadge variant="accent" class="mt-2">추천 ✦</AppBadge>
            </template>
          </ProductCard>
        </div>

        <EmptyState v-else icon="◍" title="등록된 상품이 없어요" />
      </div>
    </section>

    <!-- ===== 상품 (기본) ===== -->
    <section v-else class="u-container pb-16">
      <SectionHeader eyebrow="Shop" title="상품">
        <template #action>
          <router-link to="/products" class="text-sm text-ink-soft hover:text-ink transition-colors">전체보기 →</router-link>
        </template>
      </SectionHeader>

      <div v-if="productStore.loading" class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-5">
        <div v-for="i in 8" :key="i">
          <SkeletonBlock height="auto" width="100%" class="aspect-square mb-2.5" />
          <SkeletonBlock height="0.875rem" width="80%" class="mb-1.5" />
          <SkeletonBlock height="0.875rem" width="50%" />
        </div>
      </div>

      <div v-else-if="displayProducts.length > 0" class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-5">
        <ProductCard v-for="product in displayProducts" :key="product.id" :product="product" />
      </div>

      <EmptyState v-else icon="◍" title="등록된 상품이 없어요" />
    </section>

    <!-- ===== 커뮤니티 ===== -->
    <section class="u-container pb-8">
      <SectionHeader eyebrow="Community" title="커뮤니티">
        <template #action>
          <router-link to="/community" class="text-sm text-ink-soft hover:text-ink transition-colors">더보기 →</router-link>
        </template>
      </SectionHeader>

      <div v-if="postsLoading" class="divide-y divide-line border-y border-line">
        <div v-for="i in 4" :key="i" class="py-5 space-y-2">
          <SkeletonBlock height="1.125rem" width="50%" />
          <SkeletonBlock height="0.75rem" width="30%" />
        </div>
      </div>

      <ul v-else-if="recentPosts.length > 0" class="divide-y divide-line border-y border-line">
        <li v-for="post in recentPosts" :key="post.id">
          <router-link
            :to="`/community/${post.id}`"
            class="group flex items-start justify-between gap-4 py-5 transition-colors hover:bg-surface-soft -mx-3 px-3 rounded-lg"
          >
            <div class="flex-1 min-w-0">
              <h3 class="font-medium text-ink truncate group-hover:underline underline-offset-4 decoration-ink">{{ post.title }}</h3>
              <p class="text-sm text-ink-muted mt-1 truncate">{{ post.content }}</p>
              <div class="flex items-center gap-2 mt-2.5 text-xs text-ink-muted">
                <span>{{ post.authorNickname }}</span>
                <span aria-hidden="true">·</span>
                <span>{{ formatDate(post.createdAt) }}</span>
              </div>
            </div>
            <div class="u-mono flex items-center gap-3.5 text-xs text-ink-muted shrink-0 mt-1 tabular-nums">
              <span class="flex items-center gap-1">
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z" /></svg>
                {{ post.likeCount }}
              </span>
              <span class="flex items-center gap-1">
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M7.5 8.25h9m-9 3H12m-9.75 1.51c0 1.6 1.123 2.994 2.707 3.227 1.087.16 2.185.283 3.293.369V21l4.184-4.183a1.14 1.14 0 0 1 .778-.332 48.294 48.294 0 0 0 5.83-.498c1.585-.233 2.708-1.626 2.708-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0 0 12 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018Z" /></svg>
                {{ post.commentCount }}
              </span>
            </div>
          </router-link>
        </li>
      </ul>

      <EmptyState v-else icon="✎" title="아직 게시글이 없어요" />
    </section>

    <!-- 진단 이미지 모달 -->
    <AppModal v-model:open="showDiagnosisModal" title="퍼스널컬러 진단 결과" max-width="md">
      <img v-if="diagnosisImageSrc" :src="diagnosisImageSrc" alt="진단 이미지" class="w-full rounded-md" />
      <p v-if="personalColor" class="text-center text-sm font-bold mt-4 text-accent">
        {{ formatPersonalColor(personalColor) }}
      </p>
    </AppModal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { usePostStore } from '@/stores/posts'
import { useProductStore } from '@/stores/products'
import apiClient from '@/api/client'
import { formatPersonalColor, type DiagnosisStatus, type RecommendationItem } from '@/types'
import { useAuthedImage } from '@/composables/useAuthedImage'
import AppButton from '@/components/ui/AppButton.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppModal from '@/components/ui/AppModal.vue'
import ProductCard from '@/components/ui/ProductCard.vue'
import SectionHeader from '@/components/ui/SectionHeader.vue'
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

const authStore = useAuthStore()
const postStore = usePostStore()
const productStore = useProductStore()

const postsLoading = ref(false)
const showDiagnosisModal = ref(false)
const recommendations = ref<RecommendationItem[]>([])

// 진단 상태/퍼스널컬러는 인증 스토어를 단일 소스로 삼는다 → App.vue 폴링이
// PENDING → COMPLETED 로 갱신하면 새로고침 없이 이 화면에 즉시 반영된다.
const profileLoading = computed(() => authStore.isAuthenticated && !authStore.user)
const diagnosisStatus = computed<DiagnosisStatus>(() => authStore.user?.diagnosisStatus ?? 'NONE')
const personalColor = computed(() => authStore.user?.personalColor ?? null)
const diagnosisImageUrl = computed(
  () => authStore.user?.diagnosisImageUrl ?? authStore.user?.profileImageUrl ?? null,
)

// 진단 사진은 인증이 필요한 storage 리소스 → 토큰 붙여 fetch 후 object URL 로 표시
const { objectUrl: diagnosisImageSrc } = useAuthedImage(() => diagnosisImageUrl.value)

const recentPosts = computed(() => postStore.posts.slice(0, 5))

const displayProducts = computed(() => {
  if (diagnosisStatus.value === 'COMPLETED' && recommendations.value.length > 0) {
    return recommendations.value.slice(0, 8).map(r => r.product)
  }
  return productStore.products.slice(0, 8)
})

function formatDate(dateStr: string): string {
  const d = new Date(dateStr)
  const now = new Date()
  const diffDays = Math.floor((now.getTime() - d.getTime()) / (1000 * 60 * 60 * 24))
  if (diffDays === 0) return '오늘'
  if (diffDays === 1) return '어제'
  if (diffDays < 7) return `${diffDays}일 전`
  return d.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
}

async function loadRecommendations() {
  try {
    const { data } = await apiClient.get<RecommendationItem[]>('/recommendations')
    recommendations.value = data
  } catch {
    // recommendations optional
  }
}

onMounted(async () => {
  postsLoading.value = true
  await Promise.all([
    postStore.fetchPosts().finally(() => { postsLoading.value = false }),
    productStore.fetchProducts()
  ])

  if (diagnosisStatus.value === 'COMPLETED') loadRecommendations()
})

// 진단이 완료로 전환되면(폴링 반영 포함) 맞춤 추천을 불러온다.
watch(diagnosisStatus, (status) => {
  if (status === 'COMPLETED' && recommendations.value.length === 0) loadRecommendations()
})
</script>
