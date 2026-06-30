<script setup lang="ts">
import { apiClient } from '@ditu/api-client'
import type { CustomerServiceProfileDto } from '@ditu/types'
import { onMounted, ref } from 'vue'

const profiles = ref<CustomerServiceProfileDto[]>([])
const errorMessage = ref('')

onMounted(async () => {
  try {
    profiles.value = await apiClient.customerServices()
  } catch {
    errorMessage.value = '客服信息加载失败'
  }
})
</script>

<template>
  <section class="page">
    <h3>专属客服</h3>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    <div v-for="profile in profiles" :key="profile.id" class="card">
      <strong>{{ profile.name }}</strong>
      <p>{{ profile.positioning }}</p>
      <p>{{ profile.intro }}</p>
    </div>
  </section>
</template>
