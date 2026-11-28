<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const store = useAuthStore()

const tab = ref('account')
const mode = ref<'login' | 'register'>('login')
const accountForm = reactive({ account: 'admin', password: 'Admin@123' })
const registerForm = reactive({ target: '', code: '', password: '', nickname: '' })
const phonePattern = /^1[3-9]\d{9}$/
const emailPattern = /^[^@\s]+@[^@\s]+\.[^@\s]+$/
const smsForm = reactive({ phone: '13800000000', code: '' })
const emailForm = reactive({ email: 'student@example.com', code: '' })
const loginCaptcha = reactive({ id: '', image: '', code: '' })
const sending = ref(false)
const countdown = ref(0)
let timer: number | undefined

async function loadCaptcha() {
  try {
    const response = await authApi.captcha()
    const data = response.data.data
    loginCaptcha.id = data.id
    loginCaptcha.image = data.image
    loginCaptcha.code = ''
  } catch {
    // 错误提示由 axios 拦截器统一处理
  }
}

function ensureCaptcha() {
  if (!loginCaptcha.code.trim()) {
    ElMessage.warning('请输入图形验证码')
    return false
  }
  return true
}

function captchaPayload() {
  return { id: loginCaptcha.id, code: loginCaptcha.code }
}

watch(mode, (value) => {
  if (value === 'login' && !loginCaptcha.id) {
    loadCaptcha()
  }
})

onMounted(loadCaptcha)

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
  if (sending.value) return
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
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    sending.value = false
  }
}

async function sendEmail() {
  if (sending.value) return
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
  } catch {
    // 错误提示由 axios 拦截器统一处理
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
  if (store.loading) return
  if (!ensureCaptcha()) return
  try {
    await store.loginByAccount(accountForm.account, accountForm.password, captchaPayload())
    ElMessage.success('登录成功')
    await afterLogin()
  } catch {
    // 验证码校验后即作废，失败后换一张
    await loadCaptcha()
  }
}

async function submitSms() {
  if (store.loading) return
  if (!ensureCaptcha()) return
  try {
    await store.loginBySms(smsForm.phone, smsForm.code, captchaPayload())
    ElMessage.success('登录成功')
    await afterLogin()
  } catch {
    await loadCaptcha()
  }
}

async function submitEmail() {
  if (store.loading) return
  if (!ensureCaptcha()) return
  try {
    await store.loginByEmail(emailForm.email, emailForm.code, captchaPayload())
    ElMessage.success('登录成功')
    await afterLogin()
  } catch {
    await loadCaptcha()
  }
}

function registerChannel(): 'sms' | 'email' | '' {
  const target = registerForm.target.trim()
  if (phonePattern.test(target)) return 'sms'
  if (emailPattern.test(target)) return 'email'
  return ''
}

async function sendRegisterCode() {
  if (sending.value) return
  const target = registerForm.target.trim()
  const channel = registerChannel()
  if (!channel) {
    ElMessage.warning('请输入正确的手机号或邮箱')
    return
  }
  sending.value = true
  try {
    const response = channel === 'sms'
      ? await authApi.sendSmsCode(target)
      : await authApi.sendEmailCode(target)
    startCountdown()
    ElMessage.success(response.data.data.mock
      ? (channel === 'sms' ? '验证码已发送（模拟短信，请查看后端日志）' : '验证码已发送（开发环境请查看后端日志）')
      : '验证码已发送，请查收')
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    sending.value = false
  }
}

