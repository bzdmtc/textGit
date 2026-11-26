<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { couponAdminApi, type CouponPayload } from '@/api/trade'
import { formatDateTime } from '@/utils/format'
import type { Coupon } from '@/types'

const coupons = ref<Coupon[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const statusFilter = ref('')
const loading = ref(false)

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<CouponPayload>(emptyForm())

function toLocalIso(date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}

function emptyForm(): CouponPayload {
  const now = new Date()
  const end = new Date(now.getTime() + 90 * 24 * 3600 * 1000)
  return {
    name: '',
    type: 'FIXED',
    value: 10,
    minAmount: 100,
    totalQuantity: 100,
    perUserLimit: 1,
    startAt: toLocalIso(now),
    endAt: toLocalIso(end),
    subject: '',
    description: '',
    status: 'ON',
  }
}

async function load() {
  loading.value = true
  try {
    const response = await couponAdminApi.page({
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined,
      page: page.value,
      size: size.value,
    })
    coupons.value = response.data.data.items
    total.value = response.data.data.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(coupon: Coupon) {
  editingId.value = coupon.id
  Object.assign(form, {
    name: coupon.name,
    type: coupon.type === 'PERCENT' ? 'PERCENT' : 'FIXED',
    value: Number(coupon.value),
    minAmount: Number(coupon.minAmount),
    totalQuantity: coupon.totalQuantity,
    perUserLimit: coupon.perUserLimit,
    startAt: coupon.startAt,
    endAt: coupon.endAt,
    subject: coupon.subject || '',
    description: coupon.description || '',
    status: coupon.status,
  })
  dialogVisible.value = true
}

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写优惠券名称')
    return
  }
  if (!form.startAt || !form.endAt) {
    ElMessage.warning('请选择有效期')
    return
  }
  if (form.type === 'PERCENT' && Number(form.value) >= 100) {
    ElMessage.warning('折扣百分比必须小于 100（90 表示 9 折）')
    return
  }
  const payload: CouponPayload = {
    ...form,
    subject: form.subject?.trim() || '',
  }
  if (editingId.value) {
    await couponAdminApi.update(editingId.value, payload)
    ElMessage.success('优惠券已更新')
  } else {
    await couponAdminApi.create(payload)
    ElMessage.success('优惠券已创建')
  }
  dialogVisible.value = false
  await load()
}

async function toggleStatus(coupon: Coupon) {
  const next = coupon.status === 'ON' ? 'OFF' : 'ON'
  if (next === 'OFF') {
    await ElMessageBox.confirm(`确定停用「${coupon.name}」吗？已领取的券在有效期内仍可使用。`, '提示', {
      type: 'warning',
    })
  }
  await couponAdminApi.setStatus(coupon.id, next)
  ElMessage.success(next === 'ON' ? '优惠券已启用' : '优惠券已停用')
  await load()
}

function valueText(coupon: Coupon) {
  return coupon.type === 'PERCENT'
    ? `${(Number(coupon.value) / 10).toFixed(1)} 折`
    : `减 ¥${Number(coupon.value).toFixed(2)}`
}

function typeText(coupon: Coupon) {
  return coupon.type === 'PERCENT' ? '折扣券' : '满减券'
}

function onSearch() {
  page.value = 1
  load()
}

function onPageChange(value: number) {
  page.value = value
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">优惠券管理</div>
        <div class="muted">创建满减/折扣券，学员领取后下单锁定、支付核销</div>
      </div>
      <el-button v-priv="{ module: 'coupon', method: 'create' }" type="primary" @click="openCreate">
        新增优惠券
      </el-button>
    </div>

    <div class="page-card">
      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索优惠券名称"
          clearable
          style="width: 240px"
          @keyup.enter="onSearch"
          @clear="onSearch"
        />
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 140px" @change="onSearch">
          <el-option label="启用" value="ON" />
          <el-option label="停用" value="OFF" />
        </el-select>
        <el-button type="primary" plain @click="onSearch">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="coupons" stripe>
        <el-table-column prop="name" label="名称" min-width="200" />
        <el-table-column label="类型/优惠" width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 'PERCENT' ? 'warning' : 'danger'" effect="plain">
              {{ typeText(row) }}
            </el-tag>
            <span style="margin-left: 6px">{{ valueText(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="使用门槛" width="120">
          <template #default="{ row }">
            {{ Number(row.minAmount) > 0 ? `满 ¥${Number(row.minAmount).toFixed(2)}` : '无门槛' }}
          </template>
        </el-table-column>
        <el-table-column label="库存" width="120">
          <template #default="{ row }">
            <strong :class="{ danger: row.remainingQuantity <= 0 }">{{ row.remainingQuantity }}</strong>
            / {{ row.totalQuantity }}
          </template>
        </el-table-column>
        <el-table-column prop="perUserLimit" label="每人限领" width="90" />
        <el-table-column label="有效期" width="210">
          <template #default="{ row }">
            <div class="time-line">{{ formatDateTime(row.startAt) }}</div>
            <div class="time-line muted">至 {{ formatDateTime(row.endAt) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="适用科目" width="110">
          <template #default="{ row }">{{ row.subject || '全站通用' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ON' ? 'success' : 'info'">{{ row.status === 'ON' ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-priv="{ module: 'coupon', method: 'edit' }" link type="primary" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button
              v-priv="{ module: 'coupon', method: 'delete' }"
              link
              :type="row.status === 'ON' ? 'danger' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 'ON' ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          layout="total, prev, pager, next"
          :total="total"
          :page-size="size"
          :current-page="page"
          @current-change="onPageChange"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑优惠券' : '新增优惠券'" width="680px">
      <el-form label-width="110px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" maxlength="100" show-word-limit placeholder="例如：新人专享 · 满 500 减 50" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-radio-group v-model="form.type">
            <el-radio value="FIXED">满减券</el-radio>
            <el-radio value="PERCENT">折扣券</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="form.type === 'PERCENT' ? '折扣百分比' : '减免金额'" required>
          <el-input-number v-model="form.value" :min="1" :max="form.type === 'PERCENT' ? 99 : 99999" :precision="2" />
          <span class="form-hint">
            {{ form.type === 'PERCENT' ? '90 表示 9 折（支付原价 90%）' : '单位：元' }}
          </span>
        </el-form-item>
        <el-form-item label="最低消费">
          <el-input-number v-model="form.minAmount" :min="0" :precision="2" />
          <span class="form-hint">订单金额需达到该值才能使用，0 表示无门槛</span>
        </el-form-item>
        <el-form-item label="发放数量" required>
          <el-input-number v-model="form.totalQuantity" :min="1" :max="100000" />
        </el-form-item>
        <el-form-item label="每人限领" required>
          <el-input-number v-model="form.perUserLimit" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="有效期" required>
          <el-date-picker
            v-model="form.startAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="开始时间"
            style="width: 200px"
          />
          <span class="form-hint" style="margin: 0 8px">至</span>
          <el-date-picker
            v-model="form.endAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="结束时间"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="适用科目">
          <el-input v-model="form.subject" placeholder="留空表示全站通用，例如：数学一" maxlength="32" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 160px">
            <el-option label="启用" value="ON" />
            <el-option label="停用" value="OFF" />
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
.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.time-line {
  font-size: 12px;
  line-height: 1.6;
}

.muted {
  color: #9ca3af;
}

.danger {
  color: #e60012;
}

.form-hint {
  color: #9ca3af;
  font-size: 12px;
  margin-left: 10px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
