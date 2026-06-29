// DTO 类型与后端接口契约保持同名字段，三端复用后可以减少模型链接、次数和工单字段漂移。
export interface ApiResponse<T> {
  success: boolean
  code: string
  message: string
  data: T
  requestId: string
}

export interface PageResponse<T> {
  records: T[]
  page: number
  pageSize: number
  total: number
}

export interface UserSummary {
  id: number
  username: string
  displayName: string
  roleCode: 'USER' | 'ADMIN' | 'RAG_ADMIN' | 'CS_MANAGER'
  status: 'ACTIVE' | 'DISABLED' | 'LOCKED'
  planCode: string
  quotaTotal: number
  quotaUsed: number
  remainingQuota: number
  lastLoginAt?: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: UserSummary
}

export interface PlanDto {
  code: string
  name: string
  levelOrder: number
  monthlyQuota: number
  ragEnabled: boolean
  prioritySupport: boolean
  description: string
  benefits: string[]
}

export interface ConversationDto {
  id: number
  userId: number
  title: string
  status: string
  lastMessageAt?: string
  createdAt: string
  updatedAt: string
}

export interface MessageDto {
  id: number
  conversationId: number
  userId: number
  role: 'USER' | 'ASSISTANT' | 'SYSTEM' | 'TOOL'
  sequenceNo: number
  content: string
  agentRunId?: number
  quotaCost: number
  createdAt: string
}

export interface SendMessageResponse {
  messageId: number
  runId: number
  conversationId: number
  quotaReserved: number
}

export interface CustomerServiceProfileDto {
  id: number
  serviceType: string
  name: string
  roleName: string
  positioning: string
  intro: string
  avatarUrl?: string
}

export interface TicketDto {
  id: number
  userId: number
  serviceProfileId?: number
  conversationId?: number
  title: string
  content: string
  status: string
  priority: string
  assignedTo?: number
  lastMessageAt?: string
  closedAt?: string
  createdAt: string
  updatedAt: string
}

export interface TicketMessageDto {
  id: number
  ticketId: number
  senderUserId: number
  senderRole: string
  content: string
  createdAt: string
}

export interface TicketDetailDto {
  ticket: TicketDto
  messages: TicketMessageDto[]
}

export interface ModelConfigDto {
  id: number
  userId: number
  configName: string
  providerCode: string
  baseUrl: string
  modelName: string
  authType: 'NONE' | 'API_KEY' | 'BEARER'
  hasApiKey: boolean
  enabled: boolean
  lastTestStatus: 'UNTESTED' | 'SUCCESS' | 'FAILED'
  lastTestMessage?: string
  lastTestAt?: string
}

export interface QuotaLedgerDto {
  id: number
  userId: number
  changeType: string
  deltaCount: number
  beforeTotal: number
  beforeUsed: number
  afterTotal: number
  afterUsed: number
  reason: string
  createdAt: string
}

export interface RagCollectionDto {
  id: number
  scope: 'GLOBAL' | 'USER'
  ownerUserId?: number
  name: string
  description?: string
  enabled: boolean
}

export interface RagDocumentDto {
  id: number
  collectionId: number
  fileName: string
  mimeType?: string
  checksumSha256: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface SseEvent {
  id: string
  event: string
  data: Record<string, unknown>
}
