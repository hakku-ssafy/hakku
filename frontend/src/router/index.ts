import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: () => import('@/views/HomeView.vue') },
    { path: '/login', component: () => import('@/views/LoginView.vue'), meta: { guestOnly: true, hideChatFab: true } },
    { path: '/signup', component: () => import('@/views/SignupView.vue'), meta: { guestOnly: true, hideChatFab: true } },
    { path: '/onboarding', component: () => import('@/views/OnboardingView.vue'), meta: { requiresAuth: true, hideChatFab: true } },
    { path: '/products', component: () => import('@/views/ProductListView.vue') },
    { path: '/products/:id', component: () => import('@/views/ProductDetailView.vue') },
    { path: '/community', component: () => import('@/views/CommunityView.vue') },
    { path: '/community/:id', component: () => import('@/views/PostDetailView.vue') },
    { path: '/users/:id', component: () => import('@/views/ProfileView.vue'), meta: { requiresAuth: true } },
    { path: '/seller/products', component: () => import('@/views/SellerProductsView.vue'), meta: { requiresAuth: true } },
    { path: '/diagnosis', component: () => import('@/views/DiagnosisView.vue'), meta: { requiresAuth: true } },
    { path: '/cart', component: () => import('@/views/CartView.vue'), meta: { requiresAuth: true } },
    { path: '/order/new', component: () => import('@/views/OrderFormView.vue'), meta: { requiresAuth: true, hideChatFab: true } },
    { path: '/payments/checkout', component: () => import('@/views/PaymentCheckoutView.vue'), meta: { requiresAuth: true, hideChatFab: true } },
    { path: '/payments/success', component: () => import('@/views/PaymentSuccessView.vue'), meta: { requiresAuth: true, hideChatFab: true } },
    { path: '/payments/fail', component: () => import('@/views/PaymentFailView.vue'), meta: { hideChatFab: true } },
    { path: '/recommendations', component: () => import('@/views/RecommendationView.vue'), meta: { requiresAuth: true } },
    { path: '/notifications', component: () => import('@/views/NotificationView.vue'), meta: { requiresAuth: true } },
    { path: '/my', component: () => import('@/views/MyPageView.vue'), meta: { requiresAuth: true } },
    { path: '/magazine/:id', component: () => import('@/views/MagazineDetailView.vue') },
    {
      path: '/admin/curation',
      component: () => import('@/views/AdminCurationView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
  ]
})

function needsOnboarding(user: { role: string; onboardingCompleted: boolean } | null): boolean {
  return user?.role === 'NORMAL' && !user.onboardingCompleted
}

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return '/'
  }

  if (authStore.isAuthenticated && !authStore.user) {
    try {
      await authStore.fetchMe()
    } catch {
      authStore.logout()
      return '/login'
    }
  }

  // 어드민 전용 라우트 — ADMIN 역할이 아니면 홈으로. (백엔드 /api/admin/** 가 1차 방어선)
  if (to.meta.requiresAdmin && authStore.user?.role !== 'ADMIN') {
    return '/'
  }

  if (needsOnboarding(authStore.user) && to.path !== '/onboarding') {
    const protectedPaths = ['/', '/community', '/products', '/cart', '/recommendations', '/notifications', '/my', '/diagnosis']
    if (protectedPaths.some((p) => to.path === p || to.path.startsWith(p + '/'))) {
      return '/onboarding'
    }
  }
})

export default router
