import apiClient from './client'
import type { PublicProfile, User } from '@/types'

export async function getPublicProfile(id: number): Promise<PublicProfile> {
  const { data } = await apiClient.get<PublicProfile>(`/users/${id}`)
  return data
}

export async function updatePreferredColors(preferredColors: string[]): Promise<User> {
  const { data } = await apiClient.patch<User>('/users/me/preferred-colors', { preferredColors })
  return data
}
