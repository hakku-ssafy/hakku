import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: () => import('@/views/HomeView.vue') },
    { path: '/login', component: () => import('@/views/LoginView.vue'), meta: { guestOnly: true } },
    { path: '/signup', component: () => import('@/views/SignupView.vue'), meta: { guestOnly: true } },
    { path: '/onboarding', component: () => import('@/views/OnboardingView.vue'), meta: { requiresAuth: true } },
    { path: '/products', component: () => import('@/views/ProductListView.vue') },
    { path: '/products/:id', component: () => import('@/views/ProductDetailView.vue') },
    { path: '/community', component: () => import('@/views/CommunityView.vue') },
    { path: '/community/:id', component: () => import('@/views/PostDetailView.vue') },
    { path: '/seller/products', component: () => import('@/views/SellerProductsView.vue'), meta: { requiresAuth: true } },
    { path: '/diagnosis', component: () => import('@/views/DiagnosisView.vue'), meta: { requiresAuth: true } },
    { path: '/cart', component: () => import('@/views/CartView.vue'), meta: { requiresAuth: true } },
    { path: '/recommendations', component: () => import('@/views/RecommendationView.vue'), meta: { requiresAuth: true } },
    { path: '/notifications', component: () => import('@/views/NotificationView.vue'), meta: { requiresAuth: true } },
    { path: '/my', component: () => import('@/views/MyPageView.vue'), meta: { requiresAuth: true } },
  ]
})

function needsOnboarding(user: { role: string; onboardingCompleted: boolean } | null): boolean {
  return user?.role === 'NORMAL' && !user.onboardingCompleted
}

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return '/login'
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

  if (needsOnboarding(authStore.user) && to.path !== '/onboarding') {
    const protectedPaths = ['/', '/community', '/products', '/cart', '/recommendations', '/notifications', '/my', '/diagnosis']
    if (protectedPaths.some((p) => to.path === p || to.path.startsWith(p + '/'))) {
      return '/onboarding'
    }
  }
})

export default router
