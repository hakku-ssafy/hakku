/**
 * 업로드 이미지를 클라이언트에서 미리 줄여 storage-server 부담과 대역폭을 아낀다.
 * "어떠한 이미지가 올라오든 일정 사이즈 이상이면 용량을 낮춘다" 요구사항 구현.
 *
 * 정책: 파일이 maxBytes(기본 1MB)를 넘는 래스터 이미지(jpeg/png/webp)면
 * 긴 변을 maxEdge(기본 1920px)로 축소하고 WebP(미지원 시 JPEG)로 재인코딩한다.
 * GIF(애니메이션)·SVG(벡터)·비이미지는 손상 위험이 있어 건드리지 않는다.
 */

export interface CompressOptions {
  /** 이 바이트 수를 넘을 때만 압축한다. */
  maxBytes?: number
  /** 긴 변의 최대 픽셀. 비율은 유지한다. */
  maxEdge?: number
  /** 손실 인코딩 품질 (0~1). */
  quality?: number
  /** 선호 출력 MIME. 브라우저 미지원 시 JPEG 로 폴백한다. */
  mimeType?: string
}

export const DEFAULT_MAX_BYTES = 1024 * 1024
export const DEFAULT_MAX_EDGE = 1920
export const DEFAULT_QUALITY = 0.8

const PREFERRED_MIME = 'image/webp'
const FALLBACK_MIME = 'image/jpeg'

/** 캔버스로 안전하게 재인코딩 가능한 래스터 포맷. */
const COMPRESSIBLE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp'])

const EXT_BY_MIME: Record<string, string> = {
  'image/webp': 'webp',
  'image/jpeg': 'jpg',
  'image/png': 'png'
}

/** 파일이 압축 대상인지 판단한다(크기 + 안전하게 재인코딩 가능한 포맷). */
export function shouldCompress(
  file: { size: number; type: string },
  opts: Pick<CompressOptions, 'maxBytes'> = {}
): boolean {
  const maxBytes = opts.maxBytes ?? DEFAULT_MAX_BYTES
  return COMPRESSIBLE_TYPES.has(file.type) && file.size > maxBytes
}

/** 비율을 유지하며 긴 변을 maxEdge 이하로 줄인 치수. 작은 이미지는 확대하지 않는다. */
export function scaledDimensions(
  width: number,
  height: number,
  maxEdge: number
): { width: number; height: number } {
  const longest = Math.max(width, height)
  if (longest <= maxEdge) return { width, height }
  const scale = maxEdge / longest
  return { width: Math.round(width * scale), height: Math.round(height * scale) }
}

/** 출력 MIME 에 맞춰 파일명의 확장자만 교체한다(베이스의 점은 보존). */
export function outputFilename(name: string, mime: string): string {
  const ext = EXT_BY_MIME[mime] ?? 'img'
  const dot = name.lastIndexOf('.')
  const base = dot > 0 ? name.slice(0, dot) : name
  return `${base}.${ext}`
}

function toBlobAsync(
  canvas: HTMLCanvasElement,
  type: string,
  quality: number
): Promise<Blob | null> {
  return new Promise((resolve) => canvas.toBlob((blob) => resolve(blob), type, quality))
}

/** 선호 포맷으로 인코딩하되, 브라우저가 무시(다른 type 반환)하면 JPEG 로 폴백한다. */
async function encodeCanvas(
  canvas: HTMLCanvasElement,
  preferred: string,
  quality: number
): Promise<Blob | null> {
  const blob = await toBlobAsync(canvas, preferred, quality)
  if (blob && blob.type === preferred) return blob
  if (preferred !== FALLBACK_MIME) {
    const fallback = await toBlobAsync(canvas, FALLBACK_MIME, quality)
    if (fallback) return fallback
  }
  return blob
}

/**
 * 필요 시 이미지를 축소·재인코딩한 새 File 을 반환한다.
 * 압축 대상이 아니거나, 결과가 더 크거나, 디코딩/인코딩에 실패하면 원본 File 을 그대로 반환한다.
 */
export async function compressImage(file: File, opts: CompressOptions = {}): Promise<File> {
  const maxBytes = opts.maxBytes ?? DEFAULT_MAX_BYTES
  const maxEdge = opts.maxEdge ?? DEFAULT_MAX_EDGE
  const quality = opts.quality ?? DEFAULT_QUALITY
  const preferred = opts.mimeType ?? PREFERRED_MIME

  if (!shouldCompress(file, { maxBytes })) return file

  try {
    const bitmap = await createImageBitmap(file)
    try {
      const { width, height } = scaledDimensions(bitmap.width, bitmap.height, maxEdge)
      const canvas = document.createElement('canvas')
      canvas.width = width
      canvas.height = height
      const ctx = canvas.getContext('2d')
      if (!ctx) return file
      ctx.drawImage(bitmap, 0, 0, width, height)

      const blob = await encodeCanvas(canvas, preferred, quality)
      // 재인코딩이 오히려 용량을 키우면 원본을 유지한다.
      if (!blob || blob.size >= file.size) return file

      const mime = blob.type || preferred
      return new File([blob], outputFilename(file.name, mime), { type: mime })
    } finally {
      bitmap.close?.()
    }
  } catch {
    // 디코딩·인코딩 실패 시 원본 그대로 업로드 (graceful degradation)
    return file
  }
}
