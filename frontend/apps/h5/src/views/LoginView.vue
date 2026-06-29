<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const username = ref('demo')
const password = ref('Demo123!')
const loading = ref(false)

async function submit() {
  loading.value = true
  try {
    await auth.login(username.value, password.value)
    await router.push('/chat')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="panel login">
    <h2>用户登录</h2>
    <el-form label-position="top" @submit.prevent="submit">
      <el-form-item label="账号">
        <el-input v-model="username" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="password" type="password" show-password />
      </el-form-item>
      <el-button type="primary" :loading="loading" @click="submit">登录</el-button>
    </el-form>
  </div>
</template>

<style scoped>
.login {
  max-width: 420px;
  margin: 8vh auto;
}
</style>
