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
})
