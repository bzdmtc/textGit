<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Download, Reading, School, VideoPlay } from '@element-plus/icons-vue'
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

function go(url?: string) {
  if (url) router.push(url)
}

const features = [
  { icon: VideoPlay, title: '名师主讲', desc: '阶段清晰 · 讲练结合', path: '/courses' },
  { icon: Reading, title: '智能题库', desc: '章节练习 · 模拟考试', path: '/practice' },
  { icon: School, title: '院校数据', desc: '专业目录 · 复试线', path: '/universities' },
  { icon: Download, title: '资料下载', desc: '公式手册 · 高频词汇', path: '/materials' },
]

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
              <div class="banner-actions">
                <el-button type="primary" size="large" round @click.stop="go(banner.linkUrl || '/courses')">
                  查看课程
                </el-button>
                <el-button size="large" round class="banner-ghost" @click.stop="router.push('/practice')">
                  免费试练
                </el-button>
              </div>
            </div>
          </div>
        </el-carousel-item>
        <el-carousel-item v-if="data.banners.length === 0">
          <div class="banner banner-fallback">
            <div class="banner-content">
              <div class="banner-kicker">智学考研 · 2027 备考季</div>
              <h1>一站式考研备考平台</h1>
              <p>课程、题库、院校、资料，陪你走到上岸。</p>
              <div class="banner-actions">
                <el-button type="primary" size="large" round @click.stop="router.push('/courses')">
                  查看课程
                </el-button>
                <el-button size="large" round class="banner-ghost" @click.stop="router.push('/practice')">
                  免费试练
                </el-button>
              </div>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <section class="search-band">
      <div class="quick-card">
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

        <div class="feature-row">
          <router-link v-for="item in features" :key="item.path" :to="item.path" class="feature-item">
            <span class="feature-icon">
              <el-icon><component :is="item.icon" /></el-icon>
            </span>
            <span class="feature-text">
              <strong>{{ item.title }}</strong>
              <small>{{ item.desc }}</small>
            </span>
          </router-link>
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
  position: relative;
  height: 100%;
  border-radius: 18px;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  cursor: pointer;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.banner::after {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(15, 23, 42, 0.55) 0%, rgba(15, 23, 42, 0.22) 48%, rgba(15, 23, 42, 0) 80%);
  pointer-events: none;
}

.banner-fallback {
  background: linear-gradient(120deg, var(--brand), var(--accent));
}

.banner-content {
  position: relative;
  z-index: 1;
  padding: 0 64px;
  color: #fff;
  max-width: 620px;
}

.banner-kicker {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 12px;
  letter-spacing: 2px;
  margin-bottom: 14px;
}

.banner-content h1 {
  font-size: 42px;
  margin: 0 0 12px;
  letter-spacing: 1px;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.18);
}

.banner-content p {
  font-size: 16px;
  opacity: 0.92;
  margin: 0 0 24px;
}

.banner-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hero :deep(.el-carousel__indicators--horizontal) {
  bottom: 16px;
}

.hero :deep(.el-carousel__indicator .el-carousel__button) {
  width: 26px;
  height: 3px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.5);
  opacity: 1;
  transition:
    width 0.25s ease,
    background-color 0.25s ease;
}

.hero :deep(.el-carousel__indicator.is-active .el-carousel__button) {
  width: 36px;
  background: #fff;
}

.banner-content :deep(.el-button--primary) {
  box-shadow: 0 10px 22px rgba(230, 0, 18, 0.3);
}

.banner-ghost {
  --el-button-bg-color: transparent;
  --el-button-border-color: rgba(255, 255, 255, 0.45);
  --el-button-text-color: #fff;
  --el-button-hover-bg-color: rgba(255, 255, 255, 0.16);
  --el-button-hover-border-color: rgba(255, 255, 255, 0.85);
  --el-button-hover-text-color: #fff;
  --el-button-active-bg-color: rgba(255, 255, 255, 0.22);
  --el-button-active-border-color: #fff;
  backdrop-filter: blur(4px);
}

