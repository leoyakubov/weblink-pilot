import type { RouteRecordRaw } from 'vue-router';

export const aboutRoutes: RouteRecordRaw[] = [
  {
    path: '/about',
    name: 'about',
    component: () => import('@/features/about/AboutView.vue'),
  },
];
