<script setup lang="ts">
import { apiClient } from '@ditu/api-client'
import type { TicketDto } from '@ditu/types'
import { ticketStatusLabel } from '@ditu/ui-shared'
import { onMounted, ref } from 'vue'

const tickets = ref<TicketDto[]>([])

onMounted(async () => {
  tickets.value = (await apiClient.tickets()).records
})
</script>

<template>
  <section class="page">
    <h3>工单</h3>
    <div v-for="ticket in tickets" :key="ticket.id" class="card">
      <strong>{{ ticket.title }}</strong>
      <p>{{ ticketStatusLabel(ticket.status) }}</p>
      <p>{{ ticket.content }}</p>
    </div>
  </section>
</template>
