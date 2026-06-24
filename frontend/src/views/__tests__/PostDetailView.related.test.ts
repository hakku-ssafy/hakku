import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/vue'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory, type Router } from 'vue-router'
import PostDetailView from '../PostDetailView.vue'
import * as postsApi from '@/api/posts'
import type { Post } from '@/types'

vi.mock('@/api/posts')

const blank = { template: '<div />' }

function makeRouter(): Router {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: blank },
      { path: '/community', component: blank },
      { path: '/community/:id', component: blank },
      { path: '/users/:id', component: blank },
      { path: '/products/:id', component: blank },
      { path: '/login', component: blank },
    ],
  })
}

const post: Post = {
  id: 5,
  title: '가을 웜톤 다꾸 학생증',
  content: '키링이랑 스티커로 꾸몄어요',
  board: 'STUDENT_ID',
  imageUrl: 'https://img/card.png',
  authorId: 2,
  authorNickname: '학꾸러',
  likeCount: 3,
  commentCount: 0,
  liked: false,
  createdAt: '2026-06-24T00:00:00Z',
  relatedProducts: [
    { id: 7, name: '다꾸 키링', imageUrl: 'https://img/keyring.png', price: 4900 },
    { id: 8, name: '꾸미기 스티커', imageUrl: null, price: 2500 },
  ],
}

describe('PostDetailView 연관 상품', () => {
  let router: Router

  beforeEach(async () => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.mocked(postsApi.getPost).mockResolvedValue(post)
    vi.mocked(postsApi.getComments).mockResolvedValue([])
    router = makeRouter()
    await router.push('/community/5')
    await router.isReady()
  })

  it('게시물의 연관 상품을 노출하고, 각 상품은 상품 페이지(/products/:id)로 이동한다', async () => {
    render(PostDetailView, { global: { plugins: [router] } })

    const keyringLink = await screen.findByRole('link', { name: /다꾸 키링/ })
    expect(keyringLink.getAttribute('href')).toBe('/products/7')

    const stickerLink = screen.getByRole('link', { name: /꾸미기 스티커/ })
    expect(stickerLink.getAttribute('href')).toBe('/products/8')
  })

  it('연관 상품이 없으면 관련 상품 섹션을 노출하지 않는다', async () => {
    vi.mocked(postsApi.getPost).mockResolvedValue({ ...post, relatedProducts: [] })
    render(PostDetailView, { global: { plugins: [router] } })

    await waitFor(() => expect(screen.getByText(post.title)).toBeInTheDocument())
    expect(screen.queryByText('관련 상품')).not.toBeInTheDocument()
  })
})
