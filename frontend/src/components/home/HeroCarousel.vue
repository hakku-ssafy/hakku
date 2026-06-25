<template>
  <section class="hero" @mouseenter="pause()" @mouseleave="resume()">
    <div class="hero__viewport">
      <!-- 중앙 밴드 캐러셀: 밝은 중앙 --win 장 + 양옆은 순환 클론(dim)으로 빈 곳 없이 채움. -->
      <ul
        class="hero__track"
        :style="{ '--win': String(effWin), '--offset': String(bandStart) }"
      >
        <li
          v-for="(item, di) in displaySlides"
          :key="item.key"
          class="hero__slide"
          :class="{
            'hero__slide--in-band': di >= bandStart && di < bandStart + effWin,
            'hero__slide--clone': item.isClone,
          }"
          :data-idx="item.logicalIdx"
          :data-clone="item.isClone ? 'true' : 'false'"
          :aria-hidden="item.isClone ? 'true' : undefined"
          :inert="item.isClone || undefined"
        >
          <!-- 리드 슬라이드: 진단 완료면 추천 카드, 아니면 진단 상태 카드 -->
          <template v-if="item.type === 'lead'">
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
              v-if="isExternal(item.to)"
              :href="item.to"
              target="_blank"
              rel="noopener noreferrer"
              class="hero__slide-link"
            >
              <HeroEditorialCard
                :tone="item.tone"
                :kicker="item.kicker"
                :title="item.title"
                :sub="item.sub"
                :image-url="item.imageUrl"
              />
            </a>
            <router-link v-else :to="item.to" class="hero__slide-link">
              <HeroEditorialCard
                :tone="item.tone"
                :kicker="item.kicker"
                :title="item.title"
                :sub="item.sub"
                :image-url="item.imageUrl"
              />
            </router-link>
          </template>
        </li>
      </ul>
    </div>

    <!-- 컨트롤: 도트(이동 위치 단위) + 카운터 + 화살표. 이동 위치가 2개 이상일 때만. -->
    <div v-if="positionCount > 1" class="hero__controls">
      <div class="hero__dots">
        <button
          v-for="i in positionCount"
          :key="i"
          type="button"
          class="hero__dot"
          :class="{ 'hero__dot--active': realIndex === i - 1 }"
          :aria-label="`${i}번째 위치로 이동`"
          @click="goTo(i - 1)"
        />
        <span class="hero__counter">{{ counter }}</span>
      </div>
      <div class="hero__arrows">
        <button type="button" class="hero__arrow" aria-label="이전 슬라이드" @click="prev()">‹</button>
        <button type="button" class="hero__arrow hero__arrow--next" aria-label="다음 슬라이드" @click="next()">›</button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
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

/** 표시용 슬라이드(원본 + 논리 인덱스 + 클론 여부). */
type DisplaySlide = BaseSlide & { logicalIdx: number; isClone: boolean; key: string }

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

// 반응형 중앙 밴드 크기: 모바일 1 / 태블릿 2 / 데스크탑 3.
const BP_TABLET = 640
const BP_DESKTOP = 1024
function visibleCount(w: number): number {
  if (w >= BP_DESKTOP) return 3
  if (w >= BP_TABLET) return 2
  return 1
}
const win = ref(typeof window !== 'undefined' ? visibleCount(window.innerWidth) : 3)
function handleResize(): void {
  win.value = visibleCount(window.innerWidth)
}
onMounted(() => window.addEventListener('resize', handleResize, { passive: true }))
onUnmounted(() => window.removeEventListener('resize', handleResize))

// 실제로 적용되는 밴드 크기(카드 수보다 클 수 없음).
const effWin = computed(() => Math.min(win.value, Math.max(1, TOTAL.value)))

const canMatchMedia = typeof window !== 'undefined' && typeof window.matchMedia === 'function'
const prefersReduced = canMatchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches

const { realIndex, positionCount, next, prev, goTo, pause, resume } = useHeroCarousel(TOTAL, {
  reducedMotion: prefersReduced,
  windowSize: effWin,
})

/**
 * 표시 트랙 = [말미 W장(클론, 왼쪽 채움)] + [실제 N장] + [선두 W장(클론, 오른쪽 채움)].
 * 양끝 클론으로 어느 위치에서도 밴드 양옆이 dim 카드로 차서 빈 곳이 생기지 않는다(순환).
 */
const displaySlides = computed<DisplaySlide[]>(() => {
  const base = baseSlides.value
  const n = base.length
  const w = effWin.value
  const list: DisplaySlide[] = []
  for (let k = n - w; k < n; k++) {
    list.push({ ...base[k], logicalIdx: k, isClone: true, key: `pre-${k}` })
  }
  for (let k = 0; k < n; k++) {
    list.push({ ...base[k], logicalIdx: k, isClone: false, key: `real-${k}` })
  }
  for (let k = 0; k < w; k++) {
    list.push({ ...base[k], logicalIdx: k, isClone: true, key: `suf-${k}` })
  }
  return list
})

// 밝은 밴드의 시작(표시 트랙 기준) 인덱스 = 선두 클론 수(W) + 현재 위치.
const bandStart = computed(() => effWin.value + realIndex.value)

const counter = computed(
  () =>
    `${String(realIndex.value + 1).padStart(2, '0')} / ${String(positionCount.value).padStart(2, '0')}`,
)
</script>

<style scoped>
.hero {
  /* 프레임 = 컨테이너(u-container) 콘텐츠 좌측 경계. 컨트롤을 여기에 정렬. */
  --hero-frame: max(var(--hk-pad-x), calc((100vw - var(--hk-w-wide)) / 2 + var(--hk-pad-x)));
  --hero-gap: 14px;
  /* 모바일: 밴드 1장 + 양옆 살짝 peek. */
  --hero-card: 84vw;
  position: relative;
  margin-top: 4px;
}
@media (min-width: 640px) {
  /* 태블릿: 밴드 2장. */
  .hero {
    --hero-card: clamp(240px, 40vw, 360px);
  }
}
@media (min-width: 1024px) {
  /* 데스크탑: 밴드 3장. */
  .hero {
    --hero-card: clamp(280px, 24vw, 380px);
  }
}

.hero__viewport {
  overflow: hidden;
}

.hero__track {
  display: flex;
  gap: var(--hero-gap);
  margin: 0;
  padding: 0;
  list-style: none;
  --band-width: calc(var(--win, 1) * var(--hero-card) + (var(--win, 1) - 1) * var(--hero-gap));
  /* 밝은 밴드(--win 장)를 뷰포트 가로 중앙에 두도록 --offset 번째 카드를 정렬. */
  transform: translateX(
    calc((100vw - var(--band-width)) / 2 - var(--offset, 0) * (var(--hero-card) + var(--hero-gap)))
  );
  transition: transform var(--dur-slide, 0.6s) var(--hk-ease-slide);
  will-change: transform;
}

.hero__slide {
  flex: 0 0 var(--hero-card);
  width: var(--hero-card);
  height: var(--hero-card); /* 정사각형 */
  box-sizing: border-box;
  /* 중앙 밴드 밖 카드(클론 포함)는 어둡게(dim). */
  opacity: 0.4;
  filter: brightness(0.62);
  transition: opacity var(--dur-slide, 0.6s) var(--hk-ease-slide),
    filter var(--dur-slide, 0.6s) var(--hk-ease-slide);
}
.hero__slide--in-band {
  opacity: 1;
  filter: none;
}
.hero__slide--clone {
  pointer-events: none;
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
