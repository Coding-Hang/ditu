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
const loading = ref(false)
const sending = ref(false)
const activeConversationId = computed(() => Number(route.params.conversationId || chat.conversations[0]?.id || 0))

onMounted(async () => {
  loading.value = true
  try {
    if (!auth.user && auth.accessToken) await auth.loadMe()
    await chat.loadConversations()
    if (activeConversationId.value) await chat.loadMessages(activeConversationId.value)
  } catch {
    // 接口层已经统一派发错误提示，这里只负责结束页面 loading 状态。
  } finally {
    loading.value = false
  }
})

async function newConversation() {
  try {
    const conversation = await chat.createConversation('知识产权咨询')
    await router.push(`/chat/${conversation.id}`)
    await chat.loadMessages(conversation.id)
  } catch {
    // 全局 API 错误监听会给出用户可见提示。
  }
}

async function ensureConversation() {
  if (activeConversationId.value) return activeConversationId.value
  const conversation = await chat.createConversation('知识产权咨询')
  await router.push(`/chat/${conversation.id}`)
  return conversation.id
}

async function send() {
  if (!input.value.trim() || sending.value) return
  sending.value = true
  const content = input.value
  input.value = ''
  try {
    const conversationId = await ensureConversation()
    await chat.send(conversationId, content)
  } catch {
    input.value = content
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <div class="toolbar">
    <div>
      <h2>智能会话</h2>
      <p>面向商标、专利、版权、维权与知识产权运营的连续咨询。</p>
    </div>
    <div class="toolbar-actions">
      <el-tag v-if="auth.user" effect="plain">剩余 {{ auth.user.remainingQuota }} 次</el-tag>
      <el-button type="primary" @click="newConversation">新会话</el-button>
    </div>
  </div>
  <div v-loading="loading" class="panel chat-panel">
    <el-empty v-if="!chat.messages.length && !chat.streamingText" description="输入问题后，滴兔会自动创建会话并保留记录。" />
    <div v-for="message in chat.messages" :key="message.id" class="message" :class="{ assistant: message.role === 'ASSISTANT' }">
      <strong>{{ message.role === 'USER' ? '我' : '滴兔' }}</strong>
      <p>{{ message.content }}</p>
    </div>
    <div v-if="chat.streamingText" class="message assistant">
      <strong>滴兔</strong>
      <p>{{ chat.streamingText }}</p>
    </div>
    <el-alert v-if="chat.ragSnippets.length" type="success" :closable="false" title="已命中知识库片段">
      <p v-for="snippet in chat.ragSnippets" :key="snippet">{{ snippet }}</p>
    </el-alert>
    <div class="composer">
      <el-input v-model="input" type="textarea" :rows="4" placeholder="输入知识产权问题，例如：商标被抢注后如何处理？" @keydown.ctrl.enter.prevent="send" />
      <el-button type="primary" :loading="sending" @click="send">发送</el-button>
    </div>
  </div>
</template>

<style scoped>
.composer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 96px;
  gap: 12px;
  align-items: end;
  margin-top: 16px;
}
</style>
