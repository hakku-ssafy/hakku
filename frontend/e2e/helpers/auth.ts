import type { Page, Route } from '@playwright/test'
import { fixtures, type ResolveOpts } from '../fixtures'

export type MockOpts = ResolveOpts

/** 따뜻한 톤 SVG — storage 이미지 자리에 깨진 이미지 대신 채운다. */
const PLACEHOLDER_SVG =
  '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400">' +
  '<rect width="400" height="400" fill="#e9e2d4"/></svg>'

function parseBody(route: Route): unknown {
  const raw = route.request().postData()
  if (!raw) return undefined
  try {
    return JSON.parse(raw)
  } catch {
    return undefined
  }
}

/**
 * 라우트별 비주얼 QA를 위한 API mock 설치.
 *
 * - `opts.authed`면 localStorage.accessToken 주입(가드 통과 + Bearer 헤더).
 * - `**​/api/**` 요청을 픽스처 응답으로 가로챈다.
 * - `**​/storage/**`(인증 이미지 포함)는 플레이스홀더 이미지로 응답.
 * - `**​/ai/**`(진단 업로드)는 빈 200으로 응답.
 *
 * `E2E_REAL_BACKEND=1`이면 mock 없이 실제 백엔드로 통과시킨다.
 */
export async function mockApi(page: Page, opts: MockOpts = {}): Promise<void> {
  if (process.env.E2E_REAL_BACKEND === '1') return

  if (opts.authed) {
    await page.addInitScript(() => {
      localStorage.setItem('accessToken', 'e2e-token')
    })
  }

  await page.route('**/api/**', async (route) => {
    const body = fixtures.resolve(route.request().url(), {
      ...opts,
      method: route.request().method(),
      body: parseBody(route),
    })
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(body),
    })
  })

  await page.route('**/storage/**', async (route) => {
    await route.fulfill({ status: 200, contentType: 'image/svg+xml', body: PLACEHOLDER_SVG })
  })

  await page.route('**/ai/**', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: '{}' })
  })
}
