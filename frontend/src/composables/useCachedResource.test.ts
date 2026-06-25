import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { useCachedResource } from './useCachedResource'
import { setEntry, clearAll } from '@/lib/resourceCache'

describe('useCachedResource (뷰 단위 SWR)', () => {
  beforeEach(() => {
    clearAll()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-06-25T00:00:00Z'))
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('캐시 미스: 처음엔 loading=true, load() 후 데이터 채우고 loading=false', async () => {
    const fetcher = vi.fn().mockResolvedValue('first')
    const r = useCachedResource('k', fetcher)
    expect(r.loading.value).toBe(true)
    expect(r.data.value).toBeUndefined()

    await r.load()

    expect(r.data.value).toBe('first')
    expect(r.loading.value).toBe(false)
    expect(fetcher).toHaveBeenCalledTimes(1)
  })

  it('캐시 히트(fresh): 즉시 데이터 표시(loading=false), 네트워크 호출 없음', async () => {
    setEntry('k', 'cached')
    const fetcher = vi.fn().mockResolvedValue('fresh-from-net')
    const r = useCachedResource('k', fetcher, { staleTime: 30_000 })

    expect(r.loading.value).toBe(false)
    expect(r.data.value).toBe('cached')

    await r.load()

    expect(fetcher).not.toHaveBeenCalled()
    expect(r.data.value).toBe('cached')
  })

  it('캐시 히트(stale): 캐시 즉시 표시 + 백그라운드 갱신(revalidating)', async () => {
    setEntry('k', 'old')
    vi.advanceTimersByTime(31_000) // staleTime(30s) 초과
    const fetcher = vi.fn().mockResolvedValue('new')
    const r = useCachedResource('k', fetcher, { staleTime: 30_000 })

    expect(r.data.value).toBe('old') // 즉시 캐시
    const p = r.load()
    expect(r.revalidating.value).toBe(true)

    await p

    expect(fetcher).toHaveBeenCalledTimes(1)
    expect(r.data.value).toBe('new')
    expect(r.revalidating.value).toBe(false)
  })

  it('미스 + 실패: error 를 담고 데이터는 비운 채 loading=false', async () => {
    const fetcher = vi.fn().mockRejectedValue(new Error('네트워크'))
    const r = useCachedResource('k', fetcher)

    await r.load()

    expect(r.error.value).toBeInstanceOf(Error)
    expect(r.data.value).toBeUndefined()
    expect(r.loading.value).toBe(false)
  })

  it('갱신 실패: 기존 캐시 데이터는 유지한다', async () => {
    setEntry('k', 'old')
    vi.advanceTimersByTime(31_000)
    const fetcher = vi.fn().mockRejectedValue(new Error('네트워크'))
    const r = useCachedResource('k', fetcher, { staleTime: 30_000 })

    await r.load()

    expect(r.data.value).toBe('old') // 갱신 실패해도 캐시 유지
    expect(r.error.value).toBeInstanceOf(Error)
  })

  it('같은 키로 두 인스턴스가 동시에 load 하면 fetcher 는 한 번만 호출된다(dedupe)', async () => {
    const fetcher = vi.fn().mockResolvedValue('shared')
    const a = useCachedResource('k', fetcher)
    const b = useCachedResource('k', fetcher)

    await Promise.all([a.load(), b.load()])

    expect(fetcher).toHaveBeenCalledTimes(1)
    expect(a.data.value).toBe('shared')
    expect(b.data.value).toBe('shared')
  })
})
