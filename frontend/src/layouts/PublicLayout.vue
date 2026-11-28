<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const store = useAuthStore()

const hasAdminMenu = computed(() => store.menus.some((menu) => menu.path.startsWith('/admin')))

const navs = [
  { path: '/', label: '首页' },
  { path: '/courses', label: '课程中心' },
  { path: '/universities', label: '院校库' },
  { path: '/practice', label: '题库' },
  { path: '/seckill', label: '秒杀专区' },
  { path: '/teachers', label: '名师团队' },
  { path: '/articles', label: '考研资讯' },
  { path: '/materials', label: '资料下载' },
]

function isActive(path: string) {
  return path === '/' ? route.path === '/' : route.path.startsWith(path)
}

async function logout() {
  await store.logout()
  await router.replace('/')
}
</script>

<template>
  <div class="public-shell">
    <header class="public-header">
      <div class="header-inner">
        <router-link to="/" class="public-brand">
          <span class="brand-mark">智</span>
          <span>
            <strong>智学考研</strong>
            <small>SMART LEARNING</small>
          </span>
        </router-link>
        <nav class="public-nav">
          <router-link
            v-for="nav in navs"
            :key="nav.path"
            :to="nav.path"
            :class="{ active: isActive(nav.path) }"
          >
            {{ nav.label }}
          </router-link>
        </nav>
        <div class="header-actions">
          <template v-if="store.isLoggedIn">
            <router-link to="/user?tab=orders" class="admin-link">我的中心</router-link>
            <router-link
              v-if="hasAdminMenu"
              :to="store.hasPriv('admin', 'access') ? '/admin/dashboard' : '/admin/courses'"
              class="admin-link"
            >后台管理</router-link>
            <el-button text @click="logout">退出</el-button>
          </template>
          <template v-else>
            <router-link to="/login" class="login-link">登录 / 注册</router-link>
          </template>
        </div>
      </div>
    </header>

    <main class="public-main">
      <router-view />
    </main>

    <footer class="public-footer">
      <div class="footer-inner">
        <div class="footer-brand">
          <strong>智学考研</strong>
          <p>课程 · 题库 · 院校 · 资料，一站式考研备考平台</p>
        </div>
        <nav class="footer-links">
          <router-link to="/courses">课程中心</router-link>
          <router-link to="/practice">题库练习</router-link>
          <router-link to="/universities">院校库</router-link>
          <router-link to="/articles">考研资讯</router-link>
          <router-link to="/materials">资料下载</router-link>
        </nav>
        <div class="footer-meta">
          <span>Spring Boot 4.2 · Vue 3</span>
          <span>基础版 · 模块 7：院校与看板</span>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.public-shell {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-page);
}

.public-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: saturate(180%) blur(14px);
  border-bottom: 1px solid var(--line);
}

.header-inner {
  max-width: 1180px;
  margin: 0 auto;
  height: 68px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 26px;
}

.public-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-strong);
}

.brand-mark {
  width: 38px;
  height: 38px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 19px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--brand), var(--accent));
  box-shadow: 0 6px 14px rgba(230, 0, 18, 0.22);
}

.public-brand strong {
  display: block;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.25;
  letter-spacing: 1px;
}

.public-brand small {
  display: block;
  font-size: 9.5px;
  line-height: 1.3;
  color: var(--text-faint);
  letter-spacing: 2.2px;
}

.public-nav {
  display: flex;
  gap: 2px;
  flex: 1;
}

.public-nav a {
  padding: 8px 13px;
  border-radius: 8px;
  color: #4b5563;
  font-size: 14px;
  white-space: nowrap;
  transition:
    color 0.18s ease,
    background-color 0.18s ease;
}

.public-nav a:hover {
  color: var(--brand);
  background: var(--brand-soft);
}

.public-nav a.active {
  color: var(--brand);
  background: var(--brand-soft);
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.admin-link {
  padding: 6px 12px;
  border-radius: 8px;
  color: var(--brand);
  font-weight: 600;
  font-size: 13px;
  transition: background-color 0.18s ease;
}

.admin-link:hover {
  background: var(--brand-soft);
}

.login-link {
  padding: 8px 18px;
  border-radius: 999px;
  background: var(--brand);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  box-shadow: 0 6px 14px rgba(230, 0, 18, 0.18);
  transition: background-color 0.18s ease;
}

.login-link:hover {
  background: var(--brand-hover);
}

.header-actions :deep(.el-button.is-text) {
  color: var(--text-muted);
  font-size: 13px;
}

.header-actions :deep(.el-button.is-text:hover) {
  color: var(--brand);
  background: var(--brand-soft);
}

.public-main {
  flex: 1;
  width: 100%;
}

.public-footer {
  margin-top: 56px;
  color: #cbd5e1;
  background: #0f172a;
  border-top: 3px solid var(--brand);
}

.footer-inner {
  max-width: 1180px;
  margin: 0 auto;
  padding: 34px 24px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 40px;
  flex-wrap: wrap;
}

.footer-brand strong {
  color: #fff;
  font-size: 18px;
  letter-spacing: 0.5px;
}

.footer-brand p {
  margin: 8px 0 0;
  font-size: 13px;
  color: #94a3b8;
}

.footer-links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 22px;
  padding-top: 3px;
  font-size: 13px;
}

.footer-links a {
  color: #cbd5e1;
  transition: color 0.18s ease;
}

.footer-links a:hover {
  color: #fff;
}

.footer-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 3px;
  font-size: 12px;
  color: #7b8aa3;
  text-align: right;
}

@media (max-width: 1240px) {
  .header-inner {
    gap: 18px;
  }

  .public-nav a {
    padding: 8px 10px;
  }
}
</style>
