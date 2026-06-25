import axios from 'axios'
import { compressImage } from '@/lib/compressImage'

const storageClient = axios.create({
  baseURL: import.meta.env.VITE_STORAGE_BASE_URL ?? '/storage'
})

function publicImageUrl(imageId: string): string {
  const base = import.meta.env.VITE_STORAGE_PUBLIC_URL ?? '/storage'
  return `${base.replace(/\/$/, '')}/images/${imageId}`
}

/**
 * 공개 자산 이미지를 storage-server 에 업로드하고 공개 URL 을 반환한다.
 *
 * 업로드 전, 일정 크기를 넘는 이미지는 클라이언트에서 자동으로 용량을 줄인다(compressImage).
 * 상품·쇼케이스·매거진 이미지는 모두 비로그인 포함 누구나 보고, storageClient 는 인증 헤더를
 * 싣지 않으므로 소유자 전용(result)이 아니라 인증 없이 조회 가능한 raw 종류로 저장한다.
 */
async function uploadRawImage(file: File): Promise<string> {
  const optimized = await compressImage(file)
  const { data } = await storageClient.post<{ id: string }>(
    '/images',
    optimized,
    {
      params: { kind: 'raw' },
      headers: { 'Content-Type': optimized.type || 'application/octet-stream' },
      maxBodyLength: 10 * 1024 * 1024
    }
  )
  return publicImageUrl(data.id)
}

/** 상품 이미지를 업로드하고 공개 URL 을 반환한다. */
export function uploadProductImage(file: File): Promise<string> {
  return uploadRawImage(file)
}

/** '학생증 자랑' 공유 이미지를 업로드하고 공개 URL 을 반환한다. */
export function uploadShowcaseImage(file: File): Promise<string> {
  return uploadRawImage(file)
}

/** 매거진 커버 이미지를 업로드하고 공개 URL 을 반환한다. */
export function uploadMagazineImage(file: File): Promise<string> {
  return uploadRawImage(file)
}
