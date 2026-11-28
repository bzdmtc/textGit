<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { VideoPlay } from '@element-plus/icons-vue'
import { publicApi } from '@/api/public'
import { couponApi, orderApi, publicCouponApi } from '@/api/trade'
import { learnApi, reviewPublicApi } from '@/api/learn'
import { useAuthStore } from '@/stores/auth'
import { couponRuleText, couponValueText, formatDate } from '@/utils/format'
import type { Coupon, CourseDetail, PurchaseStatus, ReviewSummary, UserCoupon } from '@/types'

const route = useRoute()
const router = useRouter()
const store = useAuthStore()

const detail = ref<CourseDetail | null>(null)
const loading = ref(false)
const activeChapters = ref<number[]>([])
const coupons = ref<Coupon[]>([])
const myCoupons = ref<UserCoupon[]>([])
const purchase = ref<PurchaseStatus | null>(null)
const claimingId = ref<number | null>(null)
const reviews = ref<ReviewSummary | null>(null)
const favorited = ref(false)
const favoriteLoading = ref(false)

const enrolled = computed(() => purchase.value?.enrolled === true)
const pendingOrderNo = computed(() => purchase.value?.pendingOrderNo || '')
const firstLessonId = computed(() => {
  const chapters = detail.value?.chapters || []
  for (const chapter of chapters) {
    if (chapter.lessons.length > 0) return chapter.lessons[0].id
  }
  return null
})
const buyLabel = computed(() => {
  if (enrolled.value) return '已购买 · 去学习'
  if (pendingOrderNo.value) return '去支付'
  return '立即报名'
})

