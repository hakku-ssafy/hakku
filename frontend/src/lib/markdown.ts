import { marked } from 'marked'
import DOMPurify from 'dompurify'

/**
 * 마크다운 문자열을 안전한(살균된) HTML 로 변환한다.
 * 챗봇 AI 응답을 v-html 로 렌더링하기 전에 반드시 거쳐야 한다.
 * marked 로 파싱한 뒤 DOMPurify 로 스크립트·이벤트 핸들러 등 위험 요소를 제거한다(XSS 방지).
 */
export function renderMarkdown(markdown: string | null | undefined): string {
  if (!markdown) return ''
  const rawHtml = marked.parse(markdown, { async: false, breaks: true }) as string
  return DOMPurify.sanitize(rawHtml)
}
