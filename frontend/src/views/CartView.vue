<template>
  <div class="u-container u-container--cart cart">
    <h1 class="cart__title">장바구니</h1>

    <!-- 로딩 스켈레톤 -->
    <div v-if="loading" class="cart__grid">
      <ul class="cart__list">
        <li v-for="n in 3" :key="n" class="cart-row">
          <SkeletonBlock height="100px" width="84px" class="shrink-0 !rounded-img" />
          <div class="flex-1 space-y-2 pt-1">
            <SkeletonBlock height="1rem" width="60%" />
            <SkeletonBlock height="1rem" width="30%" />
          </div>
        </li>
      </ul>
      <aside class="cart-summary">
        <SkeletonBlock height="1rem" width="50%" class="mb-4" />
        <SkeletonBlock height="1rem" width="40%" class="mb-4" />
        <SkeletonBlock height="3.25rem" width="100%" class="!rounded-cta" />
      </aside>
    </div>

    <div v-else-if="errorMessage" role="alert" class="cart__error">{{ errorMessage }}</div>

    <div v-else-if="items.length === 0" class="cart__empty">
      <div class="cart__empty-title">장바구니가 비어 있어요</div>
      <p class="cart__empty-desc">마음에 드는 꾸미기 아이템을 담아보세요.</p>
      <AppButton to="/products">쇼핑하러 가기</AppButton>
    </div>

    <div v-else class="cart__grid">
      <!-- 항목 목록 -->
      <ul class="cart__list">
        <li v-for="item in items" :key="item.id" class="cart-row">
          <div class="cart-row__thumb" :class="`u-tone-${item.productId % 8}`" aria-hidden="true" />

          <div class="cart-row__info">
            <div class="cart-row__name">{{ item.productName }}</div>
            <div class="cart-row__price u-mono">{{ formatPrice(item.price) }}원</div>
          </div>

          <div class="cart-row__stepper">
            <button
              type="button"
              aria-label="수량 감소"
              :disabled="item.quantity <= 1 || updatingId === item.id"
              class="stepper-btn"
              @click="updateQuantity(item, item.quantity - 1)"
            >−</button>
            <span class="cart-row__qty u-mono">{{ item.quantity }}</span>
            <button
              type="button"
              aria-label="수량 증가"
              :disabled="updatingId === item.id"
              class="stepper-btn"
              @click="updateQuantity(item, item.quantity + 1)"
            >+</button>
          </div>

          <button
            type="button"
            aria-label="항목 삭제"
            :disabled="deletingId === item.id"
            class="cart-row__remove"
            @click="removeItem(item.id)"
          >×</button>
        </li>
      </ul>

      <!-- 주문 요약 -->
      <aside class="cart-summary">
        <div class="cart-summary__title">주문 요약</div>
        <div class="cart-summary__line">
          <span>상품 금액 <span class="u-mono cart-summary__count">({{ summary.itemCount }})</span></span>
          <span class="tabular-nums">{{ formatPrice(summary.subtotal) }}원</span>
        </div>
        <div class="cart-summary__line">
          <span>배송비</span>
          <span class="tabular-nums">{{ summary.shipping === 0 ? '무료' : `${formatPrice(summary.shipping)}원` }}</span>
        </div>
        <div class="cart-summary__total">
          <span>합계</span>
          <span class="tabular-nums">{{ formatPrice(summary.total) }}원</span>
        </div>
        <p class="cart-summary__note">3만원 이상 구매 시 배송비 무료</p>
        <button type="button" class="cart-summary__cta">주문하기</button>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import apiClient from '@/api/client'
import type { CartItem } from '@/types'
import { calcCartSummary } from '@/lib/cart'
import AppButton from '@/components/ui/AppButton.vue'
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue'

const items = ref<CartItem[]>([])
const loading = ref(false)
const errorMessage = ref('')
const updatingId = ref<number | null>(null)
const deletingId = ref<number | null>(null)

const summary = computed(() => calcCartSummary(items.value))

function formatPrice(price: number): string {
  return price.toLocaleString('ko-KR')
}

async function fetchItems() {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await apiClient.get<CartItem[]>('/cart/items')
    items.value = data
  } catch {
    errorMessage.value = '장바구니를 불러오는데 실패했습니다.'
  } finally {
    loading.value = false
  }
}

