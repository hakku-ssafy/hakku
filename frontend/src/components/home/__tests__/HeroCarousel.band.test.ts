import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, waitFor, fireEvent } from '@testing-library/vue'
import { createRouter, createMemoryHistory } from 'vue-router'
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

function setViewport(width: number): void {
  Object.defineProperty(window, 'innerWidth', { configurable: true, writable: true, value: width })
  window.dispatchEvent(new Event('resize'))
}

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/magazine/:id', component: { template: '<div>magazine</div>' } },
      { path: '/products', component: { template: '<div />' } },
      { path: '/community', component: { template: '<div />' } },
      { path: '/diagnosis', component: { template: '<div />' } },
    ],
  })
}

async function renderHero() {
  const router = makeRouter()
  await router.push('/')
  await router.isReady()
  const utils = render(HeroCarousel, {
    props: { diagnosisState: 'guest' },
    global: { plugins: [router] },
  })
  return { ...utils, router }
}

function bandIndices(container: Element): number[] {
  const slides = Array.from(container.querySelectorAll('.hero__slide'))
  return slides.flatMap((el, i) => (el.classList.contains('hero__slide--in-band') ? [i] : []))
}

describe('HeroCarousel 중앙 밴드 · 위치 도트 · 순환', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('데스크탑(≥1024)에서는 중앙 3장을 밴드로 채우고, 시작 시 첫 3장이 밴드에 든다', async () => {
    setViewport(1280)
    // 리드 1 + 매거진 4 = 슬라이드 5장
    mockList.mockResolvedValue([
      magazine(1, 'A'),
      magazine(2, 'B'),
      magazine(3, 'C'),
      magazine(4, 'D'),
    ])
    const { container } = await renderHero()

    await waitFor(() => {
      expect(bandIndices(container)).toEqual([0, 1, 2])
    })
  })

  it('도트는 이동 위치 수(n-W+1)만큼 렌더한다', async () => {
    setViewport(1280)
    mockList.mockResolvedValue([
      magazine(1, 'A'),
      magazine(2, 'B'),
      magazine(3, 'C'),
      magazine(4, 'D'),
    ])
    const { container } = await renderHero()

    // n=5, W=3 → 위치 수 = 3
    await waitFor(() => {
      expect(container.querySelectorAll('.hero__dot').length).toBe(3)
    })
  })

  it('다음 버튼은 밴드를 한 칸 오른쪽으로 옮기고, 마지막에서 처음으로 순환한다', async () => {
    setViewport(1280)
    mockList.mockResolvedValue([
      magazine(1, 'A'),
      magazine(2, 'B'),
      magazine(3, 'C'),
      magazine(4, 'D'),
    ])
    const { container, getByLabelText } = await renderHero()
    await waitFor(() => expect(bandIndices(container)).toEqual([0, 1, 2]))

    const nextBtn = getByLabelText('다음 슬라이드')
    await fireEvent.click(nextBtn)
    expect(bandIndices(container)).toEqual([1, 2, 3])

    await fireEvent.click(nextBtn) // 마지막 위치(밴드 = 마지막 3장)
    expect(bandIndices(container)).toEqual([2, 3, 4])

    await fireEvent.click(nextBtn) // 순환 → 처음으로
    expect(bandIndices(container)).toEqual([0, 1, 2])
  })

  it('모바일(<640)에서는 밴드가 1장이고 도트는 카드 수만큼이다', async () => {
    setViewport(375)
    mockList.mockResolvedValue([
      magazine(1, 'A'),
      magazine(2, 'B'),
      magazine(3, 'C'),
      magazine(4, 'D'),
    ])
    const { container } = await renderHero()

    await waitFor(() => {
      expect(bandIndices(container)).toEqual([0]) // W=1
      expect(container.querySelectorAll('.hero__dot').length).toBe(5) // n=5
    })
  })
})
