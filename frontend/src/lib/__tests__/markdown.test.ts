import { describe, it, expect } from 'vitest'
import { renderMarkdown } from '../markdown'

describe('renderMarkdown', () => {
  it('굵게/목록 마크다운을 HTML 로 변환한다', () => {
    const html = renderMarkdown('**굵게** 그리고\n\n- 항목1\n- 항목2')
    expect(html).toContain('<strong>굵게</strong>')
    expect(html).toContain('<li>항목1</li>')
  })

  it('상품 링크를 앵커로 렌더링한다(게시물→상품 이동과 동일한 /products 경로)', () => {
    const html = renderMarkdown('[다꾸 키링](/products/7)')
    expect(html).toContain('href="/products/7"')
    expect(html).toContain('다꾸 키링')
  })

  it('스크립트/이벤트 핸들러 등 위험한 HTML 은 제거한다(XSS 방지)', () => {
    const html = renderMarkdown('정상 <script>alert(1)</script> <img src=x onerror=alert(1)>')
    expect(html).not.toContain('<script')
    expect(html.toLowerCase()).not.toContain('onerror')
  })

  it('빈 입력은 빈 문자열을 반환한다', () => {
    expect(renderMarkdown('')).toBe('')
  })
})
