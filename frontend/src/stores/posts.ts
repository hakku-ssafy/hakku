import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getPosts, createPost, toggleLike } from '@/api/posts'
import type { Post } from '@/types'
import type { CreatePostRequest } from '@/api/posts'

export const usePostStore = defineStore('posts', () => {
  const posts = ref<Post[]>([])
  const currentPost = ref<Post | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchPosts() {
    loading.value = true
    error.value = null
    try {
      posts.value = await getPosts()
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '오류가 발생했습니다'
    } finally {
      loading.value = false
    }
  }

  async function createPostAction(request: CreatePostRequest): Promise<Post> {
    const created = await createPost(request)
    posts.value = [created, ...posts.value]
    return created
  }

  async function toggleLikeAction(postId: number) {
    const result = await toggleLike(postId)
    posts.value = posts.value.map(p =>
      p.id === postId ? { ...p, likeCount: result.likeCount } : p
    )
  }

  return { posts, currentPost, loading, error, fetchPosts, createPostAction, toggleLikeAction }
})
