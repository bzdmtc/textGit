<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { refundAdminApi } from '@/api/trade'
import { formatDateTime, orderStatusMeta, refundStatusMeta } from '@/utils/format'
import type { Refund } from '@/types'

const refunds = ref<Refund[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const statusFilter = ref('')
const loading = ref(false)

const handleVisible = ref(false)
const handleAction = ref<'approve' | 'reject'>('approve')
const currentRefund = ref<Refund | null>(null)
const adminNote = ref('')
const submitting = ref(false)

async function load() {
  loading.value = true
  try {
    const response = await refundAdminApi.page({
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined,
      page: page.value,
      size: size.value,
    })
    refunds.value = response.data.data.items
    total.value = response.data.data.total
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  load()
}

function onPageChange(value: number) {
  page.value = value
  load()
}

function openHandle(refund: Refund, action: 'approve' | 'reject') {
  currentRefund.value = refund
  handleAction.value = action
  adminNote.value = ''
  handleVisible.value = true
}

async function submit() {
  if (!currentRefund.value) return
  submitting.value = true
  try {
    if (handleAction.value === 'approve') {
      await refundAdminApi.approve(currentRefund.value.id, adminNote.value)
      ElMessage.success('退款已通过，课程权限已撤销')
    } else {
      await refundAdminApi.reject(currentRefund.value.id, adminNote.value)
      ElMessage.success('退款已驳回，订单恢复为已支付')
    }
    handleVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

function userText(refund: Refund) {
  if (!refund.userAccount) return '-'
  return refund.nickname && refund.nickname !== refund.userAccount
    ? `${refund.nickname}（${refund.userAccount}）`
    : refund.userAccount
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">退款审核</div>
        <div class="muted">学员申请退款后进入待审核；通过则撤销课程权限并登记退款流水，驳回则订单恢复已支付</div>
      </div>
    </div>

    <div class="page-card">
      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="订单号 / 用户账号 / 昵称"
          clearable
          style="width: 280px"
          @keyup.enter="onSearch"
          @clear="onSearch"
        />
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 150px" @change="onSearch">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-button type="primary" plain @click="onSearch">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="refunds" stripe>
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column label="用户" width="170">
          <template #default="{ row }">{{ userText(row) }}</template>
        </el-table-column>
        <el-table-column label="退款金额" width="110">
          <template #default="{ row }">
            <strong class="amount">¥{{ Number(row.amount).toFixed(2) }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="退款原因" min-width="200" show-overflow-tooltip />
        <el-table-column label="订单状态" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="orderStatusMeta(row.orderStatus).type">
              {{ orderStatusMeta(row.orderStatus).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="110">
          <template #default="{ row }">
            <el-tag :type="refundStatusMeta(row.status).type">{{ refundStatusMeta(row.status).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="处理信息" width="190">
          <template #default="{ row }">
            <div v-if="row.handledAt" class="small">{{ formatDateTime(row.handledAt) }}</div>
            <div v-if="row.adminNote" class="muted small">{{ row.adminNote }}</div>
            <span v-if="!row.handledAt" class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button
                v-priv="{ module: 'refund', method: 'approve' }"
                link
                type="danger"
                @click="openHandle(row, 'approve')"
              >通过</el-button>
              <el-button
                v-priv="{ module: 'refund', method: 'approve' }"
                link
                type="info"
                @click="openHandle(row, 'reject')"
              >驳回</el-button>
            </template>
            <span v-else class="muted">已处理</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          layout="total, prev, pager, next"
          :total="total"
          :page-size="size"
          :current-page="page"
          @current-change="onPageChange"
        />
      </div>
    </div>

    <el-dialog
      v-model="handleVisible"
      :title="handleAction === 'approve' ? '通过退款申请' : '驳回退款申请'"
      width="520px"
    >
      <template v-if="currentRefund">
        <el-alert
          v-if="handleAction === 'approve'"
          type="warning"
          :closable="false"
          show-icon
          title="通过后将撤销该订单开通的全部课程权限，并登记退款流水。"
          style="margin-bottom: 14px"
        />
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="订单号">{{ currentRefund.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="用户">{{ userText(currentRefund) }}</el-descriptions-item>
          <el-descriptions-item label="退款金额">¥{{ Number(currentRefund.amount).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="退款原因">{{ currentRefund.reason }}</el-descriptions-item>
        </el-descriptions>
        <el-form label-width="80px" style="margin-top: 16px">
          <el-form-item label="审核备注">
            <el-input
              v-model="adminNote"
              type="textarea"
              :rows="2"
              maxlength="500"
              show-word-limit
              placeholder="选填，将记录到退款申请中"
            />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button
          :type="handleAction === 'approve' ? 'danger' : 'primary'"
          :loading="submitting"
          @click="submit"
        >
          {{ handleAction === 'approve' ? '确认通过并退款' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.amount {
  color: #e60012;
}

.muted {
  color: #9ca3af;
}

.small {
  font-size: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
