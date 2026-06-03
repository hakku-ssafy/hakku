import apiClient from './client'
import type { Post, Comment } from '@/types'

export interface CreatePostRequest {
  title: string
  content: string
}

export async function getPosts(): Promise<Post[]> {
  const { data } = await apiClient.get<Post[]>('/posts')
  return data
}

export async function getPost(id: number): Promise<Post> {
  const { data } = await apiClient.get<Post>(`/posts/${id}`)
  return data
}

export async function createPost(request: CreatePostRequest): Promise<Post> {
  const { data } = await apiClient.post<Post>('/posts', request)
  return data
}

export async function updatePost(id: number, request: CreatePostRequest): Promise<Post> {
  const { data } = await apiClient.put<Post>(`/posts/${id}`, request)
  return data
}

export async function deletePost(id: number): Promise<void> {
  await apiClient.delete(`/posts/${id}`)
}

export async function toggleLike(postId: number): Promise<{ liked: boolean; likeCount: number }> {
  const { data } = await apiClient.post<{ liked: boolean; likeCount: number }>(
    `/posts/${postId}/likes`
  )
  return data
}

export async function getComments(postId: number): Promise<Comment[]> {
  const { data } = await apiClient.get<Comment[]>(`/posts/${postId}/comments`)
  return data
}

export async function createComment(postId: number, content: string): Promise<Comment> {
  const { data } = await apiClient.post<Comment>(`/posts/${postId}/comments`, { content })
  return data
}

export async function updateComment(commentId: number, content: string): Promise<Comment> {
  const { data } = await apiClient.put<Comment>(`/comments/${commentId}`, { content })
  return data
}

export async function deleteComment(commentId: number): Promise<void> {
  await apiClient.delete(`/comments/${commentId}`)
}
