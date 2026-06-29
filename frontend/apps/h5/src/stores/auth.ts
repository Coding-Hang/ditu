import { apiClient } from '@ditu/api-client'
import type { UserSummary } from '@ditu/types'
import { defineStore } from 'pinia'

interface AuthState {
  accessToken: string
  refreshToken: string
  user: UserSummary | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    accessToken: localStorage.getItem('ditu.accessToken') ?? '',
    refreshToken: localStorage.getItem('ditu.refreshToken') ?? '',
    user: null
  }),
  actions: {
    hydrateClient() {
      // Token 只保存在前端本地状态和 localStorage，所有权限仍以后端鉴权和数据归属校验为准。
      apiClient.setTokens(this.accessToken ? { accessToken: this.accessToken, refreshToken: this.refreshToken } : null)
    },
    async login(username: string, password: string) {
      const result = await apiClient.login(username, password)
      this.accessToken = result.accessToken
      this.refreshToken = result.refreshToken
      this.user = result.user
      localStorage.setItem('ditu.accessToken', this.accessToken)
      localStorage.setItem('ditu.refreshToken', this.refreshToken)
      this.hydrateClient()
    },
    async loadMe() {
      this.hydrateClient()
      this.user = await apiClient.me()
    },
    logout() {
      this.accessToken = ''
      this.refreshToken = ''
      this.user = null
      localStorage.removeItem('ditu.accessToken')
      localStorage.removeItem('ditu.refreshToken')
      apiClient.setTokens(null)
    }
  }
})
