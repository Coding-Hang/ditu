<script setup lang="ts">
import { ApiError, apiClient } from '@ditu/api-client'
import type { ConversationDto, MessageDto } from '@ditu/types'
import { ref } from 'vue'

const conversation = ref<ConversationDto | null>(null)
const messages = ref<MessageDto[]>([])
const input = ref('')
const streaming = ref('')
const sending = ref(false)
const errorMessage = ref('')

async function ensureConversation() {
  if (!conversation.value) conversation.value = await apiClient.createConversation('小程序咨询')
}

async function send() {
  if (!input.value.trim() || sending.value) return
  sending.value = true
  errorMessage.value = ''
  const content = input.value
  input.value = ''
  try {
    await ensureConversation()
    if (!conversation.value) return
    const run = await apiClient.sendMessage(conversation.value.id, content)
    streaming.value = ''
    // 小程序端同样按 SSE 事件语义处理增量、完成和次数事件，断线补发由后端 Last-Event-ID 支撑。
    await apiClient.streamRun(conversation.value.id, run.runId, event => {
      if (event.event === 'message.delta') streaming.value += String(event.data.delta ?? '')
      if (event.event === 'message.done') streaming.value = String(event.data.content ?? streaming.value)
    })
    const page = await apiClient.messages(conversation.value.id)
    messages.value = page.records
  } catch (error) {
    input.value = content
    errorMessage.value = error instanceof ApiError ? error.message : '发送失败，请稍后重试'
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <section class="page">
    <h3>智能会话</h3>
    <p class="hint">面向商标、专利、版权与维权场景的连续咨询。</p>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    <div v-for="message in messages" :key="message.id" class="card" :class="{ assistant: message.role === 'ASSISTANT' }">
      <strong>{{ message.role === 'USER' ? '我' : '滴兔' }}</strong>
      <p>{{ message.content }}</p>
    </div>
    <div v-if="streaming" class="card assistant"><strong>滴兔</strong><p>{{ streaming }}</p></div>
    <textarea v-model="input" rows="4" placeholder="输入知识产权问题" />
    <button class="primary" :disabled="sending" @click="send">{{ sending ? '发送中...' : '发送' }}</button>
  </section>
</template>
