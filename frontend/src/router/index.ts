import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/views/HomeView.vue')
    },
    {
      path: '/login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guestOnly: true }
    },
    {
      path: '/signup',
      component: () => import('@/views/SignupView.vue'),
      meta: { guestOnly: true }
    },
    {
      path: '/products',
      component: () => import('@/views/ProductListView.vue')
    },
    {
      path: '/community',
      component: () => import('@/views/CommunityView.vue')
    },
    {
      path: '/diagnosis',
      component: () => import('@/views/DiagnosisView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/cart',
      component: () => import('@/views/CartView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/recommendations',
      component: () => import('@/views/RecommendationView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/notifications',
      component: () => import('@/views/NotificationView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/my',
      component: () => import('@/views/MyPageView.vue'),
      meta: { requiresAuth: true }
    }
  ]
})

router.beforeEach((to) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return '/login'
  }

  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return '/'
  }
})

export default router
