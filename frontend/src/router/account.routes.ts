import type { RouteRecordRaw } from 'vue-router';

export const accountRoutes: RouteRecordRaw[] = [
  {
    path: '/auth/signin',
    name: 'signin',
    component: () => import('@/account/login/LoginView.vue'),
  },
  {
    path: '/auth/signup',
    name: 'signup',
    component: () => import('@/account/register/RegisterView.vue'),
  },
  {
    path: '/auth/github/complete',
    name: 'github-login-complete',
    component: () => import('@/account/github/GithubLoginCompleteView.vue'),
  },
  {
    path: '/auth/forgot-password',
    name: 'forgot-password',
    component: () => import('@/account/password-reset/request/PasswordResetRequestView.vue'),
  },
  {
    path: '/auth/reset-password',
    name: 'reset-password',
    component: () => import('@/account/password-reset/confirm/PasswordResetConfirmView.vue'),
  },
  {
    path: '/auth/verify-email/request',
    name: 'verify-email-request',
    component: () =>
      import('@/account/email-verification/request/EmailVerificationRequestView.vue'),
  },
  {
    path: '/auth/verify-email',
    name: 'verify-email',
    component: () =>
      import('@/account/email-verification/confirm/EmailVerificationConfirmView.vue'),
  },
  {
    path: '/account',
    name: 'account',
    component: () => import('@/account/account-settings/AccountView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/account/security',
    redirect: '/account',
  },
];
