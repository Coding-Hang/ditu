<script setup lang="ts">
import { apiClient } from '@ditu/api-client'
import type { ConversationDto, MessageDto } from '@ditu/types'
import { ref } from 'vue'

const conversation = ref<ConversationDto | null>(null)
const messages = ref<MessageDto[]>([])
const input = ref('')
const streaming = ref('')

async function ensureConversation() {
  if (!conversation.value) conversation.value = await apiClient.createConversation('小程序咨询')
}

async function send() {
  await ensureConversation()
  if (!conversation.value || !input.value.trim()) return
  const content = input.value
  input.value = ''
  const run = await apiClient.sendMessage(conversation.value.id, content)
  streaming.value = ''
  // 小程序端同样按 SSE 事件语义处理增量、完成和次数事件，断线补发由后端 Last-Event-ID 支撑。
  await apiClient.streamRun(conversation.value.id, run.runId, event => {
    if (event.event === 'message.delta') streaming.value += String(event.data.delta ?? '')
    if (event.event === 'message.done') streaming.value = String(event.data.content ?? streaming.value)
  })
  const page = await apiClient.messages(conversation.value.id)
  messages.value = page.records
}
</script>

<template>
  <section class="page">
    <h3>会话</h3>
    <div v-for="message in messages" :key="message.id" class="card">
      {{ message.role }}：{{ message.content }}
    </div>
    <div v-if="streaming" class="card">ASSISTANT：{{ streaming }}</div>
    <textarea v-model="input" rows="4" placeholder="输入问题" />
    <button class="primary" @click="send">发送</button>
  </section>
</template>
