/**
 * 비주얼 QA용 API mock 픽스처.
 *
 * `src/types`의 실제 타입에 맞춘 최소·결정적 데이터셋을 정의하고,
 * 요청 URL(+method/body/opts)을 적절한 응답으로 매핑하는 `resolve()`를 제공한다.
 * 실제 백엔드 없이도 모든 라우트가 의미 있는 상태로 렌더되게 하는 것이 목표.
 */
import type {
  CartItem,
  Comment,
  DiagnosisStatus,
  Notification,
  Post,
  Product,
  PublicProfile,
  RecommendationItem,
  Review,
  User,
  UserRole,
  UserSummary,
  WishlistItem,
} from '../../src/types'

export interface ResolveOpts {
  authed?: boolean
  diagnosis?: DiagnosisStatus
  role?: UserRole
}

interface ResolveCtx extends ResolveOpts {
  method?: string
  body?: unknown
}

const DEMO_IMG = '/storage/images/e2e-demo'
const DIAGNOSIS_IMG = '/storage/images/e2e-diagnosis'

/** opts에 따라 `/users/me` 응답(로그인 사용자)을 만든다. */
function makeMe(opts: ResolveOpts): User {
  const diagnosisStatus: DiagnosisStatus = opts.diagnosis ?? 'NONE'
  const role: UserRole = opts.role ?? 'NORMAL'
  const completed = diagnosisStatus === 'COMPLETED'
  return {
    id: 1,
    email: 'demo@hakku.dev',
    nickname: '하꾸유저',
    role,
    personalColor: completed ? 'WARM_SPRING' : null,
    profileImageUrl: null,
    diagnosisImageUrl: completed ? DIAGNOSIS_IMG : null,
    preferredStyles: ['girlish', 'casual'],
    preferredColors: ['pink', 'yellow'],
    diagnosisStatus,
    onboardingCompleted: true,
  }
}

const CATEGORIES = ['핀뱃지', '키링', '꾸미기 스티커'] as const

function makeProduct(id: number): Product {
  const category = CATEGORIES[id % CATEGORIES.length]
  return {
    id,
    name: `${category} 데모 상품 ${id}`,
    description: '따뜻한 무드의 데모 상품입니다. 퍼스널컬러에 맞춰 골라보세요.',
    price: 4900 + id * 1500,
    category,
    imageUrl: DEMO_IMG,
    purchaseUrl: 'https://example.com/buy',
    keyColor: 'pink',
    subColor: 'yellow',
    colors: ['pink', 'yellow'],
    styles: ['girlish'],
    sellerId: 9,
  }
}

const PRODUCTS: Product[] = Array.from({ length: 12 }, (_, i) => makeProduct(i + 1))

function makeReview(id: number, productId: number): Review {
  return {
    id,
    rating: 4 + (id % 2),
    content: '실물이 더 예뻐요. 색감이 화면이랑 똑같습니다!',
    authorId: id === 1 ? 1 : 20 + id,
    authorNickname: id === 1 ? '하꾸유저' : `리뷰어${id}`,
    productId,
    productName: `데모 상품 ${productId}`,
    createdAt: '2026-06-20T09:00:00Z',
  }
}

const REVIEWS: Review[] = [1, 2, 3].map((i) => makeReview(i, 1))

function makePost(id: number, board: Post['board']): Post {
  const isShowcase = board === 'STUDENT_ID'
  return {
    id,
    title: isShowcase ? `학생증 자랑 ${id}` : `자유게시판 글 ${id}`,
    content: '오늘 새로 산 키링이랑 핀뱃지 자랑해요. 다들 어디서 사세요?',
    board,
    imageUrl: isShowcase ? DEMO_IMG : id % 2 === 0 ? DEMO_IMG : null,
    authorId: 10 + id,
    authorNickname: `꾸미러${id}`,
    likeCount: 3 * id,
    commentCount: id,
    liked: id % 3 === 0,
    createdAt: '2026-06-21T12:00:00Z',
  }
}

const GENERAL_POSTS: Post[] = [1, 2, 3, 4, 5].map((i) => makePost(i, 'GENERAL'))
const SHOWCASE_POSTS: Post[] = [6, 7, 8].map((i) => makePost(i, 'STUDENT_ID'))
const ALL_POSTS = [...GENERAL_POSTS, ...SHOWCASE_POSTS]

