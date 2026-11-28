// 模块 7 院校与看板 UI 冒烟：院校检索 → 详情/分数线图 → 后台院校抽屉 → 数据看板 → 用户活跃 → 审计
const { chromium } = require('playwright-core')
const path = require('node:path')
const fs = require('node:fs')
const { apiLogin, uiLogin } = require('./lib/auth-helper.cjs')

const base = process.env.UI_BASE || 'http://localhost:5173'
const apiBase = 'http://localhost:8080/api/v1'
const outputDir = path.join(__dirname, '..', 'screenshots')
fs.mkdirSync(outputDir, { recursive: true })

const stamp = Date.now().toString().slice(-8)
const UNIVERSITY = `UI测试院校${stamp}`
const MAJOR = `UI测试专业${stamp}`

async function api(method, url, body, token, expectOk = true) {
  const response = await fetch(`${apiBase}${url}`, {
    method,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  })
  const data = await response.json()
  if (expectOk && !response.ok) throw new Error(`${method} ${url} -> ${response.status}: ${JSON.stringify(data)}`)
  return data
}

async function loginApi(account, password) {
  return apiLogin(account, password)
}

async function setupData() {
  const admin = (await loginApi('admin', 'Admin@123')).accessToken
  const student = (await loginApi('student', 'Student@123')).accessToken

  const university = (await api('POST', '/admin/universities', {
    name: UNIVERSITY,
    province: '江苏',
    city: '南京',
    level: '211',
    type: '综合',
    website: 'https://ui-smoke.example.com',
    introduction: 'UI 自动化测试院校简介',
    featured: true,
    sort: 5,
  }, admin)).data

  const major = (await api('POST', `/admin/universities/${university.id}/majors`, {
    name: MAJOR,
    code: '888888',
    degreeType: '学硕',
    duration: '3年',
    tuition: 8000,
    examSubjects: '政治、英语一、数学一、专业课',
    sort: 10,
  }, admin)).data

  await api('POST', `/admin/universities/${university.id}/score-lines`, {
    majorId: major.id,
    year: 2024,
    political: 60,
    english: 60,
    math: 90,
    majorScore: 95,
    total: 320,
  }, admin)
  await api('POST', `/admin/universities/${university.id}/score-lines`, {
    majorId: major.id,
    year: 2025,
    political: 60,
    english: 60,
    math: 95,
    majorScore: 100,
    total: 330,
  }, admin)
  await api('POST', `/admin/universities/${university.id}/admission-stats`, {
    majorId: major.id,
    year: 2025,
    applicants: 100,
    admitted: 12,
    minScore: 335,
  }, admin)

  // 造一笔已支付订单，让看板有成交与热销数据
  const owned = new Set(((await api('GET', '/my/courses', null, student)).data || []).map((item) => item.courseId))
  const courses = (await api('GET', '/public/courses?size=50')).data.items
  for (const course of courses) {
    if (owned.has(course.id)) continue
    const order = await api('POST', '/orders', { courseIds: [course.id] }, student, false)
    if (order.code !== 0) continue
    await api('POST', `/payments/${order.data.orderNo}/pay`, { channel: 'MOCK_ALIPAY' }, student, false)
    break
  }

  return { universityId: university.id }
}

async function login(page, account, password) {
  await uiLogin(page, base, account, password)
}

