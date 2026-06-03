<template>
  <div>
    <!-- Contextual Banner -->
    <div class="bg-white border-b border-gray-100">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 py-8 sm:py-12">

        <!-- Loading skeleton -->
        <div v-if="profileLoading" class="animate-pulse space-y-3">
          <div class="h-7 bg-gray-100 rounded-lg w-2/3"></div>
          <div class="h-4 bg-gray-100 rounded w-1/2"></div>
          <div class="h-11 bg-gray-100 rounded-xl w-36 mt-4"></div>
        </div>

        <!-- Not authenticated: Welcome -->
        <template v-else-if="!authStore.isAuthenticated">
          <div class="flex flex-col md:flex-row items-center gap-10">
            <div class="flex-1 text-center md:text-left">
              <span class="inline-block px-3 py-1 bg-purple-50 text-purple-600 text-xs font-semibold rounded-full mb-4 tracking-wide">AI 퍼스널컬러 진단</span>
              <h1 class="text-3xl sm:text-4xl font-bold text-gray-900 mb-4 leading-snug">
                나에게 어울리는 색을 찾아<br>
                <span class="text-purple-600">스타일을 완성하세요</span>
              </h1>
              <p class="text-gray-500 mb-8 leading-relaxed">AI가 당신의 퍼스널컬러를 분석하고 딱 맞는 아이템을 추천해드려요.</p>
              <div class="flex flex-col sm:flex-row gap-3 justify-center md:justify-start">
                <router-link to="/signup" class="px-6 py-3 bg-purple-600 text-white rounded-xl font-semibold hover:bg-purple-700 transition-colors text-center shadow-sm">
                  무료로 시작하기
                </router-link>
                <router-link to="/products" class="px-6 py-3 bg-white text-gray-700 border border-gray-200 rounded-xl font-medium hover:bg-gray-50 transition-colors text-center">
                  상품 둘러보기
                </router-link>
              </div>
            </div>
            <div class="hidden md:flex gap-3 items-center flex-shrink-0">
              <div class="flex flex-col gap-3">
                <div class="w-20 h-20 rounded-2xl bg-gradient-to-br from-amber-300 to-orange-400 shadow-md"></div>
                <div class="w-20 h-20 rounded-2xl bg-gradient-to-br from-sky-300 to-indigo-400 shadow-md"></div>
              </div>
              <div class="flex flex-col gap-3 mt-8">
                <div class="w-20 h-20 rounded-2xl bg-gradient-to-br from-orange-400 to-red-500 shadow-md"></div>
                <div class="w-20 h-20 rounded-2xl bg-gradient-to-br from-violet-500 to-indigo-600 shadow-md"></div>
              </div>
            </div>
          </div>
        </template>

        <!-- NONE: Needs diagnosis -->
        <template v-else-if="diagnosisStatus === 'NONE'">
          <div class="relative bg-gradient-to-r from-purple-600 via-purple-600 to-pink-500 rounded-2xl p-8 sm:p-10 overflow-hidden">
            <div class="absolute right-0 top-0 w-72 h-72 bg-white/10 rounded-full -translate-y-1/2 translate-x-1/4 pointer-events-none"></div>
            <div class="absolute right-20 bottom-0 w-48 h-48 bg-white/5 rounded-full translate-y-1/2 pointer-events-none"></div>
            <div class="relative">
              <span class="inline-block px-3 py-1 bg-white/20 text-white text-xs font-semibold rounded-full mb-4">AI 퍼스널컬러 진단</span>
              <h2 class="text-2xl sm:text-3xl font-bold text-white mb-3">
                퍼스널컬러 진단받고<br>맞춤 상품 받아보세요!
              </h2>
              <p class="text-purple-100 mb-7 text-sm sm:text-base">얼굴 사진 한 장으로 AI가 계절 타입을 분석해드려요.</p>
              <router-link to="/diagnosis" class="inline-flex items-center gap-2 px-6 py-3 bg-white text-purple-600 rounded-xl font-semibold hover:bg-purple-50 transition-colors text-sm shadow-lg">
                지금 진단받기
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
              </router-link>
            </div>
          </div>
        </template>

        <!-- PENDING: Analyzing -->
        <template v-else-if="diagnosisStatus === 'PENDING'">
          <div class="bg-purple-50 border border-purple-100 rounded-2xl p-8 flex flex-col sm:flex-row items-center gap-6">
            <div class="w-16 h-16 bg-purple-100 rounded-full flex items-center justify-center flex-shrink-0">
              <svg class="w-8 h-8 text-purple-500 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
              </svg>
            </div>
            <div class="text-center sm:text-left">
              <h2 class="text-xl font-bold text-gray-900 mb-1.5">AI가 퍼스널컬러를 분석 중이에요</h2>
              <p class="text-gray-500 text-sm">분석이 완료되면 알림으로 알려드릴게요. 자유롭게 둘러보셔도 괜찮아요.</p>
            </div>
          </div>
        </template>

        <!-- COMPLETED: Show result -->
        <template v-else-if="diagnosisStatus === 'COMPLETED'">
          <div :class="`relative bg-gradient-to-r ${seasonGradient} rounded-2xl p-8 sm:p-10 overflow-hidden`">
            <div class="absolute right-0 top-0 w-64 h-64 bg-white/10 rounded-full -translate-y-1/2 translate-x-1/4 pointer-events-none"></div>
            <div class="relative flex flex-col sm:flex-row items-center sm:items-start gap-6">
              <div class="w-20 h-20 rounded-2xl bg-white/20 backdrop-blur-sm flex items-center justify-center flex-shrink-0 shadow-inner border border-white/30">
                <span class="text-3xl">🎨</span>
              </div>
              <div class="text-center sm:text-left">
                <p class="text-white/70 text-sm font-medium mb-1">내 퍼스널컬러</p>
                <h2 class="text-2xl sm:text-3xl font-bold text-white mb-3">{{ formatPersonalColor(personalColor) }}</h2>
                <p class="text-white/70 text-sm mb-6">당신의 계절 타입에 딱 맞는 상품을 추천해드려요.</p>
                <router-link to="/recommendations" class="inline-flex items-center gap-2 px-5 py-2.5 bg-white/20 backdrop-blur-sm text-white border border-white/30 rounded-xl font-medium hover:bg-white/30 transition-colors text-sm">
                  맞춤 상품 보기
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
                </router-link>
              </div>
            </div>
          </div>
        </template>

      </div>
    </div>

    <!-- Products Section -->
    <section class="max-w-7xl mx-auto px-4 sm:px-6 py-10">
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-xl font-bold text-gray-900">
          {{ diagnosisStatus === 'COMPLETED' && recommendations.length > 0 ? '맞춤 추천 상품' : '상품' }}
        </h2>
        <router-link to="/products" class="text-sm text-purple-600 hover:text-purple-700 font-medium">전체보기</router-link>
      </div>

      <div v-if="productStore.loading" class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
        <div v-for="i in 8" :key="i" class="animate-pulse">
          <div class="aspect-square bg-gray-100 rounded-2xl mb-2.5"></div>
          <div class="h-4 bg-gray-100 rounded mb-1.5"></div>
          <div class="h-4 bg-gray-100 rounded w-2/3"></div>
        </div>
      </div>

      <div v-else-if="displayProducts.length > 0" class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
        <router-link
          v-for="product in displayProducts"
          :key="product.id"
          :to="`/products/${product.id}`"
          class="group block"
        >
          <article class="bg-white rounded-2xl border border-gray-100 overflow-hidden hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200">
            <div class="aspect-square bg-gray-50 flex items-center justify-center overflow-hidden">
              <img
                v-if="product.imageUrl"
                :src="product.imageUrl"
                :alt="product.name"
                class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
              />
              <span v-else class="text-4xl">🎨</span>
            </div>
            <div class="p-3">
              <h3 class="text-sm font-medium text-gray-900 truncate">{{ product.name }}</h3>
              <p class="text-sm font-bold text-purple-600 mt-1">{{ formatPrice(product.price) }}원</p>
            </div>
          </article>
        </router-link>
      </div>

      <div v-else class="text-center py-16">
        <p class="text-gray-400 text-sm">등록된 상품이 없습니다</p>
      </div>
    </section>

    <!-- Community Section -->
    <section class="max-w-7xl mx-auto px-4 sm:px-6 pb-14">
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-xl font-bold text-gray-900">커뮤니티</h2>
        <router-link to="/community" class="text-sm text-purple-600 hover:text-purple-700 font-medium">더보기</router-link>
      </div>

      <div v-if="postsLoading" class="space-y-3">
        <div v-for="i in 4" :key="i" class="animate-pulse bg-white rounded-2xl border border-gray-100 p-4">
          <div class="h-5 bg-gray-100 rounded w-3/4 mb-2"></div>
          <div class="h-3 bg-gray-100 rounded w-1/2 mb-3"></div>
          <div class="h-3 bg-gray-100 rounded w-1/4"></div>
        </div>
      </div>

      <div v-else-if="recentPosts.length > 0" class="space-y-3">
        <router-link
          v-for="post in recentPosts"
          :key="post.id"
          :to="`/community/${post.id}`"
          class="flex items-start justify-between gap-4 bg-white rounded-2xl border border-gray-100 p-4 hover:shadow-md hover:border-purple-100 transition-all duration-200"
        >
          <div class="flex-1 min-w-0">
            <h3 class="font-medium text-gray-900 truncate">{{ post.title }}</h3>
            <p class="text-xs text-gray-400 mt-1 truncate">{{ post.content }}</p>
            <div class="flex items-center gap-2 mt-2 text-xs text-gray-400">
              <span>{{ post.authorNickname }}</span>
              <span>·</span>
              <span>{{ formatDate(post.createdAt) }}</span>
            </div>
          </div>
          <div class="flex items-center gap-3 text-xs text-gray-400 flex-shrink-0 mt-0.5">
            <span class="flex items-center gap-1">
              <svg class="w-3.5 h-3.5 text-red-400" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z" clip-rule="evenodd"/>
              </svg>
              {{ post.likeCount }}
            </span>
            <span class="flex items-center gap-1">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
              </svg>
              {{ post.commentCount }}
            </span>
          </div>
        </router-link>
      </div>

      <div v-else class="text-center py-16">
        <p class="text-gray-400 text-sm">아직 게시글이 없어요</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { usePostStore } from '@/stores/posts'