async function updateQuantity(item: CartItem, newQuantity: number) {
  if (newQuantity < 1) return
  updatingId.value = item.id
  try {
    const { data } = await apiClient.put<CartItem>(`/cart/items/${item.id}`, { quantity: newQuantity })
    const index = items.value.findIndex((i) => i.id === item.id)
    if (index !== -1) {
      items.value = [...items.value.slice(0, index), data, ...items.value.slice(index + 1)]
    }
  } catch {
    errorMessage.value = '수량 변경에 실패했습니다.'
  } finally {
    updatingId.value = null
  }
}

async function removeItem(id: number) {
  deletingId.value = id
  try {
    await apiClient.delete(`/cart/items/${id}`)
    items.value = items.value.filter((item) => item.id !== id)
  } catch {
    errorMessage.value = '항목 삭제에 실패했습니다.'
  } finally {
    deletingId.value = null
  }
}

onMounted(fetchItems)
</script>

<style scoped>
.cart {
  padding-top: 40px;
  padding-bottom: 90px;
}
.cart__title {
  margin: 0 0 30px;
  font-size: clamp(1.65rem, 1.3rem + 1.6vw, 2rem);
  font-weight: 700;
  letter-spacing: -0.03em;
}

.cart__error {
  padding: 16px;
  border: 1px solid #f0c9c4;
  border-radius: var(--hk-radius-lg);
  background: #fdf3f2;
  color: #c0392b;
  font-size: 0.875rem;
}

.cart__empty {
  border: 1px solid var(--hk-border-soft);
  border-radius: var(--hk-radius-lg);
  background: var(--hk-surface);
  padding: 70px 30px;
  text-align: center;
}
.cart__empty-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
}
.cart__empty-desc {
  margin: 0 0 22px;
  font-size: 13.5px;
  color: var(--hk-text-muted);
}

/* 1fr 320px — < 768 요약 하단 스택 */
.cart__grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 28px;
  align-items: start;
}
@media (min-width: 768px) {
  .cart__grid {
    grid-template-columns: 1fr 320px;
  }
}

.cart__list {
  display: flex;
  flex-direction: column;
  border-top: 1px solid var(--hk-ink);
}
.cart-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 0;
  border-bottom: 1px solid var(--hk-border);
}
.cart-row__thumb {
  width: 84px;
  height: 100px;
  border-radius: var(--hk-radius-img);
  flex: none;
}
.cart-row__info {
  flex: 1;
  min-width: 0;
}
.cart-row__name {
  font-size: 14.5px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cart-row__price {
  font-size: 14px;
  font-weight: 700;
  margin-top: 6px;
}
.cart-row__stepper {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: none;
}
.stepper-btn {
  width: 30px;
  height: 30px;
  border-radius: var(--hk-radius-pill);
  border: 1px solid var(--hk-border-control);
  background: var(--hk-surface);
  font-size: 16px;
  line-height: 1;
  color: var(--hk-ink);
  transition: border-color 0.18s ease, color 0.18s ease;
}
.stepper-btn:hover:not(:disabled) {
  border-color: var(--hk-ink);
}
.stepper-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.cart-row__qty {
  font-size: 14px;
  font-weight: 600;
  width: 18px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}
.cart-row__remove {
  flex: none;
  background: transparent;
  border: none;
  color: var(--hk-text-hint);
  font-size: 18px;
  line-height: 1;
  transition: color 0.18s ease;
}
.cart-row__remove:hover:not(:disabled) {
  color: var(--hk-ink);
}
.cart-row__remove:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* 요약 카드 */
.cart-summary {
  border: 1px solid var(--hk-border);
  border-radius: var(--hk-radius-lg);
  background: var(--hk-surface);
  padding: 24px;
}
@media (min-width: 768px) {
  .cart-summary {
    position: sticky;
    top: 92px;
  }
}
.cart-summary__title {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 18px;
}
.cart-summary__line {
  display: flex;
  justify-content: space-between;
  font-size: 13.5px;
  color: var(--hk-text-muted);
  margin-bottom: 10px;
}
.cart-summary__count {
  color: var(--hk-text-quiet);
}
.cart-summary__total {
  display: flex;
  justify-content: space-between;
  font-size: 16px;
  font-weight: 800;
  border-top: 1px solid var(--hk-border);
  padding-top: 16px;
  margin-top: 6px;
}
.cart-summary__note {
  font-size: 12px;
  color: var(--hk-text-quiet);
  margin: 12px 0 0;
}
.cart-summary__cta {
  width: 100%;
  height: 52px;
  margin-top: 18px;
  border: none;
  border-radius: var(--hk-radius-cta);
  background: var(--accent, #16140f);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: filter 0.18s ease;
}
.cart-summary__cta:hover {
  filter: brightness(0.95);
}
</style>
