<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { examApi } from '@/api/practice'
import { formatDateTime } from '@/utils/format'
import type { ExamResult } from '@/types'

const route = useRoute()
const router = useRouter()
const recordId = Number(route.params.recordId)

const result = ref<ExamResult | null>(null)
const loading = ref(true)
const restarting = ref(false)

const percent = computed(() => {
  if (!result.value || Number(result.value.totalScore) <= 0) return 0
  return Math.round((Number(result.value.score) / Number(result.value.totalScore)) * 100)
})

const typeMeta: Record<string, string> = {
  SINGLE: '单选题',
  MULTIPLE: '多选题',
  JUDGE: '判断题',
}

const modeText = computed(() => {
  if (!result.value) return ''
  if (result.value.record.mode === 'EXAM') return '限时套卷'
  return result.value.record.source === 'WRONG' ? '错题重做' : '章节练习'
})

function durationText() {
  const seconds = result.value?.durationSeconds
  if (seconds === null || seconds === undefined) return '—'
  const minutes = Math.floor(seconds / 60)
  const rest = seconds % 60
  return minutes > 0 ? `${minutes} 分 ${rest} 秒` : `${rest} 秒`
}

function optionClass(questionIndex: number, label: string) {
  const question = result.value?.questions[questionIndex]
  if (!question) return {}
  const correct = question.correctAnswer?.includes(label)
  const mine = question.myAnswer?.includes(label)
  return {
    'correct-option': correct,
    'wrong-option': mine && !correct,
    'my-option': mine,
  }
}

function answerText(answer?: string | null) {
  if (!answer) return '未作答'
  return answer.split('').join('、')
}

async function restart() {
  if (!result.value) return
  if (result.value.record.mode === 'EXAM' && result.value.record.paperId) {
    restarting.value = true
    try {
      const response = await examApi.start(result.value.record.paperId)
      await router.push(`/practice/session/${response.data.data.recordId}`)
    } catch {
      // 试卷可能已下架
    } finally {
      restarting.value = false
    }
    return
  }
  ElMessage.info('返回题库选择新的练习')
  await router.push('/practice')
}

async function load() {
  loading.value = true
  try {
    const response = await examApi.result(recordId)
    result.value = response.data.data
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="result-page">
    <div v-if="result" class="result-shell">
      <section class="result-hero">
        <div class="hero-left">
          <div class="hero-tag">{{ modeText }} · 成绩报告</div>
          <h1>{{ result.record.title }}</h1>
          <p>
            交卷时间 {{ formatDateTime(result.record.submittedAt) }}
            <span class="divider">·</span>
            用时 {{ durationText() }}
          </p>
          <div class="hero-actions">
            <el-button type="danger" :loading="restarting" @click="restart">
              {{ result.record.mode === 'EXAM' ? '再考一次' : '再练一次' }}
            </el-button>
            <el-button plain @click="router.push('/wrong-book')">查看错题本</el-button>
            <el-button text @click="router.push('/practice')">返回题库</el-button>
          </div>
        </div>
        <div class="score-ring">
          <el-progress
            type="dashboard"
            :percentage="percent"
            :width="150"
            :stroke-width="12"
            color="#e60012"
          >
            <template #default>
              <div class="ring-score">{{ Number(result.score).toFixed(0) }}</div>
              <div class="ring-total">/ {{ Number(result.totalScore).toFixed(0) }} 分</div>
            </template>
          </el-progress>
        </div>
      </section>

      <section class="stat-row">
        <div class="stat-block correct">
          <strong>{{ result.correctCount }}</strong>
          <span>答对</span>
        </div>
        <div class="stat-block wrong">
          <strong>{{ result.wrongCount }}</strong>
          <span>答错</span>
        </div>
        <div class="stat-block unanswered">
          <strong>{{ result.unansweredCount }}</strong>
          <span>未作答</span>
        </div>
        <div class="stat-block">
          <strong>{{ percent }}%</strong>
          <span>得分率</span>
        </div>
      </section>

      <section class="review-list">
        <div class="review-head">
          <h2>答题回顾</h2>
          <span>正确答案已高亮，答错题目已自动进入错题本</span>
        </div>

        <article
          v-for="(question, index) in result.questions"
          :key="question.questionId"
          class="review-card"
          :class="{ wrong: !question.isCorrect }"
        >
          <div class="review-top">
            <div class="review-tags">
              <span class="review-index">第 {{ index + 1 }} 题</span>
              <el-tag size="small" effect="plain">{{ typeMeta[question.type] || question.type }}</el-tag>
              <el-tag size="small" type="info" effect="plain">{{ question.subject }}</el-tag>
              <span class="chapter">{{ question.chapter }}</span>
            </div>
            <div class="review-score">
              <span :class="question.isCorrect ? 'gain' : 'lose'">
                {{ Number(question.earnedScore).toFixed(0) }} / {{ Number(question.score).toFixed(0) }} 分
              </span>
            </div>
          </div>

          <h3 class="review-stem">{{ question.stem }}</h3>

          <div class="review-options">
            <div
              v-for="option in question.options"
              :key="option.label"
              class="review-option"
              :class="optionClass(index, option.label)"
            >
              <span class="option-label">{{ option.label }}</span>
              <span class="option-content">{{ option.content }}</span>
              <span v-if="question.correctAnswer.includes(option.label)" class="option-tag correct-tag">正确答案</span>
              <span
                v-else-if="question.myAnswer?.includes(option.label)"
                class="option-tag my-tag"
              >我的选择</span>
            </div>
          </div>

          <div class="answer-line">
            <span>我的答案：<strong :class="question.isCorrect ? 'ok' : 'bad'">{{ answerText(question.myAnswer) }}</strong></span>
            <span>正确答案：<strong class="ok">{{ answerText(question.correctAnswer) }}</strong></span>
          </div>

          <div v-if="question.analysis" class="analysis">
            <strong>解析</strong>
            <p>{{ question.analysis }}</p>
          </div>
        </article>
      </section>
    </div>
  </div>
</template>

<style scoped>
.result-page {
  min-height: 70vh;
  background: #f5f6f8;
  padding: 28px 24px 64px;
}

.result-shell {
  max-width: 1040px;
  margin: 0 auto;
}

.result-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 32px;
  padding: 32px 36px;
  border-radius: 20px;
  background: linear-gradient(120deg, #111827 0%, #7f1d1d 60%, #e60012 100%);
  color: #fff;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.2);
}

.hero-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 12px;
  letter-spacing: 1px;
}

.hero-left h1 {
  margin: 14px 0 10px;
  font-size: 28px;
}

.hero-left p {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.78);
}

