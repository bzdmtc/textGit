// 冒烟脚本共用登录工具：处理图形验证码
// 验证码答案存在本地 Redis（auth:captcha:<id>），脚本直接从 Redis 读取用于自动化。
const net = require('node:net')
const fs = require('node:fs')
const path = require('node:path')

const apiBase = process.env.API_BASE || 'http://localhost:8080/api/v1'

function redisPassword() {
  if (process.env.REDIS_PASSWORD) return process.env.REDIS_PASSWORD
  try {
    const file = path.join(__dirname, '..', '..', '..', 'application-local.yml')
    const yml = fs.readFileSync(file, 'utf8')
    const match = yml.match(/redis:\s*\r?\n\s*password:\s*["']?([^"'\r\n]+)/)
    return match ? match[1].trim() : ''
  } catch {
    return ''
  }
}

function parseReply(buffer) {
  if (buffer.length === 0) return null
  const type = String.fromCharCode(buffer[0])
  const lineEnd = buffer.indexOf('\r\n')
  if (lineEnd < 0) return null
  const line = buffer.subarray(1, lineEnd).toString()
  if (type === '+') return { value: line, offset: lineEnd + 2 }
  if (type === '-') return { value: `ERR ${line}`, offset: lineEnd + 2 }
  if (type === ':') return { value: line, offset: lineEnd + 2 }
  if (type === '$') {
    const length = Number(line)
    if (length === -1) return { value: null, offset: lineEnd + 2 }
    const start = lineEnd + 2
    if (buffer.length < start + length + 2) return null
    return { value: buffer.subarray(start, start + length).toString(), offset: start + length + 2 }
  }
  return { value: `ERR unsupported type ${type}`, offset: buffer.length }
}

function redisGet(key) {
  return new Promise((resolve, reject) => {
    const password = redisPassword()
    const socket = net.createConnection({
      host: process.env.REDIS_HOST || '127.0.0.1',
      port: Number(process.env.REDIS_PORT || 6379),
    })
    const commands = password ? [['AUTH', password], ['GET', key]] : [['GET', key]]
    const payload = commands.map((args) => `*${args.length}\r\n` + args.map((arg) => {
      const buf = Buffer.from(arg)
      return `$${buf.length}\r\n${buf}\r\n`
    }).join('')).join('')
    let pending = Buffer.alloc(0)
    const replies = []
    socket.on('connect', () => socket.write(payload))
    socket.on('data', (chunk) => {
      pending = Buffer.concat([pending, chunk])
      while (true) {
        const parsed = parseReply(pending)
        if (!parsed) break
        pending = pending.subarray(parsed.offset)
        replies.push(parsed.value)
        if (replies.length === commands.length) {
          socket.end()
          const value = replies[replies.length - 1]
          if (typeof value === 'string' && value.startsWith('ERR')) reject(new Error(value))
          else resolve(value)
        }
      }
    })
    socket.on('error', reject)
    socket.setTimeout(3000, () => {
      socket.destroy()
      reject(new Error('Redis timeout'))
    })
  })
}

async function fetchCaptcha() {
  const response = await fetch(`${apiBase}/auth/captcha`)
  const json = await response.json()
  if (!json.data) throw new Error(`captcha failed: ${json.message}`)
  const answer = await redisGet(`auth:captcha:${json.data.id}`)
  return { id: json.data.id, code: answer }
}

async function apiLogin(account, password) {
  const captcha = await fetchCaptcha()
  const response = await fetch(`${apiBase}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      account,
      password,
      captchaId: captcha.id,
      captchaCode: captcha.code,
    }),
  })
  const json = await response.json()
  if (json.code !== 0) throw new Error(`login failed: ${json.code} ${json.message}`)
  return json.data
}

async function solveCaptchaOnPage(page) {
  const image = page.locator('.captcha-img:visible').first()
  await image.waitFor({ state: 'visible', timeout: 15000 })
  const captchaId = await image.getAttribute('data-captcha-id')
  if (!captchaId) throw new Error('captcha id not found on page')
  const answer = await redisGet(`auth:captcha:${captchaId}`)
  if (!answer) throw new Error(`captcha answer not found for ${captchaId}`)
  await page.locator('input[placeholder="输入图片中的字符"]:visible').fill(answer)
}

async function uiLogin(page, base, account, password) {
  await page.goto(`${base}/login`, { waitUntil: 'networkidle' })
  await solveCaptchaOnPage(page)
  await page.getByPlaceholder('请输入账号').fill(account)
  await page.getByPlaceholder('请输入密码').fill(password)
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 15000 })
}

module.exports = { apiLogin, uiLogin, solveCaptchaOnPage, fetchCaptcha, redisGet }
