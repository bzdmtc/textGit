// 模块 5 学习 UI 冒烟：购课 → 学习页标记完成/笔记/收藏/评价 → 管理员审核 → 课程详情展示
const { chromium } = require('playwright-core')
const path = require('node:path')
const fs = require('node:fs')
const { apiLogin, uiLogin } = require('./lib/auth-helper.cjs')

const base = process.env.UI_BASE || 'http://localhost:5173'
const apiBase = 'http://localhost:8080/api/v1'
const outputDir = path.join(__dirname, '..', 'screenshots')
fs.mkdirSync(outputDir, { recursive: true })

async function api(method, url, body, token) {
  const response = await fetch(`${apiBase}${url}`, {
    method,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  })
  const data = await response.json()
  if (!response.ok) throw new Error(`${method} ${url} -> ${response.status}: ${JSON.stringify(data)}`)
  return data.data
}

async function ensureEnrolled() {
  const login = await apiLogin('student', 'Student@123')
  const token = login.accessToken
  const status = await api('GET', '/my/purchase-status?courseId=1', null, token)
  if (status.enrolled) return
  const order = await api('POST', '/orders', { courseIds: [1] }, token)
  await api('POST', `/payments/${order.orderNo}/pay`, { channel: 'MOCK_ALIPAY' }, token)
}

async function login(page, account, password) {
  await uiLogin(page, base, account, password)
}

async function main() {
  await ensureEnrolled()
  const browser = await chromium.launch({ channel: 'msedge', headless: true })
  const errors = []
  const studentContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const studentPage = await studentContext.newPage()
  const adminContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const adminPage = await adminContext.newPage()
  const guestPage = await browser.newPage({ viewport: { width: 1440, height: 960 } })

  for (const [label, target] of [['student', studentPage], ['admin', adminPage], ['guest', guestPage]]) {
    target.on('pageerror', (error) => errors.push(`${label} pageerror: ${error.message}`))
    target.on('console', (message) => {
      if (message.type() === 'error' && !message.text().includes('Failed to load resource')) {
        errors.push(`${label} console: ${message.text()}`)
      }
    })
  }

  function check(label, condition, detail) {
    console.log(`${condition ? 'OK  ' : 'FAIL'} ${label}${detail ? ' :: ' + detail : ''}`)
    if (!condition) process.exitCode = 1
  }

  // 我的课程 → 继续学习
  await login(studentPage, 'student', 'Student@123')
  await studentPage.goto(`${base}/user?tab=courses`, { waitUntil: 'networkidle' })
  await studentPage.waitForTimeout(600)
  check('我的课程展示已购课程', (await studentPage.locator('body').innerText()).includes('考研政治强化提升班'))
  await studentPage.getByRole('button', { name: '继续学习' }).first().click()
  await studentPage.waitForURL(/\/learn\/1/, { timeout: 15000 })
  await studentPage.waitForSelector('text=课程目录', { timeout: 15000 })
  check('学习页加载视频与目录', (await studentPage.locator('video').count()) === 1
    && (await studentPage.locator('body').innerText()).includes('学习笔记'))
  await studentPage.screenshot({ path: path.join(outputDir, 'learn-page.png'), fullPage: true })

  // 标记完成与进度
  await studentPage.getByRole('button', { name: '标记完成' }).first().click()
  await studentPage.waitForTimeout(1200)
  check('标记完成更新课程进度', (await studentPage.locator('body').innerText()).includes('本节已完成'))

  // 笔记
  await studentPage.getByPlaceholder('记录本节的重点、疑问或解题思路…').fill('UI 自动化笔记：物质决定意识')
  await studentPage.getByRole('button', { name: '保存笔记' }).click()
  await studentPage.waitForTimeout(800)
  check('新增笔记可见', (await studentPage.locator('body').innerText()).includes('UI 自动化笔记'))

  // 收藏
  await studentPage.getByRole('button', { name: '☆ 收藏课程' }).click()
  await studentPage.waitForTimeout(800)
  check('收藏课程成功', (await studentPage.locator('body').innerText()).includes('★ 已收藏'))

  // 评价
  await studentPage.getByRole('tab', { name: '课程评价' }).click()
  await studentPage.getByPlaceholder('说说这门课对你的帮助…').fill('UI 自动化评价：讲解清晰，进度保存正常。')
  await studentPage.getByRole('button', { name: '提交评价' }).click()
  await studentPage.waitForTimeout(1000)
  check('评价提交后显示待审核', (await studentPage.locator('body').innerText()).includes('待审核'))
  await studentPage.screenshot({ path: path.join(outputDir, 'learn-review.png'), fullPage: true })

  // 切换课时
  await studentPage.getByText('唯物论核心考点', { exact: true }).click()
  await studentPage.waitForURL(/\/learn\/1\/2/, { timeout: 15000 })
  check('切换课时 URL 与标题更新', (await studentPage.locator('body').innerText()).includes('唯物论核心考点'))

  // 管理员审核
  await login(adminPage, 'admin', 'Admin@123')
  await adminPage.goto(`${base}/admin/reviews`, { waitUntil: 'networkidle' })
  await adminPage.waitForTimeout(800)
  const adminText = await adminPage.locator('body').innerText()
  check('后台评价页展示待审核评价', adminText.includes('评价审核') && adminText.includes('UI 自动化评价'))
  await adminPage.getByRole('button', { name: '通过' }).first().click()
  await adminPage.getByRole('button', { name: '确定' }).click()
  await adminPage.waitForTimeout(1200)
  check('管理员通过评价', (await adminPage.locator('body').innerText()).includes('已通过'))
  await adminPage.screenshot({ path: path.join(outputDir, 'learn-admin-review.png'), fullPage: true })

  // 游客课程详情看到评价
  await guestPage.goto(`${base}/courses/1`, { waitUntil: 'networkidle' })
  await guestPage.waitForTimeout(800)
  const detailText = await guestPage.locator('body').innerText()
  check('课程详情展示已通过评价', detailText.includes('学员评价') && detailText.includes('UI 自动化评价'))
  check('课程详情展示平均评分', detailText.includes('平均 5.0 分') || detailText.includes('5.0'))
  await guestPage.screenshot({ path: path.join(outputDir, 'learn-course-reviews.png'), fullPage: true })

  check('浏览器无前端错误', errors.length === 0, errors.slice(0, 3).join(' | '))
  await browser.close()
  if (process.exitCode) {
    console.error('UI smoke failed')
  } else {
    console.log('ALL OK')
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
