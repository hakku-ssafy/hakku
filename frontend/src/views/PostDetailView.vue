<template>
  <div class="u-container u-container--reading py-8 sm:py-10">
    <router-link
      to="/community"
      class="inline-flex items-center gap-1 text-[13px] text-ink-muted hover:text-ink mb-7 transition-colors"
    >
      <span aria-hidden="true">←</span>
      커뮤니티로
    </router-link>

    <div v-if="loading" class="text-center py-20 text-ink-muted text-sm">불러오는 중...</div>

    <div v-else-if="errorMessage" role="alert" class="text-center py-20 text-coral text-sm">
      {{ errorMessage }}
    </div>

    <template v-else-if="post">
      <article class="u-rise">
        <h1 class="post-title">{{ post.title }}</h1>
        <div class="post-meta">
          <span class="post-meta__avatar" aria-hidden="true">{{ authorInitial }}</span>
          <router-link :to="`/users/${post.authorId}`" class="post-meta__author">
            {{ post.authorNickname }}
          </router-link>
          <span class="post-meta__time">{{ formatDate(post.createdAt) }}</span>
        </div>

        <button
          v-if="authStore.isAuthenticated"
          type="button"
          class="like-pill"
          :class="post.liked ? 'like-pill--active' : ''"
          :aria-pressed="post.liked"
          :disabled="likeLoading"
          @click="handleLike"
        >
          <HeartIcon :filled="post.liked" /> 좋아요 <span class="tabular-nums">{{ post.likeCount }}</span>
        </button>
        <span v-else class="like-pill like-pill--static">
          <HeartIcon :filled="false" /> 좋아요 <span class="tabular-nums">{{ post.likeCount }}</span>
        </span>

        <img
          v-if="post.imageUrl"
          :src="post.imageUrl"
          :alt="post.title"
          class="post-image"
        />
        <p class="post-body whitespace-pre-wrap">{{ post.content }}</p>
      </article>

      <!-- 관련 상품: 게시물에서 상품 페이지로 바로 이동 -->
      <section v-if="post.relatedProducts.length" class="mt-10 pt-8 border-t border-line">
        <h2 class="u-serif text-base text-ink mb-4">관련 상품</h2>
        <ul class="grid grid-cols-2 sm:grid-cols-3 gap-3">
          <li v-for="rp in post.relatedProducts" :key="rp.id">
            <router-link
              :to="`/products/${rp.id}`"
              class="group block rounded-md border border-line overflow-hidden hover:border-ink transition-colors"
            >
              <div class="aspect-square bg-cream overflow-hidden">
                <img
                  v-if="rp.imageUrl"
                  :src="rp.imageUrl"
                  :alt="rp.name"
                  loading="lazy"
                  class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
                />
                <div v-else class="w-full h-full grid place-items-center text-2xl text-ink/20" aria-hidden="true">🛍️</div>
              </div>
              <div class="p-2.5">
                <p class="text-[13px] text-ink truncate">{{ rp.name }}</p>
                <p class="text-[13px] font-semibold text-ink mt-0.5 tabular-nums">{{ rp.price.toLocaleString('ko-KR') }}원</p>
              </div>
            </router-link>
          </li>
        </ul>
      </section>

      <section class="mt-12">
        <h2 class="u-serif text-lg text-ink mb-5">댓글 {{ comments.length }}</h2>

        <!-- 인라인 입력 행 (components.md B4) -->
        <form
          v-if="authStore.isAuthenticated"
          class="mb-7"
          @submit.prevent="handleComment"
        >
          <div class="flex items-end gap-2 rounded-md border border-line-control bg-surface px-3.5 py-2 focus-within:border-ink transition-colors">
            <textarea
              v-model="newComment"
              rows="1"
              placeholder="댓글을 입력하세요"
              class="flex-1 resize-none bg-transparent border-0 outline-none text-sm leading-relaxed text-ink placeholder:text-ink-faint py-1.5"
            ></textarea>
            <AppButton type="submit" variant="primary" size="sm" :disabled="!newComment.trim() || commentSubmitting" :loading="commentSubmitting">
              등록
            </AppButton>
          </div>
          <p v-if="commentError" role="alert" class="text-sm text-coral mt-2">{{ commentError }}</p>
        </form>
        <p v-else class="text-sm text-ink-muted text-center py-3 mb-7 rounded-md border border-line bg-surface-soft">
          <router-link to="/login" class="text-ink font-medium hover:underline underline-offset-4">로그인</router-link> 후 댓글을 작성할 수 있습니다
        </p>

        <div v-if="commentsLoading" class="text-sm text-ink-muted py-4">댓글 불러오는 중...</div>
        <div v-else-if="comments.length === 0" class="text-sm text-ink-muted py-8 text-center">아직 댓글이 없습니다</div>
        <ul v-else>
          <li
            v-for="comment in comments"
            :key="comment.id"
            class="flex gap-3 py-4 border-b border-line-soft last:border-b-0"
          >
            <span
              class="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-cream text-[13px] font-bold text-ink/70 select-none"
              aria-hidden="true"
            >{{ comment.authorNickname?.charAt(0).toUpperCase() ?? '?' }}</span>
            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-2">
                <router-link :to="`/users/${comment.authorId}`" class="text-[13px] font-semibold text-ink hover:text-accent transition-colors truncate">
                  {{ comment.authorNickname }}
                </router-link>
                <time class="text-xs text-ink-faint shrink-0">{{ formatDate(comment.createdAt) }}</time>
              </div>
              <p class="mt-1.5 whitespace-pre-wrap text-sm leading-relaxed text-ink-soft">{{ comment.content }}</p>
            </div>
          </li>
        </ul>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { usePostStore } from '@/stores/posts'
