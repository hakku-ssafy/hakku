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
