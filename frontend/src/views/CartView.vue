<template>
  <div class="max-w-2xl mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold text-gray-900 mb-6">장바구니</h1>

    <div v-if="loading" class="space-y-3">
      <div
        v-for="n in 3"
        :key="n"
        class="bg-white rounded-xl border border-gray-100 p-4 animate-pulse"
      >
        <div class="flex gap-4">
          <div class="w-16 h-16 bg-gray-100 rounded-lg shrink-0" />
          <div class="flex-1 space-y-2">
            <div class="h-4 bg-gray-100 rounded w-2/3" />
            <div class="h-4 bg-gray-100 rounded w-1/3" />
          </div>
        </div>
      </div>
    </div>

    <div
      v-else-if="errorMessage"
      role="alert"
      class="p-4 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm"
    >
      {{ errorMessage }}
    </div>

    <div v-else-if="items.length === 0" class="py-20 text-center">
      <div class="w-16 h-16 bg-gray-50 rounded-full flex items-center justify-center mx-auto mb-4">
        <svg class="w-8 h-8 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="1.5"
            d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"
          />
        </svg>
      </div>
      <p class="text-gray-500 font-medium">장바구니가 비어있어요</p>
      <router-link
        to="/products"
        class="mt-4 inline-block px-5 py-2 bg-purple-600 text-white rounded-lg text-sm font-medium hover:bg-purple-700 transition-colors"
      >
        상품 보러가기
      </router-link>
    </div>

    <div v-else>
      <ul class="space-y-3 mb-6">
        <li
          v-for="item in items"
          :key="item.id"
          class="bg-white rounded-xl border border-gray-100 p-4 flex items-center gap-4"
        >
          <div class="w-16 h-16 bg-purple-50 rounded-lg shrink-0 flex items-center justify-center">
            <svg class="w-7 h-7 text-purple-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="1.5"
                d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
              />
            </svg>
          </div>

          <div class="flex-1 min-w-0">
            <p class="font-medium text-gray-900 truncate">{{ item.productName }}</p>
            <p class="text-purple-600 font-bold text-sm mt-0.5">{{ formatPrice(item.price) }}원</p>
          </div>

          <div class="flex items-center gap-2 shrink-0">
            <button
              type="button"
              aria-label="수량 감소"
              :disabled="item.quantity <= 1 || updatingId === item.id"
              class="w-7 h-7 rounded-lg border border-gray-200 flex items-center justify-center text-gray-500 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
              @click="updateQuantity(item, item.quantity - 1)"
            >
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4" />
              </svg>
            </button>
            <span class="w-7 text-center text-sm font-medium text-gray-900">{{ item.quantity }}</span>
            <button
              type="button"
              aria-label="수량 증가"
              :disabled="updatingId === item.id"
              class="w-7 h-7 rounded-lg border border-gray-200 flex items-center justify-center text-gray-500 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
              @click="updateQuantity(item, item.quantity + 1)"
            >
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
            </button>
          </div>

          <button
            type="button"
            aria-label="항목 삭제"
            :disabled="deletingId === item.id"
            class="w-8 h-8 rounded-lg flex items-center justify-center text-gray-300 hover:text-red-400 hover:bg-red-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors shrink-0"
            @click="removeItem(item.id)"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
              />
            </svg>
          </button>
        </li>
      </ul>

      <div class="bg-white rounded-xl border border-gray-100 p-5">
        <div class="flex justify-between items-center mb-4">
          <span class="text-gray-500 text-sm">총 {{ totalCount }}개 상품</span>
          <span class="text-xl font-bold text-gray-900">{{ formatPrice(totalPrice) }}원</span>
        </div>
        <button
          type="button"
          class="w-full py-3 bg-purple-600 text-white rounded-xl font-medium hover:bg-purple-700 transition-colors"
        >
          구매하기
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import apiClient from '@/api/client'
import type { CartItem } from '@/types'

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
