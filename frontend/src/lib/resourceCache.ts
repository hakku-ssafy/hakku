/**
 * 모듈 레벨 SWR(stale-while-revalidate) 캐시.
 *
 * SPA 가 살아있는 동안(페이지 이동에는 유지, 새로고침/F5 시 소멸) 패칭 결과를 키 단위로
 * 보관한다. 같은 키로 동시에 들어온 패칭은 dedupe 로 합쳐 중복 네트워크 요청을 막는다.
 * 사용자별 데이터는 키에 userId 를 포함하고, 로그아웃 시 clearAll() 로 비워야 한다.
 */

export interface CacheEntry<T> {
  data: T
  /** 저장 시각(ms, epoch). staleTime 판단에 쓴다. */
  fetchedAt: number
}

/** 기본 staleTime(ms). 이 시간 이내 재방문은 네트워크 호출 없이 캐시만 쓴다. */
export const DEFAULT_STALE_TIME = 30_000

const store = new Map<string, CacheEntry<unknown>>()
const inflight = new Map<string, Promise<unknown>>()

export function getEntry<T>(key: string): CacheEntry<T> | undefined {
  return store.get(key) as CacheEntry<T> | undefined
}

export function setEntry<T>(key: string, data: T): void {
  store.set(key, { data, fetchedAt: Date.now() })
}

/** 캐시가 존재하고 staleTime(ms) 이내면 true. */
export function isFresh(key: string, staleTime: number): boolean {
  const entry = store.get(key)
  if (!entry) return false
  return Date.now() - entry.fetchedAt < staleTime
}

export function invalidate(key: string): void {
  store.delete(key)
}

export function invalidatePrefix(prefix: string): void {
  for (const key of store.keys()) {
    if (key.startsWith(prefix)) store.delete(key)
  }
}

export function clearAll(): void {
  store.clear()
  inflight.clear()
}

/**
 * 같은 키로 진행 중인 패칭이 있으면 그 Promise 를 공유하고, 없으면 fetcher 를 실행한다.
 * 성공/실패와 무관하게 완료되면 in-flight 항목을 비워 다음 호출이 다시 패칭할 수 있게 한다.
 */
export function dedupe<T>(key: string, fetcher: () => Promise<T>): Promise<T> {
  const existing = inflight.get(key)
  if (existing) return existing as Promise<T>

  const promise = fetcher().finally(() => {
    inflight.delete(key)
  })
  inflight.set(key, promise)
  return promise
}
