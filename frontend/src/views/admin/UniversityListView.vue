<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { universityApi } from '@/api/university'
import UniversityDetailDrawer from '@/components/UniversityDetailDrawer.vue'
import type { University, UniversityOptions } from '@/types'

const universities = ref<University[]>([])
const options = ref<UniversityOptions>({ provinces: [], levels: [], types: [] })
const total = ref(0)
const loading = ref(false)

const filters = reactive({
  keyword: '',
  province: '',
  level: '',
  type: '',
  status: '',
  page: 1,
  size: 10,
})

interface UniversityForm {
  name: string
  province: string
  city: string
  level: string
  type: string
  logoUrl: string
  website: string
  introduction: string
  featured: boolean
  sort: number
}

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const universityForm = ref<UniversityForm>({
  name: '',
  province: '',
  city: '',
  level: '985',
  type: '综合',
  logoUrl: '',
  website: '',
  introduction: '',
  featured: false,
  sort: 0,
})

const drawerVisible = ref(false)
const drawerUniversityId = ref<number | null>(null)

async function load() {
  loading.value = true
  try {
    const response = await universityApi.page({
      keyword: filters.keyword || undefined,
      province: filters.province || undefined,
      level: filters.level || undefined,
      type: filters.type || undefined,
      status: filters.status || undefined,
      page: filters.page,
      size: filters.size,
    })
    universities.value = response.data.data.items
    total.value = response.data.data.total
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const response = await universityApi.options()
  options.value = response.data.data
}

function search() {
  filters.page = 1
  load()
}

function reset() {
  filters.keyword = ''
  filters.province = ''
  filters.level = ''
  filters.type = ''
  filters.status = ''
  filters.page = 1
  load()
}

function openCreate() {
  editingId.value = null
  Object.assign(universityForm.value, {
    name: '',
    province: '',
    city: '',
    level: '985',
    type: '综合',
    logoUrl: '',
    website: '',
    introduction: '',
    featured: false,
    sort: (universities.value.length + 1) * 10,
  })
  dialogVisible.value = true
}

function openEdit(university: University) {
  editingId.value = university.id
  Object.assign(universityForm.value, {
    name: university.name,
    province: university.province,
    city: university.city || '',
    level: university.level || '',
    type: university.type || '',
    logoUrl: university.logoUrl || '',
    website: university.website || '',
    introduction: university.introduction || '',
    featured: university.featured,
    sort: university.sort,
  })
  dialogVisible.value = true
}

async function save() {
  if (!universityForm.value.name.trim() || !universityForm.value.province.trim()) {
    ElMessage.warning('请填写院校名称与所在省份')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await universityApi.update(editingId.value, { ...universityForm.value })
      ElMessage.success('院校已更新')
    } else {
      await universityApi.create({ ...universityForm.value })
      ElMessage.success('院校已新增')
    }
    dialogVisible.value = false
    await Promise.all([load(), loadOptions()])
  } finally {
    saving.value = false
  }
}

async function toggleStatus(university: University) {
  const next = university.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await ElMessageBox.confirm(
    `确定${next === 'ACTIVE' ? '启用' : '停用'}院校「${university.name}」吗？停用后前台不再展示。`,
    '提示',
    { type: 'warning' },
  )
  await universityApi.setStatus(university.id, next)
  ElMessage.success('状态已更新')
  await load()
}

function openDrawer(university: University) {
  drawerUniversityId.value = university.id
  drawerVisible.value = true
}

onMounted(() => {
  load()
  loadOptions()
})
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">院校库管理</div>
        <div class="muted">维护院校、专业目录、历年分数线与报录比，前台院校库实时展示</div>
      </div>
      <el-button v-priv="{ module: 'university', method: 'create' }" type="primary" @click="openCreate">
        新增院校
      </el-button>
    </div>

    <div class="page-card">
      <div class="toolbar">
        <el-input
          v-model="filters.keyword"
          placeholder="院校名称"
          style="width: 200px"
          clearable
          @keyup.enter="search"
        />
        <el-select v-model="filters.province" placeholder="省份" clearable style="width: 130px" @change="search">
          <el-option v-for="province in options.provinces" :key="province" :label="province" :value="province" />
        </el-select>
        <el-select v-model="filters.level" placeholder="层次" clearable style="width: 120px" @change="search">
          <el-option v-for="level in options.levels" :key="level" :label="level" :value="level" />
        </el-select>
        <el-select v-model="filters.type" placeholder="类型" clearable style="width: 120px" @change="search">
          <el-option v-for="type in options.types" :key="type" :label="type" :value="type" />
        </el-select>
        <el-select v-model="filters.status" placeholder="状态" clearable style="width: 110px" @change="search">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="universities" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="院校" min-width="200">
          <template #default="{ row }">
            <div class="school-cell">
              <div class="school-logo">{{ row.name.slice(0, 1) }}</div>
              <div>
                <div class="school-name">{{ row.name }}</div>
                <div class="muted">{{ row.website || '未配置官网' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="地区" width="150">
          <template #default="{ row }">{{ row.province }} · {{ row.city || '—' }}</template>
        </el-table-column>
        <el-table-column prop="level" label="层次" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.level" size="small">{{ row.level }}</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="90">
          <template #default="{ row }">{{ row.type || '—' }}</template>
        </el-table-column>
        <el-table-column prop="majorCount" label="专业数" width="90" />
        <el-table-column label="精选" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.featured" size="small" type="danger" effect="plain">精选</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDrawer(row)">招生数据</el-button>
            <el-button v-priv="{ module: 'university', method: 'edit' }" link type="primary" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button
              v-priv="{ module: 'university', method: 'delete' }"
              link
              :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑院校' : '新增院校'" width="640px">
      <el-form label-width="100px">
        <el-form-item label="院校名称" required>
          <el-input v-model="universityForm.name" />
        </el-form-item>
        <el-form-item label="省份" required>
          <el-input v-model="universityForm.province" placeholder="如 北京" />
        </el-form-item>
        <el-form-item label="城市">
          <el-input v-model="universityForm.city" placeholder="如 北京" />
        </el-form-item>
        <el-form-item label="层次">
          <el-select v-model="universityForm.level" clearable style="width: 100%">
            <el-option label="985" value="985" />
            <el-option label="211" value="211" />
            <el-option label="双一流" value="双一流" />
            <el-option label="普通" value="普通" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="universityForm.type" clearable style="width: 100%">
            <el-option label="综合" value="综合" />
            <el-option label="理工" value="理工" />
            <el-option label="师范" value="师范" />
            <el-option label="财经" value="财经" />
            <el-option label="政法" value="政法" />
            <el-option label="医药" value="医药" />
          </el-select>
        </el-form-item>
        <el-form-item label="官网">
          <el-input v-model="universityForm.website" placeholder="https://" />
        </el-form-item>
        <el-form-item label="校徽地址">
          <el-input v-model="universityForm.logoUrl" placeholder="可选，留空显示院校首字" />
        </el-form-item>
        <el-form-item label="院校简介">
          <el-input v-model="universityForm.introduction" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="首页推荐">
          <el-switch v-model="universityForm.featured" />
          <span class="muted" style="margin-left: 10px">开启后出现在首页「院校推荐」</span>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="universityForm.sort" :min="0" :step="10" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <UniversityDetailDrawer
      v-model="drawerVisible"
      :university-id="drawerUniversityId"
      @refresh="load"
    />
  </div>
</template>

<style scoped>
.school-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.school-logo {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: linear-gradient(135deg, #e60012, #ff7a00);
  color: #fff;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.school-name {
  font-weight: 600;
  color: #111827;
}
</style>
