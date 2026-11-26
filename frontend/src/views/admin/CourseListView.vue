<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  courseAdminApi,
  teacherAdminApi,
  type ChapterPayload,
  type CoursePayload,
  type LessonPayload,
} from '@/api/course'
import type { Chapter, Course, CourseDetail, Lesson, Teacher } from '@/types'
import FileUploader from '@/components/FileUploader.vue'

const courses = ref<Course[]>([])
const teachers = ref<Teacher[]>([])
const total = ref(0)
const loading = ref(false)
const filters = reactive({
  keyword: '',
  status: '',
  subject: '',
  stage: '',
  page: 1,
  size: 10,
})

const courseDialog = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<CoursePayload>({
  title: '',
  subtitle: '',
  subject: '政治',
  stage: '基础',
  coverUrl: '',
  description: '',
  price: 0,
  originalPrice: undefined,
  status: 'DRAFT',
  teacherIds: [],
})

const subjectOptions = ['政治', '英语一', '英语二', '数学一', '数学二', '数学三', '计算机408', '专业课']
const stageOptions = ['基础', '强化', '冲刺', '复试']

const detailDrawer = ref(false)
const detail = ref<CourseDetail | null>(null)
const detailLoading = ref(false)

const chapterDialog = ref(false)
const chapterEditingId = ref<number | null>(null)
const chapterForm = reactive<ChapterPayload>({ title: '', sort: 0 })

const lessonDialog = ref(false)
const lessonEditingId = ref<number | null>(null)
const lessonChapterId = ref<number | null>(null)
const lessonForm = reactive<LessonPayload>({
  title: '',
  videoUrl: '',
  durationSec: 0,
  isTrial: false,
  sort: 0,
  status: 'ACTIVE',
})

async function load() {
  loading.value = true
  try {
    const response = await courseAdminApi.page({ ...filters })
    courses.value = response.data.data.items
    total.value = response.data.data.total
  } finally {
    loading.value = false
  }
}

async function loadTeachers() {
  const response = await teacherAdminApi.list()
  teachers.value = response.data.data.filter((teacher) => teacher.status === 'ACTIVE')
}

function search() {
  filters.page = 1
  load()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    title: '',
    subtitle: '',
    subject: '政治',
    stage: '基础',
    coverUrl: '',
    description: '',
    price: 0,
    originalPrice: undefined,
    status: 'DRAFT',
    teacherIds: [],
  })
  courseDialog.value = true
}

async function openEdit(course: Course) {
  const response = await courseAdminApi.detail(course.id)
  const detailData = response.data.data
  editingId.value = course.id
  Object.assign(form, {
    title: detailData.course.title,
    subtitle: detailData.course.subtitle || '',
    subject: detailData.course.subject,
    stage: detailData.course.stage,
    coverUrl: detailData.course.coverUrl || '',
    description: detailData.course.description || '',
    price: Number(detailData.course.price),
    originalPrice: detailData.course.originalPrice ? Number(detailData.course.originalPrice) : undefined,
    status: detailData.course.status,
    teacherIds: detailData.course.teachers.map((teacher) => teacher.id),
  })
  courseDialog.value = true
}

async function saveCourse() {
  if (!form.title.trim() || !form.subject || !form.stage) {
    ElMessage.warning('请填写课程标题、科目和阶段')
    return
  }
  const payload = { ...form, price: Number(form.price), originalPrice: form.originalPrice ? Number(form.originalPrice) : null }
  if (editingId.value) {
    await courseAdminApi.update(editingId.value, payload)
    ElMessage.success('课程已更新')
  } else {
    await courseAdminApi.create(payload)
    ElMessage.success('课程已创建')
  }
  courseDialog.value = false
  await load()
}

async function toggleStatus(course: Course) {
  const next = course.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED'
  await courseAdminApi.setStatus(course.id, next)
  ElMessage.success(next === 'PUBLISHED' ? '课程已上架' : '课程已下架')
  await load()
}

async function removeCourse(course: Course) {
  await ElMessageBox.confirm(`确定删除课程「${course.title}」吗？章节课时会一并删除。`, '提示', { type: 'warning' })
  await courseAdminApi.remove(course.id)
  ElMessage.success('课程已删除')
  await load()
}

async function openDetail(course: Course) {
  detailDrawer.value = true
  detailLoading.value = true
  try {
    const response = await courseAdminApi.detail(course.id)
    detail.value = response.data.data
  } finally {
    detailLoading.value = false
  }
}

async function refreshDetail() {
  if (!detail.value) return
  const response = await courseAdminApi.detail(detail.value.course.id)
  detail.value = response.data.data
}

function openChapterCreate() {
  chapterEditingId.value = null
  Object.assign(chapterForm, { title: '', sort: (detail.value?.chapters.length || 0) + 1 })
  chapterDialog.value = true
}

function openChapterEdit(chapter: Chapter) {
  chapterEditingId.value = chapter.id
  Object.assign(chapterForm, { title: chapter.title, sort: chapter.sort })
  chapterDialog.value = true
}

