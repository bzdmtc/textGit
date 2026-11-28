// 模块 6 题库考试 UI 冒烟：章节练习即时判题 → 成绩 → 错题本 → 套卷考试 → 后台题库/试卷
const { chromium } = require('playwright-core')
const path = require('node:path')
const fs = require('node:fs')
const { apiLogin, uiLogin } = require('./lib/auth-helper.cjs')

const base = process.env.UI_BASE || 'http://localhost:5173'
const apiBase = 'http://localhost:8080/api/v1'
const outputDir = path.join(__dirname, '..', 'screenshots')
fs.mkdirSync(outputDir, { recursive: true })

const stamp = Date.now().toString().slice(-8)
const SUBJECT = `UI测试-${stamp}`
const PAPER_TITLE = `UI测试-${stamp} 模拟试卷`

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

async function loginApi(account, password) {
  return (await apiLogin(account, password)).accessToken
}

async function setupData() {
  const admin = await loginApi('admin', 'Admin@123')
  const q1 = await api('POST', '/admin/questions', {
    subject: SUBJECT, chapter: 'UI章节', type: 'SINGLE',
    stem: `UI 自动化单选题一 ${stamp}：1+1 等于几？`,
    analysis: '1+1=2，选择 A。', difficulty: 1,
    options: [
      { label: 'A', content: '2', isCorrect: true },
      { label: 'B', content: '3' },
      { label: 'C', content: '4' },
      { label: 'D', content: '5' },
    ],
  }, admin)
  const q2 = await api('POST', '/admin/questions', {
    subject: SUBJECT, chapter: 'UI章节', type: 'SINGLE',
    stem: `UI 自动化单选题二 ${stamp}：2+2 等于几？`,
    analysis: '2+2=4，选择 B。', difficulty: 1,
    options: [
      { label: 'A', content: '3' },
      { label: 'B', content: '4', isCorrect: true },
      { label: 'C', content: '5' },
      { label: 'D', content: '6' },
    ],
  }, admin)
  const paper = await api('POST', '/admin/papers', {
    title: PAPER_TITLE, type: 'MOCK', subject: SUBJECT, chapter: 'UI章节',
    description: 'UI 自动化冒烟试卷', durationMin: 10,
    questions: [
      { questionId: q1.id, score: 10 },
      { questionId: q2.id, score: 10 },
    ],
  }, admin)
  await api('PUT', `/admin/papers/${paper.paper.id}/status`, { status: 'PUBLISHED' }, admin)
  return { q2Stem: q2.stem, paperId: paper.paper.id }
}

async function login(page, account, password) {
  await uiLogin(page, base, account, password)
}

