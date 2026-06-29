import { apiClient } from '@ditu/api-client'
import type { UserSummary } from '@ditu/types'
import { defineStore } from 'pinia'

interface AdminAuthState {
  accessToken: string
  refreshToken: string
  user: UserSummary | null
}

export const useAdminAuthStore = defineStore('admin-auth', {
  state: (): AdminAuthState => ({
    accessToken: localStorage.getItem('ditu.admin.accessToken') ?? '',
    refreshToken: localStorage.getItem('ditu.admin.refreshToken') ?? '',
    user: null
  }),
  actions: {
    hydrateClient() {
      // 管理端 Token 与用户 H5 分开存储，避免普通用户会话误访问管理端 API。
      apiClient.setTokens(this.accessToken ? { accessToken: this.accessToken, refreshToken: this.refreshToken } : null)
    },
    async login(username: string, password: string) {
      const result = await apiClient.login(username, password)
      this.accessToken = result.accessToken
      this.refreshToken = result.refreshToken
      this.user = result.user
      localStorage.setItem('ditu.admin.accessToken', this.accessToken)
      localStorage.setItem('ditu.admin.refreshToken', this.refreshToken)
      this.hydrateClient()
    }
  }
})
