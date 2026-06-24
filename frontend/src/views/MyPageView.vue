<template>
  <div class="u-container u-container--mypage py-10 sm:py-12">
    <header class="border-b border-line pb-5 mb-7">
      <span class="u-eyebrow">My Page</span>
      <h1 class="u-serif text-title text-ink mt-2.5">마이페이지</h1>
    </header>

    <div v-if="loading" class="rounded-lg border border-line bg-surface p-6 space-y-4">
      <div class="flex items-center gap-4">
        <SkeletonBlock height="4rem" width="4rem" class="!rounded-full" />
        <div class="space-y-2">
          <SkeletonBlock height="1.25rem" width="8rem" />
          <SkeletonBlock height="1rem" width="12rem" />
        </div>
      </div>
    </div>

    <div v-else-if="errorMessage" role="alert" class="px-4 py-4 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
      {{ errorMessage }}
    </div>

    <template v-else-if="user">
      <!-- 퍼스널컬러 카드 -->
      <section
        class="rounded-lg p-5 mb-7"
        :class="user.personalColor
          ? 'bg-accent-soft border border-accent'
          : 'bg-surface border border-line'"
      >
        <div class="flex items-center justify-between gap-4">
          <div class="min-w-0">
            <p class="text-[11px] font-semibold tracking-[0.18em] uppercase" :class="user.personalColor ? 'text-accent-ink' : 'text-ink-muted'">
              Personal Color
            </p>
            <p v-if="user.personalColor" class="mt-1.5 text-lg font-extrabold text-accent-ink tracking-tight">
              {{ formatPersonalColor(user.personalColor) }}
            </p>
            <p v-else class="mt-1.5 text-base font-semibold text-ink">아직 진단하지 않았어요</p>
          </div>
          <router-link
            to="/diagnosis"
            class="shrink-0 inline-flex items-center gap-1.5 text-sm font-semibold underline-offset-4 hover:underline transition-colors"
            :class="user.personalColor ? 'text-accent-ink' : 'text-ink'"
          >
            {{ user.personalColor ? '재진단' : 'AI 진단 시작' }}
            <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" /></svg>
          </router-link>
        </div>
      </section>

      <!-- 언더라인 탭 (D1) -->
      <nav class="flex gap-6 mb-7 border-b border-line overflow-x-auto" aria-label="마이페이지 탭">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          class="shrink-0 -mb-px pb-3 text-sm font-semibold transition-colors border-b-2"
          :class="activeTab === tab.key
            ? 'text-ink border-ink'
            : 'text-ink-muted border-transparent hover:text-ink'"
          @click="selectTab(tab.key)"
        >
          {{ tab.label }}
        </button>
      </nav>

      <!-- 프로필 -->
      <section v-show="activeTab === 'profile'">
        <div class="rounded-lg border border-line bg-surface p-6 mb-4">
          <div class="flex items-center gap-4 mb-6">
            <div class="w-16 h-16 rounded-full u-tone-0 grid place-items-center shrink-0 ring-1 ring-line">
              <span class="text-2xl font-extrabold text-ink-soft">{{ userInitial }}</span>
            </div>
            <div class="min-w-0">
              <h2 class="u-serif text-lg text-ink">{{ user.nickname }}</h2>
              <p class="text-sm text-ink-muted truncate">{{ user.email }}</p>
            </div>
          </div>

          <dl class="border-t border-line-soft">
            <div class="flex justify-between items-center py-3.5 border-b border-line-soft">
              <dt class="text-[12px] font-semibold tracking-[0.06em] uppercase text-ink-muted">역할</dt>
              <dd><span class="inline-block px-2.5 py-0.5 text-xs font-semibold rounded-full" :class="roleBadge(user.role)">{{ roleLabel(user.role) }}</span></dd>
            </div>

            <div class="flex justify-between items-center py-3.5 border-b border-line-soft">
              <dt class="text-[12px] font-semibold tracking-[0.06em] uppercase text-ink-muted">퍼스널컬러</dt>
              <dd class="text-sm">
                <button
                  v-if="user.personalColor"
                  type="button"
                  class="font-semibold text-accent-ink underline-offset-4 hover:underline cursor-pointer"
                  @click="showDiagnosisModal = true"
                >
                  {{ formatPersonalColor(user.personalColor) }}
                </button>
                <span v-else class="text-ink-muted">미진단</span>
              </dd>
            </div>

            <div class="py-3.5 border-b border-line-soft">
              <div class="flex items-center justify-between mb-2.5">
                <dt class="text-[12px] font-semibold tracking-[0.06em] uppercase text-ink-muted">선호 컬러</dt>
                <button
                  type="button"
                  class="text-xs font-semibold text-ink underline-offset-4 hover:underline"
                  @click="openColorEditor"
                >
                  변경
                </button>
              </div>
              <dd v-if="user.preferredColors.length > 0" class="flex flex-wrap gap-1.5">
                <AppBadge v-for="color in user.preferredColors" :key="color">{{ getColorLabel(color) }}</AppBadge>
              </dd>
              <dd v-else class="text-sm text-ink-muted">아직 선택한 컬러가 없어요.</dd>
            </div>

            <div v-if="user.preferredStyles.length > 0" class="py-3.5 border-b border-line-soft">
              <dt class="text-[12px] font-semibold tracking-[0.06em] uppercase text-ink-muted mb-2.5">선호 스타일</dt>
              <dd class="flex flex-wrap gap-1.5">
                <AppBadge v-for="style in user.preferredStyles" :key="style">{{ style }}</AppBadge>
              </dd>
            </div>
          </dl>
        </div>

        <router-link
          v-if="user.role === 'ADMIN'"
          to="/admin/curation"
          class="flex items-center justify-between w-full bg-surface border border-line rounded-lg px-5 py-4 mb-3 hover:border-line-strong transition-colors"
        >
          <span class="text-sm font-semibold text-ink">큐레이션 카드 관리</span>
          <svg class="w-5 h-5 text-ink-faint shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" /></svg>
        </router-link>

        <button
          type="button"
          class="flex items-center justify-between w-full bg-surface border border-line rounded-lg px-5 py-4 hover:border-line-strong transition-colors"
          @click="handleLogout"
        >
          <span class="text-sm font-semibold text-ink-soft">로그아웃</span>
          <svg class="w-5 h-5 text-ink-faint shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" /></svg>
        </button>
      </section>

      <!-- 내 리뷰 -->
      <section v-show="activeTab === 'reviews'">
        <SkeletonList v-if="reviewsState.loading" />
        <EmptyState
          v-else-if="reviewsState.items.length === 0"
          icon="✍️"
          title="작성한 리뷰가 없어요"
          description="상품 페이지에서 첫 리뷰를 남겨보세요."
        />
        <div v-else class="rounded-lg border border-line bg-surface px-5">
          <ReviewItem v-for="review in reviewsState.items" :key="review.id" :review="review" show-product />
        </div>
      </section>

      <!-- 내 찜 -->
      <section v-show="activeTab === 'wishlist'">
        <SkeletonList v-if="wishlistState.loading" />
        <EmptyState
          v-else-if="wishlistState.items.length === 0"
          icon="♡"
          title="찜한 상품이 없어요"
          description="마음에 드는 상품을 찜해보세요."
        />
        <div v-else class="grid grid-cols-2 gap-3">
          <WishlistCard
            v-for="item in wishlistState.items"
            :key="item.id"
            :item="item"
            :likable="false"
          />
        </div>
      </section>

      <!-- 내 글 -->
      <section v-show="activeTab === 'posts'">
        <SkeletonList v-if="postsState.loading" />
        <EmptyState
          v-else-if="postsState.items.length === 0"
          icon="📝"
          title="작성한 글이 없어요"
          description="커뮤니티에 첫 글을 남겨보세요."
        />
        <ul v-else class="space-y-2">
          <li v-for="post in postsState.items" :key="post.id">
            <router-link
              :to="`/community/${post.id}`"
              class="block rounded-lg border border-line bg-surface p-4 u-pop"
            >
              <div class="flex items-center gap-2 mb-1.5">
                <span class="text-[10px] font-semibold tracking-wide px-2 py-0.5 rounded-full bg-cream text-ink-soft">
                  {{ boardLabel(post.board) }}
                </span>
                <time class="text-xs text-ink-faint u-mono">{{ formatDate(post.createdAt) }}</time>
              </div>
              <p class="text-sm font-semibold text-ink line-clamp-1">{{ post.title }}</p>
              <p class="text-sm text-ink-soft line-clamp-1 mt-0.5">{{ post.content }}</p>
              <div class="flex items-center gap-3 mt-2.5 text-xs text-ink-faint tabular-nums u-mono">
                <span>♥ {{ post.likeCount }}</span>
                <span>💬 {{ post.commentCount }}</span>
              </div>
            </router-link>
          </li>
        </ul>
      </section>

      <!-- 팔로우 -->
      <section v-show="activeTab === 'follow'">
        <div class="flex gap-2 mb-5">
          <button
            v-for="sub in followSubTabs"
            :key="sub.key"
            type="button"
            class="flex-1 py-2.5 rounded-full text-sm font-semibold border transition-colors"
            :class="followSub === sub.key
              ? 'border-ink bg-ink text-white'
              : 'border-line text-ink-soft hover:border-line-strong hover:text-ink'"
            @click="followSub = sub.key"
          >
            {{ sub.label }}
            <span class="u-mono tabular-nums ml-1">{{ sub.key === 'followers' ? followState.followers.length : followState.following.length }}</span>
          </button>
        </div>

        <SkeletonList v-if="followState.loading" />
        <template v-else>
          <template v-if="followSub === 'followers'">
            <EmptyState
              v-if="followState.followers.length === 0"
              icon="👤"
              title="아직 팔로워가 없어요"
              description="활동을 시작하면 팔로워가 생겨요."
            />
            <ul v-else class="space-y-2">
              <li v-for="u in followState.followers" :key="u.id"><UserListItem :user="u" /></li>
            </ul>
          </template>
          <template v-else>
            <EmptyState
              v-if="followState.following.length === 0"
              icon="👥"
              title="팔로우한 사람이 없어요"
              description="다른 사용자의 프로필에서 팔로우해보세요."
            />
            <ul v-else class="space-y-2">
              <li v-for="u in followState.following" :key="u.id"><UserListItem :user="u" /></li>
            </ul>
          </template>
        </template>
      </section>
    </template>

    <!-- 진단 이미지 모달 -->
    <AppModal v-model:open="showDiagnosisModal" title="진단 이미지" max-width="md">
      <img v-if="diagnosisPreviewSrc" :src="diagnosisPreviewSrc" alt="퍼스널컬러 진단 이미지" class="w-full rounded-img" />
      <p v-if="user?.personalColor" class="text-center text-sm font-extrabold mt-4 text-accent-ink">
        {{ formatPersonalColor(user?.personalColor ?? null) }}
      </p>
    </AppModal>

    <!-- 선호 컬러 변경 모달 -->
    <AppModal v-model:open="showColorEdit" title="선호 컬러 변경" max-width="md">
      <p class="text-sm text-ink-soft mb-4">마음에 드는 컬러를 여러 개 선택할 수 있어요.</p>
      <div class="flex flex-wrap gap-2 mb-6">
        <button
          v-for="color in selectableColors"
          :key="color.value"
          type="button"
          class="px-4 py-2 rounded-full text-sm font-semibold border-[1.5px] transition-colors"
          :class="draftColors.includes(color.value)
            ? 'border-ink bg-paper-selected text-ink'
            : 'border-line text-ink-soft hover:border-line-strong hover:text-ink'"
          @click="toggleDraftColor(color.value)"
        >
          {{ color.label }}
        </button>
      </div>
      <div v-if="colorError" class="text-xs text-red-500 mb-3">{{ colorError }}</div>
      <div class="flex justify-end gap-2">
        <AppButton variant="ghost" @click="showColorEdit = false">취소</AppButton>
        <AppButton :loading="savingColors" @click="saveColors">저장</AppButton>
      </div>
    </AppModal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { COLOR_OPTIONS, formatPersonalColor } from '@/types'
