<script setup lang="ts">
import { DITU_BRAND } from '@ditu/ui-shared'
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminAuthStore } from './stores/auth'

const auth = useAdminAuthStore()
const route = useRoute()
const router = useRouter()
auth.hydrateClient()

const isLogin = computed(() => route.path === '/login')
const activeMenu = computed(() => route.path)

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
    <el-aside width="272px" class="nav">
      <div class="brand-lockup">
        <span class="brand-mark" aria-hidden="true"><i /><i /><i /><i /></span>
        <span class="brand-copy">
          <strong>{{ DITU_BRAND.name }}</strong>
          <small>{{ DITU_BRAND.slogan }}</small>
        </span>
      </div>
      <el-menu router :default-active="activeMenu" class="side-menu">
        <el-menu-item index="/users">用户管理</el-menu-item>
        <el-menu-item index="/quota">次数流水</el-menu-item>
        <el-menu-item index="/models">模型链接测试</el-menu-item>
        <el-menu-item index="/tickets">工单管理</el-menu-item>
        <el-menu-item index="/rag">RAG 知识库</el-menu-item>
        <el-menu-item index="/plans">套餐配置</el-menu-item>
      </el-menu>
      <div class="admin-note">
        <strong>管理审计</strong>
        <span>用户、次数、模型、知识库等敏感操作会写入 audit_log。</span>
      </div>
    </el-aside>
    <el-main class="workspace">
      <header class="topbar">
        <div>
          <strong>{{ DITU_BRAND.adminName }}</strong>
          <small>知识产权智能体运营后台</small>
        </div>
        <div class="topbar-actions">
          <el-tag v-if="auth.user" effect="plain">{{ auth.user.displayName }}</el-tag>
          <el-button text @click="logout">退出</el-button>
        </div>
      </header>
      <router-view />
    </el-main>
  </el-container>
</template>
