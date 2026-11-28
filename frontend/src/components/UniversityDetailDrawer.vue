<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { universityApi, type AdmissionStatPayload, type MajorPayload, type ScoreLinePayload } from '@/api/university'
import { formatMoney } from '@/utils/format'
import type { ScoreLine, UniversityDetail, UniversityMajor, AdmissionStat } from '@/types'

const props = defineProps<{
  modelValue: boolean
  universityId: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  refresh: []
}>()

const detail = ref<UniversityDetail | null>(null)
const loading = ref(false)
const activeTab = ref('majors')
const saving = ref(false)

const majorDialog = ref(false)
const editingMajorId = ref<number | null>(null)
const majorForm = reactive<MajorPayload>({
  name: '',
  code: '',
  degreeType: '学硕',
  duration: '3年',
  tuition: null,
  examSubjects: '',
  introduction: '',
  sort: 0,
})

const scoreDialog = ref(false)
const editingScoreId = ref<number | null>(null)
const scoreForm = reactive<ScoreLinePayload>({
  majorId: 0,
  year: new Date().getFullYear(),
  political: null,
  english: null,
  math: null,
  majorScore: null,
  total: 0,
})

const statDialog = ref(false)
const editingStatId = ref<number | null>(null)
const statForm = reactive<AdmissionStatPayload>({
  majorId: 0,
  year: new Date().getFullYear(),
  applicants: 0,
  admitted: 0,
  minScore: null,
})

const university = computed(() => detail.value?.university)
const majors = computed(() => detail.value?.majors || [])
const activeMajors = computed(() => majors.value.filter((major) => major.status === 'ACTIVE'))
const scoreLines = computed(() => detail.value?.scoreLines || [])
const admissionStats = computed(() => detail.value?.admissionStats || [])

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

async function load() {
  if (!props.universityId) return
  loading.value = true
  try {
    const response = await universityApi.detail(props.universityId)
    detail.value = response.data.data
  } finally {
    loading.value = false
  }
}

function refreshAll() {
  load()
  emit('refresh')
}

function openMajorCreate() {
  editingMajorId.value = null
  Object.assign(majorForm, {
    name: '',
    code: '',
    degreeType: '学硕',
    duration: '3年',
    tuition: null,
    examSubjects: '',
    introduction: '',
    sort: activeMajors.value.length * 10,
  })
  majorDialog.value = true
}

function openMajorEdit(major: UniversityMajor) {
  editingMajorId.value = major.id
  Object.assign(majorForm, {
    name: major.name,
    code: major.code || '',
    degreeType: major.degreeType,
    duration: major.duration || '',
    tuition: major.tuition ?? null,
    examSubjects: major.examSubjects || '',
    introduction: major.introduction || '',
    sort: major.sort,
  })
  majorDialog.value = true
}

async function saveMajor() {
  if (!props.universityId) return
  if (!majorForm.name.trim()) {
    ElMessage.warning('请填写专业名称')
    return
  }
  saving.value = true
  try {
    if (editingMajorId.value) {
      await universityApi.updateMajor(props.universityId, editingMajorId.value, { ...majorForm })
      ElMessage.success('专业已更新')
    } else {
      await universityApi.createMajor(props.universityId, { ...majorForm })
      ElMessage.success('专业已新增')
    }
    majorDialog.value = false
    refreshAll()
  } finally {
    saving.value = false
  }
}

async function disableMajor(major: UniversityMajor) {
  if (!props.universityId) return
  await ElMessageBox.confirm(`确定停用专业「${major.name}」吗？`, '提示', { type: 'warning' })
  await universityApi.removeMajor(props.universityId, major.id)
  ElMessage.success('专业已停用')
  refreshAll()
}

function openScoreCreate() {
  editingScoreId.value = null
  Object.assign(scoreForm, {
    majorId: activeMajors.value[0]?.id || 0,
    year: new Date().getFullYear(),
    political: null,
    english: null,
    math: null,
    majorScore: null,
    total: 0,
  })
  scoreDialog.value = true
}

function openScoreEdit(line: ScoreLine) {
  editingScoreId.value = line.id
  Object.assign(scoreForm, {
    majorId: line.majorId,
    year: line.year,
    political: line.political ?? null,
    english: line.english ?? null,
    math: line.math ?? null,
    majorScore: line.majorScore ?? null,
    total: line.total,
  })
  scoreDialog.value = true
}

async function saveScore() {
  if (!props.universityId) return
  if (!scoreForm.majorId || !scoreForm.total) {
    ElMessage.warning('请选择专业并填写总分')
    return
  }
  saving.value = true
  try {
    if (editingScoreId.value) {
      await universityApi.updateScoreLine(props.universityId, editingScoreId.value, { ...scoreForm })
      ElMessage.success('分数线已更新')
    } else {
      await universityApi.createScoreLine(props.universityId, { ...scoreForm })
      ElMessage.success('分数线已新增')
    }
    scoreDialog.value = false
    refreshAll()
  } finally {
    saving.value = false
  }
}

