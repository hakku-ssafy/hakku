import { defineConfig, devices } from '@playwright/test'

/**
 * 비주얼 QA 설정 — 4개 브레이크포인트 프로젝트로 전 라우트를 캡처한다.
 * 스냅샷은 `e2e/__screenshots__/{projectName}/{name}.png`로 정리된다.
 * (Phase 8에서 webkit/firefox 프로젝트를 추가해 크로스브라우저 확인.)
 */
export default defineConfig({
  testDir: './e2e',
  snapshotDir: './e2e/__screenshots__',
  snapshotPathTemplate: '{snapshotDir}/{projectName}/{arg}{ext}',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list']],
  expect: {
    toHaveScreenshot: { maxDiffPixelRatio: 0.01, animations: 'disabled' },
  },
  // 항상 새 dev 서버를 띄운다(reuse 금지). 예전엔 leftover 서버를 재사용하다
  // 옛 코드가 캡처되는 사일런트 회귀가 있었음 → strictPort 로 포트 점유 시 즉시 실패.
  webServer: {
    command: 'npm run dev -- --port 5173 --strictPort',
    url: 'http://localhost:5173',
    reuseExistingServer: false,
    timeout: 120_000,
  },
  use: { baseURL: 'http://localhost:5173' },
  projects: [
    { name: 'mobile-320', use: { ...devices['Desktop Chrome'], viewport: { width: 320, height: 800 } } },
    { name: 'tablet-768', use: { ...devices['Desktop Chrome'], viewport: { width: 768, height: 1024 } } },
    { name: 'laptop-1024', use: { ...devices['Desktop Chrome'], viewport: { width: 1024, height: 768 } } },
    { name: 'desktop-1440', use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } } },
  ],
})
