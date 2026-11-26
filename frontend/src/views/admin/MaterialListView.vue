<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { contentAdminApi, type MaterialPayload } from '@/api/content'
import type { Material } from '@/types'
import FileUploader from '@/components/FileUploader.vue'

const materials = ref<Material[]>([])
const total = ref(0)
const loading = ref(false)
const filters = reactive({ keyword: '', subject: '', status: '', page: 1, size: 10 })

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<MaterialPayload>({
  title: '',
  subject: '',
  fileUrl: '',
  fileSize: 0,
  status: 'ACTIVE',
})

const subjects = ['政治', '英语', '数学', '专业课']

async function load() {
  loading.value = true
  try {
    const response = await contentAdminApi.materials({ ...filters })
    materials.value = response.data.data.items
    total.value = response.data.data.total
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 1
  load()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { title: '', subject: '', fileUrl: '', fileSize: 0, status: 'ACTIVE' })
  dialogVisible.value = true
}

function openEdit(material: Material) {
  editingId.value = material.id
  Object.assign(form, {
    title: material.title,
    subject: material.subject || '',
    fileUrl: material.fileUrl,
    fileSize: material.fileSize,
    status: material.status,
  })
  dialogVisible.value = true
}

async function save() {
  if (!form.title.trim() || !form.fileUrl.trim()) {
    ElMessage.warning('请填写资料标题并上传文件')
    return
  }
  if (editingId.value) {
    await contentAdminApi.updateMaterial(editingId.value, { ...form })
    ElMessage.success('资料已更新')
  } else {
    await contentAdminApi.createMaterial({ ...form })
    ElMessage.success('资料已创建')
  }
  dialogVisible.value = false
  await load()
}

async function disable(material: Material) {
  await ElMessageBox.confirm(`确定停用资料「${material.title}」吗？`, '提示', { type: 'warning' })
  await contentAdminApi.disableMaterial(material.id)
  ElMessage.success('资料已停用')
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">资料管理</div>
        <div class="muted">PDF、ZIP、文本等备考资料，上传后前台可下载</div>
      </div>
      <el-button v-priv="{ module: 'material', method: 'create' }" type="primary" @click="openCreate">
        新增资料
      </el-button>
    </div>

    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="filters.keyword" placeholder="资料标题" clearable style="width: 220px" @keyup.enter="search" />
        <el-select v-model="filters.subject" placeholder="科目" clearable style="width: 140px" @change="search">
          <el-option v-for="subject in subjects" :key="subject" :label="subject" :value="subject" />
        </el-select>
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button @click="filters.keyword = ''; filters.subject = ''; search()">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="materials" stripe>
        <el-table-column prop="title" label="资料标题" min-width="240" />
        <el-table-column prop="subject" label="科目" width="120" />
        <el-table-column prop="fileUrl" label="文件地址" min-width="260" show-overflow-tooltip />
        <el-table-column prop="downloadCount" label="下载" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-priv="{ module: 'material', method: 'edit' }" link type="primary" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button v-priv="{ module: 'material', method: 'delete' }" link type="danger" @click="disable(row)">
              停用
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑资料' : '新增资料'" width="600px">
      <el-form label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="科目">
          <el-select v-model="form.subject" style="width: 100%">
            <el-option v-for="subject in subjects" :key="subject" :label="subject" :value="subject" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件" required>
          <FileUploader
            v-model="form.fileUrl"
            category="material"
            accept=".pdf,.zip,.txt,.doc,.docx,.ppt,.pptx"
            label="上传资料"
          />
        </el-form-item>
        <el-form-item label="文件大小">
          <el-input-number v-model="form.fileSize" :min="0" />
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
