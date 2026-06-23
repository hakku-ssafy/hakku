<template>
  <div class="u-container u-container--mypage py-10 sm:py-12">
    <button
      type="button"
      class="inline-flex items-center gap-1 text-[13px] text-ink-muted hover:text-ink mb-7 transition-colors"
      @click="goBack"
    >
      <span aria-hidden="true">←</span>
      뒤로
    </button>

    <div v-if="loading" class="rounded-lg border border-line bg-surface p-6">
      <SkeletonBlock height="4rem" width="4rem" class="!rounded-full mb-4" />
      <SkeletonBlock height="1.25rem" width="8rem" />
    </div>

    <div v-else-if="errorMessage" role="alert" class="px-4 py-4 bg-coral/10 border border-coral/30 rounded-md text-coral text-sm">
      {{ errorMessage }}
    </div>

    <template v-else-if="profile">
      <!-- 프로필 헤더 -->
      <div
        class="rounded-lg border p-6 mb-7"
        :class="profile.personalColor ? 'bg-accent-soft border-accent' : 'bg-surface border-line'"
      >
        <div class="flex items-center gap-4">
          <div
            class="w-16 h-16 rounded-full grid place-items-center shrink-0 select-none"
            :class="profile.personalColor ? 'bg-accent text-white' : 'bg-cream text-ink/70'"
          >
            <span class="text-2xl font-bold">{{ initial }}</span>
          </div>
          <div class="min-w-0 flex-1">
            <h1 class="u-serif text-xl text-ink truncate">{{ profile.nickname }}</h1>
            <p v-if="profile.personalColor" class="text-sm text-accent-ink font-semibold mt-0.5">
              {{ formatPersonalColor(profile.personalColor) }}
            </p>
          </div>
        </div>

        <div class="flex items-center gap-6 mt-5 text-sm">
          <div><span class="font-bold text-ink tabular-nums">{{ followerCount }}</span> <span class="text-ink-muted">팔로워</span></div>
          <div><span class="font-bold text-ink tabular-nums">{{ profile.followingCount }}</span> <span class="text-ink-muted">팔로잉</span></div>
        </div>

        <div v-if="profile.preferredColors.length > 0" class="flex flex-wrap gap-1.5 mt-4">
          <AppBadge v-for="color in profile.preferredColors" :key="color" :variant="profile.personalColor ? 'accent' : 'outline'">{{ getColorLabel(color) }}</AppBadge>
        </div>

        <div class="mt-5">
          <AppButton
            v-if="isSelf"
            variant="secondary"
            block
            to="/my"
          >
            내 마이페이지
          </AppButton>
          <button
            v-else
            type="button"
            class="follow-btn"
            :class="following ? 'follow-btn--following' : 'follow-btn--follow'"
            :disabled="followProcessing"
            @click="handleFollow"
          >
            <span v-if="followProcessing" class="follow-btn__spinner" aria-hidden="true" />
            {{ following ? '팔로잉' : '팔로우' }}
          </button>
        </div>
      </div>

      <!-- 탭 (언더라인 탭) -->
      <nav class="flex gap-7 mb-6 border-b border-line" aria-label="프로필 탭">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          class="profile-tab"
          :class="activeTab === tab.key ? 'profile-tab--active' : ''"
          :aria-selected="activeTab === tab.key"
          @click="selectTab(tab.key)"
        >
          {{ tab.label }}
        </button>
      </nav>

      <!-- 찜 -->
      <section v-show="activeTab === 'wishlist'">
        <SkeletonList v-if="wishlistState.loading" />
        <EmptyState
          v-else-if="wishlistState.items.length === 0"
          icon="♡"
          title="찜한 상품이 없어요"
        />
        <div v-else class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-x-5 gap-y-7">
          <WishlistCard
            v-for="item in wishlistState.items"
            :key="item.id"
            :item="item"
            :likable="!isSelf"
            @like="handleWishlistLike"
          />
        </div>
      </section>

      <!-- 리뷰 -->
      <section v-show="activeTab === 'reviews'">
        <SkeletonList v-if="reviewsState.loading" />
        <EmptyState
          v-else-if="reviewsState.items.length === 0"
          icon="✍️"
          title="작성한 리뷰가 없어요"
        />
        <div v-else class="rounded-lg border border-line bg-surface px-5">
          <ReviewItem v-for="review in reviewsState.items" :key="review.id" :review="review" show-product />
        </div>
      </section>

      <!-- 글 -->
      <section v-show="activeTab === 'posts'">
        <SkeletonList v-if="postsState.loading" />
        <EmptyState
          v-else-if="postsState.items.length === 0"
          icon="📝"
          title="작성한 글이 없어요"
        />
        <ul v-else class="divide-y divide-line border-y border-line">
          <li v-for="post in postsState.items" :key="post.id">
            <router-link
              :to="`/community/${post.id}`"
              class="block group py-4 transition-colors"
            >
              <p class="text-[15px] font-semibold text-ink truncate group-hover:underline underline-offset-4 decoration-ink/40">{{ post.title }}</p>
              <p class="text-sm text-ink-muted truncate mt-1">{{ post.content }}</p>
              <div class="flex items-center gap-3 mt-2 text-xs text-ink-faint tabular-nums">
                <span class="flex items-center gap-1">♥ {{ post.likeCount }}</span>
                <span class="flex items-center gap-1">💬 {{ post.commentCount }}</span>
              </div>
            </router-link>
          </li>
        </ul>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { COLOR_OPTIONS, formatPersonalColor } from '@/types'
