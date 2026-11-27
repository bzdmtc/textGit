<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reviewAdminApi } from '@/api/learn'
import { formatDateTime } from '@/utils/format'
import type { CourseReview } from '@/types'

const reviews = ref<CourseReview[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const statusFilter = ref('PENDING')
const loading = ref(false)

const statusMeta: Record<string, { text: string; type: 'success' | 'warning' | 'danger' }> = {
  PENDING: { text: '待审核', type: 'warning' },
  APPROVED: { text: '已通过', type: 'success' },
  REJECTED: { text: '已驳回', type: 'danger' },
}

async function load() {
  loading.value = true
  try {
    const response = await reviewAdminApi.page({
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined,
      page: page.value,
      size: size.value,
    })
    reviews.value = response.data.data.items
    total.value = response.data.data.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  void load()
}

async function approve(review: CourseReview) {
  await ElMessageBox.confirm(
    `通过「${review.userNickname || review.userAccount}」对课程「${review.courseTitle}」的评价？通过后将展示在课程详情页。`,
    '通过评价',
    { type: 'success' },
  )
  await reviewAdminApi.moderate(review.id, 'APPROVED')
  ElMessage.success('评价已通过')
  await load()
}

async function reject(review: CourseReview) {
  const result = await ElMessageBox.prompt('请填写驳回原因（会展示给学员）', '驳回评价', {
    type: 'warning',
    inputPlaceholder: '例如：评价内容与课程无关',
    inputValidator: (value) => Boolean(value && value.trim()) || '请填写驳回原因',
  })
  await reviewAdminApi.moderate(review.id, 'REJECTED', result.value.trim())
  ElMessage.success('评价已驳回')
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">评价审核</div>
        <div class="muted">学员评价需审核通过后才会展示在课程详情页；通过/驳回都会影响课程平均评分</div>
      </div>
    </div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索课程 / 用户 / 评价内容"
          style="width: 300px"
          clearable
          @keyup.enter="search"
        />
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 150px" @change="search">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="reviews" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="课程" min-width="180">
          <template #default="{ row }">{{ row.courseTitle || '-' }}</template>
        </el-table-column>
        <el-table-column label="学员" width="150">
          <template #default="{ row }">
            {{ row.userNickname || row.userAccount || '-' }}
            <div class="muted">{{ row.userAccount }}</div>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="140">
          <template #default="{ row }">
            <el-rate :model-value="row.rating" disabled size="small" />
          </template>
        </el-table-column>
        <el-table-column label="评价内容" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ row.content }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMeta[row.status]?.type || 'info'">
              {{ statusMeta[row.status]?.text || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button
              v-priv="{ module: 'review', method: 'approve' }"
              link
              type="success"
              :disabled="row.status === 'APPROVED'"
              @click="approve(row)"
            >通过</el-button>
            <el-button
              v-priv="{ module: 'review', method: 'approve' }"
              link
              type="danger"
              :disabled="row.status === 'REJECTED'"
              @click="reject(row)"
            >驳回</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无评价" />
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        class="pager"
        layout="total, prev, pager, next"
        :total="total"
        @current-change="load"
      />
    </el-card>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
