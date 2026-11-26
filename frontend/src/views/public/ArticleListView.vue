<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { publicApi } from '@/api/public'
import type { Article, ArticleCategory } from '@/types'

const articles = ref<Article[]>([])
const categories = ref<ArticleCategory[]>([])
const total = ref(0)
const loading = ref(false)
const filters = reactive({ categoryId: undefined as number | undefined, keyword: '', page: 1, size: 9 })

async function load() {
  loading.value = true
  try {
    const response = await publicApi.articles({ ...filters })
    articles.value = response.data.data.items
    total.value = response.data.data.total
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 1
  load()
}

onMounted(async () => {
  const categoryResponse = await publicApi.articleCategories()
  categories.value = categoryResponse.data.data
  await load()
})
</script>

<template>
  <div class="page">
    <div class="page-head">
      <h1>考研资讯</h1>
      <p>政策解读、复习经验与备考方法</p>
    </div>

    <div class="filter-row">
      <el-radio-group v-model="filters.categoryId" @change="search">
        <el-radio-button :value="undefined">全部</el-radio-button>
        <el-radio-button v-for="category in categories" :key="category.id" :value="category.id">
          {{ category.name }}
        </el-radio-button>
      </el-radio-group>
      <div class="search-box">
        <el-input v-model="filters.keyword" placeholder="搜索文章" clearable @keyup.enter="search" />
        <el-button type="primary" @click="search">搜索</el-button>
      </div>
    </div>

    <div v-loading="loading" class="article-grid">
      <router-link v-for="article in articles" :key="article.id" :to="`/articles/${article.id}`" class="article-card">
        <div class="cover" :style="article.coverUrl ? { backgroundImage: `url(${article.coverUrl})` } : undefined"></div>
        <div class="body">
          <span class="category">{{ article.categoryName || '考研资讯' }}</span>
          <h2>{{ article.title }}</h2>
          <p>{{ article.summary }}</p>
          <div class="meta">
            <span>{{ article.author }}</span>
            <span>{{ article.viewCount }} 阅读</span>
          </div>
        </div>
      </router-link>
    </div>
    <el-empty v-if="!loading && articles.length === 0" description="暂无文章" />

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

.filter-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  margin-bottom: 22px;
}

.search-box {
  display: flex;
  gap: 8px;
  width: 300px;
}

.article-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
  min-height: 160px;
}

.article-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid #eef0f4;
  transition: transform 0.2s;
}

.article-card:hover {
  transform: translateY(-3px);
}

.cover {
  height: 150px;
  background: linear-gradient(135deg, #e60012, #ff7a00);
  background-size: cover;
  background-position: center;
}

.body {
  padding: 16px;
}

.category {
  color: #e60012;
  font-size: 12px;
}

.body h2 {
  margin: 8px 0;
  font-size: 16px;
  line-height: 1.5;
  color: #111827;
}

.body p {
  margin: 0 0 12px;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta {
  display: flex;
  justify-content: space-between;
  color: #9ca3af;
  font-size: 12px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}
</style>