import type { User, UserRole, UserSummary, Review, WishlistItem, Post, PostBoard } from '@/types'
import { useAuthedImage } from '@/composables/useAuthedImage'
import { getUserReviews } from '@/api/products'
import { getUserWishlist } from '@/api/wishlist'
import { getPostsByAuthor } from '@/api/posts'
import { getFollowers, getFollowing } from '@/api/follows'
import { updatePreferredColors } from '@/api/users'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppModal from '@/components/ui/AppModal.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue'
import ReviewItem from '@/components/social/ReviewItem.vue'
import WishlistCard from '@/components/social/WishlistCard.vue'
import UserListItem from '@/components/social/UserListItem.vue'
import SkeletonList from '@/components/social/SkeletonList.vue'

type TabKey = 'profile' | 'reviews' | 'wishlist' | 'posts' | 'follow'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const user = ref<User | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const showDiagnosisModal = ref(false)

const tabs: { key: TabKey; label: string }[] = [
  { key: 'profile', label: '프로필' },
  { key: 'reviews', label: '리뷰' },
  { key: 'wishlist', label: '찜' },
  { key: 'posts', label: '내 글' },
  { key: 'follow', label: '팔로우' },
]
const activeTab = ref<TabKey>('profile')

const reviewsState = ref<{ loading: boolean; loaded: boolean; items: Review[] }>({ loading: false, loaded: false, items: [] })
const wishlistState = ref<{ loading: boolean; loaded: boolean; items: WishlistItem[] }>({ loading: false, loaded: false, items: [] })
const postsState = ref<{ loading: boolean; loaded: boolean; items: Post[] }>({ loading: false, loaded: false, items: [] })
const followState = ref<{ loading: boolean; loaded: boolean; followers: UserSummary[]; following: UserSummary[] }>({
  loading: false, loaded: false, followers: [], following: [],
})

