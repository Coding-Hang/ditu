<script setup lang="ts">
import { apiClient } from '@ditu/api-client'
import type { CustomerServiceProfileDto } from '@ditu/types'
import { onMounted, ref } from 'vue'

const profiles = ref<CustomerServiceProfileDto[]>([])

onMounted(async () => {
  profiles.value = await apiClient.customerServices()
})
</script>

<template>
  <section class="page">
    <h3>专属客服</h3>
    <div v-for="profile in profiles" :key="profile.id" class="card">
      <strong>{{ profile.name }}</strong>
      <p>{{ profile.positioning }}</p>
      <p>{{ profile.intro }}</p>
    </div>
  </section>
</template>
