<script setup lang="ts">
import { apiClient } from '@ditu/api-client'
import type { CustomerServiceProfileDto } from '@ditu/types'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const profiles = ref<CustomerServiceProfileDto[]>([])
const router = useRouter()

onMounted(async () => {
  try {
    profiles.value = await apiClient.customerServices()
  } catch {
    // 全局 API 错误监听负责给用户提示。
  }
})

async function create(profile: CustomerServiceProfileDto) {
  try {
    const detail = await apiClient.createTicket({
      serviceProfileId: profile.id,
      title: `${profile.roleName}人工服务`,
      content: `请 ${profile.name} 协助处理。`
    })
    await router.push(`/tickets/${detail.ticket.id}`)
  } catch {
    // 创建失败时保留当前页面，错误由全局监听展示。
  }
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