.divider {
  margin: 0 6px;
}

.hero-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.hero-actions :deep(.el-button.is-text) {
  color: #fff;
}

.score-ring {
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  padding: 14px;
}

.score-ring :deep(.el-progress__text) {
  color: #fff;
}

.ring-score {
  font-size: 40px;
  font-weight: 700;
  line-height: 1.1;
  color: #fff;
}

.ring-total {
  margin-top: 4px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-top: 18px;
}

.stat-block {
  background: #fff;
  border-radius: 14px;
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.05);
}

.stat-block strong {
  font-size: 26px;
  color: #111827;
}

.stat-block span {
  font-size: 13px;
  color: #9ca3af;
}

.stat-block.correct strong { color: #16a34a; }
.stat-block.wrong strong { color: #dc2626; }
.stat-block.unanswered strong { color: #6b7280; }

.review-list {
  margin-top: 30px;
}

.review-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 16px;
  margin-bottom: 14px;
}

.review-head h2 {
  margin: 0;
  font-size: 22px;
  color: #111827;
}

.review-head span {
  font-size: 13px;
  color: #9ca3af;
}

.review-card {
  background: #fff;
  border-radius: 16px;
  padding: 22px 24px;
  margin-bottom: 16px;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.05);
  border-left: 4px solid #16a34a;
}

.review-card.wrong {
  border-left-color: #dc2626;
}

.review-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.review-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.review-index {
  font-weight: 600;
  color: #111827;
}

.chapter {
  font-size: 12px;
  color: #9ca3af;
}

.gain {
  color: #16a34a;
  font-weight: 600;
}

.lose {
  color: #dc2626;
  font-weight: 600;
}

.review-stem {
  margin: 0 0 14px;
  font-size: 17px;
  line-height: 1.7;
  color: #111827;
  white-space: pre-wrap;
}

.review-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.review-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid #f3f4f6;
  font-size: 14px;
  color: #4b5563;
}

.review-option.correct-option {
  border-color: #86efac;
  background: #f0fdf4;
}

.review-option.wrong-option {
  border-color: #fca5a5;
  background: #fef2f2;
}

.option-label {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  font-weight: 600;
  font-size: 13px;
}

.correct-option .option-label {
  background: #16a34a;
  color: #fff;
}

.wrong-option .option-label {
  background: #dc2626;
  color: #fff;
}

.option-content {
  flex: 1;
}

.option-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
}

.correct-tag {
  color: #15803d;
  background: #dcfce7;
}

.my-tag {
  color: #b91c1c;
  background: #fee2e2;
}

.answer-line {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  margin-top: 14px;
  font-size: 13px;
  color: #6b7280;
}

.answer-line strong.ok { color: #15803d; }
.answer-line strong.bad { color: #b91c1c; }

.analysis {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #f9fafb;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.7;
}

.analysis strong {
  color: #111827;
}

.analysis p {
  margin: 6px 0 0;
}

@media (max-width: 820px) {
  .result-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .review-head {
    flex-direction: column;
  }
}
</style>