async function saveChapter() {
  if (!detail.value) return
  if (!chapterForm.title.trim()) {
    ElMessage.warning('请填写章节标题')
    return
  }
  const courseId = detail.value.course.id
  if (chapterEditingId.value) {
    await courseAdminApi.updateChapter(courseId, chapterEditingId.value, { ...chapterForm })
    ElMessage.success('章节已更新')
  } else {
    await courseAdminApi.addChapter(courseId, { ...chapterForm })
    ElMessage.success('章节已创建')
  }
  chapterDialog.value = false
  await refreshDetail()
}

async function removeChapter(chapter: Chapter) {
  if (!detail.value) return
  await ElMessageBox.confirm(`确定删除章节「${chapter.title}」及全部课时吗？`, '提示', { type: 'warning' })
  await courseAdminApi.removeChapter(detail.value.course.id, chapter.id)
  ElMessage.success('章节已删除')
  await refreshDetail()
}

function openLessonCreate(chapter: Chapter) {
  lessonChapterId.value = chapter.id
  lessonEditingId.value = null
  Object.assign(lessonForm, {
    title: '',
    videoUrl: '',
    durationSec: 0,
    isTrial: false,
    sort: chapter.lessons.length + 1,
    status: 'ACTIVE',
  })
  lessonDialog.value = true
}

function openLessonEdit(chapter: Chapter, lesson: Lesson) {
  lessonChapterId.value = chapter.id
  lessonEditingId.value = lesson.id
  Object.assign(lessonForm, {
    title: lesson.title,
    videoUrl: lesson.videoUrl || '',
    durationSec: lesson.durationSec,
    isTrial: lesson.isTrial,
    sort: lesson.sort,
    status: lesson.status,
  })
  lessonDialog.value = true
}

async function saveLesson() {
  if (!detail.value || !lessonChapterId.value) return
  if (!lessonForm.title.trim()) {
    ElMessage.warning('请填写课时标题')
    return
  }
  const courseId = detail.value.course.id
  const chapterId = lessonChapterId.value
  if (lessonEditingId.value) {
    await courseAdminApi.updateLesson(courseId, chapterId, lessonEditingId.value, { ...lessonForm })
    ElMessage.success('课时已更新')
  } else {
    await courseAdminApi.addLesson(courseId, chapterId, { ...lessonForm })
    ElMessage.success('课时已创建')
  }
  lessonDialog.value = false
  await refreshDetail()
}

async function removeLesson(chapter: Chapter, lesson: Lesson) {
  if (!detail.value) return
  await ElMessageBox.confirm(`确定删除课时「${lesson.title}」吗？`, '提示', { type: 'warning' })
  await courseAdminApi.removeLesson(detail.value.course.id, chapter.id, lesson.id)
  ElMessage.success('课时已删除')
  await refreshDetail()
}

