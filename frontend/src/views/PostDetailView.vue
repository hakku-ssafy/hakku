<template>
  <div class="u-container max-w-3xl py-8 sm:py-10">
    <router-link
      to="/community"
      class="inline-flex items-center gap-1.5 text-sm text-ink-soft hover:text-ink mb-7 transition-colors"
    >
      <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
      </svg>
      커뮤니티
    </router-link>

    <div v-if="loading" class="text-center py-20 text-ink-muted text-sm">불러오는 중...</div>

    <div v-else-if="errorMessage" role="alert" class="text-center py-20 text-red-500 text-sm">
      {{ errorMessage }}
    </div>

    <template v-else-if="post">
      <article class="u-rise">
        <h1 class="u-serif text-title text-ink mb-3">{{ post.title }}</h1>
        <div class="flex items-center gap-3 text-sm text-ink-muted mb-6 flex-wrap pb-6 border-b border-line">
          <span class="font-medium text-ink-soft">{{ post.authorNickname }}</span>
          <span>{{ formatDate(post.createdAt) }}</span>
          <button
            v-if="authStore.isAuthenticated"
            type="button"
            class="flex items-center gap-1 transition-colors tabular-nums"
            :class="post.liked ? 'text-accent' : 'hover:text-ink'"
            :disabled="likeLoading"
            @click="handleLike"
          >
            <HeartIcon :filled="post.liked" /> {{ post.likeCount }}
          </button>
          <span v-else class="flex items-center gap-1 tabular-nums"><HeartIcon :filled="false" /> {{ post.likeCount }}</span>
        </div>
        <img
          v-if="post.imageUrl"
          :src="post.imageUrl"
          :alt="post.title"
          class="w-full max-w-md mx-auto rounded-2xl border border-line mb-6 object-contain"
        />
        <p class="text-ink-soft leading-relaxed whitespace-pre-wrap">{{ post.content }}</p>
      </article>

      <section class="mt-12">
        <h2 class="u-serif text-lg text-ink mb-5">댓글 {{ comments.length }}</h2>

        <div v-if="commentsLoading" class="text-sm text-ink-muted py-4">댓글 불러오는 중...</div>
        <div v-else-if="comments.length === 0" class="text-sm text-ink-muted py-4">아직 댓글이 없습니다</div>
        <ul v-else class="divide-y divide-line border-y border-line mb-7">
          <li v-for="comment in comments" :key="comment.id" class="py-4">
            <div class="flex items-center gap-2 mb-1.5">
              <span class="text-sm font-medium text-ink">{{ comment.authorNickname }}</span>
              <span class="text-xs text-ink-muted">{{ formatDate(comment.createdAt) }}</span>
            </div>
            <p class="text-sm text-ink-soft">{{ comment.content }}</p>
          </li>
        </ul>

        <form v-if="authStore.isAuthenticated" class="space-y-3" @submit.prevent="handleComment">
          <AppTextarea v-model="newComment" placeholder="댓글을 입력하세요" :rows="3" />
          <div v-if="commentError" role="alert" class="text-sm text-red-500">{{ commentError }}</div>
          <div class="flex justify-end">
            <AppButton type="submit" size="sm" :disabled="!newComment.trim() || commentSubmitting" :loading="commentSubmitting">
              댓글 등록
            </AppButton>
          </div>
        </form>
        <p v-else class="text-sm text-ink-muted text-center py-2">
          <router-link to="/login" class="text-ink font-medium hover:underline underline-offset-4">로그인</router-link> 후 댓글을 작성할 수 있습니다
        </p>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { usePostStore } from '@/stores/posts'
import { getPost, getComments, createComment } from '@/api/posts'
import type { Post, Comment } from '@/types'
import AppButton from '@/components/ui/AppButton.vue'
import AppTextarea from '@/components/ui/AppTextarea.vue'

const HeartIcon = (props: { filled?: boolean }) =>
  h('svg', { class: 'w-4 h-4', viewBox: '0 0 24 24', fill: props.filled ? 'currentColor' : 'none', stroke: 'currentColor', 'stroke-width': 1.5 }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z' }),
  ])

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