async function submitRegister() {
  if (store.loading) return
  const target = registerForm.target.trim()
  if (!registerChannel()) {
    ElMessage.warning('请输入正确的手机号或邮箱')
    return
  }
  if (!registerForm.code) {
    ElMessage.warning('请输入验证码')
    return
  }
  if (registerForm.password && registerForm.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  try {
    await store.register({
      target,
      code: registerForm.code,
      password: registerForm.password || undefined,
      nickname: registerForm.nickname || undefined,
    })
    ElMessage.success('注册成功')
    await afterLogin()
  } catch {
    // 错误提示由 axios 拦截器统一处理
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-shell">
      <aside class="login-visual">
        <div class="visual-orb visual-orb-a"></div>
        <div class="visual-orb visual-orb-b"></div>
        <div class="visual-brand">
          <span class="visual-mark">智</span>
          <span>
            <strong>智学考研</strong>
            <small>SMART LEARNING</small>
          </span>
        </div>
        <h1>陪你走完<br />备考的每一公里</h1>
        <p class="visual-sub">课程、题库、院校、资料，一站式考研备考平台。</p>
        <ul class="visual-points">
          <li>
            <el-icon><Check /></el-icon>
            <span>名师阶段课程，讲练结合</span>
          </li>
          <li>
            <el-icon><Check /></el-icon>
            <span>章节练习与限时模考</span>
          </li>
          <li>
            <el-icon><Check /></el-icon>
            <span>院校专业目录与历年复试线</span>
          </li>
        </ul>
        <div class="visual-foot">2027 考研 · 现在开始</div>
      </aside>

      <section class="login-panel">
        <div class="panel-head">
          <div>
            <h2>{{ mode === 'login' ? '欢迎回来' : '创建账号' }}</h2>
            <p>{{ mode === 'login' ? '登录后继续你的备考计划' : '验证码注册，注册成功自动登录' }}</p>
          </div>
          <router-link to="/" class="back-home">返回首页</router-link>
        </div>

        <el-tabs v-if="mode === 'login'" v-model="tab">
        <el-tab-pane label="账号登录" name="account">
          <el-form label-position="top" @keyup.enter="submitAccount">
            <el-form-item label="账号">
              <el-input v-model="accountForm.account" placeholder="请输入账号" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="accountForm.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
            <el-form-item label="图形验证码">
              <div class="captcha-row">
                <el-input v-model="loginCaptcha.code" maxlength="4" placeholder="输入图片中的字符" />
                <img
                  v-if="loginCaptcha.image"
                  class="captcha-img"
                  :src="loginCaptcha.image"
                  :data-captcha-id="loginCaptcha.id"
                  alt="图形验证码"
                  title="点击刷新"
                  @click="loadCaptcha"
                />
              </div>
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
            <el-form-item label="图形验证码">
              <div class="captcha-row">
                <el-input v-model="loginCaptcha.code" maxlength="4" placeholder="输入图片中的字符" />
                <img
                  v-if="loginCaptcha.image"
                  class="captcha-img"
                  :src="loginCaptcha.image"
                  :data-captcha-id="loginCaptcha.id"
                  alt="图形验证码"
                  title="点击刷新"
                  @click="loadCaptcha"
                />
              </div>
            </el-form-item>
            <el-button type="primary" style="width: 100%" :loading="store.loading" @click="submitSms">
              登录
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
            <el-form-item label="图形验证码">
              <div class="captcha-row">
                <el-input v-model="loginCaptcha.code" maxlength="4" placeholder="输入图片中的字符" />
                <img
                  v-if="loginCaptcha.image"
                  class="captcha-img"
                  :src="loginCaptcha.image"
                  :data-captcha-id="loginCaptcha.id"
                  alt="图形验证码"
                  title="点击刷新"
                  @click="loadCaptcha"
                />
              </div>
            </el-form-item>
            <el-button type="primary" style="width: 100%" :loading="store.loading" @click="submitEmail">
              登录
            </el-button>
          </el-form>
        </el-tab-pane>
        </el-tabs>

        <div v-if="mode === 'login'" class="switch-row">
          还没有账号？<a @click="mode = 'register'">立即注册</a>
        </div>

        <template v-else>
          <el-form label-position="top" @keyup.enter="submitRegister">
            <el-form-item label="手机号 / 邮箱">
              <el-input v-model="registerForm.target" placeholder="请输入手机号或邮箱，用于接收验证码" />
            </el-form-item>
            <el-form-item label="验证码">
              <div style="display: flex; gap: 10px; width: 100%">
                <el-input v-model="registerForm.code" maxlength="6" placeholder="6 位验证码" />
                <el-button :disabled="countdown > 0" :loading="sending" @click="sendRegisterCode">
                  {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item label="密码（可选）">
              <el-input
                v-model="registerForm.password"
                type="password"
                show-password
                placeholder="至少 6 位，设置后可用密码登录"
              />
            </el-form-item>
            <el-form-item label="昵称（可选）">
              <el-input v-model="registerForm.nickname" maxlength="64" placeholder="不填则自动生成" />
            </el-form-item>
            <el-button type="primary" style="width: 100%" :loading="store.loading" @click="submitRegister">
              注册并登录
            </el-button>
          </el-form>

          <div class="switch-row">
            已有账号？<a @click="mode = 'login'">返回登录</a>
          </div>
        </template>

        <div v-if="mode === 'login'" class="demo-hint">
          演示账号：admin / Admin@123<br />
          验证码登录为无密码登录；开发环境验证码输出到后端日志。
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  background:
    radial-gradient(circle at 12% 18%, rgba(230, 0, 18, 0.1), transparent 34%),
    radial-gradient(circle at 88% 82%, rgba(255, 122, 0, 0.12), transparent 36%),
    #f3f5f9;
}

.login-shell {
  width: min(980px, 100%);
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  background: #fff;
  border-radius: 26px;
  overflow: hidden;
  box-shadow: 0 30px 80px rgba(15, 23, 42, 0.16);
}

.login-visual {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 46px 44px;
  color: #fff;
  background: linear-gradient(150deg, #7f0710 0%, #c4000f 42%, #ff7a00 100%);
  overflow: hidden;
}

.visual-orb {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.visual-orb-a {
  width: 320px;
  height: 320px;
  right: -110px;
  top: -90px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.22), rgba(255, 255, 255, 0) 70%);
}

.visual-orb-b {
  width: 360px;
  height: 360px;
  left: -140px;
  bottom: -160px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0) 70%);
}

.visual-brand {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
}

.visual-mark {
  width: 40px;
  height: 40px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.28);
}

