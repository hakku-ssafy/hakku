import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/vue'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory, type Router } from 'vue-router'
import OrderFormView from '../OrderFormView.vue'
import { useAuthStore } from '@/stores/auth'
import * as ordersApi from '@/api/orders'

vi.mock('@/api/orders')

const blank = { template: '<div />' }

function makeRouter(): Router {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/order/new', component: OrderFormView },
      { path: '/payments/checkout', component: blank },
      { path: '/cart', component: blank },
      { path: '/login', component: blank },
    ],
  })
}

describe('OrderFormView', () => {
  let router: Router

  beforeEach(async () => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.mocked(ordersApi.createOrder).mockResolvedValue({
      id: 55,
      status: 'CREATED',
      recipientName: '홍길동',
      phone: '010-1234-5678',
      postalCode: '06236',
      address1: '서울 강남구 테헤란로 1',
      address2: '101동 1001호',
      totalAmount: 9800,
      createdAt: 0,
      items: [],
    })
    router = makeRouter()
    await router.push('/order/new?amount=9800&count=2')
    await router.isReady()
    useAuthStore().token = 'test-token'
  })

  it('자동채움 버튼을 누르면 배송지 입력칸이 테스트용 값으로 채워진다', async () => {
    render(OrderFormView, { global: { plugins: [router] } })

    const nameInput = screen.getByLabelText('받는 분') as HTMLInputElement
    expect(nameInput.value).toBe('')

    await fireEvent.click(screen.getByRole('button', { name: /자동채움/ }))

    expect((screen.getByLabelText('받는 분') as HTMLInputElement).value).toBe('홍길동')
    expect((screen.getByLabelText('연락처') as HTMLInputElement).value).not.toBe('')
    expect((screen.getByLabelText('주소') as HTMLInputElement).value).not.toBe('')
  })

  it('주문하기 제출 시 createOrder 호출 후 ORDER 결제로 이동한다', async () => {
    render(OrderFormView, { global: { plugins: [router] } })

    await fireEvent.click(screen.getByRole('button', { name: /자동채움/ }))
    await fireEvent.click(screen.getByRole('button', { name: /주문하기/ }))

    await waitFor(() => {
      expect(ordersApi.createOrder).toHaveBeenCalled()
    })
    const arg = vi.mocked(ordersApi.createOrder).mock.calls[0][0]
    expect(arg.recipientName).toBe('홍길동')

    await waitFor(() => {
      expect(router.currentRoute.value.path).toBe('/payments/checkout')
    })
    expect(router.currentRoute.value.query.refType).toBe('ORDER')
    expect(router.currentRoute.value.query.refId).toBe('55')
  })

  it('필수 배송지를 비우고 제출하면 createOrder 를 호출하지 않는다', async () => {
    render(OrderFormView, { global: { plugins: [router] } })

    await fireEvent.click(screen.getByRole('button', { name: /주문하기/ }))

    expect(ordersApi.createOrder).not.toHaveBeenCalled()
  })
})
