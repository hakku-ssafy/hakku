import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/vue'
import { createRouter, createMemoryHistory, type Router } from 'vue-router'
import ProductCard from '../ProductCard.vue'
import type { Product } from '@/types'

const blank = { template: '<div />' }

function makeRouter(): Router {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: blank },
      { path: '/products/:id', component: blank },
    ],
  })
}

function product(extra: Partial<Product> = {}): Product {
  return {
    id: 1,
    name: '다꾸 키링',
    description: '',
    price: 4900,
    category: '키링',
    imageUrl: null,
    purchaseUrl: null,
    keyColor: null,
    subColor: null,
    colors: [],
    styles: [],
    sellerId: 9,
    sellerNickname: '판매왕',
    ...extra,
  }
}

describe('ProductCard 판매자 닉네임', () => {
  it('판매자 닉네임이 있으면 카드에 노출한다', () => {
    render(ProductCard, { props: { product: product() }, global: { plugins: [makeRouter()] } })
    expect(screen.getByText(/판매왕/)).toBeInTheDocument()
  })

  it('판매자 닉네임이 없으면 노출하지 않는다', () => {
    render(ProductCard, {
      props: { product: product({ sellerNickname: null }) },
      global: { plugins: [makeRouter()] },
    })
    expect(screen.queryByText(/판매왕/)).not.toBeInTheDocument()
  })

  it('스택 모드(overlay=false)에서도 판매자 닉네임을 노출한다', () => {
    render(ProductCard, {
      props: { product: product(), overlay: false },
      global: { plugins: [makeRouter()] },
    })
    expect(screen.getByText(/판매왕/)).toBeInTheDocument()
  })
})
