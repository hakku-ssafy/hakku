/**
 * 매거진 마크다운 본문을 "마크다운 블록"과 "상품 임베드 블록"으로 분해한다.
 *
 * 작성자가 본문에 학꾸 내부 상품 링크(`/products/:id`)를 한 줄로 단독으로 넣으면,
 * 렌더링 시 그 줄을 가로로 긴 상품 카드로 임베드한다. 문장 안에 인라인으로 들어간
 * 상품 링크는 일반 마크다운 링크로 그대로 둔다.
 */

export type MagazineBlock =
  | { type: 'markdown'; markdown: string }
  | { type: 'product'; productId: number }

/**
 * 링크 href 에서 학꾸 내부 상품 id 를 추출한다.
 * `/products/123`, `/products/123/`, 절대 URL, 쿼리스트링을 모두 허용한다.
 * 내부 상품 경로가 아니면 null.
 */
export function extractProductId(href: string): number | null {
  if (!href) return null
  let path = href.trim()

  // 절대 URL 이면 경로(pathname)만 사용한다.
  const scheme = path.match(/^https?:\/\/[^/]+(\/.*)?$/i)
  if (scheme) path = scheme[1] ?? ''

  // 쿼리스트링/해시 제거
  path = path.split(/[?#]/)[0]

  const matched = path.match(/^\/products\/(\d+)\/?$/)
  if (!matched) return null

  const id = Number(matched[1])
  return Number.isInteger(id) && id > 0 ? id : null
}

/** 한 줄이 통째로 상품 참조(베어 경로 또는 마크다운 링크)면 상품 id, 아니면 null. */
function lineProductId(line: string): number | null {
  const trimmed = line.trim()
  if (!trimmed) return null

  // 단독 마크다운 링크: [라벨](url)
  const mdLink = trimmed.match(/^\[[^\]]*\]\(([^)\s]+)\)$/)
  if (mdLink) return extractProductId(mdLink[1])

  // 단독 베어 경로/URL
  return extractProductId(trimmed)
}

/** 매거진 본문을 순서가 보존된 블록 배열로 분해한다. 빈 본문이면 빈 배열. */
export function parseMagazineBlocks(content: string): MagazineBlock[] {
  if (!content || !content.trim()) return []

  const blocks: MagazineBlock[] = []
  let buffer: string[] = []

  const flush = (): void => {
    if (buffer.length === 0) return
    const markdown = buffer.join('\n').trim()
    if (markdown) blocks.push({ type: 'markdown', markdown })
    buffer = []
  }

  for (const line of content.split('\n')) {
    const productId = lineProductId(line)
    if (productId !== null) {
      flush()
      blocks.push({ type: 'product', productId })
    } else {
      buffer.push(line)
    }
  }
  flush()

  return blocks
}
