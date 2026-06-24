import apiClient from './client'

/** prepare 응답: 위젯에 넣을 서버 발급 주문번호와 확정 금액. */
export interface TossPrepareResult {
  orderId: string
  amount: number
  currency: string
}

/** confirm 응답: 정산 결과 상태(APPROVED/FAILED). */
export interface TossConfirmResult {
  paymentId: number
  status: string
  amount: number
}

export interface TossPrepareInput {
  referenceType: string
  referenceId: string
  amount: number
  currency?: string
}

/** 위젯 표시 전 결제 의도를 서버에 선등록하고 orderId 를 받는다(JWT 필요). */
export async function prepareTossPayment(input: TossPrepareInput): Promise<TossPrepareResult> {
  const { data } = await apiClient.post<TossPrepareResult>('/payments/toss/prepare', input)
  return data
}

/** 토스 successUrl 이후 결제를 최종 승인한다(JWT 필요). */
export async function confirmTossPayment(input: {
  paymentKey: string
  orderId: string
  amount: number
}): Promise<TossConfirmResult> {
  const { data } = await apiClient.post<TossConfirmResult>('/payments/toss/confirm', input)
  return data
}
