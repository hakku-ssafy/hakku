import apiClient from './client'
import { normalizeProduct, type Product, type Review } from '@/types'

export interface CreateProductRequest {
  name: string
  description: string
  price: number
  category?: string
  imageUrl?: string
  keyColor?: string
  subColor?: string
  colors?: string[]
  styles?: string[]
  purchaseUrl?: string
}

export async function getProducts(): Promise<Product[]> {
  const { data } = await apiClient.get<Product[]>('/products')
  return data.map(normalizeProduct)
}

export async function getProduct(id: number): Promise<Product> {
  const { data } = await apiClient.get<Product>(`/products/${id}`)
  return normalizeProduct(data)
}

export async function createProduct(request: CreateProductRequest): Promise<Product> {
  const { data } = await apiClient.post<Product>('/products', request)
  return normalizeProduct(data)
}

export async function updateProduct(id: number, request: Partial<CreateProductRequest>): Promise<Product> {
  const { data } = await apiClient.put<Product>(`/products/${id}`, request)
  return data
}

export async function deleteProduct(id: number): Promise<void> {
  await apiClient.delete(`/products/${id}`)
}

export async function getReviews(productId: number): Promise<Review[]> {
  const { data } = await apiClient.get<Review[]>(`/products/${productId}/reviews`)
  return data
}

export async function createReview(productId: number, request: { rating: number; content: string }): Promise<Review> {
  const { data } = await apiClient.post<Review>(`/products/${productId}/reviews`, request)
  return data
}

export async function getUserReviews(userId: number): Promise<Review[]> {
  const { data } = await apiClient.get<Review[]>(`/users/${userId}/reviews`)
  return data
}

export async function updateReview(reviewId: number, request: { rating: number; content: string }): Promise<Review> {
  const { data } = await apiClient.put<Review>(`/reviews/${reviewId}`, request)
  return data
}

export async function deleteReview(reviewId: number): Promise<void> {
  await apiClient.delete(`/reviews/${reviewId}`)
}
