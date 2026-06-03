<template>
  <div class="max-w-3xl mx-auto px-4 py-8">
    <router-link
      to="/community"
      class="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-purple-600 mb-6 transition-colors"
    >
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
      </svg>
      커뮤니티
    </router-link>

    <div v-if="loading" class="text-center py-20 text-gray-400">불러오는 중...</div>

    <div v-else-if="errorMessage" role="alert" class="text-center py-20 text-red-500">
      {{ errorMessage }}
    </div>

    <template v-else-if="post">
      <article class="bg-white rounded-xl border border-gray-100 p-6 mb-6">
        <h1 class="text-2xl font-bold text-gray-900 mb-3">{{ post.title }}</h1>
        <div class="flex items-center gap-3 text-sm text-gray-400 mb-4 flex-wrap">
          <span class="font-medium text-gray-600">{{ post.authorNickname }}</span>
          <span>{{ formatDate(post.createdAt) }}</span>
          <button
            v-if="authStore.isAuthenticated"
            type="button"
            class="flex items-center gap-1 transition-colors"
            :class="post.liked ? 'text-red-500' : 'hover:text-red-500'"
            :disabled="likeLoading"
            @click="handleLike"
          >
            <span>{{ post.liked ? '❤️' : '🤍' }}</span> {{ post.likeCount }}
          </button>
          <span v-else class="flex items-center gap-1">❤️ {{ post.likeCount }}</span>
        </div>
        <p class="text-gray-700 leading-relaxed whitespace-pre-wrap">{{ post.content }}</p>
      </article>

      <section class="bg-white rounded-xl border border-gray-100 p-6">
        <h2 class="font-semibold text-gray-900 mb-4">댓글 {{ comments.length }}</h2>

        <div v-if="commentsLoading" class="text-sm text-gray-400 py-4">댓글 불러오는 중...</div>
        <div v-else-if="comments.length === 0" class="text-sm text-gray-400 py-4">아직 댓글이 없습니다</div>
        <ul v-else class="space-y-4 mb-6">
          <li v-for="comment in comments" :key="comment.id" class="border-b border-gray-50 pb-4 last:border-0">
            <div class="flex items-center gap-2 mb-1">
              <span class="text-sm font-medium text-gray-700">{{ comment.authorNickname }}</span>
              <span class="text-xs text-gray-400">{{ formatDate(comment.createdAt) }}</span>
            </div>
            <p class="text-sm text-gray-600">{{ comment.content }}</p>
          </li>
        </ul>

        <form v-if="authStore.isAuthenticated" class="space-y-3" @submit.prevent="handleComment">
          <textarea
            v-model="newComment"
            placeholder="댓글을 입력하세요"
            rows="3"
            class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 resize-none"
          ></textarea>
          <div v-if="commentError" role="alert" class="text-sm text-red-500">{{ commentError }}</div>
          <div class="flex justify-end">
            <button
              type="submit"
              :disabled="!newComment.trim() || commentSubmitting"
              class="px-4 py-2 bg-purple-600 text-white rounded-lg text-sm font-medium hover:bg-purple-700 disabled:opacity-50 transition-colors"
            >
              댓글 등록
            </button>
          </div>
        </form>
        <p v-else class="text-sm text-gray-400 text-center py-2">
          <router-link to="/login" class="text-purple-600 hover:underline">로그인</router-link> 후 댓글을 작성할 수 있습니다
        </p>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { usePostStore } from '@/stores/posts'
import { getPost, getComments, createComment } from '@/api/posts'
import type { Post, Comment } from '@/types'

const route = useRoute()
const authStore = useAuthStore()
const postStore = usePostStore()

const post = ref<Post | null>(null)
const comments = ref<Comment[]>([])
const loading = ref(false)
const commentsLoading = ref(false)
const errorMessage = ref('')
const newComment = ref('')
const commentError = ref('')
const commentSubmitting = ref(false)
const likeLoading = ref(false)

function normalizePost(raw: Post): Post {
  return {
    ...raw,
    authorNickname: raw.authorNickname ?? '알 수 없음',
    likeCount: raw.likeCount ?? 0,
    commentCount: raw.commentCount ?? 0,
    liked: raw.liked ?? false,
  }
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('ko-KR')
}

async function loadComments(postId: number) {
  commentsLoading.value = true
  try {
    comments.value = await getComments(postId)
  } catch {
    comments.value = []
  } finally {
    commentsLoading.value = false
  }
}

async function handleLike() {
  if (!post.value) return
  likeLoading.value = true
  try {
    const result = await postStore.toggleLikeAction(post.value.id)
    post.value = { ...post.value, likeCount: result.likeCount, liked: result.liked }
  } catch {
    errorMessage.value = '좋아요 처리에 실패했습니다.'
  } finally {
    likeLoading.value = false
  }
}

async function handleComment() {
  if (!newComment.value.trim() || !post.value) return
  commentSubmitting.value = true
  commentError.value = ''
  try {
    const created = await createComment(post.value.id, newComment.value.trim())
    comments.value = [...comments.value, created]
    newComment.value = ''
  } catch {
    commentError.value = '댓글 등록에 실패했습니다.'
  } finally {
    commentSubmitting.value = false
  }
}

onMounted(async () => {
  const id = Number(route.params.id)
  if (Number.isNaN(id)) {
    errorMessage.value = '잘못된 게시글 ID입니다.'
    return
  }
  loading.value = true
  try {
    post.value = normalizePost(await getPost(id))
    postStore.setCurrentPost(post.value)
    await loadComments(id)
  } catch {
    errorMessage.value = '게시글을 불러오는데 실패했습니다.'
  } finally {
    loading.value = false
  }
})
</script>
