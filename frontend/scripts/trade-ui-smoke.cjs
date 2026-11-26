// 模块 3 交易 UI 冒烟：游客领取 → 登录回跳 → 领券 → 下单 → 支付 → 我的中心 → 退款 → 管理员审核
const { chromium } = require('playwright-core')
const path = require('node:path')
const fs = require('node:fs')

const base = process.env.UI_BASE || 'http://localhost:5173'
const outputDir = path.join(__dirname, '..', 'screenshots')
fs.mkdirSync(outputDir, { recursive: true })

async function main() {
  const browser = await chromium.launch({ channel: 'msedge', headless: true })
  const studentContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const adminContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await studentContext.newPage()
  const adminPage = await adminContext.newPage()

  const errors = []
  for (const [label, target] of [['student', page], ['admin', adminPage]]) {
    target.on('pageerror', (error) => errors.push(`${label} pageerror: ${error.message}`))
    target.on('console', (message) => {
      if (message.type() === 'error') errors.push(`${label} console: ${message.text()}`)
    })
  }

  function check(label, condition, detail) {
    console.log(`${condition ? 'OK  ' : 'FAIL'} ${label}${detail ? ' :: ' + detail : ''}`)
    if (!condition) process.exitCode = 1
  }

  async function bodyText(target = page) {
    return target.locator('body').innerText()
  }

  // 游客访问课程详情 → 领券被引导登录
  await page.goto(`${base}/courses/1`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(500)
  check('课程详情展示立即报名', (await bodyText()).includes('立即报名'))
  await page.getByRole('button', { name: '领取', exact: true }).first().click()
  await page.waitForURL('**/login**', { timeout: 10000 })
  check('游客领券跳转登录并保留回跳', page.url().includes('redirect='), page.url())

  await page.getByPlaceholder('请输入账号').fill('student')
  await page.getByPlaceholder('请输入密码').fill('Student@123')
  await page.getByRole('button', { name: '登录', exact: true }).first().click()
  await page.waitForURL('**/courses/1', { timeout: 15000 })
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(600)
  check('学员登录后回跳课程详情', page.url().endsWith('/courses/1'))

  // 领取政治券
  await page.getByRole('button', { name: '领取', exact: true }).first().click()
  await page.waitForTimeout(900)
  check('领取优惠券后按钮变为已领取', (await bodyText()).includes('已领取'))
  await page.screenshot({ path: path.join(outputDir, '11-course-detail-trade.png'), fullPage: true })

  // 下单
  await page.getByRole('button', { name: '立即报名', exact: true }).click()
  await page.waitForURL('**/checkout**', { timeout: 15000 })
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(700)
  check('进入确认订单页', (await bodyText()).includes('确认订单'))
  await page.locator('.coupon-option').filter({ hasText: '政治强化' }).first().click()
  await page.waitForTimeout(900)
  const checkoutText = await bodyText()
  check('选择优惠券后应付 569.00', checkoutText.includes('569.00'), checkoutText.match(/应付金额[\s\S]{0,80}/)?.[0])
  await page.screenshot({ path: path.join(outputDir, '12-checkout.png'), fullPage: true })

  await page.getByRole('button', { name: '提交订单', exact: true }).click()
  await page.waitForURL('**/pay/**', { timeout: 15000 })
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(600)
  const orderNo = page.url().split('/pay/')[1]
  check('进入模拟收银台', (await bodyText()).includes('确认支付'), orderNo)
  await page.screenshot({ path: path.join(outputDir, '13-pay.png'), fullPage: true })

  await page.getByRole('button', { name: /确认支付/ }).click()
  await page.waitForTimeout(1500)
  check('支付成功并开通课程', (await bodyText()).includes('支付成功'))
  await page.screenshot({ path: path.join(outputDir, '14-pay-success.png'), fullPage: true })

  // 我的中心
  await page.goto(`${base}/user?tab=orders`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(700)
  const ordersText = await bodyText()
  check('我的订单显示新订单', ordersText.includes(orderNo) && ordersText.includes('已支付'))
  await page.screenshot({ path: path.join(outputDir, '15-my-orders.png'), fullPage: true })

  await page.goto(`${base}/user?tab=coupons`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(700)
  check('我的优惠券显示已使用', (await bodyText()).includes('政治强化 · 满 300 减 30'))
  await page.screenshot({ path: path.join(outputDir, '16-my-coupons.png'), fullPage: true })

  await page.goto(`${base}/user?tab=courses`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(700)
  const myCoursesText = await bodyText()
  check('我的课程包含刚购买的课程', myCoursesText.includes('考研政治强化提升班'))
  check('我的课程包含此前购买的数学课', myCoursesText.includes('考研数学一强化班'))
  await page.screenshot({ path: path.join(outputDir, '17-my-courses.png'), fullPage: true })

  // 学员申请退款
  await page.goto(`${base}/user?tab=orders`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(700)
  const orderRow = page.locator('.el-table__row').filter({ hasText: orderNo }).first()
  await orderRow.getByRole('button', { name: '申请退款' }).click()
  await page.waitForTimeout(400)
  await page.getByPlaceholder('请说明退款原因').fill('UI 冒烟测试：课程内容与预期不符')
  await page.getByRole('button', { name: '提交申请' }).click()
  await page.waitForTimeout(1200)
  check('提交退款后订单显示退款审核中', (await bodyText()).includes('退款审核中'))
  await page.screenshot({ path: path.join(outputDir, '18-refund-applied.png'), fullPage: true })

  // 管理员审核
  await adminPage.goto(`${base}/login`, { waitUntil: 'networkidle' })
  await adminPage.getByRole('button', { name: '登录', exact: true }).first().click()
  await adminPage.waitForURL('**/admin/dashboard', { timeout: 15000 })
  await adminPage.waitForLoadState('networkidle')

  await adminPage.goto(`${base}/admin/orders`, { waitUntil: 'networkidle' })
  await adminPage.waitForTimeout(700)
  check('管理员订单管理可见新订单', (await bodyText(adminPage)).includes(orderNo))
  await adminPage.screenshot({ path: path.join(outputDir, '19-admin-orders.png'), fullPage: true })

  await adminPage.goto(`${base}/admin/coupons`, { waitUntil: 'networkidle' })
  await adminPage.waitForTimeout(700)
  check('管理员优惠券管理可见种子券', (await bodyText(adminPage)).includes('新人专享'))
  await adminPage.screenshot({ path: path.join(outputDir, '20-admin-coupons.png'), fullPage: true })

  await adminPage.goto(`${base}/admin/refunds`, { waitUntil: 'networkidle' })
  await adminPage.waitForTimeout(700)
  const refundRow = adminPage.locator('.el-table__row').filter({ hasText: orderNo }).first()
  check('管理员退款列表可见待审核申请', (await refundRow.innerText()).includes('待审核'))
  await refundRow.getByRole('button', { name: '通过' }).click()
  await adminPage.waitForTimeout(400)
  await adminPage.getByPlaceholder('选填，将记录到退款申请中').fill('UI 冒烟审核通过')
  await adminPage.getByRole('button', { name: '确认通过并退款' }).click()
  await adminPage.waitForTimeout(1400)
  check('退款审核通过', (await adminPage.locator('.el-table__row').filter({ hasText: orderNo }).first().innerText()).includes('已通过'))
  await adminPage.screenshot({ path: path.join(outputDir, '21-admin-refunds.png'), fullPage: true })

  // 退款后课程权限撤销
  await page.goto(`${base}/user?tab=courses`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(700)
  const afterRefund = await bodyText()
  check('退款后撤销政治课程', !afterRefund.includes('考研政治强化提升班'))
  check('数学课程权限保留', afterRefund.includes('考研数学一强化班'))
  await page.screenshot({ path: path.join(outputDir, '22-my-courses-after-refund.png'), fullPage: true })

  console.log('page errors:', errors.length ? errors : 'none')
  await browser.close()
  if (errors.length) process.exitCode = 1
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
