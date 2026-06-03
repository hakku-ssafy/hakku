<template>
  <div class="max-w-7xl mx-auto px-4 py-8">
    <section class="bg-gradient-to-br from-purple-50 to-pink-50 rounded-2xl p-8 mb-10">
      <div class="max-w-lg">
        <h1 class="text-3xl font-bold text-gray-900 mb-3">
          나만의 퍼스널컬러로<br>
          <span class="text-purple-600">꾸미기를 시작해보세요</span>
        </h1>
        <p class="text-gray-600 mb-6">AI가 당신의 퍼스널컬러를 진단하고 어울리는 아이템을 추천해드려요</p>
        <div class="flex gap-3">
          <router-link
            v-if="!authStore.isAuthenticated"
            to="/signup"
            class="px-6 py-3 bg-purple-600 text-white rounded-xl font-medium hover:bg-purple-700 transition-colors"
          >
            무료로 시작하기
          </router-link>
          <router-link
            v-else
            to="/diagnosis"
            class="px-6 py-3 bg-purple-600 text-white rounded-xl font-medium hover:bg-purple-700 transition-colors"
          >
            퍼스널컬러 진단
          </router-link>
          <router-link
            to="/products"
            class="px-6 py-3 bg-white text-purple-600 border border-purple-200 rounded-xl font-medium hover:bg-purple-50 transition-colors"
          >
            상품 둘러보기
          </router-link>
        </div>
      </div>
    </section>

    <section v-if="authStore.isAuthenticated && recommendations.length > 0" class="mb-10">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-xl font-bold text-gray-900">나에게 어울리는 상품</h2>
        <router-link to="/recommendations" class="text-sm text-purple-600 hover:underline">더보기</router-link>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <router-link
          v-for="item in recommendations.slice(0, 4)"
          :key="item.product.id"
          :to="`/products/${item.product.id}`"
          class="block"
        >
          <article class="bg-white rounded-xl border border-gray-100 overflow-hidden hover:shadow-md transition-shadow">
            <div class="aspect-square bg-gray-50 flex items-center justify-center">
              <span class="text-4xl">🎨</span>
            </div>
            <div class="p-3">
              <h3 class="text-sm font-medium text-gray-900 truncate">{{ item.product.name }}</h3>
              <p class="text-sm font-bold text-purple-600 mt-1">{{ formatPrice(item.product.price) }}원</p>
            </div>
          </article>
        </router-link>
      </div>
    </section>

    <section>
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-xl font-bold text-gray-900">커뮤니티</h2>
        <router-link to="/community" class="text-sm text-purple-600 hover:underline">더보기</router-link>
      </div>
      <div class="space-y-3">
        <router-link
          v-for="post in recentPosts"
          :key="post.id"
          :to="`/community/${post.id}`"
          class="block bg-white rounded-xl border border-gray-100 p-4 hover:shadow-md transition-shadow"
        >
          <h3 class="font-medium text-gray-900">{{ post.title }}</h3>
          <div class="flex items-center gap-3 mt-2 text-xs text-gray-400">
            <span>{{ post.authorNickname }}</span>
            <span>❤️ {{ post.likeCount }}</span>
            <span>💬 {{ post.commentCount }}</span>
          </div>
        </router-link>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { usePostStore } from '@/stores/posts'
import apiClient from '@/api/client'
import type { RecommendationItem } from '@/types'

const authStore = useAuthStore()
const postStore = usePostStore()

const recommendations = ref<RecommendationItem[]>([])
const recentPosts = ref(postStore.posts.slice(0, 3))

function formatPrice(price: number): string {
  return price.toLocaleString('ko-KR')
}

onMounted(async () => {
  await postStore.fetchPosts()
  recentPosts.value = postStore.posts.slice(0, 3)

  if (authStore.isAuthenticated) {
    try {
      const { data } = await apiClient.get<RecommendationItem[]>('/recommendations')
      recommendations.value = data
    } catch {
      // 추천은 실패해도 홈 화면은 유지
    }
  }
})
</script>
