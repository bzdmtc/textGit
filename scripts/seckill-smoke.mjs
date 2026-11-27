// 模块 4 秒杀 API 冒烟：后台建活动 → 预热 → 抢购入队 → Kafka 建单 → 支付 → 取消回补
const BASE = 'http://localhost:8080/api/v1'

let failures = 0
function ok(label, condition, detail) {
  if (condition) {
    console.log(`OK   ${label}${detail === undefined ? '' : ' :: ' + JSON.stringify(detail)}`)
  } else {
    failures++
    console.error(`FAIL ${label}${detail === undefined ? '' : ' :: ' + JSON.stringify(detail)}`)
  }
}

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
  let data
  try {
    data = JSON.parse(text)
  } catch {
    data = text
  }
  if (expectOk && !response.ok) {
    throw new Error(`${method} ${path} -> HTTP ${response.status}: ${text.slice(0, 500)}`)
  }
  return { status: response.status, data }
}

async function login(account, password) {
  const response = await api('POST', '/auth/login', { account, password })
  return response.data.data.accessToken
}

function utcWall(offsetMs) {
  return new Date(Date.now() + offsetMs).toISOString().slice(0, 19)
}

async function waitResult(activityId, token, expected, attempts = 20) {
  for (let i = 0; i < attempts; i++) {
    const response = await api('GET', `/seckill/activities/${activityId}/result`, null, token)
    const result = response.data.data
    if (result.status === expected) return result
    if (['FAILED', 'CANCELED', 'PAID', 'CREATED'].includes(result.status) && result.status !== expected) {
      // 已进入终态但不是期望值，继续返回让断言打印
      return result
    }
    await new Promise((resolve) => setTimeout(resolve, 500))
  }
  const response = await api('GET', `/seckill/activities/${activityId}/result`, null, token)
  return response.data.data
}

async function createActivity(adminToken, courseId, stock, suffix) {
  const response = await api('POST', '/admin/seckill/activities', {
    courseId,
    seckillPrice: 9.9,
    totalStock: stock,
    perUserLimit: 1,
    startAt: utcWall(-2 * 60 * 1000),
    endAt: utcWall(30 * 60 * 1000),
    status: 'ON',
  }, adminToken)
  const activity = response.data.data
  ok(`创建活动（${suffix}）并预热 Redis`, activity.id > 0 && activity.redisStock === stock,
    { id: activity.id, redisStock: activity.redisStock })
  return activity
}

async function main() {
  const admin = await login('admin', 'Admin@123')
  const student = await login('student', 'Student@123')
  const teacher = await login('teacher', 'Teacher@123')

  const paidActivity = await createActivity(admin, 1, 1, '支付链路')
  const canceledActivity = await createActivity(admin, 2, 1, '取消回补')

  // 公开列表无需登录
  const publicList = await api('GET', '/seckill/activities')
  ok('公开秒杀列表包含新活动', publicList.data.data.some((item) => item.id === paidActivity.id),
    publicList.data.data.map((item) => item.id))

  // 抢购 → 排队 → 建单
  const claim = await api('POST', `/seckill/activities/${paidActivity.id}/claim`, null, student)
  ok('抢购进入排队', claim.data.data.status === 'QUEUED', claim.data.data)
  const created = await waitResult(paidActivity.id, student, 'CREATED')
  ok('Kafka 异步建单成功', created.status === 'CREATED' && Boolean(created.orderNo), created)

  const duplicate = await api('POST', `/seckill/activities/${paidActivity.id}/claim`, null, student, false)
  ok('同一用户重复抢购被拒绝', duplicate.status === 400 && duplicate.data.code === 70007,
    { status: duplicate.status, code: duplicate.data.code })

  const soldOut = await api('POST', `/seckill/activities/${paidActivity.id}/claim`, null, teacher, false)
  ok('库存耗尽后抢购失败', soldOut.status === 400 && soldOut.data.code === 70004,
    { status: soldOut.status, code: soldOut.data.code })

  const pay = await api('POST', `/payments/${created.orderNo}/pay`, { channel: 'MOCK_ALIPAY' }, student)
  ok('秒杀订单支付成功', pay.data.data.order.status === 'PAID', pay.data.data.order.orderNo)
  const paidResult = await waitResult(paidActivity.id, student, 'PAID')
  ok('秒杀记录变为 PAID', paidResult.status === 'PAID', paidResult)

  const paidAdmin = await api('GET', `/admin/seckill/activities?status=ON&page=1&size=50`, null, admin)
  const paidRow = paidAdmin.data.data.items.find((item) => item.id === paidActivity.id)
  ok('后台库存扣减正确', paidRow.availableStock === 0 && paidRow.redisStock === 0 && paidRow.soldCount === 1,
    { available: paidRow.availableStock, redis: paidRow.redisStock, sold: paidRow.soldCount })

  // 取消回补链路
  const teacherClaim = await api('POST', `/seckill/activities/${canceledActivity.id}/claim`, null, teacher)
  ok('教师账号抢购第二场', teacherClaim.data.data.status === 'QUEUED', teacherClaim.data.data)
  const teacherCreated = await waitResult(canceledActivity.id, teacher, 'CREATED')
  ok('第二场建单成功', teacherCreated.status === 'CREATED', teacherCreated)
  const cancel = await api('POST', `/orders/${teacherCreated.orderNo}/cancel`, null, teacher)
  ok('取消秒杀订单', cancel.data.data.status === 'CANCELED', cancel.data.data.orderNo)
  const canceledResult = await waitResult(canceledActivity.id, teacher, 'CANCELED')
  ok('秒杀记录变为 CANCELED', canceledResult.status === 'CANCELED', canceledResult)
  const canceledAdmin = await api('GET', `/admin/seckill/activities?status=ON&page=1&size=50`, null, admin)
  const canceledRow = canceledAdmin.data.data.items.find((item) => item.id === canceledActivity.id)
  ok('取消后 DB/Redis 库存回补', canceledRow.availableStock === 1 && canceledRow.redisStock === 1,
    { available: canceledRow.availableStock, redis: canceledRow.redisStock })

  const records = await api('GET', `/admin/seckill/activities/${paidActivity.id}/records`, null, admin)
  ok('后台抢购记录可查', records.data.data.items.some((item) => item.status === 'PAID'),
    records.data.data.items.map((item) => item.status))

  console.log(JSON.stringify({
    cleanupActivityIds: [paidActivity.id, canceledActivity.id],
    paidOrderNo: created.orderNo,
    canceledOrderNo: teacherCreated.orderNo,
  }))
  if (failures > 0) {
    console.error(`${failures} failure(s)`)
    process.exit(1)
  }
  console.log('ALL OK')
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
