<script setup lang="ts">
import { apiClient } from '@ditu/api-client'
import type { PlanDto } from '@ditu/types'
import { onMounted, ref } from 'vue'

const plans = ref<PlanDto[]>([])

onMounted(async () => {
  plans.value = await apiClient.plans()
})
</script>

<template>
  <section class="page">
    <h3>套餐</h3>
    <div v-for="plan in plans" :key="plan.code" class="card">
      <strong>{{ plan.name }}</strong>
      <p>{{ plan.description }}</p>
      <p>次数 {{ plan.monthlyQuota }}</p>
    </div>
  </section>
</template>
