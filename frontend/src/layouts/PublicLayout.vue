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
        <div>
          <strong>智学考研</strong>
          <p>课程 · 题库 · 院校 · 资料，一站式考研备考平台</p>
        </div>
        <div class="footer-meta">
          <span>Spring Boot 4.2 · Vue 3</span>
          <span>基础版 · 模块 3：交易</span>
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
  background: #f7f8fa;
}

.public-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid #ebeef5;
}

.header-inner {
  max-width: 1180px;
  margin: 0 auto;
  height: 72px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 32px;
}

.public-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #111827;
}

.brand-mark {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #e60012, #ff7a00);
}

.public-brand strong {
  display: block;
  font-size: 19px;
  letter-spacing: 1px;
}

.public-brand small {
  display: block;
  font-size: 10px;
  color: #9ca3af;
  letter-spacing: 2px;
}

.public-nav {
  display: flex;
  gap: 8px;
  flex: 1;
}

.public-nav a {
  padding: 8px 14px;
  border-radius: 8px;
  color: #4b5563;
  font-size: 14px;
  transition: all 0.2s;
}

.public-nav a:hover,
.public-nav a.active {
  color: #e60012;
  background: #fef2f2;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.login-link,
.admin-link {
  color: #e60012;
  font-weight: 600;
  font-size: 14px;
}

.public-main {
  flex: 1;
  width: 100%;
}

.public-footer {
  background: #111827;
  color: #cbd5e1;
  margin-top: 48px;
}

.footer-inner {
  max-width: 1180px;
  margin: 0 auto;
  padding: 32px 24px;
  display: flex;
  justify-content: space-between;
  gap: 24px;
}

.footer-inner strong {
  color: #fff;
  font-size: 18px;
}

.footer-inner p {
  margin: 8px 0 0;
  font-size: 13px;
  color: #94a3b8;
}

.footer-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
  color: #94a3b8;
  text-align: right;
}
</style>
