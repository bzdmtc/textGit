<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  Setting,
  User,
  Connection,
  Document,
  Management,
  Reading,
  Folder,
  Picture,
  List,
  RefreshRight,
  Ticket,
  Timer,
  ChatDotRound,
  EditPen,
  Tickets,
  School,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const store = useAuthStore()

const iconMap: Record<string, unknown> = {
  Setting,
  User,
  Connection,
  Document,
  Management,
  Reading,
  Folder,
  Picture,
  List,
  RefreshRight,
  Ticket,
  Timer,
  ChatDotRound,
  EditPen,
  Tickets,
  School,
}

const adminMenus = computed(() => store.menus.filter((menu) => menu.path.startsWith('/admin')))

const currentTitle = computed(() => {
  const item = adminMenus.value.find((menu) => menu.path === route.path)
  return item?.name || '管理后台'
})

async function handleCommand(command: string) {
  if (command === 'home') {
    await router.push('/')
    return
  }
  if (command === 'logout') {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
    await store.logout()
    await router.replace('/login')
  }
}
</script>

<template>
  <el-container style="height: 100%">
    <el-aside width="232px" class="admin-aside">
      <div class="brand">
        <span class="brand-mark">智</span>
        <div>
          <div class="brand-title">智学考研</div>
          <div class="brand-sub">管理后台</div>
        </div>
      </div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#111827"
        text-color="#cbd5e1"
        active-text-color="#ffffff"
        class="admin-menu"
      >
        <el-menu-item v-for="menu in adminMenus" :key="menu.path" :index="menu.path">
          <el-icon>
            <component :is="iconMap[menu.icon || ''] || Setting" />
          </el-icon>
          <span>{{ menu.name }}</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-footer">
        <div class="muted" style="color: #94a3b8">基础版 · 模块 7：院校与看板</div>
      </div>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div>
          <div class="header-title">{{ currentTitle }}</div>
          <div class="muted">Spring Boot 4.2 · 禅道式权限 · 主从读写分离</div>
        </div>
        <div class="header-actions">
          <router-link to="/" class="front-link">返回前台</router-link>
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-chip">
              <el-avatar :size="32" style="background: #e60012">
                {{ store.displayName.slice(0, 1) }}
              </el-avatar>
              <div>
                <div class="user-name">{{ store.displayName }}</div>
                <div class="muted">{{ store.me?.user.groups.join(' / ') }}</div>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="home">返回前台首页</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-aside {
  background: #111827;
  display: flex;
  flex-direction: column;
}

.brand {
  height: 76px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-mark {
  width: 36px;
  height: 36px;
  border-radius: 9px;
  background: linear-gradient(135deg, #e60012, #ff7a00);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
}

.brand-title {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 1px;
}

.brand-sub {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

.admin-menu {
  border-right: none;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 8px;
}

.admin-menu :deep(.el-menu-item) {
  border-radius: 8px;
  margin-bottom: 4px;
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: #e60012;
}

.aside-footer {
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.admin-header {
  height: 76px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 18px;
}

.front-link {
  padding: 7px 14px;
  border-radius: 8px;
  border: 1px solid #f7b3b8;
  background: #fce6e8;
  color: #e60012;
  font-size: 13px;
  transition: background 0.2s, border-color 0.2s;
}

.front-link:hover {
  background: #f9ccd0;
  border-color: #f28088;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
}

.admin-main {
  padding: 24px;
  background: #f5f6f8;
  overflow-y: auto;
}
</style>