async function load() {
  loading.value = true
  try {
    const response = await publicApi.courseDetail(Number(route.params.id))
    detail.value = response.data.data
    activeChapters.value = detail.value.chapters.map((chapter) => chapter.id)
    await Promise.all([loadSide(), loadReviews()])
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function loadReviews() {
  try {
    const response = await reviewPublicApi.forCourse(Number(route.params.id))
    reviews.value = response.data.data
  } catch {
    reviews.value = null
  }
}

async function loadSide() {
  const courseId = Number(route.params.id)
  try {
    const couponResponse = await publicCouponApi.forCourse(courseId)
    coupons.value = couponResponse.data.data
  } catch {
    coupons.value = []
  }
  if (!store.isLoggedIn) {
    myCoupons.value = []
    purchase.value = null
    favorited.value = false
    return
  }
  try {
    const [mineResponse, statusResponse, favoriteResponse] = await Promise.all([
      couponApi.mine(),
      orderApi.purchaseStatus(courseId),
      learnApi.favoriteStatus(courseId),
    ])
    myCoupons.value = mineResponse.data.data
    purchase.value = statusResponse.data.data
    favorited.value = favoriteResponse.data.data.favorited
  } catch {
    myCoupons.value = []
  }
}

async function toggleFavorite() {
  if (!store.isLoggedIn) {
    await router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  favoriteLoading.value = true
  try {
    if (favorited.value) {
      await learnApi.removeFavorite(Number(route.params.id))
      favorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await learnApi.addFavorite(Number(route.params.id))
      favorited.value = true
      ElMessage.success('已收藏课程')
    }
  } finally {
    favoriteLoading.value = false
  }
}

function myCoupon(couponId: number) {
  return myCoupons.value.find((coupon) => coupon.couponId === couponId)
}

function claimLabel(coupon: Coupon) {
  const mine = myCoupon(coupon.id)
  if (!mine) return '领取'
  if (mine.status === 'USED') return '已使用'
  if (mine.status === 'EXPIRED') return '已过期'
  if (mine.status === 'LOCKED') return '已锁定'
  return '已领取'
}

async function claim(coupon: Coupon) {
  if (!store.isLoggedIn) {
    await router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (myCoupon(coupon.id)) return
  claimingId.value = coupon.id
  try {
    await couponApi.claim(coupon.id)
    ElMessage.success('优惠券领取成功，下单时可用')
    await loadSide()
  } finally {
    claimingId.value = null
  }
}

async function buy() {
  if (enrolled.value) {
    if (firstLessonId.value) {
      await router.push(`/learn/${route.params.id}/${firstLessonId.value}`)
    } else {
      await router.push('/user?tab=courses')
    }
    return
  }
  if (pendingOrderNo.value) {
    await router.push(`/pay/${pendingOrderNo.value}`)
    return
  }
  if (!store.isLoggedIn) {
    await router.push({
      path: '/login',
      query: { redirect: `/checkout?courseId=${route.params.id}` },
    })
    return
  }
  await router.push({ path: '/checkout', query: { courseId: route.params.id } })
}

function duration(seconds: number) {
  const minutes = Math.round((seconds || 0) / 60)
  return minutes > 0 ? `${minutes} 分钟` : ''
}

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="detail-page">
    <template v-if="detail">
      <div class="detail-hero">
        <div class="hero-inner">
          <div
            class="hero-cover"
            :style="detail.course.coverUrl ? { backgroundImage: `url(${detail.course.coverUrl})` } : undefined"
          ></div>
          <div class="hero-info">
            <div class="crumbs">
              <router-link to="/">首页</router-link> /
              <router-link to="/courses">课程中心</router-link> /
              <span>{{ detail.course.title }}</span>
            </div>
            <div class="tags">
              <span>{{ detail.course.subject }}</span>
              <span>{{ detail.course.stage }}</span>
            </div>
            <h1>{{ detail.course.title }}</h1>
            <p class="subtitle">{{ detail.course.subtitle }}</p>
            <div class="meta">
              <span>评分 {{ reviews?.reviewCount ? Number(reviews.averageRating).toFixed(1) : detail.course.rating }}</span>
              <span>{{ reviews?.reviewCount || 0 }} 条评价</span>
              <span>{{ detail.course.studentCount }} 人学习</span>
              <span>名师主讲</span>
            </div>
            <div class="teachers">
              <span v-for="teacher in detail.course.teachers" :key="teacher.id" class="teacher-chip">
                <el-avatar :size="26" :src="teacher.avatar || undefined">{{ teacher.name.slice(0, 1) }}</el-avatar>
                {{ teacher.name }} · {{ teacher.title }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div class="detail-body">
        <div class="main">
          <section class="card">
            <h2>课程介绍</h2>
            <p class="description">{{ detail.course.description || '暂无课程介绍' }}</p>
          </section>

          <section class="card">
            <div class="card-head">
              <h2>课程目录</h2>
              <span>{{ detail.chapters.length }} 章</span>
            </div>
            <el-collapse v-model="activeChapters">
              <el-collapse-item v-for="chapter in detail.chapters" :key="chapter.id" :name="chapter.id">
                <template #title>
                  <div class="chapter-title">
                    <strong>{{ chapter.title }}</strong>
                    <span>{{ chapter.lessons.length }} 课时</span>
                  </div>
                </template>
                <div v-for="lesson in chapter.lessons" :key="lesson.id" class="lesson-row">
                  <div class="lesson-name">
                    <el-icon><VideoPlay /></el-icon>
                    <span>{{ lesson.title }}</span>
                    <el-tag v-if="lesson.isTrial" size="small" type="danger" effect="plain">试听</el-tag>
                  </div>
                  <span class="duration">{{ duration(lesson.durationSec) }}</span>
                </div>
                <el-empty v-if="chapter.lessons.length === 0" description="暂无课时" :image-size="60" />
              </el-collapse-item>
            </el-collapse>
          </section>

          <section class="card">
            <div class="card-head">
              <h2>学员评价</h2>
              <span v-if="reviews?.reviewCount">
                平均 {{ Number(reviews.averageRating).toFixed(1) }} 分 · {{ reviews.reviewCount }} 条
              </span>
              <span v-else>暂无评价</span>
            </div>
            <div v-if="reviews?.items.length" class="review-list">
              <div v-for="review in reviews.items" :key="review.id" class="review-item">
                <div class="review-head">
                  <div class="review-user">
                    <el-avatar :size="32" style="background: #e60012">
                      {{ (review.userNickname || review.userAccount || '学').slice(0, 1) }}
                    </el-avatar>
                    <div>
                      <strong>{{ review.userNickname || review.userAccount || '学员' }}</strong>
                      <div class="review-date">{{ formatDate(review.createdAt) }}</div>
                    </div>
                  </div>
                  <el-rate :model-value="review.rating" disabled size="small" />
                </div>
                <p>{{ review.content }}</p>
              </div>
            </div>
            <el-empty v-else description="还没有评价，购买学习后即可评价" :image-size="60" />
          </section>
        </div>

        <aside class="side">
          <div class="buy-card">
            <div class="price">
              <strong>¥{{ Number(detail.course.price).toFixed(2) }}</strong>
              <del v-if="detail.course.originalPrice">¥{{ Number(detail.course.originalPrice).toFixed(2) }}</del>
            </div>
            <el-alert
              v-if="enrolled"
              title="您已购买该课程"
              type="success"
              :closable="false"
              show-icon
              style="margin-bottom: 12px"
            />
            <el-alert
              v-else-if="pendingOrderNo"
              :title="`有待支付订单 ${pendingOrderNo}`"
              type="warning"
              :closable="false"
              show-icon
              style="margin-bottom: 12px"
            />
            <el-button type="primary" size="large" style="width: 100%" @click="buy">
              {{ buyLabel }}
            </el-button>
            <el-button
              size="large"
              style="width: 100%; margin: 10px 0 0"
              @click="router.push(store.isLoggedIn ? '/user?tab=orders' : '/login')"
            >
              我的订单
            </el-button>
            <el-button
              size="large"
              style="width: 100%; margin: 10px 0 0"
              :loading="favoriteLoading"
              @click="toggleFavorite"
            >
              {{ favorited ? '★ 已收藏' : '☆ 收藏课程' }}
            </el-button>

            <div class="coupon-box">
              <div class="coupon-head">
                <span>可领优惠券</span>
                <span class="coupon-count">{{ coupons.length }} 张</span>
              </div>
              <div v-for="coupon in coupons" :key="coupon.id" class="coupon-item">
                <div class="coupon-head">
                  <span class="coupon-name">{{ coupon.name }}</span>
                  <span class="coupon-value">{{ couponValueText(coupon.type, coupon.value) }}</span>
                </div>
                <div class="coupon-foot">
                  <span class="coupon-validity">
                    {{ couponRuleText(coupon.type, coupon.minAmount) }} · 至 {{ formatDate(coupon.endAt) }}
                  </span>
                  <el-button
                    size="small"
                    type="danger"
                    plain
                    class="coupon-claim"
                    :disabled="Boolean(myCoupon(coupon.id))"
                    :loading="claimingId === coupon.id"
                    @click="claim(coupon)"
                  >
                    {{ claimLabel(coupon) }}
                  </el-button>
                </div>
              </div>
              <div v-if="!coupons.length" class="coupon-empty">暂无可领取的优惠券</div>
            </div>

            <ul class="promise">
              <li>支持试听</li>
              <li>名师答疑</li>
              <li>配套章节练习</li>
            </ul>
          </div>
        </aside>
      </div>
    </template>
    <el-empty v-else description="课程不存在或已下架" />
  </div>
</template>

<style scoped>
.detail-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 24px 0;
}

.detail-hero {
  background: #111827;
  border-radius: 18px;
  color: #fff;
  overflow: hidden;
}

.hero-inner {
  display: flex;
  gap: 28px;
  padding: 28px;
}

.hero-cover {
  width: 360px;
  height: 220px;
  border-radius: 12px;
  flex-shrink: 0;
  background: linear-gradient(135deg, #e60012, #ff7a00);
  background-size: cover;
  background-position: center;
}

.hero-info {
  flex: 1;
}

.crumbs {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 14px;
}

.crumbs a {
  color: #cbd5e1;
}

.tags span {
  display: inline-block;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(230, 0, 18, 0.18);
  color: #fca5a5;
  margin-right: 8px;
}

.hero-info h1 {
  font-size: 30px;
  margin: 14px 0 8px;
}

.subtitle {
  color: #cbd5e1;
  margin: 0 0 14px;
}

.meta {
  display: flex;
  gap: 18px;
  color: #94a3b8;
  font-size: 13px;
  margin-bottom: 18px;
}

.teachers {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
}

.teacher-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #e2e8f0;
}

.detail-body {
  display: flex;
  gap: 22px;
  margin-top: 22px;
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
  font-size: 20px;
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

.description {
  white-space: pre-wrap;
  color: #4b5563;
  line-height: 1.9;
  margin: 0;
}

.chapter-title {
  display: flex;
  justify-content: space-between;
  width: 100%;
  padding-right: 12px;
}

.chapter-title span {
  color: #9ca3af;
  font-size: 12px;
}

.lesson-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 4px;
  border-bottom: 1px dashed #eef0f4;
}

.lesson-row:last-child {
  border-bottom: none;
}

.lesson-name {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #374151;
  font-size: 14px;
}

.duration {
  color: #9ca3af;
  font-size: 12px;
}

.side {
  width: 300px;
}

.buy-card {
  background: #fff;
  border-radius: 14px;
  padding: 22px;
  position: sticky;
  top: 96px;
}

.price {
  margin-bottom: 16px;
}

.price strong {
  color: #e60012;
  font-size: 32px;
}

.price del {
  color: #9ca3af;
  margin-left: 8px;
  font-size: 14px;
}

.promise {
  margin: 18px 0 0;
  padding: 0;
  list-style: none;
  color: #6b7280;
  font-size: 13px;
  line-height: 2;
}

.coupon-box {
  margin-top: 18px;
  border-top: 1px dashed #eef0f4;
  padding-top: 14px;
}

.coupon-head {
  display: flex;
  justify-content: space-between;
  color: #374151;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 10px;
}

.coupon-count {
  color: #9ca3af;
  font-weight: 400;
}

.coupon-item {
  padding: 10px 0;
  border-bottom: 1px dashed var(--line);
}

.coupon-item:last-child {
  border-bottom: none;
}

.coupon-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.coupon-name {
  min-width: 0;
  font-size: 12.5px;
  font-weight: 600;
  color: #374151;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.coupon-value {
  flex-shrink: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--brand);
  font-variant-numeric: tabular-nums;
}

.coupon-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 3px;
}

.coupon-validity {
  min-width: 0;
  font-size: 11px;
  color: var(--text-faint);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.coupon-claim {
  flex-shrink: 0;
  padding: 4px 10px;
  font-size: 12px;
}

.coupon-empty {
  color: var(--text-faint);
  font-size: 12px;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.review-item {
  border: 1px solid #f1f3f5;
  border-radius: 10px;
  padding: 14px 16px;
  background: #fafafa;
}

.review-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.review-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.review-date {
  color: #9ca3af;
  font-size: 12px;
  margin-top: 2px;
}

.review-item p {
  margin: 10px 0 0;
  color: #4b5563;
  line-height: 1.7;
  white-space: pre-wrap;
}
</style>
