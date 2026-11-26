<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { publicApi } from '@/api/public'
import type { Course } from '@/types'
import CourseCard from '@/components/CourseCard.vue'

const route = useRoute()
const courses = ref<Course[]>([])
const total = ref(0)
const loading = ref(false)
const filters = reactive({
  keyword: typeof route.query.keyword === 'string' ? route.query.keyword : '',
  subject: '',
  stage: '',
  page: 1,
  size: 12,
})

const subjects = ['政治', '英语一', '英语二', '数学一', '数学二', '数学三', '计算机408', '专业课']
const stages = ['基础', '强化', '冲刺', '复试']

async function load() {
  loading.value = true
  try {
    const response = await publicApi.courses({ ...filters })
    courses.value = response.data.data.items
    total.value = response.data.data.total
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 1
  load()
}

function reset() {
  filters.keyword = ''
  filters.subject = ''
  filters.stage = ''
  filters.page = 1
  load()
}

watch(() => route.query.keyword, (value) => {
  filters.keyword = typeof value === 'string' ? value : ''
  filters.page = 1
  load()
})

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>课程中心</h1>
        <p>按科目与阶段筛选，找到适合你的备考节奏</p>
      </div>
    </div>

    <div class="filter-card">
      <div class="filter-row">
        <span class="filter-label">科目</span>
        <el-radio-group v-model="filters.subject" @change="search">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button v-for="subject in subjects" :key="subject" :value="subject">
            {{ subject }}
          </el-radio-button>
        </el-radio-group>
      </div>
      <div class="filter-row">
        <span class="filter-label">阶段</span>
        <el-radio-group v-model="filters.stage" @change="search">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button v-for="stage in stages" :key="stage" :value="stage">
            {{ stage }}
          </el-radio-button>
        </el-radio-group>
      </div>
      <div class="filter-row">
        <span class="filter-label">搜索</span>
        <el-input v-model="filters.keyword" placeholder="课程关键词" clearable style="width: 300px" @keyup.enter="search" />
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <div v-loading="loading" class="course-grid">
      <CourseCard v-for="course in courses" :key="course.id" :course="course" />
    </div>
    <el-empty v-if="!loading && courses.length === 0" description="暂无符合条件的课程" />

    <div class="pagination">
      <el-pagination
        v-model:current-page="filters.page"
        :page-size="filters.size"
        :total="total"
        layout="prev, pager, next"
        @current-change="load"
      />
    </div>
  </div>
</template>

<style scoped>
.page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 32px 24px 0;
}

.page-head h1 {
  margin: 0 0 8px;
  font-size: 30px;
}

.page-head p {
  margin: 0 0 24px;
  color: #6b7280;
}

.filter-card {
  background: #fff;
  border-radius: 14px;
  padding: 18px 22px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 24px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.04);
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-label {
  width: 52px;
  color: #6b7280;
  font-size: 13px;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  min-height: 200px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

@media (max-width: 1000px) {
  .course-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
