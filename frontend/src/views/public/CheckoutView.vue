<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { publicApi } from '@/api/public'
import { couponApi, orderApi } from '@/api/trade'
import type { CourseDetail, OrderPreview, UserCoupon } from '@/types'

const route = useRoute()
const router = useRouter()

const courseId = Number(route.query.courseId)
const detail = ref<CourseDetail | null>(null)
const myCoupons = ref<UserCoupon[]>([])
const selectedCouponId = ref<number>(0)
const preview = ref<OrderPreview | null>(null)
const loading = ref(false)
const submitting = ref(false)
const loadError = ref('')

const applicableCoupons = computed(() => {
  if (!detail.value) return []
  const course = detail.value.course
  const now = Date.now()
  return myCoupons.value.filter((coupon) => {
    if (coupon.status !== 'UNUSED') return false
    if (coupon.subject && coupon.subject !== course.subject) return false
    if (Number(coupon.minAmount) > Number(course.price)) return false
    if (new Date(coupon.startAt).getTime() > now) return false
    if (new Date(coupon.endAt).getTime() < now) return false
    return true
  })
})

function couponLabel(coupon: UserCoupon) {
  if (coupon.type === 'PERCENT') {
    return `${(Number(coupon.value) / 10).toFixed(1)} 折`
  }
  return `减 ¥${Number(coupon.value).toFixed(2)}`
}

function couponRule(coupon: UserCoupon) {
  const min = Number(coupon.minAmount)
  return min > 0 ? `满 ¥${min.toFixed(2)} 可用` : '无门槛'
}

async function load() {
  if (!courseId || Number.isNaN(courseId)) {
    loadError.value = '缺少课程参数，请从课程详情页进入'
    return
  }
  loading.value = true
  try {
    const [courseResponse, couponResponse] = await Promise.all([
      publicApi.courseDetail(courseId),
      couponApi.mine('UNUSED'),
    ])
    detail.value = courseResponse.data.data
    myCoupons.value = couponResponse.data.data
    await refreshPreview()
  } catch {
    loadError.value = '加载订单信息失败，请返回课程详情重试'
  } finally {
    loading.value = false
  }
}

async function refreshPreview() {
  if (!detail.value) return
  const response = await orderApi.preview({
    courseIds: [courseId],
    userCouponId: selectedCouponId.value || null,
  })
  preview.value = response.data.data
}

async function onCouponChange() {
  try {
    await refreshPreview()
  } catch {
    selectedCouponId.value = 0
    await refreshPreview()
  }
}

