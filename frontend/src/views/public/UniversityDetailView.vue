<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { EChartsOption } from 'echarts'
import { publicUniversityApi } from '@/api/university'
import EChart from '@/components/EChart.vue'
import { formatMoney } from '@/utils/format'
import type { UniversityDetail } from '@/types'

const route = useRoute()
const detail = ref<UniversityDetail | null>(null)
const loading = ref(false)
const activeTab = ref('majors')
const selectedMajorId = ref<number | null>(null)

const university = computed(() => detail.value?.university)
const majors = computed(() => detail.value?.majors || [])

const scoreLines = computed(() => {
  const lines = detail.value?.scoreLines || []
  if (!selectedMajorId.value) return lines
  return lines.filter((line) => line.majorId === selectedMajorId.value)
})

const scoreChartOption = computed<EChartsOption>(() => {
  const lines = [...scoreLines.value].sort((a, b) => a.year - b.year)
  const majorName = majors.value.find((major) => major.id === selectedMajorId.value)?.name || '总分'
  return {
    grid: { left: 48, right: 32, top: 32, bottom: 32 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'line', lineStyle: { color: '#c2c2c2' } },
      valueFormatter: (value) => `${value} 分`,
    },
    xAxis: {
      type: 'category',
      data: lines.map((line) => `${line.year}`),
      axisLine: { lineStyle: { color: '#c2c2c2' } },
      axisTick: { show: false },
      axisLabel: { color: '#666666' },
    },
    yAxis: {
      type: 'value',
      min: (value) => Math.max(0, Math.floor(value.min / 10) * 10 - 10),
      splitLine: { lineStyle: { color: '#e7e7e7' } },
      axisLabel: { color: '#666666' },
    },
    series: [
      {
        name: majorName,
        type: 'line',
        data: lines.map((line) => line.total),
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { width: 2, color: '#257de0' },
        itemStyle: { color: '#257de0' },
        endLabel: {
          show: true,
          color: '#257de0',
          fontWeight: 600,
          formatter: '{c}',
        },
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
  }
})

async function load() {
  loading.value = true
  try {
    const response = await publicUniversityApi.detail(Number(route.params.id))
    detail.value = response.data.data
    selectedMajorId.value = response.data.data.majors[0]?.id ?? null
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="page">
    <template v-if="university">
      <div class="hero-card">
        <div class="logo">
          <img v-if="university.logoUrl" :src="university.logoUrl" :alt="university.name" />
          <span v-else>{{ university.name.slice(0, 1) }}</span>
        </div>
        <div class="hero-body">
          <div class="title-row">
            <h1>{{ university.name }}</h1>
            <el-tag v-if="university.level" type="danger" effect="plain">{{ university.level }}</el-tag>
            <el-tag v-if="university.type" type="info">{{ university.type }}</el-tag>
            <el-tag v-if="university.featured" type="warning" effect="plain">精选院校</el-tag>
          </div>
          <p class="meta">
            {{ university.province }} · {{ university.city || '—' }} · {{ university.majorCount }} 个招生专业
            <a v-if="university.website" :href="university.website" target="_blank" rel="noopener">学校官网 ↗</a>
          </p>
          <p class="intro">{{ university.introduction || '暂无院校简介' }}</p>
        </div>
      </div>

      <div class="page-card content-card">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="专业目录" name="majors">
            <el-table :data="majors" stripe>
              <el-table-column prop="name" label="专业名称" min-width="170" />
              <el-table-column prop="code" label="专业代码" width="120">
                <template #default="{ row }">{{ row.code || '—' }}</template>
              </el-table-column>
              <el-table-column prop="degreeType" label="培养类型" width="100" />
              <el-table-column prop="duration" label="学制" width="80">
                <template #default="{ row }">{{ row.duration || '—' }}</template>
              </el-table-column>
              <el-table-column label="学费" width="120">
                <template #default="{ row }">
                  {{ row.tuition === null || row.tuition === undefined ? '—' : formatMoney(row.tuition) }}
                </template>
              </el-table-column>
              <el-table-column prop="examSubjects" label="初试科目" min-width="260">
                <template #default="{ row }">{{ row.examSubjects || '—' }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-if="majors.length === 0" description="暂无专业数据" />
          </el-tab-pane>

          <el-tab-pane label="历年分数线" name="scores">
            <div class="tab-toolbar">
              <el-select
                v-model="selectedMajorId"
                placeholder="选择专业"
                style="width: 240px"
                clearable
                @clear="selectedMajorId = majors[0]?.id ?? null"
              >
                <el-option v-for="major in majors" :key="major.id" :label="major.name" :value="major.id" />
              </el-select>
              <span class="muted">总分走势（点击专业切换）</span>
            </div>
            <EChart v-if="scoreLines.length > 0" :option="scoreChartOption" height="240px" />
            <el-table :data="scoreLines" stripe style="margin-top: 16px">
              <el-table-column prop="majorName" label="专业" min-width="160" />
              <el-table-column prop="year" label="年份" width="90" />
              <el-table-column prop="political" label="政治" width="80">
                <template #default="{ row }">{{ row.political ?? '—' }}</template>
              </el-table-column>
              <el-table-column prop="english" label="英语" width="80">
                <template #default="{ row }">{{ row.english ?? '—' }}</template>
              </el-table-column>
              <el-table-column prop="math" label="数学" width="80">
                <template #default="{ row }">{{ row.math ?? '—' }}</template>
              </el-table-column>
              <el-table-column prop="majorScore" label="专业课" width="90">
                <template #default="{ row }">{{ row.majorScore ?? '—' }}</template>
              </el-table-column>
              <el-table-column prop="total" label="总分" width="90" />
            </el-table>
            <el-empty v-if="scoreLines.length === 0" description="暂无分数线数据" />
          </el-tab-pane>

          <el-tab-pane label="报录比" name="admission">
            <el-table :data="detail?.admissionStats || []" stripe>
              <el-table-column prop="majorName" label="专业" min-width="170" />
              <el-table-column prop="year" label="年份" width="90" />
              <el-table-column prop="applicants" label="报考人数" width="110" />
              <el-table-column prop="admitted" label="录取人数" width="110" />
              <el-table-column label="报录比" width="120">
                <template #default="{ row }">
                  {{ row.ratio === null || row.ratio === undefined ? '—' : `${Number(row.ratio).toFixed(2)} : 1` }}
                </template>
              </el-table-column>
              <el-table-column prop="minScore" label="最低录取分" width="120">
                <template #default="{ row }">{{ row.minScore ?? '—' }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-if="(detail?.admissionStats || []).length === 0" description="暂无报录比数据" />
          </el-tab-pane>
        </el-tabs>
      </div>

      <div class="back-row">
        <router-link to="/universities">← 返回院校库</router-link>
      </div>
    </template>
  </div>
</template>

<style scoped>
.page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 32px 24px 40px;
}

.hero-card {
  background: linear-gradient(120deg, #fff 0%, #fff7f2 100%);
  border-radius: 16px;
  padding: 28px;
  display: flex;
  gap: 22px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.04);
  margin-bottom: 18px;
}

.logo {
  width: 86px;
  height: 86px;
  border-radius: 18px;
  background: linear-gradient(135deg, #e60012, #ff7a00);
  color: #fff;
  font-size: 40px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-body h1 {
  margin: 0;
  font-size: 30px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.meta {
  margin: 10px 0;
  color: #6b7280;
  font-size: 14px;
}

.meta a {
  margin-left: 12px;
  color: #e60012;
}

.intro {
  margin: 0;
  color: #4b5563;
  line-height: 1.8;
  max-width: 900px;
}

.content-card {
  padding: 8px 20px 20px;
}

.tab-toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 12px;
}

.back-row {
  margin-top: 18px;
  color: #6b7280;
  font-size: 13px;
}
</style>
