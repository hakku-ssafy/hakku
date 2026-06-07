import { ref, watch, onUnmounted, toValue, type MaybeRefOrGetter } from 'vue'
import axios from 'axios'

/**
 * 인증이 필요한 이미지(예: 퍼스널컬러 진단 사진)를 Bearer 토큰과 함께
 * GET 하여 blob object URL 로 노출한다.
 *
 * `<img src>` 는 Authorization 헤더를 싣지 못하므로, 스토리지 서버가 인증을
 * 요구하는 이미지는 토큰을 붙여 직접 fetch 한 뒤 object URL 로 바꿔 바인딩해야 한다.
 * source 가 바뀌거나 컴포넌트가 unmount 되면 이전 object URL 을 정리(revoke)한다.
 */
export function useAuthedImage(source: MaybeRefOrGetter<string | null>) {
  const objectUrl = ref<string | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  let current: string | null = null

  function revoke() {
    if (current) {
      URL.revokeObjectURL(current)
      current = null
    }
  }

  async function load(url: string | null) {
    revoke()
    objectUrl.value = null
    error.value = null
    if (!url) return

    loading.value = true
    try {
      const token = localStorage.getItem('accessToken')
      const { data } = await axios.get<Blob>(url, {
        responseType: 'blob',
        headers: token ? { Authorization: `Bearer ${token}` } : undefined,
      })
      const objUrl = URL.createObjectURL(data)
      current = objUrl
      objectUrl.value = objUrl
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '이미지를 불러오지 못했습니다'
    } finally {
      loading.value = false
    }
  }

  watch(() => toValue(source), (url) => { void load(url) }, { immediate: true })
  onUnmounted(revoke)

  return { objectUrl, loading, error }
}
