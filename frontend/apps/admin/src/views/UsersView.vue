<script setup lang="ts">
import { apiClient } from '@ditu/api-client'
import type { UserSummary } from '@ditu/types'
import { onMounted, reactive, ref } from 'vue'

const users = ref<UserSummary[]>([])
const form = reactive({ username: '', password: 'InitPass123!', displayName: '', phone: '', planCode: 'PRO', quotaTotal: 300 })

async function load() {
  const page = await apiClient.adminUsers()
  users.value = page.records
}

async function createUser() {
  await apiClient.adminCreateUser(form)
  await load()
}

async function setStatus(user: UserSummary) {
  if (user.status === 'ACTIVE') await apiClient.adminDisableUser(user.id)
  else await apiClient.adminEnableUser(user.id)
  await load()
}

onMounted(load)
</script>

<template>
  <div class="split">
    <div class="panel">
      <el-table :data="users" stripe>
        <el-table-column prop="username" label="账号" />
        <el-table-column prop="displayName" label="名称" />
        <el-table-column prop="planCode" label="套餐" width="90" />
        <el-table-column prop="remainingQuota" label="剩余" width="90" />
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column width="120">
          <template #default="{ row }">
            <el-button size="small" @click="setStatus(row)">{{ row.status === 'ACTIVE' ? '停用' : '启用' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="panel">
      <h3>注册用户</h3>
      <el-form label-position="top">
        <el-form-item label="账号"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.displayName" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="套餐">
          <el-select v-model="form.planCode"><el-option value="BASIC" /><el-option value="PRO" /><el-option value="PLUS" /></el-select>
        </el-form-item>
        <el-form-item label="初始次数"><el-input-number v-model="form.quotaTotal" :min="0" /></el-form-item>
        <el-button type="primary" @click="createUser">创建</el-button>
      </el-form>
    </div>
  </div>
</template>
