<script setup lang="ts">
import { DITU_BRAND } from '@ditu/ui-shared'
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
auth.hydrateClient()

const isLogin = computed(() => route.path === '/login')
const activeMenu = computed(() => {
  if (route.path.startsWith('/chat')) return '/chat'
  if (route.path.startsWith('/tickets')) return '/tickets'
  return route.path
})

onMounted(async () => {
  if (auth.accessToken && !auth.user) {
    await auth.loadMe().catch(() => auth.logout())
  }
})

async function logout() {
  auth.logout()
  await router.push('/login')
}
</script>

<template>
  <main v-if="isLogin" class="login-shell">
    <router-view />
  </main>
  <el-container v-else class="shell">
    <el-aside width="264px" class="nav">
      <div class="brand-lockup">
        <span class="brand-mark" aria-hidden="true"><i /><i /><i /><i /></span>
        <span class="brand-copy">
          <strong>{{ DITU_BRAND.name }}</strong>
          <small>{{ DITU_BRAND.slogan }}</small>
        </span>
      </div>
      <el-menu router :default-active="activeMenu" class="side-menu">
        <el-menu-item index="/chat">智能会话</el-menu-item>
        <el-menu-item index="/history">会话历史</el-menu-item>
        <el-menu-item index="/plans">套餐次数</el-menu-item>
        <el-menu-item index="/services">专属客服</el-menu-item>
        <el-menu-item index="/tickets">工单跟进</el-menu-item>
      </el-menu>
      <div v-if="auth.user" class="quota-card">
        <span>{{ auth.user.displayName }}</span>
        <strong>{{ auth.user.remainingQuota }} 次</strong>
        <small>{{ auth.user.planCode }} 套餐剩余额度</small>
      </div>
    </el-aside>
    <el-main class="workspace">
      <header class="topbar">
        <div>
          <strong>{{ DITU_BRAND.appName }}</strong>
          <small>知识产权咨询、材料梳理与服务跟进</small>
        </div>
        <el-button text @click="logout">退出</el-button>
      </header>
      <router-view />
    </el-main>
  </el-container>
</template>
