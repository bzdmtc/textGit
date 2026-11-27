<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { questionAdminApi, type QuestionPayload } from '@/api/practice'
import { formatDateTime } from '@/utils/format'
import type { QuestionItem, QuestionOption } from '@/types'

const list = ref<QuestionItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const subjectFilter = ref('')
const typeFilter = ref('')
const statusFilter = ref('')
const loading = ref(false)

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  subject: '政治',
  chapter: '',
  type: 'SINGLE',
  difficulty: 3,
  stem: '',
  analysis: '',
})
const options = ref<QuestionOption[]>([])
const singleCorrect = ref('A')
const multiCorrect = ref<string[]>([])

const typeMeta: Record<string, { text: string; type: 'success' | 'warning' | 'info' }> = {
  SINGLE: { text: '单选题', type: 'success' },
  MULTIPLE: { text: '多选题', type: 'warning' },
  JUDGE: { text: '判断题', type: 'info' },
}

const isJudge = computed(() => form.type === 'JUDGE')
const isMultiple = computed(() => form.type === 'MULTIPLE')

function defaultOptions() {
  return [
    { label: 'A', content: '', isCorrect: false },
    { label: 'B', content: '', isCorrect: false },
    { label: 'C', content: '', isCorrect: false },
    { label: 'D', content: '', isCorrect: false },
  ]
}

function nextLabel() {
  for (let code = 65; code <= 90; code++) {
    const label = String.fromCharCode(code)
    if (!options.value.some((item) => item.label === label)) {
      return label
    }
  }
  return ''
}

