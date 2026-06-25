import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/vue'
import { createRouter, createMemoryHistory, type Router } from 'vue-router'
import HeroCarousel from '../HeroCarousel.vue'
import * as magazineApi from '@/api/magazine'
import type { Magazine } from '@/types'

vi.mock('@/api/magazine')
const mockList = vi.mocked(magazineApi.listActiveMagazines)

function magazine(id: number, title: string): Magazine {
  return {
    id,
    kicker: 'EDITORIAL',
    title,
    subtitle: '큐레이션 카드',
    content: null,
    coverImageUrl: null,
    displayOrder: id,
    published: true,
    createdAt: '2026-06-25T00:00:00Z',
    updatedAt: '2026-06-25T00:00:00Z',
  }
}

async function renderHero(): Promise<Router> {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/magazine/:id', component: { template: '<div>magazine</div>' } },
      { path: '/products', component: { template: '<div />' } },
      { path: '/community', component: { template: '<div />' } },
      { path: '/diagnosis', component: { template: '<div />' } },
    ],
  })
  await router.push('/')
  await router.isReady()
  render(HeroCarousel, {
    props: { diagnosisState: 'guest' },
    global: { plugins: [router] },
  })
  return router
}

describe('HeroCarousel 큐레이션 카드 링크', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('발행 매거진을 큐레이션 카드로 렌더하고, 카드 링크가 매거진 상세를 가리킨다', async () => {
    mockList.mockResolvedValue([magazine(11, '이주의 다꾸'), magazine(22, '겨울 키링')])

    await renderHero()

    const link = await screen.findByRole('link', { name: /이주의 다꾸/ })
    expect(link.getAttribute('href')).toBe('/magazine/11')
  })

  it('큐레이션 카드를 클릭하면 매거진 상세로 이동한다 (링크 연결)', async () => {
    mockList.mockResolvedValue([magazine(11, '이주의 다꾸'), magazine(22, '겨울 키링')])

    const router = await renderHero()
    const link = await screen.findByRole('link', { name: /겨울 키링/ })

    await fireEvent.click(link)

    await waitFor(() => {
      expect(router.currentRoute.value.path).toBe('/magazine/22')
    })
  })

  it('매거진이 없으면 폴백 카드가 내부 경로로 연결된다', async () => {
    mockList.mockResolvedValue([])

    await renderHero()

    const link = await screen.findByRole('link', { name: /이주의 신상/ })
    expect(link.getAttribute('href')).toBe('/products')
  })
})
