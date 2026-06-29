<script setup lang="ts">
import { apiClient } from '@ditu/api-client'
import type { ModelConfigDto } from '@ditu/types'
import { reactive, ref } from 'vue'

const userId = ref(1)
const config = ref<ModelConfigDto | null>(null)
const form = reactive({
  configName: '客户自有模型',
  providerCode: 'CUSTOM',
  baseUrl: 'https://llm.example.com/v1',
  modelName: 'customer-model',
  authType: 'API_KEY',
  apiKey: '',
  enabled: true
})

async function load() {
  config.value = await apiClient.modelConfig(userId.value)
  Object.assign(form, { ...config.value, apiKey: '' })
}

async function save() {
  // 密钥输入框只在提交时发送，查询结果永不回显明文，避免管理端页面泄露模型密钥。
  config.value = await apiClient.saveModelConfig(userId.value, form)
}

async function test() {
  config.value = await apiClient.testModelConfig(userId.value)
}

async function disable() {
  await apiClient.disableModelConfig(userId.value)
  await load()
}
</script>

<template>
  <div class="split">
    <div class="panel">
      <div class="toolbar">
        <el-input-number v-model="userId" :min="1" />
        <el-button @click="load">查询</el-button>
      </div>
      <el-form label-position="top">
        <el-form-item label="配置名"><el-input v-model="form.configName" /></el-form-item>
        <el-form-item label="接口地址"><el-input v-model="form.baseUrl" /></el-form-item>
        <el-form-item label="模型名"><el-input v-model="form.modelName" /></el-form-item>
        <el-form-item label="鉴权"><el-select v-model="form.authType"><el-option value="NONE" /><el-option value="API_KEY" /><el-option value="BEARER" /></el-select></el-form-item>
        <el-form-item label="密钥"><el-input v-model="form.apiKey" type="password" show-password /></el-form-item>
        <el-checkbox v-model="form.enabled">启用</el-checkbox>
        <div class="toolbar">
          <el-button type="primary" @click="save">保存</el-button>
          <el-button @click="test">测试</el-button>
          <el-button @click="disable">停用</el-button>
        </div>
      </el-form>
    </div>
    <div class="panel">
      <h3>测试状态</h3>
      <p>{{ config?.lastTestStatus ?? '未查询' }}</p>
      <p>{{ config?.lastTestMessage }}</p>
      <p>密钥已保存：{{ config?.hasApiKey ? '是' : '否' }}</p>
    </div>
  </div>
</template>
