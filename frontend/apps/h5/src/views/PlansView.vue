<script setup lang="ts">
import { apiClient } from '@ditu/api-client'
import type { PlanDto } from '@ditu/types'
import { planTone } from '@ditu/ui-shared'
import { onMounted, ref } from 'vue'
import { useAuthStore } from '../stores/auth'

const plans = ref<PlanDto[]>([])
const auth = useAuthStore()

onMounted(async () => {
  if (!auth.user && auth.accessToken) await auth.loadMe()
  plans.value = await apiClient.plans()
})
</script>

<template>
  <div class="toolbar">
    <el-tag v-if="auth.user" size="large">当前 {{ auth.user.planCode }}，剩余 {{ auth.user.remainingQuota }} 次</el-tag>
  </div>
  <div class="grid">
    <el-card v-for="plan in plans" :key="plan.code" shadow="never">
      <template #header>
        <el-tag :type="planTone(plan.code)">{{ plan.name }}</el-tag>
      </template>
      <p>{{ plan.description }}</p>
      <p>月度次数 {{ plan.monthlyQuota }}</p>
      <el-check-tag :checked="plan.ragEnabled">专属知识库</el-check-tag>
      <el-check-tag :checked="plan.prioritySupport">优先客服</el-check-tag>
      <ul>
        <li v-for="benefit in plan.benefits" :key="benefit">{{ benefit }}</li>
      </ul>
    </el-card>
  </div>
</template>
