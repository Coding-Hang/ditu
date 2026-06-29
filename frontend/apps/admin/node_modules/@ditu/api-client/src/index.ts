import type {
  ApiResponse,
  ConversationDto,
  CustomerServiceProfileDto,
  LoginResponse,
  MessageDto,
  ModelConfigDto,
  PageResponse,
  PlanDto,
  QuotaLedgerDto,
  RagCollectionDto,
  RagDocumentDto,
  SendMessageResponse,
  SseEvent,
  TicketDetailDto,
  TicketDto,
  UserSummary
} from '@ditu/types'

export class ApiError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly status: number
  ) {
    super(message)
  }
}

export interface TokenStore {
  accessToken: string
  refreshToken: string
}

type EventHandler = (event: SseEvent) => void

export class DituApiClient {
  private tokenStore: TokenStore | null = null

  constructor(private readonly baseUrl = resolveBaseUrl()) {}

  setTokens(tokens: TokenStore | null) {
    this.tokenStore = tokens
  }

  async login(username: string, password: string) {
    return this.request<LoginResponse>('/api/v1/auth/login', { method: 'POST', body: { username, password }, auth: false })
  }

  async me() {
    return this.request<UserSummary>('/api/v1/auth/me')
  }

  async plans(admin = false) {
    return this.request<PlanDto[]>(admin ? '/api/v1/admin/plans' : '/api/v1/plans')
  }

  async conversations(page = 1, pageSize = 20) {
    return this.request<PageResponse<ConversationDto>>(`/api/v1/conversations?page=${page}&pageSize=${pageSize}`)
  }

  async createConversation(title: string) {
    return this.request<ConversationDto>('/api/v1/conversations', { method: 'POST', body: { title } })
  }

  async messages(conversationId: number, page = 1, pageSize = 50) {
    return this.request<PageResponse<MessageDto>>(`/api/v1/conversations/${conversationId}/messages?page=${page}&pageSize=${pageSize}`)
  }

  async sendMessage(conversationId: number, content: string) {
    return this.request<SendMessageResponse>(`/api/v1/conversations/${conversationId}/messages`, {
      method: 'POST',
      body: { content, clientMessageId: crypto.randomUUID() }
    })
  }

  async customerServices() {
    return this.request<CustomerServiceProfileDto[]>('/api/v1/customer-services')
  }

  async createTicket(payload: { serviceProfileId?: number; conversationId?: number; title: string; content: string }) {
    return this.request<TicketDetailDto>('/api/v1/tickets', { method: 'POST', body: payload })
  }

  async tickets(page = 1, pageSize = 20) {
    return this.request<PageResponse<TicketDto>>(`/api/v1/tickets?page=${page}&pageSize=${pageSize}`)
  }

  async ticketDetail(ticketId: number) {
    return this.request<TicketDetailDto>(`/api/v1/tickets/${ticketId}`)
  }

  async replyTicket(ticketId: number, content: string) {
    return this.request<TicketDetailDto>(`/api/v1/tickets/${ticketId}/messages`, { method: 'POST', body: { content } })
  }

  async adminUsers(params = '') {
    return this.request<PageResponse<UserSummary>>(`/api/v1/admin/users${params}`)
  }

  async adminCreateUser(payload: Record<string, unknown>) {
    return this.request<UserSummary>('/api/v1/admin/users', { method: 'POST', body: payload })
  }

  async adminDisableUser(userId: number) {
    return this.request<UserSummary>(`/api/v1/admin/users/${userId}/disable`, { method: 'POST' })
  }

  async adminEnableUser(userId: number) {
    return this.request<UserSummary>(`/api/v1/admin/users/${userId}/enable`, { method: 'POST' })
  }

  async changeUserPlan(userId: number, planCode: string, reason: string) {
    return this.request<UserSummary>(`/api/v1/admin/users/${userId}/plan`, { method: 'POST', body: { planCode, reason } })
  }

  async quotaLedgers(userId: number) {
    return this.request<PageResponse<QuotaLedgerDto>>(`/api/v1/admin/users/${userId}/quota-ledgers`)
  }

  async adjustQuota(userId: number, deltaCount: number, reason: string) {
    return this.request(`/api/v1/admin/users/${userId}/quota-adjustments`, { method: 'POST', body: { deltaCount, reason } })
  }

  async modelConfig(userId: number) {
    return this.request<ModelConfigDto>(`/api/v1/admin/users/${userId}/model-config`)
  }

  async saveModelConfig(userId: number, payload: Record<string, unknown>) {
    return this.request<ModelConfigDto>(`/api/v1/admin/users/${userId}/model-config`, { method: 'PUT', body: payload })
  }

  async testModelConfig(userId: number) {
    return this.request<ModelConfigDto>(`/api/v1/admin/users/${userId}/model-config/test`, { method: 'POST', body: { message: 'ping' } })
  }

  async disableModelConfig(userId: number) {
    return this.request(`/api/v1/admin/users/${userId}/model-config/disable`, { method: 'POST' })
  }

