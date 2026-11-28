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

async function logout() {
  await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
  await store.logout()
  await router.replace('/login')
}

async function handleCommand(command: string) {
  if (command === 'home') {
    await router.push('/')
    return
  }
  if (command === 'logout') {
    await logout()
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
        <div class="aside-footer-line">Spring Boot 4.2 · Vue 3</div>
        <div class="aside-footer-line">基础版 · 模块 7：院校与看板</div>
      </div>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div class="header-crumb">
          <span class="crumb-root">管理后台</span>
          <span class="crumb-sep">/</span>
          <span class="crumb-current">{{ currentTitle }}</span>
        </div>
        <div class="header-actions">
          <router-link to="/" class="front-link">返回前台</router-link>
          <el-button text class="logout-link" @click="logout">退出登录</el-button>
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
  background: #0f172a;
  display: flex;
  flex-direction: column;
}

.brand {
  height: 68px;
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
  border-radius: 10px;
  background: linear-gradient(135deg, var(--brand), var(--accent));
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
  box-shadow: 0 6px 14px rgba(230, 0, 18, 0.3);
}

.brand-title {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 1px;
}

.brand-sub {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 1px;
}

.admin-menu {
  border-right: none;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px;
}

.admin-menu::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.16);
  background-clip: content-box;
}

.admin-menu :deep(.el-menu-item) {
  height: 42px;
  line-height: 42px;
  border-radius: 8px;
  margin-bottom: 2px;
  font-size: 13.5px;
  transition:
    background-color 0.18s ease,
    color 0.18s ease;
}

.admin-menu :deep(.el-menu-item .el-icon) {
  color: #94a3b8;
  font-size: 16px;
}

.admin-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, #e60012, #c90010);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 8px 18px rgba(230, 0, 18, 0.28);
}

.admin-menu :deep(.el-menu-item.is-active .el-icon) {
  color: #fff;
}

.aside-footer {
  padding: 14px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.aside-footer-line {
  font-size: 11px;
  line-height: 1.7;
  color: #64748b;
}

.admin-header {
  height: 64px;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: saturate(180%) blur(12px);
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.header-crumb {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 13px;
}

.crumb-root {
  color: var(--text-faint);
}

.crumb-sep {
  color: #cbd5e1;
  font-size: 12px;
}

.crumb-current {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-strong);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.front-link {
  padding: 6px 14px;
  border-radius: 8px;
  border: 1px solid var(--brand-line);
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 13px;
  font-weight: 500;
  transition:
    background-color 0.18s ease,
    border-color 0.18s ease;
}

.front-link:hover {
  background: #fce6e8;
  border-color: #ee4d58;
}

.logout-link {
  color: var(--text-muted);
  font-size: 13px;
}

.logout-link:hover {
  color: var(--brand);
  background: var(--brand-soft);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 10px 5px 6px;
  border-radius: 10px;
  cursor: pointer;
  transition: background-color 0.18s ease;
}

.user-chip:hover {
  background: #f8fafc;
}

.user-chip :deep(.el-avatar) {
  flex-shrink: 0;
  font-weight: 600;
  box-shadow: 0 4px 10px rgba(230, 0, 18, 0.24);
}

.user-name {
  font-size: 13.5px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--text-strong);
}

.user-chip .muted {
  font-size: 11px;
  line-height: 1.3;
}

.admin-main {
  padding: 24px;
  background: var(--bg-page);
  overflow-y: auto;
}
</style>
