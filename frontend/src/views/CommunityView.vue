<template>
  <div class="u-container max-w-3xl py-10 sm:py-12">
    <div class="flex items-end justify-between gap-4 border-b border-line pb-4 mb-5">
      <div>
        <span class="u-eyebrow">Community</span>
        <h1 class="u-serif text-title text-ink mt-2.5">커뮤니티</h1>
      </div>
      <div class="flex gap-2 shrink-0 pb-1">
        <AppButton v-if="authStore.user?.role === 'SELLER'" to="/seller/products" variant="secondary" size="sm">
          상품 올리기
        </AppButton>
        <AppButton v-if="authStore.isAuthenticated" size="sm" @click="showCreateForm = !showCreateForm">
          {{ isShowcase ? '학생증 자랑하기' : '글쓰기' }}
        </AppButton>
      </div>
    </div>

    <!-- 게시판 탭 -->
    <div class="flex gap-1.5 mb-7" role="tablist" aria-label="게시판 선택">
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
    <AppCard v-if="showCreateForm" class="mb-6">
      <h2 class="u-serif text-lg text-ink mb-4">{{ isShowcase ? '꾸민 학생증 자랑하기' : '새 게시글' }}</h2>
      <div v-if="errorMessage" role="alert" class="mb-3 px-3.5 py-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
        {{ errorMessage }}
      </div>
      <div class="space-y-3">
        <!-- 학생증 이미지 업로드 -->
        <div v-if="isShowcase">
          <div
            class="relative rounded-xl border-2 border-dashed p-6 text-center transition-colors"
            :class="imagePreview ? 'border-line-strong' : 'border-line-strong hover:border-accent'"
          >
            <template v-if="imagePreview">
              <img :src="imagePreview" alt="미리보기" class="max-h-64 mx-auto rounded-lg object-contain" />
              <button
                type="button"
                aria-label="이미지 제거"
                class="absolute top-2 right-2 w-8 h-8 bg-canvas rounded-full shadow grid place-items-center text-ink-muted hover:text-accent transition-colors"
                @click="clearImage"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </template>
            <label v-else class="cursor-pointer block">
              <span class="inline-flex items-center h-11 px-5 u-gradient-accent text-accent-ink rounded-full text-sm font-semibold hover:opacity-90 transition-opacity">
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
    <div v-if="store.loading" :class="isShowcase ? 'grid grid-cols-2 sm:grid-cols-3 gap-4' : 'space-y-3'">
      <template v-if="isShowcase">
        <SkeletonBlock v-for="i in 6" :key="i" height="auto" width="100%" class="aspect-[3/4]" />
      </template>
      <template v-else>
        <div v-for="i in 4" :key="i" class="rounded-xl border border-line p-5 space-y-2.5">
          <SkeletonBlock height="1.125rem" width="55%" />
          <SkeletonBlock height="0.875rem" width="85%" />
          <SkeletonBlock height="0.75rem" width="25%" />
        </div>
      </template>
    </div>

    <!-- 학생증 자랑: 갤러리 그리드 -->
    <div v-else-if="isShowcase && store.posts.length > 0" class="grid grid-cols-2 sm:grid-cols-3 gap-4">
      <router-link
        v-for="post in store.posts"
        :key="post.id"
        :to="`/community/${post.id}`"
        class="group block rounded-xl overflow-hidden border border-line bg-surface transition-all duration-200 hover:shadow-md hover:border-line-strong"
      >
        <div class="aspect-[3/4] overflow-hidden bg-surface-soft">
          <img
            v-if="post.imageUrl"
            :src="post.imageUrl"
            :alt="post.title"
            loading="lazy"
            class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
          />
          <div v-else class="w-full h-full grid place-items-center text-3xl text-ink-muted">🎓</div>
        </div>
        <div class="p-3">
          <h3 class="text-sm font-medium text-ink truncate">{{ post.title }}</h3>
          <div class="flex items-center justify-between mt-1.5 text-xs text-ink-muted">
            <span class="truncate">{{ post.authorNickname }}</span>
            <span class="flex items-center gap-1 tabular-nums shrink-0"><HeartIcon :filled="post.liked" /> {{ post.likeCount }}</span>
          </div>
        </div>
      </router-link>
    </div>

    <!-- 자유 게시판: 리스트 -->
    <div v-else-if="!isShowcase && store.posts.length > 0" class="space-y-3">
      <article
        v-for="post in store.posts"
        :key="post.id"
        class="rounded-xl border border-line bg-surface p-5 transition-all duration-200 hover:shadow-md hover:border-line-strong"
      >
        <router-link :to="`/community/${post.id}`" class="block group">
          <h2 class="font-medium text-ink mb-1 group-hover:underline underline-offset-4 decoration-line-strong">{{ post.title }}</h2>
          <p class="text-sm text-ink-muted line-clamp-2">{{ post.content }}</p>
        </router-link>
        <div class="flex items-center gap-4 mt-3.5 text-xs text-ink-muted">
          <span class="font-medium text-ink-soft">{{ post.authorNickname }}</span>
          <button
            v-if="authStore.isAuthenticated"
            type="button"
            class="flex items-center gap-1 transition-colors tabular-nums"
            :class="post.liked ? 'text-accent' : 'hover:text-ink'"
            @click.stop="handleLike(post.id)"
          >
            <HeartIcon :filled="post.liked" /> {{ post.likeCount }}
          </button>
          <span v-else class="flex items-center gap-1 tabular-nums"><HeartIcon :filled="false" /> {{ post.likeCount }}</span>
          <router-link :to="`/community/${post.id}`" class="flex items-center gap-1 hover:text-ink transition-colors tabular-nums">
            <ChatIcon /> {{ post.commentCount }}
          </router-link>
          <span class="ml-auto">{{ formatDate(post.createdAt) }}</span>
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
.board-tab {
  padding: 0.55rem 1.1rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-ink-soft);
  background: var(--color-surface-soft);
  border: 1px solid var(--color-line);
  border-radius: 9999px;
  cursor: pointer;
  transition: color 0.18s ease, background 0.18s ease, border-color 0.18s ease;
}
.board-tab:hover {
  color: var(--color-accent);
}
.board-tab--active {
  color: var(--color-accent-ink);
  background: var(--color-accent);
  border-color: var(--color-accent);
}
</style>
