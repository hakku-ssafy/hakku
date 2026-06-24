import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/vue'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory, type Router } from 'vue-router'
import NotificationView from '../NotificationView.vue'
import apiClient from '@/api/client'

vi.mock('@/api/client', () => ({
  default: { get: vi.fn() },
}))

const blank = { template: '<div />' }

function makeRouter(): Router {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/notifications', component: NotificationView },
      { path: '/my', component: blank },
      { path: '/community/:id', component: blank },
      { path: '/users/:id', component: blank },
      { path: '/products/:id', component: blank },
    ],
  })
}

describe('NotificationView ORDER 알림', () => {
  let router: Router

  beforeEach(async () => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.mocked(apiClient.get).mockResolvedValue({
      data: [
        {
          type: 'ORDER',
          actorId: null,
          actorNickname: null,
          postId: null,
          postTitlePreview: null,
          productId: null,
          message: '결제가 완료되었어요. 주문 내역을 확인해보세요!',
          createdAt: 1_700_000_000_000,
        },
      ],
    })
    router = makeRouter()
    await router.push('/notifications')
    await router.isReady()
  })

  it('구매 완료 알림을 누르면 마이페이지 주문 내역으로 이동한다', async () => {
    render(NotificationView, { global: { plugins: [router] } })

    const row = await screen.findByRole('button', { name: /결제가 완료/ })
    await fireEvent.click(row)

    await waitFor(() => {
      expect(router.currentRoute.value.path).toBe('/my')
    })
    expect(router.currentRoute.value.query.tab).toBe('orders')
  })
})
