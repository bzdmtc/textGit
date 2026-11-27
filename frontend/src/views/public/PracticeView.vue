<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { examApi, practiceApi } from '@/api/practice'
import { formatDateTime } from '@/utils/format'
import type { ExamRecord, Paper, PracticeOverview } from '@/types'

const router = useRouter()

const overview = ref<PracticeOverview | null>(null)
const loading = ref(false)
const subject = ref('')
const chapter = ref('')
const count = ref(10)
const starting = ref(false)
const paperFilter = ref('')
const startingPaper = ref<number | null>(null)
const redoing = ref(false)

const paperTypeMeta: Record<string, { text: string; type: 'success' | 'warning' | 'danger' | 'info' }> = {
  CHAPTER: { text: '章节练习', type: 'success' },
  PAST: { text: '历年真题', type: 'warning' },
  MOCK: { text: '模拟考试', type: 'danger' },
}

const currentSubject = computed(() => overview.value?.subjects.find((item) => item.subject === subject.value))
const chapters = computed(() => currentSubject.value?.chapters ?? [])
const filteredPapers = computed(() => {
  const papers = overview.value?.papers ?? []
  return paperFilter.value ? papers.filter((paper) => paper.type === paperFilter.value) : papers
})
const stats = computed(() => overview.value?.stats)
const recentRecords = computed(() => overview.value?.recentRecords ?? [])

watch(subject, () => {
  chapter.value = chapters.value[0]?.chapter ?? ''
})

async function load() {
  loading.value = true
  try {
    const response = await practiceApi.overview()
    overview.value = response.data.data
    if (!subject.value && overview.value.subjects.length > 0) {
      subject.value = overview.value.subjects[0].subject
      chapter.value = overview.value.subjects[0].chapters[0]?.chapter ?? ''
    } else if (subject.value && !chapter.value) {
      chapter.value = chapters.value[0]?.chapter ?? ''
    }
  } finally {
    loading.value = false
  }
}

async function startPractice() {
  if (!subject.value) {
    ElMessage.warning('请选择练习科目')
    return
  }
  starting.value = true
  try {
    const response = await practiceApi.startPractice({
      subject: subject.value,
      chapter: chapter.value || undefined,
      count: count.value,
    })
    await router.push(`/practice/session/${response.data.data.recordId}`)
  } finally {
    starting.value = false
  }
}

async function startPaper(paper: Paper) {
  startingPaper.value = paper.id
  try {
    const response = await examApi.start(paper.id)
    await router.push(`/practice/session/${response.data.data.recordId}`)
  } finally {
    startingPaper.value = null
  }
}

async function redoWrong() {
  redoing.value = true
  try {
    const response = await practiceApi.startPractice({
      subject: subject.value || '',
      count: 10,
      source: 'WRONG',
    })
    await router.push(`/practice/session/${response.data.data.recordId}`)
  } catch {
    // 无错题时后端返回 50013，http 拦截器已提示
  } finally {
    redoing.value = false
  }
}

function continueRecord(record: ExamRecord) {
  if (record.status === 'IN_PROGRESS') {
    router.push(`/practice/session/${record.id}`)
  } else {
    router.push(`/practice/result/${record.id}`)
  }
}

function modeText(record: ExamRecord) {
  if (record.mode === 'EXAM') return '套卷'
  return record.source === 'WRONG' ? '错题重做' : '章节练习'
}

function percentText(value?: number | string | null) {
  return `${Number(value ?? 0).toFixed(1)}%`
}

function scoreText(record: ExamRecord) {
  if (record.status !== 'SUBMITTED') return '—'
  return `${Number(record.score ?? 0).toFixed(0)} / ${Number(record.totalScore ?? 0).toFixed(0)}`
}

onMounted(load)
</script>