import { useProductStore } from '@/stores/products'
import apiClient from '@/api/client'
import type { DiagnosisStatus, User, RecommendationItem } from '@/types'

const authStore = useAuthStore()
const postStore = usePostStore()
const productStore = useProductStore()

const profileLoading = ref(false)
const postsLoading = ref(false)
const diagnosisStatus = ref<DiagnosisStatus>('NONE')
const personalColor = ref<string | null>(null)
const recommendations = ref<RecommendationItem[]>([])

const recentPosts = computed(() => postStore.posts.slice(0, 5))

const displayProducts = computed(() => {
  if (diagnosisStatus.value === 'COMPLETED' && recommendations.value.length > 0) {
    return recommendations.value.slice(0, 8).map(r => r.product)
  }
  return productStore.products.slice(0, 8)
})

const seasonGradient = computed(() => {
  const pc = (personalColor.value ?? '').toLowerCase()
  if (pc.includes('spring')) return 'from-amber-400 via-orange-400 to-pink-400'
  if (pc.includes('summer')) return 'from-sky-400 via-blue-400 to-purple-500'
  if (pc.includes('autumn') || pc.includes('fall')) return 'from-orange-500 via-amber-500 to-red-500'
  if (pc.includes('winter')) return 'from-indigo-600 via-blue-500 to-purple-600'
  return 'from-purple-600 via-purple-500 to-pink-500'
})

