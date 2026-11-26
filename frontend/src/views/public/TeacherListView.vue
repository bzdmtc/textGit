<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { publicApi } from '@/api/public'
import type { Teacher } from '@/types'

const teachers = ref<Teacher[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const response = await publicApi.teachers()
    teachers.value = response.data.data
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <div class="page-head">
      <h1>名师团队</h1>
      <p>多年考研教学经验，讲练结合，陪你走到上岸</p>
    </div>
    <div v-loading="loading" class="teacher-grid">
      <div v-for="teacher in teachers" :key="teacher.id" class="teacher-card">
        <div class="avatar" :style="teacher.avatar ? { backgroundImage: `url(${teacher.avatar})` } : undefined"></div>
        <div class="info">
          <div class="name-row">
            <h2>{{ teacher.name }}</h2>
            <el-tag size="small" type="danger" effect="plain">{{ teacher.subject }}</el-tag>
          </div>
          <p class="title">{{ teacher.title }}</p>
          <p class="intro">{{ teacher.introduction }}</p>
        </div>
      </div>
    </div>
    <el-empty v-if="!loading && teachers.length === 0" description="暂无讲师" />
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

.teacher-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}

.teacher-card {
  background: #fff;
  border-radius: 14px;
  padding: 22px;
  display: flex;
  gap: 18px;
  border: 1px solid #eef0f4;
}

.avatar {
  width: 96px;
  height: 96px;
  border-radius: 16px;
  flex-shrink: 0;
  background: linear-gradient(135deg, #e60012, #ff7a00);
  background-size: cover;
  background-position: center;
}

.info {
  flex: 1;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.name-row h2 {
  margin: 0;
  font-size: 20px;
}

.title {
  color: #e60012;
  font-size: 13px;
  margin: 6px 0 10px;
}

.intro {
  color: #6b7280;
  font-size: 13px;
  line-height: 1.7;
  margin: 0;
}
</style>
