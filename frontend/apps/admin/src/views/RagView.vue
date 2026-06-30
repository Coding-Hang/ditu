<script setup lang="ts">
import { apiClient } from '@ditu/api-client'
import type { RagCollectionDto, RagDocumentDto } from '@ditu/types'
import { onMounted, reactive, ref } from 'vue'

const collections = ref<RagCollectionDto[]>([])
const documents = ref<RagDocumentDto[]>([])
const activeCollectionId = ref<number>()
const form = reactive({ scope: 'GLOBAL', ownerUserId: undefined as number | undefined, name: '', description: '' })

async function loadCollections() {
  try {
    collections.value = await apiClient.ragCollections()
    activeCollectionId.value = activeCollectionId.value ?? collections.value[0]?.id
    if (activeCollectionId.value) await loadDocuments()
  } catch {
    // 全局 API 错误监听负责提示，知识库列表保留上一次成功结果。
  }
}

async function createCollection() {
  try {
    await apiClient.createRagCollection(form)
    await loadCollections()
  } catch {
    // 创建失败时保留表单。
  }
}

async function loadDocuments() {
  if (!activeCollectionId.value) return
  try {
    const page = await apiClient.ragDocuments(activeCollectionId.value)
    documents.value = page.records
  } catch {
    // 文档读取失败时保留当前列表。
  }
}

async function upload(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file || !activeCollectionId.value) return
  // RAG 上传只提交文件和 metadata，解析、切片、Embedding、权限过滤都由后端完成并写审计。
  try {
    await apiClient.uploadRagDocument(activeCollectionId.value, file)
    await loadDocuments()
  } catch {
    // 上传失败时保留当前知识库和文档列表。
  }
}

async function reindex(documentId: number) {
  try {
    await apiClient.reindexRagDocument(documentId)
    await loadDocuments()
  } catch {
    // 重建失败时保留当前文档状态。
  }
}

async function disable(documentId: number) {
  try {
    await apiClient.disableRagDocument(documentId)
    await loadDocuments()
  } catch {
    // 禁用失败时保留当前文档状态。
  }
}

onMounted(loadCollections)
</script>

<template>
  <div class="split">
    <div class="panel">
      <div class="toolbar">
        <el-select v-model="activeCollectionId" @change="loadDocuments">
          <el-option v-for="collection in collections" :key="collection.id" :value="collection.id" :label="collection.name" />
        </el-select>
        <input type="file" @change="upload" />
      </div>
      <el-table :data="documents" stripe>
        <el-table-column prop="fileName" label="文档" />
        <el-table-column prop="status" label="状态" />
        <el-table-column width="180">
          <template #default="{ row }">
            <el-button size="small" @click="reindex(row.id)">重建</el-button>
            <el-button size="small" @click="disable(row.id)">禁用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="panel">
      <h3>创建知识库</h3>
      <el-form label-position="top">
        <el-form-item label="范围"><el-select v-model="form.scope"><el-option value="GLOBAL" /><el-option value="USER" /></el-select></el-form-item>
        <el-form-item label="用户 ID"><el-input-number v-model="form.ownerUserId" :min="1" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" /></el-form-item>
        <el-button type="primary" @click="createCollection">创建</el-button>
      </el-form>
    </div>
  </div>
</template>
