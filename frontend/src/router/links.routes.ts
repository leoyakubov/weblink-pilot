import type { RouteRecordRaw } from 'vue-router';

export const linksRoutes: RouteRecordRaw[] = [
  {
    path: '/link/:code',
    name: 'link',
    component: () => import('@/features/links/link/LinkView.vue'),
    props: true,
  },
  {
    path: '/links',
    name: 'links',
    component: () => import('@/features/links/history/HistoryView.vue'),
  },
  {
    path: '/history',
    redirect: '/links',
  },
];
