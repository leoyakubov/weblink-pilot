import { createRouter, createWebHistory } from 'vue-router';
import { authState, bootstrapAuth } from '@/features/auth/services/auth.service';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: () => import('@/features/links/pages/HomeView.vue') },
    { path: '/about', name: 'about', component: () => import('@/app/pages/AboutView.vue') },
    {
      path: '/settings/reset',
      name: 'settings-reset',
      component: () => import('@/app/pages/BrowserSettingsResetView.vue'),
    },
    {
      path: '/auth/signin',
      name: 'signin',
      component: () => import('@/features/auth/pages/AuthView.vue'),
      props: { mode: 'login' },
    },
    {
      path: '/auth/signup',
      name: 'signup',
      component: () => import('@/features/auth/pages/AuthView.vue'),
      props: { mode: 'register' },
    },
    {
      path: '/auth/github/complete',
      name: 'github-login-complete',
      component: () => import('@/features/auth/pages/GithubLoginCompleteView.vue'),
    },
    {
      path: '/auth/forgot-password',
      name: 'forgot-password',
      component: () => import('@/features/auth/pages/PasswordResetRequestView.vue'),
    },
    {
      path: '/auth/reset-password',
      name: 'reset-password',
      component: () => import('@/features/auth/pages/PasswordResetConfirmView.vue'),
    },
    {
      path: '/auth/verify-email/request',
      name: 'verify-email-request',
      component: () => import('@/features/auth/pages/EmailVerificationRequestView.vue'),
    },
    {
      path: '/auth/verify-email',
      name: 'verify-email',
      component: () => import('@/features/auth/pages/EmailVerificationConfirmView.vue'),
    },
    {
      path: '/link/:code',
      name: 'link',
      component: () => import('@/features/links/pages/LinkView.vue'),
      props: true,
    },
    {
      path: '/analytics',
      name: 'analytics',
      component: () => import('@/features/links/pages/AnalyticsView.vue'),
    },
    {
      path: '/analytics/:code',
      name: 'analytics-detail',
      component: () => import('@/features/links/pages/DashboardView.vue'),
    },
    {
      path: '/dashboard',
      redirect: (to) => ({ path: '/analytics', query: to.query }),
    },
    {
      path: '/links',
      name: 'links',
      component: () => import('@/features/links/pages/HistoryView.vue'),
    },
    { path: '/history', redirect: '/links' },
    {
      path: '/account',
      name: 'account',
      component: () => import('@/features/account/pages/AccountView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/account/security',
      redirect: '/account',
    },
    {
      path: '/monitoring',
      name: 'monitoring',
      component: () => import('@/features/monitoring/pages/MonitoringView.vue'),
      meta: { requiresAdmin: true },
    },
  ],
  scrollBehavior() {
    return { top: 0 };
  },
});

router.beforeEach(async (to) => {
  if (!authState.ready) {
    await bootstrapAuth();
  }

  if (to.meta.requiresAdmin && authState.currentUser?.role !== 'ADMIN') {
    return { name: 'home' };
  }

  if (to.meta.requiresAuth && !authState.currentUser) {
    return { name: 'signin' };
  }

  return true;
});

export default router;