async function main() {
  const { q2Stem } = await setupData()
  const browser = await chromium.launch({ channel: 'msedge', headless: true })
  const errors = []
  const studentContext = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
  const studentPage = await studentContext.newPage()
  const adminContext = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
  const adminPage = await adminContext.newPage()
  const guestContext = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
  const guestPage = await guestContext.newPage()

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

  // ---------- 题库首页 ----------
  await login(studentPage, 'student', 'Student@123')
  await studentPage.goto(`${base}/practice`, { waitUntil: 'networkidle' })
  await studentPage.waitForSelector('text=章节练习', { timeout: 15000 })
  const homeText = await studentPage.locator('body').innerText()
  check('题库首页展示章节练习与统计', homeText.includes('章节练习') && homeText.includes('累计作答')
    && homeText.includes('试卷中心'))
  check('题库首页展示已发布试卷', homeText.includes(PAPER_TITLE))
  await studentPage.screenshot({ path: path.join(outputDir, 'practice-home.png'), fullPage: true })

  // 选择 UI 测试科目并开始章节练习
  await studentPage.locator('.el-select').first().click()
  await studentPage.locator('.el-select-dropdown__item').filter({ hasText: SUBJECT }).first().click()
  await studentPage.waitForTimeout(400)
  await studentPage.getByRole('button', { name: '开始章节练习' }).click()
  await studentPage.waitForURL(/\/practice\/session\/\d+/, { timeout: 15000 })
  await studentPage.waitForSelector('.option-card', { timeout: 15000 })
  check('进入章节练习答题页', (await studentPage.locator('body').innerText()).includes('章节练习'))

  // 第一题选 A（正确）
  await studentPage.locator('.option-card').first().click()
  await studentPage.waitForSelector('text=回答正确', { timeout: 15000 })
  check('练习即时判题显示回答正确与解析', (await studentPage.locator('body').innerText()).includes('解析：'))
  await studentPage.screenshot({ path: path.join(outputDir, 'practice-session.png'), fullPage: true })

  // 第二题选 A（错误，正确答案 B）
  await studentPage.getByRole('button', { name: '下一题' }).click()
  await studentPage.waitForTimeout(300)
  await studentPage.locator('.option-card').first().click()
  await studentPage.waitForSelector('text=回答错误', { timeout: 15000 })
  check('练习即时判题显示回答错误与正确答案', (await studentPage.locator('body').innerText()).includes('正确答案：'))

  // 结束练习
  await studentPage.getByRole('button', { name: '结束练习', exact: true }).first().click()
  await studentPage.getByRole('button', { name: '确定' }).click()
  await studentPage.waitForURL(/\/practice\/result\/\d+/, { timeout: 15000 })
  await studentPage.waitForSelector('text=答题回顾', { timeout: 15000 })
  const resultText = await studentPage.locator('body').innerText()
  check('练习成绩报告展示得分与回顾', resultText.includes('成绩报告') && resultText.includes('答题回顾')
    && resultText.includes('得分率'))
  await studentPage.screenshot({ path: path.join(outputDir, 'practice-result.png'), fullPage: true })

  // ---------- 错题本 ----------
  await studentPage.goto(`${base}/wrong-book`, { waitUntil: 'networkidle' })
  await studentPage.waitForTimeout(800)
  const wrongText = await studentPage.locator('body').innerText()
  check('错题本自动收录答错题目', wrongText.includes('错题本') && wrongText.includes('UI 自动化单选题二'))
  await studentPage.getByRole('button', { name: '查看答案' }).first().click()
  await studentPage.waitForTimeout(300)
  check('错题本可展开正确答案与解析', (await studentPage.locator('body').innerText()).includes('正确答案：'))
  await studentPage.getByRole('button', { name: '标记已掌握' }).first().click()
  await studentPage.waitForSelector('text=恢复待攻克', { timeout: 15000 })
  check('错题本支持标记掌握', (await studentPage.locator('body').innerText()).includes('已掌握'))
  await studentPage.screenshot({ path: path.join(outputDir, 'wrong-book.png'), fullPage: true })

  // 个人中心错题入口
  await studentPage.goto(`${base}/user?tab=wrong`, { waitUntil: 'networkidle' })
  await studentPage.waitForTimeout(600)
  check('个人中心展示我的错题', (await studentPage.locator('body').innerText()).includes('我的错题'))

  // ---------- 限时套卷 ----------
  await studentPage.goto(`${base}/practice`, { waitUntil: 'networkidle' })
  await studentPage.waitForTimeout(800)
  const paperCard = studentPage.locator('.paper-card').filter({ hasText: PAPER_TITLE }).first()
  check('题库首页展示 UI 测试试卷', await paperCard.count() > 0)
  await paperCard.getByRole('button', { name: '开始考试' }).click()
  await studentPage.waitForURL(/\/practice\/session\/\d+/, { timeout: 15000 })
  await studentPage.waitForSelector('.option-card', { timeout: 15000 })
  check('考试页展示倒计时', (await studentPage.locator('body').innerText()).includes('剩余时间'))
  await studentPage.locator('.option-card').first().click()
  await studentPage.waitForTimeout(500)
  await studentPage.getByRole('button', { name: '交卷并查看成绩' }).click()
  await studentPage.getByRole('button', { name: '确定' }).click()
  await studentPage.waitForURL(/\/practice\/result\/\d+/, { timeout: 15000 })
  await studentPage.waitForSelector('text=答题回顾', { timeout: 15000 })
  check('套卷交卷后生成成绩报告', (await studentPage.locator('body').innerText()).includes('未作答'))

  // ---------- 后台 ----------
  await login(adminPage, 'admin', 'Admin@123')
  await adminPage.goto(`${base}/admin/questions`, { waitUntil: 'networkidle' })
  await adminPage.waitForTimeout(800)
  await adminPage.getByPlaceholder('搜索题干 / 章节').fill('物质的唯一特性')
  await adminPage.getByRole('button', { name: '查询' }).click()
  await adminPage.waitForTimeout(800)
  const adminQuestionText = await adminPage.locator('body').innerText()
  check('后台题库列表与搜索', adminQuestionText.includes('题库管理')
    && adminQuestionText.includes('物质的唯一特性'))
  await adminPage.getByRole('button', { name: '新增题目' }).click()
  await adminPage.waitForSelector('text=新增题目', { timeout: 15000 })
  check('后台新增题目弹窗可打开', (await adminPage.locator('body').innerText()).includes('题干'))
  await adminPage.getByRole('button', { name: '取消' }).click()
  await adminPage.screenshot({ path: path.join(outputDir, 'admin-questions.png'), fullPage: true })

  await adminPage.goto(`${base}/admin/papers`, { waitUntil: 'networkidle' })
  await adminPage.waitForTimeout(800)
  const adminPaperText = await adminPage.locator('body').innerText()
  check('后台试卷列表展示试卷与状态', adminPaperText.includes('试卷管理')
    && adminPaperText.includes(PAPER_TITLE) && adminPaperText.includes('已发布'))
  await adminPage.getByRole('button', { name: '新增试卷' }).click()
  await adminPage.waitForSelector('text=保存', { timeout: 15000 })
  check('后台新增试卷弹窗可打开', (await adminPage.locator('body').innerText()).includes('添加题目'))
  await adminPage.getByRole('button', { name: '取消' }).first().click()
  await adminPage.screenshot({ path: path.join(outputDir, 'admin-papers.png'), fullPage: true })

  // ---------- 游客访问 ----------
  await guestPage.goto(`${base}/practice`, { waitUntil: 'networkidle' })
  await guestPage.waitForURL(/\/login/, { timeout: 15000 })
  check('游客访问题库跳转登录', guestPage.url().includes('/login'))

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
