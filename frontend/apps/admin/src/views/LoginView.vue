<script setup lang="ts">
import { ApiError } from '@ditu/api-client'
import { DITU_BRAND } from '@ditu/ui-shared'
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminAuthStore } from '../stores/auth'

const auth = useAdminAuthStore()
const route = useRoute()
const router = useRouter()
const username = ref('admin')
const password = ref('Admin123!')
const loading = ref(false)
const errorMessage = ref('')

async function submit() {
  if (loading.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    await auth.login(username.value.trim(), password.value)
    await router.push(String(route.query.redirect || '/users'))
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="login-view">
    <div class="login-brand">
      <span class="brand-mark brand-mark-large" aria-hidden="true"><i /><i /><i /><i /></span>
      <div>
        <p>{{ DITU_BRAND.slogan }}</p>
        <h1>{{ DITU_BRAND.adminName }}</h1>
        <span>统一管理用户、套餐、次数、RAG 知识库、模型链接和客服工单。</span>
      </div>
    </div>
    <div class="login-panel">
      <h2>管理端登录</h2>
      <p>管理员操作会记录审计日志，请使用授权账号进入。</p>
      <el-alert v-if="errorMessage" type="error" :closable="false" :title="errorMessage" />
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="账号"><el-input v-model="username" autocomplete="username" /></el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" autocomplete="current-password" show-password @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" size="large" class="submit-button" :loading="loading" @click="submit">登录</el-button>
      </el-form>
    </div>
  </section>
</template>
