<template>
  <div>
    <main class="home">
      <!-- ===== 히어로 캐러셀 (풀블리드) ===== -->
      <div class="home__hero">
        <HeroCarousel
          :diagnosis-state="heroState"
          :personal-color-label="formatPersonalColor(personalColor)"
          :has-diagnosis-image="!!diagnosisImageSrc"
          :recommended-products="heroRecommendedProducts"
          @view-image="showDiagnosisModal = true"
        />
      </div>

      <div class="u-container home__body">
        <!-- ===== 키워드 퀵링크 칩 ===== -->
        <section class="chips" aria-label="키워드 바로가기">
          <router-link v-for="chip in KEYWORD_CHIPS" :key="chip.label" :to="chip.to" class="chip">
            <span class="chip__glyph" aria-hidden="true">✦</span>{{ chip.label }}
          </router-link>
        </section>

        <!-- ===== 상품 / For You ===== -->
        <section class="home-section">
          <SectionHeader :eyebrow="productKicker" :title="productTitle">
            <template #action>
              <router-link to="/products" class="see-all">전체보기 →</router-link>
            </template>
          </SectionHeader>

          <div v-if="productStore.loading" class="product-grid">
            <SkeletonBlock
              v-for="i in 8"
              :key="i"
              height="auto"
              width="100%"
              class="aspect-square rounded-[5px]"
            />
          </div>

          <div v-else-if="displayProducts.length > 0" class="product-grid">
            <ProductCard v-for="product in displayProducts" :key="product.id" :product="product">
              <template v-if="isRecommended" #meta>
                <AppBadge variant="accent" class="mt-2">추천 ✦</AppBadge>
              </template>
            </ProductCard>
          </div>

          <EmptyState v-else icon="◍" title="등록된 상품이 없어요" />
        </section>

        <!-- ===== 학생증 자랑 (이미지 격자) ===== -->
        <section v-if="showcaseLoading || showcasePosts.length > 0" class="home-section">
          <SectionHeader eyebrow="Show off" title="학생증 자랑">
            <template #action>
              <router-link to="/community?board=showcase" class="see-all">더보기 →</router-link>
            </template>
          </SectionHeader>

          <div v-if="showcaseLoading" class="showcase-grid">
            <SkeletonBlock
              v-for="i in 6"
              :key="i"
              height="auto"
              width="100%"
              class="aspect-[3/4] rounded-md"
            />
          </div>

          <div v-else class="showcase-grid">
            <router-link
              v-for="post in showcasePosts"
              :key="post.id"
              :to="`/community/${post.id}`"
              class="showcase-card group"
            >
              <div class="showcase-card__thumb">
                <img
                  :src="post.imageUrl ?? ''"
                  :alt="post.title"
                  loading="lazy"
                  class="showcase-card__img"
                />
              </div>
              <p class="showcase-card__title">{{ post.title }}</p>
            </router-link>
          </div>
        </section>

        <!-- ===== 커뮤니티 프리뷰 ===== -->
        <section class="home-section">
          <SectionHeader eyebrow="Community" title="지금 학꾸 라운지">
            <template #action>
              <router-link to="/community" class="see-all">더보기 →</router-link>
            </template>
          </SectionHeader>

          <div v-if="postsLoading" class="post-list">
            <div v-for="i in 4" :key="i" class="post-row">
              <div class="post-row__main">
                <SkeletonBlock height="1.05rem" width="45%" class="mb-2" />
                <SkeletonBlock height="0.8rem" width="70%" />
              </div>
            </div>
          </div>

          <div v-else-if="recentPosts.length > 0" class="post-list">
            <router-link
              v-for="post in recentPosts"
              :key="post.id"
              :to="`/community/${post.id}`"
              class="post-row"
            >
              <div class="post-row__main">
                <div class="post-row__title">{{ post.title }}</div>
                <div class="post-row__excerpt">{{ post.content }}</div>
              </div>
              <div class="post-row__meta">
                <span class="post-row__author">{{ post.authorNickname }}</span>
                <span class="post-row__time">{{ formatDate(post.createdAt) }}</span>
                <span class="post-row__stat">♥ {{ post.likeCount }}</span>
                <span class="post-row__stat">💬 {{ post.commentCount }}</span>
              </div>
            </router-link>
          </div>

          <EmptyState v-else icon="✎" title="아직 게시글이 없어요" />
        </section>
      </div>
    </main>

    <!-- 진단 이미지 모달 -->
    <AppModal v-model:open="showDiagnosisModal" title="퍼스널컬러 진단 결과" max-width="md">
      <img v-if="diagnosisImageSrc" :src="diagnosisImageSrc" alt="진단 이미지" class="w-full rounded-md" />
      <p v-if="personalColor" class="text-center text-sm font-bold mt-4 text-accent">
        {{ formatPersonalColor(personalColor) }}
      </p>
    </AppModal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getEntry, setEntry, isFresh, dedupe, DEFAULT_STALE_TIME } from '@/lib/resourceCache'
