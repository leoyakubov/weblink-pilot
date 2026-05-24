import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import LinkView from '@/views/LinkView.vue'
import DashboardView from '@/views/DashboardView.vue'
import HistoryView from '@/views/HistoryView.vue'
import MonitoringView from '@/views/MonitoringView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/link/:code', name: 'link', component: LinkView, props: true },
    { path: '/dashboard', name: 'dashboard', component: DashboardView },
    { path: '/history', name: 'history', component: HistoryView },
    { path: '/monitoring', name: 'monitoring', component: MonitoringView },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

export default router