.search-band {
  max-width: 1180px;
  margin: 24px auto 0;
  padding: 0 24px;
}

.quick-card {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 18px;
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.search-inner {
  padding: 22px 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.feature-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border-top: 1px solid var(--line);
  background: linear-gradient(180deg, #fff, #fbfcfe);
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px 22px;
  border-left: 1px solid var(--line);
  transition: background-color 0.18s ease;
}

.feature-item:first-child {
  border-left: none;
}

.feature-item:hover {
  background: var(--brand-soft);
}

.feature-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 20px;
  flex-shrink: 0;
  transition:
    background-color 0.18s ease,
    color 0.18s ease;
}

.feature-item:hover .feature-icon {
  background: var(--brand);
  color: #fff;
}

.feature-text {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
  min-width: 0;
}

.feature-text strong {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-strong);
}

.feature-text small {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-faint);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.search-inner h2 {
  margin: 0 0 6px;
  font-size: 20px;
  color: var(--text-strong);
}

.search-inner p {
  margin: 0;
  color: var(--text-muted);
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
  gap: 16px;
  margin-bottom: 20px;
}

.section-head h2 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 6px;
  font-size: 25px;
  letter-spacing: 0.3px;
  color: var(--text-strong);
}

.section-head h2::before {
  content: "";
  width: 4px;
  height: 22px;
  border-radius: 999px;
  background: linear-gradient(180deg, var(--brand), var(--accent));
}

.section-head p {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
}

.more-link {
  padding: 6px 10px;
  border-radius: 8px;
  color: var(--brand);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  transition: background-color 0.18s ease;
}

.more-link:hover {
  background: var(--brand-soft);
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.teacher-section {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 18px;
  padding: 28px;
  max-width: 1132px;
}

.teacher-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.teacher-card {
  text-align: center;
  padding: 22px 18px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid transparent;
  transition:
    transform 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease;
}

.teacher-card:hover {
  transform: translateY(-3px);
  background: #fff;
  border-color: var(--line);
  box-shadow: var(--shadow-sm);
}

.teacher-avatar {
  width: 84px;
  height: 84px;
  border-radius: 50%;
  margin: 0 auto 12px;
  background: linear-gradient(135deg, var(--brand), var(--accent));
  background-size: cover;
  background-position: center;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.12);
}

.teacher-card h3 {
  margin: 0 0 6px;
  font-size: 17px;
  color: var(--text-strong);
}

.teacher-card p {
  margin: 0 0 8px;
  color: var(--text-muted);
  font-size: 13px;
}

.teacher-card span {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--brand);
  background: var(--brand-soft);
}

.article-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.article-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--line);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.article-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
}

.article-cover {
  height: 140px;
  background: linear-gradient(135deg, var(--brand), var(--accent));
  background-size: cover;
  background-position: center;
}

.article-body {
  padding: 16px;
}

.article-body span {
  color: var(--brand);
  font-size: 12px;
}

.article-body h3 {
  margin: 8px 0;
  font-size: 16px;
  color: var(--text-strong);
  line-height: 1.45;
}

.article-body p {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.university-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.university-item {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.university-item:hover {
  transform: translateY(-3px);
  border-color: var(--brand-line);
  box-shadow: var(--shadow-md);
}

.university-logo {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--brand), var(--accent));
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
  min-width: 0;
}

.university-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.university-title h3 {
  margin: 0;
  font-size: 16px;
  color: var(--text-strong);
}

.university-body p {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.university-arrow {
  color: var(--brand);
  font-weight: 700;
  transition: transform 0.18s ease;
}

.university-item:hover .university-arrow {
  transform: translateX(3px);
}

@media (max-width: 1080px) {
  .course-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .teacher-grid,
  .article-grid,
  .university-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .search-box {
    width: 380px;
  }

  .feature-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .feature-item {
    border-top: 1px solid var(--line);
  }

  .feature-item:nth-child(-n + 2) {
    border-top: none;
  }

  .feature-item:nth-child(odd) {
    border-left: none;
  }
}
</style>
