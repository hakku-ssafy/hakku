import apiClient from './client'
import type { FollowToggle, UserSummary } from '@/types'

export async function toggleFollow(userId: number): Promise<FollowToggle> {
  const { data } = await apiClient.post<FollowToggle>(`/users/${userId}/follow`)
  return data
}

export async function getFollowers(userId: number): Promise<UserSummary[]> {
  const { data } = await apiClient.get<UserSummary[]>(`/users/${userId}/followers`)
  return data
}

export async function getFollowing(userId: number): Promise<UserSummary[]> {
  const { data } = await apiClient.get<UserSummary[]>(`/users/${userId}/following`)
  return data
}
