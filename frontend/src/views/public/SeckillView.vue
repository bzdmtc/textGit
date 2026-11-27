<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { seckillApi } from '@/api/seckill'
import { useAuthStore } from '@/stores/auth'
import { parseUtc } from '@/utils/format'
import type { PublicSeckill, SeckillResult } from '@/types'

const store = useAuthStore()
const router = useRouter()

const activities = ref<PublicSeckill[]>([])
const loading = ref(false)
const now = ref(Date.now())
const clockOffset = ref(0)
const claiming = ref<Record<number, boolean>>({})
const results = ref<Record<number, SeckillResult>>({})

let tickTimer: number | undefined
let reloadTimer: number | undefined
const pollTimers = new Map<number, number>()

function serverNow() {
  return now.value + clockOffset.value
}

function startMs(activity: PublicSeckill) {
  return parseUtc(activity.startAt)?.getTime() ?? 0
}

function endMs(activity: PublicSeckill) {
  return parseUtc(activity.endAt)?.getTime() ?? 0
}

function phaseOf(activity: PublicSeckill) {
  const current = serverNow()
  if (current < startMs(activity)) return 'UPCOMING'
  if (current > endMs(activity)) return 'ENDED'
  return 'ACTIVE'
}

function phaseText(activity: PublicSeckill) {
  const phase = phaseOf(activity)
  if (phase === 'UPCOMING') return '即将开始'
  if (phase === 'ENDED') return '已结束'
  return '抢购中'
}

function countdown(activity: PublicSeckill) {
  const phase = phaseOf(activity)
  if (phase === 'ENDED') return '本场秒杀已结束'
  const target = phase === 'UPCOMING' ? startMs(activity) : endMs(activity)
  let seconds = Math.max(0, Math.floor((target - serverNow()) / 1000))
  const days = Math.floor(seconds / 86400)
  seconds %= 86400
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60
  const pad = (value: number) => String(value).padStart(2, '0')
  const clock = `${pad(hours)}:${pad(minutes)}:${pad(secs)}`
  return `${phase === 'UPCOMING' ? '距开始' : '距结束'} ${days > 0 ? days + ' 天 ' : ''}${clock}`
}

function soldPercent(activity: PublicSeckill) {
  if (!activity.totalStock) return 0
  return Math.min(100, Math.round((activity.soldCount / activity.totalStock) * 100))
}

function buttonText(activity: PublicSeckill) {
  const result = results.value[activity.id]
  if (result?.status === 'QUEUED' || claiming.value[activity.id]) return '排队中…'
  if (result?.status === 'CREATED' && result.orderNo) return '去支付'
  if (result?.status === 'PAID') return '已购买'
  const phase = phaseOf(activity)
  if (phase === 'UPCOMING') return '即将开始'
  if (phase === 'ENDED') return '已结束'
  if (activity.remainingStock <= 0) return '已抢光'
  return '立即抢购'
}

function buttonDisabled(activity: PublicSeckill) {
  const result = results.value[activity.id]
  if (claiming.value[activity.id]) return true
  if (result?.status === 'QUEUED') return true
  if (result?.status === 'PAID') return true
  const phase = phaseOf(activity)
  return phase !== 'ACTIVE' || activity.remainingStock <= 0
}

async function load() {
  loading.value = true
  try {
    const response = await seckillApi.list()
    const data = response.data.data
    activities.value = data
    if (data.length > 0) {
      const serverTime = parseUtc(data[0].serverTime)?.getTime()
      if (serverTime) clockOffset.value = serverTime - Date.now()
    }
  } finally {
    loading.value = false
  }
}

async function resumePending() {
  if (!store.isLoggedIn) return
  for (const activity of activities.value) {
    if (phaseOf(activity) !== 'ACTIVE') continue
    try {
      const response = await seckillApi.result(activity.id)
      const result = response.data.data
      results.value[activity.id] = result
      if (result.status === 'QUEUED') {
        pollResult(activity.id)
      }
    } catch {
      // 忽略单个活动的恢复失败
    }
  }
}

function pollResult(activityId: number) {
  const existing = pollTimers.get(activityId)
  if (existing) window.clearTimeout(existing)
  let attempts = 0
  const tick = async () => {
    attempts++
    try {
      const response = await seckillApi.result(activityId)
      const result = response.data.data
      results.value[activityId] = result
      if (result.status === 'CREATED' && result.orderNo) {
        ElMessage.success('抢购成功，请尽快完成支付')
        await router.push(`/pay/${result.orderNo}`)
        return
      }
      if (result.status === 'PAID') {
        ElMessage.success('该秒杀订单已支付')
        await router.push('/user?tab=orders')
        return
      }
      if (result.status === 'FAILED') {
        ElMessage.error(result.reason || '抢购失败，名额已释放')
        await load()
        return
      }
      if (result.status === 'CANCELED') {
        ElMessage.warning('秒杀订单已取消，名额已释放')
        await load()
        return
      }
    } catch {
      // 轮询失败继续重试
    }
    if (attempts < 30) {
      pollTimers.set(activityId, window.setTimeout(tick, 1000))
    }
  }
  void tick()
}

async function claim(activity: PublicSeckill) {
  if (!store.isLoggedIn) {
    await router.push({ path: '/login', query: { redirect: '/seckill' } })
    return
  }
  if (buttonDisabled(activity)) {
    const result = results.value[activity.id]
    if (result?.status === 'CREATED' && result.orderNo) {
      await router.push(`/pay/${result.orderNo}`)
    }
    return
  }
  claiming.value[activity.id] = true
  try {
    await seckillApi.claim(activity.id)
    ElMessage.success('抢购请求已提交，正在排队生成订单…')
    results.value[activity.id] = {
      activityId: activity.id,
      status: 'QUEUED',
      phase: phaseOf(activity),
      serverTime: new Date().toISOString(),
    }
    pollResult(activity.id)
    await load()
  } catch {
    // 错误信息已由 http 拦截器提示
  } finally {
    claiming.value[activity.id] = false
  }
}

