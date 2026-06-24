<template>
  <div class="u-container u-container--product pd">
    <router-link to="/products" class="pd__back">← 상품 목록으로</router-link>

    <div v-if="loading" role="status" class="pd__center">
      <svg class="animate-spin h-7 w-7 text-ink" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-20" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" />
        <path class="opacity-80" fill="currentColor" d="M4 12a8 8 0 018-8v3a5 5 0 00-5 5H4z" />
      </svg>
    </div>

    <div v-else-if="errorMessage" role="alert" class="pd__error">{{ errorMessage }}</div>

    <article v-else-if="product" class="u-rise">
      <div class="pd__grid">
        <!-- 갤러리 -->
        <div class="pd__gallery" :class="product.imageUrl ? '' : toneClass">
          <img v-if="product.imageUrl" :src="product.imageUrl" :alt="product.name" class="pd__img" />
          <span v-else class="pd__code u-mono">✦ NO.{{ code }} — IMAGE</span>
        </div>

        <!-- 정보 -->
        <div class="pd__info">
          <span v-if="product.category" class="pd__eyebrow">{{ product.category }}</span>
          <h1 class="pd__name">{{ product.name }}</h1>

          <div v-if="reviews.length > 0" class="pd__rating">
            <StarRating :rating="Math.round(averageRating)" size="sm" />
            <span class="pd__rating-text">평균 {{ averageRating.toFixed(1) }} · 리뷰 {{ reviews.length }}</span>
          </div>

          <div class="pd__price">{{ formatPrice(product.price) }}<span class="pd__won">원</span></div>

          <p class="pd__desc">{{ product.description }}</p>

          <div v-if="product.colors.length > 0" class="pd__colors">
            <div class="pd__colors-label">COLOR</div>
            <div class="pd__color-list">
              <span v-for="color in product.colors" :key="color" class="pd__color-pill">
                <span class="pd__color-dot" :style="{ background: colorHex(color) }" aria-hidden="true" />
                {{ getColorLabel(color) }}
              </span>
            </div>
          </div>

          <div class="pd__cta-row">
            <a
              v-if="product.purchaseUrl"
              :href="product.purchaseUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="pd__buy"
            >구매처로 이동 ↗</a>
            <p v-else class="pd__buy pd__buy--disabled">구매 링크 미등록</p>

            <button type="button" class="pd__buy pd__buy--pay" @click="goToCheckout">결제하기</button>

            <button
              type="button"
              class="pd__wish"
              :class="{ 'pd__wish--on': wishlisted }"
              :aria-pressed="wishlisted"
              aria-label="찜하기"
              @click="handleWishlistToggle"
            >
              <span class="pd__wish-heart">{{ wishlisted ? '♥' : '♡' }}</span>
              <span class="pd__wish-count u-mono">{{ wishlistCount }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 리뷰 -->
      <section aria-labelledby="reviews-heading" class="pd__reviews">
        <div class="pd__reviews-head">
          <h2 id="reviews-heading" class="pd__reviews-title">
            리뷰 <span class="u-mono">{{ reviews.length }}</span>
          </h2>
          <div v-if="reviews.length > 0" class="pd__rating">
            <StarRating :rating="Math.round(averageRating)" size="sm" />
            <span class="pd__rating-text">{{ averageRating.toFixed(1) }}</span>
          </div>
        </div>

        <!-- 내 리뷰 작성/수정 폼 -->
        <div v-if="canWrite" class="pd__form">
          <p class="pd__form-label">{{ editingReviewId ? '리뷰 수정' : '리뷰 작성' }}</p>
          <div class="pd__form-rating">
            <StarRating v-model:rating="form.rating" editable size="lg" />
            <span class="pd__form-rating-text">{{ form.rating }}점</span>
          </div>
          <AppTextarea v-model="form.content" :rows="3" placeholder="상품에 대한 솔직한 후기를 남겨주세요." />
          <div v-if="formError" class="pd__form-error">{{ formError }}</div>
          <div class="pd__form-actions">
            <AppButton v-if="editingReviewId" variant="ghost" size="sm" @click="cancelEdit">취소</AppButton>
            <AppButton size="sm" :loading="submitting" @click="submitReview">
              {{ editingReviewId ? '수정 완료' : '등록' }}
            </AppButton>
          </div>
        </div>

        <p v-else-if="!isAuthenticated" class="pd__login-notice">
          <router-link :to="loginLink" class="text-accent hover:underline underline-offset-4">로그인</router-link>
          후 리뷰를 작성할 수 있어요.
        </p>

        <!-- 내가 쓴 리뷰 (수정 모드 아닐 때 상단) -->
        <ReviewItem
          v-if="myReview && !editingReviewId"
          :review="myReview"
          can-manage
          @edit="startEdit"
          @delete="removeReview"
        />

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
import { getProduct, getReviews, createReview, updateReview, deleteReview } from '@/api/products'
import { getWishlistStatus, toggleWishlist } from '@/api/wishlist'
import { useAuthStore } from '@/stores/auth'
import { COLOR_OPTIONS } from '@/types'
import type { Product, Review } from '@/types'
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
const canWrite = computed(
  () => isAuthenticated.value && (myReview.value === null || editingReviewId.value !== null),
)

// 명명 컬러 → hex (도트 표시용).
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

function formatPrice(price: number): string {
  return price.toLocaleString('ko-KR')
}

function goToCheckout() {
  if (!product.value) return
  if (!isAuthenticated.value) {
    router.push(loginLink.value)
    return
  }
  router.push({
    path: '/payments/checkout',
    query: {
      refType: 'PRODUCT',
      refId: String(product.value.id),
      amount: String(product.value.price),
      name: product.value.name,
    },
  })
}

function getColorLabel(value: string): string {
  return COLOR_OPTIONS.find((c) => c.value === value)?.label ?? value
}

// 이미지 미등록 시 상품 id 로 안정적인 웜 톤 그라데이션(8종) 도출 — ProductCard 와 동일 규칙
const idNum = computed(() => {
  const n = Number(productId.value)
  return Number.isFinite(n) ? Math.abs(Math.trunc(n)) : 0
})
const toneClass = computed(() => `u-tone-${idNum.value % 8}`)
const code = computed(() => String(idNum.value % 100).padStart(2, '0'))

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

<style scoped>
.pd {
  padding-top: 28px;
  padding-bottom: 90px;
}
.pd__back {
  display: inline-block;
  font-size: 13px;
  color: var(--hk-text-muted-2);
  margin-bottom: 24px;
  transition: color 0.18s ease;
}
.pd__back:hover {
  color: var(--hk-ink);
}
.pd__center {
  display: flex;
  justify-content: center;
  padding: 80px 0;
}
.pd__error {
  text-align: center;
  padding: 80px 0;
  font-size: 0.875rem;
  color: #c0392b;
}

/* 2열 — < 900px 1열 스택 */
.pd__grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 32px;
  align-items: start;
}
@media (min-width: 900px) {
  .pd__grid {
    grid-template-columns: 1fr 1fr;
    gap: 56px;
  }
}

