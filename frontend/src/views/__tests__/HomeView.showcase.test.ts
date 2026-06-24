import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen } from '@testing-library/vue'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory, type Router } from 'vue-router'
import HomeView from '../HomeView.vue'
import * as postsApi from '@/api/posts'
import * as productsApi from '@/api/products'
import type { Post } from '@/types'

vi.mock('@/api/posts')
vi.mock('@/api/products')

const blank = { template: '<div />' }

function makeRouter(): Router {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: blank },
      { path: '/community', component: blank },
      { path: '/community/:id', component: blank },
      { path: '/products', component: blank },
      { path: '/diagnosis', component: blank },
    ],
  })
}

function showcasePost(id: number, title: string): Post {
  return {
    id, title, content: '꾸민 학생증', board: 'STUDENT_ID', imageUrl: `https://img/${id}.png`,
    authorId: 1, authorNickname: '학꾸러', likeCount: 2, commentCount: 0, liked: false,
    relatedProducts: [], createdAt: '2026-06-24T00:00:00Z',
  }
}

describe('HomeView 학생증 자랑 격자', () => {
  let router: Router

  beforeEach(async () => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.mocked(postsApi.getPosts).mockImplementation((board?: 'GENERAL' | 'STUDENT_ID') =>
      Promise.resolve(board === 'STUDENT_ID' ? [showcasePost(10, '가을 다꾸 학생증'), showcasePost(11, '겨울 쿨톤 학생증')] : []),
    )
    vi.mocked(productsApi.getProducts).mockResolvedValue([])
    router = makeRouter()
    await router.push('/')
    await router.isReady()
  })

  it('학생증 자랑 게시글을 이미지 격자로 노출하고 각 타일은 /community/:id 로 이동한다', async () => {
    render(HomeView, { global: { plugins: [router], stubs: { HeroCarousel: true, AppModal: true } } })

    const tile = await screen.findByRole('link', { name: /가을 다꾸 학생증/ })
    expect(tile.getAttribute('href')).toBe('/community/10')
    expect(postsApi.getPosts).toHaveBeenCalledWith('STUDENT_ID')
  })
})
