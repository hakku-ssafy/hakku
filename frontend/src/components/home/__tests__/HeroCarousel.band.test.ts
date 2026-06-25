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

const FOUR_MAGS = [magazine(1, 'A'), magazine(2, 'B'), magazine(3, 'C'), magazine(4, 'D')]
// 리드 1 + 매거진 4 = 논리 카드 5장 (logicalIdx 0..4)

/** in-band(밝은) 슬라이드들의 논리 인덱스(data-idx). */
function bandIdx(container: Element): number[] {
  return Array.from(container.querySelectorAll('.hero__slide--in-band')).map((el) =>
    Number(el.getAttribute('data-idx')),
  )
}

/** 첫 in-band 슬라이드 바로 왼쪽 슬라이드(왼쪽 dim 채움 카드). */
function leftFillSlide(container: Element): Element | null {
  const slides = Array.from(container.querySelectorAll('.hero__slide'))
  const firstBand = slides.findIndex((el) => el.classList.contains('hero__slide--in-band'))
  return firstBand > 0 ? slides[firstBand - 1] : null
}

describe('HeroCarousel 중앙 밴드 · 양옆 dim 순환 채움', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('데스크탑(≥1024): 시작 시 밝은 중앙 3장 = 논리 0·1·2', async () => {
    setViewport(1280)
    mockList.mockResolvedValue(FOUR_MAGS)
    const { container } = await renderHero()

    await waitFor(() => {
      expect(bandIdx(container)).toEqual([0, 1, 2])
    })
  })

  it('시작 시 밴드 왼쪽은 마지막 카드(논리 4)가 dim 클론으로 채워진다(빈 곳 없음)', async () => {
    setViewport(1280)
    mockList.mockResolvedValue(FOUR_MAGS)
    const { container } = await renderHero()

    await waitFor(() => expect(bandIdx(container)).toEqual([0, 1, 2]))
    const left = leftFillSlide(container)
    expect(left).not.toBeNull()
    expect(left?.getAttribute('data-idx')).toBe('4') // 순환: 첫 카드 왼쪽 = 마지막 카드
    expect(left?.getAttribute('data-clone')).toBe('true')
    expect(left?.classList.contains('hero__slide--in-band')).toBe(false) // dim
  })

  it('도트는 이동 위치 수(n-W+1 = 3)만큼 렌더한다', async () => {
    setViewport(1280)
    mockList.mockResolvedValue(FOUR_MAGS)
    const { container } = await renderHero()

    await waitFor(() => {
      expect(container.querySelectorAll('.hero__dot').length).toBe(3)
    })
  })

  it('다음 버튼은 밝은 밴드를 한 칸 옮기고, 마지막에서 처음으로 순환한다', async () => {
    setViewport(1280)
    mockList.mockResolvedValue(FOUR_MAGS)
    const { container, getByLabelText } = await renderHero()
    await waitFor(() => expect(bandIdx(container)).toEqual([0, 1, 2]))

    const nextBtn = getByLabelText('다음 슬라이드')
    await fireEvent.click(nextBtn)
    expect(bandIdx(container)).toEqual([1, 2, 3])

    await fireEvent.click(nextBtn) // 마지막 위치(밝은 = 마지막 3장)
    expect(bandIdx(container)).toEqual([2, 3, 4])

    await fireEvent.click(nextBtn) // 순환 → 처음으로
    expect(bandIdx(container)).toEqual([0, 1, 2])
  })

  it('양옆 채움 클론은 aria-hidden 이라 접근성 트리/포커스에서 빠진다', async () => {
    setViewport(1280)
    mockList.mockResolvedValue(FOUR_MAGS)
    const { container } = await renderHero()
    await waitFor(() => expect(bandIdx(container)).toEqual([0, 1, 2]))

    const slides = Array.from(container.querySelectorAll('.hero__slide'))
    const clones = slides.filter((s) => s.getAttribute('data-clone') === 'true')
    const reals = slides.filter((s) => s.getAttribute('data-clone') === 'false')

    expect(clones.length).toBeGreaterThan(0)
    // 클론은 모두 aria-hidden(중복 링크가 접근성 트리에서 제외됨)
    expect(clones.every((s) => s.getAttribute('aria-hidden') === 'true')).toBe(true)
    // 실제 카드는 숨김 처리되지 않음
    expect(reals.every((s) => s.getAttribute('aria-hidden') === null)).toBe(true)
  })

  it('모바일(<640): 밝은 밴드 1장, 왼쪽은 마지막 카드가 dim 으로 채워진다', async () => {
    setViewport(375)
    mockList.mockResolvedValue(FOUR_MAGS)
    const { container } = await renderHero()

    await waitFor(() => {
      expect(bandIdx(container)).toEqual([0]) // W=1
      expect(container.querySelectorAll('.hero__dot').length).toBe(5) // n=5
    })
    expect(leftFillSlide(container)?.getAttribute('data-idx')).toBe('4')
  })
})
