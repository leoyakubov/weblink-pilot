import { createRouter, createWebHistory } from 'vue-router';
import { authState, bootstrapAuth } from '@/lib/auth';
import HomeView from '@/views/HomeView.vue';
import AboutView from '@/views/AboutView.vue';
import AuthView from '@/views/AuthView.vue';
import LinkView from '@/views/LinkView.vue';
import DashboardView from '@/views/DashboardView.vue';
import HistoryView from '@/views/HistoryView.vue';
import MonitoringView from '@/views/MonitoringView.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/about', name: 'about', component: AboutView },
    { path: '/auth/signin', name: 'signin', component: AuthView, props: { mode: 'login' } },
    { path: '/auth/signup', name: 'signup', component: AuthView, props: { mode: 'register' } },
    { path: '/link/:code', name: 'link', component: LinkView, props: true },
    { path: '/dashboard', name: 'dashboard', component: DashboardView },
    { path: '/history', name: 'history', component: HistoryView },
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

  return true;
});

export default router;
