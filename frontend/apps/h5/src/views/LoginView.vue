<script setup lang="ts">
import { ApiError } from '@ditu/api-client'
import { DITU_BRAND } from '@ditu/ui-shared'
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const username = ref('demo')
const password = ref('Demo123!')
const loading = ref(false)
const errorMessage = ref('')

async function submit() {
  if (loading.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    await auth.login(username.value.trim(), password.value)
    await router.push(String(route.query.redirect || '/chat'))
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
        <h1>{{ DITU_BRAND.name }}</h1>
        <span>让知识产权咨询、知识库检索与专属服务形成一条可追踪的业务链路。</span>
      </div>
    </div>
    <div class="login-panel">
      <h2>用户工作台登录</h2>
      <p>进入滴兔智能体，继续知识产权咨询和工单跟进。</p>
      <el-alert v-if="errorMessage" type="error" :closable="false" :title="errorMessage" />
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="账号">
          <el-input v-model="username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" autocomplete="current-password" show-password @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" class="submit-button" @click="submit">登录</el-button>
      </el-form>
    </div>
  </section>
</template>
