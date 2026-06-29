<script setup lang="ts">
import { apiClient } from '@ditu/api-client'
import type { TicketDetailDto, TicketDto } from '@ditu/types'
import { ticketStatusLabel } from '@ditu/ui-shared'
import { onMounted, ref } from 'vue'

const tickets = ref<TicketDto[]>([])
const detail = ref<TicketDetailDto | null>(null)
const reply = ref('')
const status = ref('RESOLVED')
const assignedTo = ref(1)

async function load() {
  const page = await apiClient.adminTickets()
  tickets.value = page.records
}

async function open(ticket: TicketDto) {
  detail.value = await apiClient.adminTicketDetail(ticket.id)
}

async function assign() {
  if (detail.value) detail.value = await apiClient.assignTicket(detail.value.ticket.id, assignedTo.value)
}

async function sendReply() {
  if (detail.value) detail.value = await apiClient.adminReplyTicket(detail.value.ticket.id, reply.value)
  reply.value = ''
}

async function changeStatus() {
  if (detail.value) detail.value = await apiClient.changeTicketStatus(detail.value.ticket.id, status.value, '管理端处理')
}

onMounted(load)
</script>

<template>
  <div class="split">
    <div class="panel">
      <el-table :data="tickets" stripe @row-click="open">
        <el-table-column prop="title" label="标题" />
        <el-table-column label="状态"><template #default="{ row }">{{ ticketStatusLabel(row.status) }}</template></el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" />
      </el-table>
    </div>
    <div class="panel" v-if="detail">
      <h3>{{ detail.ticket.title }}</h3>
      <p>{{ detail.ticket.content }}</p>
      <el-timeline>
        <el-timeline-item v-for="message in detail.messages" :key="message.id" :timestamp="message.createdAt">
          {{ message.senderRole }}：{{ message.content }}
        </el-timeline-item>
      </el-timeline>
      <div class="toolbar"><el-input-number v-model="assignedTo" :min="1" /><el-button @click="assign">分配</el-button></div>
      <el-input v-model="reply" type="textarea" :rows="3" />
      <div class="toolbar">
        <el-button type="primary" @click="sendReply">回复</el-button>
        <el-select v-model="status"><el-option value="PROCESSING" /><el-option value="RESOLVED" /><el-option value="CLOSED" /></el-select>
        <el-button @click="changeStatus">变更状态</el-button>
      </div>
    </div>
  </div>
</template>
