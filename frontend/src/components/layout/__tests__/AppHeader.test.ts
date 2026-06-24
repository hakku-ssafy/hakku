import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/vue'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory, type Router } from 'vue-router'
import AppHeader from '../AppHeader.vue'
import { useAuthStore } from '@/stores/auth'

const blank = { template: '<div />' }

function makeRouter(): Router {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: blank },
      { path: '/products', component: blank },
      { path: '/community', component: blank },
      { path: '/my', component: blank },
      { path: '/login', component: blank },
      { path: '/signup', component: blank },
      { path: '/cart', component: blank },
      { path: '/notifications', component: blank },
    ],
  })
}

describe('AppHeader', () => {
  let router: Router

  beforeEach(async () => {
    setActivePinia(createPinia())
    router = makeRouter()
    await router.push('/')
    await router.isReady()
  })

  it('검색어를 입력해 제출하면 /products?q= 로 이동한다', async () => {
    render(AppHeader, { global: { plugins: [router] } })

    const input = screen.getByRole('searchbox', { name: /상품 검색/ })
    await fireEvent.update(input, '키링')
    await fireEvent.submit(screen.getByRole('search'))

    await waitFor(() => {
      expect(router.currentRoute.value.path).toBe('/products')
    })
    expect(router.currentRoute.value.query.q).toBe('키링')
  })

  it('공백만 입력하면 이동하지 않는다', async () => {
    render(AppHeader, { global: { plugins: [router] } })

    const input = screen.getByRole('searchbox', { name: /상품 검색/ })
    await fireEvent.update(input, '   ')
    await fireEvent.submit(screen.getByRole('search'))
    await router.isReady()

    expect(router.currentRoute.value.path).toBe('/')
  })

  it('로그인 상태면 진단 여부와 무관하게 마이페이지 링크가 보인다', () => {
    const auth = useAuthStore()
    auth.token = 'test-token' // 진단 전(user=null)이라 퍼스널컬러 배지는 없음
    render(AppHeader, { global: { plugins: [router] } })

    const myLink = screen.getByRole('link', { name: /마이페이지/ })
    expect(myLink).toBeInTheDocument()
    expect(myLink.getAttribute('href')).toContain('/my')
  })

  it('비로그인 상태면 마이페이지 링크가 없다', () => {
    render(AppHeader, { global: { plugins: [router] } })
    expect(screen.queryByRole('link', { name: /마이페이지/ })).not.toBeInTheDocument()
  })

  it('로그인 상태여도 찜 목록(하트) 링크는 노출하지 않는다 — 마이페이지와 기능 중복', () => {
    const auth = useAuthStore()
    auth.token = 'test-token'
    render(AppHeader, { global: { plugins: [router] } })

    expect(screen.queryByRole('link', { name: /찜 목록/ })).not.toBeInTheDocument()
  })

  it('로고는 hakku 이미지이며 클릭하면 홈으로 이동한다', () => {
    render(AppHeader, { global: { plugins: [router] } })

    const homeLink = screen.getByRole('link', { name: '학꾸 홈' })
    expect(homeLink.getAttribute('href')).toBe('/')

    const img = homeLink.querySelector('img')
    expect(img).not.toBeNull()
    expect(img?.getAttribute('src')).toContain('hakku.png')
  })
})
