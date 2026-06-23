import { test, expect, type Page } from '@playwright/test'
import { mockApi, type MockOpts } from './helpers/auth'

/**
 * 전 라우트 비주얼 회귀 캡처.
 *
 * 각 테스트는 playwright.config.ts의 4개 뷰포트 프로젝트(320/768/1024/1440)에서
 * 자동 실행되어 브레이크포인트별 풀페이지 스냅샷을 남긴다. 애니메이션은
 * toHaveScreenshot가 비활성화하므로 마퀴·펄스·스피너가 정지된 채 캡처된다.
 */

async function snap(page: Page, path: string, name: string, opts: MockOpts = {}): Promise<void> {
  await mockApi(page, opts)
  await page.goto(path)
  await page.waitForLoadState('networkidle')
  await expect(page).toHaveScreenshot(`${name}.png`, { fullPage: true })
}

test.describe('공개 라우트', () => {
  test('home — guest', async ({ page }) => {
    await snap(page, '/', 'home-guest')
  })
  test('home — diagnosis NONE', async ({ page }) => {
    await snap(page, '/', 'home-none', { authed: true, diagnosis: 'NONE' })
  })
  test('home — diagnosis PENDING', async ({ page }) => {
    await snap(page, '/', 'home-pending', { authed: true, diagnosis: 'PENDING' })
  })
  test('home — diagnosis COMPLETED', async ({ page }) => {
    await snap(page, '/', 'home-done', { authed: true, diagnosis: 'COMPLETED' })
  })
  test('product list — guest', async ({ page }) => {
    await snap(page, '/products', 'products-guest')
  })
  test('product list — authed (For You)', async ({ page }) => {
    await snap(page, '/products', 'products-authed', { authed: true, diagnosis: 'COMPLETED' })
  })
  test('product detail', async ({ page }) => {
    await snap(page, '/products/1', 'product-detail')
  })
  test('community list', async ({ page }) => {
    await snap(page, '/community', 'community')
  })
  test('post detail', async ({ page }) => {
    await snap(page, '/community/1', 'post-detail')
  })
})

test.describe('게스트 전용 라우트', () => {
  test('login', async ({ page }) => {
    await snap(page, '/login', 'login')
  })
  test('signup', async ({ page }) => {
    await snap(page, '/signup', 'signup')
  })
})

test.describe('인증 라우트', () => {
  test('onboarding', async ({ page }) => {
    await snap(page, '/onboarding', 'onboarding', { authed: true, diagnosis: 'NONE' })
  })
  test('cart', async ({ page }) => {
    await snap(page, '/cart', 'cart', { authed: true })
  })
  test('recommendations', async ({ page }) => {
    await snap(page, '/recommendations', 'recommendations', { authed: true, diagnosis: 'COMPLETED' })
  })
  test('diagnosis — NONE', async ({ page }) => {
    await snap(page, '/diagnosis', 'diagnosis-none', { authed: true, diagnosis: 'NONE' })
  })
  test('diagnosis — COMPLETED', async ({ page }) => {
    await snap(page, '/diagnosis', 'diagnosis-done', { authed: true, diagnosis: 'COMPLETED' })
  })
  test('notifications', async ({ page }) => {
    await snap(page, '/notifications', 'notifications', { authed: true })
  })
  test('my page', async ({ page }) => {
    await snap(page, '/my', 'my', { authed: true, diagnosis: 'COMPLETED' })
  })
  test('public profile', async ({ page }) => {
    await snap(page, '/users/2', 'profile', { authed: true })
  })
  test('seller products', async ({ page }) => {
    await snap(page, '/seller/products', 'seller', { authed: true, role: 'SELLER' })
  })
})

test.describe('오버레이', () => {
  test('chat window — open (authed)', async ({ page }) => {
    await mockApi(page, { authed: true })
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    await page.getByRole('button', { name: '학꾸 AI 도우미 열기' }).click()
    await expect(page.getByRole('button', { name: '닫기', exact: true })).toBeVisible()
    await expect(page).toHaveScreenshot('chat-open.png', { fullPage: true })
  })
})
