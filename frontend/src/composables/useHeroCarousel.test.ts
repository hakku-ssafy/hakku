import { describe, it, expect, vi } from 'vitest'
import { useHeroCarousel } from './useHeroCarousel'

describe('useHeroCarousel (유한 · 중앙 시작)', () => {
  it('첫 카드(중앙)에서 시작한다 (pos=0, realIndex 0)', () => {
    const c = useHeroCarousel(4, { reducedMotion: true })
    expect(c.pos.value).toBe(0)
    expect(c.realIndex.value).toBe(0)
    expect(c.isFirst.value).toBe(true)
    expect(c.isLast.value).toBe(false)
  })

  it('next/prev 가 경계에서 멈춘다(순환하지 않는다)', () => {
    const c = useHeroCarousel(4, { reducedMotion: true })
    c.prev() // 이미 0 → 그대로(왼쪽 경계)
    expect(c.realIndex.value).toBe(0)
    c.next()
    c.next()
    expect(c.realIndex.value).toBe(2)
    c.next() // → 3(마지막)
    c.next() // 마지막에서 멈춤(반복 없음)
    expect(c.realIndex.value).toBe(3)
    expect(c.isLast.value).toBe(true)
    c.prev()
    expect(c.realIndex.value).toBe(2)
  })

  it('goTo 는 인덱스를 [0, n-1] 로 클램프한다', () => {
    const c = useHeroCarousel(4, { reducedMotion: true })
    c.goTo(2)
    expect(c.realIndex.value).toBe(2)
    c.goTo(99)
    expect(c.realIndex.value).toBe(3)
    c.goTo(-5)
    expect(c.realIndex.value).toBe(0)
  })

  it('reducedMotion 이면 자동재생하지 않는다', () => {
    vi.useFakeTimers()
    const c = useHeroCarousel(4, { reducedMotion: true, autoPlayMs: 1000 })
    c.resume()
    vi.advanceTimersByTime(3000)
    expect(c.realIndex.value).toBe(0)
    vi.useRealTimers()
  })

  it('모션 허용 시 자동재생으로 다음 카드로 넘어가고, 마지막에서 멈춘다(반복 없음)', () => {
    vi.useFakeTimers()
    const c = useHeroCarousel(2, { reducedMotion: false, autoPlayMs: 1000 })
    c.resume()
    vi.advanceTimersByTime(1000)
    expect(c.realIndex.value).toBe(1) // 0 → 1(마지막)
    vi.advanceTimersByTime(3000)
    expect(c.realIndex.value).toBe(1) // 더 이상 넘어가지 않는다(순환 X)
    vi.useRealTimers()
  })
})
