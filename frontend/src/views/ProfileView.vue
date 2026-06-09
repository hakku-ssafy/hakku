<template>
  <div class="u-container max-w-2xl py-10 sm:py-12">
    <button
      type="button"
      class="inline-flex items-center gap-1.5 text-sm text-ink-soft hover:text-ink mb-7 transition-colors"
      @click="goBack"
    >
      <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
      </svg>
      뒤로
    </button>

    <div v-if="loading" class="rounded-xl border border-line p-6">
      <SkeletonBlock height="4rem" width="4rem" class="!rounded-full mb-4" />
      <SkeletonBlock height="1.25rem" width="8rem" />
    </div>

    <div v-else-if="errorMessage" role="alert" class="px-4 py-4 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm">
      {{ errorMessage }}
    </div>

    <template v-else-if="profile">
      <!-- 프로필 헤더 -->
      <div class="rounded-2xl border border-line bg-surface p-6 mb-6">
        <div class="flex items-center gap-4">
          <div class="w-16 h-16 rounded-full u-gradient-accent text-accent-ink grid place-items-center shrink-0">
            <span class="text-2xl font-bold">{{ initial }}</span>
          </div>
          <div class="min-w-0 flex-1">
            <h1 class="u-serif text-xl text-ink truncate">{{ profile.nickname }}</h1>
            <p v-if="profile.personalColor" class="text-sm u-gradient-text font-medium mt-0.5">
              {{ formatPersonalColor(profile.personalColor) }}
            </p>
          </div>
        </div>

        <div class="flex items-center gap-6 mt-5 text-sm">
          <div><span class="font-bold text-ink tabular-nums">{{ followerCount }}</span> <span class="text-ink-muted">팔로워</span></div>
          <div><span class="font-bold text-ink tabular-nums">{{ profile.followingCount }}</span> <span class="text-ink-muted">팔로잉</span></div>
        </div>

        <div v-if="profile.preferredColors.length > 0" class="flex flex-wrap gap-1.5 mt-4">
          <AppBadge v-for="color in profile.preferredColors" :key="color">{{ getColorLabel(color) }}</AppBadge>
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
          <AppButton
            v-else
            :variant="following ? 'secondary' : 'primary'"
            block
            :loading="followProcessing"
            @click="handleFollow"
          >
            {{ following ? '팔로잉' : '팔로우' }}
          </AppButton>
        </div>
      </div>

      <!-- 탭 -->
      <nav class="flex gap-1 mb-5" aria-label="프로필 탭">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          class="px-3.5 py-2 rounded-full text-sm font-medium transition-colors"
          :class="activeTab === tab.key
            ? 'u-gradient-accent text-accent-ink shadow-sm'
            : 'text-ink-soft hover:text-ink hover:bg-surface-soft'"
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
        <div v-else class="grid grid-cols-2 gap-3">
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
        <div v-else class="rounded-xl border border-line bg-surface px-5">
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
        <ul v-else class="space-y-2">
          <li v-for="post in postsState.items" :key="post.id">
            <router-link
              :to="`/community/${post.id}`"
              class="block rounded-xl border border-line bg-surface p-4 u-pop hover:border-accent-line"
            >
              <p class="text-sm font-semibold text-ink line-clamp-1">{{ post.title }}</p>
              <p class="text-sm text-ink-soft line-clamp-1 mt-0.5">{{ post.content }}</p>
              <div class="flex items-center gap-3 mt-2 text-xs text-ink-muted tabular-nums">
                <span>♥ {{ post.likeCount }}</span>
                <span>💬 {{ post.commentCount }}</span>
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
