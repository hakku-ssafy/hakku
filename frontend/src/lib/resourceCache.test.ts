import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import {
  getEntry,
  setEntry,
  isFresh,
  invalidate,
  invalidatePrefix,
  clearAll,
  dedupe,
} from './resourceCache'

describe('resourceCache (모듈 레벨 SWR 캐시)', () => {
  beforeEach(() => {
    clearAll()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-06-25T00:00:00Z'))
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('setEntry 한 값을 getEntry 로 되돌려준다', () => {
    setEntry('products', [{ id: 1 }])
    expect(getEntry<{ id: number }[]>('products')?.data).toEqual([{ id: 1 }])
  })

  it('없는 키는 undefined 를 반환한다', () => {
    expect(getEntry('missing')).toBeUndefined()
  })

  it('fetchedAt 으로 isFresh 를 판단한다(staleTime 이내 fresh)', () => {
    setEntry('k', 1)
    expect(isFresh('k', 30_000)).toBe(true)
    vi.advanceTimersByTime(29_000)
    expect(isFresh('k', 30_000)).toBe(true)
    vi.advanceTimersByTime(2_000) // 총 31s 경과
    expect(isFresh('k', 30_000)).toBe(false)
  })

  it('없는 키의 isFresh 는 false 다', () => {
    expect(isFresh('nope', 30_000)).toBe(false)
  })

  it('invalidate 는 해당 키만 제거한다', () => {
    setEntry('a', 1)
    setEntry('b', 2)
    invalidate('a')
    expect(getEntry('a')).toBeUndefined()
    expect(getEntry('b')?.data).toBe(2)
  })

  it('invalidatePrefix 는 접두사로 시작하는 키를 모두 제거한다', () => {
    setEntry('posts:all', 1)
    setEntry('posts:free', 2)
    setEntry('products', 3)
    invalidatePrefix('posts:')
    expect(getEntry('posts:all')).toBeUndefined()
    expect(getEntry('posts:free')).toBeUndefined()
    expect(getEntry('products')?.data).toBe(3)
  })

  it('clearAll 은 전체를 비운다(로그아웃 등)', () => {
    setEntry('a', 1)
    setEntry('b', 2)
    clearAll()
    expect(getEntry('a')).toBeUndefined()
    expect(getEntry('b')).toBeUndefined()
  })

  it('dedupe 는 동시 호출 시 fetcher 를 한 번만 부르고 같은 결과를 공유한다', async () => {
    const fetcher = vi.fn().mockResolvedValue('value')
    const [a, b] = await Promise.all([dedupe('k', fetcher), dedupe('k', fetcher)])
    expect(a).toBe('value')
    expect(b).toBe('value')
    expect(fetcher).toHaveBeenCalledTimes(1)
  })

  it('dedupe 는 이전 호출이 끝나면 다음 호출에서 다시 fetcher 를 부른다', async () => {
    const fetcher = vi.fn().mockResolvedValue('v')
    await dedupe('k', fetcher)
    await dedupe('k', fetcher)
    expect(fetcher).toHaveBeenCalledTimes(2)
  })

  it('dedupe 는 실패 시 in-flight 를 정리해 재시도가 가능하다', async () => {
    const fetcher = vi
      .fn()
      .mockRejectedValueOnce(new Error('네트워크'))
      .mockResolvedValueOnce('ok')
    await expect(dedupe('k', fetcher)).rejects.toThrow('네트워크')
    await expect(dedupe('k', fetcher)).resolves.toBe('ok')
    expect(fetcher).toHaveBeenCalledTimes(2)
  })
})
