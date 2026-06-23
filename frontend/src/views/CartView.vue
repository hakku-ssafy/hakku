<template>
  <div class="u-container u-container--cart py-10 sm:py-12">
    <header class="border-b border-line pb-5 mb-8">
      <span class="u-eyebrow">Cart</span>
      <h1 class="u-serif text-title text-ink mt-2.5">장바구니</h1>
    </header>

    <div v-if="loading" class="grid gap-8 lg:grid-cols-[1fr_320px]">
      <ul class="space-y-3">
        <li v-for="n in 3" :key="n" class="rounded-lg border border-line bg-surface p-4 flex gap-4">
          <SkeletonBlock height="4.5rem" width="4.5rem" class="shrink-0 !rounded-md" />
          <div class="flex-1 space-y-2 pt-1">
            <SkeletonBlock height="1rem" width="60%" />
            <SkeletonBlock height="1rem" width="30%" />
          </div>
        </li>
      </ul>
      <div class="rounded-lg border border-line bg-surface p-5 space-y-3 self-start">
        <SkeletonBlock height="1rem" width="50%" />
        <SkeletonBlock height="1rem" width="40%" />
        <SkeletonBlock height="2.75rem" width="100%" class="!rounded-cta" />
      </div>
    </div>

    <div v-else-if="errorMessage" role="alert" class="px-4 py-4 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
      {{ errorMessage }}
    </div>

    <EmptyState v-else-if="items.length === 0" icon="🛒" title="장바구니가 비어있어요" description="마음에 드는 아이템을 담아보세요.">
      <AppButton to="/products">상품 보러가기</AppButton>
    </EmptyState>

    <div v-else class="grid gap-8 lg:grid-cols-[1fr_320px]">
      <!-- 항목 목록 -->
      <ul class="divide-y divide-line-soft rounded-lg border border-line bg-surface">
        <li v-for="item in items" :key="item.id" class="flex items-center gap-4 p-4 sm:p-5">
          <div class="w-[72px] h-[72px] rounded-md u-tone-0 shrink-0 grid place-items-center text-ink-faint" aria-hidden="true">◍</div>

          <div class="flex-1 min-w-0">
            <p class="font-medium text-ink truncate">{{ item.productName }}</p>
            <p class="text-ink-soft text-[13px] mt-1 u-mono tabular-nums">{{ formatPrice(item.price) }}원</p>

            <!-- 수량 스테퍼 (B7) -->
            <div class="mt-3 inline-flex items-center gap-2.5">
              <button
                type="button"
                aria-label="수량 감소"
                :disabled="item.quantity <= 1 || updatingId === item.id"
                class="w-[30px] h-[30px] rounded-full border border-line-control bg-surface grid place-items-center text-ink-soft hover:border-ink hover:text-ink disabled:opacity-35 disabled:cursor-not-allowed disabled:hover:border-line-control transition-colors"
                @click="updateQuantity(item, item.quantity - 1)"
              >
                <svg class="w-3 h-3" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" d="M20 12H4" /></svg>
              </button>
              <span class="w-6 text-center text-sm font-semibold text-ink u-mono tabular-nums">{{ item.quantity }}</span>
              <button
                type="button"
                aria-label="수량 증가"
                :disabled="updatingId === item.id"
                class="w-[30px] h-[30px] rounded-full border border-line-control bg-surface grid place-items-center text-ink-soft hover:border-ink hover:text-ink disabled:opacity-35 disabled:cursor-not-allowed disabled:hover:border-line-control transition-colors"
                @click="updateQuantity(item, item.quantity + 1)"
              >
                <svg class="w-3 h-3" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" d="M12 4v16m8-8H4" /></svg>
              </button>
            </div>
          </div>

          <div class="flex flex-col items-end gap-3 shrink-0">
            <button
              type="button"
              aria-label="항목 삭제"
              :disabled="deletingId === item.id"
              class="text-ink-faint hover:text-ink disabled:opacity-35 disabled:cursor-not-allowed transition-colors text-lg leading-none -mt-0.5"
              @click="removeItem(item.id)"
            >
              ×
            </button>
            <span class="text-ink font-semibold text-sm tabular-nums">{{ formatPrice(item.price * item.quantity) }}원</span>
          </div>
        </li>
      </ul>

      <!-- 주문 요약 (C10) -->
      <aside class="lg:sticky lg:top-[92px] self-start rounded-lg border border-line bg-surface p-6">
        <p class="text-[12px] font-semibold tracking-[0.06em] uppercase text-ink-muted mb-5">Order Summary</p>
        <dl class="space-y-3 text-sm">
          <div class="flex justify-between">
            <dt class="text-ink-soft">상품금액 <span class="u-mono text-ink-faint">({{ totalCount }})</span></dt>
            <dd class="text-ink tabular-nums">{{ formatPrice(totalPrice) }}원</dd>
          </div>
          <div class="flex justify-between">
            <dt class="text-ink-soft">배송비</dt>
            <dd class="tabular-nums" :class="shippingFee === 0 ? 'text-ink-soft' : 'text-ink'">
              {{ shippingFee === 0 ? '무료' : `${formatPrice(shippingFee)}원` }}
            </dd>
          </div>
          <div class="flex justify-between items-baseline pt-3 border-t border-line">
            <dt class="text-ink font-semibold">합계</dt>
            <dd class="text-ink font-extrabold text-base tabular-nums">{{ formatPrice(grandTotal) }}원</dd>
          </div>
        </dl>
        <p class="text-xs text-ink-faint mt-3 leading-relaxed">3만원 이상 구매 시 배송비 무료</p>
        <AppButton block size="lg" class="mt-5">주문하기</AppButton>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import apiClient from '@/api/client'
import type { CartItem } from '@/types'
import AppButton from '@/components/ui/AppButton.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue'

const items = ref<CartItem[]>([])
const loading = ref(false)
const errorMessage = ref('')
const updatingId = ref<number | null>(null)
const deletingId = ref<number | null>(null)

const totalCount = computed(() =>
  items.value.reduce((sum, item) => sum + item.quantity, 0)
)

const totalPrice = computed(() =>
  items.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
)

const FREE_SHIPPING_THRESHOLD = 30000
const SHIPPING_FEE = 3000

const shippingFee = computed(() =>
  totalPrice.value === 0 || totalPrice.value >= FREE_SHIPPING_THRESHOLD ? 0 : SHIPPING_FEE
)

const grandTotal = computed(() => totalPrice.value + shippingFee.value)

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
      items.value = [
        ...items.value.slice(0, index),
        data,
        ...items.value.slice(index + 1)
      ]
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
