<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Delete, Edit, VideoPlay } from '@element-plus/icons-vue'
import { learnApi } from '@/api/learn'
import type { LearnCourse, LearnLesson, NoteItem } from '@/types'

const route = useRoute()
const router = useRouter()

const courseId = computed(() => Number(route.params.courseId))
const course = ref<LearnCourse | null>(null)
const loading = ref(false)
const notes = ref<NoteItem[]>([])
const notesLoading = ref(false)
const noteContent = ref('')
const noteSubmitting = ref(false)
const editingNoteId = ref<number | null>(null)
const editingContent = ref('')
const activePanel = ref('notes')
const video = ref<HTMLVideoElement | null>(null)
const reporting = ref(false)
const lastReportedSec = ref(0)
const favoriteLoading = ref(false)
const reviewRating = ref(5)
const reviewContent = ref('')
const reviewSubmitting = ref(false)

const currentLesson = computed<LearnLesson | null>(() => {
  if (!course.value) return null
  const lessonId = Number(route.params.lessonId)
  const all = course.value.chapters.flatMap((chapter) => chapter.lessons)
  return all.find((lesson) => lesson.id === lessonId)
    || all.find((lesson) => lesson.id === course.value?.lastLessonId)
    || all[0]
    || null
})

const lessonIndex = computed(() => {
  if (!course.value) return { current: 0, total: 0 }
  const all = course.value.chapters.flatMap((chapter) => chapter.lessons)
  const index = all.findIndex((lesson) => lesson.id === currentLesson.value?.id)
  return { current: index + 1, total: all.length }
})

function formatDuration(seconds?: number | null) {
  const value = Math.max(0, Math.round(seconds || 0))
  const minutes = Math.floor(value / 60)
  const rest = value % 60
  return `${minutes}:${String(rest).padStart(2, '0')}`
}

function lessonIcon(lesson: LearnLesson) {
  return lesson.completed ? Check : VideoPlay
}

async function loadCourse() {
  loading.value = true
  try {
    const response = await learnApi.course(courseId.value)
    course.value = response.data.data
    const target = currentLesson.value
    if (target && Number(route.params.lessonId) !== target.id) {
      await router.replace(`/learn/${courseId.value}/${target.id}`)
    }
    if (course.value.myReview) {
      reviewRating.value = course.value.myReview.rating
      reviewContent.value = course.value.myReview.content
    }
  } catch {
    course.value = null
  } finally {
    loading.value = false
  }
}

async function loadNotes() {
  if (!currentLesson.value) return
  notesLoading.value = true
  try {
    const response = await learnApi.notes(courseId.value, currentLesson.value.id)
    notes.value = response.data.data
  } finally {
    notesLoading.value = false
  }
}

async function report(completed = false) {
  const element = video.value
  const lesson = currentLesson.value
  if (!element || !lesson || reporting.value) return
  const watched = Math.floor(element.currentTime || lesson.watchedSec || 0)
  if (!completed && Math.abs(watched - lastReportedSec.value) < 5) return
  reporting.value = true
  try {
    const response = await learnApi.reportProgress(courseId.value, lesson.id, {
      watchedSec: watched,
      completed,
    })
    const result = response.data.data
    lesson.watchedSec = result.watchedSec
    lesson.completed = result.completed
    lastReportedSec.value = result.watchedSec
    if (course.value) course.value.progressPercent = result.courseProgressPercent
    if (result.completed) {
      ElMessage.success('本节已标记完成')
    }
  } finally {
    reporting.value = false
  }
}

function onTimeUpdate() {
  const element = video.value
  if (!element || !currentLesson.value) return
  const watched = Math.floor(element.currentTime)
  if (watched - lastReportedSec.value >= 10) {
    void report(false)
  }
}

function onLoadedMetadata() {
  const element = video.value
  const lesson = currentLesson.value
  if (!element || !lesson) return
  lastReportedSec.value = lesson.watchedSec || 0
  if (lesson.watchedSec && lesson.watchedSec > 0) {
    element.currentTime = lesson.watchedSec
  }
}

