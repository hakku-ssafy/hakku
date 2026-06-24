/**
 * 인증이 필요한 스토리지 이미지를 same-origin 으로 가져오기 위한 URL 정규화.
 *
 * 진단 결과 이미지는 AI 서버가 절대 도메인(STORAGE_PUBLIC_URL, 예:
 * `https://hakku.rearleg.com/storage`)으로 URL 을 stamp 한다. 앱 origin 이 그
 * 도메인과 다르면(로컬 nginx, http/https 차이 등) Authorization 헤더를 실은 fetch 가
 * cross-origin 요청이 되어 CORS preflight 를 유발하는데, 스토리지 서버는 CORS 를
 * 처리하지 않으므로 브라우저가 차단한다(= 진단 사진 조회 실패).
 *
 * 리버스 프록시(nginx)가 `/storage/` 를 스토리지 서버로 라우팅하므로, `/storage/`
 * 절대 URL 을 현재 origin 기준 상대경로로 바꾸면 항상 same-origin 으로 도달한다.
 * 공개 상품 이미지가 이미 상대경로(`/storage/images/{id}`)를 쓰는 것과 동일한 방식.
 */
export function toSameOriginStorageUrl(rawUrl: string): string {
  try {
    const parsed = new URL(rawUrl, window.location.origin)
    const isCrossOrigin = parsed.origin !== window.location.origin
    const isStoragePath = parsed.pathname.startsWith('/storage/')
    if (isCrossOrigin && isStoragePath) {
      return `${parsed.pathname}${parsed.search}`
    }
    return rawUrl
  } catch {
    return rawUrl
  }
}
