import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  shouldCompress,
  scaledDimensions,
  outputFilename,
  compressImage,
  DEFAULT_MAX_BYTES,
  DEFAULT_MAX_EDGE
} from './compressImage'

function fileOfSize(size: number, type = 'image/png', name = 'photo.png'): File {
  // .size 를 정확히 통제하기 위해 size 바이트짜리 버퍼로 File 을 만든다.
  return new File([new Uint8Array(size)], name, { type })
}

describe('shouldCompress', () => {
  it('임계치보다 작은 이미지는 압축하지 않는다', () => {
    expect(shouldCompress(fileOfSize(500 * 1024, 'image/jpeg'))).toBe(false)
  })

  it('임계치를 넘는 래스터 이미지는 압축 대상이다', () => {
    expect(shouldCompress(fileOfSize(DEFAULT_MAX_BYTES + 1, 'image/jpeg'))).toBe(true)
    expect(shouldCompress(fileOfSize(DEFAULT_MAX_BYTES + 1, 'image/png'))).toBe(true)
    expect(shouldCompress(fileOfSize(DEFAULT_MAX_BYTES + 1, 'image/webp'))).toBe(true)
  })

  it('GIF·SVG·비이미지는 크더라도 캔버스 압축 대상이 아니다(애니메이션·벡터 보호)', () => {
    expect(shouldCompress(fileOfSize(5 * 1024 * 1024, 'image/gif'))).toBe(false)
    expect(shouldCompress(fileOfSize(5 * 1024 * 1024, 'image/svg+xml'))).toBe(false)
    expect(shouldCompress(fileOfSize(5 * 1024 * 1024, 'application/pdf'))).toBe(false)
  })

  it('maxBytes 옵션으로 임계치를 바꿀 수 있다', () => {
    const f = fileOfSize(700 * 1024, 'image/jpeg')
    expect(shouldCompress(f, { maxBytes: 1024 * 1024 })).toBe(false)
    expect(shouldCompress(f, { maxBytes: 512 * 1024 })).toBe(true)
  })
})

describe('scaledDimensions', () => {
  it('가로가 긴 이미지는 긴 변을 maxEdge 로 맞춘다', () => {
    expect(scaledDimensions(4000, 3000, 1920)).toEqual({ width: 1920, height: 1440 })
  })

  it('세로가 긴 이미지는 높이를 maxEdge 로 맞춘다', () => {
    expect(scaledDimensions(3000, 4000, 1920)).toEqual({ width: 1440, height: 1920 })
  })

  it('정사각형 이미지도 비율을 유지한다', () => {
    expect(scaledDimensions(2000, 2000, 1920)).toEqual({ width: 1920, height: 1920 })
  })

  it('maxEdge 보다 작은 이미지는 확대하지 않는다', () => {
    expect(scaledDimensions(800, 600, 1920)).toEqual({ width: 800, height: 600 })
  })

  it('치수는 정수로 반올림한다', () => {
    const { width, height } = scaledDimensions(1000, 333, 500)
    expect(Number.isInteger(width)).toBe(true)
    expect(Number.isInteger(height)).toBe(true)
    expect(width).toBe(500)
  })
})

describe('outputFilename', () => {
  it('확장자를 출력 포맷에 맞게 교체한다', () => {
    expect(outputFilename('photo.png', 'image/webp')).toBe('photo.webp')
    expect(outputFilename('photo.png', 'image/jpeg')).toBe('photo.jpg')
  })

  it('베이스 이름의 점은 보존하고 마지막 확장자만 바꾼다', () => {
    expect(outputFilename('my.cover.PNG', 'image/webp')).toBe('my.cover.webp')
  })

  it('확장자가 없으면 새 확장자를 붙인다', () => {
    expect(outputFilename('noext', 'image/webp')).toBe('noext.webp')
  })
})

describe('compressImage', () => {
  const drawImage = vi.fn()

  beforeEach(() => {
    vi.restoreAllMocks()
    drawImage.mockClear()
    // jsdom 에는 실제 캔버스가 없으므로 브라우저 원시 API 를 스텁한다.
    ;(globalThis as unknown as { createImageBitmap: unknown }).createImageBitmap = vi.fn(
      async () => ({ width: 4000, height: 3000, close: vi.fn() })
    )
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue({
      drawImage
    } as unknown as CanvasRenderingContext2D)
    vi.spyOn(HTMLCanvasElement.prototype, 'toBlob').mockImplementation(function (
      this: HTMLCanvasElement,
      cb: BlobCallback,
      type?: string
    ) {
      cb(new Blob([new Uint8Array(10)], { type: type || 'image/png' }))
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('임계치 이하 이미지는 원본을 그대로 반환한다(재인코딩 안 함)', async () => {
    const original = fileOfSize(100 * 1024, 'image/png')
    const result = await compressImage(original)
    expect(result).toBe(original)
    expect(globalThis.createImageBitmap).not.toHaveBeenCalled()
  })

  it('큰 이미지는 긴 변을 1920px 로 줄여 WebP File 로 재인코딩한다', async () => {
    const original = fileOfSize(DEFAULT_MAX_BYTES + 1, 'image/png', 'photo.png')
    const result = await compressImage(original)

    expect(result).toBeInstanceOf(File)
    expect(result.type).toBe('image/webp')
    expect(result.name).toBe('photo.webp')
    expect(result.size).toBeLessThan(original.size)
    // 4000x3000 → 1920x1440 으로 그려져야 한다.
    const [, , , w, h] = drawImage.mock.calls[0]
    expect(w).toBe(DEFAULT_MAX_EDGE)
    expect(h).toBe(1440)
  })

  it('재인코딩 결과가 원본보다 크면 원본을 유지한다', async () => {
    vi.spyOn(HTMLCanvasElement.prototype, 'toBlob').mockImplementation(function (
      cb: BlobCallback,
      type?: string
    ) {
      cb(new Blob([new Uint8Array(4 * 1024 * 1024)], { type: type || 'image/webp' }))
    })
    const original = fileOfSize(DEFAULT_MAX_BYTES + 1, 'image/png')
    const result = await compressImage(original)
    expect(result).toBe(original)
  })

  it('WebP 인코딩을 브라우저가 지원하지 않으면 JPEG 로 폴백한다', async () => {
    vi.spyOn(HTMLCanvasElement.prototype, 'toBlob').mockImplementation(function (
      cb: BlobCallback,
      type?: string
    ) {
      // WebP 요청에 PNG 로 응답 → 미지원 시뮬레이션. JPEG 요청은 JPEG 로 응답.
      if (type === 'image/webp') cb(new Blob([new Uint8Array(10)], { type: 'image/png' }))
      else cb(new Blob([new Uint8Array(10)], { type: 'image/jpeg' }))
    })
    const original = fileOfSize(DEFAULT_MAX_BYTES + 1, 'image/png', 'photo.png')
    const result = await compressImage(original)
    expect(result.type).toBe('image/jpeg')
    expect(result.name).toBe('photo.jpg')
  })

  it('디코딩에 실패하면 원본을 그대로 업로드하도록 반환한다(graceful)', async () => {
    ;(globalThis as unknown as { createImageBitmap: unknown }).createImageBitmap = vi.fn(
      async () => {
        throw new Error('decode failed')
      }
    )
    const original = fileOfSize(DEFAULT_MAX_BYTES + 1, 'image/png')
    const result = await compressImage(original)
    expect(result).toBe(original)
  })
})
