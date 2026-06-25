import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getPosts, createPost, toggleLike } from '@/api/posts'
import { getEntry, setEntry, isFresh, invalidatePrefix, dedupe } from '@/lib/resourceCache'
import type { Post, PostBoard } from '@/types'
import type { CreatePostRequest } from '@/api/posts'

const POSTS_PREFIX = 'posts:'
const STALE_TIME = 30_000

function postsKey(board?: PostBoard): string {
  return `${POSTS_PREFIX}${board ?? 'ALL'}`
}

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
  // 현재 보여주는 목록의 캐시 키(좋아요 등 변경 시 같은 키 캐시를 동기화).
  let lastKey = postsKey()

  /**
   * 게시글 목록 — board 별 SWR. 캐시가 있으면 즉시 보여주고, staleTime 이내면 네트워크 생략,
   * 지났으면 캐시를 보여준 채 백그라운드로 갱신한다. 캐시가 없을 때만 loading 을 띄운다.
   */
  async function fetchPosts(board?: PostBoard) {
    const key = postsKey(board)
    lastKey = key
    const cached = getEntry<Post[]>(key)
    if (cached) {
      posts.value = cached.data
      error.value = null
      if (isFresh(key, STALE_TIME)) return
    } else {
      loading.value = true
      error.value = null
    }
    try {
      const data = await dedupe(key, () => getPosts(board))
      const normalized = data.map(normalizePost)
      posts.value = normalized
      setEntry(key, normalized)
      error.value = null
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '오류가 발생했습니다'
    } finally {
      loading.value = false
    }
  }

  async function createPostAction(request: CreatePostRequest): Promise<Post> {
    const created = normalizePost(await createPost(request))
    posts.value = [created, ...posts.value]
    // 새 글이 모든 board 목록에 정확히 반영되도록 캐시를 비운다(다음 방문 시 최신 재요청).
    invalidatePrefix(POSTS_PREFIX)
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
    // 현재 board 캐시를 동기화해, 재방문 시 좋아요 상태가 유지되도록 한다.
    setEntry(lastKey, posts.value)
    return result
  }

  function setCurrentPost(post: Post | null) {
    currentPost.value = post ? normalizePost(post) : null
  }

  return { posts, currentPost, loading, error, fetchPosts, createPostAction, toggleLikeAction, setCurrentPost }
})
