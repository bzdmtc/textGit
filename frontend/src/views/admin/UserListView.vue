<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { permissionApi, type PositionPayload } from '@/api/permission'
import { userApi, type UserCreatePayload, type UserUpdatePayload } from '@/api/user'
import { formatDateTime } from '@/utils/format'
import type { Group, Position, User, UserActivity, UserPermissionView } from '@/types'

const users = ref<User[]>([])
const groups = ref<Group[]>([])
const positions = ref<Position[]>([])
const keyword = ref('')
const filterStatus = ref('')
const filterPosition = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)

const activityVisible = ref(false)
const activityLoading = ref(false)
const activityUser = ref<User | null>(null)
const activityData = ref<UserActivity | null>(null)

const createVisible = ref(false)
const createForm = reactive<UserCreatePayload>({
  account: '',
  password: '',
  realname: '',
  nickname: '',
  position: '',
  phone: '',
  email: '',
  groupIds: [],
})

const editVisible = ref(false)
const editingId = ref<number | null>(null)
const editForm = reactive<UserUpdatePayload>({
  realname: '',
  nickname: '',
  position: '',
  phone: '',
  email: '',
  avatar: '',
  groupIds: [],
})
const lastCreatePosition = ref('')
const lastEditPosition = ref('')

const positionVisible = ref(false)
const positionLoading = ref(false)
const positionFormVisible = ref(false)
const positionEditingId = ref<number | null>(null)
const positionSaving = ref(false)
const positionForm = reactive<{ name: string; groupId: number | undefined; sort: number }>({
  name: '',
  groupId: undefined,
  sort: 0,
})

const groupVisible = ref(false)
const groupUserId = ref<number | null>(null)
const selectedGroupIds = ref<number[]>([])

const permVisible = ref(false)
const permData = ref<UserPermissionView | null>(null)
const permLoading = ref(false)

