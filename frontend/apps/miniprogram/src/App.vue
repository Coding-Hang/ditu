<script setup lang="ts">
import { ref } from 'vue'
import ChatPage from './pages/ChatPage.vue'
import HistoryPage from './pages/HistoryPage.vue'
import LoginPage from './pages/LoginPage.vue'
import PlansPage from './pages/PlansPage.vue'
import ServicesPage from './pages/ServicesPage.vue'
import TicketPage from './pages/TicketPage.vue'

type ViewName = 'login' | 'chat' | 'history' | 'plans' | 'services' | 'tickets'
const view = ref<ViewName>('login')
</script>

<template>
  <main class="phone">
    <LoginPage v-if="view === 'login'" @logged-in="view = 'chat'" />
    <ChatPage v-else-if="view === 'chat'" />
    <HistoryPage v-else-if="view === 'history'" />
    <PlansPage v-else-if="view === 'plans'" />
    <ServicesPage v-else-if="view === 'services'" />
    <TicketPage v-else />
    <nav class="tabs">
      <button @click="view = 'chat'">会话</button>
      <button @click="view = 'history'">历史</button>
      <button @click="view = 'plans'">套餐</button>
      <button @click="view = 'services'">客服</button>
      <button @click="view = 'tickets'">工单</button>
    </nav>
  </main>
</template>
