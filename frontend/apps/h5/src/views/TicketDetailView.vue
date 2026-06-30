<script setup lang="ts">
import { apiClient } from '@ditu/api-client'
import type { TicketDetailDto, TicketDto } from '@ditu/types'
import { ticketStatusLabel } from '@ditu/ui-shared'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const tickets = ref<TicketDto[]>([])
const detail = ref<TicketDetailDto | null>(null)
const reply = ref('')

async function load() {
  try {
    const page = await apiClient.tickets()
    tickets.value = page.records
    const id = Number(route.params.ticketId || tickets.value[0]?.id || 0)
    if (id) detail.value = await apiClient.ticketDetail(id)
  } catch {
    // 全局 API 错误监听负责提示；工单页保持当前内容。
  }
}

async function sendReply() {
  if (!detail.value || !reply.value.trim()) return
  try {
    detail.value = await apiClient.replyTicket(detail.value.ticket.id, reply.value)
    reply.value = ''
  } catch {
    // 回复失败时保留输入内容，方便用户重试。
  }
}

async function selectTicket(id: number) {
  try {
    await router.push(`/tickets/${id}`)
    detail.value = await apiClient.ticketDetail(id)
  } catch {
    // 全局 API 错误监听负责提示。
  }
}

onMounted(load)
</script>

<template>
  <div class="toolbar">
    <el-select :model-value="detail?.ticket.id" placeholder="选择工单" @change="selectTicket">
      <el-option v-for="ticket in tickets" :key="ticket.id" :value="ticket.id" :label="ticket.title" />
    </el-select>
  </div>
  <div v-if="detail" class="panel">
    <h3>{{ detail.ticket.title }}</h3>
    <el-tag>{{ ticketStatusLabel(detail.ticket.status) }}</el-tag>
    <div v-for="message in detail.messages" :key="message.id" class="message">
      <strong>{{ message.senderRole }}</strong>
      <p>{{ message.content }}</p>
    </div>
    <div class="composer">
      <el-input v-model="reply" type="textarea" :rows="3" />
      <el-button type="primary" :disabled="detail.ticket.status === 'CLOSED'" @click="sendReply">回复</el-button>
    </div>
  </div>
</template>

<style scoped>
.composer {
  display: grid;
  grid-template-columns: 1fr 96px;
  gap: 12px;
  align-items: end;
  margin-top: 16px;
}
</style>
