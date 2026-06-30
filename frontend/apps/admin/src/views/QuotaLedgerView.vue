<script setup lang="ts">
import { apiClient } from '@ditu/api-client'
import type { QuotaLedgerDto } from '@ditu/types'
import { ref } from 'vue'

const userId = ref(1)
const delta = ref(100)
const reason = ref('线下购买次数')
const ledgers = ref<QuotaLedgerDto[]>([])

async function load() {
  try {
    const page = await apiClient.quotaLedgers(userId.value)
    ledgers.value = page.records
  } catch {
    // 全局 API 错误监听负责提示，流水保留上一次成功结果。
  }
}

async function adjust() {
  try {
    await apiClient.adjustQuota(userId.value, delta.value, reason.value)
    await load()
  } catch {
    // 调整失败时不刷新列表，避免误导管理员。
  }
}
</script>

<template>
  <div class="toolbar">
    <el-input-number v-model="userId" :min="1" />
    <el-input-number v-model="delta" />
    <el-input v-model="reason" style="max-width: 260px" />
    <el-button type="primary" @click="adjust">调整次数</el-button>
    <el-button @click="load">查询流水</el-button>
  </div>
  <div class="panel">
    <el-table :data="ledgers" stripe>
      <el-table-column prop="changeType" label="类型" />
      <el-table-column prop="deltaCount" label="变化" />
      <el-table-column prop="afterTotal" label="调整后总次数" />
      <el-table-column prop="afterUsed" label="调整后已用" />
      <el-table-column prop="reason" label="原因" />
      <el-table-column prop="createdAt" label="时间" />
    </el-table>
  </div>
</template>
