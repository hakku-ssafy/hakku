import apiClient from './client'
import type { Post, Comment } from '@/types'

function normalizePost(raw: Post): Post {
  return {
    ...raw,
    authorNickname: raw.authorNickname ?? '알 수 없음',
    likeCount: raw.likeCount ?? 0,
    commentCount: raw.commentCount ?? 0,
    liked: raw.liked ?? false,
  }
}

function normalizeComment(raw: Comment): Comment {
  return {
    ...raw,
    authorNickname: raw.authorNickname ?? '알 수 없음',
  }
}

export interface CreatePostRequest {
  title: string
  content: string
}

export async function getPosts(): Promise<Post[]> {
  const { data } = await apiClient.get<Post[]>('/posts')
  return data.map(normalizePost)
}

export async function getPost(id: number): Promise<Post> {
  const { data } = await apiClient.get<Post>(`/posts/${id}`)
  return normalizePost(data)
}

export async function createPost(request: CreatePostRequest): Promise<Post> {
  const { data } = await apiClient.post<Post>('/posts', request)
  return normalizePost(data)
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
  return data.map(normalizeComment)
}

export async function createComment(postId: number, content: string): Promise<Comment> {
  const { data } = await apiClient.post<Comment>(`/posts/${postId}/comments`, { content })
  return normalizeComment(data)
}

export async function updateComment(commentId: number, content: string): Promise<Comment> {
  const { data } = await apiClient.put<Comment>(`/comments/${commentId}`, { content })
  return data
}

export async function deleteComment(commentId: number): Promise<void> {
  await apiClient.delete(`/comments/${commentId}`)
}
