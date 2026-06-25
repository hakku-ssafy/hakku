import axios from 'axios'

const storageClient = axios.create({
  baseURL: import.meta.env.VITE_STORAGE_BASE_URL ?? '/storage'
})

function publicImageUrl(imageId: string): string {
  const base = import.meta.env.VITE_STORAGE_PUBLIC_URL ?? '/storage'
  return `${base.replace(/\/$/, '')}/images/${imageId}`
}

/** 상품 이미지를 storage-server에 업로드하고 공개 URL을 반환한다. */
export async function uploadProductImage(file: File): Promise<string> {
  const { data } = await storageClient.post<{ id: string }>(
    '/images',
    file,
    {
      params: { kind: 'result' },
      headers: { 'Content-Type': file.type || 'application/octet-stream' },
      maxBodyLength: 10 * 1024 * 1024
    }
  )
  return publicImageUrl(data.id)
}

/**
 * '학생증 자랑' 공유 이미지를 업로드하고 공개 URL을 반환한다.
 * 게시판은 누구나 볼 수 있어야 하므로 인증 없이 조회 가능한 raw 종류로 저장한다.
 */
export async function uploadShowcaseImage(file: File): Promise<string> {
  const { data } = await storageClient.post<{ id: string }>(
    '/images',
    file,
    {
      params: { kind: 'raw' },
      headers: { 'Content-Type': file.type || 'application/octet-stream' },
      maxBodyLength: 10 * 1024 * 1024
    }
  )
  return publicImageUrl(data.id)
}

/**
 * 매거진 커버 이미지를 업로드하고 공개 URL을 반환한다.
 * 메인 캐러셀은 비로그인 포함 누구나 보므로 인증 없이 조회 가능한 raw 종류로 저장한다.
 */
export async function uploadMagazineImage(file: File): Promise<string> {
  const { data } = await storageClient.post<{ id: string }>(
    '/images',
    file,
    {
      params: { kind: 'raw' },
      headers: { 'Content-Type': file.type || 'application/octet-stream' },
      maxBodyLength: 10 * 1024 * 1024
    }
  )
  return publicImageUrl(data.id)
}