.pd__gallery {
  position: relative;
  aspect-ratio: 3 / 4;
  border-radius: var(--hk-radius-md);
  overflow: hidden;
  background: var(--hk-cream);
}
.pd__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.pd__code {
  position: absolute;
  left: 18px;
  bottom: 18px;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.14em;
  color: rgba(22, 20, 15, 0.5);
}

.pd__info {
  padding-top: 6px;
}
.pd__eyebrow {
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--hk-text-quiet);
}
.pd__name {
  margin: 14px 0 12px;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.3;
}
.pd__rating {
  display: flex;
  align-items: center;
  gap: 8px;
}
.pd__rating-text {
  font-size: 13px;
  color: var(--hk-text-muted-2);
}
.pd__price {
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.02em;
  margin: 18px 0 22px;
  font-variant-numeric: tabular-nums;
}
.pd__won {
  font-size: 18px;
  font-weight: 400;
  color: var(--hk-text-muted);
}
.pd__desc {
  margin: 0 0 26px;
  font-size: 14.5px;
  line-height: 1.7;
  color: var(--hk-ink-4);
  white-space: pre-wrap;
}

.pd__colors {
  margin-bottom: 28px;
}
.pd__colors-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--hk-text-muted-2);
  letter-spacing: 0.04em;
  margin-bottom: 10px;
}
.pd__color-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.pd__color-pill {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 34px;
  padding: 0 12px 0 8px;
  border-radius: var(--hk-radius-pill);
  border: 1px solid var(--hk-border-chip);
  font-size: 12.5px;
  font-weight: 500;
}
.pd__color-dot {
  width: 16px;
  height: 16px;
  border-radius: var(--hk-radius-pill);
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.12);
}

.pd__cta-row {
  display: flex;
  gap: 11px;
}
.pd__buy {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 56px;
  border-radius: var(--hk-radius-cta);
  background: var(--accent-soft, #f1efec);
  border: 1px solid var(--accent, #16140f);
  color: var(--accent-ink, #16140f);
  font-size: 14px;
  font-weight: 600;
  transition: filter 0.18s ease;
}
.pd__buy:hover {
  filter: brightness(0.98);
}
.pd__buy--disabled {
  background: var(--hk-surface);
  border-color: var(--hk-border);
  color: var(--hk-text-muted);
  cursor: default;
}
.pd__buy--pay {
  background: var(--accent, #16140f);
  border-color: var(--accent, #16140f);
  color: #fff;
}
.pd__buy--pay:hover {
  filter: brightness(0.95);
}
.pd__wish {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  min-width: 56px;
  height: 56px;
  padding: 0 12px;
  border-radius: var(--hk-radius-cta);
  border: 1px solid var(--hk-border-control);
  background: var(--hk-surface);
  transition: border-color 0.18s ease, background 0.18s ease;
}
.pd__wish:hover {
  border-color: var(--hk-ink);
}
.pd__wish--on {
  border-color: var(--accent, #16140f);
  background: var(--accent-soft, #f1efec);
}
.pd__wish-heart {
  font-size: 20px;
  line-height: 1;
  color: var(--hk-heart-off);
}
.pd__wish--on .pd__wish-heart {
  color: var(--accent, #16140f);
}
.pd__wish-count {
  font-size: 11px;
  color: var(--hk-text-muted);
  font-variant-numeric: tabular-nums;
}

/* 리뷰 */
.pd__reviews {
  margin-top: 72px;
  border-top: 1px solid var(--hk-border);
  padding-top: 36px;
}
.pd__reviews-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 24px;
}
.pd__reviews-title {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.pd__form {
  border: 1px solid var(--hk-border);
  border-radius: var(--hk-radius-cta);
  background: var(--hk-surface-soft);
  padding: 16px;
  margin-bottom: 26px;
}
.pd__form-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--hk-ink);
  margin-bottom: 10px;
}
.pd__form-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.pd__form-rating-text {
  font-size: 14px;
  color: var(--hk-text-muted);
}
.pd__form-error {
  font-size: 12px;
  color: #c0392b;
  margin-top: 8px;
}
.pd__form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}
.pd__login-notice {
  font-size: 14px;
  color: var(--hk-text-muted);
  margin-bottom: 24px;
}
</style>
