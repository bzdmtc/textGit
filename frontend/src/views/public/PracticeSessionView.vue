<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { examApi } from '@/api/practice'
import { parseUtc } from '@/utils/format'
import type { AnswerFeedback, ExamSession } from '@/types'

const route = useRoute()
const router = useRouter()
const recordId = Number(route.params.recordId)

const session = ref<ExamSession | null>(null)
const loading = ref(true)
const saving = ref(false)
const submitting = ref(false)
const currentIndex = ref(0)
const answers = ref<Record<number, string>>({})
const feedbacks = ref<Record<number, AnswerFeedback>>({})
const remaining = ref(0)
const savedHint = ref('')
let timer: number | undefined
let submitted = false

const isExam = computed(() => session.value?.mode === 'EXAM')
const isPractice = computed(() => session.value?.mode === 'PRACTICE')
const questions = computed(() => session.value?.questions ?? [])
const current = computed(() => questions.value[currentIndex.value])
const answeredCount = computed(() => questions.value.filter((item) => Boolean(answers.value[item.questionId])).length)
const currentAnswer = computed(() => (current.value ? answers.value[current.value.questionId] ?? '' : ''))
const currentFeedback = computed(() => (current.value ? feedbacks.value[current.value.questionId] : undefined))
const typeMeta: Record<string, { text: string; type: 'success' | 'warning' | 'danger' | 'info' }> = {
  SINGLE: { text: '单选题', type: 'success' },
  MULTIPLE: { text: '多选题', type: 'warning' },
  JUDGE: { text: '判断题', type: 'info' },
}

function formatClock(seconds: number) {
  const safe = Math.max(0, seconds)
  const hours = Math.floor(safe / 3600)
  const minutes = Math.floor((safe % 3600) / 60)
  const secs = safe % 60
  const pad = (value: number) => String(value).padStart(2, '0')
  return hours > 0 ? `${pad(hours)}:${pad(minutes)}:${pad(secs)}` : `${pad(minutes)}:${pad(secs)}`
}

async function load() {
  loading.value = true
  try {
    const response = await examApi.session(recordId)
    const data = response.data.data
    if (data.status === 'SUBMITTED') {
      await router.replace(`/practice/result/${recordId}`)
      return
    }
    session.value = data
    const answerMap: Record<number, string> = {}
    const feedbackMap: Record<number, AnswerFeedback> = {}
    for (const state of data.answers) {
      if (state.answer) {
        answerMap[state.questionId] = state.answer
      }
      if (state.isCorrect !== null && state.isCorrect !== undefined) {
        feedbackMap[state.questionId] = {
          questionId: state.questionId,
          answer: state.answer ?? '',
          isCorrect: state.isCorrect,
          earnedScore: state.earnedScore,
          correctAnswer: state.correctAnswer,
          analysis: state.analysis,
        }
      }
    }
    answers.value = answerMap
    feedbacks.value = feedbackMap
    if (data.mode === 'EXAM') {
      startCountdown()
    }
  } finally {
    loading.value = false
  }
}

function startCountdown() {
  const deadline = parseUtc(session.value?.deadlineAt)?.getTime()
  const tick = () => {
    if (!deadline) {
      remaining.value = Math.max(0, session.value?.remainingSeconds ?? 0)
      return
    }
    remaining.value = Math.max(0, Math.floor((deadline - Date.now()) / 1000))
    if (remaining.value === 0) {
      void autoSubmit()
    }
  }
  tick()
  timer = window.setInterval(tick, 1000)
}

async function autoSubmit() {
  if (submitted) return
  submitted = true
  if (timer) window.clearInterval(timer)
  ElMessage.warning('考试时间到，正在自动交卷')
  try {
    await examApi.submit(recordId)
  } finally {
    await router.replace(`/practice/result/${recordId}`)
  }
}

function isSelected(label: string) {
  return currentAnswer.value.includes(label)
}

function isCorrectOption(label: string) {
  const feedback = currentFeedback.value
  return Boolean(isPractice.value && feedback?.correctAnswer?.includes(label))
}

function isWrongOption(label: string) {
  const feedback = currentFeedback.value
  return Boolean(isPractice.value && feedback && feedback.isCorrect === false
    && isSelected(label) && !feedback.correctAnswer?.includes(label))
}

