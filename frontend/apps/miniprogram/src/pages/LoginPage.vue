<script setup lang="ts">
import { ref } from 'vue'
import { useMiniAuthStore } from '../stores/auth'

const emit = defineEmits<{ 'logged-in': [] }>()
const auth = useMiniAuthStore()
const username = ref('demo')
const password = ref('Demo123!')

async function login() {
  await auth.login(username.value, password.value)
  emit('logged-in')
}
</script>

<template>
  <section class="page">
    <h2>滴兔智能体</h2>
    <input v-model="username" placeholder="账号" />
    <input v-model="password" placeholder="密码" type="password" />
    <button class="primary" @click="login">登录</button>
  </section>
</template>
