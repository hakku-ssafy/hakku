import { describe, it, expect } from 'vitest'
import { extractProductId, parseMagazineBlocks } from './magazineContent'

describe('extractProductId', () => {
  it('내부 상품 경로에서 id 를 추출한다', () => {
    expect(extractProductId('/products/123')).toBe(123)
  })

  it('뒤에 슬래시가 있어도 추출한다', () => {
    expect(extractProductId('/products/123/')).toBe(123)
  })

  it('절대 URL(호스트 포함)에서도 추출한다', () => {
    expect(extractProductId('https://hakku.app/products/45')).toBe(45)
  })

  it('쿼리스트링이 붙어도 추출한다', () => {
    expect(extractProductId('http://localhost:5173/products/7?ref=mag')).toBe(7)
  })

  it('상품 경로가 아니면 null', () => {
    expect(extractProductId('/community/3')).toBeNull()
    expect(extractProductId('https://example.com/foo')).toBeNull()
  })

  it('id 가 숫자가 아니거나 비어 있으면 null', () => {
    expect(extractProductId('/products/abc')).toBeNull()
    expect(extractProductId('/products/')).toBeNull()
    expect(extractProductId('')).toBeNull()
  })
})

describe('parseMagazineBlocks', () => {
  it('상품 링크가 없으면 마크다운 블록 하나로 반환한다', () => {
    const blocks = parseMagazineBlocks('# 제목\n\n사진과 글 본문')
    expect(blocks).toEqual([{ type: 'markdown', markdown: '# 제목\n\n사진과 글 본문' }])
  })

  it('단독 줄의 상품 경로를 상품 블록으로 변환한다', () => {
    const blocks = parseMagazineBlocks('/products/7')
    expect(blocks).toEqual([{ type: 'product', productId: 7 }])
  })

  it('단독 줄의 마크다운 상품 링크도 상품 블록으로 변환한다', () => {
    const blocks = parseMagazineBlocks('[데코 스티커](/products/7)')
    expect(blocks).toEqual([{ type: 'product', productId: 7 }])
  })

  it('글 → 상품 → 글 순서를 유지해 블록을 나눈다', () => {
    const content = '첫 문단\n\n/products/7\n\n둘째 문단'
    const blocks = parseMagazineBlocks(content)
    expect(blocks).toEqual([
      { type: 'markdown', markdown: '첫 문단' },
      { type: 'product', productId: 7 },
      { type: 'markdown', markdown: '둘째 문단' },
    ])
  })

  it('문장 안에 인라인으로 들어간 상품 링크는 카드로 임베드하지 않는다(마크다운 유지)', () => {
    const content = '이거 [상품](/products/7) 진짜 좋아요'
    const blocks = parseMagazineBlocks(content)
    expect(blocks).toEqual([
      { type: 'markdown', markdown: '이거 [상품](/products/7) 진짜 좋아요' },
    ])
  })

  it('여러 상품 블록을 각각 변환한다', () => {
    const content = '/products/1\n\n/products/2'
    const blocks = parseMagazineBlocks(content)
    expect(blocks).toEqual([
      { type: 'product', productId: 1 },
      { type: 'product', productId: 2 },
    ])
  })

  it('빈 본문이면 빈 배열', () => {
    expect(parseMagazineBlocks('')).toEqual([])
    expect(parseMagazineBlocks('   \n  ')).toEqual([])
  })
})
