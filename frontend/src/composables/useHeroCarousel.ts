import {
  ref,
  computed,
  toValue,
  onMounted,
  onUnmounted,
  getCurrentInstance,
  type Ref,
  type ComputedRef,
  type MaybeRefOrGetter,
} from 'vue'

interface HeroCarouselOptions {
  /** 자동재생 간격(ms). 기본 4500. */
  autoPlayMs?: number
  /** prefers-reduced-motion 이면 자동재생을 끈다. */
  reducedMotion?: boolean
}

interface HeroCarouselController {
  index: Ref<number>
  shift: ComputedRef<string>
  next: () => void
  prev: () => void
  goTo: (i: number) => void
  pause: () => void
  resume: () => void
}

/** 3-up 레이아웃 기준 한 슬라이드 폭(%). */
const PER_VIEW_PERCENT = 100 / 3
const DEFAULT_AUTOPLAY_MS = 4500

/**
 * 홈 히어로 캐러셀 상태 컨트롤러.
 * 인덱스 wrap·clamp, 3-up 기준 translateX shift, reduced-motion 대응 자동재생을 제공한다.
 * 모바일 1-up 트랜스폼은 소비 컴포넌트가 `index` 로 직접 계산한다.
 */
export function useHeroCarousel(
  slideCount: MaybeRefOrGetter<number>,
  opts: HeroCarouselOptions = {},
): HeroCarouselController {
  const autoPlayMs = opts.autoPlayMs ?? DEFAULT_AUTOPLAY_MS
  const reducedMotion = opts.reducedMotion ?? false
  const index = ref(0)
  let timer: ReturnType<typeof setInterval> | undefined

  const count = (): number => Math.max(1, Math.floor(toValue(slideCount)))

  const shift = computed(() => {
    const pct = Number((index.value * PER_VIEW_PERCENT).toFixed(3))
    return `translateX(${-pct}%)`
  })

  function next(): void {
    index.value = (index.value + 1) % count()
  }

  function prev(): void {
    index.value = (index.value - 1 + count()) % count()
  }

  function goTo(i: number): void {
    index.value = Math.min(Math.max(i, 0), count() - 1)
  }

  function pause(): void {
    if (timer) {
      clearInterval(timer)
      timer = undefined
    }
  }

  function resume(): void {
    pause()
    if (reducedMotion || count() <= 1) return
    timer = setInterval(next, autoPlayMs)
  }

  // 컴포넌트 컨텍스트에서만 라이프사이클 훅 등록(테스트의 bare 호출 경고 방지).
  if (getCurrentInstance()) {
    onMounted(resume)
    onUnmounted(pause)
  }

  return { index, shift, next, prev, goTo, pause, resume }
}
