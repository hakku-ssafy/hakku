import { describe, it, expect, vi } from 'vitest'
import { useHeroCarousel } from './useHeroCarousel'

describe('useHeroCarousel (반응형 윈도우 · 위치 단위 · 순환)', () => {
  it('윈도우 왼쪽 인덱스(pos=0)에서 시작한다', () => {
    const c = useHeroCarousel(6, { reducedMotion: true, windowSize: 3 })
    expect(c.pos.value).toBe(0)
    expect(c.realIndex.value).toBe(0)
    // 이동 가능한 위치 수 = n - W + 1 = 6 - 3 + 1 = 4
    expect(c.positionCount.value).toBe(4)
    expect(c.isFirst.value).toBe(true)
    expect(c.isLast.value).toBe(false)
  })

  it('next 는 마지막 위치(n-W)까지 1칸씩 이동한다', () => {
    const c = useHeroCarousel(6, { reducedMotion: true, windowSize: 3 })
    c.next()
    expect(c.pos.value).toBe(1)
    c.next()
    c.next()
    expect(c.pos.value).toBe(3) // n - W = 3 (마지막 위치)
    expect(c.isLast.value).toBe(true)
  })

  it('마지막 위치에서 next 하면 처음(반대 끝)으로 순환한다', () => {
    const c = useHeroCarousel(6, { reducedMotion: true, windowSize: 3 })
    c.goTo(3) // 마지막 위치
    c.next()
    expect(c.pos.value).toBe(0)
  })

  it('첫 위치에서 prev 하면 마지막(반대 끝)으로 순환한다', () => {
    const c = useHeroCarousel(6, { reducedMotion: true, windowSize: 3 })
    c.prev()
    expect(c.pos.value).toBe(3) // n - W
  })

  it('goTo 는 위치를 [0, n-W] 로 클램프한다', () => {
    const c = useHeroCarousel(6, { reducedMotion: true, windowSize: 3 })
    c.goTo(2)
    expect(c.pos.value).toBe(2)
    c.goTo(99)
    expect(c.pos.value).toBe(3)
    c.goTo(-5)
    expect(c.pos.value).toBe(0)
  })

  it('카드 수가 윈도우와 같으면 위치가 하나뿐이라 이동하지 않는다(순차 정렬)', () => {
    const c = useHeroCarousel(3, { reducedMotion: true, windowSize: 3 })
    expect(c.positionCount.value).toBe(1)
    c.next()
    expect(c.pos.value).toBe(0)
    c.prev()
    expect(c.pos.value).toBe(0)
  })

  it('윈도우가 카드 수보다 크면 윈도우가 카드 수로 클램프되어 위치가 하나뿐이다', () => {
    const c = useHeroCarousel(2, { reducedMotion: true, windowSize: 3 })
    expect(c.positionCount.value).toBe(1)
    expect(c.isFirst.value).toBe(true)
    expect(c.isLast.value).toBe(true)
  })

  it('windowSize 기본값은 1 이다(카드 1장 단위, 위치 수 = n)', () => {
    const c = useHeroCarousel(4, { reducedMotion: true })
    expect(c.positionCount.value).toBe(4)
    c.goTo(3)
    expect(c.pos.value).toBe(3)
  })

  it('reducedMotion 이면 자동재생하지 않는다', () => {
    vi.useFakeTimers()
    const c = useHeroCarousel(6, { reducedMotion: true, windowSize: 3, autoPlayMs: 1000 })
    c.resume()
    vi.advanceTimersByTime(3000)
    expect(c.pos.value).toBe(0)
    vi.useRealTimers()
  })

  it('자동재생은 위치를 따라 진행하고 마지막에서 처음으로 순환한다', () => {
    vi.useFakeTimers()
    // n=5, W=3 → 위치: 0,1,2 (n-W=2)
    const c = useHeroCarousel(5, { reducedMotion: false, windowSize: 3, autoPlayMs: 1000 })
    c.resume()
    vi.advanceTimersByTime(1000)
    expect(c.pos.value).toBe(1)
    vi.advanceTimersByTime(1000)
    expect(c.pos.value).toBe(2) // 마지막 위치
    vi.advanceTimersByTime(1000)
    expect(c.pos.value).toBe(0) // 순환(처음으로)
    vi.useRealTimers()
  })
})