import type { PublicProfile, Review, WishlistItem, Post } from '@/types'
import { getPublicProfile } from '@/api/users'
import { toggleFollow } from '@/api/follows'
import { getUserWishlist, toggleWishlistLike } from '@/api/wishlist'
import { getUserReviews } from '@/api/products'
import { getPostsByAuthor } from '@/api/posts'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue'
import ReviewItem from '@/components/social/ReviewItem.vue'
import WishlistCard from '@/components/social/WishlistCard.vue'
import SkeletonList from '@/components/social/SkeletonList.vue'

type TabKey = 'wishlist' | 'reviews' | 'posts'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const profile = ref<PublicProfile | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const followerCount = ref(0)
const following = ref(false)
const followProcessing = ref(false)

const tabs: { key: TabKey; label: string }[] = [
  { key: 'wishlist', label: '찜' },
  { key: 'reviews', label: '리뷰' },
  { key: 'posts', label: '글' },
]
const activeTab = ref<TabKey>('wishlist')

const wishlistState = ref<{ loading: boolean; loaded: boolean; items: WishlistItem[] }>({ loading: false, loaded: false, items: [] })
const reviewsState = ref<{ loading: boolean; loaded: boolean; items: Review[] }>({ loading: false, loaded: false, items: [] })
const postsState = ref<{ loading: boolean; loaded: boolean; items: Post[] }>({ loading: false, loaded: false, items: [] })

const profileId = computed(() => Number(route.params.id))
const isSelf = computed(() => authStore.user?.id === profile.value?.id)
const initial = computed(() => profile.value?.nickname?.charAt(0).toUpperCase() ?? '?')

function getColorLabel(value: string): string {
  return COLOR_OPTIONS.find((c) => c.value === value)?.label ?? value
}

function goBack() {
  router.back()
}

async function handleFollow() {
  if (!profile.value) return
  followProcessing.value = true
  // 낙관적 업데이트
  const prev = { following: following.value, count: followerCount.value }
  following.value = !following.value
  followerCount.value += following.value ? 1 : -1
  try {
    const result = await toggleFollow(profile.value.id)
    following.value = result.following
    followerCount.value = result.followerCount
  } catch {
    following.value = prev.following
    followerCount.value = prev.count
  } finally {
    followProcessing.value = false
  }
}

async function handleWishlistLike(item: WishlistItem) {
  const target = wishlistState.value.items.find((w) => w.id === item.id)
  if (!target) return
  // 낙관적 업데이트
  const prevLiked = target.likedByMe
  const prevCount = target.likeCount
  target.likedByMe = !target.likedByMe
  target.likeCount += target.likedByMe ? 1 : -1
  try {
    const result = await toggleWishlistLike(item.id)
    target.likedByMe = result.liked
    target.likeCount = result.likeCount
  } catch {
    target.likedByMe = prevLiked
    target.likeCount = prevCount
  }
}

