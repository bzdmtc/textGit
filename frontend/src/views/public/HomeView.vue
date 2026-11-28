<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { publicApi } from '@/api/public'
import type { HomeData } from '@/types'
import CourseCard from '@/components/CourseCard.vue'

const router = useRouter()
const data = ref<HomeData>({
  banners: [],
  hotCourses: [],
  teachers: [],
  latestArticles: [],
  universities: [],
})
const keyword = ref('')
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const response = await publicApi.home()
    data.value = response.data.data
  } finally {
    loading.value = false
  }
}

function search() {
  router.push({ path: '/courses', query: { keyword: keyword.value || undefined } })
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <section class="hero">
      <el-carousel height="360px" :interval="5000" arrow="hover">
        <el-carousel-item v-for="banner in data.banners" :key="banner.id">
          <div
            class="banner"
            :style="{ backgroundImage: `url(${banner.imageUrl})` }"
            @click="banner.linkUrl && router.push(banner.linkUrl)"
          >
            <div class="banner-content">
              <div class="banner-kicker">智学考研 · 2027 备考季</div>
              <h1>{{ banner.title }}</h1>
              <p>基础、强化、冲刺阶段课程，配套题库与院校数据。</p>
              <el-button type="primary" size="large" round>查看课程</el-button>
            </div>
          </div>
        </el-carousel-item>
        <el-carousel-item v-if="data.banners.length === 0">
          <div class="banner banner-fallback">
            <div class="banner-content">
              <div class="banner-kicker">智学考研 · 2027 备考季</div>
              <h1>一站式考研备考平台</h1>
              <p>课程、题库、院校、资料，陪你走到上岸。</p>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <section class="search-band">
      <div class="search-inner">
        <div>
          <h2>找到适合你的考研课程</h2>
          <p>政治 / 英语 / 数学 / 专业课，按阶段精准匹配</p>
        </div>
        <div class="search-box">
          <el-input
            v-model="keyword"
            size="large"
            placeholder="搜索课程，如：政治 强化"
            clearable
            @keyup.enter="search"
          />
          <el-button type="primary" size="large" @click="search">搜索</el-button>
        </div>
      </div>
    </section>

    <section class="section">
      <div class="section-head">
        <div>
          <h2>热门课程</h2>
          <p>名师主讲，阶段清晰，配套练习</p>
        </div>
        <router-link to="/courses" class="more-link">全部课程 →</router-link>
      </div>
      <div class="course-grid">
        <CourseCard v-for="course in data.hotCourses" :key="course.id" :course="course" />
      </div>
    </section>

    <section class="section teacher-section">
      <div class="section-head">
        <div>
          <h2>名师团队</h2>
          <p>多年考研教学经验，讲练结合</p>
        </div>
        <router-link to="/teachers" class="more-link">全部名师 →</router-link>
      </div>
      <div class="teacher-grid">
        <div v-for="teacher in data.teachers" :key="teacher.id" class="teacher-card">
          <div class="teacher-avatar" :style="{ backgroundImage: `url(${teacher.avatar})` }"></div>
          <h3>{{ teacher.name }}</h3>
          <p>{{ teacher.title }}</p>
          <span>{{ teacher.subject }}</span>
        </div>
      </div>
    </section>

    <section v-if="data.universities.length > 0" class="section">
      <div class="section-head">
        <div>
          <h2>院校推荐</h2>
          <p>专业目录、历年复试线与报录比一站查询</p>
        </div>
        <router-link to="/universities" class="more-link">全部院校 →</router-link>
      </div>
      <div class="university-grid">
        <router-link
          v-for="university in data.universities"
          :key="university.id"
          :to="`/universities/${university.id}`"
          class="university-item"
        >
          <div class="university-logo">{{ university.name.slice(0, 1) }}</div>
          <div class="university-body">
            <div class="university-title">
              <h3>{{ university.name }}</h3>
              <el-tag v-if="university.level" size="small" type="danger" effect="plain">
                {{ university.level }}
              </el-tag>
            </div>
            <p>{{ university.province }} · {{ university.type || '综合' }} · {{ university.majorCount }} 个专业</p>
          </div>
          <span class="university-arrow">→</span>
        </router-link>
      </div>
    </section>

    <section class="section">
      <div class="section-head">
        <div>
          <h2>考研资讯</h2>
          <p>政策、经验与复习方法</p>
        </div>
        <router-link to="/articles" class="more-link">更多资讯 →</router-link>
      </div>
      <div class="article-grid">
        <router-link
          v-for="article in data.latestArticles"
          :key="article.id"
          :to="`/articles/${article.id}`"
          class="article-card"
        >
          <div class="article-cover" :style="{ backgroundImage: `url(${article.coverUrl})` }"></div>
          <div class="article-body">
            <span>{{ article.categoryName || '考研资讯' }}</span>
            <h3>{{ article.title }}</h3>
            <p>{{ article.summary }}</p>
          </div>
        </router-link>
      </div>
    </section>
  </div>
</template>

<style scoped>
.hero {
  max-width: 1180px;
  margin: 24px auto 0;
  padding: 0 24px;
}

.banner {
  height: 100%;
  border-radius: 18px;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  cursor: pointer;
  overflow: hidden;
}

.banner-fallback {
  background: linear-gradient(120deg, #e60012, #ff7a00);
}

.banner-content {
  padding: 0 64px;
  color: #fff;
  max-width: 620px;
}

.banner-kicker {
  font-size: 13px;
  letter-spacing: 2px;
  opacity: 0.9;
  margin-bottom: 12px;
}

.banner-content h1 {
  font-size: 42px;
  margin: 0 0 12px;
  letter-spacing: 1px;
}

.banner-content p {
  font-size: 16px;
  opacity: 0.92;
  margin: 0 0 24px;
}

.search-band {
  max-width: 1180px;
  margin: 28px auto 0;
  padding: 0 24px;
}

.search-inner {
  background: #fff;
  border-radius: 16px;
  padding: 22px 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.05);
}

.search-inner h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.search-inner p {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
}

.search-box {
  display: flex;
  gap: 10px;
  width: 460px;
}

.section {
  max-width: 1180px;
  margin: 44px auto 0;
  padding: 0 24px;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-head h2 {
  margin: 0 0 6px;
  font-size: 26px;
}

.section-head p {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
}

.more-link {
  color: #e60012;
  font-size: 13px;
  font-weight: 600;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}

.teacher-section {
  background: #fff;
  border-radius: 18px;
  padding: 28px;
  max-width: 1132px;
}

.teacher-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}

.teacher-card {
  text-align: center;
  padding: 18px;
  border-radius: 12px;
  background: #f8fafc;
}

.teacher-avatar {
  width: 84px;
  height: 84px;
  border-radius: 50%;
  margin: 0 auto 12px;
  background: linear-gradient(135deg, #e60012, #ff7a00);
  background-size: cover;
  background-position: center;
}

.teacher-card h3 {
  margin: 0 0 6px;
  font-size: 17px;
}

.teacher-card p {
  margin: 0 0 8px;
  color: #6b7280;
  font-size: 13px;
}

.teacher-card span {
  font-size: 12px;
  color: #e60012;
  background: #fef2f2;
  padding: 3px 10px;
  border-radius: 999px;
}

.article-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
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

.article-cover {
  height: 140px;
  background: linear-gradient(135deg, #e60012, #ff7a00);
  background-size: cover;
  background-position: center;
}

.article-body {
  padding: 16px;
}

.article-body span {
  color: #e60012;
  font-size: 12px;
}

.article-body h3 {
  margin: 8px 0;
  font-size: 16px;
  color: #111827;
}

.article-body p {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.university-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.university-item {
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 14px;
  padding: 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.university-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.08);
}

.university-logo {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  background: linear-gradient(135deg, #e60012, #ff7a00);
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.university-body {
  flex: 1;
}

.university-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.university-title h3 {
  margin: 0;
  font-size: 16px;
  color: #111827;
}

.university-body p {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 12px;
}

.university-arrow {
  color: #e60012;
  font-weight: 700;
}

@media (max-width: 1000px) {
  .course-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
