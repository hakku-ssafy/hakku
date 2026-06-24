import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getPosts, createPost, toggleLike } from '@/api/posts'
import type { Post, PostBoard } from '@/types'
import type { CreatePostRequest } from '@/api/posts'

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

export const usePostStore = defineStore('posts', () => {
  const posts = ref<Post[]>([])
  const currentPost = ref<Post | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchPosts(board?: PostBoard) {
    loading.value = true
    error.value = null
    try {
      const data = await getPosts(board)
      posts.value = data.map(normalizePost)
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '오류가 발생했습니다'
    } finally {
      loading.value = false
    }
  }

  async function createPostAction(request: CreatePostRequest): Promise<Post> {
    const created = normalizePost(await createPost(request))
    posts.value = [created, ...posts.value]
    return created
  }

  async function toggleLikeAction(postId: number) {
    const result = await toggleLike(postId)
    posts.value = posts.value.map(p =>
      p.id === postId ? { ...p, likeCount: result.likeCount, liked: result.liked } : p
    )
    if (currentPost.value?.id === postId) {
      currentPost.value = {
        ...currentPost.value,
        likeCount: result.likeCount,
        liked: result.liked,
      }
    }
    return result
  }

  function setCurrentPost(post: Post | null) {
    currentPost.value = post ? normalizePost(post) : null
  }

  return { posts, currentPost, loading, error, fetchPosts, createPostAction, toggleLikeAction, setCurrentPost }
})
