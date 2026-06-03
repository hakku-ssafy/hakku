export type UserRole = 'NORMAL' | 'SELLER' | 'ADMIN'
export type DiagnosisStatus = 'NONE' | 'PENDING' | 'COMPLETED'

export interface User {
  id: number
  email: string
  nickname: string
  role: UserRole
  personalColor: string | null
  profileImageUrl: string | null
  preferredStyles: string[]
  diagnosisStatus: DiagnosisStatus
}

export interface AuthTokens {
  accessToken: string
}

export interface Post {
  id: number
  title: string
  content: string
  authorId: number
  authorNickname: string
  likeCount: number
  commentCount: number
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
  imageUrl: string | null
  keyColor: string | null
  subColor: string | null
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
  createdAt: string
}

export interface RecommendationItem {
  product: Product
  score: number
  breakdown: {
    colorScore: number
    styleScore: number
    popularityScore: number
    reviewScore: number
    actionTagScore: number
  }
}

export interface Notification {
  type: string
  recipientId: number
  actorId: number | null
  message: string
  createdAt: string
}

export interface ApiResponse<T> {
  success: boolean
  data?: T
  error?: string
  meta?: {
    total: number
    page: number
    limit: number
  }
}