async function load() {
  loading.value = true
  try {
    const response = await questionAdminApi.page({
      keyword: keyword.value || undefined,
      subject: subjectFilter.value || undefined,
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
  form.subject = '政治'
  form.chapter = ''
  form.type = 'SINGLE'
  form.difficulty = 3
  form.stem = ''
  form.analysis = ''
  options.value = defaultOptions()
  singleCorrect.value = 'A'
  multiCorrect.value = []
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: QuestionItem) {
  const response = await questionAdminApi.detail(row.id)
  const detail = response.data.data
  editingId.value = detail.id
  form.subject = detail.subject
  form.chapter = detail.chapter
  form.type = detail.type
  form.difficulty = detail.difficulty
  form.stem = detail.stem
  form.analysis = detail.analysis || ''
  options.value = detail.options.map((item) => ({
    label: item.label,
    content: item.content,
    isCorrect: Boolean(item.isCorrect),
  }))
  singleCorrect.value = detail.options.find((item) => item.isCorrect)?.label
    || detail.correctAnswer?.charAt(0) || 'A'
  multiCorrect.value = detail.options.filter((item) => item.isCorrect).map((item) => item.label)
  dialogVisible.value = true
}

function onTypeChange() {
  if (form.type === 'JUDGE') {
    options.value = [
      { label: 'A', content: '正确', isCorrect: true },
      { label: 'B', content: '错误', isCorrect: false },
    ]
    singleCorrect.value = 'A'
    multiCorrect.value = []
    return
  }
  if (options.value.length < 2) {
    options.value = defaultOptions()
    singleCorrect.value = 'A'
    multiCorrect.value = []
  }
}

function addOption() {
  const label = nextLabel()
  if (!label) {
    ElMessage.warning('最多支持 26 个选项')
    return
  }
  options.value.push({ label, content: '', isCorrect: false })
}

function removeOption(index: number) {
  const removed = options.value[index]
  options.value.splice(index, 1)
  if (singleCorrect.value === removed.label) {
    singleCorrect.value = options.value[0]?.label || ''
  }
  multiCorrect.value = multiCorrect.value.filter((label) => label !== removed.label)
}

function validate() {
  if (!form.subject.trim()) return '请填写科目'
  if (!form.chapter.trim()) return '请填写章节'
  if (!form.stem.trim()) return '请填写题干'
  if (options.value.length < 2) return '至少需要两个选项'
  if (options.value.some((item) => !item.content.trim())) return '选项内容不能为空'
  if (isMultiple.value) {
    if (multiCorrect.value.length < 2) return '多选题至少选择两个正确选项'
  } else if (!singleCorrect.value) {
    return '请选择正确选项'
  }
  return ''
}

async function save() {
  const error = validate()
  if (error) {
    ElMessage.warning(error)
    return
  }
  const correctSet = isMultiple.value ? new Set(multiCorrect.value) : new Set([singleCorrect.value])
  const payload: QuestionPayload = {
    subject: form.subject.trim(),
    chapter: form.chapter.trim(),
    type: form.type,
    stem: form.stem.trim(),
    analysis: form.analysis.trim() || null,
    difficulty: form.difficulty,
    options: options.value.map((item, index) => ({
      label: item.label,
      content: item.content.trim(),
      isCorrect: correctSet.has(item.label),
      sort: index + 1,
    })),
  }
  saving.value = true
  try {
    if (editingId.value) {
      await questionAdminApi.update(editingId.value, payload)
      ElMessage.success('题目已更新')
    } else {
      await questionAdminApi.create(payload)
      ElMessage.success('题目已创建')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: QuestionItem) {
  const nextStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const action = nextStatus === 'DISABLED' ? '停用' : '启用'
  await ElMessageBox.confirm(`确定${action}题目「${row.stem.slice(0, 24)}…」吗？`, `${action}题目`, {
    type: 'warning',
  })
  await questionAdminApi.setStatus(row.id, nextStatus)
  ElMessage.success(`已${action}`)
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">题库管理</div>
        <div class="muted">维护单选、多选、判断题；题目和选项可组合成章节练习、历年真题与模拟试卷</div>
      </div>
      <el-button v-priv="{ module: 'question', method: 'create' }" type="primary" @click="openCreate">
        新增题目
      </el-button>
    </div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索题干 / 章节"
          style="width: 240px"
          clearable
          @keyup.enter="search"
        />
        <el-input v-model="subjectFilter" placeholder="科目" style="width: 140px" clearable @keyup.enter="search" />
        <el-select v-model="typeFilter" placeholder="全部题型" clearable style="width: 130px" @change="search">
          <el-option label="单选题" value="SINGLE" />
          <el-option label="多选题" value="MULTIPLE" />
          <el-option label="判断题" value="JUDGE" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 130px" @change="search">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="已停用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="66" />
        <el-table-column label="科目 / 章节" min-width="180">
          <template #default="{ row }">
            <div class="cell-strong">{{ row.subject }}</div>
            <div class="muted">{{ row.chapter }}</div>
          </template>
        </el-table-column>
        <el-table-column label="题型" width="96">
          <template #default="{ row }">
            <el-tag :type="typeMeta[row.type]?.type || 'info'" size="small">
              {{ typeMeta[row.type]?.text || row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="题干" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">{{ row.stem }}</template>
        </el-table-column>
        <el-table-column label="答案" width="80">
          <template #default="{ row }">
            <span class="answer-badge">{{ row.correctAnswer || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="90">
          <template #default="{ row }">{{ row.difficulty }} / 5</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-priv="{ module: 'question', method: 'edit' }" link type="primary" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button
              v-priv="{ module: 'question', method: 'delete' }"
              link
              :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无题目" />
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
      :title="editingId ? '编辑题目' : '新增题目'"
      width="760px"
      destroy-on-close
    >
      <el-form label-width="82px">
        <div class="form-grid">
          <el-form-item label="科目" required>
            <el-input v-model="form.subject" placeholder="如：政治 / 英语一 / 数学一" />
          </el-form-item>
          <el-form-item label="章节" required>
            <el-input v-model="form.chapter" placeholder="如：马克思主义基本原理" />
          </el-form-item>
          <el-form-item label="题型" required>
            <el-select v-model="form.type" style="width: 100%" @change="onTypeChange">
              <el-option label="单选题" value="SINGLE" />
              <el-option label="多选题" value="MULTIPLE" />
              <el-option label="判断题" value="JUDGE" />
            </el-select>
          </el-form-item>
          <el-form-item label="难度">
            <el-rate v-model="form.difficulty" :max="5" show-score />
          </el-form-item>
        </div>
        <el-form-item label="题干" required>
          <el-input v-model="form.stem" type="textarea" :rows="3" placeholder="请输入题干" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="form.analysis" type="textarea" :rows="2" placeholder="答题解析（选填）" />
        </el-form-item>
        <el-form-item label="选项" required>
          <div class="option-editor">
            <div v-for="(option, index) in options" :key="option.label" class="option-row">
              <span class="option-label">{{ option.label }}</span>
              <el-input v-model="option.content" placeholder="选项内容" :disabled="isJudge" />
              <el-radio
                v-if="!isMultiple"
                v-model="singleCorrect"
                :value="option.label"
                class="correct-choice"
              >正确</el-radio>
              <el-checkbox
                v-else
                v-model="multiCorrect"
                :value="option.label"
                class="correct-choice"
              >正确</el-checkbox>
              <el-button
                v-if="!isJudge && options.length > 2"
                link
                type="danger"
                @click="removeOption(index)"
              >删除</el-button>
            </div>
            <el-button v-if="!isJudge" link type="primary" @click="addOption">+ 添加选项</el-button>
            <div class="option-tip">单选题/判断题选择一个正确选项；多选题至少选择两个。</div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
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

.answer-badge {
  display: inline-block;
  min-width: 32px;
  padding: 2px 8px;
  border-radius: 6px;
  background: #f0fdf4;
  color: #15803d;
  font-weight: 600;
  text-align: center;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 18px;
}

.option-editor {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.option-label {
  width: 26px;
  height: 26px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #f3f4f6;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 13px;
}

.correct-choice {
  margin-right: 0;
  flex-shrink: 0;
}

.option-tip {
  font-size: 12px;
  color: #9ca3af;
}
</style>
