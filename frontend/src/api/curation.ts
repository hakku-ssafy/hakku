import apiClient from './client'
import type { CurationCard, CurationCardInput } from '@/types'

/** 메인 캐러셀용 — 활성 큐레이션 카드(정렬 순서대로). 공개. */
export async function listActiveCurationCards(): Promise<CurationCard[]> {
  const { data } = await apiClient.get<CurationCard[]>('/curation-cards')
  return data
}

/** 매거진 상세용 — 카드 단건. 공개. */
export async function getCurationCard(id: number): Promise<CurationCard> {
  const { data } = await apiClient.get<CurationCard>(`/curation-cards/${id}`)
  return data
}

/** 어드민 — 비활성 포함 전체 카드. */
export async function listAllCurationCards(): Promise<CurationCard[]> {
  const { data } = await apiClient.get<CurationCard[]>('/admin/curation-cards')
  return data
}

/** 어드민 — 카드 생성. */
export async function createCurationCard(input: CurationCardInput): Promise<CurationCard> {
  const { data } = await apiClient.post<CurationCard>('/admin/curation-cards', input)
  return data
}

/** 어드민 — 카드 수정. */
export async function updateCurationCard(
  id: number,
  input: CurationCardInput,
): Promise<CurationCard> {
  const { data } = await apiClient.put<CurationCard>(`/admin/curation-cards/${id}`, input)
  return data
}

/** 어드민 — 카드 삭제. */
export async function deleteCurationCard(id: number): Promise<void> {
  await apiClient.delete(`/admin/curation-cards/${id}`)
}
