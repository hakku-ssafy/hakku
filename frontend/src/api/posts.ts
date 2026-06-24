import apiClient from './client'
import type { Post, PostBoard, Comment } from '@/types'

function normalizePost(raw: Post): Post {
  return {
    ...raw,
    board: raw.board ?? 'GENERAL',
    imageUrl: raw.imageUrl ?? null,
    authorNickname: raw.authorNickname ?? '알 수 없음',
    likeCount: raw.likeCount ?? 0,
    commentCount: raw.commentCount ?? 0,
    liked: raw.liked ?? false,
    relatedProducts: raw.relatedProducts ?? [],
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
  board?: PostBoard
  imageUrl?: string | null
  /** 연관 상품 id 목록 (학생증 자랑 글에서 첨부). */
  productIds?: number[]
}

export async function getPosts(board?: PostBoard): Promise<Post[]> {
  const { data } = await apiClient.get<Post[]>('/posts', {
    params: board ? { board } : undefined,
  })
  return data.map(normalizePost)
}

export async function getPost(id: number): Promise<Post> {
  const { data } = await apiClient.get<Post>(`/posts/${id}`)
  return normalizePost(data)
}

export async function getPostsByAuthor(authorId: number): Promise<Post[]> {
  const { data } = await apiClient.get<Post[]>('/posts', { params: { authorId } })
  return data.map(normalizePost)
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
