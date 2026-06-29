<script setup lang="ts">
import { useAuthStore } from './stores/auth'

const auth = useAuthStore()
auth.hydrateClient()
</script>

<template>
  <el-container class="shell">
    <el-aside width="220px" class="nav">
      <div class="brand">滴兔智能体</div>
      <el-menu router default-active="/chat">
        <el-menu-item index="/chat">会话</el-menu-item>
        <el-menu-item index="/history">历史</el-menu-item>
        <el-menu-item index="/plans">套餐</el-menu-item>
        <el-menu-item index="/services">客服</el-menu-item>
        <el-menu-item index="/tickets">工单</el-menu-item>
      </el-menu>
    </el-aside>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>
