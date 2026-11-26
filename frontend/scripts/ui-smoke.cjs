const { chromium } = require('playwright-core')
const path = require('node:path')
const fs = require('node:fs')

const base = process.env.UI_BASE || 'http://localhost:5173'
const outputDir = path.join(__dirname, '..', 'screenshots')
fs.mkdirSync(outputDir, { recursive: true })

async function main() {
  const browser = await chromium.launch({ channel: 'msedge', headless: true })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  const errors = []
  page.on('pageerror', (error) => errors.push(`pageerror: ${error.message}`))
  page.on('console', (message) => {
    if (message.type() === 'error') errors.push(`console: ${message.text()}`)
  })

  for (const [route, file, expected] of [
    ['/', '00-home.png', '热门课程'],
    ['/courses', '00b-courses.png', '课程中心'],
    ['/courses/1', '00c-course-detail.png', '课程目录'],
    ['/teachers', '00d-teachers.png', '名师团队'],
    ['/articles', '00e-articles.png', '考研资讯'],
    ['/articles/1', '00f-article-detail.png', '考研资讯'],
    ['/materials', '00g-materials.png', '资料下载'],
  ]) {
    await page.goto(`${base}${route}`, { waitUntil: 'networkidle' })
    await page.waitForTimeout(500)
    await page.screenshot({ path: path.join(outputDir, file), fullPage: true })
    const text = await page.locator('body').innerText()
    console.log(`${route}:`, text.includes(expected) ? 'OK' : 'MISSING', text.slice(0, 120).replace(/\n/g, ' | '))
  }

  await page.goto(`${base}/login`, { waitUntil: 'networkidle' })
  await page.screenshot({ path: path.join(outputDir, '01-login.png'), fullPage: true })
  console.log('login text:', (await page.locator('body').innerText()).slice(0, 200).replace(/\n/g, ' | '))

  await page.getByRole('button', { name: '登录', exact: true }).first().click()
  await page.waitForURL('**/admin/dashboard', { timeout: 15000 })
  await page.waitForLoadState('networkidle')
  await page.screenshot({ path: path.join(outputDir, '02-dashboard.png'), fullPage: true })
  console.log('dashboard text:', (await page.locator('body').innerText()).slice(0, 300).replace(/\n/g, ' | '))

  await page.getByRole('button', { name: '检查数据库路由' }).click()
  await page.waitForTimeout(1000)
  const routeText = await page.locator('body').innerText()
  console.log('route check:', routeText.includes('smart_learning_read') && routeText.includes('smart_learning')
    ? 'OK' : 'MISSING', routeText.match(/smart_learning(_read)?/g))
  await page.screenshot({ path: path.join(outputDir, '02b-route-check.png'), fullPage: true })

  for (const [route, file, expected] of [
    ['/admin/courses', '06-courses.png', '课程管理'],
    ['/admin/teachers', '07-teachers.png', '讲师管理'],
    ['/admin/articles', '08-articles.png', '文章管理'],
    ['/admin/materials', '09-materials.png', '资料管理'],
    ['/admin/banners', '10-banners.png', 'Banner 管理'],
    ['/admin/groups', '03-groups.png', '权限分组'],
    ['/admin/users', '04-users.png', '用户管理'],
    ['/admin/logs', '05-logs.png', '审计日志'],
  ]) {
    await page.goto(`${base}${route}`, { waitUntil: 'networkidle' })
    await page.waitForTimeout(600)
    await page.screenshot({ path: path.join(outputDir, file), fullPage: true })
    const text = await page.locator('body').innerText()
    console.log(`${route}:`, text.includes(expected) ? 'OK' : 'MISSING', text.slice(0, 160).replace(/\n/g, ' | '))
    if (route === '/admin/groups') {
      await page.getByRole('button', { name: '分配权限' }).first().click()
      await page.waitForTimeout(600)
      const dialogText = await page.locator('.el-dialog').innerText()
      console.log('priv dialog:', dialogText.includes('VIEW') && dialogText.includes('ACTION') ? 'OK' : 'MISSING')
      await page.screenshot({ path: path.join(outputDir, '03b-priv-dialog.png'), fullPage: true })
      await page.getByRole('button', { name: '取消' }).first().click()
    }
  }

  console.log('page errors:', errors.length ? errors : 'none')
  await browser.close()
  if (errors.length) process.exitCode = 1
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
