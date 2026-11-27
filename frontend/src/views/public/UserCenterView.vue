<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { couponApi, myCourseApi, orderApi } from '@/api/trade'
import { learnApi } from '@/api/learn'
import { useAuthStore } from '@/stores/auth'
import {
  couponRuleText,
  couponValueText,
  formatDate,
  formatDateTime,
  orderStatusMeta,
  userCouponStatusMeta,
} from '@/utils/format'
import type { FavoriteCourse, MyCourse, Order, UserCoupon } from '@/types'

type TabName = 'orders' | 'coupons' | 'courses' | 'favorites'

const route = useRoute()
const router = useRouter()
const store = useAuthStore()

const tab = ref<TabName>(normalizeTab(route.query.tab))
const orders = ref<Order[]>([])
const orderTotal = ref(0)
const orderPage = ref(1)
const orderSize = ref(10)
const orderStatus = ref('')
const ordersLoading = ref(false)
const myCoupons = ref<UserCoupon[]>([])
const couponFilter = ref('')
const couponsLoading = ref(false)
const myCourses = ref<MyCourse[]>([])
const coursesLoading = ref(false)
const favorites = ref<FavoriteCourse[]>([])
const favoritesLoading = ref(false)

const refundVisible = ref(false)
const refundOrder = ref<Order | null>(null)
const refundReason = ref('')
const refundSubmitting = ref(false)

const visibleCoupons = computed(() => {
  if (!couponFilter.value) return myCoupons.value
  return myCoupons.value.filter((coupon) => coupon.status === couponFilter.value)
})

const unusedCouponCount = computed(() => myCoupons.value.filter((coupon) => coupon.status === 'UNUSED').length)

function normalizeTab(value: unknown): TabName {
  return value === 'coupons' || value === 'courses' || value === 'favorites' ? value : 'orders'
}

function loadTab(name: TabName) {
  if (name === 'orders') loadOrders()
  if (name === 'coupons') loadCoupons()
  if (name === 'courses') loadCourses()
  if (name === 'favorites') loadFavorites()
}

async function loadOrders() {
  ordersLoading.value = true
  try {
    const response = await orderApi.mine({
      status: orderStatus.value || undefined,
      page: orderPage.value,
      size: orderSize.value,
    })
    orders.value = response.data.data.items
    orderTotal.value = response.data.data.total
  } finally {
    ordersLoading.value = false
  }
}

async function loadCoupons() {
  couponsLoading.value = true
  try {
    const response = await couponApi.mine()
    myCoupons.value = response.data.data
  } finally {
    couponsLoading.value = false
  }
}

async function loadCourses() {
  coursesLoading.value = true
  try {
    const response = await myCourseApi.list()
    myCourses.value = response.data.data
  } finally {
    coursesLoading.value = false
  }
}

async function loadFavorites() {
  favoritesLoading.value = true
  try {
    const response = await learnApi.favorites()
    favorites.value = response.data.data
  } finally {
    favoritesLoading.value = false
  }
}

function goLearn(courseId: number) {
  router.push(`/learn/${courseId}`)
}

function handleTabChange(name: string | number) {
  const next = normalizeTab(name)
  router.replace({ query: { ...route.query, tab: next } })
}

function onOrderStatusChange() {
  orderPage.value = 1
  loadOrders()
}

function onOrderPageChange(page: number) {
  orderPage.value = page
  loadOrders()
}

function orderTitles(order: Order) {
  return order.items.map((item) => item.courseTitle).join('、') || '-'
}

function goPay(order: Order) {
  router.push(`/pay/${order.orderNo}`)
}

async function cancelOrder(order: Order) {
  await ElMessageBox.confirm(`确定取消订单 ${order.orderNo} 吗？取消后优惠券将释放。`, '取消订单', {
    type: 'warning',
  })
  await orderApi.cancel(order.orderNo)
  ElMessage.success('订单已取消')
  await loadOrders()
}

function openRefund(order: Order) {
  refundOrder.value = order
  refundReason.value = ''
  refundVisible.value = true
}

async function submitRefund() {
  if (!refundOrder.value) return
  if (!refundReason.value.trim()) {
    ElMessage.warning('请填写退款原因')
    return
  }
  refundSubmitting.value = true
  try {
    await orderApi.applyRefund(refundOrder.value.orderNo, refundReason.value.trim())
    ElMessage.success('退款申请已提交，请等待审核')
    refundVisible.value = false
    await loadOrders()
  } finally {
    refundSubmitting.value = false
  }
}

