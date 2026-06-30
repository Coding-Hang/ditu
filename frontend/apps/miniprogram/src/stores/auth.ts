import { apiClient } from '@ditu/api-client'
import type { UserSummary } from '@ditu/types'
import { defineStore } from 'pinia'

export const useMiniAuthStore = defineStore('mini-auth', {
  state: () => ({
    accessToken: localStorage.getItem('ditu.mini.accessToken') ?? '',
    refreshToken: localStorage.getItem('ditu.mini.refreshToken') ?? '',
    user: null as UserSummary | null
  }),
  actions: {
    hydrateClient() {
      // 小程序端也复用 Bearer Token；真实小程序适配时可把 localStorage 换成平台 storage API。
      apiClient.setTokens(this.accessToken ? { accessToken: this.accessToken, refreshToken: this.refreshToken } : null)
    },
    async login(username: string, password: string) {
      const result = await apiClient.login(username, password)
      this.accessToken = result.accessToken
      this.refreshToken = result.refreshToken
      this.user = result.user
      localStorage.setItem('ditu.mini.accessToken', this.accessToken)
      localStorage.setItem('ditu.mini.refreshToken', this.refreshToken)
      this.hydrateClient()
    },
    logout() {
      this.accessToken = ''
      this.refreshToken = ''
      this.user = null
      localStorage.removeItem('ditu.mini.accessToken')
      localStorage.removeItem('ditu.mini.refreshToken')
      apiClient.setTokens(null)
    }
  }
})
