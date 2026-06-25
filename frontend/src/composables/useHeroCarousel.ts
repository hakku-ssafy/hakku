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
  /** 중앙 밴드에 동시에 보이는 카드 수(반응형 1/2/3). 기본 1. */
  windowSize?: MaybeRefOrGetter<number>
}

interface HeroCarouselController {
  /** 활성 윈도우의 왼쪽 카드 인덱스(0..n-W). CSS 가 --pos 로 translateX 를 계산한다. */
  pos: Ref<number>
  /** 도트·카운터용 위치 인덱스(= 클램프된 pos). */
  realIndex: ComputedRef<number>
  /** 이동 가능한 위치 수(= n-W+1). 도트 개수·카운터 분모로 쓴다. */
  positionCount: ComputedRef<number>
  /** 첫 위치면 true. */
  isFirst: ComputedRef<boolean>
  /** 마지막 위치면 true. */
  isLast: ComputedRef<boolean>
  next: () => void
  prev: () => void
  goTo: (i: number) => void
  pause: () => void
  resume: () => void
}

const DEFAULT_AUTOPLAY_MS = 4500

/**
 * 홈 히어로 — 반응형 "중앙 밴드" 캐러셀 컨트롤러.
 *
 * 카드를 복제하지 않고 한 벌만 렌더한다. 동시에 보이는 카드 수 W(windowSize)에 대해
 * pos 는 밴드의 "왼쪽 카드 인덱스"(0..n-W)이며, 화면 중앙에 W 장이 가득 차도록 정렬하는 일은
 * 소비 컴포넌트의 CSS 가 담당한다. 여기서는 정수 위치만 다룬다.
 *
 * - 시작 위치는 0(첫 카드가 밴드의 맨 왼쪽).
 * - 마지막 위치는 n-W(마지막 카드가 밴드의 맨 오른쪽) — 밴드에 빈 칸이 생기지 않는다.
 * - next/prev 는 경계에서 반대 끝으로 순환한다(마지막→처음, 처음→마지막).
 * - 카드 수가 W 이하면 위치가 하나뿐이라 이동하지 않는다(순차 정렬).
 * - 자동재생도 위치를 따라 진행하다 마지막에서 처음으로 순환한다.
 */
export function useHeroCarousel(
  slideCount: MaybeRefOrGetter<number>,
  opts: HeroCarouselOptions = {},
): HeroCarouselController {
  const autoPlayMs = opts.autoPlayMs ?? DEFAULT_AUTOPLAY_MS
  const reducedMotion = opts.reducedMotion ?? false

  const count = (): number => Math.max(1, Math.floor(toValue(slideCount)))
  // 윈도우는 1 이상, 카드 수 이하로 클램프(카드보다 큰 밴드는 의미 없음).
  const win = (): number => Math.min(count(), Math.max(1, Math.floor(toValue(opts.windowSize ?? 1))))
  const maxPos = (): number => Math.max(0, count() - win())
  const clamp = (i: number): number => Math.min(maxPos(), Math.max(0, i))

  // 왼쪽 카드(0)에서 시작 — 중앙 정렬은 CSS 가 담당한다.
  const pos = ref(0)
  let timer: ReturnType<typeof setInterval> | undefined

  const realIndex = computed(() => clamp(pos.value))
  const positionCount = computed(() => maxPos() + 1)
  const isFirst = computed(() => realIndex.value <= 0)
  const isLast = computed(() => realIndex.value >= maxPos())

  function next(): void {
    const cur = realIndex.value
    pos.value = cur >= maxPos() ? 0 : cur + 1 // 마지막→처음 순환
  }

  function prev(): void {
    const cur = realIndex.value
    pos.value = cur <= 0 ? maxPos() : cur - 1 // 처음→마지막 순환
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

  function resume(): void {
    pause()
    if (reducedMotion || positionCount.value <= 1) return
    timer = setInterval(next, autoPlayMs)
  }

  // 컴포넌트 컨텍스트에서만 라이프사이클 훅 등록(테스트의 bare 호출 경고 방지).
  if (getCurrentInstance()) {
    onMounted(resume)
    onUnmounted(pause)
  }

  return { pos, realIndex, positionCount, isFirst, isLast, next, prev, goTo, pause, resume }
}
