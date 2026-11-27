import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/PublicLayout.vue'),
      children: [
        { path: '', name: 'home', component: () => import('@/views/public/HomeView.vue') },
        { path: 'courses', name: 'course-list', component: () => import('@/views/public/CourseListView.vue') },
        { path: 'courses/:id', name: 'course-detail', component: () => import('@/views/public/CourseDetailView.vue') },
        { path: 'seckill', name: 'seckill', component: () => import('@/views/public/SeckillView.vue') },
        {
          path: 'learn/:courseId/:lessonId?',
          name: 'learn',
          component: () => import('@/views/public/LearnView.vue'),
          meta: { requiresAuth: true },
        },
        { path: 'teachers', name: 'teachers', component: () => import('@/views/public/TeacherListView.vue') },
        { path: 'articles', name: 'articles', component: () => import('@/views/public/ArticleListView.vue') },
        { path: 'articles/:id', name: 'article-detail', component: () => import('@/views/public/ArticleDetailView.vue') },
        { path: 'materials', name: 'materials', component: () => import('@/views/public/MaterialListView.vue') },
        {
          path: 'checkout',
          name: 'checkout',
          component: () => import('@/views/public/CheckoutView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'pay/:orderNo',
          name: 'pay',
          component: () => import('@/views/public/PayView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'user',
          name: 'user-center',
          component: () => import('@/views/public/UserCenterView.vue'),
          meta: { requiresAuth: true },
        },
      ],
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/admin/dashboard' },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/admin/DashboardView.vue'),
          meta: { requiresAuth: true, priv: { module: 'admin', method: 'access' } },
        },
        {
          path: 'groups',
          name: 'groups',
          component: () => import('@/views/admin/GroupListView.vue'),
          meta: { requiresAuth: true, priv: { module: 'group', method: 'browse' } },
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('@/views/admin/UserListView.vue'),
          meta: { requiresAuth: true, priv: { module: 'user', method: 'browse' } },
        },
        {
          path: 'courses',
          name: 'admin-courses',
          component: () => import('@/views/admin/CourseListView.vue'),
          meta: { requiresAuth: true, priv: { module: 'course', method: 'browse' } },
        },
        {
          path: 'teachers',
          name: 'admin-teachers',
          component: () => import('@/views/admin/TeacherListView.vue'),
          meta: { requiresAuth: true, priv: { module: 'teacher', method: 'browse' } },
        },
        {
          path: 'articles',
          name: 'admin-articles',
          component: () => import('@/views/admin/ArticleListView.vue'),
          meta: { requiresAuth: true, priv: { module: 'article', method: 'browse' } },
        },
        {
          path: 'materials',
          name: 'admin-materials',
          component: () => import('@/views/admin/MaterialListView.vue'),
          meta: { requiresAuth: true, priv: { module: 'material', method: 'browse' } },
        },
        {
          path: 'banners',
          name: 'admin-banners',
          component: () => import('@/views/admin/BannerListView.vue'),
          meta: { requiresAuth: true, priv: { module: 'banner', method: 'browse' } },
        },
        {
          path: 'orders',
          name: 'admin-orders',
          component: () => import('@/views/admin/OrderListView.vue'),
          meta: { requiresAuth: true, priv: { module: 'order', method: 'browse' } },
        },
        {
          path: 'coupons',
          name: 'admin-coupons',
          component: () => import('@/views/admin/CouponListView.vue'),
          meta: { requiresAuth: true, priv: { module: 'coupon', method: 'browse' } },
        },
        {
          path: 'refunds',
          name: 'admin-refunds',
          component: () => import('@/views/admin/RefundListView.vue'),
          meta: { requiresAuth: true, priv: { module: 'refund', method: 'browse' } },
        },
        {
          path: 'seckill',
          name: 'admin-seckill',
          component: () => import('@/views/admin/SeckillListView.vue'),
          meta: { requiresAuth: true, priv: { module: 'seckill', method: 'browse' } },
        },
        {
          path: 'reviews',
          name: 'admin-reviews',
          component: () => import('@/views/admin/ReviewListView.vue'),
          meta: { requiresAuth: true, priv: { module: 'review', method: 'browse' } },
        },
        {
          path: 'logs',
          name: 'logs',
          component: () => import('@/views/admin/AuditLogView.vue'),
          meta: { requiresAuth: true, priv: { module: 'audit', method: 'browse' } },
        },
      ],
    },
    {
      path: '/403',
      name: 'forbidden',
      component: () => import('@/views/ForbiddenView.vue'),
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
    },
  ],
})

router.beforeEach(async (to) => {
  const store = useAuthStore()
  if (to.meta.public) {
    if (store.isLoggedIn && to.path === '/login') {
      return '/admin/dashboard'
    }
    return true
  }
  if (to.meta.requiresAuth && !store.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (store.isLoggedIn && !store.me) {
    try {
      await store.fetchMe()
    } catch {
      store.clearSession()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }
  const priv = to.meta.priv as { module: string; method: string } | undefined
  if (priv && !store.hasPriv(priv.module, priv.method)) {
    return '/403'
  }
  return true
})

export default router
