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
  try {
    const page = await apiClient.adminTickets()
    tickets.value = page.records
  } catch {
    // 全局 API 错误监听负责提示，列表保留上一次成功结果。
  }
}

async function open(ticket: TicketDto) {
  try {
    detail.value = await apiClient.adminTicketDetail(ticket.id)
  } catch {
    // 详情读取失败时不清空当前详情。
  }
}

async function assign() {
  try {
    if (detail.value) detail.value = await apiClient.assignTicket(detail.value.ticket.id, assignedTo.value)
  } catch {
    // 分配失败时保留当前处理人输入。
  }
}

async function sendReply() {
  try {
    if (detail.value) detail.value = await apiClient.adminReplyTicket(detail.value.ticket.id, reply.value)
    reply.value = ''
  } catch {
    // 回复失败时保留输入内容。
  }
}

async function changeStatus() {
  try {
    if (detail.value) detail.value = await apiClient.changeTicketStatus(detail.value.ticket.id, status.value, '管理端处理')
  } catch {
    // 状态变更失败时保留当前详情。
  }
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