<template>
  <div class="practice-page">
    <section class="practice-hero">
      <div class="hero-inner">
        <div>
          <div class="hero-tag">智能题库</div>
          <h1>刷题 · 模考 · 错题复盘</h1>
          <p>章节练习即答即判，限时套卷服务端判分，错题自动归档，稳步提升正确率。</p>
        </div>
        <div class="hero-stats">
          <div>
            <strong>{{ stats?.attemptCount ?? 0 }}</strong>
            <span>累计作答</span>
          </div>
          <div>
            <strong>{{ percentText(stats?.averagePercent) }}</strong>
            <span>平均得分率</span>
          </div>
          <div>
            <strong>{{ percentText(stats?.correctRate) }}</strong>
            <span>整体正确率</span>
          </div>
          <div class="stat-link" @click="router.push('/wrong-book')">
            <strong>{{ stats?.wrongCount ?? 0 }}</strong>
            <span>待攻克错题 →</span>
          </div>
        </div>
      </div>
    </section>

    <section v-loading="loading" class="practice-body">
      <div class="practice-grid">
        <el-card shadow="never" class="practice-card">
          <template #header>
            <div class="card-title">
              <span>章节练习</span>
              <small>即时判题 · 自动收集错题</small>
            </div>
          </template>
          <div class="form-row">
            <label>科目</label>
            <el-select v-model="subject" placeholder="选择科目" style="width: 100%">
              <el-option
                v-for="item in overview?.subjects ?? []"
                :key="item.subject"
                :label="`${item.subject}（${item.questionCount} 题）`"
                :value="item.subject"
              />
            </el-select>
          </div>
          <div class="form-row">
            <label>章节</label>
            <el-select v-model="chapter" placeholder="全部章节" clearable style="width: 100%">
              <el-option
                v-for="item in chapters"
                :key="item.chapter"
                :label="`${item.chapter}（${item.questionCount} 题）`"
                :value="item.chapter"
              />
            </el-select>
          </div>
          <div class="form-row">
            <label>题量</label>
            <el-radio-group v-model="count">
              <el-radio-button :value="5">5 题</el-radio-button>
              <el-radio-button :value="10">10 题</el-radio-button>
              <el-radio-button :value="20">20 题</el-radio-button>
            </el-radio-group>
          </div>
          <el-button type="danger" size="large" class="full-button" :loading="starting" @click="startPractice">
            开始章节练习
          </el-button>
          <div class="wrong-entry">
            <div>
              <strong>错题本</strong>
              <p>已掌握 {{ stats?.masteredCount ?? 0 }} 题，待攻克 {{ stats?.wrongCount ?? 0 }} 题</p>
            </div>
            <el-button :loading="redoing" plain @click="redoWrong">错题重做</el-button>
          </div>
        </el-card>

        <div class="papers-panel">
          <div class="papers-head">
            <div>
              <h2>试卷中心</h2>
              <p>限时套卷，交卷后统一判分并生成成绩报告</p>
            </div>
            <el-radio-group v-model="paperFilter" size="small">
              <el-radio-button value="">全部</el-radio-button>
              <el-radio-button value="CHAPTER">章节</el-radio-button>
              <el-radio-button value="PAST">真题</el-radio-button>
              <el-radio-button value="MOCK">模拟</el-radio-button>
            </el-radio-group>
          </div>
          <div v-if="filteredPapers.length" class="paper-grid">
            <article v-for="paper in filteredPapers" :key="paper.id" class="paper-card">
              <div class="paper-top">
                <el-tag :type="paperTypeMeta[paper.type]?.type || 'info'" effect="plain">
                  {{ paperTypeMeta[paper.type]?.text || paper.type }}
                </el-tag>
                <span class="paper-subject">{{ paper.subject }}</span>
              </div>
              <h3>{{ paper.title }}</h3>
              <p class="paper-desc">{{ paper.description || paper.chapter || '限时套卷练习' }}</p>
              <div class="paper-meta">
                <span>{{ paper.questionCount }} 题</span>
                <span>{{ Number(paper.totalScore).toFixed(0) }} 分</span>
                <span>{{ paper.durationMin }} 分钟</span>
              </div>
              <el-button
                type="danger"
                plain
                :loading="startingPaper === paper.id"
                @click="startPaper(paper)"
              >
                开始考试
              </el-button>
            </article>
          </div>
          <el-empty v-else description="暂无符合条件的试卷" />
        </div>
      </div>

      <el-card shadow="never" class="history-card">
        <template #header>
          <div class="card-title">
            <span>最近做题记录</span>
            <small>最多展示最近 5 次练习 / 考试</small>
          </div>
        </template>
        <el-table v-if="recentRecords.length" :data="recentRecords" stripe>
          <el-table-column label="题目" min-width="260">
            <template #default="{ row }">
              <div class="record-title">{{ row.title }}</div>
              <div class="muted">{{ modeText(row) }} · {{ formatDateTime(row.startedAt) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 'SUBMITTED' ? 'success' : 'warning'">
                {{ row.status === 'SUBMITTED' ? '已完成' : '进行中' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="得分" width="120">
            <template #default="{ row }">{{ scoreText(row) }}</template>
          </el-table-column>
          <el-table-column label="正确率" width="120">
            <template #default="{ row }">
              <span v-if="row.status === 'SUBMITTED'">
                {{ row.questionCount ? Math.round((row.correctCount / row.questionCount) * 100) : 0 }}%
              </span>
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="continueRecord(row)">
                {{ row.status === 'IN_PROGRESS' ? '继续作答' : '查看成绩' }}
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="还没有做题记录" />
          </template>
        </el-table>
        <el-empty v-else description="还没有做题记录，从章节练习开始吧" />
      </el-card>
    </section>
  </div>
</template>

<style scoped>
.practice-page {
  background: #f7f8fa;
  min-height: 70vh;
}

.practice-hero {
  background: linear-gradient(120deg, #111827 0%, #7f1d1d 55%, #e60012 100%);
  color: #fff;
  padding: 52px 24px 68px;
}

.hero-inner {
  max-width: 1180px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 40px;
}

.hero-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 13px;
  letter-spacing: 2px;
}

.practice-hero h1 {
  margin: 16px 0 10px;
  font-size: 38px;
  letter-spacing: 2px;
}

.practice-hero p {
  margin: 0;
  font-size: 15px;
  opacity: 0.9;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(92px, 1fr));
  gap: 12px;
}

.hero-stats > div {
  padding: 14px 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.1);
  min-width: 100px;
}

.hero-stats strong {
  display: block;
  font-size: 22px;
  font-variant-numeric: tabular-nums;
}

.hero-stats span {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
}

.stat-link {
  cursor: pointer;
  transition: background 0.2s;
}

.stat-link:hover {
  background: rgba(255, 255, 255, 0.2);
}

.practice-body {
  max-width: 1180px;
  margin: -36px auto 0;
  padding: 0 24px 56px;
}

.practice-grid {
  display: grid;
  grid-template-columns: 340px 1fr;
  gap: 24px;
  align-items: start;
}

.practice-card {
  border-radius: 16px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
}

.card-title {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  font-weight: 600;
  color: #111827;
}

.card-title small {
  font-weight: 400;
  font-size: 12px;
  color: #9ca3af;
}

.form-row {
  margin-bottom: 14px;
}

.form-row label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: #6b7280;
}

.full-button {
  width: 100%;
  margin-top: 4px;
  font-weight: 600;
  letter-spacing: 2px;
}

.wrong-entry {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.wrong-entry strong {
  font-size: 14px;
  color: #111827;
}

.wrong-entry p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #9ca3af;
}

.papers-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  margin-bottom: 16px;
}

.papers-head h2 {
  margin: 0;
  font-size: 22px;
  color: #111827;
}

.papers-head p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.paper-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 16px;
}

.paper-card {
  background: #fff;
  border-radius: 14px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.paper-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.paper-subject {
  font-size: 12px;
  color: #9ca3af;
}

.paper-card h3 {
  margin: 0;
  font-size: 17px;
  color: #111827;
}

.paper-desc {
  margin: 0;
  min-height: 36px;
  font-size: 13px;
  color: #6b7280;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.paper-meta {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: #9ca3af;
}

.history-card {
  margin-top: 24px;
  border-radius: 16px;
}

.record-title {
  font-weight: 600;
  color: #111827;
}

.muted {
  color: #9ca3af;
  font-size: 12px;
  margin-top: 2px;
}

@media (max-width: 960px) {
  .hero-inner {
    flex-direction: column;
    align-items: flex-start;
  }

  .practice-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hero-stats {
    grid-template-columns: repeat(2, 1fr);
    width: 100%;
  }

  .practice-hero h1 {
    font-size: 28px;
  }
}
</style>
