<script setup lang="ts">
import { ApiError } from '@ditu/api-client'
import { DITU_BRAND } from '@ditu/ui-shared'
import { ref } from 'vue'
import { useMiniAuthStore } from '../stores/auth'

const emit = defineEmits<{ 'logged-in': [] }>()
const auth = useMiniAuthStore()
const username = ref('demo')
const password = ref('Demo123!')
const loading = ref(false)
const errorMessage = ref('')

async function login() {
  if (loading.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    await auth.login(username.value.trim(), password.value)
    emit('logged-in')
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="page login-page">
    <h2>{{ DITU_BRAND.appName }}</h2>
    <p>{{ DITU_BRAND.slogan }}</p>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    <input v-model="username" placeholder="账号" autocomplete="username" />
    <input v-model="password" placeholder="密码" type="password" autocomplete="current-password" @keyup.enter="login" />
    <button class="primary" :disabled="loading" @click="login">{{ loading ? '登录中...' : '登录' }}</button>
  </section>
</template>
