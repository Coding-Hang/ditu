<script setup lang="ts">
import { apiClient } from '@ditu/api-client'
import type { ConversationDto } from '@ditu/types'
import { onMounted, ref } from 'vue'

const conversations = ref<ConversationDto[]>([])

onMounted(async () => {
  conversations.value = (await apiClient.conversations()).records
})
</script>

<template>
  <section class="page">
    <h3>历史</h3>
    <div v-for="conversation in conversations" :key="conversation.id" class="card">
      {{ conversation.title }}<br />{{ conversation.updatedAt }}
    </div>
  </section>
</template>
