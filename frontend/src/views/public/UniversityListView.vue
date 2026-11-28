<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { publicUniversityApi } from '@/api/university'
import type { University, UniversityOptions } from '@/types'

const universities = ref<University[]>([])
const options = ref<UniversityOptions>({ provinces: [], levels: [], types: [] })
const total = ref(0)
const loading = ref(false)

const filters = reactive({
  keyword: '',
  province: '',
  level: '',
  type: '',
  page: 1,
  size: 12,
})

async function load() {
  loading.value = true
  try {
    const response = await publicUniversityApi.page({
      keyword: filters.keyword || undefined,
      province: filters.province || undefined,
      level: filters.level || undefined,
      type: filters.type || undefined,
      page: filters.page,
      size: filters.size,
    })
    universities.value = response.data.data.items
    total.value = response.data.data.total
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const response = await publicUniversityApi.options()
  options.value = response.data.data
}

function search() {
  filters.page = 1
  load()
}

function reset() {
  filters.keyword = ''
  filters.province = ''
  filters.level = ''
  filters.type = ''
  filters.page = 1
  load()
}

onMounted(() => {
  loadOptions()
  load()
})
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>院校库</h1>
        <p>按地区与层次筛选院校，查看专业目录、历年复试线与报录比</p>
      </div>
    </div>

    <div class="filter-card">
      <div class="filter-row">
        <span class="filter-label">地区</span>
        <el-select
          v-model="filters.province"
          placeholder="全部省份"
          clearable
          style="width: 160px"
          @change="search"
        >
          <el-option v-for="province in options.provinces" :key="province" :label="province" :value="province" />
        </el-select>
        <span class="filter-label">层次</span>
        <el-select
          v-model="filters.level"
          placeholder="全部层次"
          clearable
          style="width: 140px"
          @change="search"
        >
          <el-option v-for="level in options.levels" :key="level" :label="level" :value="level" />
        </el-select>
        <span class="filter-label">类型</span>
        <el-select
          v-model="filters.type"
          placeholder="全部类型"
          clearable
          style="width: 140px"
          @change="search"
        >
          <el-option v-for="type in options.types" :key="type" :label="type" :value="type" />
        </el-select>
      </div>
      <div class="filter-row">
        <span class="filter-label">搜索</span>
        <el-input
          v-model="filters.keyword"
          placeholder="院校名称关键词"
          clearable
          style="width: 300px"
          @keyup.enter="search"
        />
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <div v-loading="loading" class="university-grid">
      <router-link
        v-for="university in universities"
        :key="university.id"
        :to="`/universities/${university.id}`"
        class="university-card"
      >
        <div class="card-top">
          <div class="logo">
            <img v-if="university.logoUrl" :src="university.logoUrl" :alt="university.name" />
            <span v-else>{{ university.name.slice(0, 1) }}</span>
          </div>
          <div class="card-title">
            <div class="title-row">
              <h3>{{ university.name }}</h3>
              <el-tag v-if="university.featured" size="small" type="danger" effect="plain">精选</el-tag>
            </div>
            <p>{{ university.city || university.province }}</p>
          </div>
        </div>
        <div class="tags">
          <el-tag v-if="university.level" size="small" :type="university.level === '985' ? 'danger' : 'warning'">
            {{ university.level }}
          </el-tag>
          <el-tag v-if="university.type" size="small" type="info">{{ university.type }}</el-tag>
          <span class="major-count">{{ university.majorCount }} 个专业</span>
        </div>
        <p class="intro">{{ university.introduction || '暂无院校简介' }}</p>
        <div class="card-footer">
          <span>查看专业与分数线</span>
          <span>→</span>
        </div>
      </router-link>
    </div>

    <el-empty v-if="!loading && universities.length === 0" description="暂无符合条件的院校" />

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
  flex-wrap: wrap;
}

.filter-label {
  width: 52px;
  color: #6b7280;
  font-size: 13px;
}

.university-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
  min-height: 220px;
}

.university-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.04);
  transition: transform 0.2s, box-shadow 0.2s;
}

.university-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.1);
}

.card-top {
  display: flex;
  gap: 14px;
  align-items: center;
}

.logo {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: linear-gradient(135deg, #e60012, #ff7a00);
  color: #fff;
  font-size: 24px;
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

.card-title h3 {
  margin: 0;
  font-size: 18px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-title p {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.tags {
  display: flex;
  align-items: center;
  gap: 8px;
}

.major-count {
  margin-left: auto;
  color: #9ca3af;
  font-size: 12px;
}

.intro {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 44px;
}

.card-footer {
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid #f1f2f4;
  display: flex;
  justify-content: space-between;
  color: #e60012;
  font-size: 13px;
  font-weight: 600;
}

.pagination {
  display: flex;
  justify-content: center;
  margin: 28px 0;
}
</style>