async function selectTab(tab: TabKey) {
  activeTab.value = tab
  if (tab === 'wishlist' && !wishlistState.value.loaded) await loadWishlist()
  if (tab === 'reviews' && !reviewsState.value.loaded) await loadReviews()
  if (tab === 'posts' && !postsState.value.loaded) await loadPosts()
}

async function loadWishlist() {
  wishlistState.value.loading = true
  try {
    wishlistState.value.items = await getUserWishlist(profileId.value)
    wishlistState.value.loaded = true
  } catch {
    wishlistState.value.items = []
  } finally {
    wishlistState.value.loading = false
  }
}

async function loadReviews() {
  reviewsState.value.loading = true
  try {
    reviewsState.value.items = await getUserReviews(profileId.value)
    reviewsState.value.loaded = true
  } catch {
    reviewsState.value.items = []
  } finally {
    reviewsState.value.loading = false
  }
}

async function loadPosts() {
  postsState.value.loading = true
  try {
    postsState.value.items = await getPostsByAuthor(profileId.value)
    postsState.value.loaded = true
  } catch {
    postsState.value.items = []
  } finally {
    postsState.value.loading = false
  }
}

async function loadProfile() {
  if (Number.isNaN(profileId.value)) {
    errorMessage.value = '잘못된 사용자입니다.'
    return
  }
  loading.value = true
  errorMessage.value = ''
  // 탭 상태 초기화 (다른 프로필로 이동 시)
  activeTab.value = 'wishlist'
  wishlistState.value = { loading: false, loaded: false, items: [] }
  reviewsState.value = { loading: false, loaded: false, items: [] }
  postsState.value = { loading: false, loaded: false, items: [] }
  try {
    profile.value = await getPublicProfile(profileId.value)
    followerCount.value = profile.value.followerCount
    following.value = profile.value.followedByMe
    await loadWishlist()
  } catch {
    errorMessage.value = '프로필을 불러오는데 실패했습니다.'
  } finally {
    loading.value = false
  }
}

watch(() => route.params.id, loadProfile)
onMounted(loadProfile)
</script>

<style scoped>
/* 언더라인 탭 (components.md D1) */
.profile-tab {
  position: relative;
  margin-bottom: -1px;
  padding: 0 0 0.8rem;
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-ink-muted);
  background: transparent;
  border: 0;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: color 0.18s ease, border-color 0.18s ease;
}
.profile-tab:hover {
  color: var(--color-ink-soft);
}
.profile-tab:focus-visible {
  outline: 2px solid var(--color-ink);
  outline-offset: 4px;
  border-radius: 2px;
}
.profile-tab--active,
.profile-tab--active:hover {
  color: var(--color-ink);
  border-bottom-color: var(--color-ink);
}

/* 팔로우 버튼 — 알약. 팔로우 = 먹색 채움 / 팔로잉 = 크림 */
.follow-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  width: 100%;
  height: 2.75rem;
  padding: 0 1.5rem;
  font-size: 0.875rem;
  font-weight: 600;
  border-radius: 9999px;
  cursor: pointer;
  transition: color 0.18s ease, background 0.18s ease, border-color 0.18s ease, opacity 0.18s ease;
}
.follow-btn:disabled {
  opacity: 0.6;
  pointer-events: none;
}
.follow-btn:focus-visible {
  outline: 2px solid var(--color-ink);
  outline-offset: 2px;
}
.follow-btn:active {
  transform: translateY(1px);
}
.follow-btn--follow {
  color: #fff;
  background: var(--color-ink);
  border: 1px solid var(--color-ink);
}
.follow-btn--follow:hover {
  background: color-mix(in srgb, var(--color-ink) 90%, #fff);
}
.follow-btn--following {
  color: var(--color-ink);
  background: var(--color-cream);
  border: 1px solid var(--color-line);
}
.follow-btn--following:hover {
  border-color: var(--color-ink);
}
.follow-btn__spinner {
  width: 1rem;
  height: 1rem;
  border-radius: 9999px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  animation: hk-spin 0.6s linear infinite;
}
</style>
