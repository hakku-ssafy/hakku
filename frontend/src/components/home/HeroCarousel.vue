<template>
  <section class="hero" @mouseenter="pause()" @mouseleave="resume()">
    <div class="hero__viewport">
      <!-- 유한 캐러셀: 복제 없이 한 벌만. CSS 가 --pos 로 활성 카드를 중앙 정렬. -->
      <ul
        class="hero__track"
        :style="{ '--pos': String(pos) }"
      >
        <li
          v-for="(slide, i) in baseSlides"
          :key="`${slide.type}-${i}`"
          class="hero__slide"
        >
          <!-- 리드 슬라이드: 진단 완료면 추천 카드, 아니면 진단 상태 카드 -->
          <template v-if="slide.type === 'lead'">
            <HeroRecommendCard
              v-if="diagnosisState === 'done'"
              :personal-color-label="personalColorLabel"
              :products="recommendedProducts"
            />
            <HeroDiagnosisCard
              v-else
              :state="diagnosisState"
              :personal-color-label="personalColorLabel"
              :has-diagnosis-image="hasDiagnosisImage"
              @view-image="$emit('view-image')"
            />
          </template>
          <!-- 에디토리얼 슬라이드 -->
          <template v-else>
            <a
              v-if="isExternal(slide.to)"
              :href="slide.to"
              target="_blank"
              rel="noopener noreferrer"
              class="hero__slide-link"
            >
              <HeroEditorialCard
                :tone="slide.tone"
                :kicker="slide.kicker"
                :title="slide.title"
                :sub="slide.sub"
                :image-url="slide.imageUrl"
              />
            </a>
            <router-link v-else :to="slide.to" class="hero__slide-link">
              <HeroEditorialCard
                :tone="slide.tone"
                :kicker="slide.kicker"
                :title="slide.title"
                :sub="slide.sub"
                :image-url="slide.imageUrl"
              />
            </router-link>
          </template>
        </li>
      </ul>
    </div>

    <!-- 컨트롤: 도트 + 카운터 + 화살표 -->
    <div class="hero__controls">
      <div class="hero__dots">
        <button
          v-for="i in TOTAL"
          :key="i"
          type="button"
          class="hero__dot"
          :class="{ 'hero__dot--active': realIndex === i - 1 }"
          :aria-label="`${i}번째 슬라이드로 이동`"
          @click="goTo(i - 1)"
        />
        <span class="hero__counter">{{ counter }}</span>
      </div>
      <div class="hero__arrows">
        <button type="button" class="hero__arrow" aria-label="이전 슬라이드" :disabled="isFirst" @click="prev()">‹</button>
        <button type="button" class="hero__arrow hero__arrow--next" aria-label="다음 슬라이드" :disabled="isLast" @click="next()">›</button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { Magazine, Product } from '@/types'
import { listActiveMagazines } from '@/api/magazine'
import { useHeroCarousel } from '@/composables/useHeroCarousel'
import HeroDiagnosisCard from './HeroDiagnosisCard.vue'
import HeroEditorialCard from './HeroEditorialCard.vue'
import HeroRecommendCard from './HeroRecommendCard.vue'

interface Props {
  diagnosisState: 'guest' | 'none' | 'pending' | 'done'
  personalColorLabel?: string
  hasDiagnosisImage?: boolean
  /** 진단 완료 시 리드 카드에 노출할 추천 상품(상위 N개). */
  recommendedProducts?: Product[]
}
withDefaults(defineProps<Props>(), {
  personalColorLabel: '',
  hasDiagnosisImage: false,
  recommendedProducts: () => [],
})
defineEmits<{ 'view-image': [] }>()

interface EditorialSlide {
  tone: string
  kicker: string
  title: string
  sub: string
  to: string
  imageUrl?: string
}
type BaseSlide = { type: 'lead' } | ({ type: 'editorial' } & EditorialSlide)

// 어드민 큐레이션 카드가 없거나 조회 실패 시 폴백할 기본 슬라이드.
const EDITORIAL_SLIDES: EditorialSlide[] = [
  { tone: 'u-tone-1', kicker: 'NEW ARRIVAL', title: '이주의 신상\n키링 모음', sub: '지금 가장 인기 있는 픽', to: '/products' },
  { tone: 'u-tone-2', kicker: 'LOUNGE', title: '학생증 자랑\n라운지', sub: '다꾸 인증 보러가기', to: '/community' },
  { tone: 'u-tone-4', kicker: 'GUIDE', title: '퍼스널컬러로\n고르는 법', sub: 'AI 추천 가이드', to: '/diagnosis' },
]

const TONES = ['u-tone-1', 'u-tone-2', 'u-tone-4', 'u-tone-5', 'u-tone-6', 'u-tone-3']
const fetchedSlides = ref<EditorialSlide[]>([])

