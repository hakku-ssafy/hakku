<template>
  <div class="u-container u-container--product py-8 sm:py-10">
    <router-link
      to="/products"
      class="inline-flex items-center gap-1.5 text-sm text-ink-muted hover:text-ink mb-7 transition-colors"
    >
      ← 상품 목록으로
    </router-link>

    <div v-if="loading" role="status" class="flex justify-center py-20">
      <svg class="animate-spin h-7 w-7 text-ink" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-20" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" />
        <path class="opacity-80" fill="currentColor" d="M4 12a8 8 0 018-8v3a5 5 0 00-5 5H4z" />
      </svg>
    </div>

    <div v-else-if="errorMessage" role="alert" class="text-center py-20 text-red-500 text-sm">
      {{ errorMessage }}
    </div>

    <article v-else-if="product" class="u-rise">
      <div class="grid lg:grid-cols-2 gap-8 lg:gap-14">
        <!-- 이미지 -->
        <div
          class="aspect-square rounded-md border border-line flex items-center justify-center overflow-hidden"
          :class="product.imageUrl ? 'bg-cream' : toneClass"
        >
          <img v-if="product.imageUrl" :src="product.imageUrl" :alt="product.name" class="w-full h-full object-cover" />
          <span v-else class="text-6xl text-ink/20" aria-hidden="true">◍</span>
        </div>

        <!-- 정보 -->
        <div>
          <div class="flex items-start justify-between gap-4">
            <div class="min-w-0">
              <p v-if="product.category" class="text-[0.65rem] uppercase tracking-[0.14em] font-semibold text-ink-faint mb-2">{{ product.category }}</p>
              <h1 class="u-serif text-title text-ink">{{ product.name }}</h1>
            </div>

            <button
              type="button"
              class="shrink-0 inline-flex flex-col items-center justify-center gap-0.5 rounded-md border px-4 py-2.5 transition-colors u-pop"
              :class="wishlisted ? 'border-accent bg-accent-soft' : 'border-line bg-surface hover:border-ink'"
              :aria-pressed="wishlisted"
              aria-label="찜하기"
              @click="handleWishlistToggle"
            >
              <span
                class="text-xl leading-none"
                :class="wishlisted ? 'text-accent' : ''"
                :style="wishlisted ? undefined : { color: 'var(--hk-heart-off)' }"
              >
                {{ wishlisted ? '♥' : '♡' }}
              </span>
              <span class="u-mono text-xs text-ink-soft tabular-nums">{{ wishlistCount }}</span>
            </button>
          </div>

          <p class="text-2xl font-bold text-ink mt-4 tabular-nums">
            {{ formatPrice(product.price) }}<span class="text-ink-muted font-normal text-lg">원</span>
          </p>

          <div v-if="product.colors.length > 0" class="flex flex-wrap gap-1.5 mt-5">
            <AppBadge v-for="color in product.colors" :key="color">{{ getColorLabel(color) }}</AppBadge>
          </div>

          <p class="text-ink-soft leading-relaxed whitespace-pre-wrap mt-6 mb-8">{{ product.description }}</p>

          <AppButton
            v-if="product.purchaseUrl"
            :href="product.purchaseUrl"
            target="_blank"
            rel="noopener noreferrer"
            variant="soft"
            size="lg"
            block
          >
            구매처로 이동
          </AppButton>
          <p v-else class="text-sm text-ink-muted text-center py-2">구매 링크가 아직 등록되지 않았습니다.</p>
        </div>
      </div>

      <!-- 리뷰 -->
      <section aria-labelledby="reviews-heading" class="mt-12 pt-8 border-t border-line">
        <div class="flex items-baseline justify-between mb-5">
          <h2 id="reviews-heading" class="u-serif text-lg text-ink">
            리뷰 <span class="u-mono text-ink-muted tabular-nums">{{ reviews.length }}</span>
          </h2>
          <div v-if="reviews.length > 0" class="flex items-center gap-1.5">
            <StarRating :rating="Math.round(averageRating)" size="sm" />
            <span class="u-mono text-sm text-ink-soft tabular-nums">{{ averageRating.toFixed(1) }}</span>
          </div>
        </div>

        <!-- 내 리뷰 작성/수정 폼 -->
        <div v-if="canWrite" class="rounded-block border border-line bg-surface-soft p-4 mb-6">
          <p class="text-sm font-medium text-ink mb-2.5">
            {{ editingReviewId ? '리뷰 수정' : '리뷰 작성' }}
          </p>
          <div class="flex items-center gap-2 mb-3">
            <StarRating v-model:rating="form.rating" editable size="lg" />
            <span class="text-sm text-ink-muted">{{ form.rating }}점</span>
          </div>
          <AppTextarea
            v-model="form.content"
            :rows="3"
            placeholder="상품에 대한 솔직한 후기를 남겨주세요."
          />
          <div v-if="formError" class="text-xs text-red-500 mt-2">{{ formError }}</div>
          <div class="flex justify-end gap-2 mt-3">
            <AppButton v-if="editingReviewId" variant="ghost" size="sm" @click="cancelEdit">취소</AppButton>
            <AppButton size="sm" :loading="submitting" @click="submitReview">
              {{ editingReviewId ? '수정 완료' : '등록' }}
            </AppButton>
          </div>
        </div>

        <p v-else-if="!isAuthenticated" class="text-sm text-ink-muted mb-6">
          <router-link :to="loginLink" class="text-accent hover:underline underline-offset-4">로그인</router-link>
          후 리뷰를 작성할 수 있어요.
        </p>

        <!-- 내가 쓴 리뷰 (수정 모드 아닐 때) -->
        <template v-if="myReview && !editingReviewId">
          <ReviewItem
            :review="myReview"
            can-manage
            @edit="startEdit"
            @delete="removeReview"
          />
        </template>

        <EmptyState
          v-if="reviews.length === 0"
          icon="✍️"
          title="아직 리뷰가 없어요"
          description="첫 번째 리뷰를 남겨보세요."
        />
        <div v-else>
          <ReviewItem v-for="review in otherReviews" :key="review.id" :review="review" />
        </div>
      </section>
    </article>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getProduct,
  getReviews,
  createReview,
  updateReview,
  deleteReview,
} from '@/api/products'
import { getWishlistStatus, toggleWishlist } from '@/api/wishlist'
import { useAuthStore } from '@/stores/auth'
import { COLOR_OPTIONS } from '@/types'
import type { Product, Review } from '@/types'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppTextarea from '@/components/ui/AppTextarea.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import StarRating from '@/components/social/StarRating.vue'
import ReviewItem from '@/components/social/ReviewItem.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const product = ref<Product | null>(null)
const loading = ref(false)
const errorMessage = ref('')

