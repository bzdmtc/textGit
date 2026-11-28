<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { EChartsOption } from 'echarts'
import { statsApi } from '@/api/stats'
import { systemApi } from '@/api/system'
import EChart from '@/components/EChart.vue'
import { formatMoney } from '@/utils/format'
import type { DashboardData, RouteCheck } from '@/types'

const INK = '#666666'
const INK_STRONG = '#191919'
const GRID = '#e7e7e7'
const AXIS = '#c2c2c2'
const BLUE = '#257de0'
const ORANGE = '#e76624'

const ORDER_STATUS_LABELS: Record<string, string> = {
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  CANCELED: '已取消',
  REFUNDING: '退款审核中',
  REFUNDED: '已退款',
  COMPLETED: '已完成',
}

const dashboard = ref<DashboardData | null>(null)
const days = ref(14)
const loading = ref(false)
const checking = ref(false)
const readRoute = ref<RouteCheck | null>(null)
const masterRoute = ref<RouteCheck | null>(null)

const summary = computed(() => dashboard.value?.summary)
const labels = computed(() => (dashboard.value?.trend || []).map((point) => point.date.slice(5)))

async function load() {
  loading.value = true
  try {
    const response = await statsApi.dashboard(days.value)
    dashboard.value = response.data.data
  } finally {
    loading.value = false
  }
}

const salesOption = computed<EChartsOption>(() => ({
  grid: { left: 62, right: 26, top: 24, bottom: 34 },
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'line', lineStyle: { color: AXIS } },
    valueFormatter: (value) => formatMoney(Number(value ?? 0)),
  },
  xAxis: {
    type: 'category',
    data: labels.value,
    boundaryGap: false,
    axisLine: { lineStyle: { color: AXIS } },
    axisTick: { show: false },
    axisLabel: { color: INK, interval: days.value > 14 ? 2 : days.value > 7 ? 1 : 0 },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: GRID } },
    axisLabel: {
      color: INK,
      formatter: (value: number) => (value >= 10000 ? `${(value / 10000).toFixed(1)}万` : `${value}`),
    },
  },
  series: [
    {
      name: '成交金额',
      type: 'line',
      data: (dashboard.value?.trend || []).map((point) => point.sales),
      showSymbol: false,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { width: 2, color: BLUE },
      itemStyle: { color: BLUE },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(37, 125, 224, 0.18)' },
            { offset: 1, color: 'rgba(37, 125, 224, 0)' },
          ],
        },
      },
    },
  ],
}))

const orderOption = computed<EChartsOption>(() => ({
  grid: { left: 48, right: 26, top: 24, bottom: 34 },
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(15, 23, 42, 0.04)' } },
  },
  xAxis: {
    type: 'category',
    data: labels.value,
    axisLine: { lineStyle: { color: AXIS } },
    axisTick: { show: false },
    axisLabel: { color: INK, interval: days.value > 14 ? 2 : days.value > 7 ? 1 : 0 },
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: GRID } },
    axisLabel: { color: INK },
  },
  series: [
    {
      name: '订单量',
      type: 'bar',
      data: (dashboard.value?.trend || []).map((point) => point.orders),
      barMaxWidth: 18,
      itemStyle: { color: ORANGE, borderRadius: [4, 4, 0, 0] },
    },
  ],
}))

const hotCourseOption = computed<EChartsOption>(() => {
  const items = dashboard.value?.hotCourses || []
  return {
    grid: { left: 132, right: 100, top: 12, bottom: 12 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(15, 23, 42, 0.04)' } },
      formatter: (params) => {
        const index = Array.isArray(params) ? params[0]?.dataIndex ?? 0 : 0
        const item = items[index]
        if (!item) return ''
        return `${item.title}<br/>成交 ${item.salesCount} 单 · ${formatMoney(item.salesAmount)}`
      },
    },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: GRID } },
      axisLabel: {
        color: INK,
        formatter: (value: number) => (value >= 10000 ? `${(value / 10000).toFixed(1)}万` : `${value}`),
      },
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: items.map((item) => (item.title.length > 9 ? `${item.title.slice(0, 9)}…` : item.title)),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: INK_STRONG, fontWeight: 500 },
    },
    series: [
      {
        name: '成交金额',
        type: 'bar',
        data: items.map((item) => item.salesAmount),
        barMaxWidth: 14,
        itemStyle: { color: BLUE, borderRadius: [0, 4, 4, 0] },
        label: {
          show: true,
          position: 'right',
          color: INK,
          formatter: (params) => formatMoney(Number(params.value ?? 0)),
        },
      },
    ],
  }
})

const orderStatusOption = computed<EChartsOption>(() => {
  const items = [...(dashboard.value?.orderStatus || [])].sort((a, b) => b.count - a.count)
  return {
    grid: { left: 108, right: 56, top: 12, bottom: 12 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(15, 23, 42, 0.04)' } },
    },
    xAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: GRID } },
      axisLabel: { color: INK },
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: items.map((item) => ORDER_STATUS_LABELS[item.name] || item.name),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: INK_STRONG },
    },
    series: [
      {
        name: '订单数',
        type: 'bar',
        data: items.map((item) => item.count),
        barMaxWidth: 14,
        itemStyle: { color: BLUE, borderRadius: [0, 4, 4, 0] },
        label: { show: true, position: 'right', color: INK },
      },
    ],
  }
})

