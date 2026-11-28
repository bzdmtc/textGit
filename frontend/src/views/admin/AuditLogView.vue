<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { userApi } from '@/api/user'
import { formatDateTime } from '@/utils/format'
import type { AuditLog } from '@/types'

const logs = ref<AuditLog[]>([])
const targetTypes = ref<string[]>([])
const page = ref(1)
const size = ref(15)
const total = ref(0)
const loading = ref(false)
const dateRange = ref<[string, string] | null>(null)

const filters = reactive({
  targetType: '',
  action: '',
  userId: undefined as number | undefined,
})

async function load() {
  loading.value = true
  try {
    const response = await userApi.auditLogs({
      targetType: filters.targetType || undefined,
      action: filters.action || undefined,
      userId: filters.userId || undefined,
      from: dateRange.value?.[0] || undefined,
      to: dateRange.value?.[1] || undefined,
      page: page.value,
      size: size.value,
    })
    logs.value = response.data.data.items
    total.value = response.data.data.total
  } finally {
    loading.value = false
  }
}

async function loadTargetTypes() {
  const response = await userApi.auditTargetTypes()
  targetTypes.value = response.data.data
}

function search() {
  page.value = 1
  load()
}

function reset() {
  filters.targetType = ''
  filters.action = ''
  filters.userId = undefined
  dateRange.value = null
  search()
}

function pretty(value?: string | null) {
  if (!value) return '-'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

onMounted(() => {
  loadTargetTypes()
  load()
})
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">审计日志</div>
        <div class="muted">记录用户、权限、院校数据等敏感变更的前后差异，支持按类型 / 动作 / 操作人 / 日期检索</div>
      </div>
    </div>

    <div class="page-card">
      <div class="toolbar">
        <el-select v-model="filters.targetType" placeholder="目标类型" clearable style="width: 180px" @change="search">
          <el-option v-for="type in targetTypes" :key="type" :label="type" :value="type" />
        </el-select>
        <el-input
          v-model="filters.action"
          placeholder="动作关键词，如 UNIVERSITY"
          clearable
          style="width: 220px"
          @keyup.enter="search"
        />
        <el-input-number
          v-model="filters.userId"
          placeholder="操作人 ID"
          :min="1"
          :controls="false"
          style="width: 130px"
        />
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 260px"
        />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="logs" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="action" label="动作" width="190" />
        <el-table-column prop="targetType" label="目标类型" width="150">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.targetType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetId" label="目标 ID" width="100" />
        <el-table-column prop="userId" label="操作人" width="90">
          <template #default="{ row }">{{ row.userId ?? '系统' }}</template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column label="变更前" min-width="200">
          <template #default="{ row }">
            <span class="muted json-cell" :title="pretty(row.beforeJson)">{{ pretty(row.beforeJson) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="变更后" min-width="200">
          <template #default="{ row }">
            <span class="muted json-cell" :title="pretty(row.afterJson)">{{ pretty(row.afterJson) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
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

<style scoped>
.json-cell {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}
</style>