function makeComment(id: number, postId: number): Comment {
  return {
    id,
    content: '저도 그거 샀어요! 색 조합 너무 좋아요 :)',
    authorId: 30 + id,
    authorNickname: `댓글러${id}`,
    postId,
    createdAt: '2026-06-21T13:00:00Z',
  }
}

const COMMENTS: Comment[] = [1, 2].map((i) => makeComment(i, 1))

function makeRecommendation(product: Product, rank: number): RecommendationItem {
  const score = 100 - rank * 9
  return {
    product,
    score,
    breakdown: {
      personalColor: 40 - rank * 2,
      preferredColor: 25 - rank,
      style: 15,
      popularityScore: 10,
      reviewScore: 6,
      actionTagScore: 4,
    },
  }
}

const RECOMMENDATIONS: RecommendationItem[] = PRODUCTS.slice(0, 8).map((p, i) =>
  makeRecommendation(p, i),
)

const CART_ITEMS: CartItem[] = [
  { id: 1, productId: 1, productName: '핀뱃지 데모 상품 1', price: 6400, quantity: 2 },
  { id: 2, productId: 2, productName: '키링 데모 상품 2', price: 7900, quantity: 1 },
]

const NOTIFICATIONS: Notification[] = [
  {
    type: 'DIAGNOSIS_COMPLETE',
    actorId: null,
    actorNickname: null,
    postId: null,
    postTitlePreview: null,
    productId: null,
    message: '퍼스널컬러 진단이 완료되었어요!',
    createdAt: 1_750_000_000_000,
  },
  {
    type: 'COMMENT',
    actorId: 11,
    actorNickname: '꾸미러1',
    postId: 1,
    postTitlePreview: '자유게시판 글 1',
    productId: null,
    message: '꾸미러1님이 회원님의 글에 댓글을 남겼어요.',
    createdAt: 1_749_990_000_000,
  },
  {
    type: 'FOLLOW',
    actorId: 12,
    actorNickname: '꾸미러2',
    postId: null,
    postTitlePreview: null,
    productId: null,
    message: '꾸미러2님이 회원님을 팔로우하기 시작했어요.',
    createdAt: 1_749_980_000_000,
  },
]

function makeWishlistItem(id: number): WishlistItem {
  return {
    id,
    productId: id,
    productName: `데모 상품 ${id}`,
    productImageUrl: DEMO_IMG,
    productPrice: 6400 + id * 1000,
    ownerId: 1,
    ownerNickname: '하꾸유저',
    likeCount: id * 2,
    likedByMe: id % 2 === 0,
    createdAt: '2026-06-19T08:00:00Z',
  }
}

const WISHLIST: WishlistItem[] = [1, 2, 3].map(makeWishlistItem)

function makeCurationCard(id: number) {
  return {
    id,
    kicker: 'NEW',
    title: `큐레이션 카드 ${id}`,
    subtitle: '데모 큐레이션 문구',
    body: '데모 매거진 본문입니다.',
    imageUrl: DEMO_IMG,
    linkUrl: null,
    displayOrder: id,
    active: true,
    createdAt: '2026-06-22T09:00:00Z',
    updatedAt: '2026-06-22T09:00:00Z',
  }
}

function makeUserSummary(id: number): UserSummary {
  return {
    id,
    nickname: `친구${id}`,
    profileImageUrl: null,
    personalColor: id % 2 === 0 ? 'COOL_SUMMER' : 'DEEP_AUTUMN',
  }
}

const FOLLOWERS: UserSummary[] = [2, 3, 4].map(makeUserSummary)
const FOLLOWING: UserSummary[] = [5, 6].map(makeUserSummary)

function makePublicProfile(id: number): PublicProfile {
  return {
    id,
    nickname: `친구${id}`,
    profileImageUrl: null,
    role: 'NORMAL',
    personalColor: 'COOL_SUMMER',
    preferredColors: ['blue', 'purple'],
    preferredStyles: ['chic'],
    followerCount: 12,
    followingCount: 8,
    followedByMe: false,
  }
}

function lastId(path: string): number {
  const m = path.match(/(\d+)/g)
  return m ? Number(m[m.length - 1]) : 1
}

/** path가 정규식과 매칭되면 true. */
function is(path: string, re: RegExp): boolean {
  return re.test(path)
}

/**
 * 요청 URL을 픽스처 응답으로 변환한다.
 * 더 구체적인 패턴을 먼저 검사한다(예: /users/me 가 /users/:id 보다 먼저).
 */
