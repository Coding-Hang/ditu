<script setup lang="ts">
import { DITU_BRAND } from '@ditu/ui-shared'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import ChatPage from './pages/ChatPage.vue'
import HistoryPage from './pages/HistoryPage.vue'
import LoginPage from './pages/LoginPage.vue'
import PlansPage from './pages/PlansPage.vue'
import ServicesPage from './pages/ServicesPage.vue'
import TicketPage from './pages/TicketPage.vue'
import { useMiniAuthStore } from './stores/auth'

type ViewName = 'login' | 'chat' | 'history' | 'plans' | 'services' | 'tickets'
const auth = useMiniAuthStore()
auth.hydrateClient()
const view = ref<ViewName>(auth.accessToken ? 'chat' : 'login')
const notice = ref('')

function switchView(next: ViewName) {
  if (!auth.accessToken && next !== 'login') {
    notice.value = '请先登录后再使用滴兔智能体'
    view.value = 'login'
    return
  }
  notice.value = ''
  view.value = next
}

function handleUnauthorized() {
  auth.logout()
  notice.value = '登录已过期，请重新登录'
  view.value = 'login'
}

onMounted(() => window.addEventListener('ditu:unauthorized', handleUnauthorized))
onBeforeUnmount(() => window.removeEventListener('ditu:unauthorized', handleUnauthorized))
</script>

<template>
  <main class="phone">
    <header class="mobile-brand">
      <span class="brand-mark" aria-hidden="true"><i /><i /><i /><i /></span>
      <div>
        <strong>{{ DITU_BRAND.name }}</strong>
        <small>{{ DITU_BRAND.slogan }}</small>
      </div>
    </header>
    <p v-if="notice" class="notice">{{ notice }}</p>
    <LoginPage v-if="view === 'login'" @logged-in="switchView('chat')" />
    <ChatPage v-else-if="view === 'chat'" />
    <HistoryPage v-else-if="view === 'history'" />
    <PlansPage v-else-if="view === 'plans'" />
    <ServicesPage v-else-if="view === 'services'" />
    <TicketPage v-else />
    <nav v-if="view !== 'login'" class="tabs">
      <button :class="{ active: view === 'chat' }" @click="switchView('chat')">会话</button>
      <button :class="{ active: view === 'history' }" @click="switchView('history')">历史</button>
      <button :class="{ active: view === 'plans' }" @click="switchView('plans')">套餐</button>
      <button :class="{ active: view === 'services' }" @click="switchView('services')">客服</button>
      <button :class="{ active: view === 'tickets' }" @click="switchView('tickets')">工单</button>
    </nav>
  </main>
</template>
