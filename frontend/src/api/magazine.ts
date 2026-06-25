import apiClient from './client'
import type { Magazine, MagazineInput } from '@/types'

/** 메인 캐러셀/목록용 — 발행된 매거진(정렬 순서대로). 공개. */
export async function listActiveMagazines(): Promise<Magazine[]> {
  const { data } = await apiClient.get<Magazine[]>('/magazines')
  return data
}

/** 매거진 상세용 — 단건. 공개. */
export async function getMagazine(id: number): Promise<Magazine> {
  const { data } = await apiClient.get<Magazine>(`/magazines/${id}`)
  return data
}

/** 어드민 — 미발행 포함 전체 매거진. */
export async function listAllMagazines(): Promise<Magazine[]> {
  const { data } = await apiClient.get<Magazine[]>('/admin/magazines')
  return data
}

/** 어드민 — 매거진 발행. */
export async function createMagazine(input: MagazineInput): Promise<Magazine> {
  const { data } = await apiClient.post<Magazine>('/admin/magazines', input)
  return data
}

/** 어드민 — 매거진 수정. */
export async function updateMagazine(id: number, input: MagazineInput): Promise<Magazine> {
  const { data } = await apiClient.put<Magazine>(`/admin/magazines/${id}`, input)
  return data
}

/** 어드민 — 매거진 삭제. */
export async function deleteMagazine(id: number): Promise<void> {
  await apiClient.delete(`/admin/magazines/${id}`)
}
