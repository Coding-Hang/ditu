<script setup lang="ts">
import { useAdminAuthStore } from './stores/auth'

const auth = useAdminAuthStore()
auth.hydrateClient()
</script>

<template>
  <el-container class="shell">
    <el-aside width="230px" class="nav">
      <div class="brand">滴兔管理端</div>
      <el-menu router default-active="/users">
        <el-menu-item index="/users">用户管理</el-menu-item>
        <el-menu-item index="/quota">次数流水</el-menu-item>
        <el-menu-item index="/models">模型配置</el-menu-item>
        <el-menu-item index="/tickets">工单管理</el-menu-item>
        <el-menu-item index="/rag">RAG 知识库</el-menu-item>
        <el-menu-item index="/plans">套餐配置</el-menu-item>
      </el-menu>
    </el-aside>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>