async function switchLesson(lesson: LearnLesson) {
  await report(false)
  await router.push(`/learn/${courseId.value}/${lesson.id}`)
  await loadNotes()
  await nextTick()
  if (video.value) {
    video.value.load()
  }
}

async function addNote() {
  if (!currentLesson.value || !noteContent.value.trim()) {
    ElMessage.warning('请先填写笔记内容')
    return
  }
  noteSubmitting.value = true
  try {
    const response = await learnApi.createNote(courseId.value, currentLesson.value.id, noteContent.value.trim())
    notes.value.unshift(response.data.data)
    noteContent.value = ''
    ElMessage.success('笔记已保存')
  } finally {
    noteSubmitting.value = false
  }
}

function startEditNote(note: NoteItem) {
  editingNoteId.value = note.id
  editingContent.value = note.content
}

async function saveNote(note: NoteItem) {
  if (!editingContent.value.trim()) {
    ElMessage.warning('笔记内容不能为空')
    return
  }
  const response = await learnApi.updateNote(note.id, editingContent.value.trim())
  Object.assign(note, response.data.data, { lessonTitle: note.lessonTitle })
  editingNoteId.value = null
  ElMessage.success('笔记已更新')
}

async function removeNote(note: NoteItem) {
  await ElMessageBox.confirm('确定删除这条笔记吗？', '删除笔记', { type: 'warning' })
  await learnApi.deleteNote(note.id)
  notes.value = notes.value.filter((item) => item.id !== note.id)
  ElMessage.success('笔记已删除')
}

async function toggleFavorite() {
  if (!course.value) return
  favoriteLoading.value = true
  try {
    if (course.value.favorited) {
      await learnApi.removeFavorite(courseId.value)
      course.value.favorited = false
      ElMessage.success('已取消收藏')
    } else {
      await learnApi.addFavorite(courseId.value)
      course.value.favorited = true
      ElMessage.success('已收藏课程')
    }
  } finally {
    favoriteLoading.value = false
  }
}

async function submitReview() {
  if (!reviewContent.value.trim()) {
    ElMessage.warning('请填写评价内容')
    return
  }
  reviewSubmitting.value = true
  try {
    const response = await learnApi.submitReview(courseId.value, {
      rating: reviewRating.value,
      content: reviewContent.value.trim(),
    })
    if (course.value) course.value.myReview = response.data.data
    ElMessage.success('评价已提交，等待管理员审核')
  } finally {
    reviewSubmitting.value = false
  }
}

watch(
  () => route.params.lessonId,
  async () => {
    if (!course.value) return
    lastReportedSec.value = 0
    await loadNotes()
    await nextTick()
    if (video.value) video.value.load()
  },
)

onMounted(async () => {
  await loadCourse()
  await loadNotes()
})

onBeforeUnmount(() => {
  if (video.value) {
    void report(false)
  }
})
</script>

