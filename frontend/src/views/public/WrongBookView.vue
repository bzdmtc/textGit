<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { practiceApi, wrongQuestionApi } from '@/api/practice'
import { formatDateTime } from '@/utils/format'
import type { PracticeStats, PracticeSubject, WrongQuestion } from '@/types'

const router = useRouter()

const items = ref<WrongQuestion[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const subject = ref('')
const mastered = ref('')
const loading = ref(false)
const redoing = ref(false)
const actionId = ref<number | null>(null)
const expanded = ref<Set<number>>(new Set())
const subjects = ref<PracticeSubject[]>([])
const stats = ref<PracticeStats | null>(null)

const typeMeta: Record<string, string> = {
  SINGLE: '单选题',
  MULTIPLE: '多选题',
  JUDGE: '判断题',
}

const masteredFilter = computed(() => {
  if (mastered.value === 'true') return true
  if (mastered.value === 'false') return false
  return undefined
})

async function loadOverview() {
  const response = await practiceApi.overview()
  subjects.value = response.data.data.subjects
  stats.value = response.data.data.stats
}

async function load() {
  loading.value = true
  try {
    const response = await wrongQuestionApi.page({
      subject: subject.value || undefined,
      mastered: masteredFilter.value,
      page: page.value,
      size: size.value,
    })
    items.value = response.data.data.items
    total.value = response.data.data.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  void load()
}

function toggleAnswer(id: number) {
  const next = new Set(expanded.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  expanded.value = next
}

async function toggleMastered(item: WrongQuestion) {
  actionId.value = item.id
  try {
    const response = await wrongQuestionApi.setMastered(item.id, !item.mastered)
    const updated = response.data.data
    item.mastered = updated.mastered
    ElMessage.success(updated.mastered ? '已标记掌握' : '已恢复为待攻克')
    await Promise.all([loadOverview(), load()])
  } finally {
    actionId.value = null
  }
}

async function remove(item: WrongQuestion) {
  await ElMessageBox.confirm('确定从错题本移除此题吗？', '移除错题', { type: 'warning' })
  actionId.value = item.id
  try {
    await wrongQuestionApi.remove(item.id)
    ElMessage.success('已移除')
    await Promise.all([loadOverview(), load()])
  } finally {
    actionId.value = null
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
    // 无错题时 http 拦截器提示
  } finally {
    redoing.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadOverview(), load()])
})
</script>

<template>
  <div class="wrong-page">
    <section class="wrong-hero">
      <div class="hero-inner">
        <div>
          <div class="hero-tag">错题本</div>
          <h1>把错题变成得分点</h1>
          <p>练习和考试中答错的题目会自动归档；重做答对后自动标记掌握，也可手动标记。</p>
        </div>
        <div class="hero-stats">
          <div>
            <strong>{{ stats?.wrongCount ?? 0 }}</strong>
            <span>待攻克</span>
          </div>
          <div>
            <strong>{{ stats?.masteredCount ?? 0 }}</strong>
            <span>已掌握</span>
          </div>
          <div>
            <strong>{{ total }}</strong>
            <span>当前筛选</span>
          </div>
        </div>
      </div>
    </section>

    <section class="wrong-body">
      <el-card shadow="never" class="toolbar-card">
        <div class="toolbar">
          <el-select v-model="subject" placeholder="全部科目" clearable style="width: 220px" @change="search">
            <el-option
              v-for="item in subjects"
              :key="item.subject"
              :label="`${item.subject}（${item.questionCount} 题）`"
              :value="item.subject"
            />
          </el-select>
          <el-select v-model="mastered" placeholder="全部状态" clearable style="width: 160px" @change="search">
            <el-option label="待攻克" value="false" />
            <el-option label="已掌握" value="true" />
          </el-select>
          <el-button :disabled="!stats?.wrongCount" :loading="redoing" type="danger" @click="redoWrong">
            错题重做
          </el-button>
          <span class="toolbar-tip">错题重做只抽取未掌握的题目</span>
        </div>
      </el-card>

      <div v-loading="loading" class="wrong-list">
        <article v-for="item in items" :key="item.id" class="wrong-card" :class="{ mastered: item.mastered }">
          <div class="wrong-top">
            <div class="wrong-tags">
              <el-tag size="small" effect="plain">{{ typeMeta[item.type] || item.type }}</el-tag>
              <el-tag size="small" type="info" effect="plain">{{ item.subject }}</el-tag>
              <span class="chapter">{{ item.chapter }}</span>
              <el-tag v-if="item.mastered" size="small" type="success">已掌握</el-tag>
            </div>
            <div class="wrong-meta">
              <span>错 {{ item.wrongCount }} 次</span>
              <span>{{ formatDateTime(item.lastWrongAt) }}</span>
            </div>
          </div>

          <h3 class="stem">{{ item.stem }}</h3>

          <div class="options">
            <div
              v-for="option in item.options"
              :key="option.label"
              class="option"
              :class="{ correct: expanded.has(item.id) && item.correctAnswer.includes(option.label) }"
            >
              <span class="label">{{ option.label }}</span>
              <span>{{ option.content }}</span>
            </div>
          </div>

          <div v-if="expanded.has(item.id)" class="answer-panel">
            <p class="correct-line">正确答案：<strong>{{ item.correctAnswer.split('').join('、') }}</strong></p>
            <p v-if="item.analysis" class="analysis">解析：{{ item.analysis }}</p>
          </div>

          <div class="wrong-actions">
            <el-button link type="primary" @click="toggleAnswer(item.id)">
              {{ expanded.has(item.id) ? '隐藏答案' : '查看答案' }}
            </el-button>
            <el-button
              link
              :type="item.mastered ? 'warning' : 'success'"
              :loading="actionId === item.id"
              @click="toggleMastered(item)"
            >
              {{ item.mastered ? '恢复待攻克' : '标记已掌握' }}
            </el-button>
            <el-button link type="danger" :loading="actionId === item.id" @click="remove(item)">
              移除
            </el-button>
          </div>
        </article>

        <el-empty v-if="!loading && !items.length" description="当前筛选下暂无错题">
          <el-button type="danger" plain @click="router.push('/practice')">去题库练习</el-button>
        </el-empty>
      </div>

      <el-pagination
        v-if="total > size"
        v-model:current-page="page"
        v-model:page-size="size"
        class="pager"
        layout="total, prev, pager, next"
        :total="total"
        @current-change="load"
      />
    </section>
  </div>
</template>

<style scoped>
.wrong-page {
  min-height: 70vh;
  background: #f7f8fa;
}

.wrong-hero {
  background: linear-gradient(120deg, #7f1d1d 0%, #b91c1c 55%, #f97316 100%);
  color: #fff;
  padding: 48px 24px 64px;
}

.hero-inner {
  max-width: 1000px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 32px;
}

.hero-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  font-size: 12px;
  letter-spacing: 2px;
}

.wrong-hero h1 {
  margin: 14px 0 8px;
  font-size: 34px;
  letter-spacing: 1px;
}

.wrong-hero p {
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
}

.hero-stats {
  display: flex;
  gap: 12px;
}

.hero-stats > div {
  min-width: 92px;
  padding: 12px 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.12);
}

