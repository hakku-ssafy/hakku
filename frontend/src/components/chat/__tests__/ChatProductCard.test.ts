import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/vue'
import { createRouter, createMemoryHistory } from 'vue-router'
import ChatProductCard from '../ChatProductCard.vue'
import type { ChatProduct } from '../chatTypes'

const product: ChatProduct = { id: 9, name: '그립톡', price: 5900, imageUrl: '/img/9.jpg' }

function renderCard(p: ChatProduct = product) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/products/:id', component: { template: '<div />' } },
    ],
  })
  return render(ChatProductCard, { props: { product: p }, global: { plugins: [router] } })
}

describe('ChatProductCard', () => {
  it('상품명과 가격을 보여준다', () => {
    renderCard()
    expect(screen.getByText('그립톡')).toBeInTheDocument()
    expect(screen.getByText(/5,900/)).toBeInTheDocument()
  })

  it('상품 상세(/products/:id)로 가는 링크를 건다', () => {
    renderCard()
    expect(screen.getByRole('link')).toHaveAttribute('href', '/products/9')
  })

  it('이미지가 없으면 대체 글리프를 보여준다(깨진 이미지 방지)', () => {
    renderCard({ ...product, imageUrl: null })
    expect(screen.queryByRole('img')).toBeNull()
  })
})
