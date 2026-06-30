import { createRouter, createWebHistory } from 'vue-router'
import LoginView from './views/LoginView.vue'
import ModelConfigView from './views/ModelConfigView.vue'
import PlansView from './views/PlansView.vue'
import QuotaLedgerView from './views/QuotaLedgerView.vue'
import RagView from './views/RagView.vue'
import TicketsView from './views/TicketsView.vue'
import UsersView from './views/UsersView.vue'

export const router = createRouter({
  history: createWebHistory('/admin/'),
  routes: [
    { path: '/login', component: LoginView },
    { path: '/', redirect: '/users' },
    { path: '/users', component: UsersView },
    { path: '/quota', component: QuotaLedgerView },
    { path: '/models', component: ModelConfigView },
    { path: '/tickets', component: TicketsView },
    { path: '/rag', component: RagView },
    { path: '/plans', component: PlansView }
  ]
})

router.beforeEach(to => {
  const hasToken = Boolean(localStorage.getItem('ditu.admin.accessToken'))
  if (!hasToken && to.path !== '/login') {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (hasToken && to.path === '/login') {
    return { path: '/users' }
  }
  return true
})