async function selectOption(label: string) {
  if (!current.value || submitting.value) return
  const question = current.value
  let next: string
  if (question.type === 'MULTIPLE') {
    const set = new Set((answers.value[question.questionId] ?? '').split('').filter(Boolean))
    if (set.has(label)) {
      set.delete(label)
    } else {
      set.add(label)
    }
    next = [...set].sort().join('')
  } else {
    next = label
  }
  answers.value[question.questionId] = next
  saving.value = true
  savedHint.value = ''
  try {
    const response = await examApi.saveAnswer(recordId, question.questionId, next)
    const feedback = response.data.data
    if (isPractice.value) {
      if (feedback.isCorrect === null || feedback.isCorrect === undefined) {
        const next = { ...feedbacks.value }
        delete next[question.questionId]
        feedbacks.value = next
      } else {
        feedbacks.value[question.questionId] = feedback
      }
    } else {
      if ((feedback.answer || '') !== next) {
        answers.value[question.questionId] = feedback.answer || ''
      }
      savedHint.value = '答案已保存'
    }
  } catch (error) {
    const code = (error as { response?: { data?: { code?: number } } })?.response?.data?.code
    if (code === 50009) {
      submitted = true
      if (timer) window.clearInterval(timer)
      try {
        await examApi.submit(recordId)
      } finally {
        await router.replace(`/practice/result/${recordId}`)
      }
    }
  } finally {
    saving.value = false
  }
}

function statusOf(questionId: number) {
  const answer = answers.value[questionId]
  const feedback = feedbacks.value[questionId]
  if (!answer) return 'unanswered'
  if (isPractice.value && feedback) {
    return feedback.isCorrect ? 'correct' : 'wrong'
  }
  return 'answered'
}

function goPrev() {
  if (currentIndex.value > 0) {
    currentIndex.value--
  }
}

function goNext() {
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value++
  }
}

async function submit() {
  const unanswered = questions.value.filter((item) => !answers.value[item.questionId]).length
  await ElMessageBox.confirm(
    unanswered > 0
      ? `还有 ${unanswered} 题未作答，确定${isExam.value ? '交卷' : '结束练习'}吗？`
      : `确定${isExam.value ? '交卷' : '结束练习'}并查看成绩吗？`,
    isExam.value ? '交卷确认' : '结束练习',
    { type: 'warning' },
  )
  submitting.value = true
  try {
    await examApi.submit(recordId)
    submitted = true
    if (timer) window.clearInterval(timer)
    await router.replace(`/practice/result/${recordId}`)
  } finally {
    submitting.value = false
  }
}

function correctText() {
  const feedback = currentFeedback.value
  if (!feedback?.correctAnswer) return ''
  return feedback.correctAnswer.split('').map((label) => {
    const option = current.value?.options.find((item) => item.label === label)
    return option ? `${label}. ${option.content}` : label
  }).join('；')
}