import { getPost, getComments, createComment } from '@/api/posts'
import type { Post, Comment } from '@/types'
import AppButton from '@/components/ui/AppButton.vue'

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

const authorInitial = computed(() => post.value?.authorNickname?.charAt(0).toUpperCase() ?? '?')

function normalizePost(raw: Post): Post {
  return {
    ...raw,
    authorNickname: raw.authorNickname ?? '알 수 없음',
    likeCount: raw.likeCount ?? 0,
    commentCount: raw.commentCount ?? 0,
    liked: raw.liked ?? false,
    relatedProducts: raw.relatedProducts ?? [],
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

<style scoped>
.post-title {
  margin: 0 0 14px;
  font-size: clamp(1.5rem, 1.25rem + 1.2vw, 1.75rem);
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.35;
  color: var(--hk-ink);
}
.post-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 22px;
  border-bottom: 1px solid var(--hk-border);
  margin-bottom: 26px;
}
.post-meta__avatar {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: var(--hk-radius-pill);
  background: var(--hk-cream);
  font-size: 13px;
  font-weight: 700;
  color: var(--hk-ink-3);
  flex: none;
  user-select: none;
}
.post-meta__author {
  font-size: 14px;
  font-weight: 600;
  color: var(--hk-ink);
  transition: color 0.18s ease;
}
.post-meta__author:hover {
  color: var(--accent, #16140f);
}
.post-meta__time {
  font-size: 13px;
  color: var(--hk-text-quiet);
}
.post-image {
  display: block;
  width: 100%;
  max-width: 28rem;
  margin: 0 auto 30px;
  border-radius: var(--hk-radius-md);
  border: 1px solid var(--hk-border);
  object-fit: contain;
}

/* 본문 — 에디토리얼 가독성 (reading 폭) */
.post-body {
  color: var(--hk-ink-2, #4f483f);
  font-size: 0.96875rem;
  line-height: 1.8;
}

/* 좋아요 알약 — 좋아요 활성 = accent 계열 동시 적용 */
.like-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  height: 2.75rem;
  margin-bottom: 30px;
  padding: 0 1.375rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-ink-soft);
  background: var(--color-surface);
  border: 1px solid var(--color-line);
  border-radius: 9999px;
  cursor: pointer;
  transition: color 0.18s ease, background 0.18s ease, border-color 0.18s ease;
}
.like-pill:hover {
  border-color: var(--color-ink);
  color: var(--color-ink);
}
.like-pill:focus-visible {
  outline: 2px solid var(--color-ink);
  outline-offset: 2px;
}
.like-pill:active {
  transform: translateY(1px);
}
.like-pill:disabled {
  opacity: 0.55;
  pointer-events: none;
}
.like-pill--active,
.like-pill--active:hover {
  color: var(--color-accent-ink);
  background: var(--color-accent-soft);
  border-color: var(--color-accent);
}
.like-pill--static {
  cursor: default;
}
</style>
