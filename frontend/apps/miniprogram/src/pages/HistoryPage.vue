<script setup lang="ts">
import { apiClient } from '@ditu/api-client'
import type { ConversationDto } from '@ditu/types'
import { onMounted, ref } from 'vue'

const conversations = ref<ConversationDto[]>([])
const errorMessage = ref('')

onMounted(async () => {
  try {
    conversations.value = (await apiClient.conversations()).records
  } catch {
    errorMessage.value = '历史记录加载失败'
  }
})
</script>

<template>
  <section class="page">
    <h3>历史</h3>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    <div v-for="conversation in conversations" :key="conversation.id" class="card">
      {{ conversation.title }}<br />{{ conversation.updatedAt }}
    </div>
  </section>
</template>
