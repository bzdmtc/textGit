// 定时任务验收：待支付订单超时关单 + 优惠券释放/过期
import { execSync } from 'node:child_process'

const BASE = 'http://localhost:8080/api/v1'

async function api(method, path, body, token, expectOk = true) {
  const response = await fetch(`${BASE}${path}`, {
    method,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  })
  const text = await response.text()
  const data = JSON.parse(text)
  if (expectOk && !response.ok) {
    throw new Error(`${method} ${path} -> HTTP ${response.status}: ${text.slice(0, 400)}`)
  }
  return data
}

function sql(query) {
  return execSync(`sqlcmd -S localhost -C -E -d smart_learning -Q "${query}" -W`, { encoding: 'utf8' })
}

async function login(account, password) {
  const response = await api('POST', '/auth/login', { account, password })
  return response.data.accessToken
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

const adminToken = await login('admin', 'Admin@123')
const studentToken = await login('teacher2', 'Teacher@123')

const now = Date.now()
const iso = (ms) => new Date(ms).toISOString().slice(0, 19)
const coupon = await api('POST', '/admin/coupons', {
  name: '定时任务测试券',
  type: 'FIXED',
  value: 10,
  minAmount: 100,
  totalQuantity: 5,
  perUserLimit: 1,
  startAt: iso(now - 86400000),
  endAt: iso(now + 86400000),
  subject: '',
  description: 'scheduler test',
  status: 'ON',
}, adminToken)
const couponId = coupon.data.id
console.log('created coupon', couponId)

const claim = await api('POST', `/coupons/${couponId}/claim`, null, studentToken)
const userCouponId = claim.data.id
console.log('claimed user coupon', userCouponId)

const order = await api('POST', '/orders', { courseIds: [2], userCouponId }, studentToken)
const orderNo = order.data.orderNo
console.log('created order', orderNo, 'status', order.data.status)

// 把订单与优惠券都置为已过期，等待定时任务（每 60 秒）
sql(`UPDATE orders SET expire_at = DATEADD(MINUTE, -5, SYSUTCDATETIME()) WHERE order_no = '${orderNo}'; ` +
    `UPDATE coupon SET end_at = DATEADD(MINUTE, -5, SYSUTCDATETIME()) WHERE id = ${couponId};`)
console.log('expired order and coupon in DB, waiting for scheduler...')

let canceled = null
for (let i = 0; i < 40; i++) {
  await sleep(3000)
  const detail = await api('GET', `/my/orders/${orderNo}`, null, studentToken, false)
  if (detail.data?.status === 'CANCELED') {
    canceled = detail.data
    break
  }
}

if (!canceled) {
  console.error('FAIL order was not canceled by scheduler within 120s')
  process.exitCode = 1
} else {
  console.log('OK   order auto-canceled', canceled.orderNo)
}

const mine = await api('GET', '/my/coupons', null, studentToken)
const couponAfter = mine.data.find((item) => item.id === userCouponId)
console.log(couponAfter?.status === 'EXPIRED'
  ? 'OK   coupon released then expired'
  : `FAIL coupon status is ${couponAfter?.status}`)
if (couponAfter?.status !== 'EXPIRED') process.exitCode = 1

const dbStatus = sql(`SET NOCOUNT ON; SELECT status FROM user_coupon WHERE id = ${userCouponId};`)
console.log('db user_coupon status:', dbStatus.trim().split(/\r?\n/).pop())

// 清理测试数据（订单、券关系已解除，可安全删除）
sql(`DELETE FROM order_item WHERE order_id = (SELECT id FROM orders WHERE order_no = '${orderNo}'); ` +
    `DELETE FROM orders WHERE order_no = '${orderNo}'; ` +
    `DELETE FROM user_coupon WHERE id = ${userCouponId}; ` +
    `DELETE FROM coupon WHERE id = ${couponId};`)
console.log('cleanup done')

if (process.exitCode !== 1) console.log('\nSCHEDULER CHECKS PASSED')
