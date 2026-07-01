import type { RouteRecordRaw } from 'vue-router';

export const monitoringRoutes: RouteRecordRaw[] = [
  {
    path: '/monitoring',
    name: 'monitoring',
    component: () => import('@/admin/monitoring/MonitoringView.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/settings/reset',
    name: 'settings-reset',
    component: () => import('@/admin/monitoring/reset/BrowserSettingsResetView.vue'),
  },
];
