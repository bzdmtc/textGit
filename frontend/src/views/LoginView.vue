<script setup lang="ts">
import { onBeforeUnmount, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const store = useAuthStore()

const tab = ref('account')
const accountForm = reactive({ account: 'admin', password: 'Admin@123' })
const smsForm = reactive({ phone: '13800000000', code: '' })
const emailForm = reactive({ email: 'student@example.com', code: '' })
const sending = ref(false)
const countdown = ref(0)
let timer: number | undefined

function startCountdown() {
  countdown.value = 60
  window.clearInterval(timer)
  timer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      window.clearInterval(timer)
      timer = undefined
    }
  }, 1000)
}

onBeforeUnmount(() => window.clearInterval(timer))

async function sendSms() {
  if (!/^1[3-9]\d{9}$/.test(smsForm.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  sending.value = true
  try {
    const response = await authApi.sendSmsCode(smsForm.phone)
    startCountdown()
    ElMessage.success(response.data.data.mock
      ? '验证码已发送（模拟短信，请查看后端日志）'
      : '验证码已发送')
  } finally {
    sending.value = false
  }
}

async function sendEmail() {
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(emailForm.email)) {
    ElMessage.warning('请输入正确的邮箱')
    return
  }
  sending.value = true
  try {
    const response = await authApi.sendEmailCode(emailForm.email)
    startCountdown()
    ElMessage.success(response.data.data.mock
      ? '验证码已发送（开发环境请查看后端日志）'
      : '验证码已发送，请查收邮件')
  } finally {
    sending.value = false
  }
}

async function afterLogin() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  if (redirect) {
    await router.replace(redirect)
    return
  }
  if (store.hasPriv('admin', 'access')) {
    await router.replace('/admin/dashboard')
  } else if (store.hasPriv('course', 'browse')) {
    await router.replace('/admin/courses')
  } else {
    await router.replace('/')
  }
}

async function submitAccount() {
  await store.loginByAccount(accountForm.account, accountForm.password)
  ElMessage.success('登录成功')
  await afterLogin()
}

async function submitSms() {
  await store.loginBySms(smsForm.phone, smsForm.code)
  ElMessage.success('登录成功')
  await afterLogin()
}

async function submitEmail() {
  await store.loginByEmail(emailForm.email, emailForm.code)
  ElMessage.success('登录成功')
  await afterLogin()
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">智学考研</div>
      <div class="login-subtitle">课程 · 题库 · 院校 · 资料，一站式考研备考平台</div>

      <el-tabs v-model="tab">
        <el-tab-pane label="账号登录" name="account">
          <el-form label-position="top" @keyup.enter="submitAccount">
            <el-form-item label="账号">
              <el-input v-model="accountForm.account" placeholder="请输入账号" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="accountForm.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
            <el-button type="primary" style="width: 100%" :loading="store.loading" @click="submitAccount">
              登录
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="手机号登录" name="sms">
          <el-form label-position="top" @keyup.enter="submitSms">
            <el-form-item label="手机号">
              <el-input v-model="smsForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="验证码">
              <div style="display: flex; gap: 10px; width: 100%">
                <el-input v-model="smsForm.code" maxlength="6" placeholder="6 位验证码" />
                <el-button :disabled="countdown > 0" :loading="sending" @click="sendSms">
                  {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-button type="primary" style="width: 100%" :loading="store.loading" @click="submitSms">
              登录 / 自动注册
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="邮箱登录" name="email">
          <el-form label-position="top" @keyup.enter="submitEmail">
            <el-form-item label="邮箱">
              <el-input v-model="emailForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="验证码">
              <div style="display: flex; gap: 10px; width: 100%">
                <el-input v-model="emailForm.code" maxlength="6" placeholder="6 位验证码" />
                <el-button :disabled="countdown > 0" :loading="sending" @click="sendEmail">
                  {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-button type="primary" style="width: 100%" :loading="store.loading" @click="submitEmail">
              登录 / 自动注册
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="muted" style="margin-top: 18px; line-height: 1.8">
        演示账号：admin / Admin@123<br />
        验证码登录为无密码登录；开发环境验证码输出到后端日志。
      </div>
    </div>
  </div>
</template>