<template>
  <div v-loading="loading" class="learn-page">
    <template v-if="course && currentLesson">
      <div class="learn-top">
        <div class="learn-title">
          <el-button link @click="router.push(`/courses/${courseId}`)">← 返回课程</el-button>
          <h1>{{ course.title }}</h1>
          <div class="top-meta">
            <el-tag size="small" effect="plain">{{ course.subject }}</el-tag>
            <el-tag size="small" effect="plain" type="info">{{ course.stage }}</el-tag>
            <span>第 {{ lessonIndex.current }} / {{ lessonIndex.total }} 节</span>
          </div>
        </div>
        <div class="top-actions">
          <div class="progress-box">
            <span>课程进度 {{ Number(course.progressPercent || 0).toFixed(0) }}%</span>
            <el-progress
              :percentage="Number(course.progressPercent || 0)"
              :stroke-width="6"
              :show-text="false"
            />
          </div>
          <el-button :loading="favoriteLoading" @click="toggleFavorite">
            {{ course.favorited ? '★ 已收藏' : '☆ 收藏课程' }}
          </el-button>
        </div>
      </div>

      <div class="learn-body">
        <div class="main">
          <div class="video-shell">
            <video
              ref="video"
              :key="currentLesson.id"
              class="video"
              controls
              preload="metadata"
              :src="currentLesson.videoUrl || undefined"
              @timeupdate="onTimeUpdate"
              @pause="report(false)"
              @ended="report(true)"
              @loadedmetadata="onLoadedMetadata"
            ></video>
            <div v-if="!currentLesson.videoUrl" class="video-placeholder">该课时暂无视频</div>
          </div>

          <div class="lesson-head">
            <div>
              <h2>{{ currentLesson.title }}</h2>
              <p class="muted">
                时长 {{ formatDuration(currentLesson.durationSec) }}
                <template v-if="currentLesson.watchedSec">
                  · 已学 {{ formatDuration(currentLesson.watchedSec) }}
                </template>
                <template v-if="currentLesson.completed"> · 已完成</template>
              </p>
            </div>
            <div class="lesson-actions">
              <el-button
                v-if="!currentLesson.completed"
                type="primary"
                plain
                :loading="reporting"
                @click="report(true)"
              >标记完成</el-button>
              <el-tag v-else type="success" effect="dark">本节已完成</el-tag>
            </div>
          </div>

          <el-tabs v-model="activePanel" class="panel">
            <el-tab-pane name="notes" label="学习笔记">
              <div class="note-editor">
                <el-input
                  v-model="noteContent"
                  type="textarea"
                  :rows="3"
                  maxlength="2000"
                  show-word-limit
                  placeholder="记录本节的重点、疑问或解题思路…"
                />
                <div class="note-editor-footer">
                  <el-button type="primary" :loading="noteSubmitting" @click="addNote">保存笔记</el-button>
                </div>
              </div>
              <div v-loading="notesLoading" class="note-list">
                <div v-for="note in notes" :key="note.id" class="note-item">
                  <div v-if="editingNoteId === note.id" class="note-edit">
                    <el-input
                      v-model="editingContent"
                      type="textarea"
                      :rows="3"
                      maxlength="2000"
                      show-word-limit
                    />
                    <div class="note-actions">
                      <el-button size="small" @click="editingNoteId = null">取消</el-button>
                      <el-button size="small" type="primary" @click="saveNote(note)">保存</el-button>
                    </div>
                  </div>
                  <template v-else>
                    <p>{{ note.content }}</p>
                    <div class="note-meta">
                      <span>{{ note.lessonTitle }}</span>
                      <span>
                        <el-button link type="primary" :icon="Edit" @click="startEditNote(note)">编辑</el-button>
                        <el-button link type="danger" :icon="Delete" @click="removeNote(note)">删除</el-button>
                      </span>
                    </div>
                  </template>
                </div>
                <el-empty v-if="!notes.length && !notesLoading" description="本节还没有笔记" :image-size="70" />
              </div>
            </el-tab-pane>

            <el-tab-pane name="review" label="课程评价">
              <div class="review-box">
                <div v-if="course.myReview" class="my-review">
                  <div class="my-review-head">
                    <span>我的评价</span>
                    <el-tag :type="course.myReview.status === 'APPROVED' ? 'success' : course.myReview.status === 'REJECTED' ? 'danger' : 'warning'">
                      {{ course.myReview.status === 'APPROVED' ? '已通过' : course.myReview.status === 'REJECTED' ? '未通过' : '待审核' }}
                    </el-tag>
                  </div>
                  <el-rate :model-value="course.myReview.rating" disabled />
                  <p>{{ course.myReview.content }}</p>
                  <p v-if="course.myReview.adminNote" class="muted">管理员备注：{{ course.myReview.adminNote }}</p>
                </div>
                <el-form label-width="70px">
                  <el-form-item label="评分">
                    <el-rate v-model="reviewRating" />
                  </el-form-item>
                  <el-form-item label="评价">
                    <el-input
                      v-model="reviewContent"
                      type="textarea"
                      :rows="4"
                      maxlength="1000"
                      show-word-limit
                      placeholder="说说这门课对你的帮助…"
                    />
                  </el-form-item>
                </el-form>
                <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">
                  {{ course.myReview ? '更新评价' : '提交评价' }}
                </el-button>
                <p class="muted review-tip">评价提交后需管理员审核通过，才会展示在课程详情页。</p>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>

        <aside class="sidebar">
          <div class="sidebar-head">
            <strong>课程目录</strong>
            <span>{{ course.chapters.length }} 章</span>
          </div>
          <div v-for="chapter in course.chapters" :key="chapter.id" class="chapter">
            <div class="chapter-title">{{ chapter.title }}</div>
            <div
              v-for="lesson in chapter.lessons"
              :key="lesson.id"
              class="lesson"
              :class="{ active: lesson.id === currentLesson.id, done: lesson.completed }"
              @click="switchLesson(lesson)"
            >
              <el-icon><component :is="lessonIcon(lesson)" /></el-icon>
              <span class="lesson-title">{{ lesson.title }}</span>
              <span class="lesson-time">{{ formatDuration(lesson.durationSec) }}</span>
            </div>
          </div>
        </aside>
      </div>
    </template>
    <el-empty v-else-if="!loading" description="未找到课程或尚未购买">
      <el-button type="primary" @click="router.push('/courses')">去课程中心</el-button>
    </el-empty>
  </div>