onMounted(load)

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<template>
  <div v-loading="loading" class="session-page">
    <div v-if="session" class="session-shell">
      <div class="session-top">
        <div class="top-left">
          <el-button text @click="router.push('/practice')">← 返回题库</el-button>
          <div>
            <h1>{{ session.title }}</h1>
            <div class="top-meta">
              <el-tag size="small" :type="isExam ? 'danger' : 'success'" effect="plain">
                {{ isExam ? '限时套卷' : session.source === 'WRONG' ? '错题重做' : '章节练习' }}
              </el-tag>
              <span>共 {{ questions.length }} 题 · 总分 {{ Number(session.totalScore).toFixed(0) }} 分</span>
              <span>已答 {{ answeredCount }} / {{ questions.length }}</span>
            </div>
          </div>
        </div>
        <div class="top-right">
          <div v-if="isExam" class="countdown" :class="{ urgent: remaining <= 60 }">
            <span>剩余时间</span>
            <strong>{{ formatClock(remaining) }}</strong>
          </div>
          <div v-else class="practice-hint">
            <span>即时判题</span>
            <strong>{{ answeredCount }} / {{ questions.length }}</strong>
          </div>
        </div>
      </div>

      <div class="session-grid">
        <div class="question-panel">
          <el-card v-if="current" shadow="never" class="question-card">
            <div class="question-head">
              <div>
                <span class="question-index">第 {{ currentIndex + 1 }} 题</span>
                <el-tag size="small" :type="typeMeta[current.type]?.type || 'info'" effect="plain">
                  {{ typeMeta[current.type]?.text || current.type }}
                </el-tag>
              </div>
              <span class="question-score">{{ Number(current.score).toFixed(0) }} 分</span>
            </div>

            <h2 class="stem">{{ current.stem }}</h2>
            <p v-if="current.type === 'MULTIPLE'" class="multi-tip">多选题：请选择所有正确选项后自动判题</p>

            <div class="option-list">
              <button
                v-for="option in current.options"
                :key="option.label"
                type="button"
                class="option-card"
                :class="{
                  selected: isSelected(option.label),
                  'correct-option': isCorrectOption(option.label),
                  'wrong-option': isWrongOption(option.label),
                }"
                :disabled="submitting"
                @click="selectOption(option.label)"
              >
                <span class="option-label">{{ option.label }}</span>
                <span class="option-content">{{ option.content }}</span>
                <span v-if="isCorrectOption(option.label)" class="option-mark">✓</span>
                <span v-else-if="isWrongOption(option.label)" class="option-mark">✕</span>
              </button>
            </div>

            <div v-if="isPractice && currentFeedback" class="feedback-panel"
                 :class="{ correct: currentFeedback.isCorrect, wrong: !currentFeedback.isCorrect }">
              <div class="feedback-head">
                <strong>{{ currentFeedback.isCorrect ? '回答正确' : '回答错误' }}</strong>
                <span>本题得分 {{ Number(currentFeedback.earnedScore ?? 0).toFixed(0) }} 分</span>
              </div>
              <p v-if="!currentFeedback.isCorrect" class="correct-line">正确答案：{{ correctText() }}</p>
              <p v-if="currentFeedback.analysis" class="analysis-line">解析：{{ currentFeedback.analysis }}</p>
            </div>
            <div v-else-if="isExam" class="exam-tip">
              <span v-if="saving">保存中…</span>
              <span v-else>{{ savedHint || '选择答案后自动保存，交卷后统一判分' }}</span>
            </div>

            <div class="question-actions">
              <el-button :disabled="currentIndex === 0" @click="goPrev">上一题</el-button>
              <el-button :disabled="currentIndex >= questions.length - 1" @click="goNext">下一题</el-button>
              <el-button type="danger" :loading="submitting" @click="submit">
                {{ isExam ? '交卷' : '结束练习' }}
              </el-button>
            </div>
          </el-card>
        </div>

        <aside class="answer-panel">
          <el-card shadow="never" class="answer-card">
            <template #header>
              <div class="answer-title">
                <span>答题卡</span>
                <small>{{ answeredCount }} / {{ questions.length }}</small>
              </div>
            </template>
            <div class="answer-grid">
              <button
                v-for="(question, index) in questions"
                :key="question.questionId"
                type="button"
                class="answer-dot"
                :class="[statusOf(question.questionId), { active: index === currentIndex }]"
                @click="currentIndex = index"
              >
                {{ index + 1 }}
              </button>
            </div>
            <div class="legend">
              <span><i class="dot answered" />已答</span>
              <span><i class="dot correct" />正确</span>
              <span><i class="dot wrong" />错误</span>
              <span><i class="dot unanswered" />未答</span>
            </div>
            <el-button class="submit-button" type="danger" :loading="submitting" @click="submit">
              {{ isExam ? '交卷并查看成绩' : '结束练习' }}
            </el-button>
            <p class="rule">
              {{ isExam ? '倒计时结束将自动交卷；未作答题目按 0 分计。' : '练习模式每答一题立即判题，错题自动进入错题本。' }}
            </p>
          </el-card>
        </aside>
      </div>
    </div>
  </div>
</template>

<style scoped>
.session-page {
  min-height: 70vh;
  background: #f5f6f8;
  padding: 28px 24px 64px;
}

.session-shell {
  max-width: 1180px;
  margin: 0 auto;
}

.session-top {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
  margin-bottom: 20px;
}

