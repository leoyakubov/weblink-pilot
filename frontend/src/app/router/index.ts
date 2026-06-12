import { createRouter, createWebHistory } from 'vue-router';
import { authState, bootstrapAuth } from '@/features/auth/services/auth.service';
import HomeView from '@/features/links/pages/HomeView.vue';
import AboutView from '@/features/about/pages/AboutView.vue';
import AuthView from '@/features/auth/pages/AuthView.vue';
import GithubLoginCompleteView from '@/features/auth/pages/GithubLoginCompleteView.vue';
import PasswordResetRequestView from '@/features/auth/pages/PasswordResetRequestView.vue';
import PasswordResetConfirmView from '@/features/auth/pages/PasswordResetConfirmView.vue';
import EmailVerificationRequestView from '@/features/auth/pages/EmailVerificationRequestView.vue';
import EmailVerificationConfirmView from '@/features/auth/pages/EmailVerificationConfirmView.vue';
import LinkView from '@/features/links/pages/LinkView.vue';
import DashboardView from '@/features/links/pages/DashboardView.vue';
import HistoryView from '@/features/links/pages/HistoryView.vue';
import MonitoringView from '@/features/monitoring/pages/MonitoringView.vue';
import AccountView from '@/features/account/pages/AccountView.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/about', name: 'about', component: AboutView },
    { path: '/auth/signin', name: 'signin', component: AuthView, props: { mode: 'login' } },
    { path: '/auth/signup', name: 'signup', component: AuthView, props: { mode: 'register' } },
    {
      path: '/auth/github/complete',
      name: 'github-login-complete',
      component: GithubLoginCompleteView,
    },
    { path: '/auth/forgot-password', name: 'forgot-password', component: PasswordResetRequestView },
    { path: '/auth/reset-password', name: 'reset-password', component: PasswordResetConfirmView },
    {
      path: '/auth/verify-email/request',
      name: 'verify-email-request',
      component: EmailVerificationRequestView,
    },
    {
      path: '/auth/verify-email',
      name: 'verify-email',
      component: EmailVerificationConfirmView,
    },
    { path: '/link/:code', name: 'link', component: LinkView, props: true },
    { path: '/dashboard', name: 'dashboard', component: DashboardView },
    { path: '/history', name: 'history', component: HistoryView },
    {
      path: '/account',
      name: 'account',
      component: AccountView,
      meta: { requiresAuth: true },
    },
    {
      path: '/monitoring',
      name: 'monitoring',
      component: MonitoringView,
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
