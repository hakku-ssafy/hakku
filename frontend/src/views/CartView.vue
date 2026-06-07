<template>
  <div class="u-container max-w-2xl py-10 sm:py-12">
    <div class="border-b border-line pb-4 mb-7">
      <span class="u-eyebrow">Cart</span>
      <h1 class="u-serif text-title text-ink mt-2.5">장바구니</h1>
    </div>

    <div v-if="loading" class="space-y-3">
      <div v-for="n in 3" :key="n" class="rounded-xl border border-line p-4 flex gap-4">
        <SkeletonBlock height="4rem" width="4rem" class="shrink-0" />
        <div class="flex-1 space-y-2 pt-1">
          <SkeletonBlock height="1rem" width="60%" />
          <SkeletonBlock height="1rem" width="30%" />
        </div>
      </div>
    </div>

    <div v-else-if="errorMessage" role="alert" class="px-4 py-4 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm">
      {{ errorMessage }}
    </div>

    <EmptyState v-else-if="items.length === 0" icon="🛒" title="장바구니가 비어있어요" description="마음에 드는 아이템을 담아보세요.">
      <AppButton to="/products">상품 보러가기</AppButton>
    </EmptyState>

    <div v-else>
      <ul class="space-y-3 mb-6">
        <li v-for="item in items" :key="item.id" class="rounded-xl border border-line bg-surface p-4 flex items-center gap-4">
          <div class="w-16 h-16 rounded-lg bg-surface-sunken shrink-0 grid place-items-center text-ink-muted" aria-hidden="true">◍</div>

          <div class="flex-1 min-w-0">
            <p class="font-medium text-ink truncate">{{ item.productName }}</p>
            <p class="text-ink-soft font-semibold text-sm mt-0.5 tabular-nums">{{ formatPrice(item.price) }}원</p>
          </div>

          <div class="flex items-center gap-2 shrink-0">
            <button
              type="button"
              aria-label="수량 감소"
              :disabled="item.quantity <= 1 || updatingId === item.id"
              class="w-7 h-7 rounded-lg border border-line-strong grid place-items-center text-ink-soft hover:bg-surface-sunken disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
              @click="updateQuantity(item, item.quantity - 1)"
            >
              <svg class="w-3 h-3" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" d="M20 12H4" /></svg>
            </button>
            <span class="w-7 text-center text-sm font-medium text-ink tabular-nums">{{ item.quantity }}</span>
            <button
              type="button"
              aria-label="수량 증가"
              :disabled="updatingId === item.id"
              class="w-7 h-7 rounded-lg border border-line-strong grid place-items-center text-ink-soft hover:bg-surface-sunken disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
              @click="updateQuantity(item, item.quantity + 1)"
            >
              <svg class="w-3 h-3" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" d="M12 4v16m8-8H4" /></svg>
            </button>
          </div>

          <button
            type="button"
            aria-label="항목 삭제"
            :disabled="deletingId === item.id"
            class="w-8 h-8 rounded-lg grid place-items-center text-ink-muted hover:text-red-500 hover:bg-red-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors shrink-0"
            @click="removeItem(item.id)"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </button>
        </li>
      </ul>

      <div class="rounded-xl border border-line bg-surface p-5">
        <div class="flex justify-between items-center mb-4">
          <span class="text-ink-soft text-sm">총 {{ totalCount }}개 상품</span>
          <span class="text-xl font-semibold text-ink tabular-nums">{{ formatPrice(totalPrice) }}원</span>
        </div>
        <AppButton block size="lg">구매하기</AppButton>
      </div>
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
