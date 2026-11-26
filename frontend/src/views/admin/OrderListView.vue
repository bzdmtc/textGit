<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { orderAdminApi } from '@/api/trade'
import { formatDateTime, orderStatusMeta, refundStatusMeta } from '@/utils/format'
import type { Order } from '@/types'

const orders = ref<Order[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const statusFilter = ref('')
const loading = ref(false)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailOrder = ref<Order | null>(null)

async function load() {
  loading.value = true
  try {
    const response = await orderAdminApi.page({
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined,
      page: page.value,
      size: size.value,
    })
    orders.value = response.data.data.items
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

async function openDetail(order: Order) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const response = await orderAdminApi.detail(order.orderNo)
    detailOrder.value = response.data.data
  } finally {
    detailLoading.value = false
  }
}

function userText(order: Order) {
  if (!order.userAccount) return '-'
  return order.nickname && order.nickname !== order.userAccount
    ? `${order.nickname}（${order.userAccount}）`
    : order.userAccount
}

function itemTitles(order: Order) {
  return order.items.map((item) => item.courseTitle).join('、')
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">订单管理</div>
        <div class="muted">查看全部订单、支付状态与退款进度；待支付订单 30 分钟自动关闭</div>
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
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 160px" @change="onSearch">
          <el-option label="待支付" value="PENDING_PAYMENT" />
          <el-option label="已支付" value="PAID" />
          <el-option label="退款审核中" value="REFUNDING" />
          <el-option label="已退款" value="REFUNDED" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELED" />
        </el-select>
        <el-button type="primary" plain @click="onSearch">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="orders" stripe>
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column label="用户" width="180">
          <template #default="{ row }">{{ userText(row) }}</template>
        </el-table-column>
        <el-table-column label="课程" min-width="240">
          <template #default="{ row }">{{ itemTitles(row) }}</template>
        </el-table-column>
        <el-table-column label="金额" width="160">
          <template #default="{ row }">
            <div class="amount">¥{{ Number(row.payAmount).toFixed(2) }}</div>
            <div class="muted small">
              原价 ¥{{ Number(row.totalAmount).toFixed(2) }}
              <span v-if="Number(row.discountAmount) > 0"> · 优惠 ¥{{ Number(row.discountAmount).toFixed(2) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="orderStatusMeta(row.status).type">{{ orderStatusMeta(row.status).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="90">
          <template #default="{ row }">
            {{ row.orderSource === 'SECKILL' ? '秒杀' : '普通' }}
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
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

    <el-dialog v-model="detailVisible" title="订单详情" width="720px">
      <div v-loading="detailLoading">
        <template v-if="detailOrder">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单号">{{ detailOrder.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="用户">{{ userText(detailOrder) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="orderStatusMeta(detailOrder.status).type">
                {{ orderStatusMeta(detailOrder.status).text }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="来源">
              {{ detailOrder.orderSource === 'SECKILL' ? '秒杀' : '普通购买' }}
            </el-descriptions-item>
            <el-descriptions-item label="课程总额">¥{{ Number(detailOrder.totalAmount).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="优惠抵扣">
              -¥{{ Number(detailOrder.discountAmount).toFixed(2) }}
              <span v-if="detailOrder.couponName" class="muted">（{{ detailOrder.couponName }}）</span>
            </el-descriptions-item>
            <el-descriptions-item label="实付金额">
              <strong class="amount">¥{{ Number(detailOrder.payAmount).toFixed(2) }}</strong>
            </el-descriptions-item>
            <el-descriptions-item label="支付方式">{{ detailOrder.payMethod || '-' }}</el-descriptions-item>
            <el-descriptions-item label="下单时间">{{ formatDateTime(detailOrder.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="支付时间">{{ formatDateTime(detailOrder.paidAt) }}</el-descriptions-item>
            <el-descriptions-item label="过期时间">{{ formatDateTime(detailOrder.expireAt) }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ formatDateTime(detailOrder.completedAt) }}</el-descriptions-item>
          </el-descriptions>

          <h4 style="margin: 18px 0 10px">课程明细</h4>
          <el-table :data="detailOrder.items" size="small" border>
            <el-table-column prop="courseTitle" label="课程" min-width="220" />
            <el-table-column prop="subject" label="科目" width="110" />
            <el-table-column label="价格" width="110">
              <template #default="{ row }">¥{{ Number(row.price).toFixed(2) }}</template>
            </el-table-column>
          </el-table>

          <div v-if="detailOrder.refundStatus" class="refund-block">
            <div>
              退款状态：
              <el-tag size="small" :type="refundStatusMeta(detailOrder.refundStatus).type">
                {{ refundStatusMeta(detailOrder.refundStatus).text }}
              </el-tag>
            </div>
            <div v-if="detailOrder.refundReason" class="muted">退款原因：{{ detailOrder.refundReason }}</div>
          </div>
        </template>
      </div>
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
  font-weight: 600;
}

.muted {
  color: #9ca3af;
}

.small {
  font-size: 11px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.refund-block {
  margin-top: 16px;
  padding: 12px 16px;
  border-radius: 8px;
  background: #fff7ed;
  font-size: 13px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
</style>
