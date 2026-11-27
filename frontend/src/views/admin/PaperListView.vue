<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { paperAdminApi, questionAdminApi, type PaperPayload } from '@/api/practice'
import { formatDateTime } from '@/utils/format'
import type { Paper, QuestionItem } from '@/types'

interface SelectedQuestion {
  questionId: number
  score: number
  stem: string
  type: string
  subject: string
  chapter: string
}

const list = ref<Paper[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const typeFilter = ref('')
const statusFilter = ref('')
const loading = ref(false)

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  title: '',
  type: 'CHAPTER',
  subject: '政治',
  chapter: '',
  description: '',
  durationMin: 15,
})
const selected = ref<SelectedQuestion[]>([])

const pickerVisible = ref(false)
const pickerList = ref<QuestionItem[]>([])
const pickerTotal = ref(0)
const pickerPage = ref(1)
const pickerSize = ref(50)
const pickerKeyword = ref('')
const pickerSubject = ref('')
const pickerType = ref('')
const pickerLoading = ref(false)
const pickerSelection = ref<QuestionItem[]>([])

const typeMeta: Record<string, { text: string; type: 'success' | 'warning' | 'danger' | 'info' }> = {
  CHAPTER: { text: '章节练习', type: 'success' },
  PAST: { text: '历年真题', type: 'warning' },
  MOCK: { text: '模拟考试', type: 'danger' },
}

const statusMeta: Record<string, { text: string; type: 'success' | 'warning' | 'info' }> = {
  DRAFT: { text: '草稿', type: 'info' },
  PUBLISHED: { text: '已发布', type: 'success' },
  OFF: { text: '已下架', type: 'warning' },
}

const totalScore = computed(() => selected.value.reduce((sum, item) => sum + Number(item.score || 0), 0))

