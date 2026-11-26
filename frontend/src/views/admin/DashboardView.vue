<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { systemApi } from '@/api/system'
import type { RouteCheck } from '@/types'

const store = useAuthStore()
const readRoute = ref<RouteCheck | null>(null)
const masterRoute = ref<RouteCheck | null>(null)
const checking = ref(false)

const moduleCount = computed(() => Object.keys(store.rights).length)
const privCount = computed(() => Object.values(store.rights).reduce((sum, methods) => sum + methods.length, 0))

async function checkRoutes() {
  checking.value = true
  try {
    const [read, master] = await Promise.all([systemApi.readRoute(), systemApi.masterRoute()])
    readRoute.value = read.data.data
    masterRoute.value = master.data.data
    ElMessage.success('读写分离路由检查完成')
  } finally {
    checking.value = false
  }
}
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">系统概览</div>
        <div class="muted">当前为「系统底座」模块：认证、禅道式权限、读写分离</div>
      </div>
      <el-button type="primary" :loading="checking" @click="checkRoutes">检查数据库路由</el-button>
    </div>

    <el-row :gutter="16">
      <el-col :span="6">
        <div class="page-card stat-card">
          <div class="stat-label">当前用户</div>
          <div class="stat-value">{{ store.displayName }}</div>
          <div class="muted">{{ store.me?.user.account }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="page-card stat-card">
          <div class="stat-label">权限分组</div>
          <div class="stat-value">{{ store.me?.user.groups.join(' + ') }}</div>
          <div class="muted">多分组权限取并集</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="page-card stat-card">
          <div class="stat-label">有效权限</div>
          <div class="stat-value">{{ privCount }}</div>
          <div class="muted">覆盖 {{ moduleCount }} 个模块</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="page-card stat-card">
          <div class="stat-label">数据范围</div>
          <div class="stat-value">{{ store.me?.dataScope }}</div>
          <div class="muted">{{ store.me?.limited ? '受限分组' : '非受限分组' }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <div class="page-card">
          <div class="page-title" style="font-size: 16px; margin-bottom: 12px">读写分离</div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="主库（写）">
              <el-tag type="danger" effect="plain">{{ masterRoute?.database || '尚未检查' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="从库（读）">
              <el-tag type="success" effect="plain">{{ readRoute?.database || '尚未检查' }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <div class="muted" style="margin-top: 12px; line-height: 1.8">
            写操作默认走主库；查询服务使用只读事务走从库；登录、权限、支付等强一致读强制主库。
          </div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="page-card">
          <div class="page-title" style="font-size: 16px; margin-bottom: 12px">当前菜单权限</div>
          <el-table :data="store.menus" size="small">
            <el-table-column prop="name" label="功能" />
            <el-table-column prop="module" label="模块" width="110" />
            <el-table-column prop="method" label="方法" width="100" />
            <el-table-column prop="path" label="路径" />
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.stat-card {
  min-height: 120px;
}

.stat-label {
  color: #6b7280;
  font-size: 13px;
  margin-bottom: 12px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 6px;
}
</style>
