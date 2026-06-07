import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { usePostStore } from '../posts'
import * as apiModule from '@/api/posts'

vi.mock('@/api/posts')

const mockGetPosts = vi.mocked(apiModule.getPosts)
const mockCreatePost = vi.mocked(apiModule.createPost)
const mockToggleLike = vi.mocked(apiModule.toggleLike)

const mockPost = {
  id: 1,
  title: '학꾸 후기',
  content: '스티커 너무 좋아요!',
  board: 'GENERAL' as const,
  imageUrl: null,
  authorId: 5,
  authorNickname: '테스터',
  likeCount: 3,
  commentCount: 1,
  liked: false,
  createdAt: '2026-06-01T10:00:00'
}

describe('usePostStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('초기 상태는 빈 게시글 목록이다', () => {
    const store = usePostStore()
    expect(store.posts).toHaveLength(0)
    expect(store.loading).toBe(false)
  })

  it('게시글 목록을 불러온다', async () => {
    mockGetPosts.mockResolvedValueOnce([mockPost])
    const store = usePostStore()

    await store.fetchPosts()

    expect(store.posts).toHaveLength(1)
    expect(store.posts[0].title).toBe('학꾸 후기')
  })

  it('새 게시글을 작성한다', async () => {
    mockCreatePost.mockResolvedValueOnce(mockPost)
    const store = usePostStore()

    const created = await store.createPostAction({
      title: '학꾸 후기',
      content: '스티커 너무 좋아요!'
    })

    expect(created.id).toBe(1)
  })

  it('게시글 좋아요를 토글한다', async () => {
    mockGetPosts.mockResolvedValueOnce([mockPost])
    mockToggleLike.mockResolvedValueOnce({ liked: true, likeCount: 4 })
    const store = usePostStore()
    await store.fetchPosts()

    await store.toggleLikeAction(1)

    expect(store.posts[0].likeCount).toBe(4)
  })
})
