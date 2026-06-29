<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useChatStore } from '../stores/chat'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const chat = useChatStore()
const input = ref('')
const activeConversationId = computed(() => Number(route.params.conversationId || chat.conversations[0]?.id || 0))

onMounted(async () => {
  if (!auth.user && auth.accessToken) await auth.loadMe()
  await chat.loadConversations()
  if (activeConversationId.value) await chat.loadMessages(activeConversationId.value)
})

async function newConversation() {
  const conversation = await chat.createConversation('知识产权咨询')
  await router.push(`/chat/${conversation.id}`)
}

async function send() {
  if (!activeConversationId.value || !input.value.trim()) return
  const content = input.value
  input.value = ''
  await chat.send(activeConversationId.value, content)
}
</script>

<template>
  <div class="toolbar">
    <el-button type="primary" @click="newConversation">新会话</el-button>
    <el-tag v-if="auth.user">剩余 {{ auth.user.remainingQuota }} 次</el-tag>
  </div>
  <div class="panel">
    <div v-for="message in chat.messages" :key="message.id" class="message" :class="{ assistant: message.role === 'ASSISTANT' }">
      <strong>{{ message.role === 'USER' ? '我' : '滴兔' }}</strong>
      <p>{{ message.content }}</p>
    </div>
    <div v-if="chat.streamingText" class="message assistant">
      <strong>滴兔</strong>
      <p>{{ chat.streamingText }}</p>
    </div>
    <el-alert v-if="chat.ragSnippets.length" type="success" :closable="false">
      <p v-for="snippet in chat.ragSnippets" :key="snippet">{{ snippet }}</p>
    </el-alert>
    <div class="composer">
      <el-input v-model="input" type="textarea" :rows="4" placeholder="输入知识产权问题" />
      <el-button type="primary" @click="send">发送</el-button>
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
