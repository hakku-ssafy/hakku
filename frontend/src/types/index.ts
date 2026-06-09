export type UserRole = 'NORMAL' | 'SELLER' | 'ADMIN'
export type DiagnosisStatus = 'NONE' | 'PENDING' | 'COMPLETED'

export interface User {
  id: number
  email: string
  nickname: string
  role: UserRole
  personalColor: string | null
  profileImageUrl: string | null
  diagnosisImageUrl: string | null
  preferredStyles: string[]
  preferredColors: string[]
  diagnosisStatus: DiagnosisStatus
  onboardingCompleted: boolean
}

export interface AuthTokens {
  accessToken: string
}

export type PostBoard = 'GENERAL' | 'STUDENT_ID'

export interface Post {
  id: number
  title: string
  content: string
  board: PostBoard
  imageUrl: string | null
  authorId: number
  authorNickname: string
  likeCount: number
  commentCount: number
  liked: boolean
  createdAt: string
}

export interface Comment {
  id: number
  content: string
  authorId: number
  authorNickname: string
  postId: number
  createdAt: string
}

export interface Product {
  id: number
  name: string
  description: string
  price: number
  category: string | null
  imageUrl: string | null
  purchaseUrl: string | null
  keyColor: string | null
  subColor: string | null
  colors: string[]
  styles: string[]
  sellerId: number
}

export interface CartItem {
  id: number
  productId: number
  productName: string
  price: number
  quantity: number
}

export interface Review {
  id: number
  rating: number
  content: string
  authorId: number
  authorNickname: string
  productId: number
  productName: string
  createdAt: string
}

/** 팔로워/팔로잉 목록 등에 쓰이는 회원 요약. */
export interface UserSummary {
  id: number
  nickname: string
  profileImageUrl: string | null
  personalColor: string | null
}

/** 다른 회원의 공개 프로필 + 팔로우 통계. */
export interface PublicProfile {
  id: number
  nickname: string
  profileImageUrl: string | null
  role: UserRole
  personalColor: string | null
  preferredColors: string[]
  preferredStyles: string[]
  followerCount: number
  followingCount: number
  followedByMe: boolean
}

export interface FollowToggle {
  following: boolean
  followerCount: number
}

/** 한 회원의 찜 항목(상품 요약 + 좋아요 상태). */
export interface WishlistItem {
  id: number
  productId: number
  productName: string
  productImageUrl: string | null
  productPrice: number
  ownerId: number
  ownerNickname: string
  likeCount: number
  likedByMe: boolean
  createdAt: string
}

export interface WishlistStatus {
  wishlisted: boolean
  wishlistCount: number
}

export interface WishlistLikeResult {
  liked: boolean
  likeCount: number
}

export interface RecommendationItem {
  product: Product
  score: number
  breakdown: {
    personalColor: number
    preferredColor: number
    style: number
    popularityScore: number
    reviewScore: number
    actionTagScore: number
  }
}

export type NotificationType =
  | 'COMMENT'
  | 'LIKE'
  | 'DIAGNOSIS_COMPLETE'
  | 'FOLLOW'
  | 'WISHLIST_LIKE'

export interface Notification {
  type: NotificationType
  actorId: number | null
  actorNickname: string | null
  postId: number | null
  postTitlePreview: string | null
  productId: number | null
  message: string
  createdAt: number
}

export const COLOR_OPTIONS = [
  { value: 'ALL', label: '전체 컬러' },
  { value: 'red', label: '레드' },
  { value: 'orange', label: '오렌지' },
  { value: 'yellow', label: '옐로우' },
  { value: 'green', label: '그린' },
  { value: 'blue', label: '블루' },
  { value: 'purple', label: '퍼플' },
  { value: 'pink', label: '핑크' },
  { value: 'brown', label: '브라운' },
] as const

export const PRODUCT_CATEGORIES = ['핀뱃지', '키링', '꾸미기 스티커'] as const

export const PERSONAL_COLOR_LABELS: Record<string, string> = {
  LIGHT_SPRING: '라이트 스프링',
  WARM_SPRING: '웜 스프링',
  BRIGHT_SPRING: '브라이트 스프링',
  CLEAR_SPRING: '클리어 스프링',
  LIGHT_SUMMER: '라이트 서머',
  COOL_SUMMER: '쿨 서머',
  SOFT_SUMMER: '소프트 서머',
  MUTED_SUMMER: '뮤트 서머',
  SOFT_AUTUMN: '소프트 어텀',
  WARM_AUTUMN: '웜 어텀',
  DEEP_AUTUMN: '딥 어텀',
  MUTED_AUTUMN: '뮤트 어텀',
  CLEAR_WINTER: '클리어 윈터',
  COOL_WINTER: '쿨 윈터',
  DEEP_WINTER: '딥 윈터',
  BRIGHT_WINTER: '브라이트 윈터',
}

export function formatPersonalColor(code: string | null): string {
  if (!code) return ''
  return PERSONAL_COLOR_LABELS[code] ?? code
}

export function normalizeProduct(raw: Product): Product {
  return {
    ...raw,
    colors: raw.colors ?? [],
    styles: raw.styles ?? [],
    purchaseUrl: raw.purchaseUrl ?? null,
  }
}