const PERSONAL_COLOR_LABELS: Record<string, string> = {
  LIGHT_SPRING: '라이트 스프링',
  WARM_SPRING: '웜 스프링',
  BRIGHT_SPRING: '브라이트 스프링',
  CLEAR_SPRING: '클리어 스프링',
  LIGHT_SUMMER: '라이트 서머',
  COOL_SUMMER: '쿨 서머',
  SOFT_SUMMER: '소프트 서머',
  MUTED_SUMMER: '뮤트 서머',
  SOFT_AUTUMN: '소프트 어텀',
  WARM_AUTUMN: '웜 어텀',
  DEEP_AUTUMN: '딥 어텀',
  MUTED_AUTUMN: '뮤트 어텀',
  CLEAR_WINTER: '클리어 윈터',
  COOL_WINTER: '쿨 윈터',
  DEEP_WINTER: '딥 윈터',
  BRIGHT_WINTER: '브라이트 윈터',
}

function formatPersonalColor(code: string | null): string {
  if (!code) return ''
  return PERSONAL_COLOR_LABELS[code] ?? code
}

function formatPrice(price: number): string {
  return price.toLocaleString('ko-KR')
}

function formatDate(dateStr: string): string {
  const d = new Date(dateStr)
  const now = new Date()
  const diffDays = Math.floor((now.getTime() - d.getTime()) / (1000 * 60 * 60 * 24))
  if (diffDays === 0) return '오늘'
  if (diffDays === 1) return '어제'
  if (diffDays < 7) return `${diffDays}일 전`
  return d.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
}

onMounted(async () => {
  postsLoading.value = true
  await Promise.all([
    postStore.fetchPosts().finally(() => { postsLoading.value = false }),
    productStore.fetchProducts()
  ])

  if (!authStore.isAuthenticated) return

  profileLoading.value = true
  try {
    const { data } = await apiClient.get<User>('/users/me')
    diagnosisStatus.value = data.diagnosisStatus
    personalColor.value = data.personalColor

    if (data.diagnosisStatus === 'COMPLETED') {
      try {
        const { data: recData } = await apiClient.get<RecommendationItem[]>('/recommendations')
        recommendations.value = recData
      } catch {
        // recommendations optional
      }
    }
  } catch {
    // profile fetch failed, keep defaults
  } finally {
    profileLoading.value = false
  }
})
</script>
