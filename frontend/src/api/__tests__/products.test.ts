import { describe, it, expect, vi, beforeEach } from 'vitest'
import apiClient from '../client'
import { getProduct } from '../products'
import type { Product } from '@/types'

vi.mock('../client', () => ({
  default: { get: vi.fn() },
}))

function rawProduct(extra: Partial<Product> = {}): Product {
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

describe('getProduct 정규화', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('판매자 닉네임을 그대로 보존한다', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: rawProduct() })

    const result = await getProduct(1)

    expect(result.sellerNickname).toBe('판매왕')
  })

  it('판매자 닉네임이 없으면 null 로 정규화한다', async () => {
    const { sellerNickname, ...withoutNickname } = rawProduct()
    void sellerNickname
    vi.mocked(apiClient.get).mockResolvedValue({ data: withoutNickname })

    const result = await getProduct(1)

    expect(result.sellerNickname).toBeNull()
  })
})
