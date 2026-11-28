// 样式基线/对比截图：游客、学员、管理员三类视角的主要页面
const { chromium } = require('playwright-core')
const path = require('node:path')
const fs = require('node:fs')
const { uiLogin } = require('./lib/auth-helper.cjs')

const base = process.env.UI_BASE || 'http://localhost:5173'
const outputDir = path.resolve(process.env.SHOT_DIR || path.join(__dirname, '..', 'screenshots'))

fs.mkdirSync(outputDir, { recursive: true })

async function login(page, account, password) {
  await uiLogin(page, base, account, password)
}

async function shoot(page, route, file, waitFor) {
  await page.goto(`${base}${route}`, { waitUntil: 'networkidle' })
  if (waitFor) {
    await page.getByText(waitFor, { exact: false }).first().waitFor({ timeout: 10000 }).catch(() => {})
  }
  await page.waitForTimeout(600)
  await page.screenshot({ path: path.join(outputDir, file), fullPage: true })
  console.log('shot', file, page.url())
}

async function main() {
  const browser = await chromium.launch({ channel: 'msedge', headless: true })
  const errors = []

  const guest = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const guestPage = await guest.newPage()
  guestPage.on('pageerror', (e) => errors.push(`guest: ${e.message}`))

  for (const [route, file, waitFor] of [
    ['/', 'style-home.png', '热门课程'],
    ['/courses', 'style-courses.png', '课程中心'],
    ['/courses/1', 'style-course-detail.png', '课程目录'],
    ['/universities', 'style-universities.png', '院校'],
    ['/seckill', 'style-seckill.png', '秒杀'],
    ['/teachers', 'style-teachers.png', '名师团队'],
    ['/articles', 'style-articles.png', '考研资讯'],
    ['/materials', 'style-materials.png', '资料下载'],
  ]) {
    await shoot(guestPage, route, file, waitFor)
  }
  await guestPage.goto(`${base}/login`, { waitUntil: 'networkidle' })
  await guestPage.waitForTimeout(400)
  await guestPage.screenshot({ path: path.join(outputDir, 'style-login.png'), fullPage: true })

  const student = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const studentPage = await student.newPage()
  studentPage.on('pageerror', (e) => errors.push(`student: ${e.message}`))
  await login(studentPage, 'student', 'Student@123')
  for (const [route, file, waitFor] of [
    ['/user?tab=orders', 'style-user-center.png', '我的'],
    ['/practice', 'style-practice.png', '题库'],
    ['/universities/1', 'style-university-detail.png', ''],
  ]) {
    await shoot(studentPage, route, file, waitFor)
  }

  const admin = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const adminPage = await admin.newPage()
  adminPage.on('pageerror', (e) => errors.push(`admin: ${e.message}`))
  await login(adminPage, 'admin', 'Admin@123')
  for (const [route, file, waitFor] of [
    ['/admin/dashboard', 'style-admin-dashboard.png', ''],
    ['/admin/courses', 'style-admin-courses.png', ''],
    ['/admin/users', 'style-admin-users.png', ''],
    ['/admin/groups', 'style-admin-groups.png', ''],
    ['/admin/universities', 'style-admin-universities.png', ''],
  ]) {
    await shoot(adminPage, route, file, waitFor)
  }

  await browser.close()
  if (errors.length) {
    console.log('PAGE ERRORS:')
    for (const error of errors) console.log(' -', error)
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
