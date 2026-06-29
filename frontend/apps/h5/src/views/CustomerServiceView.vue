<script setup lang="ts">
import { apiClient } from '@ditu/api-client'
import type { CustomerServiceProfileDto } from '@ditu/types'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const profiles = ref<CustomerServiceProfileDto[]>([])
const router = useRouter()

onMounted(async () => {
  profiles.value = await apiClient.customerServices()
})

async function create(profile: CustomerServiceProfileDto) {
  const detail = await apiClient.createTicket({
    serviceProfileId: profile.id,
    title: `${profile.roleName}人工服务`,
    content: `请 ${profile.name} 协助处理。`
  })
  await router.push(`/tickets/${detail.ticket.id}`)
}
</script>

<template>
  <div class="grid">
    <el-card v-for="profile in profiles" :key="profile.id" shadow="never">
      <template #header>{{ profile.name }}</template>
      <p>{{ profile.roleName }}</p>
      <p>{{ profile.positioning }}</p>
      <p>{{ profile.intro }}</p>
      <el-button type="primary" @click="create(profile)">创建工单</el-button>
    </el-card>
  </div>
</template>