async function checkRoutes() {
  checking.value = true
  try {
    const [read, master] = await Promise.all([systemApi.readRoute(), systemApi.masterRoute()])
    readRoute.value = read.data.data
    masterRoute.value = master.data.data
    ElMessage.success('读写分离路由检查完成')
  } finally {
    checking.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <div class="page-header">
      <div>
        <div class="page-title">数据看板</div>
        <div class="muted">
          经营数据按北京时间自然日统计，看板查询走主库，与订单/用户数据强一致
        </div>
      </div>
      <el-radio-group v-model="days" @change="load">
        <el-radio-button :value="7">近 7 天</el-radio-button>
        <el-radio-button :value="14">近 14 天</el-radio-button>
        <el-radio-button :value="30">近 30 天</el-radio-button>
      </el-radio-group>
    </div>

    <el-row :gutter="16">
      <el-col :span="6">
        <div class="page-card stat-card">
          <div class="stat-label">今日成交额</div>
          <div class="stat-value">{{ formatMoney(summary?.todaySales) }}</div>
          <div class="muted">累计 {{ formatMoney(summary?.totalSales) }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="page-card stat-card">
          <div class="stat-label">今日订单</div>
          <div class="stat-value">{{ summary?.todayOrders ?? 0 }}</div>
          <div class="muted">
            累计成交 {{ summary?.paidOrders ?? 0 }} 笔 / 全部 {{ summary?.totalOrders ?? 0 }} 笔
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="page-card stat-card">
          <div class="stat-label">今日新增用户</div>
          <div class="stat-value">{{ summary?.todayNewUsers ?? 0 }}</div>
          <div class="muted">累计注册 {{ summary?.totalUsers ?? 0 }} 人</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="page-card stat-card">
          <div class="stat-label">今日学习人次</div>
          <div class="stat-value">{{ summary?.todayEnrollments ?? 0 }}</div>
          <div class="muted">累计选课 {{ summary?.totalEnrollments ?? 0 }} 人次</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="secondary-row">
      <el-col :span="6">
        <div class="page-card mini-card">
          <span class="mini-label">客单价</span>
          <strong>{{ formatMoney(summary?.avgOrderAmount) }}</strong>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="page-card mini-card">
          <span class="mini-label">待处理退款</span>
          <strong :class="{ 'warn-text': (summary?.pendingRefunds ?? 0) > 0 }">
            {{ summary?.pendingRefunds ?? 0 }} 笔
          </strong>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="page-card mini-card">
          <span class="mini-label">成交订单</span>
          <strong>{{ summary?.paidOrders ?? 0 }} 笔</strong>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="page-card mini-card">
          <span class="mini-label">数据截止</span>
          <strong>{{ dashboard?.today || '—' }}</strong>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <div class="page-card chart-card">
          <div class="chart-head">
            <div>
              <div class="chart-title">成交金额趋势</div>
              <div class="muted">近 {{ days }} 天已支付订单金额</div>
            </div>
          </div>
          <EChart :option="salesOption" height="280px" />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="page-card chart-card">
          <div class="chart-head">
            <div>
              <div class="chart-title">订单量趋势</div>
              <div class="muted">近 {{ days }} 天已支付订单数</div>
            </div>
          </div>
          <EChart :option="orderOption" height="280px" />
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <div class="page-card chart-card">
          <div class="chart-head">
            <div>
              <div class="chart-title">热销课程 TOP 6</div>
              <div class="muted">按所选周期成交金额排序</div>
            </div>
          </div>
          <EChart v-if="(dashboard?.hotCourses || []).length > 0" :option="hotCourseOption" height="280px" />
          <el-empty v-else description="所选周期暂无成交课程" />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="page-card chart-card">
          <div class="chart-head">
            <div>
              <div class="chart-title">订单状态分布</div>
              <div class="muted">所选周期创建的订单</div>
            </div>
          </div>
          <EChart v-if="(dashboard?.orderStatus || []).length > 0" :option="orderStatusOption" height="280px" />
          <el-empty v-else description="所选周期暂无订单" />
        </div>
      </el-col>
    </el-row>

    <div class="page-card system-card">
      <div class="system-head">
        <div>
          <div class="chart-title">运行状态</div>
          <div class="muted">主从读写分离自检与当前菜单权限</div>
        </div>
        <el-button :loading="checking" @click="checkRoutes">检查数据库路由</el-button>
      </div>
      <div class="system-body">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="主库（写）">
            <el-tag type="danger" effect="plain">{{ masterRoute?.database || '尚未检查' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="从库（读）">
            <el-tag type="success" effect="plain">{{ readRoute?.database || '尚未检查' }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </div>
  </div>
</template>

<style scoped>
.stat-card {
  min-height: 124px;
}

.stat-label {
  color: #6b7280;
  font-size: 13px;
  margin-bottom: 12px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 8px;
  font-variant-numeric: tabular-nums;
}

.secondary-row {
  margin-top: 16px;
}

.mini-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
}

.mini-label {
  color: #6b7280;
  font-size: 13px;
}

.mini-card strong {
  font-size: 17px;
  color: #111827;
  font-variant-numeric: tabular-nums;
}

.warn-text {
  color: #dc3c3d !important;
}

.chart-row {
  margin-top: 16px;
}

.chart-card {
  padding: 18px 20px 8px;
}

.chart-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #191919;
  margin-bottom: 4px;
}

.system-card {
  margin-top: 16px;
}

.system-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 14px;
}
</style>
