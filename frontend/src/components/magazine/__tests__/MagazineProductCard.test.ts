import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/vue'
import { createRouter, createMemoryHistory } from 'vue-router'
import MagazineProductCard from '../MagazineProductCard.vue'
import type { Product } from '@/types'

const product: Product = {
  id: 7,
  name: '데코 스티커',
  description: '귀여운 스티커',
  price: 4500,
  category: '꾸미기 스티커',
  imageUrl: '/img/7.jpg',
  purchaseUrl: null,
  keyColor: null,
  subColor: null,
  colors: [],
  styles: [],
  sellerId: 1,
}

function renderCard(p: Product = product) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/products/:id', component: { template: '<div />' } },
    ],
  })
  return render(MagazineProductCard, { props: { product: p }, global: { plugins: [router] } })
}

describe('MagazineProductCard', () => {
  it('상품명과 가격을 보여준다', () => {
    renderCard()
    expect(screen.getByText('데코 스티커')).toBeInTheDocument()
    expect(screen.getByText(/4,500/)).toBeInTheDocument()
  })

  it('상품 상세(/products/:id)로 가는 링크를 건다', () => {
    renderCard()
    expect(screen.getByRole('link')).toHaveAttribute('href', '/products/7')
  })

  it('이미지가 없으면 대체 글리프를 보여준다(깨진 이미지 방지)', () => {
    const noImage: Product = { ...product, imageUrl: null }
    renderCard(noImage)
    expect(screen.queryByRole('img')).toBeNull()
  })
})
