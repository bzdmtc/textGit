// 模块 4 秒杀并发验收：20 用户 × 每人 10 次并发抢 10 个名额
// 断言：恰好 10 笔订单、无超卖、每个用户最多 1 单；重复 Kafka 消息不重复建单
import { spawnSync } from 'node:child_process'

const BASE = 'http://localhost:8080/api/v1'
const USERS = 20
const ATTEMPTS_PER_USER = 10
const STOCK = 10
const COURSE_ID = 4
const DOCKER = 'C:\\Program Files\\Docker\\Docker\\resources\\bin\\docker.exe'

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
    throw new Error(`${method} ${path} -> HTTP ${response.status}: ${text.slice(0, 400)}`)
  }
  return { status: response.status, data }
}

function utcWall(offsetMs) {
  return new Date(Date.now() + offsetMs).toISOString().slice(0, 19)
}

async function login(account, password) {
  const response = await api('POST', '/auth/login', { account, password })
  return response.data.data.accessToken
}

async function waitCreated(activityId, token, attempts = 30) {
  for (let i = 0; i < attempts; i++) {
    const response = await api('GET', `/seckill/activities/${activityId}/result`, null, token)
    if (response.data.data.status === 'CREATED') return response.data.data
    await new Promise((resolve) => setTimeout(resolve, 500))
  }
  return null
}

async function main() {
  const admin = await login('admin', 'Admin@123')
  const suffix = Date.now().toString().slice(-8)
  const accounts = Array.from({ length: USERS }, (_, index) => `conc_test_${suffix}_${index}`)

  console.log(`创建 ${USERS} 个临时用户…`)
  await Promise.all(accounts.map((account) => api('POST', '/admin/users', {
    account,
    password: 'Test@123',
    realname: '并发测试',
    nickname: '并发测试',
    position: '压测账号',
    groupIds: [],
  }, admin)))
  const tokens = await Promise.all(accounts.map((account) => login(account, 'Test@123')))
  ok('临时用户创建并登录', tokens.every(Boolean), accounts.length)

  const activityResponse = await api('POST', '/admin/seckill/activities', {
    courseId: COURSE_ID,
    seckillPrice: 1.99,
    totalStock: STOCK,
    perUserLimit: 1,
    startAt: utcWall(-2 * 60 * 1000),
    endAt: utcWall(30 * 60 * 1000),
    status: 'ON',
  }, admin)
  const activity = activityResponse.data.data
  ok('并发活动创建并预热', activity.redisStock === STOCK, { id: activity.id, redisStock: activity.redisStock })

  console.log(`并发发起 ${USERS * ATTEMPTS_PER_USER} 次抢购…`)
  const requests = []
  tokens.forEach((token, userIndex) => {
    for (let i = 0; i < ATTEMPTS_PER_USER; i++) {
      requests.push(api('POST', `/seckill/activities/${activity.id}/claim`, null, token, false)
        .then((response) => ({ userIndex, response })))
    }
  })
  const results = await Promise.all(requests)
  const successes = results.filter((item) => item.response.status === 200)
  const limitRejected = results.filter((item) => item.response.status === 400 && item.response.data.code === 70005)
  const soldOutRejected = results.filter((item) => item.response.status === 400 && item.response.data.code === 70004)

  ok('抢购成功数等于库存', successes.length === STOCK,
    { success: successes.length, limit: limitRejected.length, soldOut: soldOutRejected.length })
  const winnerIndexes = [...new Set(successes.map((item) => item.userIndex))]
  ok('每个用户最多成功 1 次', winnerIndexes.length === STOCK, winnerIndexes.length)

  console.log('等待 Kafka 消费者建单…')
  const createdResults = await Promise.all(winnerIndexes.map((index) => waitCreated(activity.id, tokens[index])))
  ok('所有成功请求都建单成功', createdResults.every(Boolean), createdResults.filter(Boolean).length)

  const orders = createdResults.filter(Boolean).map((item) => item.orderNo)
  ok('订单号互不重复', new Set(orders).size === STOCK, new Set(orders).size)

  const recordResponse = await api('GET',
    `/admin/seckill/activities/${activity.id}/records?page=1&size=100`, null, admin)
  const records = recordResponse.data.data.items
  ok('抢购记录恰好 10 条', records.length === STOCK, records.length)
  ok('记录状态均为 CREATED', records.every((record) => record.status === 'CREATED'),
    records.map((record) => record.status))

  const activityAfter = (await api('GET', '/admin/seckill/activities?status=ON&page=1&size=100', null, admin))
    .data.data.items.find((item) => item.id === activity.id)
  ok('DB 库存耗尽且无超卖', activityAfter.availableStock === 0 && activityAfter.soldCount === STOCK,
    { available: activityAfter.availableStock, sold: activityAfter.soldCount })
  ok('Redis 库存耗尽', activityAfter.redisStock === 0, activityAfter.redisStock)

  // 重复投递同一 Kafka 消息，验证消费者幂等
  const firstCreated = createdResults.find(Boolean)
  const firstRecord = records.find((record) => record.orderNo === firstCreated.orderNo)
  const duplicatePayload = JSON.stringify({
    activityId: activity.id,
    userId: firstRecord ? firstRecord.userId : null,
    requestId: 'duplicate-test',
  })
  const producer = spawnSync(DOCKER, [
    'exec', '-i', 'smart-learning-kafka',
    '/opt/kafka/bin/kafka-console-producer.sh',
    '--bootstrap-server', 'localhost:9092',
    '--topic', 'seckill-order',
  ], { input: duplicatePayload + '\n', encoding: 'utf8', timeout: 20000 })
  ok('重复消息已投递', producer.status === 0, producer.stderr ? producer.stderr.slice(0, 200) : '')
  await new Promise((resolve) => setTimeout(resolve, 3000))
  const recordsAfter = (await api('GET',
    `/admin/seckill/activities/${activity.id}/records?page=1&size=100`, null, admin)).data.data.items
  ok('重复消息未重复建单', recordsAfter.length === STOCK,
    { before: records.length, after: recordsAfter.length })

  console.log(JSON.stringify({
    cleanupActivityId: activity.id,
    cleanupUserPattern: `conc_test_${suffix}_%`,
    orders,
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