.top-left {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.top-left h1 {
  margin: 0 0 8px;
  font-size: 22px;
  color: #111827;
}

.top-meta {
  display: flex;
  gap: 12px;
  align-items: center;
  font-size: 13px;
  color: #6b7280;
}

.countdown,
.practice-hint {
  min-width: 150px;
  padding: 12px 16px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
  text-align: center;
}

.countdown span,
.practice-hint span {
  display: block;
  font-size: 12px;
  color: #9ca3af;
}

.countdown strong,
.practice-hint strong {
  display: block;
  margin-top: 4px;
  font-size: 24px;
  color: #111827;
  font-variant-numeric: tabular-nums;
}

.countdown.urgent strong {
  color: #e60012;
}

.session-grid {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 20px;
  align-items: start;
}

.question-card {
  border-radius: 16px;
  padding: 8px;
}

.question-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.question-index {
  margin-right: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.question-score {
  font-size: 13px;
  color: #9ca3af;
}

.stem {
  margin: 0 0 8px;
  font-size: 20px;
  line-height: 1.7;
  color: #111827;
  white-space: pre-wrap;
}

.multi-tip {
  margin: 0 0 14px;
  font-size: 12px;
  color: #f59e0b;
}

.option-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
}

.option-card {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 15px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: all 0.18s;
  font-size: 15px;
  color: #374151;
}

.option-card:hover {
  border-color: #fca5a5;
  background: #fff7f7;
}

.option-card.selected {
  border-color: #e60012;
  background: #fef2f2;
  box-shadow: 0 0 0 2px rgba(230, 0, 18, 0.08);
}

.option-card.correct-option {
  border-color: #16a34a;
  background: #f0fdf4;
  box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.08);
}

.option-card.wrong-option {
  border-color: #dc2626;
  background: #fef2f2;
}

.option-label {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
}

.option-card.selected .option-label {
  background: #e60012;
  color: #fff;
}

.option-card.correct-option .option-label {
  background: #16a34a;
  color: #fff;
}

.option-card.wrong-option .option-label {
  background: #dc2626;
  color: #fff;
}

.option-content {
  flex: 1;
  line-height: 1.6;
}

.option-mark {
  font-weight: 700;
  color: #16a34a;
}

.wrong-option .option-mark {
  color: #dc2626;
}

.feedback-panel {
  margin-top: 22px;
  padding: 16px 18px;
  border-radius: 12px;
  border-left: 4px solid #16a34a;
  background: #f0fdf4;
}

.feedback-panel.wrong {
  border-left-color: #dc2626;
  background: #fef2f2;
}

.feedback-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  color: #111827;
}

.correct-line,
.analysis-line {
  margin: 10px 0 0;
  font-size: 14px;
  line-height: 1.7;
  color: #4b5563;
}

.exam-tip {
  margin-top: 20px;
  font-size: 13px;
  color: #9ca3af;
}

.question-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f3f4f6;
}

.answer-card {
  border-radius: 16px;
  position: sticky;
  top: 92px;
}

.answer-title {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-weight: 600;
  color: #111827;
}

.answer-title small {
  color: #9ca3af;
  font-weight: 400;
}

.answer-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.answer-dot {
  height: 36px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #fff;
  color: #6b7280;
  cursor: pointer;
  font-variant-numeric: tabular-nums;
}

.answer-dot.answered {
  border-color: #93c5fd;
  background: #eff6ff;
  color: #1d4ed8;
}

.answer-dot.correct {
  border-color: #86efac;
  background: #f0fdf4;
  color: #15803d;
}

.answer-dot.wrong {
  border-color: #fca5a5;
  background: #fef2f2;
  color: #b91c1c;
}

.answer-dot.active {
  outline: 2px solid #e60012;
  outline-offset: 1px;
}

.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
  font-size: 12px;
  color: #6b7280;
}

.legend .dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  margin-right: 4px;
  border-radius: 3px;
  border: 1px solid #e5e7eb;
  background: #fff;
}
.legend .dot.answered { background: #eff6ff; border-color: #93c5fd; }
.legend .dot.correct { background: #f0fdf4; border-color: #86efac; }
.legend .dot.wrong { background: #fef2f2; border-color: #fca5a5; }

.submit-button {
  width: 100%;
  margin-top: 18px;
}

.rule {
  margin: 12px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: #9ca3af;
}

@media (max-width: 900px) {
  .session-grid {
    grid-template-columns: 1fr;
  }

  .answer-panel {
    order: -1;
  }

  .answer-card {
    position: static;
  }

  .session-top {
    flex-direction: column;
  }
}
</style>
