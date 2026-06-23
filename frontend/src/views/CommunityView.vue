<template>
  <div class="u-container u-container--community py-10 sm:py-12">
    <div class="flex items-end justify-between gap-4 mb-6">
      <div>
        <span class="u-eyebrow">Community</span>
        <h1 class="u-serif text-title text-ink mt-2.5">커뮤니티</h1>
      </div>
      <div class="flex gap-2 shrink-0 pb-1">
        <AppButton v-if="authStore.user?.role === 'SELLER'" to="/seller/products" variant="secondary" size="sm">
          상품 올리기
        </AppButton>
        <AppButton v-if="authStore.isAuthenticated" variant="primary" size="sm" @click="showCreateForm = !showCreateForm">
          {{ isShowcase ? '학생증 자랑하기' : '글쓰기' }}
        </AppButton>
      </div>
    </div>

    <!-- 게시판 탭 (언더라인 탭) -->
    <div class="flex gap-7 mb-8 border-b border-line" role="tablist" aria-label="게시판 선택">
      <button
        type="button"
        role="tab"
        :aria-selected="!isShowcase"
        class="board-tab"
        :class="!isShowcase ? 'board-tab--active' : ''"
        @click="selectBoard('general')"
      >
        자유 게시판
      </button>
      <button
        type="button"
        role="tab"
        :aria-selected="isShowcase"
        class="board-tab"
        :class="isShowcase ? 'board-tab--active' : ''"
        @click="selectBoard('showcase')"
      >
        🎓 학생증 자랑
      </button>
    </div>

    <!-- 작성 폼 -->
    <AppCard v-if="showCreateForm" class="mb-7">
      <h2 class="u-serif text-lg text-ink mb-4">{{ isShowcase ? '꾸민 학생증 자랑하기' : '새 게시글' }}</h2>
      <div v-if="errorMessage" role="alert" class="mb-3 px-3.5 py-3 bg-coral/10 border border-coral/30 rounded-md text-coral text-sm">
        {{ errorMessage }}
      </div>
      <div class="space-y-3">
        <!-- 학생증 이미지 업로드 -->
        <div v-if="isShowcase">
          <div
            class="relative rounded-lg border-[1.5px] border-dashed p-6 text-center transition-colors"
            :class="imagePreview ? 'border-line-control' : 'border-line-dashed hover:border-ink'"
          >
            <template v-if="imagePreview">
              <img :src="imagePreview" alt="미리보기" class="max-h-64 mx-auto rounded-md object-contain" />
              <button
                type="button"
                aria-label="이미지 제거"
                class="absolute top-2 right-2 w-8 h-8 bg-surface border border-line rounded-full shadow-card grid place-items-center text-ink-muted hover:text-ink hover:border-ink transition-colors"
                @click="clearImage"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </template>
            <label v-else class="cursor-pointer block">
              <span class="inline-flex items-center h-11 px-5 bg-accent-soft text-accent-ink border border-accent rounded-full text-sm font-semibold hover:bg-accent-soft/70 transition-colors">
                학생증 사진 올리기
              </span>
              <p class="text-ink-muted text-xs mt-2.5">JPG, PNG, WEBP (최대 10MB)</p>
              <input type="file" accept="image/*" class="hidden" @change="handleImageChange" />
            </label>
          </div>
        </div>

        <AppInput v-model="newTitle" :placeholder="isShowcase ? '한 줄 소개 (예: 가을 웜톤 다꾸 학생증 ✨)' : '제목'" />
        <AppTextarea
          v-model="newContent"
          :placeholder="isShowcase ? '어떻게 꾸몄는지 자랑해 주세요 (선택)' : '내용을 입력하세요'"
          :rows="isShowcase ? 3 : 4"
        />
        <div class="flex justify-end gap-2">
          <AppButton variant="ghost" size="sm" @click="cancelCreate">취소</AppButton>
          <AppButton size="sm" :disabled="!canSubmit || creating" :loading="creating" @click="handleCreatePost">
            등록
          </AppButton>
        </div>
      </div>
    </AppCard>

    <!-- 로딩 -->
    <div v-if="store.loading" :class="isShowcase ? 'grid grid-cols-2 sm:grid-cols-3 gap-5' : 'divide-y divide-line border-y border-line'">
      <template v-if="isShowcase">
        <SkeletonBlock v-for="i in 6" :key="i" height="auto" width="100%" class="aspect-[3/4] !rounded-md" />
      </template>
      <template v-else>
        <div v-for="i in 4" :key="i" class="flex items-start gap-4 py-5">
          <div class="flex-1 space-y-2.5">
            <SkeletonBlock height="1rem" width="55%" />
            <SkeletonBlock height="0.875rem" width="85%" />
            <SkeletonBlock height="0.75rem" width="30%" />
          </div>
          <SkeletonBlock height="3.5rem" width="3.5rem" class="shrink-0 !rounded-md" />
        </div>
      </template>
    </div>

    <!-- 학생증 자랑: 쇼케이스 그리드 -->
    <div v-else-if="isShowcase && store.posts.length > 0" class="grid grid-cols-2 sm:grid-cols-3 gap-5">
      <router-link
        v-for="(post, idx) in store.posts"
        :key="post.id"
        :to="`/community/${post.id}`"
        class="group block"
      >
        <div class="relative">
          <div class="aspect-[3/4] overflow-hidden rounded-md" :class="post.imageUrl ? 'bg-cream' : `u-tone-${idx % 8}`">
            <img
              v-if="post.imageUrl"
              :src="post.imageUrl"
              :alt="post.title"
              loading="lazy"
              class="w-full h-full object-cover transition-transform duration-500 ease-out group-hover:scale-105"
            />
            <div v-else class="w-full h-full grid place-items-center text-3xl text-ink/20" aria-hidden="true">🎓</div>
          </div>
          <span class="u-mono absolute left-2 bottom-2 text-[10px] uppercase tracking-wide text-ink/55">STUDENT ID — NO.{{ String(idx + 1).padStart(2, '0') }}</span>
          <span
            class="absolute top-2 right-2 inline-flex items-center gap-1 h-6 px-2 rounded-full text-[11px] font-semibold tabular-nums backdrop-blur-sm transition-colors"
            :class="post.liked ? 'bg-accent-soft text-accent-ink border border-accent' : 'bg-surface/85 text-ink-soft border border-line'"
          >
            <HeartIcon :filled="post.liked" /> {{ post.likeCount }}
          </span>
        </div>
        <div class="flex items-center gap-2 mt-2.5">
          <span
            class="grid h-6 w-6 shrink-0 place-items-center rounded-full bg-cream text-[10px] font-bold text-ink/70 select-none"
            aria-hidden="true"
          >{{ post.authorNickname?.charAt(0).toUpperCase() ?? '?' }}</span>
          <span class="text-xs font-medium text-ink-soft truncate">{{ post.authorNickname }}</span>
        </div>
        <h3 class="text-sm text-ink truncate mt-1">{{ post.title }}</h3>
      </router-link>
    </div>

    <!-- 자유 게시판: 포스트 행 리스트 -->
    <div v-else-if="!isShowcase && store.posts.length > 0" class="divide-y divide-line border-y border-line">
      <article
        v-for="post in store.posts"
        :key="post.id"
        class="flex items-start gap-4 py-5"
      >
        <router-link :to="`/community/${post.id}`" class="block group min-w-0 flex-1">
          <h2 class="text-base font-semibold text-ink truncate group-hover:underline underline-offset-4 decoration-ink/40">{{ post.title }}</h2>
          <p class="text-sm text-ink-muted truncate mt-1">{{ post.content }}</p>
          <div class="flex items-center gap-3 mt-2.5 text-xs text-ink-faint">
            <span class="font-medium text-ink-soft">{{ post.authorNickname }}</span>
            <span>{{ formatDate(post.createdAt) }}</span>
            <span class="flex items-center gap-1 tabular-nums"><ChatIcon /> {{ post.commentCount }}</span>
          </div>
        </router-link>
        <button
          v-if="authStore.isAuthenticated"
          type="button"
          class="like-block"
          :class="post.liked ? 'like-block--active' : ''"
          :aria-pressed="post.liked"
          aria-label="좋아요"
          @click.stop="handleLike(post.id)"
        >
          <HeartIcon :filled="post.liked" />
          <span class="tabular-nums">{{ post.likeCount }}</span>
        </button>
        <div v-else class="like-block like-block--static" aria-hidden="true">
          <HeartIcon :filled="false" />
          <span class="tabular-nums">{{ post.likeCount }}</span>
        </div>
      </article>
    </div>

    <EmptyState
      v-else
      :icon="isShowcase ? '🎓' : '✎'"
      :title="isShowcase ? '아직 자랑된 학생증이 없어요' : '아직 게시글이 없어요'"
      :description="isShowcase ? '꾸민 학생증을 가장 먼저 자랑해보세요!' : '첫 글을 남겨 대화를 시작해보세요.'"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { usePostStore } from '@/stores/posts'
