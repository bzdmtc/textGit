<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { publicApi } from '@/api/public'
import type { Material } from '@/types'

const materials = ref<Material[]>([])
const subject = ref('')
const loading = ref(false)
const subjects = ['政治', '英语', '数学', '专业课']

async function load() {
  loading.value = true
  try {
    const response = await publicApi.materials(subject.value || undefined)
    materials.value = response.data.data
  } finally {
    loading.value = false
  }
}

function sizeText(size: number) {
  if (!size) return '-'
  if (size > 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`
  if (size > 1024) return `${(size / 1024).toFixed(0)} KB`
  return `${size} B`
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-head">
      <h1>资料下载</h1>
      <p>考研复习资料、公式手册与高频词汇表</p>
    </div>

    <div class="filter-row">
      <el-radio-group v-model="subject" @change="load">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button v-for="item in subjects" :key="item" :value="item">{{ item }}</el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading" class="material-list">
      <div v-for="material in materials" :key="material.id" class="material-card">
        <div class="icon">资料</div>
        <div class="body">
          <h2>{{ material.title }}</h2>
          <div class="meta">
            <el-tag size="small" effect="plain">{{ material.subject }}</el-tag>
            <span>{{ sizeText(material.fileSize) }}</span>
            <span>{{ material.downloadCount }} 次下载</span>
          </div>
        </div>
        <a :href="material.fileUrl" target="_blank" rel="noopener">
          <el-button type="primary" plain>下载</el-button>
        </a>
      </div>
    </div>
    <el-empty v-if="!loading && materials.length === 0" description="暂无资料" />
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

.filter-row {
  margin-bottom: 20px;
}

.material-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.material-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 22px;
  display: flex;
  align-items: center;
  gap: 18px;
  border: 1px solid #eef0f4;
}

.icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: #fef2f2;
  color: #e60012;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
}

.body {
  flex: 1;
}

.body h2 {
  margin: 0 0 8px;
  font-size: 17px;
  color: #111827;
}

.meta {
  display: flex;
  align-items: center;
  gap: 14px;
  color: #9ca3af;
  font-size: 12px;
}
</style>