async function removeScore(line: ScoreLine) {
  if (!props.universityId) return
  await ElMessageBox.confirm(`确定删除 ${line.year} 年「${line.majorName}」分数线吗？`, '提示', {
    type: 'warning',
  })
  await universityApi.removeScoreLine(props.universityId, line.id)
  ElMessage.success('分数线已删除')
  refreshAll()
}

function openStatCreate() {
  editingStatId.value = null
  Object.assign(statForm, {
    majorId: activeMajors.value[0]?.id || 0,
    year: new Date().getFullYear(),
    applicants: 0,
    admitted: 0,
    minScore: null,
  })
  statDialog.value = true
}

function openStatEdit(stat: AdmissionStat) {
  editingStatId.value = stat.id
  Object.assign(statForm, {
    majorId: stat.majorId,
    year: stat.year,
    applicants: stat.applicants,
    admitted: stat.admitted,
    minScore: stat.minScore ?? null,
  })
  statDialog.value = true
}

async function saveStat() {
  if (!props.universityId) return
  if (!statForm.majorId) {
    ElMessage.warning('请选择专业')
    return
  }
  saving.value = true
  try {
    if (editingStatId.value) {
      await universityApi.updateAdmissionStat(props.universityId, editingStatId.value, { ...statForm })
      ElMessage.success('报录比已更新')
    } else {
      await universityApi.createAdmissionStat(props.universityId, { ...statForm })
      ElMessage.success('报录比已新增')
    }
    statDialog.value = false
    refreshAll()
  } finally {
    saving.value = false
  }
}

async function removeStat(stat: AdmissionStat) {
  if (!props.universityId) return
  await ElMessageBox.confirm(`确定删除 ${stat.year} 年「${stat.majorName}」报录比吗？`, '提示', {
    type: 'warning',
  })
  await universityApi.removeAdmissionStat(props.universityId, stat.id)
  ElMessage.success('报录比已删除')
  refreshAll()
}

watch(
  () => [props.modelValue, props.universityId],
  ([open]) => {
    if (open) {
      activeTab.value = 'majors'
      load()
    }
  },
)
</script>

