<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { contentAdminApi, type BannerPayload } from '@/api/content'
import type { Banner } from '@/types'
import FileUploader from '@/components/FileUploader.vue'

const banners = ref<Banner[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<BannerPayload>({
  title: '',
  imageUrl: '',
  linkUrl: '',
  sort: 0,
  status: 'ACTIVE',
})

async function load() {
  loading.value = true
  try {
    const response = await contentAdminApi.banners()
    banners.value = response.data.data
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { title: '', imageUrl: '', linkUrl: '', sort: 0, status: 'ACTIVE' })
  dialogVisible.value = true
}

function openEdit(banner: Banner) {
  editingId.value = banner.id
  Object.assign(form, {
    title: banner.title,
    imageUrl: banner.imageUrl,
    linkUrl: banner.linkUrl || '',
    sort: banner.sort,
    status: banner.status,
  })
  dialogVisible.value = true
}

async function save() {
  if (!form.title.trim() || !form.imageUrl.trim()) {
    ElMessage.warning('请填写标题并上传图片')
    return
  }
  if (editingId.value) {
    await contentAdminApi.updateBanner(editingId.value, { ...form })
    ElMessage.success('Banner 已更新')
  } else {
    await contentAdminApi.createBanner({ ...form })
    ElMessage.success('Banner 已创建')
  }
  dialogVisible.value = false
  await load()
}

async function disable(banner: Banner) {
  await ElMessageBox.confirm(`确定停用 Banner「${banner.title}」吗？`, '提示', { type: 'warning' })
  await contentAdminApi.disableBanner(banner.id)
  ElMessage.success('Banner 已停用')
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">Banner 管理</div>
        <div class="muted">首页轮播图，按排序值从小到大展示</div>
      </div>
      <el-button v-priv="{ module: 'banner', method: 'create' }" type="primary" @click="openCreate">
        新增 Banner
      </el-button>
    </div>

    <div class="page-card">
      <el-table v-loading="loading" :data="banners" stripe>
        <el-table-column label="图片" width="200">
          <template #default="{ row }">
            <div class="banner-thumb" :style="{ backgroundImage: `url(${row.imageUrl})` }"></div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="linkUrl" label="跳转链接" min-width="200" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-priv="{ module: 'banner', method: 'edit' }" link type="primary" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button v-priv="{ module: 'banner', method: 'delete' }" link type="danger" @click="disable(row)">
              停用
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑 Banner' : '新增 Banner'" width="600px">
      <el-form label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="图片" required>
          <FileUploader v-model="form.imageUrl" category="banner" accept="image/*" label="上传 Banner" />
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="form.linkUrl" placeholder="/courses" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
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

<style scoped>
.banner-thumb {
  width: 180px;
  height: 54px;
  border-radius: 6px;
  background: linear-gradient(135deg, #e60012, #ff7a00);
  background-size: cover;
  background-position: center;
}
</style>
