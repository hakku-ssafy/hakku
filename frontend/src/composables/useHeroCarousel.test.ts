import { describe, it, expect, vi } from 'vitest'
import { useHeroCarousel } from './useHeroCarousel'

describe('useHeroCarousel (무한 루프)', () => {
  it('가운데 벌의 첫 카드에서 시작한다(pos=N, realIndex 0)', () => {
    const c = useHeroCarousel(4, { reducedMotion: true })
    expect(c.pos.value).toBe(4)
    expect(c.realIndex.value).toBe(0)
  })

  it('next/prev 가 realIndex 를 순환시킨다', () => {
    const c = useHeroCarousel(4, { reducedMotion: true })
    c.next()
    expect(c.realIndex.value).toBe(1)
    c.prev()
    c.prev()
    expect(c.realIndex.value).toBe(3) // 1 → 0 → -1 == 3
  })

  it('goTo 는 해당 realIndex 의 가운데 벌 위치로 이동한다', () => {
    const c = useHeroCarousel(4, { reducedMotion: true })
    c.goTo(2)
    expect(c.realIndex.value).toBe(2)
    expect(c.pos.value).toBe(6)
    c.goTo(5) // 5 % 4 == 1
    expect(c.realIndex.value).toBe(1)
  })

  it('오른쪽 경계를 넘으면 onTransitionEnd 가 가운데 벌로 순간 리셋한다', () => {
    const c = useHeroCarousel(4, { reducedMotion: true })
    c.next()
    c.next()
    c.next()
    c.next() // pos 4 → 8 (== 2N)
    expect(c.pos.value).toBe(8)
    c.onTransitionEnd()
    expect(c.pos.value).toBe(4) // 가운데 벌 첫 카드로 리셋
    expect(c.realIndex.value).toBe(0)
  })

  it('왼쪽 경계를 넘으면 onTransitionEnd 가 가운데 벌로 순간 리셋한다', () => {
    const c = useHeroCarousel(4, { reducedMotion: true })
    c.prev() // pos 4 → 3 (< N)
    expect(c.pos.value).toBe(3)
    c.onTransitionEnd()
    expect(c.pos.value).toBe(7) // 가운데 벌 마지막 카드로 리셋
    expect(c.realIndex.value).toBe(3)
  })

  it('정상 구간 내 이동에서는 리셋하지 않는다', () => {
    const c = useHeroCarousel(4, { reducedMotion: true })
    c.next() // pos 5, 정상 구간 [4,8)
    c.onTransitionEnd()
    expect(c.pos.value).toBe(5)
  })

  it('reducedMotion 이면 자동재생하지 않는다', () => {
    vi.useFakeTimers()
    const c = useHeroCarousel(4, { reducedMotion: true, autoPlayMs: 1000 })
    vi.advanceTimersByTime(3000)
    expect(c.realIndex.value).toBe(0)
    vi.useRealTimers()
  })

  it('모션 허용 시 자동재생으로 다음 카드로 넘어간다', () => {
    vi.useFakeTimers()
    const c = useHeroCarousel(4, { reducedMotion: false, autoPlayMs: 1000 })
    c.resume()
    vi.advanceTimersByTime(1000)
    expect(c.realIndex.value).toBe(1)
    vi.useRealTimers()
  })
})
