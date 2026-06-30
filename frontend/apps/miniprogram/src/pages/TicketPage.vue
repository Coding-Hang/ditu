<script setup lang="ts">
import { apiClient } from '@ditu/api-client'
import type { TicketDto } from '@ditu/types'
import { ticketStatusLabel } from '@ditu/ui-shared'
import { onMounted, ref } from 'vue'

const tickets = ref<TicketDto[]>([])
const errorMessage = ref('')

onMounted(async () => {
  try {
    tickets.value = (await apiClient.tickets()).records
  } catch {
    errorMessage.value = '工单加载失败'
  }
})
</script>

<template>
  <section class="page">
    <h3>工单</h3>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    <div v-for="ticket in tickets" :key="ticket.id" class="card">
      <strong>{{ ticket.title }}</strong>
      <p>{{ ticketStatusLabel(ticket.status) }}</p>
      <p>{{ ticket.content }}</p>
    </div>
  </section>
</template>