import { usePostStore } from '@/stores/posts'
import { useProductStore } from '@/stores/products'
import apiClient from '@/api/client'
import { getPosts } from '@/api/posts'
import { formatPersonalColor, type DiagnosisStatus, type Post, type RecommendationItem } from '@/types'
import { useAuthedImage } from '@/composables/useAuthedImage'
import HeroCarousel from '@/components/home/HeroCarousel.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppModal from '@/components/ui/AppModal.vue'
import ProductCard from '@/components/ui/ProductCard.vue'
import SectionHeader from '@/components/ui/SectionHeader.vue'
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

const authStore = useAuthStore()
const postStore = usePostStore()
const productStore = useProductStore()

const postsLoading = ref(false)
const showDiagnosisModal = ref(false)
const recommendations = ref<RecommendationItem[]>([])

// 학생증 자랑(이미지 격자) — 커뮤니티 프리뷰가 쓰는 store.posts 를 건드리지 않도록 별도로 보관한다.
const showcasePosts = ref<Post[]>([])
const showcaseLoading = ref(false)

// 진단 상태/퍼스널컬러는 인증 스토어를 단일 소스로 삼는다 → App.vue 폴링이
// PENDING → COMPLETED 로 갱신하면 새로고침 없이 이 화면에 즉시 반영된다.
const diagnosisStatus = computed<DiagnosisStatus>(() => authStore.user?.diagnosisStatus ?? 'NONE')
const personalColor = computed(() => authStore.user?.personalColor ?? null)
const diagnosisImageUrl = computed(
  () => authStore.user?.diagnosisImageUrl ?? authStore.user?.profileImageUrl ?? null,
)

// 진단 사진은 인증이 필요한 storage 리소스 → 토큰 붙여 fetch 후 object URL 로 표시
const { objectUrl: diagnosisImageSrc } = useAuthedImage(() => diagnosisImageUrl.value)

// 히어로 진단 카드 상태(비로그인/미진단/분석중/완료).
const heroState = computed<'guest' | 'none' | 'pending' | 'done'>(() => {
  if (!authStore.isAuthenticated) return 'guest'
  if (diagnosisStatus.value === 'PENDING') return 'pending'
  if (diagnosisStatus.value === 'COMPLETED') return 'done'
  return 'none'
})

const KEYWORD_CHIPS = [
  { label: '핀뱃지', to: '/products' },
  { label: '키링', to: '/products' },
  { label: '꾸미기 스티커', to: '/products' },
  { label: '그립톡', to: '/products' },
  { label: '다꾸', to: '/products' },
  { label: '키캡', to: '/products' },
  { label: '마스킹테이프', to: '/products' },
  { label: '퍼스널컬러', to: '/diagnosis' },
]

const recentPosts = computed(() => postStore.posts.slice(0, 5))

const isRecommended = computed(
  () => diagnosisStatus.value === 'COMPLETED' && recommendations.value.length > 0,
)
const productKicker = computed(() => (isRecommended.value ? 'For You' : 'Shop'))
const productTitle = computed(() => (isRecommended.value ? '맞춤 추천 상품' : '상품'))

const displayProducts = computed(() => {
  if (isRecommended.value) {
    return recommendations.value.slice(0, 8).map((r) => r.product)
  }
  return productStore.products.slice(0, 8)
})

// 히어로 리드 카드(진단 완료 시)용 추천 상품 — 상위 4개.
const heroRecommendedProducts = computed(() => recommendations.value.slice(0, 4).map((r) => r.product))

function formatDate(dateStr: string): string {
  const d = new Date(dateStr)
  const now = new Date()
  const diffDays = Math.floor((now.getTime() - d.getTime()) / (1000 * 60 * 60 * 24))
  if (diffDays === 0) return '오늘'
  if (diffDays === 1) return '어제'
  if (diffDays < 7) return `${diffDays}일 전`
  return d.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
}

