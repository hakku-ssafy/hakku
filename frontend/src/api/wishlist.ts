import apiClient from './client'
import type { WishlistItem, WishlistStatus, WishlistLikeResult } from '@/types'

export async function getWishlistStatus(productId: number): Promise<WishlistStatus> {
  const { data } = await apiClient.get<WishlistStatus>(`/products/${productId}/wishlist`)
  return data
}

export async function toggleWishlist(productId: number): Promise<WishlistStatus> {
  const { data } = await apiClient.post<WishlistStatus>(`/products/${productId}/wishlist`)
  return data
}

export async function getUserWishlist(userId: number): Promise<WishlistItem[]> {
  const { data } = await apiClient.get<WishlistItem[]>(`/users/${userId}/wishlist`)
  return data
}

export async function toggleWishlistLike(wishlistId: number): Promise<WishlistLikeResult> {
  const { data } = await apiClient.post<WishlistLikeResult>(`/wishlist/${wishlistId}/likes`)
  return data
}