const hasActivities = computed(() => activities.value.length > 0)

onMounted(async () => {
  await load()
  await resumePending()
  tickTimer = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
  reloadTimer = window.setInterval(() => {
    void load()
  }, 15000)
})

onBeforeUnmount(() => {
  if (tickTimer) window.clearInterval(tickTimer)
  if (reloadTimer) window.clearInterval(reloadTimer)
  pollTimers.forEach((timer) => window.clearTimeout(timer))
  pollTimers.clear()
})
</script>

<template>
  <div class="seckill-page">
    <section class="seckill-hero">
      <div class="hero-inner">
        <div class="hero-tag">限时秒杀</div>
        <h1>好课低价，手慢无</h1>
        <p>整点开抢 · 限量名额 · 每人限购 1 份，抢到后 15 分钟内完成支付</p>
      </div>
    </section>

    <section v-loading="loading" class="seckill-body">
      <div v-if="hasActivities" class="seckill-grid">
        <article
          v-for="activity in activities"
          :key="activity.id"
          class="seckill-card"
          :class="phaseOf(activity).toLowerCase()"
        >
          <div class="cover">
            <img v-if="activity.courseCover" :src="activity.courseCover" :alt="activity.courseTitle" />
            <div v-else class="cover-placeholder">考研好课</div>
            <span class="phase-badge">{{ phaseText(activity) }}</span>
          </div>
          <div class="card-body">
            <h3>{{ activity.courseTitle }}</h3>
            <p class="subtitle">{{ activity.courseSubtitle || activity.subject || '限时特惠课程' }}</p>
            <div class="price-row">
              <span class="seckill-price">¥{{ Number(activity.seckillPrice).toFixed(2) }}</span>
              <span class="origin-price">¥{{ Number(activity.coursePrice).toFixed(2) }}</span>
              <span class="save-tag">
                省 ¥{{ Math.max(0, Number(activity.coursePrice) - Number(activity.seckillPrice)).toFixed(2) }}
              </span>
            </div>
            <el-progress
              :percentage="soldPercent(activity)"
              :stroke-width="8"
              :show-text="false"
              color="#e60012"
            />
            <div class="stock-row">
              <span>已抢 {{ activity.soldCount }} / {{ activity.totalStock }}</span>
              <strong>剩余 {{ activity.remainingStock }} 份</strong>
            </div>
            <div class="countdown">{{ countdown(activity) }}</div>
            <el-button
              class="claim-button"
              type="danger"
              size="large"
              :loading="claiming[activity.id]"
              :disabled="buttonDisabled(activity)"
              @click="claim(activity)"
            >
              {{ buttonText(activity) }}
            </el-button>
            <p class="rule-tip">秒杀订单需在 15 分钟内支付，超时名额自动释放</p>
          </div>
        </article>
      </div>
      <el-empty v-else-if="!loading" description="暂无进行中的秒杀活动，敬请期待" />
    </section>
  </div>
</template>

<style scoped>
.seckill-page {
  background: #f7f8fa;
  min-height: 60vh;
}

.seckill-hero {
  background: linear-gradient(120deg, #b91c1c 0%, #e60012 45%, #ff7a00 100%);
  color: #fff;
  padding: 56px 24px 72px;
}

.hero-inner {
  max-width: 1180px;
  margin: 0 auto;
}

.hero-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  font-size: 13px;
  letter-spacing: 2px;
}

.seckill-hero h1 {
  margin: 16px 0 10px;
  font-size: 40px;
  letter-spacing: 2px;
}

.seckill-hero p {
  margin: 0;
  font-size: 15px;
  opacity: 0.92;
}

.seckill-body {
  max-width: 1180px;
  margin: -40px auto 0;
  padding: 0 24px 56px;
}

.seckill-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.seckill-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
}

.cover {
  position: relative;
  height: 170px;
  background: #fee2e2;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #b91c1c;
  font-size: 20px;
  letter-spacing: 4px;
}

.phase-badge {
  position: absolute;
  top: 14px;
  left: 14px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(17, 24, 39, 0.78);
  color: #fff;
  font-size: 12px;
}

.seckill-card.active .phase-badge {
  background: #e60012;
}

.seckill-card.upcoming .phase-badge {
  background: #f59e0b;
}

.card-body {
  padding: 20px 20px 22px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card-body h3 {
  margin: 0;
  font-size: 18px;
  color: #111827;
}

.subtitle {
  margin: 0;
  min-height: 20px;
  color: #6b7280;
  font-size: 13px;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.seckill-price {
  color: #e60012;
  font-size: 28px;
  font-weight: 700;
}

.origin-price {
  color: #9ca3af;
  text-decoration: line-through;
  font-size: 14px;
}

.save-tag {
  margin-left: auto;
  padding: 2px 8px;
  border-radius: 6px;
  background: #fef2f2;
  color: #e60012;
  font-size: 12px;
}

.stock-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #6b7280;
}

.stock-row strong {
  color: #b91c1c;
}

.countdown {
  padding: 8px 12px;
  border-radius: 8px;
  background: #fff7ed;
  color: #c2410c;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.claim-button {
  width: 100%;
  font-weight: 600;
  letter-spacing: 2px;
}

.rule-tip {
  margin: 0;
  text-align: center;
  color: #9ca3af;
  font-size: 12px;
}
</style>
