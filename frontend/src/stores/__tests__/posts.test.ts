import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { usePostStore } from '../posts'
import { clearAll } from '@/lib/resourceCache'
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
  relatedProducts: [],
  createdAt: '2026-06-01T10:00:00'
}

describe('usePostStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    clearAll() // 모듈 레벨 SWR 캐시 격리
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

  it('같은 board 재방문 시 staleTime 이내면 다시 받지 않는다(SWR)', async () => {
    vi.useFakeTimers()
    mockGetPosts.mockResolvedValue([mockPost])
    const store = usePostStore()

    await store.fetchPosts()
    await store.fetchPosts()

    expect(mockGetPosts).toHaveBeenCalledTimes(1)
    expect(store.posts).toHaveLength(1)
    vi.useRealTimers()
  })

  it('board 별로 캐시가 분리된다', async () => {
    vi.useFakeTimers()
    mockGetPosts.mockResolvedValue([mockPost])
    const store = usePostStore()

    await store.fetchPosts('GENERAL')
    await store.fetchPosts('STUDENT_ID')
    await store.fetchPosts('GENERAL') // 캐시(fresh)

    expect(mockGetPosts).toHaveBeenCalledTimes(2) // board 별 1회씩
    vi.useRealTimers()
  })

  it('글 작성 후 목록 캐시가 무효화되어 다음 방문에 다시 받는다', async () => {
    vi.useFakeTimers()
    mockGetPosts.mockResolvedValue([mockPost])
    mockCreatePost.mockResolvedValueOnce(mockPost)
    const store = usePostStore()

    await store.fetchPosts() // 호출 1
    await store.createPostAction({ title: 'x', content: 'y' })
    await store.fetchPosts() // 무효화됨 → 호출 2

    expect(mockGetPosts).toHaveBeenCalledTimes(2)
    vi.useRealTimers()
  })
})