.visual-brand strong {
  display: block;
  font-size: 19px;
  letter-spacing: 1px;
  line-height: 1.25;
}

.visual-brand small {
  display: block;
  font-size: 9.5px;
  letter-spacing: 2.4px;
  opacity: 0.7;
}

.login-visual h1 {
  position: relative;
  margin: 62px 0 16px;
  font-size: 38px;
  line-height: 1.32;
  letter-spacing: 1px;
  font-weight: 700;
}

.visual-sub {
  position: relative;
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  opacity: 0.86;
}

.visual-points {
  position: relative;
  list-style: none;
  margin: 32px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  font-size: 14px;
}

.visual-points li {
  display: flex;
  align-items: center;
  gap: 10px;
}

.visual-points .el-icon {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.18);
  font-size: 13px;
  flex-shrink: 0;
}

.visual-foot {
  position: relative;
  margin-top: auto;
  padding-top: 28px;
  font-size: 11px;
  letter-spacing: 2.4px;
  opacity: 0.66;
}

.login-panel {
  display: flex;
  flex-direction: column;
  padding: 46px 48px 36px;
  background: #fff;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-head h2 {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: var(--text-strong);
}

.panel-head p {
  margin: 0;
  font-size: 13px;
  color: var(--text-muted);
}

.back-home {
  font-size: 12px;
  color: var(--text-faint);
  padding: 5px 10px;
  border-radius: 999px;
  border: 1px solid var(--line);
  transition:
    color 0.18s ease,
    border-color 0.18s ease,
    background-color 0.18s ease;
  white-space: nowrap;
}

.back-home:hover {
  color: var(--brand);
  border-color: var(--brand-line);
  background: var(--brand-soft);
}

.login-panel :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.login-panel :deep(.el-tabs__item) {
  font-size: 14px;
  padding: 0 18px;
}

.login-panel :deep(.el-input__wrapper) {
  padding: 2px 12px;
}

.login-panel :deep(.el-button--primary) {
  height: 44px;
  font-size: 15px;
  letter-spacing: 4px;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.captcha-img {
  width: 120px;
  height: 40px;
  flex-shrink: 0;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #f8fafc;
  cursor: pointer;
  transition: border-color 0.18s ease;
}

.captcha-img:hover {
  border-color: var(--brand-line);
}

.switch-row {
  margin-top: 16px;
  text-align: center;
  font-size: 12.5px;
  color: var(--text-muted);
}

.switch-row a {
  margin-left: 4px;
  color: var(--brand);
  font-weight: 600;
  cursor: pointer;
}

.switch-row a:hover {
  text-decoration: underline;
}

.demo-hint {
  margin-top: auto;
  padding-top: 20px;
  border-top: 1px dashed var(--line);
  font-size: 12px;
  line-height: 1.9;
  color: var(--text-faint);
}

@media (max-width: 880px) {
  .login-shell {
    grid-template-columns: 1fr;
    width: min(440px, 100%);
  }

  .login-visual {
    display: none;
  }

  .login-panel {
    padding: 32px 28px 26px;
  }
}
</style>
