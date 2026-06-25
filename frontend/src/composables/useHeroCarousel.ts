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
  /** 활성 카드 인덱스(0..n-1). CSS 가 --pos 로 translateX 를 계산한다. */
  pos: Ref<number>
  /** 도트·카운터용 논리 인덱스(= pos). */
  realIndex: ComputedRef<number>
  /** 첫 카드면 true(이전 비활성화용). */
  isFirst: ComputedRef<boolean>
  /** 마지막 카드면 true(다음 비활성화용). */
  isLast: ComputedRef<boolean>
  next: () => void
  prev: () => void
  goTo: (i: number) => void
  pause: () => void
  resume: () => void
}

const DEFAULT_AUTOPLAY_MS = 4500

/**
 * 홈 히어로 — 유한(non-looping) 캐러셀 컨트롤러.
 *
 * 카드를 복제하지 않고 한 벌만 렌더한다. pos 는 활성 카드 인덱스(0..n-1)이며
 * next/prev/goTo 는 경계에서 클램프되어 순환하지 않는다(같은 카드가 반복 노출되지 않음).
 * 활성 카드를 화면 중앙에 두는 정렬은 소비 컴포넌트의 CSS 가 담당하고, 여기서는
 * 정수 위치만 다룬다. 시작 위치는 리드 카드(0)이며 CSS 중앙 정렬로 가운데에서 시작한다.
 *
 * 자동재생은 마지막 카드에 도달하면 멈춰, 처음으로 되돌아가는 반복을 만들지 않는다.
 */
export function useHeroCarousel(
  slideCount: MaybeRefOrGetter<number>,
  opts: HeroCarouselOptions = {},
): HeroCarouselController {
  const autoPlayMs = opts.autoPlayMs ?? DEFAULT_AUTOPLAY_MS
  const reducedMotion = opts.reducedMotion ?? false

  const count = (): number => Math.max(1, Math.floor(toValue(slideCount)))
  const clamp = (i: number): number => Math.min(count() - 1, Math.max(0, i))

  // 리드 카드(0)에서 시작 — 중앙 정렬은 CSS 가 담당하므로 "가운데에서 시작"이 된다.
  const pos = ref(0)
  let timer: ReturnType<typeof setInterval> | undefined

  const realIndex = computed(() => clamp(pos.value))
  const isFirst = computed(() => pos.value <= 0)
  const isLast = computed(() => pos.value >= count() - 1)

  function next(): void {
    if (pos.value < count() - 1) pos.value += 1
  }

  function prev(): void {
    if (pos.value > 0) pos.value -= 1
  }

  function goTo(i: number): void {
    pos.value = clamp(i)
  }

  function pause(): void {
    if (timer) {
      clearInterval(timer)
      timer = undefined
    }
  }

  function autoAdvance(): void {
    if (pos.value >= count() - 1) {
      pause() // 마지막에 도달하면 멈춘다(처음으로 되돌아가지 않음).
      return
    }
    pos.value += 1
  }

  function resume(): void {
    pause()
    if (reducedMotion || count() <= 1 || pos.value >= count() - 1) return
    timer = setInterval(autoAdvance, autoPlayMs)
  }

  // 컴포넌트 컨텍스트에서만 라이프사이클 훅 등록(테스트의 bare 호출 경고 방지).
  if (getCurrentInstance()) {
    onMounted(resume)
    onUnmounted(pause)
  }

  return { pos, realIndex, isFirst, isLast, next, prev, goTo, pause, resume }
}
