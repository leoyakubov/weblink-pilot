import { createRouter, createWebHistory } from 'vue-router';
import { authState, bootstrapAuth } from '@/account/AuthSession';
import { aboutRoutes } from './about.routes';
import { accountRoutes } from './account.routes';
import { adminRoutes } from './admin.routes';
import { analyticsRoutes } from './analytics.routes';
import { homeRoutes } from './home.routes';
import { linksRoutes } from './links.routes';
import { monitoringRoutes } from './monitoring.routes';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    ...homeRoutes,
    ...aboutRoutes,
    ...linksRoutes,
    ...analyticsRoutes,
    ...accountRoutes,
    ...monitoringRoutes,
    ...adminRoutes,
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
