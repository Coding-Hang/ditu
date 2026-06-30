<script setup lang="ts">
import { apiClient } from '@ditu/api-client'
import type { PlanDto } from '@ditu/types'
import { onMounted, ref } from 'vue'

const plans = ref<PlanDto[]>([])

onMounted(async () => {
  try {
    plans.value = await apiClient.plans(true)
  } catch {
    // 全局 API 错误监听负责提示。
  }
})
</script>

<template>
  <div class="panel">
    <el-table :data="plans" stripe>
      <el-table-column prop="code" label="编码" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="monthlyQuota" label="月度次数" />
      <el-table-column prop="ragEnabled" label="专属知识库" />
      <el-table-column prop="prioritySupport" label="优先客服" />
    </el-table>
  </div>
</template>
