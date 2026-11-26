<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled, Money, Wallet } from '@element-plus/icons-vue'
import { orderApi, paymentApi } from '@/api/trade'
import { formatDateTime, parseUtc } from '@/utils/format'
import type { Order, PaymentChannel, PaymentResult } from '@/types'

const route = useRoute()
const router = useRouter()
const orderNo = String(route.params.orderNo)

const order = ref<Order | null>(null)
const channels = ref<PaymentChannel[]>([])
const selectedChannel = ref('MOCK_ALIPAY')
const result = ref<PaymentResult | null>(null)
const loading = ref(false)
const paying = ref(false)
const error = ref('')
const now = ref(Date.now())
let timer: number | undefined

const paid = computed(() => order.value?.status === 'PAID' || order.value?.status === 'COMPLETED')
const usable = computed(() => ['PENDING_PAYMENT', 'PAID', 'COMPLETED'].includes(order.value?.status || ''))

const remainingSeconds = computed(() => {
  if (!order.value?.expireAt) return 0
  const expire = parseUtc(order.value.expireAt)
  return expire ? Math.max(0, Math.floor((expire.getTime() - now.value) / 1000)) : 0
})

const countdown = computed(() => {
  const total = remainingSeconds.value
  const minutes = Math.floor(total / 60)
  const seconds = total % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

function statusText(status?: string) {
  const map: Record<string, string> = {
    PENDING_PAYMENT: '待支付',
    PAID: '已支付',
    CANCELED: '已取消',
    REFUNDING: '退款审核中',
    REFUNDED: '已退款',
    COMPLETED: '已完成',
  }
  return status ? map[status] || status : ''
}

function channelIcon(code: string) {
  return code === 'MOCK_ALIPAY' ? Money : Wallet
}

async function load() {
  loading.value = true
  try {
    const [orderResponse, channelResponse] = await Promise.all([
      orderApi.detail(orderNo),
      paymentApi.channels(),
    ])
    order.value = orderResponse.data.data
    channels.value = channelResponse.data.data
    if (channels.value.length && !channels.value.some((channel) => channel.code === selectedChannel.value)) {
      selectedChannel.value = channels.value[0].code
    }
  } catch {
    error.value = '订单不存在或无权访问'
  } finally {
    loading.value = false
  }
}

async function pay() {
  if (!order.value || paying.value) return
  paying.value = true
  try {
    const response = await paymentApi.pay(orderNo, selectedChannel.value)
    result.value = response.data.data
    order.value = response.data.data.order
    ElMessage.success(response.data.data.message || '支付成功')
  } finally {
    paying.value = false
  }
}

onMounted(() => {
  load()
  timer = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<template>
  <div v-loading="loading" class="pay-page">
    <div v-if="error">
      <el-empty :description="error">
        <el-button type="primary" @click="router.push('/user?tab=orders')">查看我的订单</el-button>
      </el-empty>
    </div>

    <template v-else-if="order">
      <div class="page-head">
        <div>
          <el-steps :active="paid ? 3 : 2" simple>
            <el-step title="确认订单" />
            <el-step title="支付" />
            <el-step title="开通课程" />
          </el-steps>
        </div>
        <div class="order-no">订单号：{{ order.orderNo }}</div>
      </div>

      <div class="pay-card">
        <template v-if="paid">
          <el-icon class="success-icon"><CircleCheckFilled /></el-icon>
          <h1>支付成功</h1>
          <p class="muted">课程已开通，可前往「我的课程」开始学习</p>
          <div class="paid-meta">
            <div><span>支付金额</span><strong>¥{{ Number(order.payAmount).toFixed(2) }}</strong></div>
            <div><span>支付方式</span><strong>{{ order.payMethod || '-' }}</strong></div>
            <div><span>支付时间</span><strong>{{ formatDateTime(order.paidAt) }}</strong></div>
          </div>
          <div class="success-actions">
            <el-button type="primary" size="large" @click="router.push('/user?tab=courses')">去我的课程</el-button>
            <el-button size="large" @click="router.push('/user?tab=orders')">查看订单</el-button>
          </div>
        </template>

        <template v-else-if="order.status === 'PENDING_PAYMENT'">
          <div class="pay-head">
            <div>
              <div class="muted">应付金额</div>
              <div class="amount">¥{{ Number(order.payAmount).toFixed(2) }}</div>
            </div>
            <div class="countdown">
              <span>支付剩余时间</span>
              <strong :class="{ danger: remainingSeconds < 300 }">{{ countdown }}</strong>
            </div>
          </div>

          <div class="course-summary">
            <div v-for="item in order.items" :key="item.courseId" class="course-line">
              <div>
                <strong>{{ item.courseTitle }}</strong>
                <span class="muted">{{ item.subject }}</span>
              </div>
              <span>¥{{ Number(item.price).toFixed(2) }}</span>
            </div>
            <div class="amount-lines">
              <div><span>课程总额</span><span>¥{{ Number(order.totalAmount).toFixed(2) }}</span></div>
              <div><span>优惠抵扣{{ order.couponName ? `（${order.couponName}）` : '' }}</span>
                <span>-¥{{ Number(order.discountAmount).toFixed(2) }}</span>
              </div>
              <div class="total"><span>应付</span><strong>¥{{ Number(order.payAmount).toFixed(2) }}</strong></div>
            </div>
          </div>

          <div class="channel-title">选择支付方式</div>
          <el-radio-group v-model="selectedChannel" class="channels">
            <el-radio
              v-for="channel in channels"
              :key="channel.code"
              :value="channel.code"
              class="channel"
              border
            >
              <el-icon><component :is="channelIcon(channel.code)" /></el-icon>
              <span>{{ channel.name }}</span>
            </el-radio>
          </el-radio-group>
          <p class="mock-tip">开发环境使用模拟支付，点击支付即视为支付成功。</p>
          <el-alert
            v-if="remainingSeconds <= 0"
            type="warning"
            :closable="false"
            show-icon
            title="订单已超时，系统将自动关闭，请返回课程详情重新下单"
            style="margin-top: 10px"
          />

          <div class="pay-actions">
            <el-button size="large" @click="router.push('/user?tab=orders')">稍后支付</el-button>
            <el-button
              type="danger"
              size="large"
              :loading="paying"
              :disabled="remainingSeconds <= 0"
              @click="pay"
            >
              确认支付 ¥{{ Number(order.payAmount).toFixed(2) }}
            </el-button>
          </div>
        </template>

        <template v-else>
          <el-empty :description="`订单状态：${statusText(order.status)}`">
            <el-button type="primary" @click="router.push('/user?tab=orders')">查看我的订单</el-button>
          </el-empty>
        </template>
      </div>

      <div v-if="result" class="result-tip">
        交易流水号：{{ result.transactionNo || '-' }}
      </div>
      <div v-if="!usable" class="result-tip">该订单已关闭，如需购买请返回课程详情重新下单。</div>
    </template>
  </div>
</template>

<style scoped>
.pay-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 28px 24px 0;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  margin-bottom: 18px;
}

.page-head :deep(.el-steps) {
  width: 440px;
}

.order-no {
  color: #6b7280;
  font-size: 13px;
}

.pay-card {
  background: #fff;
  border-radius: 16px;
  padding: 30px 34px;
  min-height: 320px;
}

.pay-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 22px;
  border-bottom: 1px solid #f1f2f4;
}

.amount {
  color: #e60012;
  font-size: 36px;
  font-weight: 700;
  margin-top: 6px;
}

.countdown {
  text-align: right;
  color: #6b7280;
  font-size: 13px;
}

.countdown strong {
  display: block;
  margin-top: 6px;
  font-size: 24px;
  color: #111827;
  font-variant-numeric: tabular-nums;
}

.countdown strong.danger {
  color: #e60012;
}

.muted {
  color: #9ca3af;
  font-size: 13px;
}

.course-summary {
  margin: 20px 0;
}

.course-line {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px dashed #eef0f4;
}

.course-line strong {
  margin-right: 10px;
}

.amount-lines {
  margin-top: 12px;
  color: #4b5563;
  font-size: 14px;
}

.amount-lines > div {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
}

.amount-lines .total strong {
  color: #e60012;
  font-size: 20px;
}

.channel-title {
  font-weight: 600;
  margin: 22px 0 12px;
}

.channels {
  display: flex;
  gap: 14px;
}

.channel {
  height: 56px;
  min-width: 180px;
  margin-right: 0;
}

.channel :deep(.el-radio__label) {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mock-tip {
  color: #9ca3af;
  font-size: 12px;
  margin: 12px 0 0;
}

.pay-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 26px;
}

.success-icon {
  font-size: 62px;
  color: #16a34a;
  margin: 10px auto 12px;
  display: block;
}

.pay-card h1 {
  text-align: center;
  margin: 0 0 8px;
}

.pay-card > .muted {
  text-align: center;
  display: block;
}

.paid-meta {
  display: flex;
  justify-content: center;
  gap: 48px;
  margin: 26px 0;
}

.paid-meta div {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #6b7280;
  font-size: 13px;
  text-align: center;
}

.paid-meta strong {
  color: #111827;
  font-size: 15px;
}

.success-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.result-tip {
  text-align: center;
  color: #9ca3af;
  font-size: 12px;
  margin-top: 14px;
}
</style>
