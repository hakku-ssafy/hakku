import { describe, it, expect, vi, beforeEach } from 'vitest'

// storage.ts 는 모듈 내부에서 axios.create() 로 전용 클라이언트를 만든다.
// 그 인스턴스의 post 를 가로채기 위해 axios 를 모킹한다.
const { post, compressImage } = vi.hoisted(() => ({ post: vi.fn(), compressImage: vi.fn() }))
vi.mock('axios', () => ({ default: { create: () => ({ post }) } }))
vi.mock('@/lib/compressImage', () => ({ compressImage }))

import { uploadProductImage, uploadShowcaseImage, uploadMagazineImage } from '../storage'

function imageFile(name = 'p.png'): File {
  return new File([new Uint8Array([1, 2, 3])], name, { type: 'image/png' })
}

describe('uploadProductImage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    post.mockResolvedValue({ data: { id: 'img-123' } })
    // 기본은 패스스루: 압축이 없을 때의 업로드 동작을 검증하는 기존 테스트를 보존한다.
    compressImage.mockImplementation((file: File) => Promise.resolve(file))
  })

  it('상품 이미지는 비로그인 공개 조회가 가능하도록 kind=raw 로 업로드한다', async () => {
    await uploadProductImage(imageFile())

    expect(post).toHaveBeenCalledTimes(1)
    const [url, body, config] = post.mock.calls[0]
    expect(url).toBe('/images')
    expect(body).toBeInstanceOf(File)
    expect(config.params).toEqual({ kind: 'raw' })
  })

  it('업로드한 이미지 id 로 공개 URL 을 만들어 반환한다', async () => {
    const url = await uploadProductImage(imageFile())
    expect(url).toContain('/images/img-123')
  })

  it('쇼케이스 이미지와 동일하게 raw 종류를 사용한다(공개 자산 일관성)', async () => {
    await uploadProductImage(imageFile())
    const productKind = post.mock.calls[0][2].params.kind
    vi.clearAllMocks()
    post.mockResolvedValue({ data: { id: 'img-456' } })
    await uploadShowcaseImage(imageFile())
    const showcaseKind = post.mock.calls[0][2].params.kind
    expect(productKind).toBe(showcaseKind)
  })
})

describe('업로드 전 자동 압축', () => {
  const uploaders = [
    ['상품', uploadProductImage],
    ['쇼케이스', uploadShowcaseImage],
    ['매거진', uploadMagazineImage]
  ] as const

  beforeEach(() => {
    vi.clearAllMocks()
    post.mockResolvedValue({ data: { id: 'img-123' } })
    compressImage.mockImplementation((file: File) => Promise.resolve(file))
  })

  it.each(uploaders)('%s 업로드도 compressImage 를 거친다(어떤 이미지든 압축)', async (_label, upload) => {
    const original = imageFile()
    await upload(original)
    expect(compressImage).toHaveBeenCalledWith(original)
  })

  it('압축된 파일을 그 MIME 타입으로 업로드한다(원본 대신)', async () => {
    const original = imageFile('big.png')
    const compressed = new File([new Uint8Array([9])], 'big.webp', { type: 'image/webp' })
    compressImage.mockResolvedValue(compressed)

    await uploadProductImage(original)

    const [, body, config] = post.mock.calls[0]
    expect(body).toBe(compressed)
    expect(config.headers['Content-Type']).toBe('image/webp')
  })
})