  async adminTickets(params = '') {
    return this.request<PageResponse<TicketDto>>(`/api/v1/admin/tickets${params}`)
  }

  async adminTicketDetail(ticketId: number) {
    return this.request<TicketDetailDto>(`/api/v1/admin/tickets/${ticketId}`)
  }

  async assignTicket(ticketId: number, assignedTo: number) {
    return this.request<TicketDetailDto>(`/api/v1/admin/tickets/${ticketId}/assign`, { method: 'POST', body: { assignedTo } })
  }

  async changeTicketStatus(ticketId: number, status: string, reason: string) {
    return this.request<TicketDetailDto>(`/api/v1/admin/tickets/${ticketId}/status`, { method: 'POST', body: { status, reason } })
  }

  async adminReplyTicket(ticketId: number, content: string) {
    return this.request<TicketDetailDto>(`/api/v1/admin/tickets/${ticketId}/messages`, { method: 'POST', body: { content } })
  }

  async ragCollections() {
    return this.request<RagCollectionDto[]>('/api/v1/admin/rag/collections')
  }

  async createRagCollection(payload: Record<string, unknown>) {
    return this.request<RagCollectionDto>('/api/v1/admin/rag/collections', { method: 'POST', body: payload })
  }

  async ragDocuments(collectionId: number) {
    return this.request<PageResponse<RagDocumentDto>>(`/api/v1/admin/rag/collections/${collectionId}/documents`)
  }

  async disableRagDocument(documentId: number) {
    return this.request(`/api/v1/admin/rag/documents/${documentId}/disable`, { method: 'POST' })
  }

  async reindexRagDocument(documentId: number) {
    return this.request(`/api/v1/admin/rag/documents/${documentId}/reindex`, { method: 'POST' })
  }

  async uploadRagDocument(collectionId: number, file: File, metadata = '{}') {
    const form = new FormData()
    form.append('file', file)
    form.append('metadata', metadata)
    return this.raw<RagDocumentDto>(`/api/v1/admin/rag/collections/${collectionId}/documents`, {
      method: 'POST',
      body: form
    })
  }

  async streamRun(conversationId: number, runId: number, onEvent: EventHandler, lastEventId?: string) {
    // EventSource 不能设置 Authorization 头，所以用 fetch 读取 text/event-stream，确保 SSE 也受 Token 保护。
    const response = await fetch(`${this.baseUrl}/api/v1/conversations/${conversationId}/runs/${runId}/events`, {
      headers: this.sseHeaders(lastEventId)
    })
    if (!response.ok || !response.body) {
      throw new ApiError('SSE_FAILED', '流式连接失败', response.status)
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    for (;;) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const chunks = buffer.split('\n\n')
      buffer = chunks.pop() ?? ''
      chunks.map(parseSseChunk).filter(Boolean).forEach(event => onEvent(event as SseEvent))
    }
  }

  private async request<T>(path: string, options: { method?: string; body?: unknown; auth?: boolean } = {}) {
    return this.raw<T>(path, {
      method: options.method ?? 'GET',
      headers: { 'Content-Type': 'application/json' },
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      auth: options.auth
    })
  }

  private async raw<T>(path: string, init: RequestInit & { auth?: boolean } = {}) {
    const { auth, ...requestInit } = init
    const headers = new Headers(requestInit.headers)
    if (!(requestInit.body instanceof FormData)) {
      headers.set('Content-Type', 'application/json')
    }
    if (auth !== false && this.tokenStore?.accessToken) {
      headers.set('Authorization', `Bearer ${this.tokenStore.accessToken}`)
    }
    const response = await fetch(`${this.baseUrl}${path}`, {
      ...requestInit,
      headers
    })
    const json = (await response.json()) as ApiResponse<T>
    if (!response.ok || !json.success) {
      // Token 失效、次数不足、权限不足等稳定错误码在这里统一抛给 Pinia Store 和页面处理。
      throw new ApiError(json.code, json.message, response.status)
    }
    return json.data
  }

  private authHeaders(): Record<string, string> {
    return this.tokenStore?.accessToken ? { Authorization: `Bearer ${this.tokenStore.accessToken}` } : {}
  }

  private sseHeaders(lastEventId?: string): Headers {
    const headers = new Headers(this.authHeaders())
    if (lastEventId) {
      headers.set('Last-Event-ID', lastEventId)
    }
    return headers
  }
}

function resolveBaseUrl() {
  const meta = import.meta as ImportMeta & { env?: { VITE_API_BASE_URL?: string } }
  return meta.env?.VITE_API_BASE_URL ?? 'http://localhost:8080'
}

function parseSseChunk(chunk: string): SseEvent | null {
  const lines = chunk.split('\n')
  const event = lines.find(line => line.startsWith('event:'))?.slice(6).trim()
  const id = lines.find(line => line.startsWith('id:'))?.slice(3).trim()
  const data = lines.find(line => line.startsWith('data:'))?.slice(5).trim()
  if (!event || !id || !data) return null
  return { event, id, data: JSON.parse(data) as Record<string, unknown> }
}

export const apiClient = new DituApiClient()
