import apiClient from './client'
import type { Product, Review } from '@/types'

export interface CreateProductRequest {
  name: string
  description: string
  price: number
  imageUrl?: string
  keyColor?: string
  subColor?: string
  styles?: string[]
}

export async function getProducts(): Promise<Product[]> {
  const { data } = await apiClient.get<Product[]>('/products')
  return data
}

export async function getProduct(id: number): Promise<Product> {
  const { data } = await apiClient.get<Product>(`/products/${id}`)
  return data
}

export async function createProduct(request: CreateProductRequest): Promise<Product> {
  const { data } = await apiClient.post<Product>('/products', request)
  return data
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

export async function createReview(
  productId: number,
  request: { rating: number; content: string }
): Promise<Review> {
  const { data } = await apiClient.post<Review>(`/products/${productId}/reviews`, request)
  return data
}