<template>
  <el-drawer v-model="visible" size="76%" :title="university ? `${university.name} · 招生数据` : '院校数据'">
    <div v-loading="loading">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="专业目录" name="majors">
          <div class="toolbar">
            <el-button v-priv="{ module: 'university', method: 'create' }" type="primary" @click="openMajorCreate">
              新增专业
            </el-button>
            <span class="muted">共 {{ majors.length }} 个专业，停用后前台不再展示</span>
          </div>
          <el-table :data="majors" stripe>
            <el-table-column prop="name" label="专业名称" min-width="160" />
            <el-table-column prop="code" label="代码" width="100">
              <template #default="{ row }">{{ row.code || '—' }}</template>
            </el-table-column>
            <el-table-column prop="degreeType" label="类型" width="80" />
            <el-table-column prop="duration" label="学制" width="80">
              <template #default="{ row }">{{ row.duration || '—' }}</template>
            </el-table-column>
            <el-table-column label="学费" width="120">
              <template #default="{ row }">
                {{ row.tuition === null || row.tuition === undefined ? '—' : formatMoney(row.tuition) }}
              </template>
            </el-table-column>
            <el-table-column prop="examSubjects" label="初试科目" min-width="220">
              <template #default="{ row }">{{ row.examSubjects || '—' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
                  {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button v-priv="{ module: 'university', method: 'edit' }" link type="primary" @click="openMajorEdit(row)">
                  编辑
                </el-button>
                <el-button
                  v-if="row.status === 'ACTIVE'"
                  v-priv="{ module: 'university', method: 'delete' }"
                  link
                  type="danger"
                  @click="disableMajor(row)"
                >
                  停用
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="历年分数线" name="scores">
          <div class="toolbar">
            <el-button v-priv="{ module: 'university', method: 'create' }" type="primary" @click="openScoreCreate">
              新增分数线
            </el-button>
            <span class="muted">同一专业每年仅一条记录</span>
          </div>
          <el-table :data="scoreLines" stripe>
            <el-table-column prop="majorName" label="专业" min-width="170" />
            <el-table-column prop="year" label="年份" width="90" />
            <el-table-column prop="political" label="政治" width="80">
              <template #default="{ row }">{{ row.political ?? '—' }}</template>
            </el-table-column>
            <el-table-column prop="english" label="英语" width="80">
              <template #default="{ row }">{{ row.english ?? '—' }}</template>
            </el-table-column>
            <el-table-column prop="math" label="数学" width="80">
              <template #default="{ row }">{{ row.math ?? '—' }}</template>
            </el-table-column>
            <el-table-column prop="majorScore" label="专业课" width="90">
              <template #default="{ row }">{{ row.majorScore ?? '—' }}</template>
            </el-table-column>
            <el-table-column prop="total" label="总分" width="90" />
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button v-priv="{ module: 'university', method: 'edit' }" link type="primary" @click="openScoreEdit(row)">
                  编辑
                </el-button>
                <el-button v-priv="{ module: 'university', method: 'delete' }" link type="danger" @click="removeScore(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="报录比" name="admission">
          <div class="toolbar">
            <el-button v-priv="{ module: 'university', method: 'create' }" type="primary" @click="openStatCreate">
              新增报录比
            </el-button>
            <span class="muted">报录比 = 报考人数 / 录取人数，由服务端自动计算</span>
          </div>
          <el-table :data="admissionStats" stripe>
            <el-table-column prop="majorName" label="专业" min-width="170" />
            <el-table-column prop="year" label="年份" width="90" />
            <el-table-column prop="applicants" label="报考人数" width="110" />
            <el-table-column prop="admitted" label="录取人数" width="110" />
            <el-table-column label="报录比" width="120">
              <template #default="{ row }">
                {{ row.ratio === null || row.ratio === undefined ? '—' : `${Number(row.ratio).toFixed(2)} : 1` }}
              </template>
            </el-table-column>
            <el-table-column prop="minScore" label="最低分" width="100">
              <template #default="{ row }">{{ row.minScore ?? '—' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button v-priv="{ module: 'university', method: 'edit' }" link type="primary" @click="openStatEdit(row)">
                  编辑
                </el-button>
                <el-button v-priv="{ module: 'university', method: 'delete' }" link type="danger" @click="removeStat(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="majorDialog" :title="editingMajorId ? '编辑专业' : '新增专业'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="专业名称" required>
          <el-input v-model="majorForm.name" />
        </el-form-item>
        <el-form-item label="专业代码">
          <el-input v-model="majorForm.code" placeholder="如 081200" />
        </el-form-item>
        <el-form-item label="培养类型" required>
          <el-radio-group v-model="majorForm.degreeType">
            <el-radio value="学硕">学硕</el-radio>
            <el-radio value="专硕">专硕</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="学制">
          <el-input v-model="majorForm.duration" placeholder="如 3年" />
        </el-form-item>
        <el-form-item label="学费（元）">
          <el-input-number v-model="majorForm.tuition" :min="0" :step="1000" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="初试科目">
          <el-input v-model="majorForm.examSubjects" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="majorForm.sort" :min="0" :step="10" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="majorDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveMajor">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="scoreDialog" :title="editingScoreId ? '编辑分数线' : '新增分数线'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="专业" required>
          <el-select v-model="scoreForm.majorId" style="width: 100%">
            <el-option v-for="major in activeMajors" :key="major.id" :label="major.name" :value="major.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="年份" required>
          <el-input-number v-model="scoreForm.year" :min="2000" :max="2100" controls-position="right" />
        </el-form-item>
        <el-form-item label="政治">
          <el-input-number v-model="scoreForm.political" :min="0" :max="100" controls-position="right" />
        </el-form-item>
        <el-form-item label="英语">
          <el-input-number v-model="scoreForm.english" :min="0" :max="100" controls-position="right" />
        </el-form-item>
        <el-form-item label="数学">
          <el-input-number v-model="scoreForm.math" :min="0" :max="150" controls-position="right" />
        </el-form-item>
        <el-form-item label="专业课">
          <el-input-number v-model="scoreForm.majorScore" :min="0" :max="300" controls-position="right" />
        </el-form-item>
        <el-form-item label="总分" required>
          <el-input-number v-model="scoreForm.total" :min="0" :max="500" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scoreDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveScore">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statDialog" :title="editingStatId ? '编辑报录比' : '新增报录比'" width="520px">
      <el-form label-width="90px">
        <el-form-item label="专业" required>
          <el-select v-model="statForm.majorId" style="width: 100%">
            <el-option v-for="major in activeMajors" :key="major.id" :label="major.name" :value="major.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="年份" required>
          <el-input-number v-model="statForm.year" :min="2000" :max="2100" controls-position="right" />
        </el-form-item>
        <el-form-item label="报考人数" required>
          <el-input-number v-model="statForm.applicants" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="录取人数" required>
          <el-input-number v-model="statForm.admitted" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="最低分">
          <el-input-number v-model="statForm.minScore" :min="0" :max="500" controls-position="right" />
        </el-form-item>
        <el-form-item label="报录比">
          <span class="muted">
            {{ statForm.admitted > 0 ? `${(statForm.applicants / statForm.admitted).toFixed(2)} : 1` : '录取人数为 0 时不计算' }}
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveStat">保存</el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>
