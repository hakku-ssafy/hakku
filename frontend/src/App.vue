<template>
  <div id="app-root">
    <nav class="bg-white border-b border-gray-100 sticky top-0 z-50">
      <div class="max-w-7xl mx-auto px-4 h-14 flex items-center justify-between">
        <router-link to="/" class="text-xl font-bold text-purple-600">학꾸</router-link>

        <div class="flex items-center gap-1">
          <router-link to="/products" class="px-3 py-1.5 text-sm text-gray-600 hover:text-gray-900 rounded-lg hover:bg-gray-50">상품</router-link>
          <router-link to="/community" class="px-3 py-1.5 text-sm text-gray-600 hover:text-gray-900 rounded-lg hover:bg-gray-50">커뮤니티</router-link>

          <template v-if="authStore.isAuthenticated">
            <router-link to="/diagnosis" class="px-3 py-1.5 text-sm text-gray-600 hover:text-gray-900 rounded-lg hover:bg-gray-50">진단</router-link>
            <router-link to="/cart" class="px-3 py-1.5 text-sm text-gray-600 hover:text-gray-900 rounded-lg hover:bg-gray-50">장바구니</router-link>
            <router-link to="/notifications" class="px-3 py-1.5 text-sm text-gray-600 hover:text-gray-900 rounded-lg hover:bg-gray-50">알림</router-link>
            <router-link to="/my" class="px-3 py-1.5 text-sm text-purple-600 font-medium hover:bg-purple-50 rounded-lg">마이페이지</router-link>
          </template>
          <template v-else>
            <router-link to="/login" class="px-3 py-1.5 text-sm text-gray-600 hover:text-gray-900 rounded-lg hover:bg-gray-50">로그인</router-link>
            <router-link to="/signup" class="px-4 py-1.5 text-sm bg-purple-600 text-white rounded-lg hover:bg-purple-700">회원가입</router-link>
          </template>
        </div>
      </div>
    </nav>

    <main>
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
</script>