async function main() {
  await setupData()
  const browser = await chromium.launch({ channel: 'msedge', headless: true })
  const errors = []
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
  const page = await context.newPage()
  page.on('pageerror', (error) => errors.push(`pageerror: ${error.message}`))
  page.on('console', (message) => {
    if (message.type() === 'error' && !message.text().includes('Failed to load resource')) {
      errors.push(`console: ${message.text()}`)
    }
  })

  function check(label, condition, detail) {
    console.log(`${condition ? 'OK  ' : 'FAIL'} ${label}${detail ? ' :: ' + detail : ''}`)
    if (!condition) process.exitCode = 1
  }

  // ---------- 前台院校库 ----------
  await page.goto(`${base}/universities`, { waitUntil: 'networkidle' })
  await page.waitForSelector('.university-card', { timeout: 15000 })
  let text = await page.locator('body').innerText()
  check('院校库展示筛选与演示院校', text.includes('院校库') && text.includes('北京大学')
    && text.includes('个专业'), text.slice(0, 80))

  await page.getByPlaceholder('院校名称关键词').fill('北京大学')
  await page.getByRole('button', { name: '搜索', exact: true }).click()
  await page.waitForTimeout(600)
  const cards = page.locator('.university-card')
  check('院校关键词检索命中', (await cards.count()) === 1)
  await page.screenshot({ path: path.join(outputDir, 'university-list.png'), fullPage: true })

  // ---------- 院校详情 ----------
  await cards.first().click()
  await page.waitForURL(/\/universities\/\d+/, { timeout: 15000 })
  await page.waitForSelector('text=专业目录', { timeout: 15000 })
  text = await page.locator('body').innerText()
  check('院校详情展示专业目录与报录比', text.includes('北京大学') && text.includes('计算机科学与技术')
    && text.includes('报录比'))

  await page.getByRole('tab', { name: '历年分数线' }).click()
  await page.waitForSelector('.content-card canvas', { timeout: 15000 })
  await page.waitForTimeout(500)
  check('分数线页渲染走势图', await page.locator('.content-card canvas').count() >= 1)
  text = await page.locator('body').innerText()
  check('分数线表格展示分数明细', text.includes('总分') && text.includes('专业课'))
  await page.screenshot({ path: path.join(outputDir, 'university-detail.png'), fullPage: true })

  // ---------- 后台院校管理 ----------
  await login(page, 'admin', 'Admin@123')
  await page.goto(`${base}/admin/universities`, { waitUntil: 'networkidle' })
  await page.waitForSelector('text=院校库管理', { timeout: 15000 })
  check('后台院校管理页加载', (await page.locator('body').innerText()).includes('院校库管理'))

  await page.getByPlaceholder('院校名称').fill(UNIVERSITY)
  await page.getByRole('button', { name: '查询', exact: true }).click()
  await page.waitForTimeout(700)
  const row = page.locator('.el-table__row').filter({ hasText: UNIVERSITY }).first()
  check('后台可检索新增院校', await row.count() > 0)
  await row.getByRole('button', { name: '招生数据' }).click()
  await page.waitForSelector('.el-drawer', { timeout: 15000 })
  await page.waitForTimeout(600)
  text = await page.locator('.el-drawer').innerText()
  check('招生数据抽屉展示专业/分数线/报录比页签',
    text.includes(MAJOR) && text.includes('历年分数线') && text.includes('报录比'))
  await page.screenshot({ path: path.join(outputDir, 'admin-university-drawer.png'), fullPage: true })
  await page.locator('.el-drawer__close-btn').click()
  await page.waitForTimeout(400)

  // ---------- 数据看板 ----------
  await page.goto(`${base}/admin/dashboard`, { waitUntil: 'networkidle' })
  await page.waitForSelector('.chart-card canvas', { timeout: 15000 })
  await page.waitForTimeout(700)
  text = await page.locator('body').innerText()
  check('数据看板展示 KPI 与图表', text.includes('今日成交额') && text.includes('热销课程 TOP 6')
    && text.includes('订单状态分布') && text.includes('近 14 天'))
  const canvasCount = await page.locator('.chart-card canvas').count()
  check('看板四个图表均已渲染', canvasCount >= 4, `canvas=${canvasCount}`)
  await page.screenshot({ path: path.join(outputDir, 'admin-dashboard.png'), fullPage: true })

  // 切换近 7 天
  await page.getByText('近 7 天').click()
  await page.waitForTimeout(600)
  check('看板时间范围切换', (await page.locator('body').innerText()).includes('近 7 天已支付'))

  // ---------- 用户活跃 ----------
  await page.goto(`${base}/admin/users`, { waitUntil: 'networkidle' })
  await page.waitForSelector('.el-table__row', { timeout: 15000 })
  const adminRow = page.locator('.el-table__row').filter({ hasText: 'admin' }).first()
  await adminRow.getByRole('button', { name: '活跃数据' }).click()
  await page.waitForSelector('text=用户活跃数据', { timeout: 15000 })
  await page.waitForTimeout(500)
  text = await page.locator('.el-dialog').innerText()
  check('用户活跃数据弹窗展示订单/消费/选课', text.includes('已支付订单') && text.includes('累计消费')
    && text.includes('已购课程'))
  await page.screenshot({ path: path.join(outputDir, 'admin-user-activity.png'), fullPage: true })
  await page.getByRole('button', { name: '关闭', exact: true }).click()
  await page.waitForTimeout(400)

  // ---------- 审计 ----------
  await page.goto(`${base}/admin/logs`, { waitUntil: 'networkidle' })
  await page.waitForSelector('.el-table__row', { timeout: 15000 })
  await page.waitForTimeout(500)
  text = await page.locator('body').innerText()
  check('审计日志展示院校变更', text.includes('UNIVERSITY') && text.includes('UNIVERSITY_MAJOR'))
  await page.screenshot({ path: path.join(outputDir, 'admin-audit.png'), fullPage: true })

  check('无前端异常', errors.length === 0, errors.join(' | '))
  await browser.close()
  console.log(`\n截图输出目录：${outputDir}`)
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
