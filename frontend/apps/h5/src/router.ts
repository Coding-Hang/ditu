import { createRouter, createWebHistory } from 'vue-router'
import ChatView from './views/ChatView.vue'
import CustomerServiceView from './views/CustomerServiceView.vue'
import HistoryView from './views/HistoryView.vue'
import LoginView from './views/LoginView.vue'
import PlansView from './views/PlansView.vue'
import TicketDetailView from './views/TicketDetailView.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView },
    { path: '/', redirect: '/chat' },
    { path: '/chat/:conversationId?', component: ChatView },
    { path: '/history', component: HistoryView },
    { path: '/plans', component: PlansView },
    { path: '/services', component: CustomerServiceView },
    { path: '/tickets/:ticketId?', component: TicketDetailView }
  ]
})

router.beforeEach(to => {
  const hasToken = Boolean(localStorage.getItem('ditu.accessToken'))
  if (!hasToken && to.path !== '/login') {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (hasToken && to.path === '/login') {
    return { path: '/chat' }
  }
  return true
})