onMounted(async () => {
  await Promise.all([load(), loadTeachers()])
})
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">课程管理</div>
        <div class="muted">维护课程、章节课时与上架状态；讲师只能管理自己的课程</div>
      </div>
      <el-button v-priv="{ module: 'course', method: 'create' }" type="primary" @click="openCreate">
        新增课程
      </el-button>
    </div>

    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="filters.keyword" placeholder="课程标题" clearable style="width: 220px" @keyup.enter="search" />
        <el-select v-model="filters.status" placeholder="状态" clearable style="width: 140px" @change="search">
          <el-option label="已上架" value="PUBLISHED" />
          <el-option label="草稿" value="DRAFT" />
        </el-select>
        <el-select v-model="filters.subject" placeholder="科目" clearable style="width: 150px" @change="search">
          <el-option v-for="item in subjectOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="filters.stage" placeholder="阶段" clearable style="width: 130px" @change="search">
          <el-option v-for="item in stageOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button @click="filters.keyword = ''; filters.status = ''; filters.subject = ''; filters.stage = ''; search()">
          重置
        </el-button>
      </div>

      <el-table v-loading="loading" :data="courses" stripe>
        <el-table-column label="封面" width="100">
          <template #default="{ row }">
            <div class="cover-thumb" :style="row.coverUrl ? { backgroundImage: `url(${row.coverUrl})` } : undefined"></div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="课程" min-width="190">
          <template #default="{ row }">
            <div class="course-title">{{ row.title }}</div>
            <div class="muted">{{ row.subtitle }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="subject" label="科目" width="95" />
        <el-table-column prop="stage" label="阶段" width="80" />
        <el-table-column label="价格" width="100">
          <template #default="{ row }">¥{{ Number(row.price).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="讲师" min-width="120">
          <template #default="{ row }">
            {{ row.teachers?.map((teacher: Teacher) => teacher.name).join(' / ') || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">
              {{ row.status === 'PUBLISHED' ? '已上架' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="270" fixed="right">
          <template #default="{ row }">
            <el-button v-priv="{ module: 'course', method: 'edit' }" link type="primary" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button v-priv="{ module: 'course', method: 'edit' }" link type="primary" @click="openDetail(row)">
              章节课时
            </el-button>
            <el-button
              v-priv="{ module: 'course', method: 'publish' }"
              link
              :type="row.status === 'PUBLISHED' ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 'PUBLISHED' ? '下架' : '上架' }}
            </el-button>
            <el-button v-priv="{ module: 'course', method: 'delete' }" link type="danger" @click="removeCourse(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="filters.page"
          v-model:page-size="filters.size"
          :total="total"
          layout="total, prev, pager, next, sizes"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>

    <el-dialog v-model="courseDialog" :title="editingId ? '编辑课程' : '新增课程'" width="720px" top="5vh">
      <el-form label-width="90px">
        <el-form-item label="课程标题" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="科目" required>
              <el-select v-model="form.subject" style="width: 100%">
                <el-option v-for="item in subjectOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="阶段" required>
              <el-select v-model="form.stage" style="width: 100%">
                <el-option v-for="item in stageOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="封面">
          <FileUploader v-model="form.coverUrl" category="course" accept="image/*" label="上传封面" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="价格" required>
              <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原价">
              <el-input-number v-model="form.originalPrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="讲师">
          <el-select v-model="form.teacherIds" multiple style="width: 100%">
            <el-option v-for="teacher in teachers" :key="teacher.id" :label="`${teacher.name}（${teacher.subject || ''}）`" :value="teacher.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已上架" value="PUBLISHED" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程介绍">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="courseDialog = false">取消</el-button>
        <el-button type="primary" @click="saveCourse">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailDrawer" :title="detail?.course.title || '章节课时'" size="620px">
      <div v-loading="detailLoading">
        <div class="toolbar">
          <el-button v-priv="{ module: 'course', method: 'edit' }" type="primary" @click="openChapterCreate">
            新增章节
          </el-button>
        </div>
        <el-empty v-if="detail && detail.chapters.length === 0" description="还没有章节" />
        <div v-for="chapter in detail?.chapters || []" :key="chapter.id" class="chapter-block">
          <div class="chapter-head">
            <strong>{{ chapter.title }}</strong>
            <div>
              <el-button v-priv="{ module: 'course', method: 'edit' }" link type="primary" @click="openChapterEdit(chapter)">
                编辑
              </el-button>
              <el-button v-priv="{ module: 'course', method: 'edit' }" link type="danger" @click="removeChapter(chapter)">
                删除
              </el-button>
              <el-button v-priv="{ module: 'course', method: 'edit' }" link type="primary" @click="openLessonCreate(chapter)">
                加课时
              </el-button>
            </div>
          </div>
          <div v-for="lesson in chapter.lessons" :key="lesson.id" class="lesson-row">
            <span>
              {{ lesson.title }}
              <el-tag v-if="lesson.isTrial" size="small" type="danger" effect="plain">试听</el-tag>
            </span>
            <span class="muted">{{ Math.round(lesson.durationSec / 60) }} 分钟</span>
            <span>
              <el-button v-priv="{ module: 'course', method: 'edit' }" link type="primary" @click="openLessonEdit(chapter, lesson)">
                编辑
              </el-button>
              <el-button v-priv="{ module: 'course', method: 'edit' }" link type="danger" @click="removeLesson(chapter, lesson)">
                删除
              </el-button>
            </span>
          </div>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="chapterDialog" :title="chapterEditingId ? '编辑章节' : '新增章节'" width="480px">
      <el-form label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="chapterForm.title" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="chapterForm.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="chapterDialog = false">取消</el-button>
        <el-button type="primary" @click="saveChapter">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="lessonDialog" :title="lessonEditingId ? '编辑课时' : '新增课时'" width="580px">
      <el-form label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="lessonForm.title" />
        </el-form-item>
        <el-form-item label="视频地址">
          <el-input v-model="lessonForm.videoUrl" placeholder="可填写视频 URL，后续学习模块接入播放器" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="时长(秒)">
              <el-input-number v-model="lessonForm.durationSec" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="lessonForm.sort" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="试听">
          <el-switch v-model="lessonForm.isTrial" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="lessonForm.status" style="width: 100%">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="lessonDialog = false">取消</el-button>
        <el-button type="primary" @click="saveLesson">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.cover-thumb {
  width: 76px;
  height: 44px;
  border-radius: 6px;
  background: linear-gradient(135deg, #e60012, #ff7a00);
  background-size: cover;
  background-position: center;
}

.course-title {
  font-weight: 600;
  color: #111827;
}

.chapter-block {
  border: 1px solid #eef0f4;
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 12px;
}

.chapter-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.lesson-row {
  display: grid;
  grid-template-columns: 1fr 90px 130px;
  gap: 10px;
  align-items: center;
  padding: 8px 0;
  border-top: 1px dashed #eef0f4;
  font-size: 14px;
}
</style>