/** 발행 매거진 → 에디토리얼 슬라이드. 슬라이드를 누르면 매거진 상세(/magazine/:id)로 이동. */
function toSlide(magazine: Magazine, i: number): EditorialSlide {
  return {
    tone: TONES[i % TONES.length],
    kicker: magazine.kicker ?? '',
    title: magazine.title,
    sub: magazine.subtitle ?? '',
    to: `/magazine/${magazine.id}`,
    imageUrl: magazine.coverImageUrl ?? undefined,
  }
}

onMounted(async () => {
  try {
    const magazines = await listActiveMagazines()
    if (Array.isArray(magazines) && magazines.length > 0) {
      fetchedSlides.value = magazines.map(toSlide)
    }
  } catch {
    /* 폴백: 기본 슬라이드 유지 */
  }
})

const editorialSlides = computed(() =>
  fetchedSlides.value.length > 0 ? fetchedSlides.value : EDITORIAL_SLIDES,
)

/** 외부 링크(http/https)는 새 탭, 내부 경로는 router-link 로 처리한다. */
function isExternal(to: string): boolean {
  return /^https?:\/\//i.test(to)
}

// 한 벌의 슬라이드(리드 1 + 에디토리얼 N).
const baseSlides = computed<BaseSlide[]>(() => [
  { type: 'lead' },
  ...editorialSlides.value.map((s): BaseSlide => ({ type: 'editorial', ...s })),
])
const TOTAL = computed(() => baseSlides.value.length)

const canMatchMedia = typeof window !== 'undefined' && typeof window.matchMedia === 'function'
const prefersReduced = canMatchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches

const { pos, realIndex, isFirst, isLast, next, prev, goTo, pause, resume } = useHeroCarousel(
  TOTAL,
  { reducedMotion: prefersReduced },
)

const counter = computed(
  () => `${String(realIndex.value + 1).padStart(2, '0')} / ${String(TOTAL.value).padStart(2, '0')}`,
)
</script>

<style scoped>
.hero {
  /* 프레임 = 컨테이너(u-container) 콘텐츠 좌측 경계. 활성 카드를 여기에 정렬. */
  --hero-frame: max(var(--hk-pad-x), calc((100vw - var(--hk-w-wide)) / 2 + var(--hk-pad-x)));
  --hero-gap: 14px;
  /* 모바일: 메인 1장이 거의 화면 폭 + 다음 카드 살짝 peek. */
  --hero-card: calc(100vw - var(--hero-frame) - 46px);
  position: relative;
  margin-top: 4px;
}
@media (min-width: 640px) {
  .hero {
    --hero-card: clamp(340px, 46vw, 420px);
  }
}
@media (min-width: 1024px) {
  .hero {
    --hero-card: clamp(400px, 34vw, 480px);
  }
}

.hero__viewport {
  overflow: hidden;
}

.hero__track {
  display: flex;
  gap: var(--hero-gap);
  margin: 0;
  /* 활성 카드를 뷰포트 가로 중앙에 정렬(양옆 카드는 peek). */
  padding: 0 calc((100vw - var(--hero-card)) / 2);
  list-style: none;
  /* pos 칸만큼 좌측 이동(카드폭 + gap 단위). */
  transform: translateX(calc(-1 * var(--pos, 0) * (var(--hero-card) + var(--hero-gap))));
  transition: transform var(--dur-slide, 0.6s) var(--hk-ease-slide);
  will-change: transform;
}

.hero__slide {
  flex: 0 0 var(--hero-card);
  width: var(--hero-card);
  height: var(--hero-card); /* 정사각형 */
  box-sizing: border-box;
}
.hero__slide-link {
  display: block;
  height: 100%;
}

/* 컨트롤 — 프레임(컨테이너 경계)에 정렬 */
.hero__controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20px;
  padding-left: var(--hero-frame);
  padding-right: var(--hero-frame);
}
.hero__dots {
  display: flex;
  align-items: center;
  gap: 8px;
}
.hero__dot {
  height: 4px;
  width: 8px;
  border-radius: var(--hk-radius-pill);
  background: var(--hk-border-control);
  transition: width 0.4s ease, background 0.4s ease;
  cursor: pointer;
}
.hero__dot--active {
  width: 30px;
  background: var(--accent, #16140f);
}
.hero__counter {
  margin-left: 14px;
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.14em;
  color: var(--hk-text-quiet);
}
.hero__arrows {
  display: flex;
  align-items: center;
  gap: 8px;
}
.hero__arrow {
  width: 38px;
  height: 38px;
  border-radius: var(--hk-radius-pill);
  border: 1px solid var(--hk-border-control);
  background: var(--hk-surface);
  color: var(--hk-ink);
  font-size: 17px;
  line-height: 1;
  cursor: pointer;
  transition: transform 0.18s var(--ease-out-expo), background 0.18s ease;
}
.hero__arrow:hover {
  transform: translateY(-1px);
}
.hero__arrow--next {
  border-color: var(--hk-ink);
  background: var(--hk-ink);
  color: var(--hk-on-dark);
}
.hero__arrow:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.hero__arrow:disabled:hover {
  transform: none;
}
</style>
