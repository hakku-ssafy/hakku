import { describe, it, expect, vi } from 'vitest'
import { useHeroCarousel } from './useHeroCarousel'

describe('useHeroCarousel', () => {
  it('starts at index 0 and computes shift', () => {
    const c = useHeroCarousel(4, { reducedMotion: true })
    expect(c.index.value).toBe(0)
    expect(c.shift.value).toContain('translateX(0')
  })
  it('next/prev wrap around bounds', () => {
    const c = useHeroCarousel(3, { reducedMotion: true })
    c.prev(); expect(c.index.value).toBe(2)
    c.next(); expect(c.index.value).toBe(0)
  })
  it('goTo clamps to range', () => {
    const c = useHeroCarousel(3, { reducedMotion: true })
    c.goTo(5); expect(c.index.value).toBe(2)
    c.goTo(-2); expect(c.index.value).toBe(0)
  })
  it('does not autoplay when reducedMotion', () => {
    vi.useFakeTimers()
    const c = useHeroCarousel(3, { reducedMotion: true, autoPlayMs: 1000 })
    vi.advanceTimersByTime(3000)
    expect(c.index.value).toBe(0)
    vi.useRealTimers()
  })
  it('autoplays when motion allowed', () => {
    vi.useFakeTimers()
    const c = useHeroCarousel(3, { reducedMotion: false, autoPlayMs: 1000 })
    c.resume()
    vi.advanceTimersByTime(1000)
    expect(c.index.value).toBe(1)
    vi.useRealTimers()
  })
})
