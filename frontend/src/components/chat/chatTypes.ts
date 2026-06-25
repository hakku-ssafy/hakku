/** 챗봇 답변에 임베드되는 상품 카드용 슬림 타입. 백엔드 recommend_products 결과 형태와 일치. */
export interface ChatProduct {
  id: number
  name: string
  price: number
  imageUrl: string | null
}