const followSubTabs: { key: 'followers' | 'following'; label: string }[] = [
  { key: 'followers', label: '팔로워' },
  { key: 'following', label: '팔로잉' },
]
const followSub = ref<'followers' | 'following'>('followers')

// 선호 컬러 편집
const selectableColors = COLOR_OPTIONS.filter((c) => c.value !== 'ALL')
const showColorEdit = ref(false)
const draftColors = ref<string[]>([])
const savingColors = ref(false)
const colorError = ref('')

const diagnosisPreviewUrl = computed(() => user.value?.diagnosisImageUrl ?? user.value?.profileImageUrl ?? null)
const { objectUrl: diagnosisPreviewSrc } = useAuthedImage(diagnosisPreviewUrl)
const userInitial = computed(() => user.value?.nickname?.charAt(0).toUpperCase() ?? '?')

function getColorLabel(value: string): string {
  return COLOR_OPTIONS.find((c) => c.value === value)?.label ?? value
}

function roleLabel(role: UserRole): string {
  switch (role) {
    case 'ADMIN': return '관리자'
    case 'SELLER': return '판매자'
    default: return '일반 회원'
  }
}

function roleBadge(role: UserRole): string {
  switch (role) {
    case 'ADMIN': return 'bg-ink text-white'
    case 'SELLER': return 'border border-line-strong text-ink'
    default: return 'bg-cream text-ink-soft'
  }
}