import { uploadShowcaseImage } from '@/api/storage'
import type { PostBoard } from '@/types'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppTextarea from '@/components/ui/AppTextarea.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue'

const MAX_IMAGE_BYTES = 10 * 1024 * 1024

const HeartIcon = (props: { filled?: boolean }) =>
  h('svg', { class: 'w-3.5 h-3.5', viewBox: '0 0 24 24', fill: props.filled ? 'currentColor' : 'none', stroke: 'currentColor', 'stroke-width': 1.5 }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z' }),
  ])
const ChatIcon = () =>
  h('svg', { class: 'w-3.5 h-3.5', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': 1.5 }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M7.5 8.25h9m-9 3H12m9-1.5a8.96 8.96 0 0 1-4.255 7.626L12 21l-1.745-1.624A8.962 8.962 0 0 1 3 9.75C3 5.444 7.03 2 12 2s9 3.444 9 7.75Z' }),
  ])

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const store = usePostStore()

const activeBoard = computed<PostBoard>(() =>
  route.query.board === 'showcase' ? 'STUDENT_ID' : 'GENERAL',
)
const isShowcase = computed(() => activeBoard.value === 'STUDENT_ID')

const showCreateForm = ref(false)
const newTitle = ref('')
const newContent = ref('')
const errorMessage = ref('')
const creating = ref(false)
const selectedImage = ref<File | null>(null)
const imagePreview = ref<string | null>(null)

