<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { publicApi } from '@/api/public'
import type { Article } from '@/types'

const route = useRoute()
const article = ref<Article | null>(null)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const response = await publicApi.articleDetail(Number(route.params.id))
    article.value = response.data.data
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading" class="page">
    <template v-if="article">
      <div class="crumbs">
        <router-link to="/">首页</router-link> /
        <router-link to="/articles">考研资讯</router-link> /
        <span>{{ article.categoryName || '资讯' }}</span>
      </div>
      <article class="article">
        <span class="category">{{ article.categoryName || '考研资讯' }}</span>
        <h1>{{ article.title }}</h1>
        <div class="meta">
          <span>{{ article.author }}</span>
          <span>{{ article.publishedAt?.replace('T', ' ').slice(0, 16) }}</span>
          <span>{{ article.viewCount }} 阅读</span>
        </div>
        <div class="cover" :style="article.coverUrl ? { backgroundImage: `url(${article.coverUrl})` } : undefined"></div>
        <div class="content" v-html="article.contentHtml || `<p>${article.summary || ''}</p>`"></div>
      </article>
    </template>
    <el-empty v-else description="文章不存在" />
  </div>
</template>

<style scoped>
.page {
  max-width: 860px;
  margin: 0 auto;
  padding: 32px 24px 0;
}

.crumbs {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 18px;
}

.article {
  background: #fff;
  border-radius: 16px;
  padding: 40px 48px;
}

.category {
  color: #e60012;
  font-size: 12px;
  font-weight: 600;
}

h1 {
  margin: 12px 0 14px;
  font-size: 32px;
  line-height: 1.35;
}

.meta {
  display: flex;
  gap: 18px;
  color: #9ca3af;
  font-size: 13px;
  margin-bottom: 24px;
}

.cover {
  height: 280px;
  border-radius: 12px;
  background: linear-gradient(135deg, #e60012, #ff7a00);
  background-size: cover;
  background-position: center;
  margin-bottom: 28px;
}

.content {
  color: #374151;
  line-height: 2;
  font-size: 15px;
}

.content :deep(p) {
  margin: 0 0 18px;
}
</style>