.hero-stats strong {
  display: block;
  font-size: 24px;
}

.hero-stats span {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
}

.wrong-body {
  max-width: 1000px;
  margin: -32px auto 0;
  padding: 0 24px 56px;
}

.toolbar-card {
  border-radius: 14px;
  margin-bottom: 18px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-tip {
  font-size: 12px;
  color: #9ca3af;
}

.wrong-list {
  min-height: 160px;
}

.wrong-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px 22px;
  margin-bottom: 14px;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.05);
  border-left: 4px solid #f97316;
}

.wrong-card.mastered {
  border-left-color: #16a34a;
  opacity: 0.86;
}

.wrong-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.wrong-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.chapter {
  font-size: 12px;
  color: #9ca3af;
}

.wrong-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #9ca3af;
}

.stem {
  margin: 0 0 12px;
  font-size: 16px;
  line-height: 1.7;
  color: #111827;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #f9fafb;
  font-size: 14px;
  color: #4b5563;
}

.option.correct {
  background: #f0fdf4;
  color: #15803d;
}

.label {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #e5e7eb;
  font-size: 12px;
  font-weight: 600;
}

.option.correct .label {
  background: #16a34a;
  color: #fff;
}

.answer-panel {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #f0fdf4;
  font-size: 13px;
  color: #4b5563;
  line-height: 1.7;
}

.correct-line {
  margin: 0;
}

.analysis {
  margin: 6px 0 0;
}

.wrong-actions {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed #f3f4f6;
}

.pager {
  margin-top: 18px;
  justify-content: flex-end;
}

@media (max-width: 760px) {
  .hero-inner {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-stats {
    width: 100%;
  }

  .hero-stats > div {
    flex: 1;
  }
}
</style>
