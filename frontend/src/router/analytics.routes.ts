import type { RouteRecordRaw } from 'vue-router';

export const analyticsRoutes: RouteRecordRaw[] = [
  {
    path: '/analytics',
    name: 'analytics',
    component: () => import('@/features/analytics/analytics/AnalyticsView.vue'),
  },
  {
    path: '/analytics/:code',
    name: 'analytics-detail',
    component: () => import('@/features/analytics/analytics-detail/AnalyticsDetailView.vue'),
  },
  {
    path: '/dashboard',
    redirect: (to) => ({ path: '/analytics', query: to.query }),
  },
];
