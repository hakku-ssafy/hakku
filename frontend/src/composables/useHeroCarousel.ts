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
  /** 비주얼 위치. 정상 동작 구간은 가운데 벌 [N, 2N). */
  pos: Ref<number>
  /** 0..N-1 — 도트·카운터용 논리 인덱스. */
  realIndex: ComputedRef<number>
  /** false 면 transition 을 끄고 순간 점프(seamless wrap 리셋용). */
  animating: Ref<boolean>
  next: () => void
  prev: () => void
  goTo: (i: number) => void
  pause: () => void
  resume: () => void
  /** 트랙 transitionend 에서 호출 — 벌 경계를 넘었으면 가운데 벌로 순간 리셋. */
  onTransitionEnd: () => void
}

const DEFAULT_AUTOPLAY_MS = 4500

/**
 * 홈 히어로 — 무한 루프(seamless) peek 캐러셀 컨트롤러.
 *
 * 소비 컴포넌트가 슬라이드를 **3벌** 복제해 렌더한다는 전제로, 가운데 벌
 * (인덱스 N..2N-1)을 정상 동작 구간으로 삼는다. next/prev 로 pos 를 증감하다
 * 한쪽 벌 경계를 넘으면 transition 을 끈 채 가운데 벌의 동일 카드로 순간
 * 점프(seamless wrap)한다. 좌우로 한 벌씩 버퍼가 있어 어느 위치에서도 peek
 * 영역(메인 카드 오른쪽, 화면 끝까지)에 빈 칸이 생기지 않는다.
 *
 * translateX·카드폭은 CSS 가 `--pos` 변수로 계산하므로 여기선 정수 위치만 다룬다.
 */
export function useHeroCarousel(
  slideCount: MaybeRefOrGetter<number>,
  opts: HeroCarouselOptions = {},
): HeroCarouselController {
  const autoPlayMs = opts.autoPlayMs ?? DEFAULT_AUTOPLAY_MS
  const reducedMotion = opts.reducedMotion ?? false

  const count = (): number => Math.max(1, Math.floor(toValue(slideCount)))

  // 가운데 벌의 첫 카드에서 시작.
  const pos = ref(count())
  const animating = ref(true)
  let timer: ReturnType<typeof setInterval> | undefined
  let raf1: number | undefined
  let raf2: number | undefined

  const realIndex = computed(() => {
    const n = count()
    return (((pos.value - n) % n) + n) % n
  })

  // 두 프레임 뒤 transition 을 복원한다. 한 프레임만으론 일부 브라우저가
  // 점프 좌표를 애니메이션해버리므로 reflow 를 한 번 더 보장한다.
  function restoreTransitionNextFrame(): void {
    if (typeof requestAnimationFrame !== 'function') {
      animating.value = true
      return
    }
    raf1 = requestAnimationFrame(() => {
      raf2 = requestAnimationFrame(() => {
        animating.value = true
      })
    })
  }

  function next(): void {
    pos.value += 1
  }

  function prev(): void {
    pos.value -= 1
  }

  function goTo(i: number): void {
    const n = count()
    pos.value = n + (((i % n) + n) % n)
  }

  function onTransitionEnd(): void {
    const n = count()
    if (pos.value >= 2 * n || pos.value < n) {
      animating.value = false
      pos.value = n + realIndex.value
      restoreTransitionNextFrame()
    }
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
    onUnmounted(() => {
      pause()
      if (typeof cancelAnimationFrame === 'function') {
        if (raf1 !== undefined) cancelAnimationFrame(raf1)
        if (raf2 !== undefined) cancelAnimationFrame(raf2)
      }
    })
  }

  return { pos, realIndex, animating, next, prev, goTo, pause, resume, onTransitionEnd }
}