async function load() {
  loading.value = true
  try {
    const response = await userApi.page({
      keyword: keyword.value || undefined,
      status: filterStatus.value || undefined,
      position: filterPosition.value || undefined,
      page: page.value,
      size: size.value,
    })
    users.value = response.data.data.items
    total.value = response.data.data.total
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  keyword.value = ''
  filterStatus.value = ''
  filterPosition.value = ''
  search()
}

async function loadGroups() {
  const response = await permissionApi.groups()
  groups.value = response.data.data.filter((group) => group.status === 'ACTIVE')
}

async function loadPositions() {
  positionLoading.value = true
  try {
    const response = await permissionApi.positions()
    positions.value = response.data.data
  } finally {
    positionLoading.value = false
  }
}

function switchPositionGroup(oldName: string, newName: string, current: number[] | undefined): number[] {
  const set = new Set(current ?? [])
  const oldPosition = positions.value.find((item) => item.name === oldName)
  if (oldPosition) {
    set.delete(oldPosition.groupId)
  }
  const newPosition = positions.value.find((item) => item.name === newName)
  if (newPosition) {
    set.add(newPosition.groupId)
  }
  return [...set]
}

function onCreatePositionChange(name: string) {
  createForm.groupIds = switchPositionGroup(lastCreatePosition.value, name, createForm.groupIds)
  lastCreatePosition.value = name || ''
}

function onEditPositionChange(name: string) {
  editForm.groupIds = switchPositionGroup(lastEditPosition.value, name, editForm.groupIds)
  lastEditPosition.value = name || ''
}

function openPositions() {
  positionVisible.value = true
  void loadPositions()
}

function openPositionCreate() {
  positionEditingId.value = null
  positionForm.name = ''
  positionForm.groupId = groups.value[0]?.id
  positionForm.sort = (positions.value.length + 1) * 10
  positionFormVisible.value = true
}

function openPositionEdit(position: Position) {
  positionEditingId.value = position.id
  positionForm.name = position.name
  positionForm.groupId = position.groupId
  positionForm.sort = position.sort
  positionFormVisible.value = true
}

async function savePosition() {
  if (!positionForm.name.trim()) {
    ElMessage.warning('请填写职位名称')
    return
  }
  if (!positionForm.groupId) {
    ElMessage.warning('请选择默认权限分组')
    return
  }
  const payload: PositionPayload = {
    name: positionForm.name.trim(),
    groupId: positionForm.groupId,
    sort: positionForm.sort,
  }
  positionSaving.value = true
  try {
    if (positionEditingId.value) {
      await permissionApi.updatePosition(positionEditingId.value, payload)
      ElMessage.success('职位已更新，现有用户的职位名称已同步')
    } else {
      await permissionApi.createPosition(payload)
      ElMessage.success('职位已创建')
    }
    positionFormVisible.value = false
    await Promise.all([loadPositions(), load()])
  } finally {
    positionSaving.value = false
  }
}

async function togglePositionStatus(position: Position) {
  const next = position.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await ElMessageBox.confirm(
    `确定${next === 'ACTIVE' ? '启用' : '停用'}职位「${position.name}」吗？停用后新用户不能再选择该职位。`,
    '职位状态',
    { type: 'warning' },
  )
  await permissionApi.setPositionStatus(position.id, next)
  ElMessage.success('职位状态已更新')
  await loadPositions()
}

function search() {
  page.value = 1
  load()
}

function openCreate() {
  Object.assign(createForm, {
    account: '',
    password: '',
    realname: '',
    nickname: '',
    position: '',
    phone: '',
    email: '',
    groupIds: [],
  })
  lastCreatePosition.value = ''
  createVisible.value = true
}

async function saveCreate() {
  if (!createForm.account.trim() || createForm.password.length < 6) {
    ElMessage.warning('账号和至少 6 位密码必填')
    return
  }
  await userApi.create({ ...createForm })
  ElMessage.success('用户已创建')
  createVisible.value = false
  await load()
}

function openEdit(user: User) {
  editingId.value = user.id
  Object.assign(editForm, {
    realname: user.realname || '',
    nickname: user.nickname || '',
    position: user.position || '',
    phone: user.phone || '',
    email: user.email || '',
    avatar: user.avatar || '',
    groupIds: user.groups.map((group) => group.id),
  })
  lastEditPosition.value = user.position || ''
  editVisible.value = true
}

async function saveEdit() {
  if (!editingId.value) return
  await userApi.update(editingId.value, { ...editForm })
  ElMessage.success('用户已更新')
  editVisible.value = false
  await load()
}

async function toggleStatus(user: User) {
  const next = user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await ElMessageBox.confirm(
    `确定将用户「${user.account}」${next === 'ACTIVE' ? '启用' : '禁用'}吗？`,
    '提示',
    { type: 'warning' },
  )
  await userApi.setStatus(user.id, next)
  ElMessage.success('状态已更新')
  await load()
}

function openGroups(user: User) {
  groupUserId.value = user.id
  selectedGroupIds.value = user.groups.map((group) => group.id)
  groupVisible.value = true
}

async function saveGroups() {
  if (!groupUserId.value) return
  await userApi.assignGroups(groupUserId.value, selectedGroupIds.value)
  ElMessage.success('分组已保存，权限立即生效')
  groupVisible.value = false
  await load()
}

async function openPermissions(user: User) {
  permVisible.value = true
  permLoading.value = true
  try {
    const response = await userApi.permissions(user.id)
    permData.value = response.data.data
  } finally {
    permLoading.value = false
  }
}

async function openActivity(user: User) {
  activityUser.value = user
  activityData.value = null
  activityVisible.value = true
  activityLoading.value = true
  try {
    const response = await userApi.activity(user.id)
    activityData.value = response.data.data
  } finally {
    activityLoading.value = false
  }
}

onMounted(async () => {
  await Promise.all([load(), loadGroups(), loadPositions()])
})
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">用户管理</div>
        <div class="muted">选择职位会自动带出对应权限分组，可再调整；最终权限仍由分组并集决定</div>
      </div>
      <div style="display: flex; gap: 10px">
        <el-button v-priv="{ module: 'user', method: 'edit' }" @click="openPositions">
          职位管理
        </el-button>
        <el-button v-priv="{ module: 'user', method: 'create' }" type="primary" @click="openCreate">
          新增用户
        </el-button>
      </div>
    </div>

    <div class="page-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索账号 / 姓名 / 职位 / 手机 / 邮箱"
          style="width: 280px"
          clearable
          @keyup.enter="search"
        />
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 110px" @change="search">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
        <el-select v-model="filterPosition" placeholder="职位" clearable style="width: 160px" @change="search">
          <el-option v-for="position in positions" :key="position.id" :label="position.name" :value="position.name" />
        </el-select>
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="users" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="account" label="账号" width="130" />
        <el-table-column prop="realname" label="姓名" width="110" />
        <el-table-column prop="position" label="职位" width="130">
          <template #default="{ row }">{{ row.position || '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="分组" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="group in row.groups" :key="group.id" size="small" style="margin-right: 4px">
              {{ group.code }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="最近登录" width="170">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
              {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <el-button v-priv="{ module: 'user', method: 'edit' }" link type="primary" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button
              v-priv="{ module: 'user', method: 'assign-group' }"
              link
              type="primary"
              @click="openGroups(row)"
            >
              分配分组
            </el-button>
            <el-button link type="primary" @click="openPermissions(row)">权限预览</el-button>
            <el-button link type="primary" @click="openActivity(row)">活跃数据</el-button>
            <el-button
              v-priv="{ module: 'user', method: 'disable' }"
              link
              :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next, sizes"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>

    <el-dialog v-model="createVisible" title="新增用户" width="560px">
      <el-form label-width="90px">
        <el-form-item label="账号" required>
          <el-input v-model="createForm.account" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="createForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="createForm.realname" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="createForm.nickname" />
        </el-form-item>
        <el-form-item label="职位">
          <el-select
            v-model="createForm.position"
            clearable
            placeholder="选择职位（自动带出权限分组）"
            style="width: 100%"
            @change="onCreatePositionChange"
          >
            <el-option
              v-for="position in positions"
              :key="position.id"
              :label="`${position.name}（${position.groupCode || position.groupName || '未映射'}）`"
              :value="position.name"
              :disabled="position.status !== 'ACTIVE'"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="createForm.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="createForm.email" />
        </el-form-item>
        <el-form-item label="分组">
          <el-select v-model="createForm.groupIds" multiple style="width: 100%">
            <el-option v-for="group in groups" :key="group.id" :label="`${group.name} (${group.code})`" :value="group.id" />
          </el-select>
          <div class="muted" style="font-size: 12px; margin-top: 4px">
            切换职位会移除旧职位的默认分组并加入新职位的默认分组，其他手动分组保留。
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑用户" width="560px">
      <el-form label-width="90px">
        <el-form-item label="姓名">
          <el-input v-model="editForm.realname" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" />
        </el-form-item>
        <el-form-item label="职位">
          <el-select
            v-model="editForm.position"
            clearable
            placeholder="选择职位（自动带出权限分组）"
            style="width: 100%"
            @change="onEditPositionChange"
          >
            <el-option
              v-for="position in positions"
              :key="position.id"
              :label="`${position.name}（${position.groupCode || position.groupName || '未映射'}）`"
              :value="position.name"
              :disabled="position.status !== 'ACTIVE'"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="权限分组">
          <el-select v-model="editForm.groupIds" multiple style="width: 100%" placeholder="请选择分组">
            <el-option v-for="group in groups" :key="group.id" :label="`${group.name} (${group.code})`" :value="group.id" />
          </el-select>
          <div class="muted" style="font-size: 12px; margin-top: 4px">
            切换职位会替换该职位的默认分组，其他手动分组保留；最终权限以实际分组并集为准。
          </div>
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="editForm.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" />
        </el-form-item>
        <el-form-item label="头像地址">
          <el-input v-model="editForm.avatar" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="positionVisible" title="职位管理" width="760px">
      <div class="muted" style="margin-bottom: 12px">
        职位是用户属性；每个职位绑定一个默认权限分组。选择职位时自动带出该分组，切换职位时替换旧职位的默认分组，最终权限仍以用户实际分组为准。
      </div>
      <div class="toolbar">
        <el-button
          v-priv="{ module: 'user', method: 'edit' }"
          type="primary"
          @click="openPositionCreate"
        >
          新增职位
        </el-button>
      </div>
      <el-table v-loading="positionLoading" :data="positions" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="职位名称" min-width="160" />
        <el-table-column label="默认权限分组" min-width="220">
          <template #default="{ row }">
            <span v-if="row.groupName">
              {{ row.groupName }}<span class="muted">（{{ row.groupCode }}）</span>
            </span>
            <span v-else class="muted">未映射</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button
              v-priv="{ module: 'user', method: 'edit' }"
              link
              type="primary"
              @click="openPositionEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-priv="{ module: 'user', method: 'edit' }"
              link
              :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
              @click="togglePositionStatus(row)"
            >
              {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无职位" />
        </template>
      </el-table>
      <template #footer>
        <el-button @click="positionVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="positionFormVisible"
      :title="positionEditingId ? '编辑职位' : '新增职位'"
      width="480px"
      append-to-body
    >
      <el-form label-width="100px">
        <el-form-item label="职位名称" required>
          <el-input v-model="positionForm.name" placeholder="如：考研英语讲师" />
        </el-form-item>
        <el-form-item label="默认分组" required>
          <el-select v-model="positionForm.groupId" style="width: 100%" placeholder="选择权限分组">
            <el-option
              v-for="group in groups"
              :key="group.id"
              :label="`${group.name}（${group.code}）`"
              :value="group.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="positionForm.sort" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <div class="muted" style="font-size: 12px; padding-left: 100px">
        修改职位名称会同步更新现有用户的职位显示，但不会自动改动他们的分组。
      </div>
      <template #footer>
        <el-button @click="positionFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="positionSaving" @click="savePosition">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="groupVisible" title="分配用户分组" width="520px">
      <div class="muted" style="margin-bottom: 12px">保存后会递增该用户的权限版本，旧令牌下一次请求即按新权限判定。</div>
      <el-select v-model="selectedGroupIds" multiple style="width: 100%" placeholder="请选择分组">
        <el-option v-for="group in groups" :key="group.id" :label="`${group.name} (${group.code})`" :value="group.id" />
      </el-select>
      <template #footer>
        <el-button @click="groupVisible = false">取消</el-button>
        <el-button type="primary" @click="saveGroups">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permVisible" title="有效权限预览" width="720px" top="7vh">
      <div v-loading="permLoading">
        <template v-if="permData">
          <el-descriptions :column="3" border style="margin-bottom: 16px">
            <el-descriptions-item label="用户">{{ permData.account }}</el-descriptions-item>
            <el-descriptions-item label="数据范围">{{ permData.dataScope }}</el-descriptions-item>
            <el-descriptions-item label="受限">{{ permData.limited ? '是' : '否' }}</el-descriptions-item>
          </el-descriptions>
          <div v-if="Object.keys(permData.rights).length === 0" class="muted">该用户没有任何权限。</div>
          <div v-for="(methods, module) in permData.rights" :key="module" class="priv-module">
            <div class="priv-module-title">{{ module }}</div>
            <el-tag v-for="method in methods" :key="method" style="margin-right: 6px">{{ method }}</el-tag>
          </div>
        </template>
      </div>
    </el-dialog>

    <el-dialog v-model="activityVisible" title="用户活跃数据" width="560px">
      <div v-loading="activityLoading">
        <div v-if="activityUser" class="activity-head">
          <div>
            <div class="activity-name">{{ activityUser.realname || activityUser.nickname || activityUser.account }}</div>
            <div class="muted">{{ activityUser.account }} · {{ activityUser.position || '未设置职位' }}</div>
          </div>
          <el-tag :type="activityUser.status === 'ACTIVE' ? 'success' : 'info'">
            {{ activityUser.status === 'ACTIVE' ? '启用' : '禁用' }}
          </el-tag>
        </div>
        <el-row v-if="activityData" :gutter="12" style="margin-top: 16px">
          <el-col :span="8">
            <div class="activity-metric">
              <span>已支付订单</span>
              <strong>{{ activityData.paidOrderCount }} 笔</strong>
              <small>全部订单 {{ activityData.orderCount }} 笔</small>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="activity-metric">
              <span>累计消费</span>
              <strong>¥{{ Number(activityData.paidAmount).toFixed(2) }}</strong>
              <small>已支付 / 已完成订单</small>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="activity-metric">
              <span>已购课程</span>
              <strong>{{ activityData.courseCount }} 门</strong>
              <small>当前有效选课</small>
            </div>
          </el-col>
          <el-col :span="8" style="margin-top: 12px">
            <div class="activity-metric">
              <span>练习 / 考试</span>
              <strong>{{ activityData.practiceCount }} 次</strong>
              <small>含章节练习与套卷</small>
            </div>
          </el-col>
          <el-col :span="8" style="margin-top: 12px">
            <div class="activity-metric">
              <span>待攻克错题</span>
              <strong>{{ activityData.wrongCount }} 道</strong>
              <small>未标记掌握</small>
            </div>
          </el-col>
          <el-col :span="8" style="margin-top: 12px">
            <div class="activity-metric">
              <span>注册时间</span>
              <strong style="font-size: 14px">{{ formatDateTime(activityUser?.createdAt) }}</strong>
              <small>最近登录 {{ formatDateTime(activityUser?.lastLoginAt) }}</small>
            </div>
          </el-col>
        </el-row>
      </div>
      <template #footer>
        <el-button @click="activityVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.activity-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.activity-name {
  font-size: 17px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 4px;
}

.activity-metric {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #eef0f4;
  height: 100%;
}

.activity-metric span {
  color: #6b7280;
  font-size: 12px;
}

.activity-metric strong {
  font-size: 18px;
  color: #111827;
  font-variant-numeric: tabular-nums;
}

.activity-metric small {
  color: #9ca3af;
  font-size: 11px;
}
</style>