async function load() {
  loading.value = true
  try {
    const response = await paperAdminApi.page({
      keyword: keyword.value || undefined,
      type: typeFilter.value || undefined,
      status: statusFilter.value || undefined,
      page: page.value,
      size: size.value,
    })
    list.value = response.data.data.items
    total.value = response.data.data.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  void load()
}

function resetForm() {
  editingId.value = null
  form.title = ''
  form.type = 'CHAPTER'
  form.subject = '政治'
  form.chapter = ''
  form.description = ''
  form.durationMin = 15
  selected.value = []
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: Paper) {
  const response = await paperAdminApi.detail(row.id)
  const detail = response.data.data
  editingId.value = detail.paper.id
  form.title = detail.paper.title
  form.type = detail.paper.type
  form.subject = detail.paper.subject
  form.chapter = detail.paper.chapter || ''
  form.description = detail.paper.description || ''
  form.durationMin = detail.paper.durationMin
  selected.value = detail.questions.map((item) => ({
    questionId: item.questionId,
    score: Number(item.score),
    stem: item.stem,
    type: item.type,
    subject: item.subject,
    chapter: item.chapter,
  }))
  dialogVisible.value = true
}

async function loadPicker() {
  pickerLoading.value = true
  try {
    const response = await questionAdminApi.page({
      keyword: pickerKeyword.value || undefined,
      subject: pickerSubject.value || undefined,
      type: pickerType.value || undefined,
      status: 'ACTIVE',
      page: pickerPage.value,
      size: pickerSize.value,
    })
    pickerList.value = response.data.data.items
    pickerTotal.value = response.data.data.total
  } finally {
    pickerLoading.value = false
  }
}

function openPicker() {
  pickerPage.value = 1
  pickerKeyword.value = ''
  pickerSubject.value = ''
  pickerType.value = ''
  pickerSelection.value = []
  pickerVisible.value = true
  void loadPicker()
}

function pickerSearch() {
  pickerPage.value = 1
  void loadPicker()
}

function isPicked(row: QuestionItem) {
  return selected.value.some((item) => item.questionId === row.id)
}

function confirmPicker() {
  const added = pickerSelection.value.filter((row) => !isPicked(row))
  for (const row of added) {
    selected.value.push({
      questionId: row.id,
      score: 5,
      stem: row.stem,
      type: row.type,
      subject: row.subject,
      chapter: row.chapter,
    })
  }
  if (added.length) {
    ElMessage.success(`已添加 ${added.length} 道题目`)
  }
  pickerVisible.value = false
}

function removeQuestion(index: number) {
  selected.value.splice(index, 1)
}

function validate() {
  if (!form.title.trim()) return '请填写试卷标题'
  if (!form.subject.trim()) return '请填写科目'
  if (!form.durationMin || form.durationMin < 1) return '请填写考试时长'
  if (!selected.value.length) return '请至少添加一道题目'
  if (selected.value.some((item) => !item.score || Number(item.score) <= 0)) return '每题分值必须大于 0'
  return ''
}

async function save() {
  const error = validate()
  if (error) {
    ElMessage.warning(error)
    return
  }
  const payload: PaperPayload = {
    title: form.title.trim(),
    type: form.type,
    subject: form.subject.trim(),
    chapter: form.chapter.trim() || null,
    description: form.description.trim() || null,
    durationMin: form.durationMin,
    questions: selected.value.map((item) => ({
      questionId: item.questionId,
      score: Number(item.score),
    })),
  }
  saving.value = true
  try {
    if (editingId.value) {
      await paperAdminApi.update(editingId.value, payload)
      ElMessage.success('试卷已更新')
    } else {
      await paperAdminApi.create(payload)
      ElMessage.success('试卷已创建，发布后学员即可作答')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function togglePublish(row: Paper) {
  const publishing = row.status !== 'PUBLISHED'
  const nextStatus = publishing ? 'PUBLISHED' : 'OFF'
  await ElMessageBox.confirm(
    publishing
      ? `发布试卷「${row.title}」后学员可在题库首页看到并开始考试。`
      : `下架试卷「${row.title}」后学员将无法开始新的考试。`,
    publishing ? '发布试卷' : '下架试卷',
    { type: 'warning' },
  )
  await paperAdminApi.setStatus(row.id, nextStatus)
  ElMessage.success(publishing ? '试卷已发布' : '试卷已下架')
  await load()
}

async function disable(row: Paper) {
  await ElMessageBox.confirm(`确定停用试卷「${row.title}」吗？`, '停用试卷', { type: 'warning' })
  await paperAdminApi.remove(row.id)
  ElMessage.success('试卷已停用')
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">试卷管理</div>
        <div class="muted">从题库组卷，设置分值与时长；发布后学员可进行限时考试并由服务端统一判分</div>
      </div>
      <el-button v-priv="{ module: 'paper', method: 'create' }" type="primary" @click="openCreate">
        新增试卷
      </el-button>
    </div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索试卷标题" style="width: 240px" clearable @keyup.enter="search" />
        <el-select v-model="typeFilter" placeholder="全部类型" clearable style="width: 140px" @change="search">
          <el-option label="章节练习" value="CHAPTER" />
          <el-option label="历年真题" value="PAST" />
          <el-option label="模拟考试" value="MOCK" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 130px" @change="search">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="已下架" value="OFF" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="66" />
        <el-table-column label="试卷" min-width="240">
          <template #default="{ row }">
            <div class="cell-strong">{{ row.title }}</div>
            <div class="muted">{{ row.subject }}<span v-if="row.chapter"> · {{ row.chapter }}</span></div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="108">
          <template #default="{ row }">
            <el-tag :type="typeMeta[row.type]?.type || 'info'" size="small">
              {{ typeMeta[row.type]?.text || row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="questionCount" label="题数" width="80" />
        <el-table-column label="总分" width="90">
          <template #default="{ row }">{{ Number(row.totalScore).toFixed(0) }}</template>
        </el-table-column>
        <el-table-column label="时长" width="90">
          <template #default="{ row }">{{ row.durationMin }} 分钟</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMeta[row.status]?.type || 'info'" size="small">
              {{ statusMeta[row.status]?.text || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button v-priv="{ module: 'paper', method: 'edit' }" link type="primary" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button
              v-priv="{ module: 'paper', method: 'publish' }"
              link
              :type="row.status === 'PUBLISHED' ? 'warning' : 'success'"
              @click="togglePublish(row)"
            >
              {{ row.status === 'PUBLISHED' ? '下架' : '发布' }}
            </el-button>
            <el-button v-priv="{ module: 'paper', method: 'delete' }" link type="danger" @click="disable(row)">
              停用
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无试卷" />
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        class="pager"
        layout="total, prev, pager, next"
        :total="total"
        @current-change="load"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑试卷' : '新增试卷'"
      width="860px"
      destroy-on-close
    >
      <el-form label-width="82px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="如：考研政治基础阶段测试" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="类型" required>
            <el-select v-model="form.type" style="width: 100%">
              <el-option label="章节练习" value="CHAPTER" />
              <el-option label="历年真题" value="PAST" />
              <el-option label="模拟考试" value="MOCK" />
            </el-select>
          </el-form-item>
          <el-form-item label="科目" required>
            <el-input v-model="form.subject" placeholder="如：政治" />
          </el-form-item>
          <el-form-item label="章节">
            <el-input v-model="form.chapter" placeholder="章节练习可填写章节" />
          </el-form-item>
          <el-form-item label="时长" required>
            <el-input-number v-model="form.durationMin" :min="1" :max="600" />
            <span class="unit">分钟</span>
          </el-form-item>
        </div>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="试卷说明（选填）" />
        </el-form-item>
        <el-form-item label="题目" required>
          <div class="question-editor">
            <div class="question-toolbar">
              <span>共 {{ selected.length }} 题，总分 {{ totalScore }} 分</span>
              <el-button v-priv="{ module: 'question', method: 'browse' }" size="small" @click="openPicker">
                添加题目
              </el-button>
            </div>
            <el-table :data="selected" size="small" max-height="300">
              <el-table-column type="index" label="#" width="48" />
              <el-table-column label="题干" min-width="280" show-overflow-tooltip>
                <template #default="{ row }">
                  <div>{{ row.stem }}</div>
                  <div class="muted">{{ row.subject }} · {{ row.chapter }}</div>
                </template>
              </el-table-column>
              <el-table-column label="题型" width="86">
                <template #default="{ row }">
                  {{ row.type === 'SINGLE' ? '单选' : row.type === 'MULTIPLE' ? '多选' : '判断' }}
                </template>
              </el-table-column>
              <el-table-column label="分值" width="130">
                <template #default="{ row }">
                  <el-input-number v-model="row.score" :min="0.5" :max="100" :step="0.5" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ $index }">
                  <el-button link type="danger" @click="removeQuestion($index)">移除</el-button>
                </template>
              </el-table-column>
              <template #empty>
                <el-empty description="请添加题目" :image-size="60" />
              </template>
            </el-table>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pickerVisible" title="从题库添加题目" width="820px" destroy-on-close>
      <div class="toolbar">
        <el-input v-model="pickerKeyword" placeholder="搜索题干" style="width: 220px" clearable @keyup.enter="pickerSearch" />
        <el-input v-model="pickerSubject" placeholder="科目" style="width: 120px" clearable @keyup.enter="pickerSearch" />
        <el-select v-model="pickerType" placeholder="全部题型" clearable style="width: 130px" @change="pickerSearch">
          <el-option label="单选题" value="SINGLE" />
          <el-option label="多选题" value="MULTIPLE" />
          <el-option label="判断题" value="JUDGE" />
        </el-select>
        <el-button type="primary" @click="pickerSearch">查询</el-button>
      </div>
      <el-table
        v-loading="pickerLoading"
        :data="pickerList"
        size="small"
        max-height="380"
        @selection-change="(rows: QuestionItem[]) => (pickerSelection = rows)"
      >
        <el-table-column type="selection" width="48" :selectable="(row: QuestionItem) => !isPicked(row)" />
        <el-table-column label="题干" min-width="320" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.stem }}</span>
            <span v-if="isPicked(row)" class="picked-tag">已添加</span>
          </template>
        </el-table-column>
        <el-table-column prop="subject" label="科目" width="110" />
        <el-table-column label="题型" width="86">
          <template #default="{ row }">
            {{ row.type === 'SINGLE' ? '单选' : row.type === 'MULTIPLE' ? '多选' : '判断' }}
          </template>
        </el-table-column>
        <el-table-column prop="correctAnswer" label="答案" width="70" />
        <template #empty>
          <el-empty description="没有可添加的题目" />
        </template>
      </el-table>
      <el-pagination
        v-model:current-page="pickerPage"
        class="pager"
        layout="total, prev, pager, next"
        :total="pickerTotal"
        :page-size="pickerSize"
        @current-change="loadPicker"
      />
      <template #footer>
        <el-button @click="pickerVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPicker">
          添加选中题目（{{ pickerSelection.length }}）
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #111827;
}

.muted {
  color: #9ca3af;
  font-size: 12px;
  margin-top: 4px;
}

.toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.cell-strong {
  font-weight: 600;
  color: #111827;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 18px;
}

.unit {
  margin-left: 8px;
  color: #9ca3af;
  font-size: 13px;
}

.question-editor {
  width: 100%;
}

.question-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 13px;
  color: #6b7280;
}

.picked-tag {
  margin-left: 8px;
  font-size: 11px;
  color: #9ca3af;
}
</style>
