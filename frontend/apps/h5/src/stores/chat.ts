import { apiClient } from '@ditu/api-client'
import type { ConversationDto, MessageDto, SseEvent } from '@ditu/types'
import { defineStore } from 'pinia'

interface ChatState {
  conversations: ConversationDto[]
  messages: MessageDto[]
  streamingText: string
  ragSnippets: string[]
}

export const useChatStore = defineStore('chat', {
  state: (): ChatState => ({
    conversations: [],
    messages: [],
    streamingText: '',
    ragSnippets: []
  }),
  actions: {
    async loadConversations() {
      const page = await apiClient.conversations()
      this.conversations = page.records
    },
    async loadMessages(conversationId: number) {
      const page = await apiClient.messages(conversationId)
      this.messages = page.records
    },
    async createConversation(title: string) {
      const conversation = await apiClient.createConversation(title)
      await this.loadConversations()
      return conversation
    },
    async send(conversationId: number, content: string) {
      const run = await apiClient.sendMessage(conversationId, content)
      this.streamingText = ''
      this.ragSnippets = []
      // SSE 事件按后端 sequence_no 顺序处理；delta 追加展示，done 覆盖最终文本，RAG 摘要单独显示。
      await apiClient.streamRun(conversationId, run.runId, (event: SseEvent) => {
        if (event.event === 'message.delta') {
          this.streamingText += String(event.data.delta ?? '')
        }
        if (event.event === 'message.done') {
          this.streamingText = String(event.data.content ?? this.streamingText)
        }
        if (event.event === 'rag.context') {
          const chunks = (event.data.chunks ?? []) as Array<{ snippet: string }>
          this.ragSnippets = chunks.map(chunk => chunk.snippet)
        }
      })
      await this.loadMessages(conversationId)
    }
  }
})
