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

/** 게시글에 연결된 연관 상품 요약 (학생증 자랑 → 상품 페이지 이동용). */
export interface RelatedProduct {
  id: number
  name: string
  imageUrl: string | null
  price: number
}

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
  relatedProducts: RelatedProduct[]
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
  /** 판매자(등록자) 닉네임. 추천 등 닉네임을 모르는 응답에서는 null. */
  sellerNickname?: string | null
  /** 활성화 여부(서버 응답에 항상 포함; normalizeProduct 가 기본 true 로 채움). 비활성이면 공개 목록·검색·추천에서 숨김. */
  active?: boolean
}

/** 어드민 상품 목록 커서 페이지(무한 스크롤). */
export interface AdminProductPage {
  items: Product[]
  /** 다음 페이지 커서(없으면 마지막 페이지). */
  nextCursor?: number
  hasMore: boolean
}

/** 어드민 인라인 수정 — 편집 가능한 필드만 담는다. */
export interface AdminProductEdit {
  name: string
  category: string | null
  styles: string[]
  active: boolean
}

export interface CartItem {
  id: number
  productId: number
  productName: string
  price: number
  quantity: number
}

export type OrderStatus = 'CREATED' | 'PAID' | 'CANCELLED'

/** 배송지 입력값(주문 생성 요청 본문). */
export interface ShippingAddress {
  recipientName: string
  phone: string
  postalCode: string
  address1: string
  address2: string
}

export interface OrderItem {
  productId: number
  productName: string
  price: number
  quantity: number
  lineTotal: number
}

export interface Order {
  id: number
  status: OrderStatus
  recipientName: string
  phone: string
  postalCode: string
  address1: string
  address2: string | null
  totalAmount: number
  createdAt: number
  items: OrderItem[]
}

export interface Magazine {
  id: number
  kicker: string | null
  title: string
  subtitle: string | null
  /** 마크다운 본문 (사진·글, 내부 상품 링크 임베드). */
  content: string | null
  coverImageUrl: string | null
  displayOrder: number
  published: boolean
  createdAt: string
  updatedAt: string
}

/** 매거진 발행/수정 입력(어드민). */
export interface MagazineInput {
  kicker: string | null
  title: string
  subtitle: string | null
  content: string | null
  coverImageUrl: string | null
  displayOrder: number
  published: boolean
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
  | 'ORDER'

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
    sellerNickname: raw.sellerNickname ?? null,
    active: raw.active ?? true,
  }
}
