<script setup lang="ts">
import { ApiError, apiClient } from '@ditu/api-client'
import type { ModelConfigDto } from '@ditu/types'
import { ElMessage } from 'element-plus'
import { computed, reactive, ref } from 'vue'

type AuthType = ModelConfigDto['authType']

const userId = ref(1)
const config = ref<ModelConfigDto | null>(null)
const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const disabling = ref(false)
const lastActionMessage = ref('')
const testPrompt = ref('请仅回复 OK')
const form = reactive({
  configName: '客户自有模型',
  providerCode: 'CUSTOM',
  baseUrl: 'https://llm.example.com/v1',
  modelName: 'customer-model',
  authType: 'API_KEY' as AuthType,
  apiKey: '',
  enabled: true
})

const statusType = computed(() => {
  if (config.value?.lastTestStatus === 'SUCCESS') return 'success'
  if (config.value?.lastTestStatus === 'FAILED') return 'danger'
  return 'info'
})

const statusLabel = computed(() => {
  if (!config.value) return '未查询'
  if (config.value.lastTestStatus === 'SUCCESS') return '测试成功'
  if (config.value.lastTestStatus === 'FAILED') return '测试失败'
  return '未测试'
})

async function load() {
  loading.value = true
  lastActionMessage.value = ''
  try {
    const dto = await apiClient.modelConfig(userId.value)
    applyConfig(dto)
    lastActionMessage.value = '已读取用户模型链接配置'
  } catch (error) {
    config.value = null
    lastActionMessage.value = error instanceof ApiError ? error.message : '查询失败'
  } finally {
    loading.value = false
  }
}

async function save(showSuccess = true) {
  saving.value = true
  lastActionMessage.value = ''
  try {
    // 密钥输入框只在提交时发送，查询结果永不回显明文，避免管理端页面泄露模型密钥。
    const dto = await apiClient.saveModelConfig(userId.value, { ...form })
    applyConfig(dto)
    if (showSuccess) ElMessage.success('模型链接已保存')
    return dto
  } catch (error) {
    lastActionMessage.value = error instanceof ApiError ? error.message : '保存失败'
    throw error
  } finally {
    saving.value = false
  }
}

async function testSaved() {
  testing.value = true
  lastActionMessage.value = '正在请求模型接口，请稍候'
  try {
    const dto = await apiClient.testModelConfig(userId.value, testPrompt.value)
    applyConfig(dto)
    lastActionMessage.value = dto.lastTestMessage ?? '连接成功'
    ElMessage.success('模型连接测试成功')
  } catch (error) {
    lastActionMessage.value = error instanceof ApiError ? error.message : '模型连接测试失败'
  } finally {
    testing.value = false
  }
}

async function saveAndTest() {
  try {
    await save(false)
    await testSaved()
  } catch {
    // save/test 内部已经更新状态和错误文案，避免重复弹出。
  }
}

async function disable() {
  disabling.value = true
  lastActionMessage.value = ''
  try {
    await apiClient.disableModelConfig(userId.value)
    ElMessage.success('模型链接已停用')
    await load()
  } catch (error) {
    lastActionMessage.value = error instanceof ApiError ? error.message : '停用失败'
  } finally {
    disabling.value = false
  }
}

function applyConfig(dto: ModelConfigDto) {
  config.value = dto
  Object.assign(form, {
    configName: dto.configName,
    providerCode: dto.providerCode,
    baseUrl: dto.baseUrl,
    modelName: dto.modelName,
    authType: dto.authType,
    apiKey: '',
    enabled: dto.enabled
  })
}
</script>

<template>
  <div class="toolbar">
    <div>
      <h2>模型链接测试</h2>
      <p class="section-desc">按用户保存专属大模型链接，并用实际 OpenAI-compatible 请求验证配置。</p>
    </div>
    <div class="toolbar-actions">
      <el-input-number v-model="userId" :min="1" controls-position="right" />
      <el-button :loading="loading" @click="load">查询用户配置</el-button>
    </div>
  </div>

  <div class="model-grid">
    <div v-loading="loading" class="panel">
      <h3 class="section-title">连接参数</h3>
      <p class="section-desc">首次保存 API_KEY 或 BEARER 鉴权配置时必须填密钥；后续留空会沿用已保存密钥。</p>
      <el-form label-position="top">
        <el-form-item label="配置名"><el-input v-model="form.configName" /></el-form-item>
        <el-form-item label="接口地址"><el-input v-model="form.baseUrl" placeholder="https://example.com/v1 或完整 /chat/completions 地址" /></el-form-item>
        <el-form-item label="模型名"><el-input v-model="form.modelName" /></el-form-item>
        <el-form-item label="鉴权方式">
          <el-select v-model="form.authType">
            <el-option value="NONE" label="NONE" />
            <el-option value="API_KEY" label="API_KEY，原样写入 Authorization" />
            <el-option value="BEARER" label="BEARER，自动补 Bearer 前缀" />
          </el-select>
        </el-form-item>
        <el-form-item label="密钥">
          <el-input v-model="form.apiKey" type="password" show-password placeholder="查询不会回显密钥；留空表示不更新密钥" />
        </el-form-item>
        <el-checkbox v-model="form.enabled">启用该用户模型链接</el-checkbox>
        <div class="toolbar-actions form-actions">
          <el-button type="primary" :loading="saving" @click="save()">保存</el-button>
          <el-button type="success" :loading="saving || testing" @click="saveAndTest">保存并测试</el-button>
          <el-button :loading="testing" :disabled="!config" @click="testSaved">测试已保存配置</el-button>
          <el-button :loading="disabling" :disabled="!config" @click="disable">停用</el-button>
        </div>
      </el-form>
    </div>

    <aside class="panel status-card">
      <div>
        <h3>测试状态</h3>
        <p>测试会真实调用用户保存的大模型链接，并把结果写回 last_test_status。</p>
      </div>
      <el-tag :type="statusType" size="large">{{ statusLabel }}</el-tag>
      <div class="metric">
        <small>密钥已保存</small>
        <strong>{{ config?.hasApiKey ? '是' : '否' }}</strong>
      </div>
      <div class="metric">
        <small>启用状态</small>
        <strong>{{ config?.enabled ? '启用' : '停用或未配置' }}</strong>
      </div>
      <div class="metric">
        <small>最近测试时间</small>
        <strong>{{ config?.lastTestAt ?? '暂无' }}</strong>
      </div>
      <el-form label-position="top">
        <el-form-item label="测试提示词">
          <el-input v-model="testPrompt" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <el-alert v-if="lastActionMessage || config?.lastTestMessage" :type="config?.lastTestStatus === 'FAILED' ? 'error' : 'info'" :closable="false">
        {{ lastActionMessage || config?.lastTestMessage }}
      </el-alert>
    </aside>
  </div>
</template>

<style scoped>
.form-actions {
  margin-top: 18px;
  flex-wrap: wrap;
}
</style>
