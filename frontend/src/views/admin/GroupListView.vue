<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { permissionApi, type GroupPayload } from '@/api/permission'
import type { Group, Priv } from '@/types'

const groups = ref<Group[]>([])
const privs = ref<Priv[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<GroupPayload>({
  code: '',
  name: '',
  role: '',
  dataScope: 'SELF',
  limited: false,
  scopeType: 'GLOBAL',
  scopeId: 0,
  sort: 0,
})

const privDialogVisible = ref(false)
const privGroup = ref<Group | null>(null)
const checkedPrivIds = ref<number[]>([])
const privSaving = ref(false)

const moduleLabels: Record<string, string> = {
  admin: '后台',
  manage: '管理端',
  user: '用户',
  group: '权限分组',
  priv: '权限字典',
  audit: '审计',
  profile: '个人资料',
}

const privModules = computed(() => {
  const map = new Map<string, Priv[]>()
  for (const priv of privs.value) {
    if (priv.status !== 'ACTIVE') continue
    const list = map.get(priv.module) || []
    list.push(priv)
    map.set(priv.module, list)
  }
  return Array.from(map.entries()).map(([module, items]) => ({
    module,
    label: moduleLabels[module] || module,
    items,
  }))
})

async function load() {
  loading.value = true
  try {
    const [groupResponse, privResponse] = await Promise.all([permissionApi.groups(), permissionApi.privs()])
    groups.value = groupResponse.data.data
    privs.value = privResponse.data.data
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    code: '',
    name: '',
    role: '',
    dataScope: 'SELF',
    limited: false,
    scopeType: 'GLOBAL',
    scopeId: 0,
    sort: (groups.value.length + 1) * 10,
  })
  dialogVisible.value = true
}

function openEdit(group: Group) {
  editingId.value = group.id
  Object.assign(form, {
    code: group.code,
    name: group.name,
    role: group.role || '',
    dataScope: group.dataScope,
    limited: group.limited,
    scopeType: group.scopeType,
    scopeId: group.scopeId,
    sort: group.sort,
  })
  dialogVisible.value = true
}

async function saveGroup() {
  if (!form.code.trim() || !form.name.trim()) {
    ElMessage.warning('请填写分组编码和名称')
    return
  }
  if (editingId.value) {
    await permissionApi.updateGroup(editingId.value, { ...form })
    ElMessage.success('分组已更新')
  } else {
    await permissionApi.createGroup({ ...form })
    ElMessage.success('分组已创建')
  }
  dialogVisible.value = false
  await load()
}

async function openPriv(group: Group) {
  privGroup.value = group
  const response = await permissionApi.groupPrivIds(group.id)
  checkedPrivIds.value = response.data.data
  privDialogVisible.value = true
}

async function savePrivs() {
  if (!privGroup.value) return
  privSaving.value = true
  try {
    await permissionApi.assignPrivs(privGroup.value.id, checkedPrivIds.value)
    ElMessage.success('权限已保存，相关用户的权限立即生效')
    privDialogVisible.value = false
    await load()
  } finally {
    privSaving.value = false
  }
}

async function disableGroup(group: Group) {
  await ElMessageBox.confirm(`确定停用分组「${group.name}」吗？`, '提示', { type: 'warning' })
  await permissionApi.disableGroup(group.id)
  ElMessage.success('分组已停用')
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">权限分组</div>
        <div class="muted">禅道式授权：用户属于分组，分组拥有权限点，多分组取并集</div>
      </div>
      <el-button v-priv="{ module: 'group', method: 'create' }" type="primary" @click="openCreate">
        新增分组
      </el-button>
    </div>

    <div class="page-card">
      <el-table v-loading="loading" :data="groups" stripe>
        <el-table-column prop="code" label="编码" width="120" />
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column prop="role" label="角色标签" width="110" />
        <el-table-column label="数据范围" width="110">
          <template #default="{ row }">
            <el-tag :type="row.dataScope === 'ALL' ? 'danger' : row.dataScope === 'OWN' ? 'warning' : 'info'">
              {{ row.dataScope }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="受限" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.limited" type="danger" effect="plain">是</el-tag>
            <span v-else class="muted">否</span>
          </template>
        </el-table-column>
        <el-table-column prop="userCount" label="用户数" width="90" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-priv="{ module: 'group', method: 'edit' }" link type="primary" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button v-priv="{ module: 'group', method: 'assign-priv' }" link type="primary" @click="openPriv(row)">
              分配权限
            </el-button>
            <el-button
              v-priv="{ module: 'group', method: 'delete' }"
              link
              type="danger"
              :disabled="row.userCount > 0"
              @click="disableGroup(row)"
            >
              停用
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分组' : '新增分组'" width="520px">
      <el-form label-width="90px">
        <el-form-item label="分组编码">
          <el-input v-model="form.code" placeholder="如 COURSE_OPERATOR" />
        </el-form-item>
        <el-form-item label="分组名称">
          <el-input v-model="form.name" placeholder="如 课程运营" />
        </el-form-item>
        <el-form-item label="角色标签">
          <el-input v-model="form.role" placeholder="仅展示，不参与鉴权" />
        </el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="form.dataScope" style="width: 100%">
            <el-option label="ALL - 全部数据" value="ALL" />
            <el-option label="OWN - 本人负责的数据" value="OWN" />
            <el-option label="SELF - 仅本人数据" value="SELF" />
          </el-select>
        </el-form-item>
        <el-form-item label="受限分组">
          <el-switch v-model="form.limited" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveGroup">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="privDialogVisible"
      :title="`分配权限 · ${privGroup?.name || ''}`"
      width="820px"
      top="6vh"
    >
      <div class="muted" style="margin-bottom: 14px">
        VIEW 控制菜单与页面入口，ACTION 控制按钮和写接口。保存后整组覆盖，并递增用户权限版本。
      </div>
      <el-checkbox-group v-model="checkedPrivIds">
        <div v-for="module in privModules" :key="module.module" class="priv-module">
          <div class="priv-module-title">{{ module.label }}（{{ module.module }}）</div>
          <el-checkbox
            v-for="priv in module.items"
            :key="priv.id"
            :value="priv.id"
            class="priv-item"
          >
            {{ priv.name }}
            <el-tag size="small" :type="priv.type === 'VIEW' ? 'success' : 'warning'" style="margin-left: 4px">
              {{ priv.type }}
            </el-tag>
          </el-checkbox>
        </div>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="privDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="privSaving" @click="savePrivs">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>
