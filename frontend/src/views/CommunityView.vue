<template>
  <div class="max-w-3xl mx-auto px-4 py-8">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-gray-900">커뮤니티</h1>
      <button
        v-if="authStore.isAuthenticated"
        @click="showCreateForm = !showCreateForm"
        class="px-4 py-2 bg-purple-600 text-white rounded-lg text-sm font-medium hover:bg-purple-700 transition-colors"
      >
        글쓰기
      </button>
    </div>

    <div v-if="showCreateForm" class="bg-white rounded-xl border border-gray-100 p-5 mb-6">
      <h2 class="font-semibold text-gray-900 mb-4">새 게시글</h2>
      <div class="space-y-3">
        <input
          v-model="newTitle"
          type="text"
          placeholder="제목"
          class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
        />
        <textarea
          v-model="newContent"
          placeholder="내용을 입력하세요"
          rows="4"
          class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 resize-none"
        ></textarea>
        <div class="flex justify-end gap-2">
          <button @click="showCreateForm = false" class="px-4 py-2 text-sm text-gray-600 hover:text-gray-900">취소</button>
          <button
            @click="handleCreatePost"
            :disabled="!newTitle.trim() || !newContent.trim()"
            class="px-4 py-2 bg-purple-600 text-white rounded-lg text-sm disabled:opacity-50"
          >
            등록
          </button>
        </div>
      </div>
    </div>

    <div v-if="store.loading" class="text-center py-20 text-gray-400">불러오는 중...</div>

    <div v-else class="space-y-3">
      <router-link
        v-for="post in store.posts"
        :key="post.id"
        :to="`/community/${post.id}`"
        class="block bg-white rounded-xl border border-gray-100 p-5 hover:shadow-md transition-shadow"
      >
        <h2 class="font-semibold text-gray-900 mb-1">{{ post.title }}</h2>
        <p class="text-sm text-gray-500 line-clamp-2">{{ post.content }}</p>
        <div class="flex items-center gap-4 mt-3 text-xs text-gray-400">
          <span class="font-medium text-gray-600">{{ post.authorNickname }}</span>
          <button
            @click.prevent="store.toggleLikeAction(post.id)"
            class="flex items-center gap-1 hover:text-red-500 transition-colors"
          >
            ❤️ {{ post.likeCount }}
          </button>
          <span>💬 {{ post.commentCount }}</span>
          <span class="ml-auto">{{ formatDate(post.createdAt) }}</span>
        </div>
      </router-link>
    </div>

    <div v-if="!store.loading && store.posts.length === 0" class="text-center py-20 text-gray-400">
      아직 게시글이 없습니다
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { usePostStore } from '@/stores/posts'

const authStore = useAuthStore()
const store = usePostStore()

const showCreateForm = ref(false)
const newTitle = ref('')
const newContent = ref('')

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('ko-KR')
}

async function handleCreatePost() {
  if (!newTitle.value.trim() || !newContent.value.trim()) return
  await store.createPostAction({ title: newTitle.value, content: newContent.value })
  newTitle.value = ''
  newContent.value = ''
  showCreateForm.value = false
}

onMounted(() => {
  store.fetchPosts()
})
</script>
