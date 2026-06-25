import { describe, it, expect, vi, beforeEach } from 'vitest'

// storage.ts 는 모듈 내부에서 axios.create() 로 전용 클라이언트를 만든다.
// 그 인스턴스의 post 를 가로채기 위해 axios 를 모킹한다.
const { post } = vi.hoisted(() => ({ post: vi.fn() }))
vi.mock('axios', () => ({ default: { create: () => ({ post }) } }))

import { uploadProductImage, uploadShowcaseImage } from '../storage'

function imageFile(name = 'p.png'): File {
  return new File([new Uint8Array([1, 2, 3])], name, { type: 'image/png' })
}

describe('uploadProductImage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    post.mockResolvedValue({ data: { id: 'img-123' } })
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
