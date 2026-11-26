<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { teacherAdminApi, type TeacherPayload } from '@/api/course'
import type { Teacher } from '@/types'
import FileUploader from '@/components/FileUploader.vue'

const teachers = ref<Teacher[]>([])
const keyword = ref('')
const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<TeacherPayload>({
  name: '',
  title: '',
  avatar: '',
  subject: '',
  introduction: '',
  sort: 0,
  status: 'ACTIVE',
})

async function load() {
  loading.value = true
  try {
    const response = await teacherAdminApi.list(keyword.value)
    teachers.value = response.data.data
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { name: '', title: '', avatar: '', subject: '', introduction: '', sort: 0, status: 'ACTIVE' })
  dialogVisible.value = true
}

function openEdit(teacher: Teacher) {
  editingId.value = teacher.id
  Object.assign(form, {
    name: teacher.name,
    title: teacher.title || '',
    avatar: teacher.avatar || '',
    subject: teacher.subject || '',
    introduction: teacher.introduction || '',
    sort: teacher.sort,
    status: teacher.status,
  })
  dialogVisible.value = true
}

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写讲师姓名')
    return
  }
  if (editingId.value) {
    await teacherAdminApi.update(editingId.value, { ...form })
    ElMessage.success('讲师已更新')
  } else {
    await teacherAdminApi.create({ ...form })
    ElMessage.success('讲师已创建')
  }
  dialogVisible.value = false
  await load()
}

async function disable(teacher: Teacher) {
  await ElMessageBox.confirm(`确定停用讲师「${teacher.name}」吗？`, '提示', { type: 'warning' })
  await teacherAdminApi.disable(teacher.id)
  ElMessage.success('讲师已停用')
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">讲师管理</div>
        <div class="muted">维护课程讲师资料，讲师可关联到多门课程</div>
      </div>
      <el-button v-priv="{ module: 'teacher', method: 'create' }" type="primary" @click="openCreate">
        新增讲师
      </el-button>
    </div>

    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索姓名 / 头衔" style="width: 280px" clearable @keyup.enter="load" />
        <el-button @click="load">搜索</el-button>
      </div>
      <el-table v-loading="loading" :data="teachers" stripe>
        <el-table-column label="头像" width="90">
          <template #default="{ row }">
            <el-avatar :size="42" :src="row.avatar || undefined">{{ row.name.slice(0, 1) }}</el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" width="130" />
        <el-table-column prop="title" label="头衔" min-width="160" />
        <el-table-column prop="subject" label="科目" width="110" />
        <el-table-column prop="introduction" label="简介" min-width="260" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button v-priv="{ module: 'teacher', method: 'edit' }" link type="primary" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button
              v-priv="{ module: 'teacher', method: 'delete' }"
              link
              type="danger"
              :disabled="row.status !== 'ACTIVE'"
              @click="disable(row)"
            >
              停用
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑讲师' : '新增讲师'" width="600px">
      <el-form label-width="90px">
        <el-form-item label="姓名" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="头衔">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="科目">
          <el-input v-model="form.subject" />
        </el-form-item>
        <el-form-item label="头像">
          <FileUploader v-model="form.avatar" category="avatar" accept="image/*" label="上传头像" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.introduction" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
