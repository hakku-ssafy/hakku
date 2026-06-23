<template>
  <section class="hero" @mouseenter="pause()" @mouseleave="resume()">
    <div class="hero__viewport">
      <div class="hero__track" :style="{ transform: trackTransform }">
        <!-- slide 1: 진단 상태 카드 -->
        <div class="hero__slide">
          <HeroDiagnosisCard
            :state="diagnosisState"
            :personal-color-label="personalColorLabel"
            :has-diagnosis-image="hasDiagnosisImage"
            @view-image="$emit('view-image')"
          />
        </div>
        <!-- 에디토리얼 슬라이드 -->
        <router-link
          v-for="(slide, i) in EDITORIAL_SLIDES"
          :key="i"
          :to="slide.to"
          class="hero__slide hero__slide--link"
        >
          <HeroEditorialCard :tone="slide.tone" :kicker="slide.kicker" :title="slide.title" :sub="slide.sub" />
        </router-link>
      </div>
    </div>

    <!-- 컨트롤: 도트 + 카운터 + 화살표 -->
    <div class="hero__controls">
      <div class="hero__dots">
        <button
          v-for="i in TOTAL"
          :key="i"
          type="button"
          class="hero__dot"
          :class="{ 'hero__dot--active': index === i - 1 }"
          :aria-label="`${i}번째 슬라이드로 이동`"
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
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useHeroCarousel } from '@/composables/useHeroCarousel'
import HeroDiagnosisCard from './HeroDiagnosisCard.vue'
import HeroEditorialCard from './HeroEditorialCard.vue'

interface Props {
  diagnosisState: 'guest' | 'none' | 'pending' | 'done'
  personalColorLabel?: string
  hasDiagnosisImage?: boolean
}
withDefaults(defineProps<Props>(), { personalColorLabel: '', hasDiagnosisImage: false })
defineEmits<{ 'view-image': [] }>()

interface EditorialSlide {
  tone: string
  kicker: string
  title: string
  sub: string
  to: string
}

const EDITORIAL_SLIDES: EditorialSlide[] = [
  { tone: 'u-tone-1', kicker: 'NEW ARRIVAL', title: '이주의 신상\n키링 모음', sub: '지금 가장 인기 있는 픽', to: '/products' },
  { tone: 'u-tone-2', kicker: 'LOUNGE', title: '학생증 자랑\n라운지', sub: '다꾸 인증 보러가기', to: '/community' },
  { tone: 'u-tone-4', kicker: 'GUIDE', title: '퍼스널컬러로\n고르는 법', sub: 'AI 추천 가이드', to: '/diagnosis' },
]

const TOTAL = 1 + EDITORIAL_SLIDES.length

const canMatchMedia = typeof window !== 'undefined' && typeof window.matchMedia === 'function'
const prefersReduced = canMatchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches

const { index, shift, next, prev, goTo, pause, resume } = useHeroCarousel(TOTAL, {
  reducedMotion: prefersReduced,
})

// 모바일은 1-up(슬라이드당 100% 이동), 데스크톱은 3-up(컨트롤러의 33.333% shift).
const MOBILE_QUERY = '(max-width: 767.98px)'
const isMobile = ref(canMatchMedia && window.matchMedia(MOBILE_QUERY).matches)
let mql: MediaQueryList | undefined
function onMobileChange(e: MediaQueryListEvent) {
  isMobile.value = e.matches
}
onMounted(() => {
  if (!canMatchMedia) return
  mql = window.matchMedia(MOBILE_QUERY)
  mql.addEventListener('change', onMobileChange)
})
onUnmounted(() => mql?.removeEventListener('change', onMobileChange))

const trackTransform = computed(() =>
  isMobile.value ? `translateX(-${index.value * 100}%)` : shift.value,
)

const counter = computed(
  () => `${String(index.value + 1).padStart(2, '0')} / ${String(TOTAL).padStart(2, '0')}`,
)
</script>

<style scoped>
.hero {
  position: relative;
  margin-top: 4px;
}
.hero__viewport {
  overflow: hidden;
}
.hero__track {
  display: flex;
  transition: transform var(--dur-slide, 0.6s) var(--hk-ease-slide);
  will-change: transform;
}

.hero__slide {
  flex: 0 0 100%;
  box-sizing: border-box;
  height: 420px;
  padding: 0 16px;
}
.hero__slide--link {
  display: block;
}

@media (min-width: 768px) {
  .hero__slide {
    flex: 0 0 33.333%;
    height: 440px;
    padding: 0 7px;
  }
}

/* 컨트롤 — 뷰포트 가장자리 기준 패딩(풀블리드 정합) */
.hero__controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20px;
  padding-inline: var(--hk-pad-x);
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
</style>
