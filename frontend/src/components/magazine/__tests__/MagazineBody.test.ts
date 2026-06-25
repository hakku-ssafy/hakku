import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/vue'
import { createRouter, createMemoryHistory } from 'vue-router'
import MagazineBody from '../MagazineBody.vue'
import type { Product } from '@/types'

const product: Product = {
  id: 7,
  name: '데코 스티커',
  description: '',
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

function renderBody(content: string, productsById: Record<number, Product> = {}) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/products/:id', component: { template: '<div />' } },
    ],
  })
  return render(MagazineBody, { props: { content, productsById }, global: { plugins: [router] } })
}

describe('MagazineBody', () => {
  it('마크다운 본문을 HTML로 렌더링한다', () => {
    renderBody('# 큰제목\n\n**굵게** 글')
    expect(screen.getByRole('heading', { name: '큰제목' })).toBeInTheDocument()
    expect(document.querySelector('strong')?.textContent).toBe('굵게')
  })

  it('단독 상품 링크는 가로 상품 카드로 임베드한다(앞뒤 글 유지)', () => {
    renderBody('소개 문단\n\n/products/7', { 7: product })
    expect(screen.getByText('소개 문단')).toBeInTheDocument()
    expect(screen.getByText('데코 스티커')).toBeInTheDocument()
  })

  it('상품 데이터가 아직 없으면 상세 링크로 폴백한다', () => {
    renderBody('/products/7', {})
    expect(screen.getByRole('link')).toHaveAttribute('href', '/products/7')
  })
})