watch(
  () => route.query.tab,
  (value) => {
    const next = normalizeTab(value)
    tab.value = next
    loadTab(next)
  },
)

onMounted(() => {
  loadTab(tab.value)
})
</script>

<template>
  <div class="user-page">
    <div class="user-hero">
      <el-avatar :size="64" style="background: #e60012; font-size: 24px">
        {{ store.displayName.slice(0, 1) }}
      </el-avatar>
      <div class="user-meta">
        <h1>{{ store.displayName }}</h1>
        <p>
          <span v-if="store.me?.user.position">{{ store.me?.user.position }}</span>
          <span>{{ store.me?.user.account }}</span>
          <span v-if="store.me?.user.phone">{{ store.me?.user.phone }}</span>
          <span v-if="store.me?.user.email">{{ store.me?.user.email }}</span>
        </p>
      </div>
      <div class="hero-stats">
        <div>
          <strong>{{ orderTotal }}</strong>
          <span>订单</span>
        </div>
        <div>
          <strong>{{ unusedCouponCount }}</strong>
          <span>可用优惠券</span>
        </div>
        <div>
          <strong>{{ myCourses.length || '—' }}</strong>
          <span>已购课程</span>
        </div>
        <div>
          <strong>{{ favorites.length || '—' }}</strong>
          <span>收藏课程</span>
        </div>
      </div>
    </div>

    <el-tabs v-model="tab" class="user-tabs" @tab-change="handleTabChange">
      <el-tab-pane name="orders" label="我的订单">
        <div class="toolbar">
          <el-select v-model="orderStatus" placeholder="全部状态" clearable style="width: 180px" @change="onOrderStatusChange">
            <el-option label="待支付" value="PENDING_PAYMENT" />
            <el-option label="已支付" value="PAID" />
            <el-option label="退款审核中" value="REFUNDING" />
            <el-option label="已退款" value="REFUNDED" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已取消" value="CANCELED" />
          </el-select>
          <el-button text @click="loadOrders">刷新</el-button>
        </div>

        <el-table v-loading="ordersLoading" :data="orders" stripe>
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="order-detail">
                <div v-for="item in row.items" :key="item.courseId" class="detail-line">
                  <router-link :to="`/courses/${item.courseId}`">{{ item.courseTitle }}</router-link>
                  <span>¥{{ Number(item.price).toFixed(2) }}</span>
                </div>
                <div class="detail-line muted">
                  <span>订单号 {{ row.orderNo }}</span>
                  <span v-if="row.couponName">优惠券：{{ row.couponName }}</span>
                </div>
                <div v-if="row.refundReason" class="detail-line muted">
                  <span>退款原因：{{ row.refundReason }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="orderNo" label="订单号" width="200" />
          <el-table-column label="课程" min-width="220">
            <template #default="{ row }">{{ orderTitles(row) }}</template>
          </el-table-column>
          <el-table-column label="实付" width="110">
            <template #default="{ row }">
              <strong class="pay-amount">¥{{ Number(row.payAmount).toFixed(2) }}</strong>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="140">
            <template #default="{ row }">
              <el-tag :type="orderStatusMeta(row.status).type" effect="light">
                {{ orderStatusMeta(row.status).text }}
              </el-tag>
              <el-tag
                v-if="row.refundStatus === 'REJECTED'"
                type="danger"
                effect="plain"
                size="small"
                style="margin-left: 6px"
              >已驳回</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="下单时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 'PENDING_PAYMENT'">
                <el-button link type="danger" @click="goPay(row)">去支付</el-button>
                <el-button link type="info" @click="cancelOrder(row)">取消</el-button>
              </template>
              <el-button
                v-else-if="row.status === 'PAID'"
                link
                type="primary"
                @click="openRefund(row)"
              >申请退款</el-button>
              <el-button link type="primary" @click="router.push(`/courses/${row.items[0]?.courseId}`)">
                课程详情
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无订单">
              <el-button type="primary" @click="router.push('/courses')">去选课</el-button>
            </el-empty>
          </template>
        </el-table>

        <div class="pager">
          <el-pagination
            layout="total, prev, pager, next"
            :total="orderTotal"
            :page-size="orderSize"
            :current-page="orderPage"
            @current-change="onOrderPageChange"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane name="coupons" label="我的优惠券">
        <div class="toolbar">
          <el-select v-model="couponFilter" placeholder="全部状态" clearable style="width: 160px">
            <el-option label="未使用" value="UNUSED" />
            <el-option label="已使用" value="USED" />
            <el-option label="已过期" value="EXPIRED" />
            <el-option label="已锁定" value="LOCKED" />
          </el-select>
          <el-button text @click="loadCoupons">刷新</el-button>
        </div>

        <div v-loading="couponsLoading" class="coupon-grid">
          <div v-for="coupon in visibleCoupons" :key="coupon.id" class="coupon-card" :class="coupon.status.toLowerCase()">
            <div class="coupon-value">
              <strong>{{ couponValueText(coupon.type, coupon.value) }}</strong>
              <span>{{ couponRuleText(coupon.type, coupon.minAmount) }}</span>
            </div>
            <div class="coupon-body">
              <div class="coupon-name">{{ coupon.name }}</div>
              <div class="coupon-subject">{{ coupon.subject || '全站课程通用' }}</div>
              <div class="coupon-validity">
                有效期至 {{ formatDate(coupon.endAt) }}
              </div>
            </div>
            <el-tag class="coupon-status" :type="userCouponStatusMeta(coupon.status).type" size="small">
              {{ userCouponStatusMeta(coupon.status).text }}
            </el-tag>
          </div>
          <el-empty v-if="!visibleCoupons.length" description="暂无优惠券">
            <el-button type="primary" @click="router.push('/courses')">去选课领券</el-button>
          </el-empty>
        </div>
      </el-tab-pane>

      <el-tab-pane name="courses" label="我的课程">
        <div v-loading="coursesLoading" class="course-grid">
          <div
            v-for="course in myCourses"
            :key="course.id"
            class="course-card"
            @click="router.push(`/courses/${course.courseId}`)"
          >
            <div
              class="course-cover"
              :style="course.coverUrl ? { backgroundImage: `url(${course.coverUrl})` } : undefined"
            ></div>
            <div class="course-info">
              <strong>{{ course.courseTitle }}</strong>
              <div class="course-tags">
                <el-tag size="small" effect="plain">{{ course.subject }}</el-tag>
                <el-tag size="small" effect="plain" type="info">{{ course.stage }}</el-tag>
              </div>
              <el-progress :percentage="Number(course.progressPercent || 0)" :stroke-width="6" />
              <div class="course-meta">
                <span>开通于 {{ formatDate(course.enrolledAt) }}</span>
                <span>订单 {{ course.orderId || '-' }}</span>
              </div>
              <el-button size="small" type="primary" plain @click.stop="goLearn(course.courseId)">
                继续学习
              </el-button>
            </div>
          </div>
          <el-empty v-if="!myCourses.length" description="还没有已购课程">
            <el-button type="primary" @click="router.push('/courses')">去课程中心</el-button>
          </el-empty>
        </div>
      </el-tab-pane>

      <el-tab-pane name="favorites" label="我的收藏">
        <div v-loading="favoritesLoading" class="course-grid">
          <div
            v-for="favorite in favorites"
            :key="favorite.id"
            class="course-card"
            @click="router.push(`/courses/${favorite.courseId}`)"
          >
            <div
              class="course-cover"
              :style="favorite.coverUrl ? { backgroundImage: `url(${favorite.coverUrl})` } : undefined"
            ></div>
            <div class="course-info">
              <strong>{{ favorite.courseTitle }}</strong>
              <div class="course-tags">
                <el-tag size="small" effect="plain">{{ favorite.subject }}</el-tag>
                <el-tag size="small" effect="plain" type="info">{{ favorite.stage }}</el-tag>
                <el-tag v-if="favorite.enrolled" size="small" effect="dark" type="danger">已购</el-tag>
              </div>
              <el-progress
                v-if="favorite.enrolled"
                :percentage="Number(favorite.progressPercent || 0)"
                :stroke-width="6"
              />
              <div class="course-meta">
                <span>收藏于 {{ formatDate(favorite.favoritedAt) }}</span>
                <span>评分 {{ Number(favorite.rating).toFixed(1) }}</span>
              </div>
              <el-button
                v-if="favorite.enrolled"
                size="small"
                type="primary"
                plain
                @click.stop="goLearn(favorite.courseId)"
              >继续学习</el-button>
            </div>
          </div>
          <el-empty v-if="!favorites.length" description="还没有收藏课程">
            <el-button type="primary" @click="router.push('/courses')">去课程中心</el-button>
          </el-empty>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="refundVisible" title="申请退款" width="520px">
      <div v-if="refundOrder" class="refund-summary">
        <div><span>订单号</span><strong>{{ refundOrder.orderNo }}</strong></div>
        <div><span>课程</span><strong>{{ orderTitles(refundOrder) }}</strong></div>
        <div><span>退款金额</span><strong class="pay-amount">¥{{ Number(refundOrder.payAmount).toFixed(2) }}</strong></div>
      </div>
      <el-form label-width="80px" style="margin-top: 16px">
        <el-form-item label="退款原因" required>
          <el-input
            v-model="refundReason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请说明退款原因"
          />
        </el-form-item>
      </el-form>
      <p class="muted" style="font-size: 12px">退款申请提交后订单进入审核中，审核通过后将撤销课程权限并原路退回（模拟）。</p>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="danger" :loading="refundSubmitting" @click="submitRefund">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 24px 0;
}

