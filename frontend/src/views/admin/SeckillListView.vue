<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { courseAdminApi } from '@/api/course'
import { seckillAdminApi, type SeckillActivityPayload } from '@/api/seckill'
import { formatDateTime, parseUtc } from '@/utils/format'
import type { Course, SeckillActivity, SeckillRecord } from '@/types'

const activities = ref<SeckillActivity[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const statusFilter = ref('')
const loading = ref(false)

const courses = ref<Course[]>([])

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const submitting = ref(false)
const form = reactive({
  courseId: undefined as number | undefined,
  seckillPrice: 0,
  totalStock: 50,
  perUserLimit: 1,
  startAt: '',
  endAt: '',
  status: 'ON',
})

const recordsVisible = ref(false)
const records = ref<SeckillRecord[]>([])
const recordTotal = ref(0)
const recordPage = ref(1)
const recordSize = ref(20)
const recordActivity = ref<SeckillActivity | null>(null)

function pad(value: number) {
  return String(value).padStart(2, '0')
}

function toLocalInput(date: Date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}

function fromUtc(value?: string | null) {
  const date = parseUtc(value)
  return date ? toLocalInput(date) : ''
}

function toUtcWall(local: string) {
  const date = new Date(local)
  if (Number.isNaN(date.getTime())) return ''
  return date.toISOString().slice(0, 19)
}

function phaseMeta(phase: string) {
  const map: Record<string, { text: string; type: 'danger' | 'warning' | 'info' | 'success' }> = {
    ACTIVE: { text: '进行中', type: 'danger' },
    UPCOMING: { text: '未开始', type: 'warning' },
    ENDED: { text: '已结束', type: 'info' },
    OFF: { text: '已停用', type: 'info' },
  }
  return map[phase] || { text: phase, type: 'info' as const }
}

async function load() {
  loading.value = true
  try {
    const response = await seckillAdminApi.page({
      status: statusFilter.value || undefined,
      page: page.value,
      size: size.value,
    })
    activities.value = response.data.data.items
    total.value = response.data.data.total
  } finally {
    loading.value = false
  }
}

async function loadCourses() {
  const response = await courseAdminApi.page({ status: 'PUBLISHED', page: 1, size: 100 })
  courses.value = response.data.data.items
}

function emptyForm() {
  const now = new Date()
  const end = new Date(now.getTime() + 24 * 3600 * 1000)
  return {
    courseId: undefined as number | undefined,
    seckillPrice: 0,
    totalStock: 50,
    perUserLimit: 1,
    startAt: toLocalInput(now),
    endAt: toLocalInput(end),
    status: 'ON',
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(activity: SeckillActivity) {
  editingId.value = activity.id
  Object.assign(form, {
    courseId: activity.courseId,
    seckillPrice: Number(activity.seckillPrice),
    totalStock: activity.totalStock,
    perUserLimit: 1,
    startAt: fromUtc(activity.startAt),
    endAt: fromUtc(activity.endAt),
    status: activity.status,
  })
  dialogVisible.value = true
}

function selectedCourse() {
  return courses.value.find((course) => course.id === form.courseId)
}

function onCourseChange(courseId: number) {
  const course = courses.value.find((item) => item.id === courseId)
  if (course && !form.seckillPrice) {
    form.seckillPrice = Number((Number(course.price) * 0.6).toFixed(2))
  }
}

function editingSold() {
  if (!editingId.value) return 0
  const current = activities.value.find((item) => item.id === editingId.value)
  return current?.soldCount ?? 0
}

async function submit() {
  if (!form.courseId) {
    ElMessage.warning('请选择课程')
    return
  }
  const course = selectedCourse()
  if (!course) {
    ElMessage.warning('课程不存在或已下架')
    return
  }
  if (Number(form.seckillPrice) <= 0) {
    ElMessage.warning('秒杀价必须大于 0')
    return
  }
  if (Number(form.seckillPrice) > Number(course.price)) {
    ElMessage.warning('秒杀价不能高于课程原价')
    return
  }
  if (form.totalStock < editingSold()) {
    ElMessage.warning(`总名额不能小于已售数量 ${editingSold()}`)
    return
  }
  if (!form.startAt || !form.endAt || new Date(form.startAt) >= new Date(form.endAt)) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }
  const payload: SeckillActivityPayload = {
    courseId: form.courseId,
    seckillPrice: Number(form.seckillPrice),
    totalStock: form.totalStock,
    perUserLimit: 1,
    startAt: toUtcWall(form.startAt),
    endAt: toUtcWall(form.endAt),
    status: form.status,
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await seckillAdminApi.update(editingId.value, payload)
      ElMessage.success('秒杀活动已更新')
    } else {
      await seckillAdminApi.create(payload)
      ElMessage.success('秒杀活动已创建并预热 Redis 库存')
    }
    dialogVisible.value = false
    await load()
  } catch {
    // 错误信息已由 http 拦截器提示
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(activity: SeckillActivity) {
  const next = activity.status === 'ON' ? 'OFF' : 'ON'
  await ElMessageBox.confirm(
    next === 'ON' ? '上线后重新按 DB 库存预热 Redis，确定上线吗？' : '停用后前台不再展示，确定停用吗？',
    '秒杀活动状态',
    { type: 'warning' },
  )
  await seckillAdminApi.setStatus(activity.id, next)
  ElMessage.success(next === 'ON' ? '活动已上线' : '活动已停用')
  await load()
}

async function reconcile(activity: SeckillActivity) {
  await ElMessageBox.confirm(
    '将按有效抢购记录重算 DB 剩余库存，并覆盖 Redis 库存。进行中的活动请谨慎操作。',
    '库存对账',
    { type: 'warning' },
  )
  await seckillAdminApi.reconcile(activity.id)
  ElMessage.success('库存对账完成')
  await load()
}

async function remove(activity: SeckillActivity) {
  await ElMessageBox.confirm(`确定删除秒杀活动「${activity.courseTitle}」吗？`, '删除活动', {
    type: 'warning',
  })
  await seckillAdminApi.remove(activity.id)
  ElMessage.success('活动已删除')
  await load()
}

async function openRecords(activity: SeckillActivity) {
  recordActivity.value = activity
  recordPage.value = 1
  recordsVisible.value = true
  await loadRecords()
}

async function loadRecords() {
  if (!recordActivity.value) return
  const response = await seckillAdminApi.records(recordActivity.value.id, {
    page: recordPage.value,
    size: recordSize.value,
  })
  records.value = response.data.data.items
  recordTotal.value = response.data.data.total
}

function search() {
  page.value = 1
  void load()
}

onMounted(async () => {
  await Promise.all([load(), loadCourses()])
})
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">秒杀活动</div>
        <div class="muted">Redis 预扣库存 · Kafka 异步建单 · 每人限购 1 份，订单 15 分钟内支付</div>
      </div>
      <el-button v-priv="{ module: 'seckill', method: 'create' }" type="primary" @click="openCreate">
        新增秒杀活动
      </el-button>
    </div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="statusFilter" placeholder="活动状态" clearable style="width: 140px" @change="search">
          <el-option label="上线 ON" value="ON" />
          <el-option label="停用 OFF" value="OFF" />
        </el-select>
        <el-button @click="search">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="activities" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="课程" min-width="200">
          <template #default="{ row }">
            <div class="course-cell">
              <img v-if="row.courseCover" :src="row.courseCover" alt="" />
              <span>{{ row.courseTitle }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="150">
          <template #default="{ row }">
            <div class="price-cell">
              <strong>¥{{ Number(row.seckillPrice).toFixed(2) }}</strong>
              <span>原价 ¥{{ Number(row.coursePrice).toFixed(2) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="库存（DB / Redis）" width="150">
          <template #default="{ row }">
            {{ row.availableStock }} / {{ row.redisStock ?? '-' }}
            <div class="muted">总 {{ row.totalStock }} · 已售 {{ row.soldCount }}</div>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="210">
          <template #default="{ row }">
            <div class="muted">{{ formatDateTime(row.startAt) }}</div>
            <div>{{ formatDateTime(row.endAt) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="phaseMeta(row.phase).type">{{ phaseMeta(row.phase).text }}</el-tag>
            <div class="muted">{{ row.status }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="270" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openRecords(row)">抢购记录</el-button>
            <el-button
              v-priv="{ module: 'seckill', method: 'edit' }"
              link
              type="primary"
              @click="openEdit(row)"
            >编辑</el-button>
            <el-button
              v-priv="{ module: 'seckill', method: 'edit' }"
              link
              :type="row.status === 'ON' ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >{{ row.status === 'ON' ? '停用' : '上线' }}</el-button>
            <el-button
              v-priv="{ module: 'seckill', method: 'edit' }"
              link
              type="primary"
              @click="reconcile(row)"
            >对账</el-button>
            <el-button
              v-priv="{ module: 'seckill', method: 'delete' }"
              link
              type="danger"
              @click="remove(row)"
            >删除</el-button>
          </template>
        </el-table-column>
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
      :title="editingId ? '编辑秒杀活动' : '新增秒杀活动'"
      width="560px"
    >
      <el-form label-width="110px">
        <el-form-item label="课程" required>
          <el-select
            v-model="form.courseId"
            placeholder="选择已上架课程"
            filterable
            style="width: 100%"
            @change="onCourseChange"
          >
            <el-option
              v-for="course in courses"
              :key="course.id"
              :label="`${course.title}（原价 ¥${Number(course.price).toFixed(2)}）`"
              :value="course.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="秒杀价" required>
          <el-input-number v-model="form.seckillPrice" :min="0.01" :precision="2" :step="10" />
          <span v-if="selectedCourse()" class="muted" style="margin-left: 10px">
            原价 ¥{{ Number(selectedCourse()!.price).toFixed(2) }}
          </span>
        </el-form-item>
        <el-form-item label="总名额" required>
          <el-input-number v-model="form.totalStock" :min="1" :step="10" />
          <span v-if="editingId" class="muted" style="margin-left: 10px">已售 {{ editingSold() }}</span>
        </el-form-item>
        <el-form-item label="每人限购">
          <el-input-number :model-value="1" :min="1" :max="1" disabled />
          <span class="muted" style="margin-left: 10px">当前版本固定 1 份（唯一约束防重复下单）</span>
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-date-picker
            v-model="form.startAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择开始时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-date-picker
            v-model="form.endAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="上线状态">
          <el-switch v-model="form.status" active-value="ON" inactive-value="OFF" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="recordsVisible"
      :title="`抢购记录 · ${recordActivity?.courseTitle || ''}`"
      size="640px"
    >
      <el-table :data="records" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="userAccount" label="账号" width="130" />
        <el-table-column prop="orderNo" label="订单号" min-width="180">
          <template #default="{ row }">{{ row.orderNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column prop="reason" label="说明" min-width="140">
          <template #default="{ row }">{{ row.reason || '-' }}</template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="recordPage"
        v-model:page-size="recordSize"
        class="pager"
        layout="total, prev, pager, next"
        :total="recordTotal"
        @current-change="loadRecords"
      />
    </el-drawer>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.course-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.course-cell img {
  width: 52px;
  height: 34px;
  border-radius: 6px;
  object-fit: cover;
}

.price-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.price-cell strong {
  color: #e60012;
}

.price-cell span {
  color: #9ca3af;
  font-size: 12px;
}
</style>