function boardLabel(board: PostBoard): string {
  return board === 'STUDENT_ID' ? '학생증 자랑' : '자유'
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' })
}

function handleLogout() {
  authStore.logout()
  router.push('/')
}

async function selectTab(tab: TabKey) {
  activeTab.value = tab
  if (tab === 'reviews' && !reviewsState.value.loaded) await loadReviews()
  if (tab === 'wishlist' && !wishlistState.value.loaded) await loadWishlist()
  if (tab === 'posts' && !postsState.value.loaded) await loadPosts()
  if (tab === 'follow' && !followState.value.loaded) await loadFollow()
}

async function loadReviews() {
  if (!user.value) return
  reviewsState.value.loading = true
  try {
    reviewsState.value.items = await getUserReviews(user.value.id)
    reviewsState.value.loaded = true
  } catch {
    reviewsState.value.items = []
  } finally {
    reviewsState.value.loading = false
  }
}

async function loadWishlist() {
  if (!user.value) return
  wishlistState.value.loading = true
  try {
    wishlistState.value.items = await getUserWishlist(user.value.id)
    wishlistState.value.loaded = true
  } catch {
    wishlistState.value.items = []
  } finally {
    wishlistState.value.loading = false
  }
}

async function loadPosts() {
  if (!user.value) return
  postsState.value.loading = true
  try {
    postsState.value.items = await getPostsByAuthor(user.value.id)
    postsState.value.loaded = true
  } catch {
    postsState.value.items = []
  } finally {
    postsState.value.loading = false
  }
}

async function loadFollow() {
  if (!user.value) return
  followState.value.loading = true
  try {
    const [followers, following] = await Promise.all([
      getFollowers(user.value.id),
      getFollowing(user.value.id),
    ])
    followState.value.followers = followers
    followState.value.following = following
    followState.value.loaded = true
  } catch {
    followState.value.followers = []
    followState.value.following = []
  } finally {
    followState.value.loading = false
  }
}

function openColorEditor() {
  draftColors.value = [...(user.value?.preferredColors ?? [])]
  colorError.value = ''
  showColorEdit.value = true
}

function toggleDraftColor(value: string) {
  const idx = draftColors.value.indexOf(value)
  if (idx >= 0) draftColors.value.splice(idx, 1)
  else draftColors.value.push(value)
}

async function saveColors() {
  savingColors.value = true
  colorError.value = ''
  try {
    const updated = await updatePreferredColors(draftColors.value)
    user.value = updated
    authStore.user = updated
    showColorEdit.value = false
  } catch {
    colorError.value = '선호 컬러 저장에 실패했어요.'
  } finally {
    savingColors.value = false
  }
}

watch(
  () => route.query.view,
  (view) => {
    if (view === 'diagnosis' && user.value?.personalColor) showDiagnosisModal.value = true
  },
)

onMounted(async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    await authStore.fetchMe()
    user.value = authStore.user
    if (route.query.view === 'diagnosis' && user.value?.personalColor) showDiagnosisModal.value = true
  } catch {
    errorMessage.value = '사용자 정보를 불러오는데 실패했습니다.'
  } finally {
    loading.value = false
  }
})
</script>