.user-hero {
  background: linear-gradient(135deg, #111827, #1f2937);
  border-radius: 16px;
  color: #fff;
  padding: 26px 30px;
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-meta {
  flex: 1;
}

.user-meta h1 {
  margin: 0 0 8px;
  font-size: 24px;
}

.user-meta p {
  display: flex;
  gap: 16px;
  margin: 0;
  color: #94a3b8;
  font-size: 13px;
}

.hero-stats {
  display: flex;
  gap: 40px;
}

.hero-stats div {
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: center;
}

.hero-stats strong {
  font-size: 24px;
}

.hero-stats span {
  color: #94a3b8;
  font-size: 12px;
}

.user-tabs {
  margin-top: 18px;
  background: #fff;
  border-radius: 14px;
  padding: 6px 22px 22px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}

.pay-amount {
  color: #e60012;
}

.order-detail {
  padding: 12px 36px;
  background: #fafbfc;
}

.detail-line {
  display: flex;
  justify-content: space-between;
  padding: 5px 0;
  color: #374151;
  font-size: 13px;
}

.detail-line.muted {
  color: #9ca3af;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.coupon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(330px, 1fr));
  gap: 16px;
}

.coupon-card {
  display: flex;
  border: 1px solid #f1f2f4;
  border-radius: 12px;
  overflow: hidden;
  position: relative;
  min-height: 120px;
  background: #fff;
}

.coupon-card.used,
.coupon-card.expired {
  opacity: 0.62;
}

.coupon-value {
  width: 120px;
  background: linear-gradient(160deg, #e60012, #ff7a00);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px;
  text-align: center;
}

.coupon-value strong {
  font-size: 22px;
}

.coupon-value span {
  font-size: 11px;
  opacity: 0.9;
}

.coupon-body {
  flex: 1;
  padding: 14px 16px;
}

.coupon-name {
  font-weight: 600;
  color: #111827;
  margin-bottom: 6px;
}

.coupon-subject,
.coupon-validity {
  color: #9ca3af;
  font-size: 12px;
  margin-top: 4px;
}

.coupon-status {
  position: absolute;
  right: 12px;
  top: 12px;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 18px;
}

.course-card {
  display: flex;
  gap: 16px;
  border: 1px solid #f1f2f4;
  border-radius: 12px;
  padding: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.course-card:hover {
  border-color: #e60012;
  box-shadow: 0 10px 24px rgba(17, 24, 39, 0.08);
}

.course-cover {
  width: 150px;
  height: 96px;
  border-radius: 9px;
  background: linear-gradient(135deg, #e60012, #ff7a00);
  background-size: cover;
  background-position: center;
  flex-shrink: 0;
}

.course-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.course-info strong {
  font-size: 15px;
}

.course-tags {
  display: flex;
  gap: 6px;
}

.course-meta {
  display: flex;
  justify-content: space-between;
  color: #9ca3af;
  font-size: 11px;
}

.refund-summary {
  background: #fafbfc;
  border-radius: 10px;
  padding: 14px 16px;
}

.refund-summary > div {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 4px 0;
  font-size: 13px;
  color: #4b5563;
}

.refund-summary strong {
  color: #111827;
  text-align: right;
}
</style>
