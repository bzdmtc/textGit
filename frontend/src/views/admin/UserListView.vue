<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { permissionApi } from '@/api/permission'
import { userApi, type UserCreatePayload, type UserUpdatePayload } from '@/api/user'
import type { Group, User, UserPermissionView } from '@/types'

const users = ref<User[]>([])
const groups = ref<Group[]>([])
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)

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
    const response = await userApi.page(keyword.value, page.value, size.value)
    users.value = response.data.data.items
    total.value = response.data.data.total
  } finally {
    loading.value = false
  }
}

async function loadGroups() {
  const response = await permissionApi.groups()
  groups.value = response.data.data.filter((group) => group.status === 'ACTIVE')
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
  })
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

onMounted(async () => {
  await Promise.all([load(), loadGroups()])
})
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">用户管理</div>
        <div class="muted">职位是组织岗位信息；权限由多个分组并集决定，两者互不影响</div>
      </div>
      <el-button v-priv="{ module: 'user', method: 'create' }" type="primary" @click="openCreate">
        新增用户
      </el-button>
    </div>

    <div class="page-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索账号 / 姓名 / 职位 / 手机 / 邮箱"
          style="width: 320px"
          clearable
          @keyup.enter="search"
        />
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button @click="keyword = ''; search()">重置</el-button>
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
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
              {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
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
          <el-input v-model="createForm.position" placeholder="例如：考研政治讲师、学员" />
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
          <el-input v-model="editForm.position" placeholder="例如：考研政治讲师、学员" />
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
  </div>
</template>