const reviews = ref<Review[]>([])
const wishlisted = ref(false)
const wishlistCount = ref(0)

const form = ref<{ rating: number; content: string }>({ rating: 5, content: '' })
const editingReviewId = ref<number | null>(null)
const submitting = ref(false)
const formError = ref('')

const isAuthenticated = computed(() => authStore.isAuthenticated)
const myUserId = computed(() => authStore.user?.id ?? null)
const productId = computed(() => Number(route.params.id))
const loginLink = computed(() => ({ path: '/login', query: { redirect: route.fullPath } }))

const myReview = computed(() => reviews.value.find((r) => r.authorId === myUserId.value) ?? null)
const otherReviews = computed(() => reviews.value.filter((r) => r.authorId !== myUserId.value))
const averageRating = computed(() =>
  reviews.value.length === 0
    ? 0
    : reviews.value.reduce((sum, r) => sum + r.rating, 0) / reviews.value.length,
)
// 인증됐고, (내 리뷰가 없거나) 수정 중일 때 폼 노출
const canWrite = computed(
  () => isAuthenticated.value && (myReview.value === null || editingReviewId.value !== null),
)

function formatPrice(price: number): string {
  return price.toLocaleString('ko-KR')
}

function getColorLabel(value: string): string {
  return COLOR_OPTIONS.find((c) => c.value === value)?.label ?? value
}

// 이미지 미등록 시 상품 id 로 안정적인 웜 톤 그라데이션(8종) 도출 — ProductCard 와 동일 규칙
const toneClass = computed(() => {
  const n = Number(productId.value)
  const idNum = Number.isFinite(n) ? Math.abs(Math.trunc(n)) : 0
  return `u-tone-${idNum % 8}`
})

async function handleWishlistToggle() {
  if (!isAuthenticated.value) {
    router.push(loginLink.value)
    return
  }
  // 낙관적 업데이트
  const prev = { wishlisted: wishlisted.value, count: wishlistCount.value }
  wishlisted.value = !wishlisted.value
  wishlistCount.value += wishlisted.value ? 1 : -1
  try {
    const status = await toggleWishlist(productId.value)
    wishlisted.value = status.wishlisted
    wishlistCount.value = status.wishlistCount
  } catch {
    wishlisted.value = prev.wishlisted
    wishlistCount.value = prev.count
  }
}

function startEdit(review: Review) {
  editingReviewId.value = review.id
  form.value = { rating: review.rating, content: review.content }
  formError.value = ''
}

function cancelEdit() {
  editingReviewId.value = null
  form.value = { rating: 5, content: '' }
  formError.value = ''
}

async function submitReview() {
  if (form.value.content.trim().length === 0) {
    formError.value = '리뷰 내용을 입력해주세요.'
    return
  }
  submitting.value = true
  formError.value = ''
  try {
    const payload = { rating: form.value.rating, content: form.value.content.trim() }
    if (editingReviewId.value) {
      const updated = await updateReview(editingReviewId.value, payload)
      reviews.value = reviews.value.map((r) => (r.id === updated.id ? updated : r))
    } else {
      const created = await createReview(productId.value, payload)
      reviews.value = [created, ...reviews.value]
    }
    cancelEdit()
  } catch {
    formError.value = '리뷰 저장에 실패했어요. 잠시 후 다시 시도해주세요.'
  } finally {
    submitting.value = false
  }
}

async function removeReview(review: Review) {
  try {
    await deleteReview(review.id)
    reviews.value = reviews.value.filter((r) => r.id !== review.id)
  } catch {
    // 실패 시 목록 유지
  }
}

onMounted(async () => {
  if (Number.isNaN(productId.value)) {
    errorMessage.value = '잘못된 상품 ID입니다.'
    return
  }
  loading.value = true
  try {
    product.value = await getProduct(productId.value)
    const [reviewList, status] = await Promise.all([
      getReviews(productId.value).catch(() => [] as Review[]),
      getWishlistStatus(productId.value).catch(() => ({ wishlisted: false, wishlistCount: 0 })),
    ])
    reviews.value = reviewList
    wishlisted.value = status.wishlisted
    wishlistCount.value = status.wishlistCount
  } catch {
    errorMessage.value = '상품을 불러오는데 실패했습니다.'
  } finally {
    loading.value = false
  }
})
</script>