function resolve(rawUrl: string, ctx: ResolveCtx = {}): unknown {
  const url = new URL(rawUrl)
  const path = url.pathname.replace(/^.*\/api/, '') // `/api` 접두어 제거 → `/products` 등
  const method = (ctx.method ?? 'GET').toUpperCase()
  const params = url.searchParams

  // --- auth ---
  if (is(path, /^\/auth\/login$/) || is(path, /^\/auth\/signup$/)) {
    return { accessToken: 'e2e-token' }
  }

  // --- current user ---
  if (is(path, /^\/users\/me\/preferred-colors$/)) {
    return makeMe(ctx)
  }
  if (is(path, /^\/users\/me$/)) {
    return makeMe(ctx)
  }

  // --- users/:id sub-resources (구체적 패턴 우선) ---
  if (is(path, /^\/users\/\d+\/reviews$/)) return REVIEWS
  if (is(path, /^\/users\/\d+\/wishlist$/)) return WISHLIST
  if (is(path, /^\/users\/\d+\/followers$/)) return FOLLOWERS
  if (is(path, /^\/users\/\d+\/following$/)) return FOLLOWING
  if (is(path, /^\/users\/\d+\/follow$/)) return { following: true, followerCount: 13 }
  if (is(path, /^\/users\/\d+$/)) return makePublicProfile(lastId(path))

  // --- products ---
  if (is(path, /^\/products\/\d+\/reviews$/)) {
    if (method === 'POST') return makeReview(99, lastId(path))
    return REVIEWS
  }
  if (is(path, /^\/products\/\d+\/wishlist$/)) {
    return { wishlisted: method === 'POST', wishlistCount: method === 'POST' ? 6 : 5 }
  }
  if (is(path, /^\/products\/\d+$/)) {
    return makeProduct(lastId(path))
  }
  if (is(path, /^\/products$/)) {
    if (method === 'POST') return makeProduct(99)
    return PRODUCTS
  }

  // --- reviews/:id ---
  if (is(path, /^\/reviews\/\d+$/)) return makeReview(lastId(path), 1)

  // --- posts ---
  if (is(path, /^\/posts\/\d+\/likes$/)) return { liked: true, likeCount: 7 }
  if (is(path, /^\/posts\/\d+\/comments$/)) {
    if (method === 'POST') return makeComment(99, lastId(path))
    return COMMENTS
  }
  if (is(path, /^\/posts\/\d+$/)) {
    const id = lastId(path)
    return ALL_POSTS.find((p) => p.id === id) ?? makePost(id, 'GENERAL')
  }
  if (is(path, /^\/posts$/)) {
    if (method === 'POST') return makePost(99, 'GENERAL')
    const board = params.get('board')
    if (board === 'STUDENT_ID') return SHOWCASE_POSTS
    if (board === 'GENERAL') return GENERAL_POSTS
    if (params.get('authorId')) return GENERAL_POSTS
    return ALL_POSTS
  }

  // --- comments/:id ---
  if (is(path, /^\/comments\/\d+$/)) return makeComment(lastId(path), 1)

  // --- wishlist/:id/likes ---
  if (is(path, /^\/wishlist\/\d+\/likes$/)) return { liked: true, likeCount: 9 }

  // --- curation cards (메인 캐러셀은 빈 배열 → 기본 슬라이드 폴백 유지) ---
  if (is(path, /^\/admin\/curation-cards(\/\d+)?$/)) {
    if (method === 'POST' || method === 'PUT') return makeCurationCard(99)
    if (method === 'DELETE') return {}
    return [makeCurationCard(1), makeCurationCard(2)]
  }
  if (is(path, /^\/curation-cards\/\d+$/)) return makeCurationCard(lastId(path))
  if (is(path, /^\/curation-cards$/)) return []

  // --- recommendations ---
  if (is(path, /^\/recommendations$/)) return RECOMMENDATIONS

  // --- cart ---
  if (is(path, /^\/cart\/items\/\d+$/)) {
    const id = lastId(path)
    const existing = CART_ITEMS.find((c) => c.id === id) ?? CART_ITEMS[0]
    const body = (ctx.body ?? {}) as { quantity?: number }
    return { ...existing, quantity: body.quantity ?? existing.quantity }
  }
  if (is(path, /^\/cart\/items$/)) return CART_ITEMS

  // --- notifications ---
  if (is(path, /^\/notifications$/)) return NOTIFICATIONS

  // 알 수 없는 엔드포인트: 빈 객체(렌더 깨짐 방지)
  return {}
}

export const fixtures = { resolve }