const canSubmit = computed(() => {
  if (!newTitle.value.trim()) return false
  if (isShowcase.value) return selectedImage.value !== null
  return newContent.value.trim() !== ''
})

function selectBoard(board: 'general' | 'showcase') {
  if ((board === 'showcase') === isShowcase.value) return
  router.replace({ query: board === 'showcase' ? { board: 'showcase' } : {} })
}

function handleImageChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    errorMessage.value = '이미지 파일만 업로드 가능합니다.'
    return
  }
  if (file.size > MAX_IMAGE_BYTES) {
    errorMessage.value = '파일 크기는 10MB 이하여야 합니다.'
    return
  }
  errorMessage.value = ''
  selectedImage.value = file
  if (imagePreview.value) URL.revokeObjectURL(imagePreview.value)
  imagePreview.value = URL.createObjectURL(file)
}

function clearImage() {
  selectedImage.value = null
  if (imagePreview.value) {
    URL.revokeObjectURL(imagePreview.value)
    imagePreview.value = null
  }
}

function resetForm() {
  newTitle.value = ''
  newContent.value = ''
  errorMessage.value = ''
  clearImage()
}

function cancelCreate() {
  showCreateForm.value = false
  resetForm()
}

async function handleLike(postId: number) {
  try {
    await store.toggleLikeAction(postId)
  } catch {
    errorMessage.value = '좋아요 처리에 실패했습니다.'
  }
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('ko-KR')
}

async function handleCreatePost() {
  if (!canSubmit.value) return
  creating.value = true
  errorMessage.value = ''
  try {
    let imageUrl: string | null = null
    if (isShowcase.value && selectedImage.value) {
      imageUrl = await uploadShowcaseImage(selectedImage.value)
    }
    // 백엔드 content 는 필수이므로, 자랑 글에서 설명이 비면 제목을 본문으로 사용한다.
    const content = newContent.value.trim() || newTitle.value.trim()
    await store.createPostAction({
      title: newTitle.value.trim(),
      content,
      board: activeBoard.value,
      imageUrl,
    })
    showCreateForm.value = false
    resetForm()
    await store.fetchPosts(activeBoard.value)
  } catch {
    errorMessage.value = isShowcase.value
      ? '학생증 자랑 등록에 실패했습니다.'
      : '게시글 등록에 실패했습니다.'
  } finally {
    creating.value = false
  }
}

watch(activeBoard, (board) => {
  showCreateForm.value = false
  resetForm()
  store.fetchPosts(board)
})

onMounted(() => {
  store.fetchPosts(activeBoard.value)
})
</script>

<style scoped>
/* 언더라인 탭 (components.md D1) — 활성 = 먹색 텍스트 + 2px 하단 보더 */
.board-tab {
  position: relative;
  margin-bottom: -1px;
  padding: 0 0 0.85rem;
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-ink-muted);
  background: transparent;
  border: 0;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: color 0.18s ease, border-color 0.18s ease;
}
.board-tab:hover {
  color: var(--color-ink-soft);
}
.board-tab:focus-visible {
  outline: 2px solid var(--color-ink);
  outline-offset: 4px;
  border-radius: 2px;
}
.board-tab--active,
.board-tab--active:hover {
  color: var(--color-ink);
  border-bottom-color: var(--color-ink);
}

/* 세로 좋아요 블록 (components.md C5) — 좋아요 활성 = accent 계열 동시 적용 */
.like-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.2rem;
  width: 3.5rem;
  min-height: 3.5rem;
  padding: 0.5rem 0;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-ink-muted);
  background: var(--color-surface);
  border: 1px solid var(--color-line);
  border-radius: 8px;
  cursor: pointer;
  transition: color 0.18s ease, background 0.18s ease, border-color 0.18s ease;
}
.like-block:hover {
  border-color: var(--color-ink);
  color: var(--color-ink);
}
.like-block:focus-visible {
  outline: 2px solid var(--color-ink);
  outline-offset: 2px;
}
.like-block:active {
  transform: translateY(1px);
}
.like-block--active,
.like-block--active:hover {
  color: var(--color-accent-ink);
  background: var(--color-accent-soft);
  border-color: var(--color-accent);
}
.like-block--static {
  cursor: default;
}
</style>
