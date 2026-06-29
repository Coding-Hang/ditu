<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useChatStore } from '../stores/chat'

const chat = useChatStore()
const router = useRouter()

onMounted(chat.loadConversations)
</script>

<template>
  <div class="panel">
    <el-table :data="chat.conversations" stripe>
      <el-table-column prop="title" label="会话" />
      <el-table-column prop="updatedAt" label="更新时间" />
      <el-table-column width="120">
        <template #default="{ row }">
          <el-button size="small" @click="router.push(`/chat/${row.id}`)">继续</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