</template>

<style scoped>
.learn-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px;
  min-height: 70vh;
}

.learn-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  margin-bottom: 20px;
}

.learn-title h1 {
  margin: 8px 0 8px;
  font-size: 24px;
  color: #111827;
}

.learn-title :deep(.el-button) {
  padding-left: 0;
}

.top-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #6b7280;
  font-size: 13px;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.progress-box {
  width: 180px;
  font-size: 12px;
  color: #6b7280;
}

.progress-box span {
  display: block;
  margin-bottom: 6px;
}

.learn-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: start;
}

.main {
  min-width: 0;
}

.video-shell {
  position: relative;
  background: #000;
  border-radius: 12px;
  overflow: hidden;
  aspect-ratio: 16 / 9;
}

.video {
  width: 100%;
  height: 100%;
  display: block;
  background: #000;
}

.video-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
}

.lesson-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 16px 4px;
  border-bottom: 1px solid #ebeef5;
}

.lesson-head h2 {
  margin: 0 0 6px;
  font-size: 18px;
}

.lesson-head p {
  margin: 0;
  font-size: 13px;
}

.lesson-actions {
  flex-shrink: 0;
}

.panel {
  margin-top: 16px;
  background: #fff;
  border-radius: 12px;
  padding: 4px 18px 18px;
}

.note-editor {
  margin-bottom: 16px;
}

.note-editor-footer {
  margin-top: 10px;
  text-align: right;
}

.note-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.note-item {
  border: 1px solid #f1f3f5;
  border-radius: 10px;
  padding: 14px;
  background: #fafafa;
}

.note-item p {
  margin: 0 0 10px;
  white-space: pre-wrap;
  line-height: 1.7;
  color: #374151;
}

.note-meta {
  display: flex;
  justify-content: space-between;
  color: #9ca3af;
  font-size: 12px;
}

.note-actions {
  margin-top: 10px;
  text-align: right;
}

.review-box {
  max-width: 620px;
}

.my-review {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 16px;
  background: #fff7ed;
}

.my-review-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.my-review p {
  margin: 8px 0 0;
}

.review-tip {
  font-size: 12px;
  margin-top: 8px;
}

.sidebar {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  max-height: calc(100vh - 160px);
  overflow: auto;
  position: sticky;
  top: 88px;
}

.sidebar-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  color: #6b7280;
  font-size: 13px;
}

.sidebar-head strong {
  color: #111827;
  font-size: 15px;
}

.chapter {
  margin-bottom: 14px;
}

.chapter-title {
  font-weight: 600;
  font-size: 14px;
  color: #374151;
  margin-bottom: 8px;
}

.lesson {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border-radius: 8px;
  cursor: pointer;
  color: #4b5563;
  font-size: 13px;
}

.lesson:hover {
  background: #f9fafb;
}

.lesson.active {
  background: #fef2f2;
  color: #e60012;
}

.lesson.done {
  color: #16a34a;
}

.lesson.done.active {
  color: #e60012;
}

.lesson-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lesson-time {
  color: #9ca3af;
  font-size: 12px;
}

@media (max-width: 980px) {
  .learn-body {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: static;
    max-height: none;
  }

  .learn-top {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
