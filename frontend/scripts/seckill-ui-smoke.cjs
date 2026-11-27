// 模块 4 秒杀 UI 冒烟：游客浏览 → 学员抢购/排队/跳转收银台/支付 → 管理员活动页
const { chromium } = require('playwright-core')
const path = require('node:path')
const fs = require('node:fs')

const base = process.env.UI_BASE || 'http://localhost:5173'
const outputDir = path.join(__dirname, '..', 'screenshots')
fs.mkdirSync(outputDir, { recursive: true })

async function login(page, account, password) {
  await page.goto(`${base}/login`, { waitUntil: 'networkidle' })
  await page.getByPlaceholder('请输入账号').fill(account)
  await page.getByPlaceholder('请输入密码').fill(password)
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 15000 })
}

async function main() {
  const browser = await chromium.launch({ channel: 'msedge', headless: true })
  const errors = []

  const studentContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const studentPage = await studentContext.newPage()
  const adminContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const adminPage = await adminContext.newPage()

  for (const [label, target] of [['student', studentPage], ['admin', adminPage]]) {
    target.on('pageerror', (error) => errors.push(`${label} pageerror: ${error.message}`))
    target.on('console', (message) => {
      if (message.type() === 'error') errors.push(`${label} console: ${message.text()}`)
    })
  }

  function check(label, condition, detail) {
    console.log(`${condition ? 'OK  ' : 'FAIL'} ${label}${detail ? ' :: ' + detail : ''}`)
    if (!condition) process.exitCode = 1
  }

  // 游客访问秒杀专区
  await studentPage.goto(`${base}/seckill`, { waitUntil: 'networkidle' })
  await studentPage.waitForTimeout(800)
  const guestText = await studentPage.locator('body').innerText()
  check('游客可浏览秒杀专区', guestText.includes('限时秒杀') && guestText.includes('立即抢购'))
  check('秒杀卡片展示倒计时与名额', guestText.includes('距结束') || guestText.includes('距开始'))
  await studentPage.screenshot({ path: path.join(outputDir, 'seckill-public.png'), fullPage: true })

  // 学员抢购
  await login(studentPage, 'student', 'Student@123')
  await studentPage.goto(`${base}/seckill`, { waitUntil: 'networkidle' })
  await studentPage.waitForTimeout(800)
  const claimButton = studentPage.getByRole('button', { name: '立即抢购' }).first()
  check('登录后出现可抢购按钮', await claimButton.isVisible())
  await claimButton.click()
  await studentPage.waitForURL('**/pay/**', { timeout: 30000 })
  check('抢购成功跳转模拟收银台', studentPage.url().includes('/pay/'), studentPage.url())
  await studentPage.screenshot({ path: path.join(outputDir, 'seckill-pay.png'), fullPage: true })

  await studentPage.getByRole('button', { name: /确认支付/ }).click()
  await studentPage.waitForTimeout(1500)
  const paidText = await studentPage.locator('body').innerText()
  check('秒杀订单支付成功', paidText.includes('支付成功'))
  await studentPage.screenshot({ path: path.join(outputDir, 'seckill-paid.png'), fullPage: true })

  // 管理员活动管理
  await login(adminPage, 'admin', 'Admin@123')
  await adminPage.goto(`${base}/admin/seckill`, { waitUntil: 'networkidle' })
  await adminPage.waitForTimeout(800)
  const adminText = await adminPage.locator('body').innerText()
  check('后台秒杀菜单与页面可见', adminText.includes('秒杀活动') && adminText.includes('库存'))
  check('后台展示演示活动', adminText.includes('考研'))
  await adminPage.getByRole('button', { name: '新增秒杀活动' }).click()
  await adminPage.waitForTimeout(500)
  const dialogText = await adminPage.locator('.el-dialog').innerText()
  check('新增活动弹窗包含关键字段', dialogText.includes('课程') && dialogText.includes('秒杀价')
    && dialogText.includes('总名额') && dialogText.includes('每人限购'))
  await adminPage.screenshot({ path: path.join(outputDir, 'seckill-admin.png'), fullPage: true })

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