async function loadRecommendations() {
  // 추천은 사용자별로 캐시(상품목록 페이지와 같은 키를 공유해 재요청을 막는다).
  const key = `recommendations:${authStore.user?.id ?? 'anon'}`
  const cached = getEntry<RecommendationItem[]>(key)
  if (cached) {
    recommendations.value = cached.data
    if (isFresh(key, DEFAULT_STALE_TIME)) return
  }
  try {
    const data = await dedupe(key, async () => {
      const res = await apiClient.get<RecommendationItem[]>('/recommendations')
      return res.data
    })
    recommendations.value = data
    setEntry(key, data)
  } catch {
    // recommendations optional
  }
}

/** 학생증 자랑 게시판에서 이미지가 있는 글만 상위 8개 격자로 노출. */
async function loadShowcase() {
  const key = 'home:showcase'
  const cached = getEntry<Post[]>(key)
  if (cached) {
    showcasePosts.value = cached.data
    if (isFresh(key, DEFAULT_STALE_TIME)) return
  } else {
    showcaseLoading.value = true
  }
  try {
    const posts = await dedupe(key, () => getPosts('STUDENT_ID'))
    const filtered = posts.filter((p) => p.imageUrl).slice(0, 8)
    showcasePosts.value = filtered
    setEntry(key, filtered)
  } catch {
    if (!cached) showcasePosts.value = []
  } finally {
    showcaseLoading.value = false
  }
}

onMounted(async () => {
  postsLoading.value = true
  await Promise.all([
    postStore.fetchPosts().finally(() => {
      postsLoading.value = false
    }),
    productStore.fetchProducts(),
    loadShowcase(),
  ])

  if (diagnosisStatus.value === 'COMPLETED') loadRecommendations()
})

// 진단이 완료로 전환되면(폴링 반영 포함) 맞춤 추천을 불러온다.
watch(diagnosisStatus, (status) => {
  if (status === 'COMPLETED' && recommendations.value.length === 0) loadRecommendations()
})
</script>

<style scoped>
.home {
  padding-bottom: 90px;
}
.home__hero {
  padding-top: 30px;
}
.home__body {
  padding-top: 26px;
}

/* 키워드 칩 */
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
}
.chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 17px;
  border-radius: var(--hk-radius-pill);
  background: var(--hk-surface);
  border: 1px solid var(--hk-border);
  font-size: 13px;
  font-weight: 500;
  color: var(--hk-ink);
  transition: border-color 0.18s ease, transform 0.18s var(--ease-out-expo);
}
.chip:hover {
  border-color: var(--hk-border-control);
  transform: translateY(-1px);
}
.chip__glyph {
  color: var(--accent, #16140f);
}

/* 섹션 리듬 */
.home-section {
  margin-top: 60px;
}
.see-all {
  font-size: 13px;
  font-weight: 600;
  color: var(--hk-text-muted);
  transition: color 0.18s ease;
}
.see-all:hover {
  color: var(--hk-ink);
}

/* 상품 그리드 — 4열(1024+) → 3열(768) → 2열(모바일).
   minmax(0, 1fr): 큰 이미지의 min-content 가 트랙을 제각각 늘려 썸네일이 뒤죽박죽
   커지는 것을 방지(min 을 0 으로 강제). */
.product-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
}
@media (min-width: 768px) {
  .product-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
  }
}
@media (min-width: 1024px) {
  .product-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

/* 학생증 자랑 — 이미지 격자(4→3→2열, 세로 3:4 카드) */
.showcase-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
@media (min-width: 768px) {
  .showcase-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (min-width: 1024px) {
  .showcase-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
.showcase-card__thumb {
  aspect-ratio: 3 / 4;
  border-radius: var(--hk-radius-md);
  overflow: hidden;
  background: var(--hk-cream);
}
.showcase-card__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s var(--ease-out-expo);
}
.showcase-card:hover .showcase-card__img {
  transform: scale(1.05);
}
.showcase-card__title {
  margin-top: 9px;
  font-size: 13px;
  color: var(--hk-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 커뮤니티 프리뷰 — border-top 리스트 행 */
.post-list {
  border-top: 1px solid var(--hk-border);
}
.post-row {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 18px 4px;
  border-bottom: 1px solid var(--hk-border);
  transition: background 0.18s ease;
}
.post-row:hover {
  background: var(--hk-surface-soft);
}
.post-row__main {
  flex: 1;
  min-width: 0;
}
.post-row__title {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.01em;
  margin-bottom: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.post-row__excerpt {
  font-size: 13px;
  color: var(--hk-text-muted-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.post-row__meta {
  flex: none;
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  color: var(--hk-text-quiet);
}

/* 모바일 — 작성자/시간 숨기고 좋아요·댓글만 */
@media (max-width: 639.98px) {
  .post-row__meta {
    gap: 12px;
  }
  .post-row__author,
  .post-row__time {
    display: none;
  }
}
</style>
