import { ref, type Ref } from 'vue'
import { getEntry, setEntry, isFresh, dedupe, DEFAULT_STALE_TIME } from '@/lib/resourceCache'

interface UseCachedResourceOptions {
  /** 이 시간(ms) 이내면 캐시를 그대로 쓰고 백그라운드 갱신도 생략. 기본 30초. */
  staleTime?: number
}

interface CachedResource<T> {
  data: Ref<T | undefined>
  /** 캐시가 없어 첫 패칭 중일 때만 true(스피너용). */
  loading: Ref<boolean>
  /** 캐시를 보여주면서 뒤에서 갱신 중일 때 true. */
  revalidating: Ref<boolean>
  error: Ref<unknown>
  load: () => Promise<void>
}

/**
 * 뷰-로컬 패칭에 stale-while-revalidate 를 입힌다.
 *
 * - 캐시가 있으면 즉시 표시(loading=false). staleTime 이내면 네트워크 호출도 생략.
 * - staleTime 이 지났으면 캐시를 보여준 채 백그라운드로 갱신(revalidating).
 * - 캐시가 없으면 loading=true 로 첫 패칭.
 * - 갱신 실패 시 기존 캐시 데이터는 유지하고 error 만 기록한다.
 */
export function useCachedResource<T>(
  key: string,
  fetcher: () => Promise<T>,
  options: UseCachedResourceOptions = {},
): CachedResource<T> {
  const staleTime = options.staleTime ?? DEFAULT_STALE_TIME
  const initial = getEntry<T>(key)

  const data = ref(initial?.data) as Ref<T | undefined>
  const loading = ref(!initial)
  const revalidating = ref(false)
  const error = ref<unknown>(null)

  async function load(): Promise<void> {
    const entry = getEntry<T>(key)
    if (entry) {
      data.value = entry.data
      loading.value = false
      if (isFresh(key, staleTime)) return // fresh → 네트워크 생략
      revalidating.value = true
    } else {
      loading.value = true
    }

    try {
      const result = await dedupe(key, fetcher)
      setEntry(key, result)
      data.value = result
      error.value = null
    } catch (e) {
      error.value = e // 캐시 데이터는 유지(덮어쓰지 않음)
    } finally {
      loading.value = false
      revalidating.value = false
    }
  }

  return { data, loading, revalidating, error, load }
}