async function submit() {
  if (!detail.value || submitting.value) return
  submitting.value = true
  try {
    const response = await orderApi.create({
      courseIds: [courseId],
      userCouponId: selectedCouponId.value || null,
    })
    ElMessage.success('订单已创建，请完成支付')
    await router.push(`/pay/${response.data.data.orderNo}`)
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="checkout-page">
    <div class="page-head">
      <div>
        <div class="crumb">
          <router-link to="/">首页</router-link> /
          <router-link to="/courses">课程中心</router-link> /
          <router-link v-if="detail" :to="`/courses/${courseId}`">{{ detail.course.title }}</router-link>
          / <span>确认订单</span>
        </div>
        <h1>确认订单</h1>
        <p class="muted">课程为虚拟商品，支付成功后立即开通学习权限</p>
      </div>
      <el-steps :active="1" simple style="width: 360px">
        <el-step title="确认订单" />
        <el-step title="支付" />
        <el-step title="开通课程" />
      </el-steps>
    </div>

    <el-empty v-if="loadError" :description="loadError" />

    <template v-else-if="detail">
      <div class="checkout-body">
        <div class="main">
          <section class="card">
            <div class="card-head">
              <h2>课程清单</h2>
              <span>{{ Number(detail.course.price).toFixed(2) }} 元</span>
            </div>
            <div class="course-row">
              <div
                class="cover"
                :style="detail.course.coverUrl ? { backgroundImage: `url(${detail.course.coverUrl})` } : undefined"
              ></div>
              <div class="course-info">
                <strong>{{ detail.course.title }}</strong>
                <p>{{ detail.course.subtitle || detail.course.description }}</p>
                <div class="tags">
                  <el-tag size="small" effect="plain">{{ detail.course.subject }}</el-tag>
                  <el-tag size="small" effect="plain" type="info">{{ detail.course.stage }}</el-tag>
                  <span class="muted">共 {{ detail.chapters.length }} 章</span>
                </div>
              </div>
              <div class="price">¥{{ Number(detail.course.price).toFixed(2) }}</div>
            </div>
          </section>

          <section class="card">
            <div class="card-head">
              <h2>优惠券</h2>
              <span v-if="applicableCoupons.length">{{ applicableCoupons.length }} 张可用</span>
            </div>
            <el-radio-group
              v-if="applicableCoupons.length"
              v-model="selectedCouponId"
              class="coupon-list"
              @change="onCouponChange"
            >
              <el-radio :value="0" class="coupon-option">
                不使用优惠券
              </el-radio>
              <el-radio
                v-for="coupon in applicableCoupons"
                :key="coupon.id"
                :value="coupon.id"
                class="coupon-option"
              >
                <span class="coupon-amount">{{ couponLabel(coupon) }}</span>
                <span class="coupon-name">{{ coupon.name }}</span>
                <span class="coupon-rule">{{ couponRule(coupon) }}</span>
              </el-radio>
            </el-radio-group>
            <el-empty v-else description="暂无可用优惠券" :image-size="56" />
            <div class="coupon-tip">
              <router-link to="/user?tab=coupons">查看我的优惠券</router-link>
            </div>
          </section>

          <section class="card">
            <h2>购买须知</h2>
            <ul class="promise">
              <li>课程为虚拟内容，支付成功后不支持无理由退款，如有问题可在「我的订单」申请退款。</li>
              <li>待支付订单 30 分钟内有效，超时自动取消并释放优惠券。</li>
              <li>支付成功后可在「我的中心 → 我的课程」查看已开通课程。</li>
            </ul>
          </section>
        </div>

        <aside class="side">
          <div class="summary">
            <h2>订单金额</h2>
            <div class="line">
              <span>课程总额</span>
              <strong>¥{{ Number(preview?.totalAmount ?? detail.course.price).toFixed(2) }}</strong>
            </div>
            <div class="line discount">
              <span>优惠券抵扣</span>
              <strong>-¥{{ Number(preview?.discountAmount ?? 0).toFixed(2) }}</strong>
            </div>
            <div v-if="preview?.couponName" class="coupon-used">已选：{{ preview.couponName }}</div>
            <div class="line total">
              <span>应付金额</span>
              <strong>¥{{ Number(preview?.payAmount ?? detail.course.price).toFixed(2) }}</strong>
            </div>
            <el-button type="danger" size="large" style="width: 100%" @click="submit">
              提交订单
            </el-button>
            <el-button size="large" style="width: 100%; margin: 10px 0 0" @click="router.back()">
              返回
            </el-button>
          </div>
        </aside>
      </div>
    </template>
  </div>
</template>

<style scoped>
.checkout-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 24px 0;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
  margin-bottom: 20px;
}

.crumb {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 10px;
}

.crumb a {
  color: #6b7280;
}

.page-head h1 {
  margin: 0 0 6px;
  font-size: 26px;
}

.muted {
  color: #9ca3af;
  font-size: 13px;
}

.checkout-body {
  display: flex;
  gap: 22px;
  align-items: flex-start;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.card {
  background: #fff;
  border-radius: 14px;
  padding: 22px 24px;
}

.card h2 {
  margin: 0 0 14px;
  font-size: 18px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-head h2 {
  margin: 0;
}

.card-head span {
  color: #9ca3af;
  font-size: 13px;
}

.course-row {
  display: flex;
  gap: 18px;
  align-items: center;
}

.cover {
  width: 180px;
  height: 108px;
  border-radius: 10px;
  background: linear-gradient(135deg, #e60012, #ff7a00);
  background-size: cover;
  background-position: center;
  flex-shrink: 0;
}

.course-info {
  flex: 1;
}

.course-info strong {
  font-size: 17px;
}

.course-info p {
  color: #6b7280;
  font-size: 13px;
  margin: 8px 0;
}

.tags {
  display: flex;
  align-items: center;
  gap: 8px;
}

.price {
  color: #e60012;
  font-size: 20px;
  font-weight: 700;
}

.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: stretch;
}

.coupon-option {
  margin-right: 0;
  border: 1px solid #eef0f4;
  border-radius: 10px;
  padding: 10px 14px;
  width: 100%;
  height: auto;
}

.coupon-option :deep(.el-radio__label) {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.coupon-amount {
  color: #e60012;
  font-weight: 700;
}

.coupon-name {
  color: #374151;
  font-size: 14px;
}

.coupon-rule {
  color: #9ca3af;
  font-size: 12px;
}

.coupon-tip {
  margin-top: 12px;
  font-size: 12px;
}

.coupon-tip a {
  color: #6b7280;
}

.promise {
  margin: 0;
  padding-left: 18px;
  color: #6b7280;
  font-size: 13px;
  line-height: 2;
}

.side {
  width: 320px;
}

.summary {
  background: #fff;
  border-radius: 14px;
  padding: 22px;
  position: sticky;
  top: 96px;
}

.summary h2 {
  margin: 0 0 16px;
  font-size: 18px;
}

.line {
  display: flex;
  justify-content: space-between;
  color: #4b5563;
  font-size: 14px;
  margin-bottom: 12px;
}

.line.discount strong {
  color: #e60012;
}

.coupon-used {
  font-size: 12px;
  color: #e60012;
  margin: -4px 0 12px;
}

.line.total {
  border-top: 1px dashed #eef0f4;
  padding-top: 14px;
  margin-bottom: 18px;
  align-items: baseline;
}

.line.total strong {
  color: #e60012;
  font-size: 28px;
}
</style>
