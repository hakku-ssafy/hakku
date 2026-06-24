import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen } from '@testing-library/vue'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory, type Router } from 'vue-router'
import MyPageView from '../MyPageView.vue'
import { useAuthStore } from '@/stores/auth'
import * as authApi from '@/api/auth'
import * as ordersApi from '@/api/orders'

vi.mock('@/api/auth')
vi.mock('@/api/orders')

const blank = { template: '<div />' }

const user = {
  id: 1,
  email: 'u@test.com',
  nickname: '테스터',
  role: 'NORMAL' as const,
  personalColor: null,
  profileImageUrl: null,
  diagnosisImageUrl: null,
  preferredStyles: [],
  preferredColors: [],
  diagnosisStatus: 'NONE' as const,
  onboardingCompleted: true,
}

function makeRouter(): Router {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: blank },
      { path: '/my', component: MyPageView },
      { path: '/diagnosis', component: blank },
    ],
  })
}

describe('MyPageView 주문 내역 탭', () => {
  let router: Router

  beforeEach(async () => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.mocked(authApi.getMe).mockResolvedValue(user)
    vi.mocked(ordersApi.getMyOrders).mockResolvedValue([
      {
        id: 1,
        status: 'PAID',
        recipientName: '홍길동',
        phone: '010-1234-5678',
        postalCode: '06236',
        address1: '서울 강남구 테헤란로 1',
        address2: '101동',
        totalAmount: 9800,
        createdAt: 1_700_000_000_000,
        items: [
          { productId: 10, productName: '다꾸 키링', price: 4900, quantity: 2, lineTotal: 9800 },
        ],
      },
    ])
    router = makeRouter()
    await router.push('/my?tab=orders')
    await router.isReady()
    useAuthStore().token = 'test-token'
  })

  it('tab=orders 로 진입하면 주문 내역이 표시된다', async () => {
    render(MyPageView, { global: { plugins: [router] } })

    expect(await screen.findByText('다꾸 키링')).toBeInTheDocument()
    expect(screen.getAllByText(/9,800/).length).toBeGreaterThan(0)
    expect(ordersApi.getMyOrders).toHaveBeenCalled()
  })

  it('주문 내역 탭 버튼이 존재한다', async () => {
    render(MyPageView, { global: { plugins: [router] } })
    await screen.findByText('다꾸 키링')

    expect(screen.getByRole('button', { name: '주문 내역' })).toBeInTheDocument()
  })
})
