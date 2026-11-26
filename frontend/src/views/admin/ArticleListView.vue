<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { contentAdminApi, type ArticlePayload } from '@/api/content'
import type { Article, ArticleCategory } from '@/types'
import FileUploader from '@/components/FileUploader.vue'

const articles = ref<Article[]>([])
const categories = ref<ArticleCategory[]>([])
const total = ref(0)
const loading = ref(false)
const filters = reactive({ keyword: '', status: '', categoryId: undefined as number | undefined, page: 1, size: 10 })

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<ArticlePayload>({
  categoryId: undefined,
  title: '',
  summary: '',
  coverUrl: '',
  contentHtml: '',
  author: '',
  status: 'DRAFT',
})

async function load() {
  loading.value = true
  try {
    const response = await contentAdminApi.articles({ ...filters })
    articles.value = response.data.data.items
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
  Object.assign(form, {
    categoryId: categories.value[0]?.id,
    title: '',
    summary: '',
    coverUrl: '',
    contentHtml: '',
    author: '',
    status: 'DRAFT',
  })
  dialogVisible.value = true
}

function openEdit(article: Article) {
  editingId.value = article.id
  Object.assign(form, {
    categoryId: article.categoryId,
    title: article.title,
    summary: article.summary || '',
    coverUrl: article.coverUrl || '',
    contentHtml: article.contentHtml || '',
    author: article.author || '',
    status: article.status,
  })
  dialogVisible.value = true
}

async function save() {
  if (!form.title.trim()) {
    ElMessage.warning('请填写文章标题')
    return
  }
  if (editingId.value) {
    await contentAdminApi.updateArticle(editingId.value, { ...form })
    ElMessage.success('文章已更新')
  } else {
    await contentAdminApi.createArticle({ ...form })
    ElMessage.success('文章已创建')
  }
  dialogVisible.value = false
  await load()
}

async function togglePublish(article: Article) {
  const next = article.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED'
  await contentAdminApi.publishArticle(article.id, next)
  ElMessage.success(next === 'PUBLISHED' ? '文章已发布' : '文章已下架')
  await load()
}

async function disable(article: Article) {
  await ElMessageBox.confirm(`确定停用文章「${article.title}」吗？`, '提示', { type: 'warning' })
  await contentAdminApi.disableArticle(article.id)
  ElMessage.success('文章已停用')
  await load()
}

onMounted(async () => {
  const response = await contentAdminApi.categories()
  categories.value = response.data.data
  await load()
})
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">文章管理</div>
        <div class="muted">考研资讯、复习经验与政策解读</div>
      </div>
      <el-button v-priv="{ module: 'article', method: 'create' }" type="primary" @click="openCreate">
        新增文章
      </el-button>
    </div>

    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="filters.keyword" placeholder="标题关键词" clearable style="width: 220px" @keyup.enter="search" />
        <el-select v-model="filters.categoryId" placeholder="分类" clearable style="width: 150px" @change="search">
          <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
        </el-select>
        <el-select v-model="filters.status" placeholder="状态" clearable style="width: 140px" @change="search">
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="草稿" value="DRAFT" />
        </el-select>
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button @click="filters.keyword = ''; filters.status = ''; filters.categoryId = undefined; search()">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="articles" stripe>
        <el-table-column label="封面" width="110">
          <template #default="{ row }">
            <div class="cover-thumb" :style="row.coverUrl ? { backgroundImage: `url(${row.coverUrl})` } : undefined"></div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="240" />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column prop="author" label="作者" width="110" />
        <el-table-column prop="viewCount" label="阅读" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">
              {{ row.status === 'PUBLISHED' ? '已发布' : row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-priv="{ module: 'article', method: 'edit' }" link type="primary" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button
              v-priv="{ module: 'article', method: 'publish' }"
              link
              :type="row.status === 'PUBLISHED' ? 'warning' : 'success'"
              @click="togglePublish(row)"
            >
              {{ row.status === 'PUBLISHED' ? '下架' : '发布' }}
            </el-button>
            <el-button v-priv="{ module: 'article', method: 'delete' }" link type="danger" @click="disable(row)">
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑文章' : '新增文章'" width="760px" top="5vh">
      <el-form label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="分类">
              <el-select v-model="form.categoryId" style="width: 100%">
                <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作者">
              <el-input v-model="form.author" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="封面">
          <FileUploader v-model="form.coverUrl" category="article" accept="image/*" label="上传封面" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="发布" value="PUBLISHED" />
          </el-select>
        </el-form-item>
        <el-form-item label="正文 HTML">
          <el-input v-model="form.contentHtml" type="textarea" :rows="10" placeholder="支持简单 HTML，如 <p>段落</p>" />
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
.cover-thumb {
  width: 84px;
  height: 48px;
  border-radius: 6px;
  background: linear-gradient(135deg, #e60012, #ff7a00);
  background-size: cover;
  background-position: center;
}
</style>
