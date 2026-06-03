import apiClient from './client'
import type { AuthTokens, User, UserRole } from '@/types'

export interface LoginRequest {
  email: string
  password: string
}

export interface SignupRequest {
  email: string
  password: string
  nickname: string
  role: UserRole
}

export async function login(request: LoginRequest): Promise<AuthTokens> {
  const { data } = await apiClient.post<AuthTokens>('/auth/login', request)
  return data
}

export async function signup(request: SignupRequest): Promise<AuthTokens> {
  const { data } = await apiClient.post<AuthTokens>('/auth/signup', request)
  return data
}

export async function getMe(): Promise<User> {
  const { data } = await apiClient.get<User>('/users/me')
  return data
}
