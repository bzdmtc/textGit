<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { userApi } from '@/api/user'
import type { AuditLog } from '@/types'

const logs = ref<AuditLog[]>([])
const targetType = ref('')
const page = ref(1)
const size = ref(15)
const total = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const response = await userApi.auditLogs(targetType.value, page.value, size.value)
    logs.value = response.data.data.items
    total.value = response.data.data.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">审计日志</div>
        <div class="muted">记录权限分组、用户分组等敏感变更的前后差异</div>
      </div>
    </div>

    <div class="page-card">
      <div class="toolbar">
        <el-select v-model="targetType" placeholder="目标类型" clearable style="width: 180px" @change="search">
          <el-option label="GROUP" value="GROUP" />
          <el-option label="USER" value="USER" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="logs" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="action" label="动作" width="170" />
        <el-table-column prop="targetType" label="目标类型" width="110" />
        <el-table-column prop="targetId" label="目标 ID" width="100" />
        <el-table-column prop="userId" label="操作人" width="100" />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column label="变更前" min-width="200">
          <template #default="{ row }">
            <span class="muted">{{ row.beforeJson || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="变更后" min-width="200">
          <template #default="{ row }">
            <span class="muted">{{ row.afterJson || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="180" />
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next, sizes"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>
  </div>
</template>
